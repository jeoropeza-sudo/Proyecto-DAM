package cl.duocuc.Induztek.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.Response

class SyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    private val cache = ApiCacheDbHelper(appContext)
    private val api = ApiClient.crear(appContext)
    private val gson = Gson()

    override suspend fun doWork(): Result {
        while (true) {
            val tarea = cache.tomarSiguientePendiente() ?: break
            if (tarea.operation == "CERRAR_VISITA" &&
                tarea.resourceId != null &&
                cache.hayConflictoVisita(tarea.resourceId)
            ) {
                cache.marcarPendiente(
                    tarea.id,
                    "Resuelve los conflictos de esta visita antes de cerrarla."
                )
                cache.close()
                return Result.success()
            }
            val respuesta = try {
                enviarTarea(tarea)
            } catch (error: IOException) {
                cache.marcarPendiente(tarea.id, "Sin conexión. Se reintentará automáticamente.")
                cache.close()
                return Result.retry()
            } catch (error: IllegalArgumentException) {
                cache.marcarFallido(tarea.id, error.message ?: "La solicitud de sincronización no es válida.")
                continue
            } catch (error: JsonParseException) {
                cache.marcarFallido(tarea.id, "No se pudo interpretar la solicitud guardada para sincronizar.")
                continue
            }

            if (respuesta.isSuccessful) {
                guardarRespuestaCacheada(tarea, respuesta)
                cache.marcarSincronizado(tarea.id)
            } else {
                val detalle = respuesta.errorBody()?.string().orEmpty()
                val mensaje = ApiErrorTranslator.mensaje(respuesta.code(), detalle)
                when {
                    respuesta.code() == 409 || respuesta.code() == 412 -> {
                        cache.marcarConflicto(tarea.id, mensaje)
                    }
                    respuesta.code() >= 500 -> {
                        cache.marcarPendiente(tarea.id, mensaje)
                        cache.close()
                        return Result.retry()
                    }
                    else -> cache.marcarFallido(tarea.id, mensaje)
                }
            }
        }
        cache.close()
        return Result.success()
    }

    private suspend fun enviarTarea(tarea: SyncQueueEntry): Response<*> {
        val solicitud = JsonParser.parseString(tarea.payload).asJsonObject
        return when (tarea.operation) {
            "CREAR_EMPRESA" -> api.crearEmpresa(solicitud)
            "CREAR_USUARIO" -> {
                val solicitudRemota = solicitud.deepCopy()
                val empresaLocalId = solicitud.get("empresaIdLocal")
                if (empresaLocalId != null && !empresaLocalId.isJsonNull) {
                    val contenido = cache.leerCache("empresa", empresaLocalId.asString)
                        ?: throw IOException("La empresa asociada todavía no se ha sincronizado.")
                    val empresa = JsonParser.parseString(contenido).asJsonObject
                    val idEmpresa = when {
                        empresa.has("idEmpresa") -> empresa.get("idEmpresa").asLong
                        empresa.has("id") -> empresa.get("id").asLong
                        else -> throw IOException("El servidor todavía no asignó un ID a la empresa.")
                    }
                    solicitudRemota.addProperty("empresaId", idEmpresa)
                    solicitudRemota.remove("empresaIdLocal")
                }
                api.crearUsuario(solicitudRemota)
            }
            "CREAR_INSTALACION" -> {
                val solicitudRemota = solicitud.deepCopy()
                val empresaLocalId = solicitud.get("idEmpresaLocal")
                if (empresaLocalId != null && !empresaLocalId.isJsonNull) {
                    val contenido = cache.leerCache("empresa", empresaLocalId.asString)
                        ?: throw IOException("La empresa asociada todavía no se ha sincronizado.")
                    val empresa = JsonParser.parseString(contenido).asJsonObject
                    solicitudRemota.addProperty(
                        "idEmpresa",
                        leerIdRemoto(empresa, "idEmpresa", "id")
                    )
                    solicitudRemota.remove("idEmpresaLocal")
                }
                val usuarioLocalId = solicitud.get("idUsuarioClienteLocal")
                if (usuarioLocalId != null && !usuarioLocalId.isJsonNull) {
                    val contenido = cache.leerCache("usuario", usuarioLocalId.asString)
                        ?: throw IOException("El usuario asociado todavía no se ha sincronizado.")
                    val usuario = JsonParser.parseString(contenido).asJsonObject
                    solicitudRemota.addProperty(
                        "idUsuarioCliente",
                        leerIdRemoto(usuario, "idUsuario", "id")
                    )
                    solicitudRemota.remove("idUsuarioClienteLocal")
                }
                api.crearInstalacion(solicitudRemota)
            }
            "CREAR_EQUIPO" -> api.crearEquipo(solicitud)
            "CREAR_VISITA" -> api.crearVisita(solicitud)
            "ACTUALIZAR_ASIGNACIONES" -> {
                val ids = mutableListOf<Long>()
                val idsJson = solicitud.getAsJsonArray("idsInstalacion")
                for (idJson in idsJson) {
                    ids.add(idJson.asLong)
                }
                val idTecnico = tarea.resourceId
                    ?: throw IllegalArgumentException("La asignación no tiene técnico asociado.")
                val actor = solicitud.get("asignadoPor").asLong
                api.asignarInstalaciones(idTecnico, SolicitudAsignacion(actor, ids))
            }
            "AGREGAR_EQUIPO_VISITA" -> {
                val idVisitaLocal = idVisitaLocal(tarea)
                val idVisitaRemota = resolverIdVisita(idVisitaLocal)
                api.agregarEquipoVisita(idVisitaRemota, solicitud)
            }
            "CERRAR_VISITA" -> {
                val idVisitaLocal = idVisitaLocal(tarea)
                val idVisitaRemota = resolverIdVisita(idVisitaLocal)
                api.cerrarVisitaRemota(idVisitaRemota)
            }
            "CREAR_PROTOCOLO" -> enviarProtocolo(solicitud)
            "CREAR_INSTRUMENTO" -> api.crearInstrumento(solicitud)
            "CAMBIAR_ESTADO_INSTRUMENTO" -> {
                val idInstrumento = tarea.resourceId
                    ?: throw IllegalArgumentException("La actualización no tiene instrumento asociado.")
                val activo = solicitud.get("activo").asBoolean
                api.cambiarEstadoInstrumento(idInstrumento, activo)
            }
            "CREAR_CALIBRACION" -> {
                val idInstrumento = tarea.resourceId
                    ?: throw IllegalArgumentException("La calibración no tiene instrumento asociado.")
                api.registrarCalibracion(idInstrumento, solicitud)
            }
            else -> throw IllegalArgumentException(
                "Tipo de sincronización no reconocido: ${tarea.operation}"
            )
        }
    }

    private fun guardarRespuestaCacheada(tarea: SyncQueueEntry, respuesta: Response<*>) {
        val cuerpo = respuesta.body() ?: return
        val claveCache = if (tarea.resourceType == "visita" ||
            tarea.resourceType == "empresa" ||
            tarea.resourceType == "usuario"
        ) {
            tarea.resourceId?.toString() ?: tarea.id
        } else {
            tarea.id
        }
        cache.guardarCache(tarea.resourceType, claveCache, gson.toJson(cuerpo))
    }

    private fun leerIdRemoto(
        respuesta: JsonObject,
        clavePrincipal: String,
        claveAlternativa: String
    ): Long {
        if (respuesta.has(clavePrincipal) && !respuesta.get(clavePrincipal).isJsonNull) {
            return respuesta.get(clavePrincipal).asLong
        }
        if (respuesta.has(claveAlternativa) && !respuesta.get(claveAlternativa).isJsonNull) {
            return respuesta.get(claveAlternativa).asLong
        }
        throw IOException("El servidor no devolvió el identificador del registro.")
    }

    private suspend fun enviarProtocolo(solicitud: JsonObject): Response<*> {
        val protocolo = gson.fromJson(solicitud, ProtocoloPendienteRequest::class.java)
        val idVisita = protocolo.protocolo.idVisita
        if (idVisita == null) {
            return api.crearProtocolo(protocolo)
        }
        val idVisitaRemota = resolverIdVisita(idVisita)
        val solicitudRemota = protocolo.copy(
            protocolo = protocolo.protocolo.copy(idVisita = idVisitaRemota)
        )
        return api.crearProtocolo(solicitudRemota)
    }

    private fun idVisitaLocal(tarea: SyncQueueEntry): Long {
        return tarea.resourceId
            ?: throw IllegalArgumentException("La visita local no está asociada.")
    }

    private fun resolverIdVisita(idLocal: Long): Long {
        if (idLocal >= 0) {
            return idLocal
        }
        val contenido = cache.leerCache("visita", idLocal.toString())
            ?: throw IOException("La visita asociada todavía no se ha sincronizado.")
        val visita = JsonParser.parseString(contenido).asJsonObject
        val idRemoto = visita.get("idVisita")
        if (idRemoto == null || idRemoto.isJsonNull) {
            throw IOException("El servidor todavía no asignó un ID a la visita.")
        }
        return idRemoto.asLong
    }
}

object SyncScheduler {
    fun programar(context: Context) {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val solicitud = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(restricciones)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "induztek_sync_queue",
            ExistingWorkPolicy.APPEND,
            solicitud
        )
    }

    fun programarPeriodico(context: Context) {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val solicitud = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(restricciones)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "induztek_sync_periodica",
            ExistingPeriodicWorkPolicy.KEEP,
            solicitud
        )
    }
}

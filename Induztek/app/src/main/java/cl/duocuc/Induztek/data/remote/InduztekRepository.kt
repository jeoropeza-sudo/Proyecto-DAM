package cl.duocuc.Induztek.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import retrofit2.Response

data class RemoteWriteResult(
    val payload: String,
    val enCola: Boolean
)

class ApiRequestException(
    val statusCode: Int,
    override val message: String
) : IOException(message)

class InduztekRepository(context: Context) {
    private val context = context.applicationContext
    val api = ApiClient.crear(context)
    private val cache = ApiCacheDbHelper(this.context)
    private val gson = Gson()

    suspend fun iniciarSesion(correo: String, contrasena: String): LoginResponse {
        val response = api.iniciarSesion(LoginRequest(correo.trim(), contrasena))
        return exigirCuerpo(response)
    }

    suspend fun usuario(id: Long): UsuarioResponse {
        val response = api.usuario(id)
        return exigirCuerpo(response)
    }

    suspend fun solicitarCodigo(correo: String) {
        val response = api.solicitarCodigo(SolicitudCorreoRecuperacion(correo.trim()))
        exigirExito(response)
    }

    suspend fun restablecerClave(correo: String, codigo: String, clave: String) {
        val response = api.restablecerClave(
            SolicitudRestablecimiento(correo.trim(), codigo.trim(), clave)
        )
        exigirExito(response)
    }

    suspend fun empresas(): List<JsonObject> {
        return listarEnCache("empresa") { api.empresas() }
    }

    suspend fun instalaciones(): List<JsonObject> {
        return listarEnCache("instalacion") { api.instalaciones() }
    }

    suspend fun instalacionesEmpresa(idEmpresa: Long): List<JsonObject> {
        return listarEnCache("instalacion_empresa_$idEmpresa") {
            api.instalacionesEmpresa(idEmpresa)
        }
    }

    suspend fun instalacionesCliente(idCliente: Long): List<JsonObject> {
        return listarEnCache("instalacion_cliente_$idCliente") {
            api.instalacionesCliente(idCliente)
        }
    }

    suspend fun instalacionesTecnico(idTecnico: Long): List<JsonObject> {
        return listarEnCache("instalacion_tecnico_$idTecnico") {
            api.instalacionesTecnico(idTecnico)
        }
    }

    suspend fun equiposInstalacion(idInstalacion: Long): List<JsonObject> {
        return listarEnCache("equipo_instalacion_$idInstalacion") {
            api.equiposInstalacion(idInstalacion)
        }
    }

    suspend fun equiposCliente(idCliente: Long): List<JsonObject> {
        return listarEnCache("equipo_cliente_$idCliente") {
            api.equiposCliente(idCliente)
        }
    }

    suspend fun equiposTecnico(idTecnico: Long): List<JsonObject> {
        return listarEnCache("equipo_tecnico_$idTecnico") {
            api.equiposTecnico(idTecnico)
        }
    }

    suspend fun asignacionesTecnico(idTecnico: Long): List<JsonObject> {
        return listarEnCache("asignacion_tecnico_$idTecnico") {
            api.asignacionesTecnico(idTecnico)
        }
    }

    suspend fun visitas(idInstalacion: Long?, idTecnico: Long?): List<JsonObject> {
        val claveCache = "visitas_${idInstalacion ?: 0}_${idTecnico ?: 0}"
        return listarEnCache(claveCache) {
            api.visitas(instalacion = idInstalacion, tecnico = idTecnico)
        }
    }

    suspend fun protocolosVisita(idVisita: Long): List<JsonObject> {
        return listarEnCache("protocolos_visita_$idVisita") {
            api.protocolosVisita(idVisita)
        }
    }

    suspend fun protocolosCliente(idCliente: Long): List<JsonObject> {
        return listarEnCache("protocolos_cliente_$idCliente") {
            api.protocolosCliente(idCliente)
        }
    }

    suspend fun protocolosEquipo(idEquipo: Long): List<JsonObject> {
        return listarEnCache("protocolos_equipo_$idEquipo") {
            api.protocolosEquipo(idEquipo)
        }
    }

    suspend fun protocolos(): List<JsonObject> {
        return listarEnCache("protocolo") { api.protocolos() }
    }

    suspend fun protocolo(idProtocolo: Long): JsonObject {
        try {
            val respuesta = exigirCuerpo(api.protocolo(idProtocolo))
            cache.guardarCache("protocolo_detalle", idProtocolo.toString(), gson.toJson(respuesta))
            return respuesta
        } catch (error: IOException) {
            if (error is ApiRequestException) {
                throw error
            }
            val contenido = cache.leerCache("protocolo_detalle", idProtocolo.toString())
            if (contenido == null) {
                throw error
            }
            return JsonParser.parseString(contenido).asJsonObject
        }
    }

    suspend fun medicionesProtocolo(idProtocolo: Long): List<JsonObject> {
        return listarEnCache("mediciones_protocolo_$idProtocolo") {
            api.mediciones(idProtocolo)
        }
    }

    suspend fun instrumentos(tipoPrueba: String? = null): List<JsonObject> {
        val tipoCache = tipoPrueba ?: "todos"
        return listarEnCache("instrumentos_$tipoCache") {
            api.instrumentos(tipoPrueba = tipoPrueba, activo = true)
        }
    }

    suspend fun crearEmpresa(
        solicitud: JsonObject,
        idLocal: Long?
    ): RemoteWriteResult {
        return guardarRemotoOCrearCola(
            "empresa",
            "CREAR_EMPRESA",
            idLocal,
            solicitud,
            { api.crearEmpresa(solicitud) }
        )
    }

    fun vincularEmpresaCacheada(idLocal: Long, empresa: JsonObject) {
        cache.guardarCache("empresa", idLocal.toString(), gson.toJson(empresa))
    }

    fun leerEmpresaCacheada(idLocal: Long): JsonObject? {
        val contenido = cache.leerCache("empresa", idLocal.toString())
        if (contenido == null) {
            return null
        }
        return JsonParser.parseString(contenido).asJsonObject
    }

    suspend fun crearUsuario(
        solicitud: JsonObject,
        idEmpresaLocal: Long?,
        idUsuarioLocal: Long
    ): RemoteWriteResult {
        if (idEmpresaLocal == null) {
            throw ApiRequestException(
                400,
                "La cuenta no tiene una empresa asociada; el servidor requiere una empresa para crear usuarios."
            )
        }
        val empresaCacheada = cache.leerCache("empresa", idEmpresaLocal.toString())
        if (empresaCacheada == null) {
            val solicitudEnCola = solicitud.deepCopy()
            solicitudEnCola.addProperty("empresaIdLocal", idEmpresaLocal)
            val contenido = gson.toJson(solicitudEnCola)
            cache.encolar("usuario", "CREAR_USUARIO", idUsuarioLocal, contenido)
            SyncScheduler.programar(context)
            return RemoteWriteResult(contenido, true)
        }
        val empresa = JsonParser.parseString(empresaCacheada).asJsonObject
        val idRemoto = when {
            empresa.has("idEmpresa") -> empresa.get("idEmpresa").asLong
            empresa.has("id") -> empresa.get("id").asLong
            else -> throw ApiRequestException(502, "La empresa no devolvió un identificador remoto.")
        }
        val solicitudRemota = solicitud.deepCopy()
        solicitudRemota.addProperty("empresaId", idRemoto)
        val solicitudEnCola = solicitud.deepCopy()
        solicitudEnCola.addProperty("empresaIdLocal", idEmpresaLocal)
        return guardarRemotoOCrearCola(
            "usuario",
            "CREAR_USUARIO",
            idUsuarioLocal,
            solicitudEnCola,
            { api.crearUsuario(solicitudRemota) }
        )
    }

    suspend fun crearInstalacion(
        solicitud: JsonObject,
        idTemporal: Long? = null
    ): RemoteWriteResult {
        val solicitudEnCola = solicitud.deepCopy()
        val solicitudRemota = solicitud.deepCopy()
        val empresaLocalId = solicitud.get("idEmpresaLocal")
        val usuarioLocalId = solicitud.get("idUsuarioClienteLocal")
        var debeEncolar = false

        if (empresaLocalId != null && !empresaLocalId.isJsonNull) {
            val empresa = cache.leerCache("empresa", empresaLocalId.asString)
            if (empresa == null) {
                debeEncolar = true
            } else {
                val jsonEmpresa = JsonParser.parseString(empresa).asJsonObject
                val idEmpresaRemota = leerIdRemoto(jsonEmpresa, "idEmpresa", "id")
                solicitudRemota.addProperty("idEmpresa", idEmpresaRemota)
            }
        }
        solicitudRemota.remove("idEmpresaLocal")

        if (usuarioLocalId != null && !usuarioLocalId.isJsonNull) {
            val usuario = cache.leerCache("usuario", usuarioLocalId.asString)
            if (usuario == null) {
                debeEncolar = true
            } else {
                val jsonUsuario = JsonParser.parseString(usuario).asJsonObject
                val idUsuarioRemoto = leerIdRemoto(jsonUsuario, "idUsuario", "id")
                solicitudRemota.addProperty("idUsuarioCliente", idUsuarioRemoto)
            }
        }
        solicitudRemota.remove("idUsuarioClienteLocal")

        if (debeEncolar) {
            val contenido = gson.toJson(solicitudEnCola)
            cache.encolar("instalacion", "CREAR_INSTALACION", idTemporal, contenido)
            SyncScheduler.programar(context)
            return RemoteWriteResult(contenido, true)
        }

        return guardarRemotoOCrearCola(
            "instalacion",
            "CREAR_INSTALACION",
            idTemporal,
            solicitudEnCola,
            { api.crearInstalacion(solicitudRemota) }
        )
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
        throw ApiRequestException(502, "El servidor no devolvió el identificador del registro.")
    }

    suspend fun crearEquipo(
        solicitud: JsonObject,
        idTemporal: Long? = null
    ): RemoteWriteResult {
        return guardarRemotoOCrearCola(
            "equipo",
            "CREAR_EQUIPO",
            idTemporal,
            solicitud,
            { api.crearEquipo(solicitud) }
        )
    }

    suspend fun actualizarAsignaciones(
        idTecnico: Long,
        idActor: Long,
        idsInstalaciones: List<Long>
    ): RemoteWriteResult {
        val solicitud = SolicitudAsignacion(idActor, idsInstalaciones)
        return guardarRemotoOCrearCola(
            "asignacion",
            "ACTUALIZAR_ASIGNACIONES",
            idTecnico,
            JsonParser.parseString(gson.toJson(solicitud)).asJsonObject,
            { api.asignarInstalaciones(idTecnico, solicitud) }
        )
    }

    suspend fun crearVisita(
        solicitud: JsonObject,
        idTemporal: Long? = null
    ): RemoteWriteResult {
        return guardarRemotoOCrearCola(
            "visita",
            "CREAR_VISITA",
            idTemporal,
            solicitud,
            { api.crearVisita(solicitud) }
        )
    }

    suspend fun agregarEquipoVisita(
        idVisita: Long,
        idEquipo: Long,
        orden: Int
    ): RemoteWriteResult {
        val solicitud = JsonObject()
        solicitud.addProperty("idEquipo", idEquipo)
        solicitud.addProperty("orden", orden)
        val visitaRemota = resolverIdVisita(idVisita)
        if (visitaRemota == null) {
            val contenido = gson.toJson(solicitud)
            cache.encolar("visita_equipo", "AGREGAR_EQUIPO_VISITA", idVisita, contenido)
            SyncScheduler.programar(context)
            return RemoteWriteResult(contenido, true)
        }
        return guardarRemotoOCrearCola(
            "visita_equipo",
            "AGREGAR_EQUIPO_VISITA",
            idVisita,
            solicitud,
            { api.agregarEquipoVisita(visitaRemota, solicitud) }
        )
    }

    suspend fun cerrarVisita(idVisita: Long): RemoteWriteResult {
        val visitaRemota = resolverIdVisita(idVisita)
        if (visitaRemota == null) {
            val solicitud = JsonObject()
            cache.encolar("visita", "CERRAR_VISITA", idVisita, gson.toJson(solicitud))
            SyncScheduler.programar(context)
            return RemoteWriteResult(gson.toJson(solicitud), true)
        }
        try {
            val respuesta = api.cerrarVisitaRemota(visitaRemota)
            val cuerpo = exigirCuerpo(respuesta)
            val contenido = gson.toJson(cuerpo)
            cache.guardarCache("visita", idVisita.toString(), contenido)
            return RemoteWriteResult(contenido, false)
        } catch (error: IOException) {
            if (error is ApiRequestException) {
                throw error
            }
            val solicitud = JsonObject()
            val contenido = gson.toJson(solicitud)
            cache.encolar("visita", "CERRAR_VISITA", idVisita, contenido)
            SyncScheduler.programar(context)
            return RemoteWriteResult(contenido, true)
        }
    }

    suspend fun crearProtocolo(solicitud: ProtocoloPendienteRequest): RemoteWriteResult {
        val json = JsonParser.parseString(gson.toJson(solicitud)).asJsonObject
        val idTemporal = solicitud.protocolo.idVisita
        val visitaRemota = if (idTemporal == null) null else resolverIdVisita(idTemporal)
        if (idTemporal != null && idTemporal < 0 && visitaRemota == null) {
            val contenido = gson.toJson(json)
            cache.encolar("protocolo", "CREAR_PROTOCOLO", idTemporal, contenido)
            SyncScheduler.programar(context)
            return RemoteWriteResult(contenido, true)
        }
        val solicitudRemota = if (visitaRemota == null) {
            solicitud
        } else {
            solicitud.copy(protocolo = solicitud.protocolo.copy(idVisita = visitaRemota))
        }
        return guardarRemotoOCrearCola(
            "protocolo",
            "CREAR_PROTOCOLO",
            idTemporal,
            json,
            { api.crearProtocolo(solicitudRemota) }
        )
    }

    suspend fun crearInstrumento(solicitud: JsonObject): RemoteWriteResult {
        return guardarRemotoOCrearCola(
            "instrumento",
            "CREAR_INSTRUMENTO",
            null,
            solicitud,
            { api.crearInstrumento(solicitud) }
        )
    }

    suspend fun cambiarEstadoInstrumento(
        id: Long,
        activo: Boolean
    ): RemoteWriteResult {
        val solicitud = JsonObject()
        solicitud.addProperty("activo", activo)
        return guardarRemotoOCrearCola(
            "instrumento",
            "CAMBIAR_ESTADO_INSTRUMENTO",
            id,
            solicitud,
            { api.cambiarEstadoInstrumento(id, activo) }
        )
    }

    suspend fun registrarCalibracion(
        id: Long,
        solicitud: JsonObject
    ): RemoteWriteResult {
        return guardarRemotoOCrearCola(
            "calibracion",
            "CREAR_CALIBRACION",
            id,
            solicitud,
            { api.registrarCalibracion(id, solicitud) }
        )
    }

    fun conflictosPendientes(): List<SyncQueueEntry> {
        return cache.conflictos()
    }

    fun tareasDeSincronizacion(): List<SyncQueueEntry> {
        return cache.tareasDeSincronizacion()
    }

    fun resolverConflicto(id: String, mantenerLocal: Boolean) {
        cache.resolverConflicto(id, mantenerLocal)
        SyncScheduler.programar(context)
    }

    fun cerrar() {
        cache.close()
    }

    private suspend fun guardarRemotoOCrearCola(
        tipo: String,
        operacion: String,
        id: Long?,
        solicitud: JsonObject,
        llamada: suspend () -> Response<out Any>
    ): RemoteWriteResult {
        val jsonSolicitud = gson.toJson(solicitud)
        try {
            val response = llamada()
            val body = exigirCuerpo(response)
            val contenido = gson.toJson(body)
            cache.guardarCache(tipo, id?.toString() ?: "nuevo_${System.currentTimeMillis()}", contenido)
            return RemoteWriteResult(contenido, false)
        } catch (error: IOException) {
            if (error is ApiRequestException) {
                throw error
            }
            cache.encolar(tipo, operacion, id, jsonSolicitud)
            SyncScheduler.programar(context)
            return RemoteWriteResult(jsonSolicitud, true)
        }
    }

    private fun resolverIdVisita(idLocal: Long): Long? {
        if (idLocal >= 0) {
            return idLocal
        }
        val visitaCacheada = cache.leerCache("visita", idLocal.toString()) ?: return null
        val visita = JsonParser.parseString(visitaCacheada).asJsonObject
        val idRemoto = visita.get("idVisita")
        if (idRemoto == null || idRemoto.isJsonNull) {
            return null
        }
        return idRemoto.asLong
    }

    private suspend fun listarEnCache(
        tipoCache: String,
        llamada: suspend () -> Response<List<JsonObject>>
    ): List<JsonObject> {
        try {
            val respuesta = llamada()
            val lista = exigirCuerpo(respuesta)
            for (registro in lista) {
                val id = when {
                    registro.has("id") -> registro.get("id").asString
                    registro.has("idEmpresa") -> registro.get("idEmpresa").asString
                    registro.has("idInstalacion") -> registro.get("idInstalacion").asString
                    registro.has("idEquipo") -> registro.get("idEquipo").asString
                    registro.has("idInstrumento") -> registro.get("idInstrumento").asString
                    registro.has("idVisita") -> registro.get("idVisita").asString
                    registro.has("idProtocolo") -> registro.get("idProtocolo").asString
                    else -> registro.hashCode().toString()
                }
                cache.guardarCache(tipoCache, id, gson.toJson(registro))
            }
            return lista
        } catch (error: IOException) {
            if (error is ApiRequestException) {
                throw error
            }
            val respuestasCacheadas = cache.leerCache(tipoCache)
            if (respuestasCacheadas.isEmpty()) {
                throw error
            }
            val lista = mutableListOf<JsonObject>()
            for (contenido in respuestasCacheadas) {
                val json = JsonParser.parseString(contenido)
                if (json.isJsonObject) {
                    lista.add(json.asJsonObject)
                }
            }
            return lista
        }
    }

    private fun <T> exigirCuerpo(response: Response<T>): T {
        exigirExito(response)
        val body = response.body()
        if (body == null) {
            throw ApiRequestException(response.code(), "El servidor devolvió una respuesta vacía.")
        }
        return body
    }

    private fun exigirExito(response: Response<*>) {
        if (!response.isSuccessful) {
            throw ApiRequestException(response.code(), ApiErrorTranslator.mensaje(response))
        }
    }
}

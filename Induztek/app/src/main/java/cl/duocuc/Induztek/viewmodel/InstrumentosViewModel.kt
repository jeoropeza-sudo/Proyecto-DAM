package cl.duocuc.Induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.Induztek.data.remote.ApiRequestException
import cl.duocuc.Induztek.data.remote.InduztekRepository
import cl.duocuc.Induztek.data.remote.SyncQueueEntry
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InstrumentoAdministrativo(
    val id: Long,
    val nombre: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val tipoPrueba: String,
    val activo: Boolean,
    val bloqueadoPorVencimiento: Boolean
)

data class InstrumentosUiState(
    val instrumentos: List<InstrumentoAdministrativo> = emptyList(),
    val conflictos: List<SyncQueueEntry> = emptyList(),
    val cargando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class InstrumentosViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = InduztekRepository(application)
    private val _estado = MutableStateFlow(InstrumentosUiState())
    val estado = _estado.asStateFlow()

    init {
        cargarInstrumentos()
    }

    fun cargarInstrumentos() {
        _estado.value = _estado.value.copy(
            cargando = true,
            error = null,
            conflictos = repositorio.conflictosPendientes()
        )
        viewModelScope.launch {
            try {
                val respuestas = repositorio.instrumentos(tipoPrueba = null)
                val instrumentos = mutableListOf<InstrumentoAdministrativo>()
                for (respuesta in respuestas) {
                    instrumentos.add(
                        InstrumentoAdministrativo(
                            id = leerLong(respuesta, "idInstrumento"),
                            nombre = leerTexto(respuesta, "nombre"),
                            marca = leerTexto(respuesta, "marca"),
                            modelo = leerTexto(respuesta, "modelo"),
                            numeroSerie = leerTexto(respuesta, "numeroSerie"),
                            tipoPrueba = leerTexto(respuesta, "tipoPrueba"),
                            activo = leerBooleano(respuesta, "activo"),
                            bloqueadoPorVencimiento = leerBooleano(respuesta, "bloqueadoPorVencimiento")
                        )
                    )
                }
                _estado.value = _estado.value.copy(instrumentos = instrumentos, cargando = false)
            } catch (error: ApiRequestException) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = error.message,
                    conflictos = repositorio.conflictosPendientes()
                )
            } catch (_: java.io.IOException) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = "No se pudo cargar el inventario de instrumentos.",
                    conflictos = repositorio.conflictosPendientes()
                )
            }
        }
    }

    fun crearInstrumento(
        nombre: String,
        marca: String,
        modelo: String,
        numeroSerie: String,
        tipoPrueba: String
    ) {
        val solicitud = JsonObject()
        solicitud.addProperty("nombre", nombre.trim())
        solicitud.addProperty("marca", marca.trim())
        solicitud.addProperty("modelo", modelo.trim())
        solicitud.addProperty("numeroSerie", numeroSerie.trim())
        solicitud.addProperty("tipoPrueba", tipoPrueba)
        solicitud.addProperty("activo", true)
        cambiarEstadoCargando()
        viewModelScope.launch {
            try {
                val resultado = repositorio.crearInstrumento(solicitud)
                _estado.value = _estado.value.copy(
                    cargando = false,
                    mensaje = if (resultado.enCola) {
                        "Instrumento guardado en la cola y se sincronizará cuando vuelva la conexión."
                    } else {
                        "Instrumento creado."
                    },
                    error = null
                )
                if (!resultado.enCola) {
                    cargarInstrumentos()
                } else {
                    refrescarConflictos()
                }
            } catch (error: ApiRequestException) {
                _estado.value = _estado.value.copy(cargando = false, error = error.message)
            } catch (_: java.io.IOException) {
                _estado.value = _estado.value.copy(cargando = false, error = "No se pudo guardar el instrumento.")
            }
        }
    }

    fun cambiarEstado(instrumento: InstrumentoAdministrativo) {
        cambiarEstadoCargando()
        viewModelScope.launch {
            try {
                val resultado = repositorio.cambiarEstadoInstrumento(
                    instrumento.id,
                    !instrumento.activo
                )
                _estado.value = _estado.value.copy(
                    cargando = false,
                    mensaje = if (resultado.enCola) {
                        "Cambio de estado pendiente de sincronización."
                    } else {
                        "Estado del instrumento actualizado."
                    },
                    error = null
                )
                if (!resultado.enCola) {
                    cargarInstrumentos()
                } else {
                    refrescarConflictos()
                }
            } catch (error: ApiRequestException) {
                _estado.value = _estado.value.copy(cargando = false, error = error.message)
            } catch (_: java.io.IOException) {
                _estado.value = _estado.value.copy(cargando = false, error = "No se pudo actualizar el instrumento.")
            }
        }
    }

    fun registrarCalibracion(
        instrumentoId: Long,
        fechaCalibracion: String,
        fechaVencimiento: String,
        laboratorio: String,
        certificado: String
    ) {
        val solicitud = JsonObject()
        solicitud.addProperty("fechaCalibracion", fechaCalibracion)
        solicitud.addProperty("fechaVencimiento", fechaVencimiento)
        solicitud.addProperty("laboratorio", laboratorio.trim())
        solicitud.addProperty("certificado", certificado.trim())
        cambiarEstadoCargando()
        viewModelScope.launch {
            try {
                val resultado = repositorio.registrarCalibracion(instrumentoId, solicitud)
                _estado.value = _estado.value.copy(
                    cargando = false,
                    mensaje = if (resultado.enCola) {
                        "Calibración en cola para sincronizar."
                    } else {
                        "Calibración registrada."
                    },
                    error = null
                )
                if (!resultado.enCola) {
                    cargarInstrumentos()
                } else {
                    refrescarConflictos()
                }
            } catch (error: ApiRequestException) {
                _estado.value = _estado.value.copy(cargando = false, error = error.message)
            } catch (_: java.io.IOException) {
                _estado.value = _estado.value.copy(cargando = false, error = "No se pudo guardar la calibración.")
            }
        }
    }

    private fun cambiarEstadoCargando() {
        _estado.value = _estado.value.copy(cargando = true, error = null, mensaje = null)
    }

    fun resolverConflicto(conflictoId: String, mantenerLocal: Boolean) {
        repositorio.resolverConflicto(conflictoId, mantenerLocal)
        refrescarConflictos()
        val mensaje = if (mantenerLocal) {
            "El cambio local se volverá a intentar."
        } else {
            "Se descartó el cambio local; se conservará el registro del servidor."
        }
        _estado.value = _estado.value.copy(mensaje = mensaje)
    }

    private fun refrescarConflictos() {
        _estado.value = _estado.value.copy(conflictos = repositorio.conflictosPendientes())
    }

    private fun leerTexto(json: JsonObject, clave: String): String {
        val valor = json.get(clave)
        if (valor == null || valor.isJsonNull) {
            return ""
        }
        return valor.asString
    }

    private fun leerLong(json: JsonObject, clave: String): Long {
        val valor = json.get(clave)
        if (valor == null || valor.isJsonNull) {
            return 0L
        }
        return valor.asLong
    }

    private fun leerBooleano(json: JsonObject, clave: String): Boolean {
        val valor = json.get(clave)
        if (valor == null || valor.isJsonNull) {
            return false
        }
        return valor.asBoolean
    }

    override fun onCleared() {
        repositorio.cerrar()
        super.onCleared()
    }
}

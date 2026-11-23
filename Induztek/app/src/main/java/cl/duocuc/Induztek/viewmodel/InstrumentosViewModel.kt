package cl.duocuc.Induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.Induztek.data.remote.ApiRequestException
import cl.duocuc.Induztek.data.remote.RetrofitClient
import cl.duocuc.Induztek.data.remote.exigirCuerpo
import cl.duocuc.Induztek.data.remote.exigirExito
import com.google.gson.JsonObject
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
    val cargando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class InstrumentosViewModel(application: Application) : AndroidViewModel(application) {
    private val api = RetrofitClient.crear(application).instrumentos
    private val _estado = MutableStateFlow(InstrumentosUiState())
    val estado = _estado.asStateFlow()

    init {
        cargarInstrumentos()
    }

    fun cargarInstrumentos() {
        _estado.value = _estado.value.copy(
            cargando = true,
            error = null
        )
        viewModelScope.launch {
            try {
                val respuestas = api.instrumentos(tipoPrueba = null, activo = true).exigirCuerpo()
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
                    error = error.message
                )
            } catch (_: java.io.IOException) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = "No se pudo cargar el inventario de instrumentos."
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
                api.crearInstrumento(solicitud).exigirExito()
                _estado.value = _estado.value.copy(
                    cargando = false,
                    mensaje = "Instrumento creado.",
                    error = null
                )
                cargarInstrumentos()
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
                api.cambiarEstadoInstrumento(
                    instrumento.id,
                    !instrumento.activo
                ).exigirExito()
                _estado.value = _estado.value.copy(
                    cargando = false,
                    mensaje = "Estado del instrumento actualizado.",
                    error = null
                )
                cargarInstrumentos()
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
                api.registrarCalibracion(instrumentoId, solicitud).exigirExito()
                _estado.value = _estado.value.copy(
                    cargando = false,
                    mensaje = "Calibración registrada.",
                    error = null
                )
                cargarInstrumentos()
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
}

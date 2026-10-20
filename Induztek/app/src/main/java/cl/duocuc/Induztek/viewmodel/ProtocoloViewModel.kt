package cl.duocuc.Induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.Induztek.data.InduztekDbHelper
import cl.duocuc.Induztek.model.DevanadoOption
import cl.duocuc.Induztek.model.FormularioProtocoloUiState
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.model.TipoEquipoOption
import cl.duocuc.Induztek.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FormularioProtocoloViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InduztekDbHelper(application)
    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _uiState = MutableStateFlow(FormularioProtocoloUiState())
    val uiState: StateFlow<FormularioProtocoloUiState> = _uiState.asStateFlow()

    private val _historial = MutableStateFlow<List<ProtocoloTerreno>>(emptyList())
    val historial: StateFlow<List<ProtocoloTerreno>> = _historial.asStateFlow()

    init {
        recargarHistorial(1L)
    }

    private fun recargarHistorial(idEquipo: Long) {
        try {
            _historial.value = db.listarProtocolosPorEquipo(idEquipo)
        } catch (e: Exception) {
            e.printStackTrace()
            _historial.value = emptyList()
        }
    }

    fun onCorreoLoginCambia(valor: String) {
        _uiState.update { it.copy(correoLogin = valor.trim(), errorLogin = null) }
    }

    fun onClaveLoginCambia(valor: String) {
        _uiState.update { it.copy(claveLogin = valor, errorLogin = null) }
    }

    fun onAlternarClaveVisible() {
        _uiState.update { it.copy(claveVisible = !it.claveVisible) }
    }

    fun iniciarSesion(correo: String, clave: String): Usuario? {
        val usuario = db.autenticarUsuario(correo, clave)
        if (usuario != null) {
            _usuarioActivo.value = usuario
            _uiState.update { it.copy(errorLogin = null) }
        } else {
            _uiState.update { it.copy(errorLogin = "Credenciales incorrectas. Verifique correo y contraseña.") }
        }
        return usuario
    }

    fun cerrarSesion() {
        _usuarioActivo.value = null
        _uiState.update { it.copy(correoLogin = "", claveLogin = "", errorLogin = null) }
    }

    // --- EVENTOS DEL FORMULARIO DE TERRENO ---

    fun onTagEquipoSeleccionado(tag: String) {
        val idEquipo = when (tag) {
            "TR-01" -> 1L
            "TR-02" -> 2L
            else -> 1L
        }
        _uiState.update {
            it.copy(
                tagEquipo = tag,
                registroExitoso = false
            )
        }
        recargarHistorial(idEquipo)
    }

    fun onTipoEquipoSeleccionado(tipo: TipoEquipoOption) {
        _uiState.update { it.copy(tipoEquipo = tipo, registroExitoso = false) }
    }

    fun onDevanadoSeleccionado(devanado: DevanadoOption) {
        _uiState.update { it.copy(devanadoSeleccionado = devanado, registroExitoso = false) }
    }

    fun onTensionSeleccionada(tension: String) {
        _uiState.update { it.copy(tensionPruebaKv = tension, registroExitoso = false) }
    }

    fun onLectura1MinCambia(valor: String) {
        _uiState.update {
            it.copy(
                lectura1Min = valor.trim(),
                errorLectura1Min = null,
                registroExitoso = false
            )
        }
        calcularEvaluacionTecnica()
    }

    fun onLectura10MinCambia(valor: String) {
        _uiState.update {
            it.copy(
                lectura10Min = valor.trim(),
                errorLectura10Min = null,
                registroExitoso = false
            )
        }
        calcularEvaluacionTecnica()
    }

    fun onObservacionesCambia(valor: String) {
        _uiState.update {
            it.copy(
                observaciones = valor,
                errorObservaciones = null,
                registroExitoso = false
            )
        }
    }

    private fun calcularEvaluacionTecnica() {
        val estado = _uiState.value
        val l1 = estado.lectura1Min.toDoubleOrNull()
        val l10 = estado.lectura10Min.toDoubleOrNull()

        if (l1 != null && l10 != null && l1 > 0) {
            val ip = l10 / l1

            val resultado = when {
                l1 < 100.0 || ip < 1.0 -> "RECHAZADO"
                ip in 1.0..1.999 -> "OBSERVADO"
                else -> "APROBADO"
            }

            val idEquipo = if (estado.tagEquipo == "TR-02") 2L else 1L
            val lecturaAnterior = try {
                db.obtenerUltimaLecturaAnterior(idEquipo, "LECTURA_1MIN")
            } catch (e: Exception) {
                null
            }

            var hayVariacionSignificativa = false
            if (lecturaAnterior != null && lecturaAnterior > 0) {
                val diferenciaPct = Math.abs(l1 - lecturaAnterior) / lecturaAnterior * 100.0
                hayVariacionSignificativa = diferenciaPct > 25.0
            }

            _uiState.update {
                it.copy(
                    ipCalculado = ip,
                    resultadoEvaluado = resultado,
                    variacionDetectada = hayVariacionSignificativa
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    ipCalculado = 0.0,
                    resultadoEvaluado = "PENDIENTE",
                    variacionDetectada = false
                )
            }
        }
    }

    fun onRegistrarProtocolo(): Boolean {
        val estado = _uiState.value

        val l1 = estado.lectura1Min.toDoubleOrNull()
        val l10 = estado.lectura10Min.toDoubleOrNull()

        val errorL1 = if (l1 == null || l1 <= 0) "Ingresa un valor numérico válido (> 0)" else null
        val errorL10 = if (l10 == null || l10 <= 0) "Ingresa un valor numérico válido (> 0)" else null

        val hayErrores = listOf(errorL1, errorL10).any { it != null }

        if (hayErrores) {
            _uiState.update {
                it.copy(
                    errorLectura1Min = errorL1,
                    errorLectura10Min = errorL10,
                    registroExitoso = false
                )
            }
            return false
        }

        val idEquipo = if (estado.tagEquipo == "TR-02") 2L else 1L
        val idUsuarioActivo = _usuarioActivo.value?.idUsuario ?: 1L

        val idInsertado = try {
            db.insertarProtocoloCompleto(
                idEquipo = idEquipo,
                l1Min = l1!!,
                l10Min = l10!!,
                resultado = estado.resultadoEvaluado,
                observaciones = estado.observaciones,
                idUsuarioRegistra = idUsuarioActivo
            )
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }

        val exito = idInsertado > 0

        _uiState.update {
            it.copy(
                errorLectura1Min = null,
                errorLectura10Min = null,
                registroExitoso = exito
            )
        }

        if (exito) {
            recargarHistorial(idEquipo)
        }

        return exito
    }

    fun onLimpiar() {
        _uiState.value = FormularioProtocoloUiState()
    }

    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}
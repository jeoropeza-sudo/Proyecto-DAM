package cl.duocuc.Induztek.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ProtocoloViewModelTest {

    private fun crearViewModel(): ProtocoloViewModel {
        val aplicacion = ApplicationProvider.getApplicationContext<Application>()
        return ProtocoloViewModel(aplicacion)
    }

    @Test
    fun validarDatosVisitaRechazaTemperaturaYHumedadVacias() {
        val viewModel = crearViewModel()

        val valido = viewModel.validarDatosVisita()

        assertFalse(valido)
        assertTrue(viewModel.uiState.value.mensajeErrorTemperatura != null)
        assertTrue(viewModel.uiState.value.mensajeErrorHumedad != null)
    }

    @Test
    fun validarDatosVisitaAceptaDecimalesConComa() {
        val viewModel = crearViewModel()
        viewModel.onTempAmbienteCambia("20,5")
        viewModel.onHumedadPctCambia("50")

        val valido = viewModel.validarDatosVisita()

        assertTrue(valido)
        assertEquals(null, viewModel.uiState.value.mensajeErrorTemperatura)
        assertEquals(null, viewModel.uiState.value.mensajeErrorHumedad)
    }

    @Test
    fun medicionTtrCalculaErrorYResultado() {
        val viewModel = crearViewModel()

        viewModel.onTtrMedidaCambia(0, "30,2")

        val medicion = viewModel.uiState.value.ttrFilas[0]
        assertEquals(0.6666666666666667, medicion.errorPct, 0.0001)
        assertEquals("OBSERVADO", medicion.resultado)
    }

    @Test
    fun medicionTtrDentroDeToleranciaSeAprueba() {
        val viewModel = crearViewModel()

        viewModel.onTtrMedidaCambia(0, "30")

        val medicion = viewModel.uiState.value.ttrFilas[0]
        assertEquals(0.0, medicion.errorPct, 0.0001)
        assertEquals("APROBADO", medicion.resultado)
    }

    @Test
    fun medicionTtrIgnoraUnIndiceFueraDeRango() {
        val viewModel = crearViewModel()
        val medicionesAntes = viewModel.uiState.value.ttrFilas

        viewModel.onTtrMedidaCambia(10, "30")

        assertEquals(medicionesAntes, viewModel.uiState.value.ttrFilas)
    }
}

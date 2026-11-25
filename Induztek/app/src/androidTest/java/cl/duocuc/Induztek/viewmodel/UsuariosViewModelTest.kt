package cl.duocuc.Induztek.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class UsuariosViewModelTest {

    private fun crearViewModel(): UsuariosViewModel {
        val aplicacion = ApplicationProvider.getApplicationContext<Application>()
        return UsuariosViewModel(aplicacion)
    }

    @Test
    fun iniciarSesionConCorreoVacioMuestraError() {
        val viewModel = crearViewModel()

        viewModel.iniciarSesion(" ", "clave-segura")

        assertEquals("Ingresa el correo electrónico de tu cuenta.", viewModel.errorAuth.value)
        assertFalse(viewModel.autenticando.value)
    }

    @Test
    fun iniciarSesionSinClaveMuestraError() {
        val viewModel = crearViewModel()

        viewModel.iniciarSesion("persona@ejemplo.cl", "")

        assertEquals("Ingresa la contraseña de tu cuenta.", viewModel.errorAuth.value)
        assertFalse(viewModel.autenticando.value)
    }

    @Test
    fun solicitarRecuperacionSinCorreoMuestraErrorSinCargar() {
        val viewModel = crearViewModel()

        viewModel.solicitarCodigoRecuperacion("")

        assertEquals("Ingresa tu correo electrónico.", viewModel.recuperacion.value.error)
        assertFalse(viewModel.recuperacion.value.cargando)
        assertFalse(viewModel.recuperacion.value.codigoSolicitado)
    }

    @Test
    fun restablecerClaveSinCodigoMuestraErrorSinCargar() {
        val viewModel = crearViewModel()

        viewModel.restablecerClaveConCodigo("persona@ejemplo.cl", " ", "ClaveSegura123!")

        assertEquals("Ingresa el código enviado a tu correo.", viewModel.recuperacion.value.error)
        assertFalse(viewModel.recuperacion.value.cargando)
    }

    @Test
    fun limpiarRecuperacionEliminaEstadoDeError() {
        val viewModel = crearViewModel()
        viewModel.solicitarCodigoRecuperacion("")

        viewModel.limpiarRecuperacion()

        assertNull(viewModel.recuperacion.value.error)
        assertNull(viewModel.recuperacion.value.mensaje)
        assertFalse(viewModel.recuperacion.value.cargando)
        assertFalse(viewModel.recuperacion.value.codigoSolicitado)
    }
}

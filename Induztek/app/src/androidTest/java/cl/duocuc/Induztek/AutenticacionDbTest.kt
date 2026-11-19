package cl.duocuc.Induztek

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duocuc.Induztek.data.InduztekDbHelper
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutenticacionDbTest {
    private val nombreBaseDatosPrueba = "induztek-test.db"
    private lateinit var context: Context
    private lateinit var db: InduztekDbHelper

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(nombreBaseDatosPrueba)
        db = InduztekDbHelper(context, nombreBaseDatosPrueba)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(nombreBaseDatosPrueba)
    }

    @Test
    fun authenticatesSeededExampleAccount() {
        val usuario = db.autenticarUsuario("mgodoy@induztek.ejemplo", "1234")

        assertNotNull(usuario)
        assertEquals("TECNICO", usuario?.rolCodigo)
    }

    @Test
    fun registersAndAuthenticatesCustomerAccount() {
        val idUsuario = db.registrarNuevoUsuario(
            rut = "12.345.678-5",
            nombre = "Usuario de prueba",
            correo = "cliente.test@example.com",
            telefono = "",
            clavePlana = "ClienteFuerte2026!",
            rolCodigo = "CLIENTE",
            pregunta = "Pregunta de prueba",
            respuestaPlana = "respuesta"
        )

        assertTrue(idUsuario > 0)
        val usuario = db.autenticarUsuario("cliente.test@example.com", "ClienteFuerte2026!")
        assertNotNull(usuario)
        assertEquals(idUsuario, usuario?.idUsuario)
        assertEquals("CLIENTE", usuario?.rolCodigo)
    }

    @Test
    fun passwordRecoveryRequiresTheSavedQuestionAndStrongPassword() {
        val preguntaRegistrada = "¿Ciudad donde naciste?"
        val idUsuario = db.registrarNuevoUsuario(
            rut = "12.345.678-5",
            nombre = "Usuario de prueba",
            correo = "cliente.test@example.com",
            telefono = "",
            clavePlana = "ClienteFuerte2026!",
            rolCodigo = "CLIENTE",
            pregunta = preguntaRegistrada,
            respuestaPlana = "respuesta"
        )
        assertTrue(idUsuario > 0)

        assertFalse(
            db.recuperarClaveConRespuesta(
                "cliente.test@example.com",
                "¿Nombre de tu primera mascota?",
                "respuesta",
                "Fuerte2026!Clave"
            )
        )
        assertFalse(
            db.recuperarClaveConRespuesta(
                "cliente.test@example.com",
                preguntaRegistrada,
                "respuesta",
                "corta1!"
            )
        )
        assertTrue(
            db.recuperarClaveConRespuesta(
                "cliente.test@example.com",
                preguntaRegistrada,
                "respuesta",
                "Fuerte2026!Clave"
            )
        )
        assertNotNull(db.autenticarUsuario("cliente.test@example.com", "Fuerte2026!Clave"))
    }

    @Test
    fun associatesAnUnlinkedCustomerWithAnExistingCompanyOnlyOnce() {
        val idCliente = db.registrarNuevoUsuario(
            rut = "12.345.678-5",
            nombre = "Cliente sin empresa",
            correo = "cliente.sin.empresa@example.com",
            telefono = "",
            clavePlana = "ClienteFuerte2026!",
            rolCodigo = "CLIENTE",
            pregunta = "Pregunta de prueba",
            respuestaPlana = "respuesta"
        )
        assertTrue(idCliente > 0)

        val clientesPendientes = db.listarClientesSinEmpresa()
        assertTrue(clientesPendientes.any { cliente -> cliente.idUsuario == idCliente })
        assertTrue(db.listarEmpresasParaAsociacion().isNotEmpty())

        assertFalse(db.asociarClienteAEmpresa(idCliente, 9999L))
        assertTrue(db.asociarClienteAEmpresa(idCliente, 1L))
        assertFalse(db.asociarClienteAEmpresa(idCliente, 2L))

        val clienteVinculado = db.buscarPorIdUsuario(idCliente)
        assertEquals(1L, clienteVinculado?.idEmpresaCliente)
        assertFalse(db.listarClientesSinEmpresa().any { cliente -> cliente.idUsuario == idCliente })
    }

    @Test
    fun savesAndLoadsProfilePhoto() {
        val usuario = db.autenticarUsuario("mgodoy@induztek.ejemplo", "1234")
        assertNotNull(usuario)

        val foto = byteArrayOf(1, 2, 3, 4)
        assertTrue(db.actualizarFotoUsuario(usuario!!.idUsuario, foto))

        val usuarioActualizado = db.buscarPorIdUsuario(usuario.idUsuario)
        assertNotNull(usuarioActualizado)
        assertArrayEquals(foto, usuarioActualizado?.avatar)
    }
}

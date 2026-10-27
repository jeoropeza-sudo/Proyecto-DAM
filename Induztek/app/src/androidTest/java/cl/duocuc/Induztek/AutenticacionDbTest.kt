package cl.duocuc.Induztek

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duocuc.Induztek.data.InduztekDbHelper
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
            clavePlana = "clave123",
            rolCodigo = "CLIENTE",
            pregunta = "Pregunta de prueba",
            respuestaPlana = "respuesta"
        )

        assertTrue(idUsuario > 0)
        val usuario = db.autenticarUsuario("cliente.test@example.com", "clave123")
        assertNotNull(usuario)
        assertEquals(idUsuario, usuario?.idUsuario)
        assertEquals("CLIENTE", usuario?.rolCodigo)
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

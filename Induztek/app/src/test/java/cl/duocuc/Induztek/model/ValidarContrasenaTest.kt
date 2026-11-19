package cl.duocuc.Induztek.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidarContrasenaTest {

    @Test
    fun exigeCadaCriterioDeSeguridad() {
        assertTrue(ValidarContrasena.esSegura("ClaveSegura2026!"))
        assertFalse(ValidarContrasena.esSegura("clavesegura2026!"))
        assertFalse(ValidarContrasena.esSegura("ClaveSegura2026"))
        assertFalse(ValidarContrasena.esSegura("Clave!"))
    }

    @Test
    fun calculaNivelConLosCriteriosCumplidos() {
        assertEquals(0, ValidarContrasena.nivelSeguridad(""))
        assertEquals(3, ValidarContrasena.nivelSeguridad("ClaveSegura!"))
        assertEquals(4, ValidarContrasena.nivelSeguridad("ClaveSegura2026!"))
    }
}

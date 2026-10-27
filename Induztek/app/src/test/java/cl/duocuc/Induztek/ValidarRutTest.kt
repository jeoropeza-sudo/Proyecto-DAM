package cl.duocuc.Induztek

import cl.duocuc.Induztek.util.ValidarRut
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidarRutTest {
    @Test
    fun acceptsRutWithAndWithoutFormatting() {
        assertTrue(ValidarRut.validarRut("22.133.632-1"))
        assertTrue(ValidarRut.validarRut("221336321"))
    }

    @Test
    fun acceptsKAsCheckDigit() {
        assertTrue(ValidarRut.validarRut("6-K"))
    }

    @Test
    fun rejectsIncorrectCheckDigit() {
        assertFalse(ValidarRut.validarRut("22.133.632-2"))
    }

    @Test
    fun rejectsMalformedRut() {
        assertFalse(ValidarRut.validarRut(""))
        assertFalse(ValidarRut.validarRut("22.133.63A-1"))
        assertFalse(ValidarRut.validarRut("٢٢١٣٣٦٣٢-1"))
    }
}

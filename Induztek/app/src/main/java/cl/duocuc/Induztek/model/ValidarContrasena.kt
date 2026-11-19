package cl.duocuc.Induztek.model

object ValidarContrasena {
    const val LONGITUD_MINIMA = 12

    fun esSegura(clave: String): Boolean {
        val claveNormalizada = clave.trim()
        val tieneMayuscula = claveNormalizada.any { it.isUpperCase() }
        val tieneNumero = claveNormalizada.any { it.isDigit() }
        val tieneEspecial = claveNormalizada.any { !it.isLetterOrDigit() }

        return claveNormalizada.length >= LONGITUD_MINIMA &&
            tieneMayuscula &&
            tieneNumero &&
            tieneEspecial
    }
}

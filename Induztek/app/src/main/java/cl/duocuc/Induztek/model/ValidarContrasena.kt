package cl.duocuc.Induztek.model

object ValidarContrasena {
    const val LONGITUD_MINIMA = 12

    fun esSegura(clave: String): Boolean {
        return nivelSeguridad(clave) == 4
    }

    fun nivelSeguridad(clave: String): Int {
        val claveNormalizada = clave.trim()
        var seguridad = 0
        if (claveNormalizada.length >= LONGITUD_MINIMA) seguridad++
        val tieneMayuscula = claveNormalizada.any { it.isUpperCase() }
        val tieneNumero = claveNormalizada.any { it.isDigit() }
        val tieneEspecial = claveNormalizada.any { !it.isLetterOrDigit() }

        if (tieneMayuscula) seguridad++
        if (tieneNumero) seguridad++
        if (tieneEspecial) seguridad++
        return seguridad
    }
}

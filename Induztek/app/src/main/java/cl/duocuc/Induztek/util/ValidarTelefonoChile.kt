package cl.duocuc.Induztek.util

object ValidarTelefonoChile {

    private val movilInternacional = Regex("\\+569\\d{8}")
    private val movilNacional = Regex("9\\d{8}")
    private val separadores = Regex("[\\s-]")

    fun normalizar(telefono: String): String? {
        val compacto = telefono.trim().replace(separadores, "")
        val numeroNacional = when {
            movilInternacional.matches(compacto) -> compacto.substring(3)
            movilNacional.matches(compacto) -> compacto
            else -> return null
        }
        return "+56$numeroNacional"
    }

    fun esValido(telefono: String): Boolean = normalizar(telefono) != null
}

package cl.duocuc.Induztek.model

object ValidarRut{

    fun validarRut(rut: String): Boolean {
        val limpio = rut.replace(".", "").replace("-", "").trim().uppercase()
        if (limpio.length < 2) return false

        val cuerpo = limpio.substring(0, limpio.length - 1)
        val dvIngresado = limpio.last()

        if (!cuerpo.all { it in '0'..'9' }) return false

        var suma = 0
        var multiplicador = 2
        for (i in cuerpo.indices.reversed()) {
            suma += (cuerpo[i] - '0') * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }

        val resto = 11 - (suma % 11)
        val dvEsperado = when (resto) {
            11 -> '0'
            10 -> 'K'
            else -> resto.toString()[0]
        }

        return dvIngresado == dvEsperado
    }
}
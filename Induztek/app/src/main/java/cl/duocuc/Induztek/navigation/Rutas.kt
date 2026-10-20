package cl.duocuc.Induztek.navigation

sealed class Ruta(val ruta: String) {
    data object Login : Ruta("login")
    data object Formulario : Ruta("formulario")

    data object Historial : Ruta("historial/{idEquipo}") {
        fun crear(idEquipo: Long) = "historial/$idEquipo"
    }

    data object Comprobante : Ruta("comprobante/{idProtocolo}") {
        fun crear(idProtocolo: Long) = "comprobante/$idProtocolo"
    }
}
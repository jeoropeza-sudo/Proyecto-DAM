package cl.duocuc.Induztek.navigation

sealed class Rutas(val ruta: String) {
    object Formulario : Rutas("formulario")
    object Historial : Rutas("historial/{idEquipo}") {
        fun crearRuta(idEquipo: Long) = "historial/$idEquipo"
    }
    object Comprobante : Rutas("comprobante/{idProtocolo}") {
        fun crearRuta(idProtocolo: Long) = "comprobante/$idProtocolo"
    }
}
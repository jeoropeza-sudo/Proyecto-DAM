package cl.duocuc.Induztek.navigation

sealed class Ruta(val ruta: String) {
    data object Login : Ruta("login")
    data object Registro : Ruta("registro")
    data object RecuperarClave : Ruta("recuperar_clave")
    data object Formulario : Ruta("formulario")
    data object Historial : Ruta("historial/{idEquipo}") {
        fun crear(idEquipo: Long) = "historial/$idEquipo"
    }
    data object Perfil : Ruta("perfil")
    data object Asignaciones : Ruta("asignaciones")
    data object VistaCliente : Ruta("vista_cliente")
    data object Comprobante : Ruta("comprobante/{idProtocolo}") {
        fun crear(idProtocolo: Long) = "comprobante/$idProtocolo"
    }
}
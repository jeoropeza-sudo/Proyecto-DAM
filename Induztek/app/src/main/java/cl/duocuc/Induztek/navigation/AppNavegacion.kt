package cl.duocuc.Induztek.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.ui.screens.ComprobanteTerrenoScreen
import cl.duocuc.Induztek.ui.screens.FormularioProtocoloScreen
import cl.duocuc.Induztek.ui.screens.HistorialEquipoScreen
import cl.duocuc.Induztek.ui.screens.LoginScreen
import cl.duocuc.Induztek.ui.screens.PerfilScreen
import cl.duocuc.Induztek.ui.screens.ClienteScreen
import cl.duocuc.Induztek.ui.screens.AsignacionesTecnicosScreen
import cl.duocuc.Induztek.ui.screens.RecuperarClaveScreen
import cl.duocuc.Induztek.ui.screens.RegistroScreen
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel
import cl.duocuc.Induztek.viewmodel.UsuariosViewModel

@Composable
fun AppNavegacion(
    navController: NavHostController = rememberNavController(),
    usuariosViewModel: UsuariosViewModel = viewModel(),
    protocoloViewModel: ProtocoloViewModel = viewModel()
) {
    val usuarioActivo by usuariosViewModel.usuarioActual.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    val esCliente = usuarioActivo?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)
    val esJefeTecnico = usuarioActivo?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)
    LaunchedEffect(usuarioActivo, rutaActual) {
        if (usuarioActivo != null && rutaActual == Ruta.Login.ruta) {
            val destino = if (esCliente) Ruta.VistaCliente.ruta else Ruta.Formulario.ruta
            navController.navigate(destino) {
                popUpTo(Ruta.Login.ruta) { inclusive = true }
            }
        }
    }

    val mostrarBarraInferior = usuarioActivo != null &&
            rutaActual != Ruta.Login.ruta &&
            rutaActual != Ruta.Registro.ruta &&
            rutaActual != Ruta.RecuperarClave.ruta &&
            rutaActual?.startsWith("comprobante") != true

    Scaffold(
        bottomBar = {
            if (mostrarBarraInferior) {
                NavigationBar {
                    if (!esCliente) {
                        NavigationBarItem(
                            selected = rutaActual == Ruta.Formulario.ruta,
                            onClick = {
                                navController.navigate(Ruta.Formulario.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text("Protocolo", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.List, contentDescription = "Protocolo") }
                        )

                        NavigationBarItem(
                            selected = rutaActual?.startsWith("historial") == true,
                            onClick = {
                                navController.navigate(Ruta.Historial.crear(1L)) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text("Historial", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.AccountBox, contentDescription = "Historial") }
                        )

                        if (esJefeTecnico) {
                            NavigationBarItem(
                                selected = rutaActual == Ruta.Asignaciones.ruta,
                                onClick = {
                                    navController.navigate(Ruta.Asignaciones.ruta) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                label = { Text("Asignaciones", fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Default.Assignment, contentDescription = "Asignaciones") }
                            )
                        }
                    } else {
                        NavigationBarItem(
                            selected = rutaActual == Ruta.VistaCliente.ruta,
                            onClick = {
                                navController.navigate(Ruta.VistaCliente.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text("Mi Portal", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Portal Cliente") }
                        )
                    }

                    NavigationBarItem(
                        selected = rutaActual == Ruta.Perfil.ruta,
                        onClick = {
                            navController.navigate(Ruta.Perfil.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") }
                    )
                }
            }
        }
    ) { paddingValores ->
        NavHost(
            navController = navController,
            startDestination = Ruta.Login.ruta,
            modifier = Modifier.padding(paddingValores)
        ) {
            composable(Ruta.Login.ruta) {
                LoginScreen(
                    viewModel = usuariosViewModel,
                    onLoginExitoso = {
                        val usuarioLogueado = usuariosViewModel.usuarioActual.value
                        val destino = if (usuarioLogueado?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) Ruta.VistaCliente.ruta else Ruta.Formulario.ruta
                        navController.navigate(destino) { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                    },
                    onIrARegistro = { navController.navigate(Ruta.Registro.ruta) },
                    onIrARecuperarClave = { navController.navigate(Ruta.RecuperarClave.ruta) }
                )
            }

            composable(Ruta.Registro.ruta) {
                RegistroScreen(
                    viewModel = usuariosViewModel,
                    onRegistroExitoso = { navController.navigate(Ruta.Login.ruta) { popUpTo(Ruta.Registro.ruta) { inclusive = true } } },
                    onVolverLogin = { navController.popBackStack() }
                )
            }

            composable(Ruta.RecuperarClave.ruta) {
                RecuperarClaveScreen(
                    viewModel = usuariosViewModel,
                    onClaveActualizada = { navController.navigate(Ruta.Login.ruta) { popUpTo(Ruta.RecuperarClave.ruta) { inclusive = true } } },
                    onVolverLogin = { navController.popBackStack() }
                )
            }

            composable(Ruta.Formulario.ruta) {
                FormularioProtocoloScreen(
                    onVerHistorial = { navController.navigate(Ruta.Historial.crear(1L)) },
                    onVerComprobante = { idProtocolo -> navController.navigate(Ruta.Comprobante.crear(idProtocolo)) },
                    viewModel = protocoloViewModel,
                    usuarioActivo = usuarioActivo,
                    onCerrarSesion = {
                        usuariosViewModel.cerrarSesion()
                        navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable(Ruta.VistaCliente.ruta) {
                ClienteScreen(
                    viewModel = protocoloViewModel,
                    usuarioActivo = usuarioActivo,
                    onVerComprobante = { idProtocolo -> navController.navigate(Ruta.Comprobante.crear(idProtocolo)) },
                    onVerHistorialEquipo = { idEquipo -> navController.navigate(Ruta.Historial.crear(idEquipo)) }
                )
            }

            composable(
                route = Ruta.Historial.ruta,
                arguments = listOf(navArgument("idEquipo") { type = NavType.LongType; defaultValue = 1L })
            ) { backStackEntry ->
                val idEquipo = backStackEntry.arguments?.getLong("idEquipo") ?: 0L
                HistorialEquipoScreen(
                    idEquipo = idEquipo,
                    viewModel = protocoloViewModel,
                    usuarioActivo = usuarioActivo,
                    esUsuarioCliente = esCliente,
                    onVerComprobante = { idProtocolo -> navController.navigate(Ruta.Comprobante.crear(idProtocolo)) },
                    onVolver = { navController.popBackStack() }
                )
            }

            composable(
                route = Ruta.Comprobante.ruta,
                arguments = listOf(navArgument("idProtocolo") { type = NavType.LongType })
            ) { backStackEntry ->
                val idProtocolo = backStackEntry.arguments?.getLong("idProtocolo") ?: 1L
                ComprobanteTerrenoScreen(
                    idProtocolo = idProtocolo,
                    viewModel = protocoloViewModel,
                    usuarioActivo = usuarioActivo,
                    onVolverInicio = {
                        val inicioDestino = if (esCliente) Ruta.VistaCliente.ruta else Ruta.Formulario.ruta
                        navController.navigate(inicioDestino) { popUpTo(inicioDestino) { inclusive = true } }
                    }
                )
            }

            composable(Ruta.Perfil.ruta) {
                PerfilScreen(
                    usuarioActivo = usuarioActivo,
                    onCambiarFotoCamara = { foto -> usuariosViewModel.cambiarAvatarDesdeCamara(foto) },
                    onCambiarFotoGaleria = { uri -> usuariosViewModel.cambiarAvatarDesdeGaleria(uri) },
                    onCambiarClave = { claveActual, claveNueva -> usuariosViewModel.cambiarClave(claveActual, claveNueva) },
                    onActualizarPerfil = { nombre, telefono -> usuariosViewModel.actualizarPerfil(nombre, telefono) },
                    onCerrarSesion = {
                        usuariosViewModel.cerrarSesion()
                        navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable(Ruta.Asignaciones.ruta) {
                AsignacionesTecnicosScreen(
                    viewModel = protocoloViewModel,
                    usuarioActivo = usuarioActivo
                )
            }
        }
    }
}
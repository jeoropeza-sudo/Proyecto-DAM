package cl.duocuc.Induztek.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.Induztek.ui.screens.ComprobanteTerrenoScreen
import cl.duocuc.Induztek.ui.screens.FormularioProtocoloScreen
import cl.duocuc.Induztek.ui.screens.HistorialEquipoScreen
import cl.duocuc.Induztek.ui.screens.LoginScreen
import cl.duocuc.Induztek.viewmodel.FormularioProtocoloViewModel

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    val viewModel: FormularioProtocoloViewModel = viewModel()
    val usuarioActivo by viewModel.usuarioActivo.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Ruta.Login.ruta
    ) {
        composable(Ruta.Login.ruta) {
            LoginScreen(
                viewModel = viewModel,
                onLoginExitoso = {
                    navController.navigate(Ruta.Formulario.ruta) {
                        popUpTo(Ruta.Login.ruta) { inclusive = true }
                    }
                }
            )
        }

        composable(Ruta.Formulario.ruta) {
            FormularioProtocoloScreen(
                viewModel = viewModel,
                usuarioActivo = usuarioActivo,
                onVerHistorial = { idEquipo ->
                    navController.navigate(Ruta.Historial.crear(idEquipo))
                },
                onVerComprobante = { idProtocolo ->
                    navController.navigate(Ruta.Comprobante.crear(idProtocolo))
                },
                onCerrarSesion = {
                    viewModel.cerrarSesion()
                    navController.navigate(Ruta.Login.ruta) {
                        popUpTo(Ruta.Formulario.ruta) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Ruta.Historial.ruta,
            arguments = listOf(navArgument("idEquipo") { type = NavType.LongType })
        ) { entrada ->
            val idEquipo = entrada.arguments?.getLong("idEquipo") ?: 1L
            HistorialEquipoScreen(
                idEquipo = idEquipo,
                viewModel = viewModel,
                onVolver = { navController.navigateUp() }
            )
        }

        composable(
            route = Ruta.Comprobante.ruta,
            arguments = listOf(navArgument("idProtocolo") { type = NavType.LongType })
        ) { entrada ->
            val idProtocolo = entrada.arguments?.getLong("idProtocolo") ?: 1L
            ComprobanteTerrenoScreen(
                idProtocolo = idProtocolo,
                onVolverInicio = { navController.navigateUp() }
            )
        }
    }
}
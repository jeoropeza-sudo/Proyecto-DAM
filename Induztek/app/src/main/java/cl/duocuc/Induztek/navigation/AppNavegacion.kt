package cl.duocuc.Induztek.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.Induztek.ui.screens.ComprobanteTerrenoScreen
import cl.duocuc.Induztek.ui.screens.FormularioProtocoloScreen
import cl.duocuc.Induztek.ui.screens.HistorialEquipoScreen
import cl.duocuc.Induztek.viewmodel.FormularioProtocoloViewModel

@Composable
fun AppNavegacion(
    viewModel: FormularioProtocoloViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.Formulario.ruta
    ) {
        composable(Rutas.Formulario.ruta) {
            FormularioProtocoloScreen(
                viewModel = viewModel,
                onVerHistorial = { idEquipo ->
                    navController.navigate(Rutas.Historial.crearRuta(idEquipo))
                },
                onVerComprobante = { idProtocolo ->
                    navController.navigate(Rutas.Comprobante.crearRuta(idProtocolo))
                }
            )
        }

        composable(
            route = Rutas.Historial.ruta,
            arguments = listOf(navArgument("idEquipo") { type = NavType.LongType })
        ) { backStackEntry ->
            val idEquipo = backStackEntry.arguments?.getLong("idEquipo") ?: 1L
            HistorialEquipoScreen(
                idEquipo = idEquipo,
                viewModel = viewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        composable(
            route = Rutas.Comprobante.ruta,
            arguments = listOf(navArgument("idProtocolo") { type = NavType.LongType })
        ) { backStackEntry ->
            val idProtocolo = backStackEntry.arguments?.getLong("idProtocolo") ?: 1L
            ComprobanteTerrenoScreen(
                idProtocolo = idProtocolo,
                onVolverInicio = {
                    navController.navigate(Rutas.Formulario.ruta) {
                        popUpTo(Rutas.Formulario.ruta) { inclusive = true }
                    }
                }
            )
        }
    }
}
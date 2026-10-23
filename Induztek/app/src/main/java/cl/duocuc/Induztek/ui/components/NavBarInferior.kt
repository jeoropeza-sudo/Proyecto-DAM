package cl.duocuc.Induztek.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.ui.theme.AmarilloElectricosBase

sealed class ItemNavegacion(val ruta: String, val titulo: String, val icono: ImageVector) {
    data object Formulario : ItemNavegacion("formulario", "Formulario", Icons.Default.Assignment)
    data object Historial : ItemNavegacion("historial/1", "Historial", Icons.Default.History)
    data object Perfil : ItemNavegacion("perfil", "Perfil", Icons.Default.AccountCircle)

    data object MisEquipos : ItemNavegacion("vista_cliente", "Mis Equipos", Icons.Default.ViewList)
    data object ActasCliente : ItemNavegacion("historial/1", "Actas", Icons.Default.History)
}

@Composable
fun NavBarInferior(
    rutaActual: String?,
    rolCodigo: String,
    onNavegar: (String) -> Unit
) {
    val items = if (rolCodigo == "CLIENTE") {
        listOf(
            ItemNavegacion.MisEquipos,
            ItemNavegacion.ActasCliente,
            ItemNavegacion.Perfil
        )
    } else {
        listOf(
            ItemNavegacion.Formulario,
            ItemNavegacion.Historial,
            ItemNavegacion.Perfil
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val seleccionado = rutaActual?.startsWith(item.ruta.substringBefore("/")) == true

            NavigationBarItem(
                selected = seleccionado,
                onClick = {
                    if (!seleccionado) {
                        onNavegar(item.ruta)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icono,
                        contentDescription = item.titulo
                    )
                },
                label = { Text(item.titulo) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = AmarilloElectricosBase.copy(alpha = 0.38f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.LogoInduztek
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@Composable
fun InicioScreen(
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    onAbrirPortalCliente: () -> Unit,
    onAbrirProtocolo: () -> Unit,
    onAbrirHistorial: () -> Unit,
    onAbrirAsignaciones: () -> Unit,
    onCrearCuenta: () -> Unit,
    onGestionarInstalaciones: () -> Unit,
    onGestionarSincronizacion: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val rol = usuarioActivo?.rolCodigo.orEmpty()
    val esCliente = rol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)
    val esTecnico = rol.equals(Rol.CODIGO_TECNICO, ignoreCase = true)
    val puedeAsignar = rol.equals(Rol.CODIGO_INGENIERO, ignoreCase = true) ||
            rol.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)
    val clienteSinEmpresa = esCliente && usuarioActivo?.idEmpresaCliente == null

    LaunchedEffect(usuarioActivo?.idUsuario) {
        usuarioActivo?.let(viewModel::inicializarParaUsuario)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 20.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(224.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LogoInduztek(
                        sobreFondoOscuro = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(208.dp)
                    )
                }
                Text(
                    "Hola, ${usuarioActivo?.nombre.orEmpty()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        esCliente -> "Aquí puedes consultar la información de tu empresa."
                        esTecnico -> "Aquí tienes lo necesario para comenzar tu trabajo."
                        puedeAsignar -> "Administra el trabajo y revisa el estado de los protocolos."
                        else -> "Bienvenido a Induztek."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (clienteSinEmpresa) {
            item {
                PanelInicio(
                    titulo = "Tu cuenta aún no está lista",
                    texto = "Esta cuenta no tiene una empresa registrada. Contacta a soporte para revisar los datos de tu cuenta.\n\n" +
                            "Las cuentas nuevas se crean junto con los datos de su empresa. La app no permite solicitar servicios ni crear ubicaciones.",
                    destacado = true
                )
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (esTecnico && uiState.instalacionesDisponibles.isEmpty()) {
                        PanelInicio(
                            titulo = "Aún no tienes instalaciones asignadas",
                            texto = "Pide a Ingeniería o Jefatura Técnica que te asigne una instalación. Luego podrás registrar sus protocolos."
                        )
                    } else if (esTecnico) {
                        Text(
                            "Puedes registrar protocolos en las instalaciones que te asignaron.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Accesos rápidos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val acciones = when {
                        clienteSinEmpresa -> emptyList()
                        esCliente -> listOf(
                            AccionInicio("Mi Portal", "Ver equipos y protocolos", Icons.Filled.Business, onAbrirPortalCliente)
                        )
                        esTecnico -> listOf(
                            AccionInicio(
                                "Registrar",
                                "Nuevo protocolo",
                                Icons.Filled.Description,
                                onAbrirProtocolo,
                                habilitada = uiState.instalacionesDisponibles.isNotEmpty()
                            ),
                            AccionInicio("Historial", "Consultar equipos", Icons.Filled.History, onAbrirHistorial)
                        )
                        puedeAsignar -> buildList {
                            add(AccionInicio("Registrar", "Nuevo protocolo", Icons.Filled.Description, onAbrirProtocolo))
                            add(AccionInicio("Historial", "Consultar equipos", Icons.Filled.History, onAbrirHistorial))
                            add(AccionInicio("Asignaciones", "Técnicos y equipos", Icons.Filled.Assignment, onAbrirAsignaciones))
                            add(AccionInicio("Crear cuenta", "Usuario y empresa cliente", Icons.Filled.PersonAdd, onCrearCuenta))
                            add(AccionInicio("Instalaciones", "Crear plantas o sucursales", Icons.Filled.Business, onGestionarInstalaciones))
                            add(AccionInicio("Sincronización", "Pendientes y conflictos", Icons.Filled.Sync, onGestionarSincronizacion))
                        }
                        else -> emptyList()
                    }
                    acciones.chunked(2).forEachIndexed { filaIndex, fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            fila.forEachIndexed { columnaIndex, accion ->
                                BotonAccesoInicio(
                                    accion = accion.copy(estilo = (filaIndex * 2 + columnaIndex) % 3),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (fila.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private data class AccionInicio(
    val titulo: String,
    val detalle: String,
    val icono: ImageVector,
    val onClick: () -> Unit,
    val habilitada: Boolean = true,
    val estilo: Int = 0
)

@Composable
private fun BotonAccesoInicio(
    accion: AccionInicio,
    modifier: Modifier = Modifier
) {
    val colorFondo = when (accion.estilo) {
        1 -> MaterialTheme.colorScheme.secondaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val colorContenido = when (accion.estilo) {
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        2 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(enabled = accion.habilitada, onClick = accion.onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (accion.habilitada) colorFondo else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = accion.icono,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = colorContenido
            )
            Text(
                accion.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colorContenido
            )
            Text(
                accion.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = colorContenido.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PanelInicio(
    titulo: String,
    texto: String,
    destacado: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (destacado) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

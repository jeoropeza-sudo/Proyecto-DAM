package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.Induztek.model.EstadoProtocolo
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.ui.theme.Alerta
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.components.MarcaCompacta
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.ui.theme.Exito
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteScreen(
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    onVerComprobante: (Long) -> Unit,
    onVerHistorialEquipo: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val historialRaw by viewModel.historial.collectAsState()

    var busquedaTag by remember { mutableStateOf("") }
    var filtroEstadoSeleccionado by remember { mutableStateOf("TODOS") }

    LaunchedEffect(usuarioActivo?.idUsuario) {
        usuarioActivo?.let {
            viewModel.inicializarParaUsuario(it)
            viewModel.cargarProtocolosCliente(it)
        }
    }

    val protocolosVisibles = historialRaw.filter { proto ->
        val st = EstadoProtocolo.desdeCodigo(proto.estado)
        val coincideVisibilidad = st != EstadoProtocolo.BORRADOR && st != EstadoProtocolo.ANULADO
        val coincideTag = proto.tagEquipo.contains(busquedaTag, ignoreCase = true) || proto.numeroProtocolo.contains(busquedaTag, ignoreCase = true)
        val coincideFiltroEstado = when (filtroEstadoSeleccionado) {
            "FORMALIZADO" -> st == EstadoProtocolo.FORMALIZADO
            "PENDIENTE" -> st == EstadoProtocolo.REGISTRADO_TERRENO || st == EstadoProtocolo.OBSERVADO
            else -> true
        }
        coincideVisibilidad && coincideTag && coincideFiltroEstado
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MarcaCompacta(
                        subtitulo = "Portal de clientes · ${usuarioActivo?.nombre ?: "Empresa Cliente"}",
                        sobreFondoOscuro = true
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("INSTALACIÓN ACTIVA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    Text(uiState.instalacionNombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                }
            }

            OutlinedTextField(
                value = busquedaTag,
                onValueChange = { busquedaTag = it },
                label = { Text("Buscar por TAG o Nº Protocolo") },
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filtroEstadoSeleccionado == "TODOS",
                    onClick = { filtroEstadoSeleccionado = "TODOS" },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filtroEstadoSeleccionado == "PENDIENTE",
                    onClick = { filtroEstadoSeleccionado = "PENDIENTE" },
                    label = { Text("Pendientes") }
                )
                FilterChip(
                    selected = filtroEstadoSeleccionado == "FORMALIZADO",
                    onClick = { filtroEstadoSeleccionado = "FORMALIZADO" },
                    label = { Text("Formalizados") }
                )
            }

            Text("CERTIFICADOS Y PROTOCOLOS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            if (protocolosVisibles.isEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("No se encontraron registros para la búsqueda aplicada.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(protocolosVisibles) { proto ->
                        val estadoObj = EstadoProtocolo.desdeCodigo(proto.estado)
                        Column {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onVerComprobante(proto.idProtocolo) },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(proto.numeroProtocolo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(proto.fechaHora, style = MaterialTheme.typography.labelSmall)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                estadoObj.obtenerEtiquetaCliente(),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text("Resultado: ${proto.resultadoGlobal}", fontWeight = FontWeight.Bold, color = if (proto.resultadoGlobal == "APROBADO") Exito else ErrorColor)
                                    }
                                }
                            }
                            TextButton(onClick = { onVerHistorialEquipo(proto.idEquipo) }) {
                                Text("Ver historial del equipo ${proto.tagEquipo}")
                            }
                        }
                    }
                }
            }
        }
    }
}
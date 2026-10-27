package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.Induztek.model.EstadoProtocolo
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.MarcaCompacta
import cl.duocuc.Induztek.ui.theme.Alerta
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.ui.theme.Exito
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialEquipoScreen(
    idEquipo: Long,
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    esUsuarioCliente: Boolean = false,
    onVerComprobante: (Long) -> Unit = {},
    onVolver: () -> Unit = {}
) {
    val historialRaw by viewModel.historial.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(idEquipo, usuarioActivo?.idUsuario) {
        viewModel.cargarHistorialEquipo(idEquipo, usuarioActivo)
    }

    val historialFiltrado = if (esUsuarioCliente) {
        historialRaw.filter {
            val st = EstadoProtocolo.desdeCodigo(it.estado)
            st != EstadoProtocolo.BORRADOR && st != EstadoProtocolo.ANULADO
        }
    } else {
        historialRaw
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MarcaCompacta(
                        subtitulo = "Historial · ${uiState.tagEquipo} · ${uiState.instalacionNombre}",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (historialFiltrado.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                ) {
                    Text(
                        text = "No existen protocolos disponibles para el equipo ${uiState.tagEquipo}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historialFiltrado) { proto ->
                        TarjetaHistorialProtocolo(
                            protocolo = proto,
                            esUsuarioCliente = esUsuarioCliente,
                            onClick = { onVerComprobante(proto.idProtocolo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaHistorialProtocolo(
    protocolo: ProtocoloTerreno,
    esUsuarioCliente: Boolean,
    onClick: () -> Unit
) {
    val numProtocoloText = if (protocolo.numeroProtocolo.isNotBlank()) protocolo.numeroProtocolo else "PT-${protocolo.idProtocolo}"
    val fechaText = if (protocolo.fechaHora.isNotBlank()) protocolo.fechaHora else protocolo.fechaRegistro
    val resultadoEval = if (protocolo.resultadoGlobal.isNotBlank()) protocolo.resultadoGlobal else protocolo.resultadoEvaluado

    val colorResultado = when (resultadoEval.uppercase()) {
        "APROBADO" -> Exito
        "OBSERVADO" -> Alerta
        "RECHAZADO" -> ErrorColor
        else -> Color.Gray
    }

    val estadoObj = EstadoProtocolo.desdeCodigo(protocolo.estado)
    val textoEstado = if (esUsuarioCliente) estadoObj.obtenerEtiquetaCliente() else estadoObj.etiquetaTecnica

    val (colorFondoEstado, colorTextoEstado) = when (estadoObj) {
        EstadoProtocolo.FORMALIZADO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstadoProtocolo.OBSERVADO -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    val tecnicoResp = protocolo.tecnicoNombre.ifBlank { "Sin técnico registrado" }
    val tempHumText = if (protocolo.temperaturaC > 0) "${protocolo.temperaturaC} °C / ${protocolo.humedadPct} %" else "19 °C / 60 %"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = numProtocoloText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = fechaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.secondary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RESULTADO TÉCNICO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = colorResultado.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = resultadoEval.ifBlank { "PENDIENTE" },
                            color = colorResultado,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ESTADO DEL PROTOCOLO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = colorFondoEstado,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = textoEstado.ifBlank { "REGISTRADO EN TERRENO" },
                            color = colorTextoEstado,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Técnico: $tecnicoResp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tempHumText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!protocolo.observaciones.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Obs: ${protocolo.observaciones}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
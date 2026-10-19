package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duocuc.Induztek.R
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.ui.theme.Alerta
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.ui.theme.Exito
import cl.duocuc.Induztek.viewmodel.FormularioProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialEquipoScreen(
    idEquipo: Long,
    onVolver: () -> Unit,
    viewModel: FormularioProtocoloViewModel
) {
    val historial by viewModel.historial.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial del Equipo") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            painter = painterResource(R.drawable.ic_volver),
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (historial.isEmpty()) {
                Text(
                    text = "No hay registros anteriores para este equipo.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Dimens.espacioMedio)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.espacioMedio),
                    verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
                ) {
                    items(historial, key = { it.idProtocolo }) { protocolo ->
                        TarjetaProtocoloHistorial(protocolo = protocolo)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaProtocoloHistorial(protocolo: ProtocoloTerreno) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.espacioMedio)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protocolo.numeroProtocolo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = if (protocolo.estado == "FORMALIZADO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = protocolo.estado,
                        color = Color.Black,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Fecha: ${protocolo.fechaHora}", style = MaterialTheme.typography.bodyMedium)

            val colorResultado = when (protocolo.resultadoGlobal) {
                "APROBADO" -> Exito
                "OBSERVADO" -> Alerta
                else -> ErrorColor
            }

            Text(
                text = "Resultado: ${protocolo.resultadoGlobal}",
                color = colorResultado,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
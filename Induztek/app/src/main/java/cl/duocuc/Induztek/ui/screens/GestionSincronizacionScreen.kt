package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.data.remote.SyncQueueEntry
import cl.duocuc.Induztek.viewmodel.EstadoSincronizacionViewModel

@Composable
fun GestionSincronizacionScreen(
    viewModel: EstadoSincronizacionViewModel,
    onVolver: () -> Unit
) {
    val tareas by viewModel.tareas.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.actualizar()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Sincronización", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.actualizar() }) {
                    Text("Actualizar")
                }
                Button(onClick = onVolver) {
                    Text("Volver")
                }
            }
            if (tareas.isEmpty()) {
                Text("No hay operaciones pendientes ni conflictos.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tareas, key = { tarea -> tarea.id }) { tarea ->
                        TareaSincronizacionCard(
                            tarea = tarea,
                            onReintentar = { viewModel.reintentar(tarea.id) },
                            onDescartar = { viewModel.descartar(tarea.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TareaSincronizacionCard(
    tarea: SyncQueueEntry,
    onReintentar: () -> Unit,
    onDescartar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(tarea.operation, style = MaterialTheme.typography.titleMedium)
            Text("Estado: ${nombreEstado(tarea.status)}")
            val error = tarea.lastError
            if (!error.isNullOrBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onReintentar) {
                    Text("Reintentar")
                }
                OutlinedButton(onClick = onDescartar) {
                    Text("Descartar")
                }
            }
        }
    }
}

private fun nombreEstado(estado: String): String {
    return when (estado) {
        "PENDING" -> "Pendiente"
        "IN_PROGRESS" -> "Enviando"
        "CONFLICT" -> "Conflicto"
        "FAILED" -> "Fallida"
        else -> estado
    }
}

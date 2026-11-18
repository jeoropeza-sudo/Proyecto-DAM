package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.FiltroProtocolos
import cl.duocuc.Induztek.model.ProtocoloIndexado

@Composable
fun FiltroProtocoloCombo(
    protocolos: List<ProtocoloIndexado>,
    seleccion: Set<String>,
    onSeleccionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var seleccionTemporal by remember { mutableStateOf(emptySet<String>()) }
    val opciones = remember(protocolos) { FiltroProtocolos.opciones(protocolos) }
    val opcionesVisibles = remember(opciones, busqueda) {
        FiltroProtocolos.filtrarOpciones(opciones, busqueda)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (seleccion.isEmpty()) {
                ""
            } else {
                "${seleccion.size} filtro(s) seleccionado(s)"
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("Filtrar") },
            placeholder = { Text("Toca para buscar y seleccionar") },
            trailingIcon = { Text("⌄", style = MaterialTheme.typography.titleMedium) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                seleccionTemporal = seleccion
                busqueda = ""
                abierto = true
            }
        )
    }

    if (abierto) {
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text("Filtrar protocolos") },
            text = {
                Column {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        label = { Text("Buscar filtros") },
                        placeholder = { Text("Cliente, equipo, TAG, fecha o estado") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Selecciona uno o varios filtros",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    TextButton(onClick = { seleccionTemporal = emptySet() }) {
                        Text("Limpiar selección")
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(opcionesVisibles) { opcion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        seleccionTemporal = if (opcion in seleccionTemporal) {
                                            seleccionTemporal - opcion
                                        } else {
                                            seleccionTemporal + opcion
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = opcion in seleccionTemporal,
                                    onCheckedChange = { marcado ->
                                        seleccionTemporal = if (marcado) {
                                            seleccionTemporal + opcion
                                        } else {
                                            seleccionTemporal - opcion
                                        }
                                    }
                                )
                                Text(
                                    text = opcion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSeleccionChange(seleccionTemporal)
                        abierto = false
                    }
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

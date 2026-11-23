package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.viewmodel.InstrumentoAdministrativo
import cl.duocuc.Induztek.viewmodel.InstrumentosViewModel

@Composable
fun GestionInstrumentosScreen(
    usuarioActivo: Usuario?,
    viewModel: InstrumentosViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val puedeAdministrar = usuarioActivo?.rolCodigo.equals(Rol.CODIGO_INGENIERO, true) ||
        usuarioActivo?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, true)
    var nombre by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var serie by remember { mutableStateOf("") }
    var tipoPrueba by remember { mutableStateOf("AISLACION") }
    var instrumentoCalibracion by remember { mutableStateOf<InstrumentoAdministrativo?>(null) }
    var fechaCalibracion by remember { mutableStateOf("") }
    var fechaVencimiento by remember { mutableStateOf("") }
    var laboratorio by remember { mutableStateOf("") }
    var certificado by remember { mutableStateOf("") }
    var errorFormulario by remember { mutableStateOf<String?>(null) }

    if (instrumentoCalibracion != null) {
        AlertDialog(
            onDismissRequest = { instrumentoCalibracion = null },
            title = { Text("Registrar calibración") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(instrumentoCalibracion?.modelo.orEmpty())
                    OutlinedTextField(
                        value = fechaCalibracion,
                        onValueChange = { fechaCalibracion = it },
                        label = { Text("Fecha calibración (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fechaVencimiento,
                        onValueChange = { fechaVencimiento = it },
                        label = { Text("Fecha vencimiento (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = laboratorio,
                        onValueChange = { laboratorio = it },
                        label = { Text("Laboratorio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = certificado,
                        onValueChange = { certificado = it },
                        label = { Text("Certificado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val fechaValida = Regex("\\d{4}-\\d{2}-\\d{2}")
                        if (!fechaValida.matches(fechaCalibracion) ||
                            !fechaValida.matches(fechaVencimiento)
                        ) {
                            errorFormulario = "Ingresa fechas válidas en formato AAAA-MM-DD."
                        } else {
                            val seleccionado = instrumentoCalibracion
                            if (seleccionado != null) {
                                viewModel.registrarCalibracion(
                                    seleccionado.id,
                                    fechaCalibracion,
                                    fechaVencimiento,
                                    laboratorio,
                                    certificado
                                )
                                instrumentoCalibracion = null
                                fechaCalibracion = ""
                                fechaVencimiento = ""
                                laboratorio = ""
                                certificado = ""
                                errorFormulario = null
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { instrumentoCalibracion = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Administración de instrumentos", style = MaterialTheme.typography.headlineSmall)
            if (!puedeAdministrar) {
                Text(
                    "Solo Ingeniería y Jefatura Técnica pueden administrar instrumentos.",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serie,
                    onValueChange = { serie = it },
                    label = { Text("Número de serie") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo de prueba")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("AISLACION", "TTR", "DEVANADOS", "RELE").forEach { tipo ->
                        Text(
                            text = tipo,
                            color = if (tipoPrueba == tipo) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .clickable { tipoPrueba = tipo }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                Button(
                    onClick = {
                        if (nombre.isBlank() || marca.isBlank() || modelo.isBlank() || serie.isBlank()) {
                            errorFormulario = "Completa nombre, marca, modelo y número de serie."
                        } else {
                            errorFormulario = null
                            viewModel.crearInstrumento(nombre, marca, modelo, serie, tipoPrueba)
                        }
                    },
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar instrumento")
                }

                if (errorFormulario != null) {
                    Text(errorFormulario.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                if (estado.error != null) {
                    Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                if (estado.mensaje != null) {
                    Text(estado.mensaje.orEmpty(), color = MaterialTheme.colorScheme.primary)
                }

                if (estado.conflictos.isNotEmpty()) {
                    Text(
                        "Conflictos de sincronización",
                        style = MaterialTheme.typography.titleLarge
                    )
                    for (conflicto in estado.conflictos) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "${conflicto.resourceType}: ${conflicto.operation}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    conflicto.lastError.orEmpty(),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Elige qué copia conservar.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            viewModel.resolverConflicto(conflicto.id, true)
                                        }
                                    ) {
                                        Text("Reintentar local")
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.resolverConflicto(conflicto.id, false)
                                        }
                                    ) {
                                        Text("Descartar local")
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Inventario", style = MaterialTheme.typography.titleLarge)
                if (estado.cargando) {
                    Text("Cargando instrumentos...")
                }
                for (instrumento in estado.instrumentos) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "${instrumento.nombre} · ${instrumento.modelo}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("${instrumento.marca} · S/N ${instrumento.numeroSerie}")
                            Text("Prueba: ${instrumento.tipoPrueba}")
                            if (instrumento.bloqueadoPorVencimiento) {
                                Text(
                                    "Bloqueado por calibración vencida.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { viewModel.cambiarEstado(instrumento) }
                                ) {
                                    Text(if (instrumento.activo) "Desactivar" else "Activar")
                                }
                                TextButton(
                                    onClick = { instrumentoCalibracion = instrumento }
                                ) {
                                    Text("Registrar calibración")
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { viewModel.cargarInstrumentos() },
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Actualizar inventario")
                }
            }
        }
    }
}

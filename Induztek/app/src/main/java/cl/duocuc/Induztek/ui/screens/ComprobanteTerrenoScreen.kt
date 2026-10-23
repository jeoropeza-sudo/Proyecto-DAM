package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.ui.components.LogoInduztek
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.ui.theme.Exito
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprobanteTerrenoScreen(
    idProtocolo: Long,
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    onVolverInicio: () -> Unit
) {
    var protocolo by remember(idProtocolo) { mutableStateOf<ProtocoloTerreno?>(null) }
    var motivoRevision by remember(idProtocolo) { mutableStateOf("") }
    var mensajeRevision by remember(idProtocolo) { mutableStateOf<String?>(null) }
    LaunchedEffect(idProtocolo, usuarioActivo) {
        protocolo = viewModel.obtenerProtocoloVisible(idProtocolo, usuarioActivo)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LogoInduztek(sobreFondoOscuro = true, modifier = Modifier.size(40.dp))
                        Text(
                            "ACTA DE TERRENO",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val registro = protocolo
            if (registro == null) {
                Text(
                    "No se encontró el protocolo o no tienes permiso para consultarlo.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            } else {
                val estado = EstadoProtocolo.desdeCodigo(registro.estado)
                val estadoVisible = when (estado) {
                    EstadoProtocolo.REGISTRADO_TERRENO, EstadoProtocolo.OBSERVADO -> "PENDIENTE DE FORMALIZACIÓN"
                    else -> estado.etiquetaTecnica.uppercase()
                }

                Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        LogoInduztek(sobreFondoOscuro = false, modifier = Modifier.size(76.dp))
                        Text("OT-${registro.numeroOt}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text("Protocolo Nº: ${registro.numeroProtocolo}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                    HorizontalDivider()

                    // ESTADO CLIENTE VISIBLE
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = estadoVisible,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // DATOS DE LA VISITA Y CLIENTE
            FilaComprobante("Empresa Cliente", registro.clienteNombre)
            FilaComprobante("Instalación", registro.instalacionNombre)
            FilaComprobante("Tipo de visita", registro.tipoVisita)
            FilaComprobante("Descripción de mantenimiento", registro.descripcionMantenimiento.orEmpty())
            FilaComprobante("Fecha y Hora", registro.fechaHora)
            FilaComprobante("Técnico a Cargo", registro.tecnicoNombre)

            HorizontalDivider()

            // DATOS DEL EQUIPO
            Text("DATOS DEL EQUIPO PROBADO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            FilaComprobante("TAG Equipo", registro.tagEquipo)
            FilaComprobante("Tipo de equipo", registro.tipoEquipo)
            FilaComprobante("Marca / modelo", "${registro.marcaEquipo} / ${registro.modeloEquipo}")
            FilaComprobante("Nº de serie", registro.numeroSerieEquipo)
            FilaComprobante("Año de fabricación", registro.anoFabricacion)
            FilaComprobante("Potencia", registro.potenciaKva)
            FilaComprobante("Tensiones", registro.tensiones)
            FilaComprobante("Tap / grupo de conexión", "${registro.posicionTap} / ${registro.grupoConexion}")

            HorizontalDivider()

            // CONDICIONES Y RESULTADO GLOBAL
            Text("RESULTADO Y CONDICIONES", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            FilaComprobante("Temperatura / Humedad", "${registro.temperaturaC} °C / ${registro.humedadPct} %")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Resultado Evaluación:", fontWeight = FontWeight.Bold)
                Text(registro.resultadoGlobal, fontWeight = FontWeight.ExtraBold, color = if (registro.resultadoGlobal == "APROBADO") Exito else ErrorColor)
            }
            if (!registro.observaciones.isNullOrBlank()) {
                FilaComprobante("Observaciones", registro.observaciones)
            }
            if (!registro.motivoObservacion.isNullOrBlank()) {
                FilaComprobante("Motivo de observación", registro.motivoObservacion)
            }

            HorizontalDivider()

            Text("MEDICIONES Y RESULTADOS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (registro.mediciones.isEmpty()) {
                Text("No se registraron mediciones: ${registro.observaciones.orEmpty()}")
            } else {
                registro.mediciones.forEach { medicion ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("${medicion.tipoPrueba}: ${medicion.nombre}", fontWeight = FontWeight.Bold)
                            val valores = when (medicion.tipoPrueba) {
                                "AISLACION" -> "1 min: ${medicion.valor1} ${medicion.unidad}; 10 min: ${medicion.valor2} ${medicion.unidad}; IP: ${medicion.valor3}"
                                "TTR" -> "Teórica: ${medicion.referencia}; medida: ${medicion.valor1}; error: ${medicion.valor2}%"
                                "DEVANADOS" -> "Fase U: ${medicion.valor1}; V: ${medicion.valor2}; W: ${medicion.valor3} ${medicion.unidad}"
                                "CONTACTOS" -> "Medido: ${medicion.valor1} ${medicion.unidad}; máximo placa: ${medicion.referencia}; variación: ${medicion.valor2}%"
                                "RELE" -> "Ajuste: ${medicion.valor1} A; corriente: ${medicion.valor2} A; tiempo esperado: ${medicion.referencia} ms; medido: ${medicion.valor3} ms; operó: ${medicion.valor4}"
                                else -> listOf(medicion.valor1, medicion.valor2, medicion.valor3, medicion.valor4)
                                    .filter(String::isNotBlank).joinToString(" · ")
                            }
                            Text(valores, style = MaterialTheme.typography.bodySmall)
                            Text("Resultado: ${medicion.resultado}", fontWeight = FontWeight.SemiBold)
                            FilaComprobante("Instrumento", medicion.instrumento)
                        }
                    }
                }
            }

            HorizontalDivider()

            // CONSTANCIA DE RECEPCIÓN (RF-08)
            Text("CONSTANCIA DE RECEPCIÓN CLIENTE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            FilaComprobante("Nombre Receptor", registro.receptorNombre)
            FilaComprobante("RUT Receptor", registro.receptorRut)
            FilaComprobante("Cargo Receptor", registro.receptorCargo)
            FilaComprobante("Fecha y hora recepción", registro.fechaRecepcion)
            if (!registro.fechaFormalizacion.isNullOrBlank()) {
                FilaComprobante("Formalizado por", registro.usuarioFormalizaNombre.orEmpty())
                FilaComprobante("Fecha de formalización", registro.fechaFormalizacion)
            }

            HorizontalDivider()
            Text("HISTORIAL DE ESTADOS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            registro.historialEstados.forEach { evento ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${evento.estadoAnterior ?: "Nuevo"} → ${evento.estadoNuevo}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("${evento.fechaHora} · ${evento.usuarioNombre}", style = MaterialTheme.typography.bodySmall)
                    if (!evento.motivo.isNullOrBlank()) {
                        Text(evento.motivo, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (
                usuarioActivo?.rolCodigo in setOf("INGENIERO", "JEFE_TECNICO") &&
                estado in setOf(EstadoProtocolo.REGISTRADO_TERRENO, EstadoProtocolo.OBSERVADO)
            ) {
                HorizontalDivider()
                Text("REVISIÓN DE INGENIERÍA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = motivoRevision,
                    onValueChange = { motivoRevision = it },
                    label = { Text("Motivo de observación") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                if (estado == EstadoProtocolo.REGISTRADO_TERRENO) {
                    Button(
                        onClick = {
                            val actualizado = usuarioActivo?.let {
                                viewModel.cambiarEstadoProtocolo(idProtocolo, "OBSERVADO", it, motivoRevision)
                            }
                            if (actualizado != null) protocolo = actualizado
                            mensajeRevision = if (actualizado?.estado == "OBSERVADO") {
                                "Protocolo observado; el motivo quedó registrado en el historial."
                            } else "No se pudo observar el protocolo. Verifique el rol y el motivo."
                        },
                        enabled = motivoRevision.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OBSERVAR PROTOCOLO")
                    }
                }
                Button(
                    onClick = {
                        val actualizado = usuarioActivo?.let {
                            viewModel.cambiarEstadoProtocolo(
                                idProtocolo,
                                "FORMALIZADO",
                                it,
                                "Aprobado y formalizado oficialmente"
                            )
                        }
                        if (actualizado != null) protocolo = actualizado
                        mensajeRevision = if (actualizado?.estado == "FORMALIZADO") {
                            "Protocolo formalizado y registrado en el historial."
                        } else "No se pudo formalizar el protocolo. Verifique el rol y el estado."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FORMALIZAR PROTOCOLO")
                }
                Button(
                    onClick = {
                        val actualizado = usuarioActivo?.let {
                            viewModel.cambiarEstadoProtocolo(
                                idProtocolo,
                                "ANULADO",
                                it,
                                motivoRevision
                            )
                        }
                        if (actualizado != null) protocolo = actualizado
                        mensajeRevision = if (actualizado?.estado == "ANULADO") {
                            "Protocolo anulado y motivo registrado en el historial."
                        } else "No se pudo anular el protocolo. Verifique el rol, estado y motivo."
                    },
                    enabled = motivoRevision.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ANULAR PROTOCOLO")
                }
                mensajeRevision?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
            }

            Spacer(Modifier.height(10.dp))

            Button(onClick = onVolverInicio, modifier = Modifier.fillMaxWidth()) {
                Text("VOLVER AL INICIO")
            }
        }
    }
}

@Composable
private fun FilaComprobante(label: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
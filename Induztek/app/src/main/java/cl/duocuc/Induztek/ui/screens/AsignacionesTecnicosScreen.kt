package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.InstalacionAsignacionLocal
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.MarcaCompacta
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignacionesTecnicosScreen(
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?
) {
    val esJefeTecnico = usuarioActivo?.rolCodigo.equals(
        Rol.CODIGO_JEFE_TECNICO,
        ignoreCase = true
    )
    val tecnicos = remember(usuarioActivo?.idUsuario, esJefeTecnico) {
        viewModel.tecnicosParaAsignacion(usuarioActivo)
    }
    val instalaciones = remember(usuarioActivo?.idUsuario, esJefeTecnico) {
        viewModel.instalacionesParaAsignacion(usuarioActivo)
    }
    var tecnicoSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var instalacionesSeleccionadas by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var instalacionesOcupadas by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var menuTecnicosExpandido by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(usuarioActivo?.idUsuario, tecnicos) {
        tecnicoSeleccionadoId = tecnicos.firstOrNull()?.idUsuario
    }
    LaunchedEffect(tecnicoSeleccionadoId, usuarioActivo?.idUsuario) {
        val idTecnico = tecnicoSeleccionadoId
        if (idTecnico != null) {
            instalacionesSeleccionadas =
                viewModel.idsAsignadosATecnico(idTecnico, usuarioActivo)
            instalacionesOcupadas =
                viewModel.instalacionesAsignadasAOtrosTecnicos(idTecnico, usuarioActivo)
            mensaje = null
        } else {
            instalacionesSeleccionadas = emptySet()
            instalacionesOcupadas = emptyMap()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MarcaCompacta(subtitulo = "Asignación de terreno", sobreFondoOscuro = true)
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!esJefeTecnico) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Solo el Jefe Técnico puede administrar estas asignaciones.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (tecnicos.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "No hay técnicos activos para asignar.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Alcance de acceso",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Cada instalación se asigna a un solo técnico. Ingeniería y jefatura conservan acceso global.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = menuTecnicosExpandido,
                    onExpandedChange = { menuTecnicosExpandido = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tecnicos.firstOrNull {
                            it.idUsuario == tecnicoSeleccionadoId
                        }?.nombre.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Técnico") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = menuTecnicosExpandido
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = menuTecnicosExpandido,
                        onDismissRequest = { menuTecnicosExpandido = false }
                    ) {
                        tecnicos.forEach { tecnico ->
                            DropdownMenuItem(
                                text = { Text(tecnico.nombre) },
                                onClick = {
                                    tecnicoSeleccionadoId = tecnico.idUsuario
                                    menuTecnicosExpandido = false
                                }
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Instalaciones disponibles",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Selecciona todas las instalaciones en las que este técnico puede trabajar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary)

                        instalaciones.groupBy { it.empresa }.forEach { (empresa, lista) ->
                            Text(
                                empresa,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            lista.forEach { instalacion ->
                                FilaInstalacionAsignable(
                                    instalacion = instalacion,
                                    seleccionada = instalacion.idInstalacion in instalacionesSeleccionadas,
                                    asignadaAOtroTecnico = instalacionesOcupadas[instalacion.idInstalacion],
                                    onCambiar = { marcada ->
                                        instalacionesSeleccionadas =
                                            if (marcada) {
                                                instalacionesSeleccionadas + instalacion.idInstalacion
                                            } else {
                                                instalacionesSeleccionadas - instalacion.idInstalacion
                                            }
                                        mensaje = null
                                    }
                                )
                            }
                        }
                    }
                }

                mensaje?.let { texto ->
                    Text(
                        texto,
                        color = if (texto.startsWith("Asignaciones guardadas")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                BotonPrincipal(
                    texto = "Guardar asignaciones",
                    onClick = {
                        val idTecnico = tecnicoSeleccionadoId
                        val guardado = idTecnico != null && viewModel.guardarAsignacionesTecnico(
                            idTecnico,
                            instalacionesSeleccionadas,
                            usuarioActivo
                        )
                        mensaje = if (guardado) {
                            "Asignaciones guardadas correctamente."
                        } else {
                            "No se pudieron guardar las asignaciones."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    habilitado = esJefeTecnico && tecnicoSeleccionadoId != null
                )
            }
        }
    }
}

@Composable
private fun FilaInstalacionAsignable(
    instalacion: InstalacionAsignacionLocal,
    seleccionada: Boolean,
    asignadaAOtroTecnico: String?,
    onCambiar: (Boolean) -> Unit
) {
    val habilitada = asignadaAOtroTecnico == null || seleccionada
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitada) { onCambiar(!seleccionada) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = seleccionada,
            onCheckedChange = onCambiar,
            enabled = habilitada
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(instalacion.nombre, style = MaterialTheme.typography.bodyMedium)
            if (asignadaAOtroTecnico != null && !seleccionada) {
                Text(
                    "Asignada a $asignadaAOtroTecnico",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

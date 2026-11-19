package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.Induztek.model.InstalacionAsignacionLocal
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignacionesTecnicosScreen(
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?
) {
    val puedeAsignar = usuarioActivo?.rolCodigo.equals(
        Rol.CODIGO_JEFE_TECNICO,
        ignoreCase = true
    ) || usuarioActivo?.rolCodigo.equals(Rol.CODIGO_INGENIERO, ignoreCase = true)
    val tecnicos = remember(usuarioActivo?.idUsuario, puedeAsignar) {
        viewModel.tecnicosParaAsignacion(usuarioActivo)
    }
    val instalaciones = remember(usuarioActivo?.idUsuario, puedeAsignar) {
        viewModel.instalacionesParaAsignacion(usuarioActivo)
    }
    var tecnicoSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var instalacionesSeleccionadas by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var asignacionesOriginales by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var instalacionesOcupadas by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var menuTecnicosExpandido by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val instalacionesFiltradas = instalaciones.filter {
        it.nombre.contains(busqueda.trim(), ignoreCase = true) ||
                it.empresa.contains(busqueda.trim(), ignoreCase = true)
    }

    LaunchedEffect(usuarioActivo?.idUsuario, tecnicos) {
        tecnicoSeleccionadoId = tecnicos.firstOrNull()?.idUsuario
    }
    LaunchedEffect(tecnicoSeleccionadoId, usuarioActivo?.idUsuario) {
        val idTecnico = tecnicoSeleccionadoId
        if (idTecnico != null) {
            val asignadas = viewModel.idsAsignadosATecnico(idTecnico, usuarioActivo)
            instalacionesSeleccionadas = asignadas
            asignacionesOriginales = asignadas
            instalacionesOcupadas =
                viewModel.instalacionesAsignadasAOtrosTecnicos(idTecnico, usuarioActivo)
            mensaje = null
        } else {
            instalacionesSeleccionadas = emptySet()
            asignacionesOriginales = emptySet()
            instalacionesOcupadas = emptyMap()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Asignaciones",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "Técnicos e instalaciones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
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
            if (!puedeAsignar) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Solo Ingeniería y Jefatura Técnica pueden administrar estas asignaciones.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (tecnicos.isEmpty()) {
                MensajeAsignacion(
                    titulo = "No hay técnicos disponibles",
                    detalle = "Registra o activa una cuenta con perfil de técnico antes de crear asignaciones."
                )
            } else {
                Text(
                    "Asigna instalaciones para definir qué equipos puede atender cada técnico.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                        label = { Text("1. Elige un técnico") },
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
                            "2. Elige sus instalaciones",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Seleccionadas: ${instalacionesSeleccionadas.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = busqueda,
                            onValueChange = { busqueda = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Buscar por empresa o instalación") },
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    instalacionesSeleccionadas = instalacionesSeleccionadas +
                                            instalacionesFiltradas
                                                .filter {
                                                    instalacionesOcupadas[it.idInstalacion] == null ||
                                                            it.idInstalacion in instalacionesSeleccionadas
                                                }
                                                .map { it.idInstalacion }
                                    mensaje = null
                                },
                                enabled = instalacionesFiltradas.any {
                                    instalacionesOcupadas[it.idInstalacion] == null &&
                                            it.idInstalacion !in instalacionesSeleccionadas
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "Marcar disp.",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    instalacionesSeleccionadas = instalacionesSeleccionadas -
                                            instalacionesFiltradas.map { it.idInstalacion }.toSet()
                                    mensaje = null
                                },
                                enabled = instalacionesFiltradas.any {
                                    it.idInstalacion in instalacionesSeleccionadas
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "Limpiar",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary)

                        if (instalaciones.isEmpty()) {
                            Text(
                                "No hay instalaciones registradas para asignar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (instalacionesFiltradas.isEmpty()) {
                            Text(
                                "No encontramos instalaciones con esa búsqueda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            instalacionesFiltradas.groupBy { it.empresa }.forEach { (empresa, lista) ->
                                Text(
                                    empresa,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
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
                }

                mensaje?.let { texto ->
                    Text(
                        texto,
                        color = if (texto.startsWith("Listo:")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                BotonPrincipal(
                    texto = "Guardar cambios",
                    onClick = {
                        val idTecnico = tecnicoSeleccionadoId
                        if (idTecnico == null) {
                            mensaje = "Elige un técnico antes de guardar."
                        } else {
                            val guardado = viewModel.guardarAsignacionesTecnico(
                                idTecnico,
                                instalacionesSeleccionadas,
                                usuarioActivo
                            )
                            mensaje = if (guardado) {
                                asignacionesOriginales = instalacionesSeleccionadas
                                instalacionesOcupadas =
                                    viewModel.instalacionesAsignadasAOtrosTecnicos(idTecnico, usuarioActivo)
                                "Listo: se guardaron ${instalacionesSeleccionadas.size} instalaciones para este técnico."
                            } else {
                                "No se guardaron los cambios. Revisa si alguna instalación fue asignada a otra persona."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    habilitado = puedeAsignar &&
                            tecnicoSeleccionadoId != null &&
                            instalacionesSeleccionadas != asignacionesOriginales
                )
            }
        }
    }
}

@Composable
private fun MensajeAsignacion(titulo: String, detalle: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(titulo, fontWeight = FontWeight.Bold)
            Text(detalle, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
            Text(instalacion.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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

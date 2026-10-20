package cl.duocuc.Induztek.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.Induztek.R
import cl.duocuc.Induztek.model.DevanadoOption
import cl.duocuc.Induztek.model.FormularioProtocoloUiState
import cl.duocuc.Induztek.model.TipoEquipoOption
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.BotonSecundario
import cl.duocuc.Induztek.ui.components.CampoTexto
import cl.duocuc.Induztek.ui.components.GrupoRadio
import cl.duocuc.Induztek.ui.components.ImagenEncabezado
import cl.duocuc.Induztek.ui.components.TarjetaSeccion
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.ui.theme.Alerta
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.ui.theme.Exito
import cl.duocuc.Induztek.ui.theme.ExitoFondo
import cl.duocuc.Induztek.ui.theme.InduztekTheme
import cl.duocuc.Induztek.viewmodel.FormularioProtocoloViewModel
import kotlinx.coroutines.launch

@Composable
fun FormularioProtocoloScreen(
    onVerHistorial: (Long) -> Unit,
    onVerComprobante: (Long) -> Unit,
    viewModel: FormularioProtocoloViewModel = viewModel(),
    usuarioActivo: Usuario? = null,
    onCerrarSesion: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var mostrarDialogoLimpiar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (mostrarDialogoLimpiar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLimpiar = false },
            title = { Text("Limpiar Formulario") },
            text = { Text("¿Está seguro de que desea borrar todos los datos ingresados en la prueba?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoLimpiar = false
                    viewModel.onLimpiar()
                }) { Text("Limpiar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoLimpiar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Induztek Ingeniería",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(Dimens.espacioMedio)
                )

                // Muestra los datos del operador si hay sesión activa
                if (usuarioActivo != null) {
                    Column(
                        modifier = Modifier.padding(horizontal = Dimens.espacioMedio, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Operador: ${usuarioActivo.nombre}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = usuarioActivo.correo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.espacioChico))

                NavigationDrawerItem(
                    label = { Text("Historial del Equipo") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val idEquipo = if (uiState.tagEquipo == "TR-02") 2L else 1L
                        onVerHistorial(idEquipo)
                    }
                )

                if (onCerrarSesion != null) {
                    NavigationDrawerItem(
                        label = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onCerrarSesion()
                        }
                    )
                }
            }
        }
    ) {
        FormularioProtocoloContent(
            uiState = uiState,
            onTagEquipoSeleccionado = viewModel::onTagEquipoSeleccionado,
            onTipoEquipoSeleccionado = viewModel::onTipoEquipoSeleccionado,
            onDevanadoSeleccionado = viewModel::onDevanadoSeleccionado,
            onTensionSeleccionada = viewModel::onTensionSeleccionada,
            onLectura1MinCambia = viewModel::onLectura1MinCambia,
            onLectura10MinCambia = viewModel::onLectura10MinCambia,
            onObservacionesCambia = viewModel::onObservacionesCambia,
            onRegistrar = {
                if (viewModel.onRegistrarProtocolo()) {
                    onVerComprobante(1L)
                }
            },
            onLimpiar = { mostrarDialogoLimpiar = true },
            onAbrirMenu = { scope.launch { drawerState.open() } },
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioProtocoloContent(
    uiState: FormularioProtocoloUiState,
    onTagEquipoSeleccionado: (String) -> Unit,
    onTipoEquipoSeleccionado: (TipoEquipoOption) -> Unit,
    onDevanadoSeleccionado: (DevanadoOption) -> Unit,
    onTensionSeleccionada: (String) -> Unit,
    onLectura1MinCambia: (String) -> Unit,
    onLectura10MinCambia: (String) -> Unit,
    onObservacionesCambia: (String) -> Unit,
    onRegistrar: () -> Unit,
    onLimpiar: () -> Unit,
    onAbrirMenu: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protocolo Terreno: ${uiState.tagEquipo}") },
                navigationIcon = {
                    IconButton(onClick = onAbrirMenu) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = "Menú"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ImagenEncabezado(
                imagen = R.drawable.banner_registro,
                descripcion = "Banner Induztek",
                titulo = "Protocolo de Mantenimiento",
                subtitulo = "Planta Lampa - Envasadora del Pacífico"
            )

            Column(
                modifier = Modifier.padding(Dimens.espacioPantalla),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
            ) {
                // Sección 1: Selección de Equipo e Instalación
                TarjetaSeccion(titulo = "Identificación del Equipo") {
                    var expandidoTag by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandidoTag,
                        onExpandedChange = { expandidoTag = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.tagEquipo,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Seleccionar Equipo (TAG)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoTag) },
                            shape = EstilosCampo.forma,
                            colors = EstilosCampo.colores(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expandidoTag,
                            onDismissRequest = { expandidoTag = false }
                        ) {
                            uiState.tagsDisponibles.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        onTagEquipoSeleccionado(tag)
                                        expandidoTag = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Tipo de Equipo:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    GrupoRadio(
                        opciones = TipoEquipoOption.entries,
                        seleccionada = uiState.tipoEquipo,
                        onSeleccionar = onTipoEquipoSeleccionado,
                        textoDe = { it.etiqueta }
                    )
                }

                // Sección 2: Mediciones de Resistencia de Aislamiento
                TarjetaSeccion(titulo = "Prueba de Aislamiento (RN-05)") {
                    Text(
                        text = "Devanado / Elemento a probar:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    GrupoRadio(
                        opciones = DevanadoOption.entries,
                        seleccionada = uiState.devanadoSeleccionado,
                        onSeleccionar = onDevanadoSeleccionado,
                        textoDe = { it.etiqueta }
                    )

                    CampoTexto(
                        valor = uiState.lectura1Min,
                        onValorCambia = onLectura1MinCambia,
                        etiqueta = "Lectura a 1 Minuto (MΩ)",
                        icono = R.drawable.ic_persona,
                        error = uiState.errorLectura1Min,
                        tipoTeclado = KeyboardType.Number
                    )

                    CampoTexto(
                        valor = uiState.lectura10Min,
                        onValorCambia = onLectura10MinCambia,
                        etiqueta = "Lectura a 10 Minutos (MΩ)",
                        icono = R.drawable.ic_persona,
                        error = uiState.errorLectura10Min,
                        tipoTeclado = KeyboardType.Number
                    )
                }

                // Sección 3: Evaluación Automática e Indicadores (RN-06 / RN-09)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.espacioMedio),
                        verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
                    ) {
                        Text(
                            text = "Índice de Polarización (IP): %.2f".format(uiState.ipCalculado),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        val colorEstado = when (uiState.resultadoEvaluado) {
                            "APROBADO" -> Exito
                            "OBSERVADO" -> Alerta
                            "RECHAZADO" -> ErrorColor
                            else -> Color.Gray
                        }

                        Text(
                            text = "Resultado Evaluado: ${uiState.resultadoEvaluado}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorEstado
                        )

                        if (uiState.variacionDetectada) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "⚠️ ALERTA: Variación mayor al 25% respecto a la prueba anterior (RN-09).",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ErrorColor
                            )
                        }
                    }
                }

                // Sección 4: Observaciones en Terreno
                TarjetaSeccion(titulo = "Observaciones de Mantenimiento") {
                    OutlinedTextField(
                        value = uiState.observaciones,
                        onValueChange = onObservacionesCambia,
                        label = { Text("Detalle de inspección o limpieza realizada") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = EstilosCampo.forma,
                        colors = EstilosCampo.colores()
                    )
                }

                // Botones de Acción
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                    BotonSecundario(
                        texto = "Limpiar",
                        onClick = onLimpiar,
                        modifier = Modifier.weight(1f)
                    )
                    BotonPrincipal(
                        texto = "Guardar",
                        onClick = onRegistrar,
                        icono = R.drawable.ic_check,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.registroExitoso) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ExitoFondo, contentColor = Exito)
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.espacioMedio),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = Exito,
                                modifier = Modifier.size(Dimens.tamanoIcono)
                            )
                            Text(
                                text = "Protocolo guardado exitosamente en SQLite local",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = Dimens.espacioChico)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.espacioGrande))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FormularioProtocoloPreview() {
    InduztekTheme {
        FormularioProtocoloContent(
            uiState = FormularioProtocoloUiState(),
            onTagEquipoSeleccionado = {},
            onTipoEquipoSeleccionado = {},
            onDevanadoSeleccionado = {},
            onTensionSeleccionada = {},
            onLectura1MinCambia = {},
            onLectura10MinCambia = {},
            onObservacionesCambia = {},
            onRegistrar = {},
            onLimpiar = {}
        )
    }
}
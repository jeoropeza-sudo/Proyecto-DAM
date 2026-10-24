package cl.duocuc.Induztek.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.permisos.Permisos
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.MarcaCompacta
import cl.duocuc.Induztek.ui.components.TarjetaSeccion
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioProtocoloScreen(
    onVerHistorial: (Long) -> Unit,
    onVerComprobante: (Long) -> Unit,
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val launcherPermisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    val esCliente = usuarioActivo?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)
    val tecnicoSinAsignaciones = usuarioActivo?.rolCodigo.equals(
        Rol.CODIGO_TECNICO,
        ignoreCase = true
    ) && uiState.instalacionesDisponibles.isEmpty()
    val visitaEnCurso = uiState.protocolosPendientesVisita > 0

    LaunchedEffect(uiState.idProtocoloRegistrado) {
        uiState.idProtocoloRegistrado?.let { idProtocolo ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !Permisos.tiene(context, Permisos.NOTIFICACIONES)
            ) {
                launcherPermisoNotificaciones.launch(Permisos.NOTIFICACIONES)
            }
            onVerComprobante(idProtocolo)
        }
    }

    LaunchedEffect(usuarioActivo) {
        if (usuarioActivo != null) {
            viewModel.inicializarParaUsuario(usuarioActivo)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MarcaCompacta(
                        subtitulo = "Registro FT-03 · OT ${uiState.otNumero}",
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
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            if (esCliente) {
                Text(
                    text = "Modo de solo lectura. Los clientes no pueden registrar protocolos.",
                    color = ErrorColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (tecnicoSinAsignaciones) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "SIN INSTALACIONES ASIGNADAS",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Solicita al Jefe Técnico que asigne tus instalaciones para habilitar el registro de protocolos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            TarjetaSeccion(titulo = "1. DATOS DE LA VISITA") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uiState.fechaVisita, onValueChange = {}, label = { Text("Fecha") }, readOnly = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uiState.horaInicio, onValueChange = {}, label = { Text("Hora Inicio") }, readOnly = true, modifier = Modifier.weight(1f))
                }
                SelectorDesplegable(
                    label = "Cliente", opciones = uiState.clientesDisponibles, seleccionActual = uiState.clienteNombre, onSeleccion = { viewModel.onClienteSeleccionado(it) }, habilitado = !esCliente && !visitaEnCurso
                )
                SelectorDesplegable(
                    label = "Instalación", opciones = uiState.instalacionesDisponibles, seleccionActual = uiState.instalacionNombre, onSeleccion = { viewModel.onInstalacionSeleccionada(it) }, habilitado = !esCliente && !visitaEnCurso
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.tempAmbiente, onValueChange = { viewModel.onTempAmbienteCambia(it) }, label = { Text("T° Amb (°C) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !esCliente && !visitaEnCurso, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.humedadPct, onValueChange = { viewModel.onHumedadPctCambia(it) }, label = { Text("Humedad (%) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !esCliente && !visitaEnCurso, modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = uiState.tecnicoNombre, onValueChange = {}, label = { Text("Técnico(s)") }, readOnly = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.ingenieroCargo, onValueChange = {}, label = { Text("Ingeniero") }, readOnly = true, modifier = Modifier.fillMaxWidth())
                SelectorDesplegable(
                    label = "Tipo de visita",
                    opciones = listOf("SOLO PRUEBAS", "PRUEBAS Y MANTENIMIENTO"),
                    seleccionActual = uiState.tipoVisita,
                    onSeleccion = { viewModel.onTipoVisitaCambia(it) },
                    habilitado = !esCliente && !visitaEnCurso
                )
                if (uiState.tipoVisita == "PRUEBAS_Y_MANTENIMIENTO") {
                    OutlinedTextField(
                        value = uiState.descripcionMantenimiento,
                        onValueChange = { viewModel.onDescripcionMantenimientoCambia(it) },
                        label = { Text("Descripción del mantenimiento *") },
                        enabled = !esCliente && !visitaEnCurso,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            TarjetaSeccion(titulo = "REALIZACIÓN DEL ENSAYO") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pruebas realizadas", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = uiState.pruebaRealizada,
                        onCheckedChange = { viewModel.onPruebaRealizadaCambia(it) },
                        enabled = !esCliente
                    )
                }
                if (!uiState.pruebaRealizada) {
                    OutlinedTextField(
                        value = uiState.motivoNoRealizada,
                        onValueChange = { viewModel.onMotivoNoRealizadaCambia(it) },
                        label = { Text("Motivo por el que no se realizó *") },
                        enabled = !esCliente,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            TarjetaSeccion(titulo = "2. DATOS DEL EQUIPO (PLACA)") {
                SelectorDesplegable(
                    label = "Seleccionar Equipo (TAG)", opciones = uiState.tagsDisponibles, seleccionActual = uiState.tagEquipo, onSeleccion = { viewModel.onTagEquipoSeleccionado(it) }, habilitado = !esCliente
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uiState.tipoEquipo, onValueChange = {}, label = { Text("Tipo") }, readOnly = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uiState.marcaEquipo, onValueChange = {}, label = { Text("Marca") }, readOnly = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uiState.modeloEquipo, onValueChange = {}, label = { Text("Modelo") }, readOnly = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uiState.potenciaKva, onValueChange = {}, label = { Text("Potencia") }, readOnly = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uiState.numeroSerieEquipo, onValueChange = {}, label = { Text("Nº Serie") }, readOnly = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uiState.anoFabricacion, onValueChange = {}, label = { Text("Año") }, readOnly = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uiState.tensiones, onValueChange = {}, label = { Text("Tensiones") }, readOnly = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = "${uiState.tapPosition} / ${uiState.conexionGrupo}", onValueChange = {}, label = { Text("Tap / Grupo") }, readOnly = true, modifier = Modifier.weight(1f))
                }
            }

            if (uiState.pruebaRealizada) when (uiState.tipoEquipo) {
                "Transformador" -> {
                    TarjetaSeccion(titulo = "3. RESISTENCIA DE AISLACIÓN (MΩ)") {
                        val opcionesInstAis = uiState.instrumentosDisponiblesAislacion.map { "${it.modelo} (${it.numeroSerie})" }
                        SelectorDesplegable(label = "Instrumento Megóhmetro", opciones = opcionesInstAis, seleccionActual = "${uiState.instrumentoAislacionSeleccionado.modelo} (${uiState.instrumentoAislacionSeleccionado.numeroSerie})", onSeleccion = { viewModel.onInstrumentoAislacionSeleccionado(it) }, habilitado = !esCliente)
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.aislacionFilas.forEachIndexed { idx, fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = fila.lectura1Min, onValueChange = { viewModel.onAislacionCambia(idx, it, fila.lectura10Min) }, label = { Text("${fila.devanado} 1m") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fila.lectura10Min, onValueChange = { viewModel.onAislacionCambia(idx, fila.lectura1Min, it) }, label = { Text("10m") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                Text(text = "IP: ${fila.ipCalculado}", modifier = Modifier.weight(0.5f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    TarjetaSeccion(titulo = "4. RELACIÓN DE TRANSFORMACIÓN (TTR)") {
                        val opcionesInstTtr = uiState.instrumentosDisponiblesTtr.map { "${it.modelo} (${it.numeroSerie})" }
                        SelectorDesplegable(label = "Instrumento TTR", opciones = opcionesInstTtr, seleccionActual = "${uiState.instrumentoTtrSeleccionado.modelo} (${uiState.instrumentoTtrSeleccionado.numeroSerie})", onSeleccion = { viewModel.onInstrumentoTtrSeleccionado(it) }, habilitado = !esCliente)
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.ttrFilas.forEachIndexed { idx, fila ->
                            OutlinedTextField(value = fila.relacionMedida, onValueChange = { viewModel.onTtrMedidaCambia(idx, it) }, label = { Text("Fase ${fila.fase} (Teórica: ${fila.relacionTeorica})") }, enabled = !esCliente, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    TarjetaSeccion(titulo = "5. RESISTENCIA DE DEVANADOS") {
                        val opcionesInstDev = uiState.instrumentosDisponiblesDevanados.map { "${it.modelo} (${it.numeroSerie})" }
                        SelectorDesplegable(label = "Instrumento Micro-ohmímetro", opciones = opcionesInstDev, seleccionActual = "${uiState.instrumentoDevanadosSeleccionado.modelo} (${uiState.instrumentoDevanadosSeleccionado.numeroSerie})", onSeleccion = { viewModel.onInstrumentoDevanadosSeleccionado(it) }, habilitado = !esCliente)
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.devanadoFilas.forEachIndexed { idx, fila ->
                            Text("Devanado ${fila.devanado} (${fila.unidad})", fontWeight = FontWeight.Bold)
                            SelectorDesplegable(
                                label = "Evaluación manual",
                                opciones = listOf("APROBADO", "OBSERVADO", "RECHAZADO"),
                                seleccionActual = fila.resultadoManual,
                                onSeleccion = { viewModel.onDevanadoResultadoCambia(idx, it) },
                                habilitado = !esCliente
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = fila.faseU, onValueChange = { viewModel.onDevanadoCambia(idx, it, fila.faseV, fila.faseW) }, label = { Text("Fase U") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fila.faseV, onValueChange = { viewModel.onDevanadoCambia(idx, fila.faseU, it, fila.faseW) }, label = { Text("Fase V") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fila.faseW, onValueChange = { viewModel.onDevanadoCambia(idx, fila.faseU, fila.faseV, it) }, label = { Text("Fase W") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                "Celda MT", "Interruptor" -> {
                    TarjetaSeccion(titulo = "3. RESISTENCIA DE AISLACIÓN (MΩ)") {
                        val opcionesInstAis = uiState.instrumentosDisponiblesAislacion.map { "${it.modelo} (${it.numeroSerie})" }
                        SelectorDesplegable(label = "Instrumento Megóhmetro", opciones = opcionesInstAis, seleccionActual = "${uiState.instrumentoAislacionSeleccionado.modelo} (${uiState.instrumentoAislacionSeleccionado.numeroSerie})", onSeleccion = { viewModel.onInstrumentoAislacionSeleccionado(it) }, habilitado = !esCliente)
                        uiState.aislacionFilas.forEachIndexed { idx, fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = fila.lectura1Min, onValueChange = { viewModel.onAislacionCambia(idx, it, fila.lectura10Min) }, label = { Text("${fila.devanado} 1m") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fila.lectura10Min, onValueChange = { viewModel.onAislacionCambia(idx, fila.lectura1Min, it) }, label = { Text("10m") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                Text(text = "IP: ${fila.ipCalculado}", modifier = Modifier.weight(0.5f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    TarjetaSeccion(titulo = "3. RESISTENCIA DE CONTACTOS (µΩ)") {
                        val opcionesInstDev = uiState.instrumentosDisponiblesDevanados.map { "${it.modelo} (${it.numeroSerie})" }
                        SelectorDesplegable(label = "Micro-ohmímetro", opciones = opcionesInstDev, seleccionActual = "${uiState.instrumentoDevanadosSeleccionado.modelo} (${uiState.instrumentoDevanadosSeleccionado.numeroSerie})", onSeleccion = { viewModel.onInstrumentoDevanadosSeleccionado(it) }, habilitado = !esCliente)
                        uiState.contactosFilas.forEachIndexed { idx, fila ->
                            OutlinedTextField(value = fila.valorMedidoUohm, onValueChange = { viewModel.onContactoMedidoCambia(idx, it) }, label = { Text("${fila.polo} (Máx placa: ${if (fila.valorMaximoPlacaUohm > 0) fila.valorMaximoPlacaUohm else "sin dato"} µΩ)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !esCliente, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                "Cable de poder" -> {
                    TarjetaSeccion(titulo = "3. RESISTENCIA DE AISLACIÓN (MΩ)") {
                        val opcionesInstAis = uiState.instrumentosDisponiblesAislacion.map { "${it.modelo} (${it.numeroSerie})" }
                        SelectorDesplegable(label = "Instrumento Megóhmetro", opciones = opcionesInstAis, seleccionActual = "${uiState.instrumentoAislacionSeleccionado.modelo} (${uiState.instrumentoAislacionSeleccionado.numeroSerie})", onSeleccion = { viewModel.onInstrumentoAislacionSeleccionado(it) }, habilitado = !esCliente)
                        uiState.aislacionFilas.forEachIndexed { idx, fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(value = fila.lectura1Min, onValueChange = { viewModel.onAislacionCambia(idx, it, fila.lectura10Min) }, label = { Text("${fila.devanado} 1m") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = fila.lectura10Min, onValueChange = { viewModel.onAislacionCambia(idx, fila.lectura1Min, it) }, label = { Text("10m") }, enabled = !esCliente, modifier = Modifier.weight(1f))
                                Text(text = "IP: ${fila.ipCalculado}", modifier = Modifier.weight(0.5f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                "Relé de protección" -> {
                    TarjetaSeccion(titulo = "3. INYECCIÓN SECUNDARIA") {
                        val opcionesInstRele = uiState.instrumentosDisponiblesRele.map { "${it.modelo} (${it.numeroSerie})" }
                        SelectorDesplegable(label = "Instrumento de inyección", opciones = opcionesInstRele, seleccionActual = "${uiState.instrumentoReleSeleccionado.modelo} (${uiState.instrumentoReleSeleccionado.numeroSerie})", onSeleccion = { viewModel.onInstrumentoReleSeleccionado(it) }, habilitado = !esCliente)
                        uiState.releFilas.forEachIndexed { idx, fila ->
                            OutlinedTextField(value = fila.corrienteMedidaA, onValueChange = { viewModel.onReleCorrienteMedidaCambia(idx, it) }, label = { Text("Corriente medida (A)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !esCliente, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = fila.tiempoMedidoMs, onValueChange = { viewModel.onReleMedicionCambia(idx, it, fila.operaCorrectamente) }, label = { Text("Tiempo Medido (ms) - Esp: ${fila.tiempoEsperadoMs}") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !esCliente, modifier = Modifier.fillMaxWidth())
                            Text(
                                "Desviación máxima de corriente/tiempo: ${"%.2f".format(java.util.Locale.ROOT, fila.diferenciaTiempoPct)} %",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = fila.operaCorrectamente == true,
                                    onClick = { viewModel.onReleOperacionCambia(idx, true) },
                                    label = { Text("Operó") },
                                    enabled = !esCliente
                                )
                                FilterChip(
                                    selected = fila.operaCorrectamente == false,
                                    onClick = { viewModel.onReleOperacionCambia(idx, false) },
                                    label = { Text("No operó") },
                                    enabled = !esCliente
                                )
                            }
                        }
                    }
                }
            }

            TarjetaSeccion(titulo = "6. OBSERVACIONES") {
                OutlinedTextField(
                    value = uiState.observaciones,
                    onValueChange = { viewModel.onObservacionesCambia(it) },
                    label = { Text("Observaciones de la prueba (ej: fuga de aceite)") },
                    enabled = !esCliente,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            TarjetaSeccion(titulo = "7. FIRMAS / RECEPCIÓN CLIENTE") {
                OutlinedTextField(value = uiState.clienteRecibeNombre, onValueChange = { viewModel.onClienteRecibeNombreCambia(it) }, label = { Text("Nombre Receptor *") }, enabled = !esCliente && !visitaEnCurso, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.clienteRecibeRut, onValueChange = { viewModel.onClienteRecibeRutCambia(it) }, label = { Text("RUT Receptor *") }, isError = uiState.errorRutClienteRecibe, enabled = !esCliente && !visitaEnCurso, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = uiState.clienteRecibeCargo, onValueChange = { viewModel.onClienteRecibeCargoCambia(it) }, label = { Text("Cargo Receptor") }, enabled = !esCliente && !visitaEnCurso, modifier = Modifier.fillMaxWidth())
            }

            if (uiState.mensajeErrorFormulario != null) {
                Text(uiState.mensajeErrorFormulario!!, color = ErrorColor, fontWeight = FontWeight.Bold)
            }

            if (usuarioActivo != null && !esCliente) {
                if (uiState.protocolosPendientesVisita == 0) {
                    BotonPrincipal(
                        texto = "Registrar protocolo",
                        onClick = { viewModel.onRegistrarProtocolo(usuarioActivo.idUsuario) },
                        modifier = Modifier.fillMaxWidth(),
                        habilitado = !tecnicoSinAsignaciones
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "VISITA EN PREPARACIÓN · ${uiState.protocolosPendientesVisita} EQUIPO(S)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                uiState.tagsPendientesVisita.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Agrega otro equipo o cierra la visita para registrar los protocolos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                BotonPrincipal(
                    texto = "Agregar equipo a la visita",
                    onClick = { viewModel.onRegistrarProtocolo(usuarioActivo.idUsuario, agregarAVisita = true) },
                    modifier = Modifier.fillMaxWidth(),
                    habilitado = !tecnicoSinAsignaciones
                )
                if (uiState.protocolosPendientesVisita > 0) {
                    BotonPrincipal(
                        texto = "Cerrar visita y registrar todo",
                        onClick = { viewModel.cerrarVisita(usuarioActivo.idUsuario) },
                        modifier = Modifier.fillMaxWidth(),
                        habilitado = !tecnicoSinAsignaciones
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDesplegable(
    label: String,
    opciones: List<String>,
    seleccionActual: String,
    onSeleccion: (String) -> Unit,
    habilitado: Boolean
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { if (habilitado) expandido = it }) {
        OutlinedTextField(
            value = seleccionActual,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = { onSeleccion(opcion); expandido = false }
                )
            }
        }
    }
}
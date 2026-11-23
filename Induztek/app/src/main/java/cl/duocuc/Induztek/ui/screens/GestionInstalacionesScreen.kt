package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.BotonSecundario
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionInstalacionesScreen(
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    onVolver: () -> Unit,
    onIrInicio: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val errorLecturaRemota by viewModel.errorLecturaRemota.collectAsState()
    var clientes by remember(usuarioActivo?.idUsuario) {
        mutableStateOf(emptyList<cl.duocuc.Induztek.model.CuentaEmpresaLocal>())
    }
    var clienteSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var menuExpandido by remember { mutableStateOf(false) }
    var nombreInstalacion by remember { mutableStateOf("") }
    var errorNombre by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableStateOf(0) }
    var instalacionEquipoId by remember { mutableStateOf<Long?>(null) }
    var tipoEquipoId by remember { mutableStateOf<Long?>(null) }
    var menuTipoExpandido by remember { mutableStateOf(false) }
    var tag by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var numeroSerie by remember { mutableStateOf("") }
    var potenciaKva by remember { mutableStateOf("") }
    var tensiones by remember { mutableStateOf("") }
    var anoFabricacion by remember { mutableStateOf("") }
    var posicionTap by remember { mutableStateOf("") }
    var grupoConexion by remember { mutableStateOf("") }
    var resistenciaContacto by remember { mutableStateOf("") }
    var erroresEquipo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var mensajeEquipo by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tiposEquipo = remember(usuarioActivo?.idUsuario) {
        viewModel.tiposEquipoParaGestion(usuarioActivo)
    }
    val clienteSeleccionado = clientes.firstOrNull { it.idUsuario == clienteSeleccionadoId }
    var instalaciones by remember { mutableStateOf(emptyList<cl.duocuc.Induztek.model.InstalacionCatalogoLocal>()) }
    var equiposPorInstalacion by remember { mutableStateOf(emptyMap<Long, List<cl.duocuc.Induztek.model.EquipoCatalogoLocal>>()) }

    LaunchedEffect(usuarioActivo?.idUsuario) {
        clientes = viewModel.leerClientesConEmpresaParaInstalaciones(usuarioActivo)
        if (clienteSeleccionadoId == null) {
            clienteSeleccionadoId = clientes.firstOrNull()?.idUsuario
        }
    }

    LaunchedEffect(clienteSeleccionadoId, recarga, usuarioActivo?.idUsuario) {
        val idCliente = clienteSeleccionadoId
        if (idCliente == null) {
            instalaciones = emptyList()
            equiposPorInstalacion = emptyMap()
        } else {
            instalaciones = viewModel.leerInstalacionesDeClienteParaGestion(idCliente, usuarioActivo)
            val equipos = mutableMapOf<Long, List<cl.duocuc.Induztek.model.EquipoCatalogoLocal>>()
            for (instalacion in instalaciones) {
                equipos[instalacion.idInstalacion] =
                    viewModel.leerEquiposDeInstalacionParaGestion(instalacion.idInstalacion, usuarioActivo)
            }
            equiposPorInstalacion = equipos
        }
    }

    LaunchedEffect(errorLecturaRemota) {
        if (errorLecturaRemota != null) {
            snackbarHostState.showSnackbar(errorLecturaRemota.orEmpty())
        }
    }

    LaunchedEffect(estado.mensajeErrorFormulario) {
        val errorRemoto = estado.mensajeErrorFormulario
        if (errorRemoto != null &&
            (errorRemoto.startsWith("El servidor") || errorRemoto.startsWith("No se pudo"))
        ) {
            snackbarHostState.showSnackbar(errorRemoto)
            viewModel.descartarErrorFormulario()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Instalaciones de clientes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Cada instalación representa una planta, sucursal o faena. Después de crearla podrás asignarla a un técnico.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (clientes.isEmpty()) {
                Text(
                    "No hay cuentas cliente con empresa. Primero crea una cuenta de cliente desde «Crear cuenta».",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = menuExpandido,
                    onExpandedChange = { menuExpandido = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = clienteSeleccionado?.let {
                            "${it.razonSocial} · ${it.nombreUsuario}"
                        }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Empresa y cuenta cliente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpandido) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        colors = EstilosCampo.colores()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpandido,
                        onDismissRequest = { menuExpandido = false }
                    ) {
                        clientes.forEach { cliente ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cliente.razonSocial)
                                        Text(
                                            cliente.nombreUsuario,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    clienteSeleccionadoId = cliente.idUsuario
                                    menuExpandido = false
                                    mensaje = null
                                    instalacionEquipoId = null
                                    tipoEquipoId = null
                                    tag = ""
                                    marca = ""
                                    modelo = ""
                                    numeroSerie = ""
                                    potenciaKva = ""
                                    tensiones = ""
                                    anoFabricacion = ""
                                    posicionTap = ""
                                    grupoConexion = ""
                                                    resistenciaContacto = ""
                                    erroresEquipo = emptyMap()
                                    mensajeEquipo = null
                                }
                            )
                        }
                    }
                }

                if (clienteSeleccionado != null) {
                    Text(
                        "Instalaciones registradas (${instalaciones.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (instalaciones.isEmpty()) {
                        Text(
                            "Esta empresa todavía no tiene instalaciones.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        instalaciones.forEach { instalacion ->
                            val equipos = equiposPorInstalacion[instalacion.idInstalacion].orEmpty()
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        instalacion.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (equipos.isEmpty()) {
                                        Text(
                                            "Sin equipos registrados.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        equipos.forEach { equipo ->
                                            Text(
                                                "${equipo.tag} · ${equipo.tipo} · ${equipo.marca} ${equipo.modelo}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    BotonSecundario(
                                        texto = if (instalacionEquipoId == instalacion.idInstalacion) {
                                            "Cerrar registro de equipo"
                                        } else {
                                            "Registrar equipo"
                                        },
                                        onClick = {
                                            instalacionEquipoId =
                                                if (instalacionEquipoId == instalacion.idInstalacion) {
                                                    null
                                                } else {
                                                    instalacion.idInstalacion
                                                }
                                            tipoEquipoId = null
                                            tag = ""
                                            marca = ""
                                            modelo = ""
                                            numeroSerie = ""
                                            potenciaKva = ""
                                            tensiones = ""
                                            anoFabricacion = ""
                                            posicionTap = ""
                                            grupoConexion = ""
                                            resistenciaContacto = ""
                                            erroresEquipo = emptyMap()
                                            mensajeEquipo = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (instalacionEquipoId == instalacion.idInstalacion) {
                                        val tipoSeleccionado = tiposEquipo.firstOrNull {
                                            it.idTipoEquipo == tipoEquipoId
                                        }
                                        val nombreTipoEquipo = tipoSeleccionado?.nombre.orEmpty()
                                        val esTransformador = nombreTipoEquipo.equals("Transformador", true)
                                        val esEquipoConContactos =
                                            nombreTipoEquipo.equals("Celda MT", true) ||
                                                nombreTipoEquipo.equals("Interruptor", true)
                                        val esCable = nombreTipoEquipo.equals("Cable de poder", true)
                                        val esRele = nombreTipoEquipo.equals("Relé de protección", true)
                                        ExposedDropdownMenuBox(
                                            expanded = menuTipoExpandido,
                                            onExpandedChange = { menuTipoExpandido = it },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = tipoSeleccionado?.let {
                                                    "${it.nombre} (${it.clasificacion})"
                                                }.orEmpty(),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Tipo de equipo *") },
                                                isError = erroresEquipo.containsKey("tipo"),
                                                supportingText = erroresEquipo["tipo"]?.let { error ->
                                                    { Text(error, color = MaterialTheme.colorScheme.error) }
                                                },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(menuTipoExpandido)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                                colors = EstilosCampo.colores()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = menuTipoExpandido,
                                                onDismissRequest = { menuTipoExpandido = false }
                                            ) {
                                                tiposEquipo.forEach { tipo ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text("${tipo.nombre} (${tipo.clasificacion})")
                                                        },
                                                        onClick = {
                                                            tipoEquipoId = tipo.idTipoEquipo
                                                            menuTipoExpandido = false
                                                            erroresEquipo = emptyMap()
                                                            potenciaKva = ""
                                                            tensiones = ""
                                                            anoFabricacion = ""
                                                            posicionTap = ""
                                                            grupoConexion = ""
                                                            resistenciaContacto = ""
                                                            mensajeEquipo = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        CampoEquipo("TAG *", tag, erroresEquipo["tag"]) {
                                            tag = it
                                            erroresEquipo = erroresEquipo - "tag"
                                        }
                                        CampoEquipo("Marca *", marca, erroresEquipo["marca"]) {
                                            marca = it
                                            erroresEquipo = erroresEquipo - "marca"
                                        }
                                        CampoEquipo("Modelo *", modelo, erroresEquipo["modelo"]) {
                                            modelo = it
                                            erroresEquipo = erroresEquipo - "modelo"
                                        }
                                        CampoEquipo("Número de serie *", numeroSerie, erroresEquipo["serie"]) {
                                            numeroSerie = it
                                            erroresEquipo = erroresEquipo - "serie"
                                        }
                                        Text(
                                            "Datos técnicos${if (esTransformador || esEquipoConContactos || esCable) " * = obligatorio" else ""}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (esTransformador) {
                                            CampoEquipo(
                                                "Potencia (kVA) *",
                                                potenciaKva,
                                                erroresEquipo["potencia"],
                                                KeyboardType.Decimal
                                            ) {
                                                potenciaKva = it
                                                erroresEquipo = erroresEquipo - "potencia"
                                            }
                                            CampoEquipo(
                                                "Tensiones AT / BT (ej. 12 / 0,4 kV) *",
                                                tensiones,
                                                erroresEquipo["tensiones"]
                                            ) {
                                                tensiones = it
                                                erroresEquipo = erroresEquipo - "tensiones"
                                            }
                                            CampoEquipo(
                                                "Posición TAP *",
                                                posicionTap,
                                                erroresEquipo["tap"],
                                                KeyboardType.Number
                                            ) {
                                                posicionTap = it
                                                erroresEquipo = erroresEquipo - "tap"
                                            }
                                            CampoEquipo(
                                                "Grupo de conexión *",
                                                grupoConexion,
                                                erroresEquipo["grupo"]
                                            ) {
                                                grupoConexion = it
                                                erroresEquipo = erroresEquipo - "grupo"
                                            }
                                        } else if (esEquipoConContactos) {
                                            CampoEquipo(
                                                "Tensión nominal (ej. 12 kV) *",
                                                tensiones,
                                                erroresEquipo["tensiones"]
                                            ) {
                                                tensiones = it
                                                erroresEquipo = erroresEquipo - "tensiones"
                                            }
                                            CampoEquipo(
                                                "Resistencia máxima de contacto (µΩ) *",
                                                resistenciaContacto,
                                                erroresEquipo["contacto"],
                                                KeyboardType.Decimal
                                            ) {
                                                resistenciaContacto = it
                                                erroresEquipo = erroresEquipo - "contacto"
                                            }
                                        } else if (esCable) {
                                            CampoEquipo(
                                                "Tensión y sección (ej. 15 kV, 3x240 mm²) *",
                                                tensiones,
                                                erroresEquipo["tensiones"]
                                            ) {
                                                tensiones = it
                                                erroresEquipo = erroresEquipo - "tensiones"
                                            }
                                        } else if (esRele) {
                                            CampoEquipo(
                                                "Corriente nominal o ajuste (ej. 5 A)",
                                                tensiones
                                            ) { tensiones = it }
                                        }
                                        CampoEquipo(
                                            "Año de fabricación",
                                            anoFabricacion,
                                            erroresEquipo["ano"],
                                            KeyboardType.Number
                                        ) {
                                            anoFabricacion = it
                                            erroresEquipo = erroresEquipo - "ano"
                                        }
                                        mensajeEquipo?.let {
                                            Text(
                                                it,
                                                color = if (it.startsWith("Equipo registrado")) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                }
                                            )
                                        }
                                        BotonPrincipal(
                                            texto = "Guardar equipo",
                                            onClick = {
                                                val nuevosErrores = buildMap {
                                                    if (tipoEquipoId == null) {
                                                        put("tipo", "Selecciona el tipo de equipo.")
                                                    }
                                                    if (tag.isBlank()) put("tag", "Ingresa el TAG.")
                                                    if (marca.isBlank()) put("marca", "Ingresa la marca.")
                                                    if (modelo.isBlank()) put("modelo", "Ingresa el modelo.")
                                                    if (numeroSerie.isBlank()) put("serie", "Ingresa el número de serie.")
                                                    if (esTransformador) {
                                                        if (potenciaKva.replace(',', '.').toDoubleOrNull()
                                                                ?.let { it > 0 } != true
                                                        ) {
                                                            put("potencia", "Ingresa una potencia mayor que cero.")
                                                        }
                                                        if (!Regex("""\d+(?:[.,]\d+)?\s*/\s*\d+(?:[.,]\d+)?""")
                                                                .containsMatchIn(tensiones)
                                                        ) {
                                                            put("tensiones", "Ingresa las tensiones AT/BT separadas por «/», por ejemplo 12 / 0,4 kV.")
                                                        }
                                                        if (posicionTap.isBlank() ||
                                                            posicionTap.toIntOrNull()?.let { it > 0 } != true
                                                        ) {
                                                            put("tap", "Ingresa una posición TAP entera mayor que cero.")
                                                        }
                                                        if (grupoConexion.isBlank()) {
                                                            put("grupo", "Ingresa el grupo de conexión, por ejemplo Dyn11.")
                                                        }
                                                    }
                                                    if ((esEquipoConContactos || esCable) &&
                                                        !Regex("""\d+(?:[.,]\d+)?""").containsMatchIn(tensiones)
                                                    ) {
                                                        put("tensiones", "Ingresa la tensión o los datos nominales del equipo.")
                                                    }
                                                    if (esEquipoConContactos &&
                                                        resistenciaContacto.replace(',', '.').toDoubleOrNull()
                                                            ?.let { it > 0 } != true
                                                    ) {
                                                        put("contacto", "Ingresa una resistencia máxima mayor que cero.")
                                                    }
                                                    if (anoFabricacion.isNotBlank() &&
                                                        (!anoFabricacion.matches(Regex("""\d{4}""")) ||
                                                            anoFabricacion.toIntOrNull()?.let { it in 1900..java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) } != true)
                                                    ) {
                                                        put("ano", "Ingresa un año válido entre 1900 y el año actual.")
                                                    }
                                                }
                                                erroresEquipo = nuevosErrores
                                                if (nuevosErrores.isEmpty()) {
                                                    scope.launch {
                                                        val resultado = viewModel.crearEquipoParaInstalacion(
                                                            idInstalacion = instalacion.idInstalacion,
                                                            idTipoEquipo = tipoEquipoId!!,
                                                            tag = tag,
                                                            marca = marca,
                                                            modelo = modelo,
                                                            numeroSerie = numeroSerie,
                                                            potenciaKva = potenciaKva.takeIf { it.isNotBlank() }
                                                                ?.replace(',', '.')?.toDouble(),
                                                            tensiones = tensiones,
                                                            anoFabricacion = anoFabricacion,
                                                            posicionTap = posicionTap,
                                                            grupoConexion = grupoConexion,
                                                            resistenciaContactoMaxUohm =
                                                                resistenciaContacto.takeIf { it.isNotBlank() }
                                                                    ?.replace(',', '.')?.toDouble(),
                                                            idCliente = clienteSeleccionado.idUsuario,
                                                            usuario = usuarioActivo
                                                        )
                                                        if (resultado.exitosa) {
                                                            mensajeEquipo = "Equipo registrado. Ya está disponible para crear protocolos."
                                                            tag = ""
                                                            marca = ""
                                                            modelo = ""
                                                            numeroSerie = ""
                                                            potenciaKva = ""
                                                            tensiones = ""
                                                            anoFabricacion = ""
                                                            posicionTap = ""
                                                            grupoConexion = ""
                                                            resistenciaContacto = ""
                                                            recarga += 1
                                                            snackbarHostState.showSnackbar(
                                                                "Equipo creado correctamente"
                                                            )
                                                            onIrInicio()
                                                        } else {
                                                            val campoError = resultado.campoError
                                                            if (campoError in setOf("tag", "tipo")) {
                                                                erroresEquipo = mapOf(
                                                                    campoError!! to resultado.mensajeError.orEmpty()
                                                                )
                                                            } else {
                                                                mensajeEquipo = resultado.mensajeError
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nombreInstalacion,
                        onValueChange = {
                            nombreInstalacion = it
                            errorNombre = null
                            mensaje = null
                        },
                        label = { Text("Nombre de la nueva instalación *") },
                        supportingText = errorNombre?.let { texto ->
                            { Text(texto, color = MaterialTheme.colorScheme.error) }
                        },
                        isError = errorNombre != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        colors = EstilosCampo.colores(),
                        shape = EstilosCampo.forma
                    )
                    mensaje?.let {
                        Text(
                            it,
                            color = if (it.startsWith("Instalación creada")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BotonSecundario(
                            texto = "Volver",
                            onClick = onVolver,
                            modifier = Modifier.weight(0.8f)
                        )
                        BotonPrincipal(
                            texto = "Agregar instalación",
                            onClick = {
                                val nombreLimpio = nombreInstalacion.trim()
                                when {
                                    nombreLimpio.isBlank() -> {
                                        errorNombre = "Ingresa el nombre de la instalación."
                                    }
                                    instalaciones.any {
                                        it.nombre.equals(nombreLimpio, ignoreCase = true)
                                    } -> {
                                        errorNombre = "Esta instalación ya existe para la empresa."
                                    }
                                    else -> {
                                        scope.launch {
                                            val creada = viewModel.crearInstalacionParaCliente(
                                                clienteSeleccionado.idUsuario,
                                                nombreLimpio,
                                                usuarioActivo
                                            )
                                            if (creada) {
                                                nombreInstalacion = ""
                                                errorNombre = null
                                                mensaje = "Instalación creada. Ya puedes asignarla a un técnico."
                                                recarga += 1
                                            } else {
                                                mensaje = estado.mensajeErrorFormulario
                                                    ?: "No se pudo crear la instalación. Comprueba los datos e inténtalo nuevamente."
                                            }
                                        }
                                    }
                                }

                            },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun CampoEquipo(
    etiqueta: String,
    valor: String,
    error: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    onCambio: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        supportingText = error?.let { texto ->
            { Text(texto, color = MaterialTheme.colorScheme.error) }
        },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        modifier = Modifier.fillMaxWidth(),
        colors = EstilosCampo.colores(),
        shape = EstilosCampo.forma
    )
}

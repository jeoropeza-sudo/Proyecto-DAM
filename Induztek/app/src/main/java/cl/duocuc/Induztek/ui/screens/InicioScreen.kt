package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.EstadoProtocolo
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    onAbrirPortalCliente: () -> Unit,
    onAbrirProtocolo: () -> Unit,
    onAbrirHistorial: () -> Unit,
    onAbrirAsignaciones: () -> Unit
) {
    val historial by viewModel.historial.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val esCliente = usuarioActivo?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)
    val esTecnico = usuarioActivo?.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)
    val puedeAsignar = usuarioActivo?.rolCodigo.equals(
        Rol.CODIGO_INGENIERO,
        ignoreCase = true
    ) || usuarioActivo?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)
    val protocolosVisibles = if (esCliente) {
        historial.filter {
            val estado = EstadoProtocolo.desdeCodigo(it.estado)
            estado != EstadoProtocolo.BORRADOR && estado != EstadoProtocolo.ANULADO
        }
    } else {
        historial
    }
    var clientesSinEmpresa by remember { mutableStateOf(emptyList<Usuario>()) }
    val empresas = remember(usuarioActivo?.idUsuario, puedeAsignar) {
        viewModel.empresasParaAsociarCliente(usuarioActivo)
    }
    var idClienteSeleccionado by remember { mutableStateOf<Long?>(null) }
    var idEmpresaSeleccionada by remember { mutableStateOf<Long?>(null) }
    var menuClientesAbierto by remember { mutableStateOf(false) }
    var menuEmpresasAbierto by remember { mutableStateOf(false) }
    var mensajeAsociacion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(usuarioActivo?.idUsuario) {
        usuarioActivo?.let {
            viewModel.inicializarParaUsuario(it)
            viewModel.cargarHistorialEquipo(0L, it)
            clientesSinEmpresa = viewModel.clientesSinEmpresa(it)
            idClienteSeleccionado = clientesSinEmpresa.firstOrNull()?.idUsuario
            idEmpresaSeleccionada = empresas.firstOrNull()?.idEmpresa
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Inicio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Hola, ${usuarioActivo?.nombre.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (esCliente && usuarioActivo?.idEmpresaCliente == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Cuenta pendiente de asociación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tu cuenta aún no está vinculada a una empresa cliente. " +
                                    "Contacta a Ingeniería o Jefatura Técnica para que revisen y asocien tu acceso."
                        )
                        Text(
                            "La app no permite solicitar servicios ni crear ubicaciones. " +
                                    "Cuando la cuenta esté asociada, podrás consultar los protocolos autorizados."
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (esCliente) "Resumen de tu empresa" else "Resumen de trabajo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (esTecnico && uiState.instalacionesDisponibles.isEmpty()) {
                            Text("Aún no tienes instalaciones asignadas.")
                        } else {
                            Text("Protocolos disponibles: ${protocolosVisibles.size}")
                            if (esCliente) {
                                val pendientes = protocolosVisibles.count {
                                    EstadoProtocolo.desdeCodigo(it.estado) in setOf(
                                        EstadoProtocolo.REGISTRADO_TERRENO,
                                        EstadoProtocolo.OBSERVADO
                                    )
                                }
                                Text("Pendientes de formalización: $pendientes")
                            } else if (esTecnico) {
                                Text("Instalaciones asignadas: ${uiState.instalacionesDisponibles.size}")
                            }
                        }
                    }
                }
            }
        }

        if (puedeAsignar && clientesSinEmpresa.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Asociar cuenta cliente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Vincula una cuenta nueva a su empresa para habilitar únicamente los datos de esa empresa."
                        )
                        ExposedDropdownMenuBox(
                            expanded = menuClientesAbierto,
                            onExpandedChange = { menuClientesAbierto = !menuClientesAbierto },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = clientesSinEmpresa.firstOrNull {
                                    it.idUsuario == idClienteSeleccionado
                                }?.let { "${it.nombre} · ${it.correo}" }.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cuenta cliente") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(menuClientesAbierto)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = menuClientesAbierto,
                                onDismissRequest = { menuClientesAbierto = false }
                            ) {
                                clientesSinEmpresa.forEach { cliente ->
                                    DropdownMenuItem(
                                        text = { Text("${cliente.nombre} · ${cliente.correo}") },
                                        onClick = {
                                            idClienteSeleccionado = cliente.idUsuario
                                            menuClientesAbierto = false
                                        }
                                    )
                                }
                            }
                        }
                        ExposedDropdownMenuBox(
                            expanded = menuEmpresasAbierto,
                            onExpandedChange = { menuEmpresasAbierto = !menuEmpresasAbierto },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = empresas.firstOrNull {
                                    it.idEmpresa == idEmpresaSeleccionada
                                }?.razonSocial.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Empresa verificada") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(menuEmpresasAbierto)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = menuEmpresasAbierto,
                                onDismissRequest = { menuEmpresasAbierto = false }
                            ) {
                                empresas.forEach { empresa ->
                                    DropdownMenuItem(
                                        text = { Text(empresa.razonSocial) },
                                        onClick = {
                                            idEmpresaSeleccionada = empresa.idEmpresa
                                            menuEmpresasAbierto = false
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                val idCliente = idClienteSeleccionado
                                val idEmpresa = idEmpresaSeleccionada
                                val asociada = idCliente != null && idEmpresa != null &&
                                        viewModel.asociarClienteAEmpresa(
                                            idCliente,
                                            idEmpresa,
                                            usuarioActivo
                                        )
                                if (asociada) {
                                    clientesSinEmpresa = viewModel.clientesSinEmpresa(usuarioActivo)
                                    idClienteSeleccionado = clientesSinEmpresa.firstOrNull()?.idUsuario
                                    mensajeAsociacion = "Cuenta asociada a la empresa."
                                } else {
                                    mensajeAsociacion = "No se pudo asociar la cuenta."
                                }
                            },
                            enabled = idClienteSeleccionado != null &&
                                    idEmpresaSeleccionada != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Asociar cuenta")
                        }
                        mensajeAsociacion?.let { Text(it) }
                    }
                }
            }
        }

        item {
            Text(
                "Accesos rápidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (esCliente) {
                    Button(
                        onClick = onAbrirPortalCliente,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = usuarioActivo?.idEmpresaCliente != null
                    ) {
                        Text("Consultar protocolos y equipos")
                    }
                } else {
                    Button(onClick = onAbrirProtocolo, modifier = Modifier.fillMaxWidth()) {
                        Text("Abrir registro de protocolos")
                    }
                    Button(onClick = onAbrirHistorial, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver historial")
                    }
                    if (puedeAsignar) {
                        Button(
                            onClick = onAbrirAsignaciones,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Administrar técnicos e instalaciones")
                        }
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (esCliente) {
                        "Solo se muestra información de tu empresa y los protocolos permitidos."
                    } else {
                        "Usa las secciones de la barra inferior para continuar tu trabajo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

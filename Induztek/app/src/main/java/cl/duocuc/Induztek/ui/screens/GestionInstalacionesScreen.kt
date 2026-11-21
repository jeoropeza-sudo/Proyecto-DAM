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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.BotonSecundario
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.viewmodel.ProtocoloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionInstalacionesScreen(
    viewModel: ProtocoloViewModel,
    usuarioActivo: Usuario?,
    onVolver: () -> Unit
) {
    val clientes = remember(usuarioActivo?.idUsuario) {
        viewModel.clientesConEmpresaParaInstalaciones(usuarioActivo)
    }
    var clienteSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var menuExpandido by remember { mutableStateOf(false) }
    var nombreInstalacion by remember { mutableStateOf("") }
    var errorNombre by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableStateOf(0) }
    val clienteSeleccionado = clientes.firstOrNull { it.idUsuario == clienteSeleccionadoId }
    val instalaciones = remember(clienteSeleccionadoId, recarga) {
        clienteSeleccionadoId?.let {
            viewModel.instalacionesDeClienteParaGestion(it, usuarioActivo)
        }.orEmpty()
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
                            Text(
                                "• ${instalacion.nombre}",
                                modifier = Modifier.padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                                    viewModel.crearInstalacionParaCliente(
                                        clienteSeleccionado.idUsuario,
                                        nombreLimpio,
                                        usuarioActivo
                                    ) -> {
                                        nombreInstalacion = ""
                                        errorNombre = null
                                        mensaje = "Instalación creada. Ya puedes asignarla a un técnico."
                                        recarga += 1
                                    }
                                    else -> {
                                        mensaje = "No se pudo crear la instalación. Comprueba los datos e inténtalo nuevamente."
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

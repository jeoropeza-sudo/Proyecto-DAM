package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.util.ValidarRut
import cl.duocuc.Induztek.util.ValidarTelefonoChile
import cl.duocuc.Induztek.viewmodel.UsuariosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    viewModel: UsuariosViewModel,
    onRegistroExitoso: () -> Unit,
    onVolverLogin: () -> Unit
) {
    var rut by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    val preguntasDisponibles = listOf(
        "¿Nombre de tu primera mascota?",
        "¿Ciudad donde naciste?",
        "¿Nombre de tu colegio de infancia?",
        "¿Marca de tu primer automóvil?"
    )

    var preguntaSeleccionada by remember { mutableStateOf(preguntasDisponibles.first()) }
    var respuestaSeguridad by remember { mutableStateOf("") }
    var expandidoPregunta by remember { mutableStateOf(false) }

    var mensajeError by remember { mutableStateOf<String?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CREAR CUENTA CLIENTE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Completa tus datos para acceder al portal de clientes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            OutlinedTextField(
                value = rut,
                onValueChange = { rut = it },
                label = { Text("RUT Empresa / Usuario *") },
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo o Razón Social *") },
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma
            )

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo Electrónico *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono de Contacto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma,
                isError = telefono.isNotBlank() && !ValidarTelefonoChile.esValido(telefono),
                supportingText = {
                    Text(
                        if (telefono.isNotBlank() && !ValidarTelefonoChile.esValido(telefono)) {
                            "Ingresa un móvil válido: 9XXXXXXXX o +569XXXXXXXX."
                        } else {
                            "Opcional. Formato móvil chileno: 9XXXXXXXX o +569XXXXXXXX."
                        },
                        color = if (telefono.isNotBlank() && !ValidarTelefonoChile.esValido(telefono)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )

            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text("Contraseña *") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma
            )

            ExposedDropdownMenuBox(
                expanded = expandidoPregunta,
                onExpandedChange = { expandidoPregunta = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = preguntaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pregunta de Seguridad *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoPregunta) },
                    shape = EstilosCampo.forma,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expandidoPregunta,
                    onDismissRequest = { expandidoPregunta = false }
                ) {
                    preguntasDisponibles.forEach { preg ->
                        DropdownMenuItem(
                            text = { Text(preg) },
                            onClick = {
                                preguntaSeleccionada = preg
                                expandidoPregunta = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = respuestaSeguridad,
                onValueChange = { respuestaSeguridad = it },
                label = { Text("Respuesta Secreta *") },
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma
            )

            if (mensajeError != null) {
                Text(mensajeError!!, color = ErrorColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val rutLimpio = rut.trim()
                    when {
                        rutLimpio.isBlank() || nombre.isBlank() || correo.isBlank() || clave.isBlank() || respuestaSeguridad.isBlank() -> {
                            mensajeError = "Por favor complete todos los campos obligatorios."
                        }
                        !ValidarRut.validarRut(rutLimpio) -> {
                            mensajeError = "El RUT ingresado es inválido (Módulo 11)."
                        }
                        telefono.isNotBlank() && !ValidarTelefonoChile.esValido(telefono) -> {
                            mensajeError = "Ingresa un teléfono móvil chileno válido."
                        }
                        else -> {
                            val telefonoNormalizado = if (telefono.isBlank()) "" else {
                                ValidarTelefonoChile.normalizar(telefono).orEmpty()
                            }
                            val exito = viewModel.registrar(
                                rut = rutLimpio,
                                nombre = nombre,
                                correo = correo,
                                tel = telefonoNormalizado,
                                clave = clave,
                                rol = "CLIENTE",
                                preg = preguntaSeleccionada,
                                resp = respuestaSeguridad
                            )
                            if (exito) onRegistroExitoso() else mensajeError = "No se pudo registrar la cuenta."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("REGISTRAR CUENTA")
            }

            TextButton(onClick = onVolverLogin) {
                Text("¿Ya tienes cuenta? Inicia sesión aquí", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
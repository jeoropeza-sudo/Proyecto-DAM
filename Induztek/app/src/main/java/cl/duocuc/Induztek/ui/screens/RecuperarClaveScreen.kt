package cl.duocuc.Induztek.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.TopAppBarDefaults
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
import cl.duocuc.Induztek.model.PreguntasSeguridad
import cl.duocuc.Induztek.model.ValidarContrasena
import cl.duocuc.Induztek.ui.components.MarcaCompacta
import cl.duocuc.Induztek.viewmodel.UsuariosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarClaveScreen(
    viewModel: UsuariosViewModel,
    onClaveActualizada: () -> Unit,
    onVolverLogin: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var puedeRecuperar by remember { mutableStateOf(false) }
    var preguntaSeleccionada by remember { mutableStateOf<String?>(null) }
    var expandidoPregunta by remember { mutableStateOf(false) }
    var respuesta by remember { mutableStateOf("") }
    var nuevaClave by remember { mutableStateOf("") }
    var repetirClave by remember { mutableStateOf("") }
    var errorCorreo by remember { mutableStateOf<String?>(null) }
    var errorRespuesta by remember { mutableStateOf<String?>(null) }
    var errorNuevaClave by remember { mutableStateOf<String?>(null) }
    var errorRepetirClave by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    val seguridad = ValidarContrasena.nivelSeguridad(nuevaClave)
    val claveEvaluada = nuevaClave.trim()
    val cumpleLongitud = claveEvaluada.length >= ValidarContrasena.LONGITUD_MINIMA
    val tieneMayuscula = claveEvaluada.any(Char::isUpperCase)
    val tieneNumero = claveEvaluada.any(Char::isDigit)
    val tieneEspecial = claveEvaluada.any { !it.isLetterOrDigit() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { MarcaCompacta(subtitulo = "Recuperar contraseña", sobreFondoOscuro = true) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (mostrarConfirmacion) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacion = false },
                title = { Text("Contraseña actualizada") },
                text = { Text("Ya puedes iniciar sesión con tu nueva contraseña.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mostrarConfirmacion = false
                            onClaveActualizada()
                        }
                    ) {
                        Text("Ir a iniciar sesión")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        "Recupera el acceso",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Verifica tu identidad para establecer una nueva contraseña.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    puedeRecuperar = false
                    preguntaSeleccionada = null
                    respuesta = ""
                    nuevaClave = ""
                    repetirClave = ""
                    errorCorreo = null
                    errorRespuesta = null
                    errorNuevaClave = null
                    errorRepetirClave = null
                    expandidoPregunta = false
                },
                label = { Text("Correo Electrónico Registrado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errorCorreo != null,
                supportingText = { ErrorInline(errorCorreo) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val correoIngresado = correo.trim()
                    when {
                        correoIngresado.isBlank() -> {
                            puedeRecuperar = false
                            errorCorreo = "Ingresa el correo asociado a tu cuenta."
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(correoIngresado).matches() -> {
                            puedeRecuperar = false
                            errorCorreo = "Ingresa un correo válido, por ejemplo nombre@empresa.cl."
                        }
                        viewModel.existeUsuarioActivo(correoIngresado) -> {
                            correo = correoIngresado
                            puedeRecuperar = true
                            preguntaSeleccionada = null
                            respuesta = ""
                            nuevaClave = ""
                            repetirClave = ""
                            errorCorreo = null
                            errorRespuesta = null
                            errorNuevaClave = null
                            errorRepetirClave = null
                        }
                        else -> {
                            puedeRecuperar = false
                            errorCorreo = "No encontramos una cuenta activa con ese correo. Verifica que esté escrito correctamente."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CONTINUAR")
            }

            if (puedeRecuperar) {
                ExposedDropdownMenuBox(
                    expanded = expandidoPregunta,
                    onExpandedChange = { expandidoPregunta = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = preguntaSeleccionada.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Elige nuevamente tu pregunta de seguridad") },
                        placeholder = { Text("Selecciona una pregunta") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoPregunta)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandidoPregunta,
                        onDismissRequest = { expandidoPregunta = false }
                    ) {
                        PreguntasSeguridad.disponibles.forEach { pregunta ->
                            DropdownMenuItem(
                                text = { Text(pregunta) },
                                onClick = {
                                    preguntaSeleccionada = pregunta
                                    expandidoPregunta = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = respuesta,
                    onValueChange = {
                        respuesta = it
                        errorRespuesta = null
                    },
                    label = { Text("Respuesta de seguridad") },
                    isError = errorRespuesta != null,
                    supportingText = { ErrorInline(errorRespuesta) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nuevaClave,
                    onValueChange = {
                        nuevaClave = it
                        errorNuevaClave = null
                    },
                    label = { Text("Nueva contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = errorNuevaClave != null,
                    supportingText = { ErrorInline(errorNuevaClave) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = when {
                        seguridad == 4 -> "Seguridad: Fuerte"
                        seguridad >= 3 -> "Seguridad: Media"
                        else -> "Seguridad: Débil"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        seguridad == 4 -> MaterialTheme.colorScheme.primary
                        seguridad >= 3 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                androidx.compose.material3.LinearProgressIndicator(
                    progress = seguridad / 4f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${if (cumpleLongitud) "✓" else "○"} ${ValidarContrasena.LONGITUD_MINIMA} caracteres · " +
                        "${if (tieneMayuscula) "✓" else "○"} mayúscula · " +
                        "${if (tieneNumero) "✓" else "○"} número · " +
                        "${if (tieneEspecial) "✓" else "○"} carácter especial",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = repetirClave,
                    onValueChange = {
                        repetirClave = it
                        errorRepetirClave = null
                    },
                    label = { Text("Repetir contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = errorRepetirClave != null,
                    supportingText = { ErrorInline(errorRepetirClave) },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        when {
                            preguntaSeleccionada == null -> {
                                errorRespuesta = "Selecciona la pregunta de seguridad que registraste."
                            }
                            respuesta.isBlank() -> {
                                errorRespuesta = "Ingresa la respuesta de seguridad."
                            }
                            !ValidarContrasena.esSegura(nuevaClave) -> {
                                errorNuevaClave =
                                    "La contraseña debe tener mínimo ${ValidarContrasena.LONGITUD_MINIMA} caracteres, mayúscula, número y carácter especial."
                            }
                            nuevaClave != repetirClave -> {
                                errorRepetirClave = "Las contraseñas no coinciden. Repite la nueva contraseña."
                            }
                            viewModel.recuperarClaveConRespuesta(
                                correo,
                                preguntaSeleccionada.orEmpty(),
                                respuesta,
                                nuevaClave
                            ) -> {
                                errorRespuesta = null
                                errorNuevaClave = null
                                errorRepetirClave = null
                                mostrarConfirmacion = true
                            }
                            else -> {
                                errorRespuesta = "La pregunta o respuesta no coincide. Verifica tus datos."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ACTUALIZAR CONTRASEÑA")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onVolverLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Volver al Inicio de Sesión", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ErrorInline(error: String?) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(tween(160)) + expandVertically(tween(180)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
    ) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }
    val claveEvaluada = nuevaClave.trim()
    val cumpleLongitud = claveEvaluada.length >= ValidarContrasena.LONGITUD_MINIMA
    val tieneMayuscula = claveEvaluada.any { it.isUpperCase() }
    val tieneNumero = claveEvaluada.any { it.isDigit() }
    val tieneEspecial = claveEvaluada.any { !it.isLetterOrDigit() }
    val seguridad = ValidarContrasena.nivelSeguridad(nuevaClave)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MarcaCompacta(subtitulo = "Recuperar contraseña", sobreFondoOscuro = true)
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
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                    mensaje = null
                    expandidoPregunta = false
                },
                label = { Text("Correo Electrónico Registrado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (correo.isBlank()) {
                        puedeRecuperar = false
                        mensaje = "Ingresa el correo registrado."
                        esError = true
                    } else if (viewModel.existeUsuarioActivo(correo)) {
                        puedeRecuperar = true
                        preguntaSeleccionada = null
                        respuesta = ""
                        nuevaClave = ""
                        repetirClave = ""
                        mensaje = null
                        esError = false
                    } else {
                        puedeRecuperar = false
                        preguntaSeleccionada = null
                        mensaje = "No se encontró ningún usuario activo con ese correo."
                        esError = true
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
                        value = preguntaSeleccionada ?: "",
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
                    onValueChange = { respuesta = it },
                    label = { Text("Su Respuesta") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nuevaClave,
                    onValueChange = { nuevaClave = it },
                    label = { Text("Nueva Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                LinearProgressIndicator(
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
                    onValueChange = { repetirClave = it },
                    label = { Text("Repetir contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = repetirClave.isNotEmpty() && repetirClave != nuevaClave,
                    supportingText = {
                        if (repetirClave.isNotEmpty() && repetirClave != nuevaClave) {
                            Text("Las contraseñas no coinciden.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val pregunta = preguntaSeleccionada
                        when {
                            pregunta == null -> {
                                mensaje = "Selecciona tu pregunta de seguridad."
                                esError = true
                            }
                            respuesta.isBlank() -> {
                                mensaje = "Ingresa la respuesta de seguridad."
                                esError = true
                            }
                            !ValidarContrasena.esSegura(nuevaClave) -> {
                                mensaje = "La contraseña debe tener mínimo ${ValidarContrasena.LONGITUD_MINIMA} caracteres, mayúscula, número y carácter especial."
                                esError = true
                            }
                            nuevaClave != repetirClave -> {
                                mensaje = "Las contraseñas no coinciden."
                                esError = true
                            }
                            else -> {
                                val exito = viewModel.recuperarClaveConRespuesta(
                                    correo,
                                    pregunta,
                                    respuesta,
                                    nuevaClave
                                )
                                if (exito) {
                                    mensaje = "Contraseña actualizada con éxito."
                                    esError = false
                                    onClaveActualizada()
                                } else {
                                    mensaje = "La pregunta o respuesta no coincide. Verifica tus datos."
                                    esError = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ACTUALIZAR CONTRASEÑA")
                }
            }

            val textoMensaje = mensaje
            if (textoMensaje != null) {
                Text(
                    text = textoMensaje,
                    color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
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
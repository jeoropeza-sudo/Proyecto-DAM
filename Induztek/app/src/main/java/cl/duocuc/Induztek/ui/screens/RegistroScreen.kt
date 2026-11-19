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
import cl.duocuc.Induztek.model.ValidarRut
import cl.duocuc.Induztek.model.ValidarTelefonoChile
import cl.duocuc.Induztek.model.PreguntasSeguridad
import cl.duocuc.Induztek.model.ValidarContrasena
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

    var preguntaSeleccionada by remember { mutableStateOf(PreguntasSeguridad.disponibles.first()) }
    var respuestaSeguridad by remember { mutableStateOf("") }
    var expandidoPregunta by remember { mutableStateOf(false) }

    var mensajeError by remember { mutableStateOf<String?>(null) }
    val rutNormalizado = rut.replace(".", "").replace("-", "").trim().uppercase()
    val rutError = when {
        rut.isBlank() -> null
        rutNormalizado.length < 2 -> "El RUT debe incluir el dígito verificador (ej.: 12.345.678-5)."
        !rutNormalizado.dropLast(1).all { it in '0'..'9' } ||
            rutNormalizado.last() !in '0'..'9' && rutNormalizado.last() != 'K' ->
            "El RUT debe tener números y terminar con un dígito verificador entre 0 y 9, o K."
        !ValidarRut.validarRut(rut) -> "El dígito verificador no corresponde al RUT. Revisa el último carácter."
        else -> null
    }
    val correoInvalido = correo.isNotBlank() &&
        !android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()
    val claveSegura = ValidarContrasena.esSegura(clave)
    val claveEvaluada = clave.trim()
    val cumpleLongitud = claveEvaluada.length >= ValidarContrasena.LONGITUD_MINIMA
    val tieneMayuscula = claveEvaluada.any { it.isUpperCase() }
    val tieneNumero = claveEvaluada.any { it.isDigit() }
    val tieneEspecial = claveEvaluada.any { !it.isLetterOrDigit() }
    val seguridad = ValidarContrasena.nivelSeguridad(clave)

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

            Text(
                "Datos de la cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rut,
                onValueChange = {
                    rut = it
                    mensajeError = null
                },
                label = { Text("RUT Empresa / Usuario *") },
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma,
                isError = rutError != null,
                supportingText = {
                    if (rutError != null) {
                        Text(rutError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Incluye el dígito verificador, por ejemplo 12.345.678-5.")
                    }
                }
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    mensajeError = null
                },
                label = { Text("Nombre Completo o Razón Social *") },
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma
            )

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    mensajeError = null
                },
                label = { Text("Correo Electrónico *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma,
                isError = correoInvalido,
                supportingText = {
                    if (correoInvalido) {
                        Text("Ingresa un correo válido, por ejemplo nombre@empresa.cl.")
                    }
                }
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = {
                    telefono = it
                    mensajeError = null
                },
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

            Text(
                "Seguridad de la cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            OutlinedTextField(
                value = clave,
                onValueChange = {
                    clave = it
                    mensajeError = null
                },
                label = { Text("Contraseña *") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = EstilosCampo.forma,
                isError = clave.isNotEmpty() && !claveSegura,
                supportingText = {
                    Text(
                        "Mínimo ${ValidarContrasena.LONGITUD_MINIMA} caracteres, una mayúscula, un número y un carácter especial.",
                        color = if (clave.isNotEmpty() && !claveSegura) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
            Text(
                text = when {
                    seguridad == 4 -> "Seguridad de contraseña: Fuerte"
                    seguridad >= 3 -> "Seguridad de contraseña: Media"
                    else -> "Seguridad de contraseña: Débil"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    seguridad == 4 -> MaterialTheme.colorScheme.primary
                    seguridad >= 3 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.fillMaxWidth()
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
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
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
                    PreguntasSeguridad.disponibles.forEach { preg ->
                        DropdownMenuItem(
                            text = { Text(preg) },
                            onClick = {
                                preguntaSeleccionada = preg
                                expandidoPregunta = false
                                mensajeError = null
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = respuestaSeguridad,
                onValueChange = {
                    respuestaSeguridad = it
                    mensajeError = null
                },
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
                        rutLimpio.isBlank() -> mensajeError = "Ingresa el RUT con su dígito verificador."
                        rutError != null -> mensajeError = rutError
                        nombre.isBlank() -> mensajeError = "Ingresa el nombre completo o la razón social."
                        correo.isBlank() -> mensajeError = "Ingresa un correo electrónico de contacto."
                        correoInvalido -> mensajeError = "El correo no tiene un formato válido. Usa nombre@empresa.cl."
                        clave.isBlank() -> mensajeError = "Ingresa una contraseña."
                        !claveSegura -> mensajeError =
                            "La contraseña debe tener mínimo ${ValidarContrasena.LONGITUD_MINIMA} caracteres, una mayúscula, un número y un carácter especial."
                        telefono.isNotBlank() && !ValidarTelefonoChile.esValido(telefono) -> {
                            mensajeError = "El teléfono debe tener formato 9XXXXXXXX o +569XXXXXXXX."
                        }
                        respuestaSeguridad.isBlank() -> {
                            mensajeError = "Ingresa una respuesta para la pregunta de seguridad."
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
                            if (exito) {
                                onRegistroExitoso()
                            } else {
                                mensajeError = "No se pudo crear la cuenta. Verifica si el RUT o el correo ya están registrados."
                            }
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
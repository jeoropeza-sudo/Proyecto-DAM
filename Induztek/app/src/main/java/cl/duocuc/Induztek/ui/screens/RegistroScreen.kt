package cl.duocuc.Induztek.ui.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import cl.duocuc.Induztek.model.PreguntasSeguridad
import cl.duocuc.Induztek.model.ValidarContrasena
import cl.duocuc.Induztek.model.ValidarRut
import cl.duocuc.Induztek.model.ValidarTelefonoChile
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.ui.theme.Dimens
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
    var respuestaSeguridad by remember { mutableStateOf("") }
    var preguntaSeleccionada by remember { mutableStateOf(PreguntasSeguridad.disponibles.first()) }
    var expandidoPregunta by remember { mutableStateOf(false) }
    var erroresCampos by remember { mutableStateOf(emptyMap<String, String>()) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    val seguridad = ValidarContrasena.nivelSeguridad(clave)
    val claveEvaluada = clave.trim()
    val cumpleLongitud = claveEvaluada.length >= ValidarContrasena.LONGITUD_MINIMA
    val tieneMayuscula = claveEvaluada.any(Char::isUpperCase)
    val tieneNumero = claveEvaluada.any(Char::isDigit)
    val tieneEspecial = claveEvaluada.any { !it.isLetterOrDigit() }

    Scaffold { padding ->
        if (mostrarConfirmacion) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacion = false },
                title = { Text("Cuenta creada") },
                text = {
                    Text(
                        "Tu cuenta quedó registrada. Ingeniería o Jefatura Técnica debe vincularla a tu empresa antes de que puedas consultar sus protocolos."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mostrarConfirmacion = false
                            onRegistroExitoso()
                        }
                    ) {
                        Text("Ir al inicio de sesión")
                    }
                }
            )
        }

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
                        "CREAR CUENTA CLIENTE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Completa tus datos para acceder al portal de clientes.",
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
            CampoRegistro(
                valor = rut,
                etiqueta = "RUT Empresa / Usuario *",
                error = erroresCampos["rut"],
                ayuda = "Incluye el dígito verificador, por ejemplo 12.345.678-5.",
                teclado = KeyboardType.Text,
                onValorCambia = { rut = it; erroresCampos = erroresCampos - "rut" }
            )
            CampoRegistro(
                valor = nombre,
                etiqueta = "Nombre Completo o Razón Social *",
                error = erroresCampos["nombre"],
                onValorCambia = { nombre = it; erroresCampos = erroresCampos - "nombre" }
            )
            CampoRegistro(
                valor = correo,
                etiqueta = "Correo Electrónico *",
                error = erroresCampos["correo"],
                teclado = KeyboardType.Email,
                onValorCambia = { correo = it; erroresCampos = erroresCampos - "correo" }
            )
            CampoRegistro(
                valor = telefono,
                etiqueta = "Teléfono de Contacto",
                error = erroresCampos["telefono"],
                ayuda = "Opcional. Formato móvil chileno: 9XXXXXXXX o +569XXXXXXXX.",
                teclado = KeyboardType.Phone,
                onValorCambia = { telefono = it; erroresCampos = erroresCampos - "telefono" }
            )

            Text(
                "Seguridad de la cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            CampoRegistro(
                valor = clave,
                etiqueta = "Contraseña *",
                error = erroresCampos["clave"],
                ayuda = "Mínimo ${ValidarContrasena.LONGITUD_MINIMA} caracteres, una mayúscula, un número y un carácter especial.",
                teclado = KeyboardType.Password,
                transformacion = PasswordVisualTransformation(),
                onValorCambia = { clave = it; erroresCampos = erroresCampos - "clave" }
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
            CampoRegistro(
                valor = respuestaSeguridad,
                etiqueta = "Respuesta Secreta *",
                error = erroresCampos["respuesta"],
                onValorCambia = {
                    respuestaSeguridad = it
                    erroresCampos = erroresCampos - "respuesta"
                }
            )

            Button(
                onClick = {
                    val nuevosErrores = buildMap {
                        val rutNormalizado = rut.replace(".", "").replace("-", "").trim().uppercase()
                        when {
                            rut.isBlank() -> put("rut", "Ingresa el RUT con su dígito verificador.")
                            rutNormalizado.length < 2 ->
                                put("rut", "El RUT debe incluir el dígito verificador, por ejemplo 12.345.678-5.")
                            !rutNormalizado.dropLast(1).all { it in '0'..'9' } ||
                                rutNormalizado.last() !in '0'..'9' && rutNormalizado.last() != 'K' ->
                                put("rut", "El RUT debe tener números y terminar con un dígito verificador entre 0 y 9, o K.")
                            !ValidarRut.validarRut(rut) ->
                                put("rut", "El dígito verificador no corresponde al RUT. Revisa el último carácter.")
                        }
                        if (nombre.isBlank()) put("nombre", "Ingresa el nombre completo o la razón social.")
                        when {
                            correo.isBlank() -> put("correo", "Ingresa un correo electrónico de contacto.")
                            !Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() ->
                                put("correo", "Ingresa un correo válido, por ejemplo nombre@empresa.cl.")
                        }
                        if (telefono.isNotBlank() && !ValidarTelefonoChile.esValido(telefono)) {
                            put("telefono", "El teléfono debe tener formato 9XXXXXXXX o +569XXXXXXXX.")
                        }
                        when {
                            clave.isBlank() -> put("clave", "Ingresa una contraseña.")
                            !ValidarContrasena.esSegura(clave) ->
                                put(
                                    "clave",
                                    "La contraseña debe tener mínimo ${ValidarContrasena.LONGITUD_MINIMA} caracteres, una mayúscula, un número y un carácter especial."
                                )
                        }
                        if (respuestaSeguridad.isBlank()) {
                            put("respuesta", "Ingresa una respuesta para la pregunta de seguridad.")
                        }
                    }
                    erroresCampos = nuevosErrores
                    if (nuevosErrores.isEmpty()) {
                        val telefonoNormalizado =
                            if (telefono.isBlank()) "" else ValidarTelefonoChile.normalizar(telefono).orEmpty()
                        val exito = viewModel.registrar(
                            rut = rut.trim(),
                            nombre = nombre.trim(),
                            correo = correo.trim(),
                            tel = telefonoNormalizado,
                            clave = clave,
                            rol = "CLIENTE",
                            preg = preguntaSeleccionada,
                            resp = respuestaSeguridad
                        )
                        if (exito) {
                            mostrarConfirmacion = true
                        } else {
                            erroresCampos = mapOf(
                                "rut" to "No se pudo registrar. Comprueba que este RUT no esté asociado a otra cuenta.",
                                "correo" to "No se pudo registrar. Comprueba que este correo no esté asociado a otra cuenta."
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("REGISTRAR CUENTA")
            }

            TextButton(onClick = onVolverLogin) {
                Text("¿Ya tienes cuenta? Inicia sesión aquí", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CampoRegistro(
    valor: String,
    etiqueta: String,
    error: String?,
    onValorCambia: (String) -> Unit,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    transformacion: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        label = { Text(etiqueta) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        visualTransformation = transformacion,
        isError = error != null,
        modifier = Modifier.fillMaxWidth(),
        shape = EstilosCampo.forma,
        supportingText = {
            if (error != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(160)) + expandVertically(tween(180)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            } else if (ayuda != null) {
                Text(ayuda, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

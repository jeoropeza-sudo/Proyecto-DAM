package cl.duocuc.Induztek.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.ValidarContrasena
import cl.duocuc.Induztek.ui.components.MarcaCompacta
import cl.duocuc.Induztek.viewmodel.UsuariosViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecuperarClaveCorreoScreen(
    viewModel: UsuariosViewModel,
    onClaveActualizada: () -> Unit,
    onVolverLogin: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var claveNueva by remember { mutableStateOf("") }
    var repetirClave by remember { mutableStateOf("") }
    var errorValidacion by remember { mutableStateOf<String?>(null) }
    val estado by viewModel.recuperacion.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.limpiarRecuperacion()
    }

    if (estado.claveActualizada) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Contraseña actualizada") },
            text = { Text("Ya puedes iniciar sesión con tu nueva contraseña.") },
            confirmButton = {
                TextButton(onClick = onClaveActualizada) {
                    Text("Ir a iniciar sesión")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MarcaCompacta(
                        subtitulo = "Recuperar contraseña",
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Recupera el acceso por correo",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Te enviaremos un código temporal para validar el cambio de contraseña.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    errorValidacion = null
                },
                label = { Text("Correo electrónico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
                        errorValidacion = "Ingresa un correo válido."
                    } else {
                        errorValidacion = null
                        viewModel.solicitarCodigoRecuperacion(correo)
                    }
                },
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estado.cargando) "Enviando..." else "Enviar código")
            }

            if (estado.codigoSolicitado) {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = {
                        codigo = it
                        errorValidacion = null
                    },
                    label = { Text("Código recibido por correo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = claveNueva,
                    onValueChange = {
                        claveNueva = it
                        errorValidacion = null
                    },
                    label = { Text("Nueva contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = repetirClave,
                    onValueChange = {
                        repetirClave = it
                        errorValidacion = null
                    },
                    label = { Text("Repetir contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        when {
                            codigo.isBlank() -> errorValidacion = "Ingresa el código enviado a tu correo."
                            !ValidarContrasena.esSegura(claveNueva) ->
                                errorValidacion = "La contraseña no cumple los requisitos de seguridad."
                            claveNueva != repetirClave ->
                                errorValidacion = "Las contraseñas no coinciden."
                            else -> {
                                errorValidacion = null
                                viewModel.restablecerClaveConCodigo(correo, codigo, claveNueva)
                            }
                        }
                    },
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (estado.cargando) "Actualizando..." else "Actualizar contraseña")
                }
            }

            if (errorValidacion != null) {
                Text(
                    errorValidacion.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (estado.error != null) {
                Text(
                    estado.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (estado.mensaje != null) {
                Text(
                    estado.mensaje.orEmpty(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onVolverLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Volver al inicio de sesión")
            }
        }
    }
}

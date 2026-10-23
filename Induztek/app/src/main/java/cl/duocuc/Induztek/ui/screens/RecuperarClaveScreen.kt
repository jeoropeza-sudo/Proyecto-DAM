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
import cl.duocuc.Induztek.viewmodel.UsuariosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarClaveScreen(
    viewModel: UsuariosViewModel,
    onClaveActualizada: () -> Unit,
    onVolverLogin: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var preguntaSeguridad by remember { mutableStateOf<String?>(null) }
    var respuesta by remember { mutableStateOf("") }
    var nuevaClave by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recuperar Contraseña") },
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
                onValueChange = { correo = it.trim() },
                label = { Text("Correo Electrónico Registrado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val preg = viewModel.obtenerPreguntaSeguridad(correo)
                    if (preg != null) {
                        preguntaSeguridad = preg
                        mensaje = null
                    } else {
                        preguntaSeguridad = null
                        mensaje = "No se encontró ningún usuario activo con ese correo."
                        esError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("BUSCAR PREGUNTA DE SEGURIDAD")
            }

            if (preguntaSeguridad != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pregunta:", style = MaterialTheme.typography.labelSmall)
                        Text(preguntaSeguridad!!, style = MaterialTheme.typography.bodyMedium)
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
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val exito = viewModel.recuperarClaveConRespuesta(correo, respuesta, nuevaClave)
                        if (exito) {
                            mensaje = "Contraseña actualizada con éxito."
                            esError = false
                            onClaveActualizada()
                        } else {
                            mensaje = "Respuesta incorrecta. Verifique sus datos."
                            esError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ACTUALIZAR CONTRASEÑA")
                }
            }

            if (mensaje != null) {
                Text(
                    text = mensaje!!,
                    color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onVolverLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Volver al Inicio de Sesión")
            }
        }
    }
}
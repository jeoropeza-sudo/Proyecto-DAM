package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.model.ValidarContrasena
import cl.duocuc.Induztek.viewmodel.UsuariosViewModel

@Composable
fun CambioClaveObligatorioScreen(
    viewModel: UsuariosViewModel,
    usuarioActivo: Usuario?,
    onClaveCambiada: () -> Unit
) {
    var claveTemporal by remember { mutableStateOf("") }
    var claveNueva by remember { mutableStateOf("") }
    var repetirClave by remember { mutableStateOf("") }
    var errorTemporal by remember { mutableStateOf<String?>(null) }
    var errorNueva by remember { mutableStateOf<String?>(null) }
    var errorRepetir by remember { mutableStateOf<String?>(null) }
    var cambiando by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Cambia tu contraseña", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Por seguridad, debes reemplazar la contraseña temporal antes de usar la aplicación."
            )
            OutlinedTextField(
                value = claveTemporal,
                onValueChange = { claveTemporal = it; errorTemporal = null },
                label = { Text("Contraseña temporal") },
                visualTransformation = PasswordVisualTransformation(),
                isError = errorTemporal != null,
                supportingText = errorTemporal?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = claveNueva,
                onValueChange = { claveNueva = it; errorNueva = null },
                label = { Text("Nueva contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                isError = errorNueva != null,
                supportingText = errorNueva?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Mínimo ${ValidarContrasena.LONGITUD_MINIMA} caracteres, una mayúscula, un número y un carácter especial.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = repetirClave,
                onValueChange = { repetirClave = it; errorRepetir = null },
                label = { Text("Repetir nueva contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                isError = errorRepetir != null,
                supportingText = errorRepetir?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    errorTemporal = if (claveTemporal.isBlank()) "Ingresa la contraseña temporal." else null
                    errorNueva = when {
                        claveNueva.isBlank() -> "Ingresa una contraseña nueva."
                        !ValidarContrasena.esSegura(claveNueva) ->
                            "Debe cumplir los requisitos de seguridad indicados."
                        claveNueva == claveTemporal -> "La nueva contraseña debe ser distinta de la temporal."
                        else -> null
                    }
                    errorRepetir = when {
                        repetirClave.isBlank() -> "Repite la nueva contraseña."
                        repetirClave != claveNueva -> "Las contraseñas no coinciden."
                        else -> null
                    }
                    if (errorTemporal == null && errorNueva == null && errorRepetir == null) {
                        if (usuarioActivo == null) {
                            errorTemporal = "Inicia sesión nuevamente para cambiar la contraseña."
                        } else {
                            cambiando = true
                            viewModel.cambiarClave(claveTemporal, claveNueva) { error ->
                                cambiando = false
                                if (error == null) onClaveCambiada() else errorTemporal = error
                            }
                        }
                    }
                },
                enabled = !cambiando,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (cambiando) "ACTUALIZANDO..." else "CAMBIAR CONTRASEÑA")
            }
        }
    }
}

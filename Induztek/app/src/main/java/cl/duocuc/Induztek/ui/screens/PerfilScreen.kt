package cl.duocuc.Induztek.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.permisos.Permisos
import cl.duocuc.Induztek.data.ConvertidorImagen
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.BotonSecundario
import cl.duocuc.Induztek.ui.components.MarcaCompacta
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.theme.ErrorColor
import cl.duocuc.Induztek.ui.theme.Exito
import cl.duocuc.Induztek.util.ValidarTelefonoChile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    usuarioActivo: Usuario?,
    onCambiarFotoCamara: (Bitmap) -> Unit,
    onCambiarFotoGaleria: (Uri) -> Unit,
    onCambiarClave: (String, String) -> Boolean,
    onActualizarPerfil: (String, String) -> Boolean,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    var mostrarOpcionesFoto by remember { mutableStateOf(false) }
    var mensajePermiso by remember { mutableStateOf<String?>(null) }
    var permisoCamaraDenegado by remember { mutableStateOf(false) }
    var editarPerfil by remember { mutableStateOf(false) }
    var nombreEditado by remember(usuarioActivo?.idUsuario, usuarioActivo?.nombre) {
        mutableStateOf(usuarioActivo?.nombre.orEmpty())
    }
    var telefonoEditado by remember(usuarioActivo?.idUsuario, usuarioActivo?.telefono) {
        mutableStateOf(usuarioActivo?.telefono.orEmpty())
    }
    var errorPerfil by remember { mutableStateOf<String?>(null) }
    var mensajeExitoPerfil by remember { mutableStateOf<String?>(null) }

    var mostrarFormularioClave by remember { mutableStateOf(false) }
    var claveActual by remember { mutableStateOf("") }
    var claveNueva by remember { mutableStateOf("") }
    var claveConfirmar by remember { mutableStateOf("") }
    var errorClave by remember { mutableStateOf<String?>(null) }
    var mensajeExitoClave by remember { mutableStateOf<String?>(null) }

    val launcherCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onCambiarFotoCamara(bitmap)
        }
    }

    val launcherPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoCamaraDenegado = !concedido
        if (concedido) {
            mensajePermiso = null
            launcherCamara.launch()
        } else {
            mensajePermiso = "Se necesita permiso de cámara para tomar una foto. Puedes habilitarlo en Ajustes."
        }
    }

    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onCambiarFotoGaleria(uri)
        }
    }

    if (mostrarOpcionesFoto) {
        AlertDialog(
            onDismissRequest = { mostrarOpcionesFoto = false },
            title = { Text("Actualizar Foto de Perfil") },
            text = { Text("Seleccione la fuente para cambiar la foto:") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarOpcionesFoto = false
                    if (Permisos.tiene(context, Permisos.CAMARA)) {
                        launcherCamara.launch()
                    } else {
                        launcherPermisoCamara.launch(Permisos.CAMARA)
                    }
                }) { Text("Cámara", color = MaterialTheme.colorScheme.onSurface) }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarOpcionesFoto = false
                    launcherGaleria.launch("image/*")
                }) { Text("Galería", color = MaterialTheme.colorScheme.onSurface) }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MarcaCompacta(subtitulo = "Mi perfil", sobreFondoOscuro = true)
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
                .padding(Dimens.espacioPantalla),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(3.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                    .clickable { mostrarOpcionesFoto = true },
                contentAlignment = Alignment.Center
            ) {
                val bitmapFoto = usuarioActivo?.avatar?.let { ConvertidorImagen.aBitmap(it) }
                if (bitmapFoto != null) {
                    Image(
                        bitmap = bitmapFoto.asImageBitmap(),
                        contentDescription = "Foto Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_camera),
                        contentDescription = "Cámara",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Toca la imagen para cambiar foto de perfil",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (mensajePermiso != null) {
                Text(
                    text = mensajePermiso.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (permisoCamaraDenegado) {
                TextButton(onClick = { Permisos.abrirAjustes(context) }) {
                    Text("Abrir ajustes de cámara", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.espacioMedio),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Datos del usuario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()

                    if (editarPerfil) {
                        OutlinedTextField(
                            value = nombreEditado,
                            onValueChange = { nombreEditado = it; errorPerfil = null; mensajeExitoPerfil = null },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = EstilosCampo.forma,
                            colors = EstilosCampo.colores()
                        )
                        OutlinedTextField(
                            value = telefonoEditado,
                            onValueChange = { telefonoEditado = it; errorPerfil = null; mensajeExitoPerfil = null },
                            label = { Text("Teléfono") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = EstilosCampo.forma,
                            colors = EstilosCampo.colores(),
                            isError = telefonoEditado.isNotBlank() && !ValidarTelefonoChile.esValido(telefonoEditado),
                            supportingText = {
                                if (telefonoEditado.isNotBlank() && !ValidarTelefonoChile.esValido(telefonoEditado)) {
                                    Text(
                                        "Ingresa un móvil válido: 9XXXXXXXX o +569XXXXXXXX.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nombre:", fontWeight = FontWeight.Bold)
                            Text(usuarioActivo?.nombre ?: "N/A")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Teléfono:", fontWeight = FontWeight.Bold)
                            Text(usuarioActivo?.telefono ?: "N/A")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RUT:", fontWeight = FontWeight.Bold)
                        Text(usuarioActivo?.rut ?: "N/A")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Correo:", fontWeight = FontWeight.Bold)
                        Text(usuarioActivo?.correo ?: "N/A")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rol Sistema:", fontWeight = FontWeight.Bold)
                        Text(usuarioActivo?.rolCodigo ?: "N/A")
                    }
                    if (errorPerfil != null) {
                        Text(errorPerfil.orEmpty(), color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                    }
                    if (mensajeExitoPerfil != null) {
                        Text(mensajeExitoPerfil.orEmpty(), color = Exito, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonSecundario(
                            texto = if (editarPerfil) "Cancelar" else "Editar datos",
                            onClick = {
                                if (editarPerfil) {
                                    nombreEditado = usuarioActivo?.nombre.orEmpty()
                                    telefonoEditado = usuarioActivo?.telefono.orEmpty()
                                    errorPerfil = null
                                }
                                editarPerfil = !editarPerfil
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (editarPerfil) {
                            BotonPrincipal(
                                texto = "Guardar",
                                onClick = {
                                    when {
                                        nombreEditado.isBlank() -> errorPerfil = "Ingresa tu nombre."
                                        telefonoEditado.isBlank() -> errorPerfil = "Ingresa tu teléfono."
                                        !ValidarTelefonoChile.esValido(telefonoEditado) -> {
                                            errorPerfil = "Ingresa un teléfono móvil chileno válido."
                                        }
                                        onActualizarPerfil(nombreEditado, telefonoEditado) -> {
                                            telefonoEditado = ValidarTelefonoChile.normalizar(telefonoEditado).orEmpty()
                                            editarPerfil = false
                                            errorPerfil = null
                                            mensajeExitoPerfil = "Datos actualizados."
                                        }
                                        else -> errorPerfil = "No se pudieron guardar los datos."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            BotonPrincipal(
                texto = if (mostrarFormularioClave) "Ocultar Cambio de Contraseña" else "Cambiar Contraseña",
                onClick = {
                    mostrarFormularioClave = !mostrarFormularioClave
                    errorClave = null
                    mensajeExitoClave = null
                },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(
                visible = mostrarFormularioClave,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.espacioMedio),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Actualizar Contraseña",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = claveActual,
                            onValueChange = { claveActual = it; errorClave = null },
                            label = { Text("Contraseña Actual") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = EstilosCampo.forma,
                            colors = EstilosCampo.colores()
                        )

                        OutlinedTextField(
                            value = claveNueva,
                            onValueChange = { claveNueva = it; errorClave = null },
                            label = { Text("Nueva Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = EstilosCampo.forma,
                            colors = EstilosCampo.colores()
                        )

                        OutlinedTextField(
                            value = claveConfirmar,
                            onValueChange = { claveConfirmar = it; errorClave = null },
                            label = { Text("Confirmar Nueva Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = EstilosCampo.forma,
                            colors = EstilosCampo.colores()
                        )

                        if (errorClave != null) {
                            Text(text = errorClave!!, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                        }

                        if (mensajeExitoClave != null) {
                            Text(text = mensajeExitoClave!!, color = Exito, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        BotonPrincipal(
                            texto = "Confirmar Cambio",
                            onClick = {
                                when {
                                    claveActual.isBlank() || claveNueva.isBlank() || claveConfirmar.isBlank() -> {
                                        errorClave = "Complete todos los campos."
                                    }
                                    claveNueva != claveConfirmar -> {
                                        errorClave = "La contraseña nueva no coincide."
                                    }
                                    claveNueva.length < 4 -> {
                                        errorClave = "La contraseña debe tener al menos 4 caracteres."
                                    }
                                    else -> {
                                        val exito = onCambiarClave(claveActual, claveNueva)
                                        if (exito) {
                                            mensajeExitoClave = "¡Contraseña actualizada correctamente!"
                                            claveActual = ""
                                            claveNueva = ""
                                            claveConfirmar = ""
                                            errorClave = null
                                        } else {
                                            errorClave = "La contraseña actual es incorrecta."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            BotonSecundario(
                texto = "Cerrar Sesión",
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
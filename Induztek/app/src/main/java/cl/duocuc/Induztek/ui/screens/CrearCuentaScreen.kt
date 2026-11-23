package cl.duocuc.Induztek.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.model.CrearCuentaResultado
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.model.ValidarRut
import cl.duocuc.Induztek.model.ValidarTelefonoChile
import cl.duocuc.Induztek.ui.styles.EstilosCampo
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.BotonSecundario
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.viewmodel.UsuariosViewModel

private val rolesDisponibles = listOf(
    Rol.CODIGO_TECNICO to "Técnico",
    Rol.CODIGO_CLIENTE to "Cliente"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCuentaScreen(
    viewModel: UsuariosViewModel,
    usuarioActivo: Usuario?,
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit
) {
    var rut by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf(Rol.CODIGO_TECNICO) }
    var rolExpandido by remember { mutableStateOf(false) }
    var pasoEmpresa by remember { mutableStateOf(false) }
    var pasoInstalacion by remember { mutableStateOf(false) }
    var rutEmpresa by remember { mutableStateOf("") }
    var razonSocial by remember { mutableStateOf("") }
    var giro by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var contactoNombre by remember { mutableStateOf("") }
    var contactoCorreo by remember { mutableStateOf("") }
    var contactoTelefono by remember { mutableStateOf("") }
    var nombreInstalacion by remember { mutableStateOf("") }
    var errores by remember { mutableStateOf(emptyMap<String, String>()) }
    var resultado by remember { mutableStateOf<CrearCuentaResultado?>(null) }
    val estadoAltaRemota by viewModel.altaRemota.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.limpiarEstadoAltaRemota()
    }

    val esAdmin = usuarioActivo?.rolCodigo in setOf(
        Rol.CODIGO_INGENIERO,
        Rol.CODIGO_JEFE_TECNICO
    )
    val esCliente = rol == Rol.CODIGO_CLIENTE

    Scaffold { padding ->
        val alta = resultado
        if (alta != null && alta.exitosa) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Cuenta creada") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Las credenciales de acceso se generan y envían por correo cuando el servidor confirma el alta."
                        )
                        if (estadoAltaRemota.cargando) {
                            Text("Sincronizando la cuenta con el servidor.")
                            CircularProgressIndicator()
                        }
                        val mensajeAlta = estadoAltaRemota.mensaje
                        if (!mensajeAlta.isNullOrBlank()) {
                            Text(mensajeAlta)
                        }
                        val errorAlta = estadoAltaRemota.error
                        if (!errorAlta.isNullOrBlank()) {
                            Text(
                                errorAlta,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = { viewModel.reintentarAltaRemota() }
                            ) {
                                Text("Reintentar sincronización")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.limpiarEstadoAltaRemota()
                        onRegistroExitoso()
                    }) { Text("Volver al inicio") }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        when {
                            pasoInstalacion -> "PRIMERA INSTALACIÓN"
                            pasoEmpresa -> "DATOS DE LA EMPRESA"
                            else -> "CREAR CUENTA"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        when {
                            pasoInstalacion -> "Registra una planta, sucursal o instalación para asignarla a un técnico."
                            pasoEmpresa -> "Registra la empresa asociada a la cuenta cliente."
                            else -> "Completa los datos de la cuenta y selecciona Técnico o Cliente."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (!esAdmin) {
                Text("Solo Ingeniería o Jefatura Técnica pueden crear cuentas.")
            } else if (!pasoEmpresa) {
                CampoAdmin("RUT personal *", rut, errores["rut"], KeyboardType.Text) { rut = it }
                CampoAdmin("Nombre y apellido *", nombre, errores["nombre"]) { nombre = it }
                CampoAdmin("Correo electrónico *", correo, errores["correo"], KeyboardType.Email) { correo = it }
                CampoAdmin("Teléfono", telefono, errores["telefono"], KeyboardType.Phone) { telefono = it }

                ExposedDropdownMenuBox(
                    expanded = rolExpandido,
                    onExpandedChange = { rolExpandido = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = rolesDisponibles.first { it.first == rol }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rolExpandido) },
                        isError = errores.containsKey("rol"),
                        supportingText = errores["rol"]?.let { mensaje -> { Text(mensaje, color = MaterialTheme.colorScheme.error) } },
                        shape = EstilosCampo.forma,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = rolExpandido,
                        onDismissRequest = { rolExpandido = false }
                    ) {
                        rolesDisponibles.forEach { (codigo, etiqueta) ->
                            DropdownMenuItem(
                                text = { Text(etiqueta) },
                                onClick = {
                                    rol = codigo
                                    rolExpandido = false
                                    errores = errores - "rol"
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonSecundario(
                        texto = "Cancelar",
                        onClick = onVolver,
                        modifier = Modifier.weight(0.8f)
                    )
                    BotonPrincipal(
                        texto = if (esCliente) "Continuar" else "Crear cuenta",
                        onClick = {
                            var nuevosErrores = validarCuenta(rut, nombre, correo, telefono)
                            if (nuevosErrores.isEmpty()) {
                                nuevosErrores = viewModel.validarDisponibilidadCuenta(rut, correo)
                            }
                            errores = nuevosErrores
                            if (nuevosErrores.isEmpty()) {
                                if (esCliente) {
                                    pasoEmpresa = true
                                } else {
                                    val alta = crearCuenta(
                                        viewModel, usuarioActivo, rut, nombre, correo, telefono, rol
                                    )
                                    resultado = alta
                                    errores = if (alta.exitosa) emptyMap() else mapOf(
                                        (alta.campoError ?: "general") to alta.mensajeError.orEmpty()
                                    )
                                    if (alta.campoError == "rut" || alta.campoError == "correo") {
                                        pasoEmpresa = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        habilitado = esAdmin
                    )
                }
            } else if (!pasoInstalacion) {
                CampoAdmin("RUT de empresa *", rutEmpresa, errores["rutEmpresa"], KeyboardType.Text) { rutEmpresa = it }
                CampoAdmin("Razón social *", razonSocial, errores["razonSocial"]) { razonSocial = it }
                CampoAdmin("Giro *", giro, errores["giro"]) { giro = it }
                CampoAdmin("Dirección *", direccion, errores["direccion"]) { direccion = it }
                Spacer(Modifier.height(4.dp))
                Text("Contacto de la empresa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                CampoAdmin("Nombre del contacto *", contactoNombre, errores["contactoNombre"]) { contactoNombre = it }
                CampoAdmin("Correo del contacto *", contactoCorreo, errores["contactoCorreo"], KeyboardType.Email) { contactoCorreo = it }
                CampoAdmin("Teléfono del contacto *", contactoTelefono, errores["contactoTelefono"], KeyboardType.Phone) { contactoTelefono = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonSecundario(
                        texto = "Atrás",
                        onClick = {
                            pasoEmpresa = false
                            pasoInstalacion = false
                        },
                        modifier = Modifier.weight(0.8f)
                    )
                    BotonPrincipal(
                        texto = "Crear cuenta",
                        onClick = {
                            val nuevosErrores = validarEmpresa(
                                rutEmpresa, razonSocial, giro, direccion,
                                contactoNombre, contactoCorreo, contactoTelefono
                            )
                            val erroresCompletos = if (nuevosErrores.isEmpty()) {
                                viewModel.validarDisponibilidadEmpresa(rutEmpresa, razonSocial)
                            } else {
                                nuevosErrores
                            }
                            errores = erroresCompletos
                            if (erroresCompletos.isEmpty()) pasoInstalacion = true
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            } else {
                CampoAdmin(
                    "Nombre de la primera instalación *",
                    nombreInstalacion,
                    errores["instalacion"]
                ) { nombreInstalacion = it }
                Text(
                    "Ejemplo: Planta Lampa, Sucursal Norte o Faena Til Til. Podrás agregar más instalaciones después.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonSecundario(
                        texto = "Atrás",
                        onClick = { pasoInstalacion = false },
                        modifier = Modifier.weight(0.8f)
                    )
                    BotonPrincipal(
                        texto = "Crear cuenta",
                        onClick = {
                            if (nombreInstalacion.isBlank()) {
                                errores = mapOf("instalacion" to "Ingresa el nombre de la instalación.")
                            } else {
                                val alta = crearCuenta(
                                    viewModel = viewModel,
                                    actor = usuarioActivo,
                                    rut = rut,
                                    nombre = nombre,
                                    correo = correo,
                                    telefono = telefono,
                                    rol = rol,
                                    rutEmpresa = rutEmpresa,
                                    razonSocial = razonSocial,
                                    giro = giro,
                                    direccion = direccion,
                                    contactoNombre = contactoNombre,
                                    contactoCorreo = contactoCorreo,
                                    contactoTelefono = contactoTelefono,
                                    nombreInstalacion = nombreInstalacion
                                )
                                resultado = alta
                                errores = if (alta.exitosa) emptyMap() else mapOf(
                                    (alta.campoError ?: "general") to alta.mensajeError.orEmpty()
                                )
                                when (alta.campoError) {
                                    "rut", "correo", "nombre", "telefono" -> {
                                        pasoInstalacion = false
                                        pasoEmpresa = false
                                    }
                                    "rutEmpresa", "razonSocial", "giro", "direccion",
                                    "contactoNombre", "contactoCorreo", "contactoTelefono" -> {
                                        pasoInstalacion = false
                                        pasoEmpresa = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            errores["general"]?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun crearCuenta(
    viewModel: UsuariosViewModel,
    actor: Usuario?,
    rut: String,
    nombre: String,
    correo: String,
    telefono: String,
    rol: String,
    rutEmpresa: String? = null,
    razonSocial: String? = null,
    giro: String? = null,
    direccion: String? = null,
    contactoNombre: String? = null,
    contactoCorreo: String? = null,
    contactoTelefono: String? = null,
    nombreInstalacion: String? = null
) = viewModel.crearCuentaAdministrativa(
    actor = actor,
    rut = rut.trim(),
    nombre = nombre.trim(),
    correo = correo.trim(),
    telefono = telefono.trim(),
    rol = rol,
    rutEmpresa = rutEmpresa?.trim(),
    razonSocial = razonSocial?.trim(),
    giro = giro?.trim(),
    direccion = direccion?.trim(),
    contactoNombre = contactoNombre?.trim(),
    contactoCorreo = contactoCorreo?.trim(),
    contactoTelefono = contactoTelefono?.trim(),
    nombreInstalacion = nombreInstalacion?.trim()
)

private fun validarCuenta(
    rut: String,
    nombre: String,
    correo: String,
    telefono: String
): Map<String, String> = buildMap {
    when {
        rut.isBlank() -> put("rut", "Ingresa el RUT personal.")
        !ValidarRut.validarRut(rut) -> put("rut", "Ingresa un RUT válido.")
    }
    if (nombre.trim().split(Regex("\\s+")).size < 2) {
        put("nombre", "Ingresa al menos un nombre y un apellido.")
    }
    when {
        correo.isBlank() -> put("correo", "Ingresa un correo electrónico.")
        !Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() ->
            put("correo", "Ingresa un correo válido.")
    }
    if (telefono.isBlank()) {
        put("telefono", "Ingresa un teléfono móvil.")
    } else if (!ValidarTelefonoChile.esValido(telefono)) {
        put("telefono", "Ingresa un teléfono móvil chileno válido.")
    }
}

private fun validarEmpresa(
    rut: String,
    razonSocial: String,
    giro: String,
    direccion: String,
    contactoNombre: String,
    contactoCorreo: String,
    contactoTelefono: String
): Map<String, String> = buildMap {
    when {
        rut.isBlank() -> put("rutEmpresa", "Ingresa el RUT de la empresa.")
        !ValidarRut.validarRut(rut) -> put("rutEmpresa", "Ingresa un RUT válido.")
    }
    if (razonSocial.isBlank()) put("razonSocial", "Ingresa la razón social.")
    if (giro.isBlank()) put("giro", "Ingresa el giro de la empresa.")
    if (direccion.isBlank()) put("direccion", "Ingresa la dirección.")
    if (contactoNombre.isBlank()) put("contactoNombre", "Ingresa el nombre del contacto.")
    when {
        contactoCorreo.isBlank() -> put("contactoCorreo", "Ingresa el correo del contacto.")
        !Patterns.EMAIL_ADDRESS.matcher(contactoCorreo.trim()).matches() ->
            put("contactoCorreo", "Ingresa un correo válido.")
    }
    if (!ValidarTelefonoChile.esValido(contactoTelefono)) {
        put("contactoTelefono", "Ingresa un teléfono móvil chileno válido.")
    }
}

@Composable
private fun CampoAdmin(
    etiqueta: String,
    valor: String,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text,
    onValorCambia: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        label = { Text(etiqueta) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        colors = EstilosCampo.colores(),
        isError = error != null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = EstilosCampo.forma,
        supportingText = error?.let { mensaje ->
            { Text(mensaje, color = MaterialTheme.colorScheme.error) }
        }
    )
}

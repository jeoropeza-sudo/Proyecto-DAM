package cl.duocuc.Induztek.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class SesionPreferencias(context: Context) {

    private val prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)
    private val credencialesSeguras = crearPreferenciasSeguras(context)

    companion object {
        private const val CLAVE_ID_USUARIO = "id_usuario"
        private const val CLAVE_TOKEN = "jwt"
        private const val SIN_SESION = -1L
    }

    fun guardar(idUsuario: Long) {
        prefs.edit {
            putLong(CLAVE_ID_USUARIO, idUsuario)
            remove("usuario")
        }
    }

    fun guardarToken(token: String) {
        credencialesSeguras.edit { putString(CLAVE_TOKEN, token) }
    }

    fun leerToken(): String? {
        return credencialesSeguras.getString(CLAVE_TOKEN, null)
    }

    fun leer(): Long {
        prefs.edit { remove("usuario") }
        return prefs.getLong(CLAVE_ID_USUARIO, SIN_SESION)
    }

    fun borrar() {
        prefs.edit {
            remove(CLAVE_ID_USUARIO)
            remove("usuario")
        }
        credencialesSeguras.edit { remove(CLAVE_TOKEN) }
    }

    private fun crearPreferenciasSeguras(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            "sesion_segura",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}

package cl.duocuc.Induztek.data.remote

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class SyncQueueEntry(
    val id: String,
    val resourceType: String,
    val operation: String,
    val resourceId: Long?,
    val payload: String,
    val status: String,
    val lastError: String?
)

class ApiCacheDbHelper(context: Context) :
    SQLiteOpenHelper(context, "induztek_api_cache.db", null, 2) {

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE api_cache (" +
                "resource_type TEXT NOT NULL, resource_id TEXT NOT NULL, " +
                "payload TEXT NOT NULL, updated_at INTEGER NOT NULL, " +
                "PRIMARY KEY(resource_type, resource_id))"
        )
        database.execSQL(
            "CREATE TABLE sync_queue (" +
                "id TEXT PRIMARY KEY, resource_type TEXT NOT NULL, " +
                "operation TEXT NOT NULL, resource_id INTEGER, payload TEXT NOT NULL, " +
                "status TEXT NOT NULL, last_error TEXT, created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL)"
        )
        database.execSQL("CREATE INDEX sync_queue_status_idx ON sync_queue(status, created_at)")
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            database.execSQL(
                "ALTER TABLE sync_queue ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL("UPDATE sync_queue SET updated_at = created_at")
        }
    }

    fun guardarCache(tipo: String, id: String, payload: String) {
        val valores = ContentValues()
        valores.put("resource_type", tipo)
        valores.put("resource_id", id)
        valores.put("payload", payload)
        valores.put("updated_at", System.currentTimeMillis())
        writableDatabase.insertWithOnConflict(
            "api_cache",
            null,
            valores,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun leerCache(tipo: String, id: String): String? {
        val cursor = readableDatabase.query(
            "api_cache",
            arrayOf("payload"),
            "resource_type = ? AND resource_id = ?",
            arrayOf(tipo, id),
            null,
            null,
            null
        )
        cursor.use {
            if (!it.moveToFirst()) {
                return null
            }
            return it.getString(0)
        }
    }

    fun leerCache(tipo: String): List<String> {
        val contenido = mutableListOf<String>()
        val cursor = readableDatabase.query(
            "api_cache",
            arrayOf("payload"),
            "resource_type = ?",
            arrayOf(tipo),
            null,
            null,
            "resource_id"
        )
        cursor.use {
            while (it.moveToNext()) {
                contenido.add(it.getString(0))
            }
        }
        return contenido
    }

    fun encolar(
        tipo: String,
        operacion: String,
        id: Long?,
        payload: String
    ): String {
        val idTarea = UUID.randomUUID().toString()
        val valores = ContentValues()
        valores.put("id", idTarea)
        valores.put("resource_type", tipo)
        valores.put("operation", operacion)
        if (id == null) {
            valores.putNull("resource_id")
        } else {
            valores.put("resource_id", id)
        }
        valores.put("payload", payload)
        valores.put("status", ESTADO_PENDIENTE)
        val ahora = System.currentTimeMillis()
        valores.put("created_at", ahora)
        valores.put("updated_at", ahora)
        writableDatabase.insertOrThrow("sync_queue", null, valores)
        return idTarea
    }

    fun tomarSiguientePendiente(): SyncQueueEntry? {
        val database = writableDatabase
        database.beginTransaction()
        try {
            val limiteAtascado = System.currentTimeMillis() - TIEMPO_REINTENTO_ATASCADO
            val valorPendiente = ContentValues()
            valorPendiente.put("status", ESTADO_PENDIENTE)
            database.update(
                "sync_queue",
                valorPendiente,
                "status = ? AND updated_at < ?",
                arrayOf(ESTADO_EN_PROCESO, limiteAtascado.toString())
            )
            val cursor = database.query(
                "sync_queue",
                arrayOf("id", "resource_type", "operation", "resource_id", "payload", "status", "last_error"),
                "status = ?",
                arrayOf(ESTADO_PENDIENTE),
                null,
                null,
                "created_at",
                "1"
            )
            val entrada = cursor.use {
                if (!it.moveToFirst()) {
                    null
                } else {
                    val idRecurso = if (it.isNull(3)) null else it.getLong(3)
                    SyncQueueEntry(
                        id = it.getString(0),
                        resourceType = it.getString(1),
                        operation = it.getString(2),
                        resourceId = idRecurso,
                        payload = it.getString(4),
                        status = it.getString(5),
                        lastError = if (it.isNull(6)) null else it.getString(6)
                    )
                }
            }
            if (entrada != null) {
                val valorEnProceso = ContentValues()
                valorEnProceso.put("status", ESTADO_EN_PROCESO)
                valorEnProceso.put("updated_at", System.currentTimeMillis())
                database.update(
                    "sync_queue",
                    valorEnProceso,
                    "id = ? AND status = ?",
                    arrayOf(entrada.id, ESTADO_PENDIENTE)
                )
            }
            database.setTransactionSuccessful()
            return entrada
        } finally {
            database.endTransaction()
        }
    }

    fun conflictos(): List<SyncQueueEntry> {
        return listarPorEstado(ESTADO_CONFLICTO)
    }

    fun tareasDeSincronizacion(): List<SyncQueueEntry> {
        val entradas = mutableListOf<SyncQueueEntry>()
        val cursor = readableDatabase.query(
            "sync_queue",
            arrayOf("id", "resource_type", "operation", "resource_id", "payload", "status", "last_error"),
            null,
            null,
            null,
            null,
            "created_at"
        )
        cursor.use {
            while (it.moveToNext()) {
                val idRecurso = if (it.isNull(3)) null else it.getLong(3)
                entradas.add(
                    SyncQueueEntry(
                        id = it.getString(0),
                        resourceType = it.getString(1),
                        operation = it.getString(2),
                        resourceId = idRecurso,
                        payload = it.getString(4),
                        status = it.getString(5),
                        lastError = if (it.isNull(6)) null else it.getString(6)
                    )
                )
            }
        }
        return entradas
    }

    fun hayConflictoVisita(idVisita: Long): Boolean {
        val cursor = readableDatabase.query(
            "sync_queue",
            arrayOf("id"),
            "resource_id = ? AND status = ? AND resource_type IN (?, ?)",
            arrayOf(
                idVisita.toString(),
                ESTADO_CONFLICTO,
                "protocolo",
                "visita_equipo"
            ),
            null,
            null,
            null,
            "1"
        )
        cursor.use {
            return it.moveToFirst()
        }
    }

    fun resolverConflicto(id: String, mantenerLocal: Boolean) {
        if (mantenerLocal) {
            actualizarEstado(id, ESTADO_PENDIENTE, "Reintento solicitado por el usuario.")
        } else {
            marcarSincronizado(id)
        }
    }

    fun marcarPendiente(id: String, error: String) {
        actualizarEstado(id, ESTADO_PENDIENTE, error)
    }

    fun marcarConflicto(id: String, error: String) {
        actualizarEstado(id, ESTADO_CONFLICTO, error)
    }

    fun marcarFallido(id: String, error: String) {
        actualizarEstado(id, ESTADO_FALLIDO, error)
    }

    fun marcarSincronizado(id: String) {
        writableDatabase.delete("sync_queue", "id = ?", arrayOf(id))
    }

    private fun listarPorEstado(estado: String): List<SyncQueueEntry> {
        val entradas = mutableListOf<SyncQueueEntry>()
        val cursor = readableDatabase.query(
            "sync_queue",
            arrayOf("id", "resource_type", "operation", "resource_id", "payload", "status", "last_error"),
            "status = ?",
            arrayOf(estado),
            null,
            null,
            "created_at"
        )
        cursor.use {
            while (it.moveToNext()) {
                val idRecurso = if (it.isNull(3)) null else it.getLong(3)
                entradas.add(
                    SyncQueueEntry(
                        id = it.getString(0),
                        resourceType = it.getString(1),
                        operation = it.getString(2),
                        resourceId = idRecurso,
                        payload = it.getString(4),
                        status = it.getString(5),
                        lastError = if (it.isNull(6)) null else it.getString(6)
                    )
                )
            }
        }
        return entradas
    }

    private fun actualizarEstado(id: String, estado: String, error: String?) {
        val valores = ContentValues()
        valores.put("status", estado)
        valores.put("updated_at", System.currentTimeMillis())
        if (error == null) {
            valores.putNull("last_error")
        } else {
            valores.put("last_error", error)
        }
        writableDatabase.update("sync_queue", valores, "id = ?", arrayOf(id))
    }

    companion object {
        const val ESTADO_PENDIENTE = "PENDING"
        const val ESTADO_EN_PROCESO = "IN_PROGRESS"
        const val ESTADO_CONFLICTO = "CONFLICT"
        const val ESTADO_FALLIDO = "FAILED"
        private const val TIEMPO_REINTENTO_ATASCADO = 5 * 60 * 1000L
    }
}

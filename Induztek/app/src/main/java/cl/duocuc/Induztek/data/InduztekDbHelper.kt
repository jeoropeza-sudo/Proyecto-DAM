package cl.duocuc.Induztek.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.model.PruebaMedicion

class InduztekDbHelper(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_BD, null, VERSION_BD) {

    companion object {
        private const val NOMBRE_BD = "induztek.db"
        private const val VERSION_BD = 1

        // TABLA: TIPO_EQUIPO
        private const val TABLA_TIPO_EQUIPO = "tipo_equipo"
        private const val TEQ_ID = "id_tipo_equipo"
        private const val TEQ_NOMBRE = "nombre"
        private const val TEQ_CLASIFICACION = "clasificacion"

        // TABLA: EQUIPO
        private const val TABLA_EQUIPO = "equipo"
        private const val EQP_ID = "id_equipo"
        private const val EQP_INSTALACION = "id_instalacion"
        private const val EQP_TAG = "tag"
        private const val EQP_TIPO_ID = "id_tipo_equipo"
        private const val EQP_MARCA = "marca"
        private const val EQP_MODELO = "modelo"
        private const val EQP_SERIE = "numero_serie"
        private const val EQP_POTENCIA = "potencia_kva"
        private const val EQP_TENSIONES = "tensiones"

        // TABLA: PROTOCOLO
        private const val TABLA_PROTOCOLO = "protocolo"
        private const val PRT_ID = "id_protocolo"
        private const val PRT_NUMERO = "numero"
        private const val PRT_UUID = "uuid_local"
        private const val PRT_VISITA = "id_visita"
        private const val PRT_EQUIPO_ID = "id_equipo"
        private const val PRT_USUARIO_REGISTRA = "id_usuario_registra"
        private const val PRT_FECHA = "fecha_hora_prueba"
        private const val PRT_TEMP = "temperatura_c"
        private const val PRT_HUMEDAD = "humedad_pct"
        private const val PRT_OBSERVACIONES = "descripcion_mantenimiento"
        private const val PRT_ESTADO = "estado"
        private const val PRT_RESULTADO_GLOBAL = "resultado_global"
        private const val PRT_SYNC = "sync_status"

        // TABLA: PRUEBA
        private const val TABLA_PRUEBA = "prueba"
        private const val PRB_ID = "id_prueba"
        private const val PRB_PROTOCOLO_ID = "id_protocolo"
        private const val PRB_TIPO_PRUEBA_ID = "id_tipo_prueba"
        private const val PRB_ELEMENTO = "elemento"
        private const val PRB_MOMENTO = "momento"
        private const val PRB_REALIZADA = "realizada"
        private const val PRB_MOTIVO_NO = "motivo_no_realizada"
        private const val PRB_RESULTADO = "resultado"

        // TABLA: MEDICION
        private const val TABLA_MEDICION = "medicion"
        private const val MED_ID = "id_medicion"
        private const val MED_PRUEBA_ID = "id_prueba"
        private const val MED_PARAMETRO = "codigo_parametro"
        private const val MED_VALOR = "valor"

        // CONSULTAS DDL CREACION DE TABLAS (3FN)
        private const val SQL_CREAR_TIPO_EQUIPO =
            "CREATE TABLE $TABLA_TIPO_EQUIPO (" +
                    "$TEQ_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$TEQ_NOMBRE TEXT NOT NULL UNIQUE, " +
                    "$TEQ_CLASIFICACION TEXT NOT NULL)"

        private const val SQL_CREAR_EQUIPO =
            "CREATE TABLE $TABLA_EQUIPO (" +
                    "$EQP_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$EQP_INSTALACION INTEGER NOT NULL, " +
                    "$EQP_TAG TEXT NOT NULL, " +
                    "$EQP_TIPO_ID INTEGER NOT NULL REFERENCES $TABLA_TIPO_EQUIPO($TEQ_ID), " +
                    "$EQP_MARCA TEXT NOT NULL, " +
                    "$EQP_MODELO TEXT NOT NULL, " +
                    "$EQP_SERIE TEXT NOT NULL, " +
                    "$EQP_POTENCIA REAL, " +
                    "$EQP_TENSIONES TEXT, " +
                    "UNIQUE($EQP_INSTALACION,$EQP_TAG))"

        private const val SQL_CREAR_PROTOCOLO =
            "CREATE TABLE $TABLA_PROTOCOLO (" +
                    "$PRT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$PRT_NUMERO TEXT NOT NULL UNIQUE, " +
                    "$PRT_UUID TEXT NOT NULL, " +
                    "$PRT_VISITA INTEGER NOT NULL, " +
                    "$PRT_EQUIPO_ID INTEGER NOT NULL REFERENCES $TABLA_EQUIPO($EQP_ID), " +
                    "$PRT_USUARIO_REGISTRA INTEGER NOT NULL, " +
                    "$PRT_FECHA TEXT NOT NULL, " +
                    "$PRT_TEMP REAL NOT NULL, " +
                    "$PRT_HUMEDAD REAL NOT NULL, " +
                    "$PRT_OBSERVACIONES TEXT, " +
                    "$PRT_ESTADO TEXT NOT NULL DEFAULT 'BORRADOR', " +
                    "$PRT_RESULTADO_GLOBAL TEXT NOT NULL DEFAULT 'APROBADO', " +
                    "$PRT_SYNC INTEGER NOT NULL DEFAULT 0, " +
                    "UNIQUE($PRT_VISITA,$PRT_EQUIPO_ID))"

        private const val SQL_CREAR_PRUEBA =
            "CREATE TABLE $TABLA_PRUEBA (" +
                    "$PRB_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$PRB_PROTOCOLO_ID INTEGER NOT NULL REFERENCES $TABLA_PROTOCOLO($PRT_ID) ON DELETE CASCADE, " +
                    "$PRB_TIPO_PRUEBA_ID INTEGER NOT NULL, " +
                    "$PRB_ELEMENTO TEXT NOT NULL, " +
                    "$PRB_MOMENTO TEXT NOT NULL DEFAULT 'UNICA', " +
                    "$PRB_REALIZADA INTEGER NOT NULL DEFAULT 1, " +
                    "$PRB_MOTIVO_NO TEXT, " +
                    "$PRB_RESULTADO TEXT NOT NULL DEFAULT 'APROBADO')"

        private const val SQL_CREAR_MEDICION =
            "CREATE TABLE $TABLA_MEDICION (" +
                    "$MED_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$MED_PRUEBA_ID INTEGER NOT NULL REFERENCES $TABLA_PRUEBA($PRB_ID) ON DELETE CASCADE, " +
                    "$MED_PARAMETRO TEXT NOT NULL, " +
                    "$MED_VALOR REAL NOT NULL)"

        // CONSULTAS DATOS PRECARGADOS

        private const val SQL_PRECARGAR_TIPOS =
            "INSERT INTO $TABLA_TIPO_EQUIPO ($TEQ_ID, $TEQ_NOMBRE,$TEQ_CLASIFICACION) VALUES " +
                    "(1, 'Transformador', 'PRIMARIO'), " +
                    "(2, 'Celda MT', 'PRIMARIO'), " +
                    "(3, 'Interruptor', 'PRIMARIO'), " +
                    "(4, 'Cable de poder', 'PRIMARIO'), " +
                    "(5, 'Relé de protección', 'SECUNDARIO')"

        private const val SQL_PRECARGAR_EQUIPOS =
            "INSERT INTO $TABLA_EQUIPO ($EQP_ID, $EQP_INSTALACION,$EQP_TAG, $EQP_TIPO_ID,$EQP_MARCA, $EQP_MODELO,$EQP_SERIE, $EQP_POTENCIA,$EQP_TENSIONES) VALUES " +
                    "(1, 1, 'TR-01', 1, 'Rhona', 'RH-2012', 'RH-18733', 1500.0, '12 / 0.4 kV'), " +
                    "(2, 1, 'TR-02', 1, 'Rhona', 'RH-2017', 'RH-21904', 1000.0, '12 / 0.4 kV')"

        private const val SQL_PRECARGAR_PROTOCOLOS =
            "INSERT INTO $TABLA_PROTOCOLO ($PRT_ID, $PRT_NUMERO,$PRT_UUID, $PRT_VISITA,$PRT_EQUIPO_ID, $PRT_USUARIO_REGISTRA,$PRT_FECHA, $PRT_TEMP,$PRT_HUMEDAD, $PRT_OBSERVACIONES,$PRT_ESTADO, $PRT_RESULTADO_GLOBAL,$PRT_SYNC) VALUES " +
                    "(1, 'PT-2024-0312', 'uuid-1', 1, 1, 1, '2024-09-14 10:00:00', 18.0, 55.0, NULL, 'FORMALIZADO', 'APROBADO', 1), " +
                    "(2, 'PT-2025-0288', 'uuid-2', 1, 1, 2, '2025-09-13 11:30:00', 21.0, 48.0, NULL, 'FORMALIZADO', 'APROBADO', 1), " +
                    "(3, 'PT-2026-0401', 'uuid-3', 1, 1, 1, '2026-09-26 09:40:00', 19.0, 60.0, 'IP bajo en AT-T. Se realizó limpieza.', 'REGISTRADO_TERRENO', 'OBSERVADO', 0)"

        private const val SQL_PRECARGAR_PRUEBAS =
            "INSERT INTO $TABLA_PRUEBA ($PRB_ID, $PRB_PROTOCOLO_ID,$PRB_TIPO_PRUEBA_ID, $PRB_ELEMENTO,$PRB_MOMENTO, $PRB_REALIZADA,$PRB_RESULTADO) VALUES " +
                    "(1, 3, 1, 'AT-T', 'UNICA', 1, 'OBSERVADO')"

        private const val SQL_PRECARGAR_MEDICIONES =
            "INSERT INTO $TABLA_MEDICION ($MED_PRUEBA_ID, $MED_PARAMETRO,$MED_VALOR) VALUES " +
                    "(1, 'TENSION_PRUEBA', 5.0), " +
                    "(1, 'LECTURA_1MIN', 6100.0), " +
                    "(1, 'LECTURA_10MIN', 10980.0)"

        private const val SQL_SELECT_PROTOCOLOS =
            "SELECT p.*, e.$EQP_TAG AS tag_equipo, e.$EQP_MARCA AS marca_equipo " +
                    "FROM $TABLA_PROTOCOLO p " +
                    "INNER JOIN $TABLA_EQUIPO e ON p.$PRT_EQUIPO_ID = e.$EQP_ID"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREAR_TIPO_EQUIPO)
        db.execSQL(SQL_CREAR_EQUIPO)
        db.execSQL(SQL_CREAR_PROTOCOLO)
        db.execSQL(SQL_CREAR_PRUEBA)
        db.execSQL(SQL_CREAR_MEDICION)

        db.execSQL(SQL_PRECARGAR_TIPOS)
        db.execSQL(SQL_PRECARGAR_EQUIPOS)
        db.execSQL(SQL_PRECARGAR_PROTOCOLOS)
        db.execSQL(SQL_PRECARGAR_PRUEBAS)
        db.execSQL(SQL_PRECARGAR_MEDICIONES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Reservado para futuras migraciones
    }

    // METODOS DE CONSULTA Y OPERACIONES

    fun listarProtocolosPorEquipo(idEquipo: Long): List<ProtocoloTerreno> =
        consultarProtocolos(
            "$SQL_SELECT_PROTOCOLOS WHERE p.$PRT_EQUIPO_ID = ? ORDER BY p.$PRT_FECHA DESC",
            arrayOf(idEquipo.toString())
        )

    fun buscarProtocoloPorId(idProtocolo: Long): ProtocoloTerreno? =
        consultarProtocolos(
            "$SQL_SELECT_PROTOCOLOS WHERE p.$PRT_ID = ?",
            arrayOf(idProtocolo.toString())
        ).firstOrNull()

    fun obtenerUltimaLecturaAnterior(idEquipo: Long, codigoParametro: String): Double? {
        val sql = "SELECT m.$MED_VALOR FROM $TABLA_MEDICION m " +
                "INNER JOIN $TABLA_PRUEBA pr ON m.$MED_PRUEBA_ID = pr.$PRB_ID " +
                "INNER JOIN $TABLA_PROTOCOLO pt ON pr.$PRB_PROTOCOLO_ID = pt.$PRT_ID " +
                "WHERE pt.$PRT_EQUIPO_ID = ? AND m.$MED_PARAMETRO = ? " +
                "ORDER BY pt.$PRT_FECHA DESC LIMIT 1"

        readableDatabase.rawQuery(sql, arrayOf(idEquipo.toString(), codigoParametro)).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getDouble(cursor.getColumnIndexOrThrow(MED_VALOR))
            }
        }
        return null
    }

    // Transacción para insertar un protocolo completo con su prueba y mediciones
    fun insertarProtocoloCompleto(
        idEquipo: Long,
        l1Min: Double,
        l10Min: Double,
        resultado: String,
        observaciones: String
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val numProto = "PT-2026-" + (1000..9999).random()

            val valoresProto = ContentValues().apply {
                put(PRT_NUMERO, numProto)
                put(PRT_UUID, java.util.UUID.randomUUID().toString())
                put(PRT_VISITA, 1)
                put(PRT_EQUIPO_ID, idEquipo)
                put(PRT_USUARIO_REGISTRA, 1)
                put(PRT_FECHA, "2026-10-18 10:00:00")
                put(PRT_TEMP, 20.0)
                put(PRT_HUMEDAD, 50.0)
                put(PRT_OBSERVACIONES, observaciones)
                put(PRT_ESTADO, "REGISTRADO_TERRENO")
                put(PRT_RESULTADO_GLOBAL, resultado)
                put(PRT_SYNC, 0)
            }
            val idProtocolo = db.insert(TABLA_PROTOCOLO, null, valoresProto)

            val valoresPrueba = ContentValues().apply {
                put(PRB_PROTOCOLO_ID, idProtocolo)
                put(PRB_TIPO_PRUEBA_ID, 1)
                put(PRB_ELEMENTO, "AT-T")
                put(PRB_MOMENTO, "UNICA")
                put(PRB_REALIZADA, 1)
                put(PRB_RESULTADO, resultado)
            }
            val idPrueba = db.insert(TABLA_PRUEBA, null, valoresPrueba)

            val valoresM1 = ContentValues().apply {
                put(MED_PRUEBA_ID, idPrueba)
                put(MED_PARAMETRO, "LECTURA_1MIN")
                put(MED_VALOR, l1Min)
            }
            val valoresM10 = ContentValues().apply {
                put(MED_PRUEBA_ID, idPrueba)
                put(MED_PARAMETRO, "LECTURA_10MIN")
                put(MED_VALOR, l10Min)
            }

            db.insert(TABLA_MEDICION, null, valoresM1)
            db.insert(TABLA_MEDICION, null, valoresM10)

            db.setTransactionSuccessful()
            return idProtocolo
        } finally {
            db.endTransaction()
        }
    }

    private fun consultarProtocolos(sql: String, args: Array<String>?): List<ProtocoloTerreno> {
        val lista = mutableListOf<ProtocoloTerreno>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(leerProtocolo(cursor))
            }
        }
        return lista
    }

    private fun leerProtocolo(cursor: Cursor) = ProtocoloTerreno(
        idProtocolo = cursor.getLong(cursor.getColumnIndexOrThrow(PRT_ID)),
        numeroProtocolo = cursor.getString(cursor.getColumnIndexOrThrow(PRT_NUMERO)),
        tagEquipo = cursor.getString(cursor.getColumnIndexOrThrow("tag_equipo")),
        tipoEquipo = "Transformador",
        clienteNombre = "Envasadora del Pacífico S.A.",
        instalacionNombre = "Planta Lampa",
        tecnicoNombre = "Matías Godoy Herrera",
        fechaHora = cursor.getString(cursor.getColumnIndexOrThrow(PRT_FECHA)),
        temperaturaC = cursor.getDouble(cursor.getColumnIndexOrThrow(PRT_TEMP)),
        humedadPct = cursor.getDouble(cursor.getColumnIndexOrThrow(PRT_HUMEDAD)),
        estado = cursor.getString(cursor.getColumnIndexOrThrow(PRT_ESTADO)),
        resultadoGlobal = cursor.getString(cursor.getColumnIndexOrThrow(PRT_RESULTADO_GLOBAL))
    )
}
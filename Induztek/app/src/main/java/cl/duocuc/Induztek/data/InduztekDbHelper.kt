package cl.duocuc.Induztek.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.model.Usuario

class InduztekDbHelper(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_BD, null, VERSION_BD) {

    companion object {
        private const val NOMBRE_BD = "induztek.db"
        private const val VERSION_BD = 2

        // TABLA: ROL
        private const val TABLA_ROL = "rol"
        private const val ROL_ID = "id_rol"
        private const val ROL_CODIGO = "codigo"
        private const val ROL_NOMBRE = "nombre"

        // TABLA: USUARIO
        private const val TABLA_USUARIO = "usuario"
        private const val USR_ID = "id_usuario"
        private const val USR_ROL_ID = "id_rol"
        private const val USR_RUT = "rut"
        private const val USR_NOMBRE = "nombre"
        private const val USR_CORREO = "correo"
        private const val USR_TELEFONO = "telefono"
        private const val USR_PASS = "password_hash"
        private const val USR_ACTIVO = "activo"
        private const val USR_CLIENTE_ID = "id_empresa_cliente"

        // TABLAS EXISTENTES DEL MVP
        private const val TABLA_TIPO_EQUIPO = "tipo_equipo"
        private const val TEQ_ID = "id_tipo_equipo"
        private const val TEQ_NOMBRE = "nombre"
        private const val TEQ_CLASIFICACION = "clasificacion"

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

        private const val TABLA_PRUEBA = "prueba"
        private const val PRB_ID = "id_prueba"
        private const val PRB_PROTOCOLO_ID = "id_protocolo"
        private const val PRB_TIPO_PRUEBA_ID = "id_tipo_prueba"
        private const val PRB_ELEMENTO = "elemento"
        private const val PRB_MOMENTO = "momento"
        private const val PRB_REALIZADA = "realizada"
        private const val PRB_MOTIVO_NO = "motivo_no_realizada"
        private const val PRB_RESULTADO = "resultado"

        private const val TABLA_MEDICION = "medicion"
        private const val MED_ID = "id_medicion"
        private const val MED_PRUEBA_ID = "id_prueba"
        private const val MED_PARAMETRO = "codigo_parametro"
        private const val MED_VALOR = "valor"

        // DDL CREACION DE TABLAS
        private const val SQL_CREAR_ROL =
            "CREATE TABLE $TABLA_ROL (" +
                    "$ROL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$ROL_CODIGO TEXT NOT NULL UNIQUE, " +
                    "$ROL_NOMBRE TEXT NOT NULL)"

        private const val SQL_CREAR_USUARIO =
            "CREATE TABLE $TABLA_USUARIO (" +
                    "$USR_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$USR_ROL_ID INTEGER NOT NULL REFERENCES $TABLA_ROL($ROL_ID), " +
                    "$USR_RUT TEXT NOT NULL UNIQUE, " +
                    "$USR_NOMBRE TEXT NOT NULL, " +
                    "$USR_CORREO TEXT NOT NULL UNIQUE, " +
                    "$USR_TELEFONO TEXT, " +
                    "$USR_PASS TEXT NOT NULL DEFAULT '1234', " +
                    "$USR_ACTIVO INTEGER NOT NULL DEFAULT 1, " +
                    "$USR_CLIENTE_ID INTEGER)"

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
                    "$PRT_SYNC INTEGER NOT NULL DEFAULT 0)"

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

        // PRECARGA SEMILLA DEL CASO (ANEXO 9)
        private const val SQL_PRECARGAR_ROLES =
            "INSERT INTO $TABLA_ROL ($ROL_ID, $ROL_CODIGO, $ROL_NOMBRE) VALUES " +
                    "(1, 'TECNICO', 'Técnico de Terreno'), " +
                    "(2, 'INGENIERO', 'Ingeniero de Pruebas'), " +
                    "(3, 'JEFE_TECNICO', 'Jefe Técnico'), " +
                    "(4, 'CLIENTE', 'Usuario Cliente')"

        private const val SQL_PRECARGAR_USUARIOS =
            "INSERT INTO $TABLA_USUARIO ($USR_ID, $USR_ROL_ID, $USR_RUT, $USR_NOMBRE, $USR_CORREO, $USR_TELEFONO, $USR_PASS, $USR_ACTIVO, $USR_CLIENTE_ID) VALUES " +
                    "(1, 1, '17.284.519-3', 'Matías Godoy Herrera', 'mgodoy@induztek.ejemplo', '+56900000100', '1234', 1, NULL), " +
                    "(2, 1, '19.412.087-7', 'Javiera Tapia Muñoz', 'jtapia@induztek.ejemplo', '+56900000101', '1234', 1, NULL), " +
                    "(3, 2, '15.906.334-8', 'Andrea Contreras Silva', 'acontreras@induztek.ejemplo', '+56900000102', '1234', 1, NULL), " +
                    "(4, 3, '13.871.905-7', 'Ricardo Espinoza Lagos', 'respinoza@induztek.ejemplo', '+56900000103', '1234', 1, NULL), " +
                    "(5, 4, '14.226.791-8', 'Marcela Fuentes Rojas', 'mfuentes@envpacifico.ejemplo', '+56900000104', '1234', 1, 1)"

        private const val SQL_PRECARGAR_TIPOS =
            "INSERT INTO $TABLA_TIPO_EQUIPO ($TEQ_ID, $TEQ_NOMBRE, $TEQ_CLASIFICACION) VALUES " +
                    "(1, 'Transformador', 'PRIMARIO'), " +
                    "(2, 'Celda MT', 'PRIMARIO'), " +
                    "(3, 'Interruptor', 'PRIMARIO'), " +
                    "(4, 'Cable de poder', 'PRIMARIO'), " +
                    "(5, 'Relé de protección', 'SECUNDARIO')"

        private const val SQL_PRECARGAR_EQUIPOS =
            "INSERT INTO $TABLA_EQUIPO ($EQP_ID, $EQP_INSTALACION, $EQP_TAG, $EQP_TIPO_ID, $EQP_MARCA, $EQP_MODELO, $EQP_SERIE, $EQP_POTENCIA, $EQP_TENSIONES) VALUES " +
                    "(1, 1, 'TR-01', 1, 'Rhona', 'RH-2012', 'RH-18733', 1500.0, '12 / 0.4 kV'), " +
                    "(2, 1, 'TR-02', 1, 'Rhona', 'RH-2017', 'RH-21904', 1000.0, '12 / 0.4 kV')"

        private const val SQL_PRECARGAR_PROTOCOLOS =
            "INSERT INTO $TABLA_PROTOCOLO ($PRT_ID, $PRT_NUMERO, $PRT_UUID, $PRT_VISITA, $PRT_EQUIPO_ID, $PRT_USUARIO_REGISTRA, $PRT_FECHA, $PRT_TEMP, $PRT_HUMEDAD, $PRT_OBSERVACIONES, $PRT_ESTADO, $PRT_RESULTADO_GLOBAL, $PRT_SYNC) VALUES " +
                    "(1, 'PT-2024-0312', 'uuid-1', 1, 1, 1, '2024-09-14 10:00:00', 18.0, 55.0, NULL, 'FORMALIZADO', 'APROBADO', 1), " +
                    "(2, 'PT-2025-0288', 'uuid-2', 2, 1, 2, '2025-09-13 11:30:00', 21.0, 48.0, NULL, 'FORMALIZADO', 'APROBADO', 1), " +
                    "(3, 'PT-2026-0401', 'uuid-3', 3, 1, 1, '2026-09-26 09:40:00', 19.0, 60.0, 'IP bajo en AT-T. Se realizó limpieza.', 'REGISTRADO_TERRENO', 'OBSERVADO', 0)"

        private const val SQL_PRECARGAR_PRUEBAS =
            "INSERT INTO $TABLA_PRUEBA ($PRB_ID, $PRB_PROTOCOLO_ID, $PRB_TIPO_PRUEBA_ID, $PRB_ELEMENTO, $PRB_MOMENTO, $PRB_REALIZADA, $PRB_RESULTADO) VALUES " +
                    "(1, 3, 1, 'AT-T', 'UNICA', 1, 'OBSERVADO')"

        private const val SQL_PRECARGAR_MEDICIONES =
            "INSERT INTO $TABLA_MEDICION ($MED_PRUEBA_ID, $MED_PARAMETRO, $MED_VALOR) VALUES " +
                    "(1, 'TENSION_PRUEBA', 5.0), " +
                    "(1, 'LECTURA_1MIN', 6100.0), " +
                    "(1, 'LECTURA_10MIN', 10980.0)"

        private const val SQL_SELECT_USUARIOS =
            "SELECT u.*, r.$ROL_CODIGO AS rol_codigo " +
                    "FROM $TABLA_USUARIO u " +
                    "INNER JOIN $TABLA_ROL r ON u.$USR_ROL_ID = r.$ROL_ID"

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
        db.execSQL(SQL_CREAR_ROL)
        db.execSQL(SQL_CREAR_USUARIO)
        db.execSQL(SQL_CREAR_TIPO_EQUIPO)
        db.execSQL(SQL_CREAR_EQUIPO)
        db.execSQL(SQL_CREAR_PROTOCOLO)
        db.execSQL(SQL_CREAR_PRUEBA)
        db.execSQL(SQL_CREAR_MEDICION)

        db.execSQL(SQL_PRECARGAR_ROLES)
        db.execSQL(SQL_PRECARGAR_USUARIOS)
        db.execSQL(SQL_PRECARGAR_TIPOS)
        db.execSQL(SQL_PRECARGAR_EQUIPOS)
        db.execSQL(SQL_PRECARGAR_PROTOCOLOS)
        db.execSQL(SQL_PRECARGAR_PRUEBAS)
        db.execSQL(SQL_PRECARGAR_MEDICIONES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(SQL_CREAR_ROL)
            db.execSQL(SQL_CREAR_USUARIO)
            db.execSQL(SQL_PRECARGAR_ROLES)
            db.execSQL(SQL_PRECARGAR_USUARIOS)
        }
    }

    fun autenticarUsuario(correoInput: String, claveInput: String): Usuario? {
        val sql = "$SQL_SELECT_USUARIOS WHERE LOWER(u.$USR_CORREO) = LOWER(?) AND u.$USR_PASS = ? AND u.$USR_ACTIVO = 1"
        return consultarUsuarios(sql, arrayOf(correoInput.trim(), claveInput.trim())).firstOrNull()
    }

    fun listarUsuarios(): List<Usuario> =
        consultarUsuarios("$SQL_SELECT_USUARIOS ORDER BY u.$USR_NOMBRE", null)

    fun buscarPorIdUsuario(id: Long): Usuario? =
        consultarUsuarios("$SQL_SELECT_USUARIOS WHERE u.$USR_ID = ?", arrayOf(id.toString())).firstOrNull()

    fun insertarUsuario(usuario: Usuario, clave: String = "1234"): Long =
        writableDatabase.insert(TABLA_USUARIO, null, valoresDe(usuario, clave))

    fun actualizarUsuario(usuario: Usuario): Int =
        writableDatabase.update(
            TABLA_USUARIO, valoresDe(usuario), "$USR_ID = ?", arrayOf(usuario.idUsuario.toString())
        )

    private fun valoresDe(usuario: Usuario, clave: String = "1234") = ContentValues().apply {
        put(USR_RUT, usuario.rut)
        put(USR_NOMBRE, usuario.nombre)
        put(USR_CORREO, usuario.correo)
        put(USR_TELEFONO, usuario.telefono)
        put(USR_PASS, clave)
        put(USR_ROL_ID, obtenerIdRolSegunCodigo(usuario.rolCodigo))
        put(USR_ACTIVO, if (usuario.activo) 1 else 0)
        put(USR_CLIENTE_ID, usuario.idEmpresaCliente)
    }

    private fun obtenerIdRolSegunCodigo(codigo: String): Long {
        return when (codigo) {
            "TECNICO" -> 1L
            "INGENIERO" -> 2L
            "JEFE_TECNICO" -> 3L
            "CLIENTE" -> 4L
            else -> 1L
        }
    }

    private fun consultarUsuarios(sql: String, args: Array<String>?): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(leerUsuario(cursor))
            }
        }
        return lista
    }

    private fun leerUsuario(cursor: Cursor) = Usuario(
        idUsuario = cursor.getLong(cursor.getColumnIndexOrThrow(USR_ID)),
        rut = cursor.getString(cursor.getColumnIndexOrThrow(USR_RUT)),
        nombre = cursor.getString(cursor.getColumnIndexOrThrow(USR_NOMBRE)),
        correo = cursor.getString(cursor.getColumnIndexOrThrow(USR_CORREO)),
        telefono = cursor.getString(cursor.getColumnIndexOrThrow(USR_TELEFONO)),
        rolCodigo = cursor.getString(cursor.getColumnIndexOrThrow("rol_codigo")),
        idEmpresaCliente = if (cursor.isNull(cursor.getColumnIndexOrThrow(USR_CLIENTE_ID))) null
        else cursor.getLong(cursor.getColumnIndexOrThrow(USR_CLIENTE_ID)),
        activo = cursor.getInt(cursor.getColumnIndexOrThrow(USR_ACTIVO)) == 1
    )

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

    fun insertarProtocoloCompleto(
        idEquipo: Long,
        l1Min: Double,
        l10Min: Double,
        resultado: String,
        observaciones: String,
        idUsuarioRegistra: Long = 1
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val numProto = "PT-2026-" + (1000..9999).random()

            val valoresProto = ContentValues().apply {
                put(PRT_NUMERO, numProto)
                put(PRT_UUID, java.util.UUID.randomUUID().toString())
                put(PRT_VISITA, (100..999).random())
                put(PRT_EQUIPO_ID, idEquipo)
                put(PRT_USUARIO_REGISTRA, idUsuarioRegistra)
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
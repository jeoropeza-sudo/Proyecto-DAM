package cl.duocuc.Induztek.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.model.MedicionProtocolo
import cl.duocuc.Induztek.model.RegistroProtocolo
import cl.duocuc.Induztek.model.ClienteCatalogoLocal
import cl.duocuc.Induztek.model.EquipoCatalogoLocal
import cl.duocuc.Induztek.model.HistorialEstadoProtocolo
import cl.duocuc.Induztek.model.InstalacionCatalogoLocal
import cl.duocuc.Induztek.model.InstalacionAsignacionLocal
import cl.duocuc.Induztek.model.InstrumentoItem
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.util.ValidarRut
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InduztekDbHelper(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_BD, null, VERSION_BD) {

    companion object {
        private const val NOMBRE_BD = "induztek.db"
        private const val VERSION_BD = 13

        private const val TABLA_ROL = "rol"
        private const val TABLA_EMPRESA = "empresa_cliente"
        private const val ROL_ID = "id_rol"
        private const val ROL_CODIGO = "codigo"
        private const val ROL_NOMBRE = "nombre"

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
        private const val USR_PREGUNTA = "pregunta_seguridad"
        private const val USR_RESPUESTA = "respuesta_seguridad"
        private const val USR_FOTO = "foto_blob"

        private const val TABLA_INSTALACION = "instalacion"
        private const val INS_ID = "id_instalacion"
        private const val INS_CLIENTE_ID = "id_usuario_cliente"
        private const val INS_NOMBRE = "nombre"

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
        private const val EQP_ANO = "ano_fabricacion"
        private const val EQP_TAP = "posicion_tap"
        private const val EQP_GRUPO = "grupo_conexion"
        private const val EQP_MAX_CONTACTO = "resistencia_contacto_max_uohm"

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
        private const val PRT_OBSERVACIONES = "observaciones"
        private const val PRT_ESTADO = "estado"
        private const val PRT_RESULTADO_GLOBAL = "resultado_global"
        private const val PRT_SYNC = "sync_status"

        private const val TABLA_HISTORIAL_ESTADO = "historial_estado"
        private const val TABLA_VISITA = "visita"
        private const val TABLA_INSTRUMENTO = "instrumento"
        private const val TABLA_MEDICION = "medicion_protocolo"
        private const val TABLA_ASIGNACION_TECNICO = "asignacion_tecnico_instalacion"

        private const val CLAVE_DEFAULT_HASH = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
        private const val RESP_DEFAULT_HASH = "93010b98729587425f1d977e23485b0d0ed4b553c30669d0840b85a3a0e63715"

        private const val SQL_SELECT_USUARIOS =
            "SELECT u.*, r.$ROL_CODIGO AS rol_codigo FROM $TABLA_USUARIO u INNER JOIN $TABLA_ROL r ON u.$USR_ROL_ID = r.$ROL_ID"

        private const val SQL_SELECT_PROTOCOLOS =
            "SELECT p.*, e.$EQP_TAG AS tag_equipo, te.$TEQ_NOMBRE AS tipo_equipo, " +
                    "e.$EQP_MARCA AS marca_equipo, e.$EQP_MODELO AS modelo_equipo, " +
                    "e.$EQP_SERIE AS serie_equipo, e.$EQP_ANO AS ano_equipo, " +
                    "e.$EQP_POTENCIA AS potencia_equipo, " +
                    "e.$EQP_TENSIONES AS tensiones_equipo, e.$EQP_TAP AS tap_equipo, " +
                    "e.$EQP_GRUPO AS grupo_equipo, " +
                    "i.$INS_NOMBRE AS instalacion_nombre, ec.razon_social AS cliente_nombre, " +
                    "u_tec.$USR_NOMBRE AS tecnico_nombre, v.numero_ot AS numero_ot, " +
                    "COALESCE(v.receptor_nombre, p.receptor_nombre) AS receptor_nombre, " +
                    "COALESCE(v.receptor_rut, p.receptor_rut) AS receptor_rut, " +
                    "COALESCE(v.receptor_cargo, p.receptor_cargo) AS receptor_cargo, " +
                    "COALESCE(v.fecha_recepcion, p.fecha_recepcion) AS fecha_recepcion, " +
                    "COALESCE(v.tipo_visita, p.tipo_visita) AS visita_tipo, " +
                    "COALESCE(v.descripcion_mantenimiento, p.descripcion_mantenimiento) AS visita_descripcion, " +
                    "uf.$USR_NOMBRE AS usuario_formaliza_nombre " +
                    "FROM $TABLA_PROTOCOLO p " +
                    "INNER JOIN $TABLA_EQUIPO e ON p.$PRT_EQUIPO_ID = e.$EQP_ID " +
                    "INNER JOIN $TABLA_TIPO_EQUIPO te ON e.$EQP_TIPO_ID = te.$TEQ_ID " +
                    "INNER JOIN $TABLA_INSTALACION i ON e.$EQP_INSTALACION = i.$INS_ID " +
                    "INNER JOIN $TABLA_USUARIO u_cli ON i.$INS_CLIENTE_ID = u_cli.$USR_ID " +
                    "LEFT JOIN $TABLA_EMPRESA ec ON u_cli.$USR_CLIENTE_ID = ec.id_empresa_cliente " +
                    "INNER JOIN $TABLA_USUARIO u_tec ON p.$PRT_USUARIO_REGISTRA = u_tec.$USR_ID " +
                    "LEFT JOIN $TABLA_USUARIO uf ON p.id_usuario_formaliza = uf.$USR_ID " +
                    "LEFT JOIN $TABLA_VISITA v ON p.$PRT_VISITA = v.id_visita"
    }

    private fun listarHistorialEstados(idProtocolo: Long): List<HistorialEstadoProtocolo> =
    readableDatabase.rawQuery(
        "SELECT h.estado_anterior, h.estado_nuevo, u.$USR_NOMBRE, h.fecha_hora, h.motivo " +
                "FROM $TABLA_HISTORIAL_ESTADO h " +
                "LEFT JOIN $TABLA_USUARIO u ON h.id_usuario = u.$USR_ID " +
                "WHERE h.id_protocolo = ? ORDER BY h.id_historial",
        arrayOf(idProtocolo.toString())
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    HistorialEstadoProtocolo(
                        estadoAnterior = if (cursor.isNull(0)) null else cursor.getString(0),
                        estadoNuevo = cursor.getString(1),
                        usuarioNombre = if (cursor.isNull(2)) "Usuario desconocido" else cursor.getString(2),
                        fechaHora = cursor.getString(3),
                        motivo = if (cursor.isNull(4)) null else cursor.getString(4)
                    )
                )
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLA_ROL ($ROL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $ROL_CODIGO TEXT NOT NULL UNIQUE, $ROL_NOMBRE TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $TABLA_EMPRESA (id_empresa_cliente INTEGER PRIMARY KEY, razon_social TEXT NOT NULL UNIQUE, rut TEXT NOT NULL UNIQUE, giro TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $TABLA_USUARIO ($USR_ID INTEGER PRIMARY KEY AUTOINCREMENT, $USR_ROL_ID INTEGER NOT NULL REFERENCES $TABLA_ROL($ROL_ID), $USR_RUT TEXT NOT NULL UNIQUE, $USR_NOMBRE TEXT NOT NULL, $USR_CORREO TEXT NOT NULL UNIQUE, $USR_TELEFONO TEXT, $USR_PASS TEXT NOT NULL DEFAULT '$CLAVE_DEFAULT_HASH', $USR_ACTIVO INTEGER NOT NULL DEFAULT 1, $USR_CLIENTE_ID INTEGER, $USR_PREGUNTA TEXT, $USR_RESPUESTA TEXT, $USR_FOTO BLOB)")
        db.execSQL("CREATE TABLE $TABLA_INSTALACION ($INS_ID INTEGER PRIMARY KEY AUTOINCREMENT, $INS_CLIENTE_ID INTEGER NOT NULL REFERENCES $TABLA_USUARIO($USR_ID), $INS_NOMBRE TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $TABLA_TIPO_EQUIPO ($TEQ_ID INTEGER PRIMARY KEY AUTOINCREMENT, $TEQ_NOMBRE TEXT NOT NULL UNIQUE, $TEQ_CLASIFICACION TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $TABLA_EQUIPO ($EQP_ID INTEGER PRIMARY KEY AUTOINCREMENT, $EQP_INSTALACION INTEGER NOT NULL REFERENCES $TABLA_INSTALACION($INS_ID), $EQP_TAG TEXT NOT NULL, $EQP_TIPO_ID INTEGER NOT NULL REFERENCES $TABLA_TIPO_EQUIPO($TEQ_ID), $EQP_MARCA TEXT NOT NULL, $EQP_MODELO TEXT NOT NULL, $EQP_SERIE TEXT NOT NULL, $EQP_POTENCIA REAL, $EQP_TENSIONES TEXT, $EQP_ANO TEXT NOT NULL DEFAULT '', $EQP_TAP TEXT NOT NULL DEFAULT '', $EQP_GRUPO TEXT NOT NULL DEFAULT '', $EQP_MAX_CONTACTO REAL, UNIQUE($EQP_INSTALACION,$EQP_TAG))")
        db.execSQL("CREATE TABLE $TABLA_PROTOCOLO ($PRT_ID INTEGER PRIMARY KEY AUTOINCREMENT, $PRT_NUMERO TEXT NOT NULL UNIQUE, $PRT_UUID TEXT NOT NULL, $PRT_VISITA INTEGER NOT NULL, $PRT_EQUIPO_ID INTEGER NOT NULL REFERENCES $TABLA_EQUIPO($EQP_ID), $PRT_USUARIO_REGISTRA INTEGER NOT NULL REFERENCES $TABLA_USUARIO($USR_ID), $PRT_FECHA TEXT NOT NULL, $PRT_TEMP REAL NOT NULL, $PRT_HUMEDAD REAL NOT NULL, $PRT_OBSERVACIONES TEXT, $PRT_ESTADO TEXT NOT NULL DEFAULT 'BORRADOR', $PRT_RESULTADO_GLOBAL TEXT NOT NULL DEFAULT 'APROBADO', $PRT_SYNC INTEGER NOT NULL DEFAULT 0, lectura_1min REAL, lectura_10min REAL, id_usuario_formaliza INTEGER, fecha_formalizacion TEXT, motivo_observacion TEXT)")
        db.execSQL("CREATE TABLE $TABLA_HISTORIAL_ESTADO (id_historial INTEGER PRIMARY KEY AUTOINCREMENT, id_protocolo INTEGER NOT NULL, estado_anterior TEXT, estado_nuevo TEXT NOT NULL, id_usuario INTEGER NOT NULL, fecha_hora TEXT NOT NULL, motivo TEXT)")
        crearTablasProtocoloExtendido(db)

        db.execSQL("INSERT INTO $TABLA_ROL ($ROL_ID, $ROL_CODIGO, $ROL_NOMBRE) VALUES (1, 'TECNICO', 'Técnico de Terreno'), (2, 'INGENIERO', 'Ingeniero de Pruebas'), (3, 'JEFE_TECNICO', 'Jefe Técnico'), (4, 'CLIENTE', 'Usuario Cliente')")
        insertarEmpresasIniciales(db)

        db.execSQL("INSERT INTO $TABLA_USUARIO ($USR_ID, $USR_ROL_ID, $USR_RUT, $USR_NOMBRE, $USR_CORREO, $USR_TELEFONO, $USR_PASS, $USR_ACTIVO, $USR_CLIENTE_ID, $USR_PREGUNTA, $USR_RESPUESTA) VALUES " +
                "(1, 1, '17.284.519-3', 'Matías Godoy Herrera', 'mgodoy@induztek.ejemplo', '+56900000001', '$CLAVE_DEFAULT_HASH', 1, NULL, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(2, 1, '19.412.087-7', 'Javiera Tapia Muñoz', 'jtapia@induztek.ejemplo', '+56900000002', '$CLAVE_DEFAULT_HASH', 1, NULL, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(3, 1, '26.104.583-4', 'Carlos Rondón Pérez', 'crondon@induztek.ejemplo', '+56900000003', '$CLAVE_DEFAULT_HASH', 1, NULL, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(4, 1, '20.137.648-3', 'Diego Valenzuela Ríos', 'dvalenzuela@induztek.ejemplo', '+56900000004', '$CLAVE_DEFAULT_HASH', 1, NULL, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(5, 2, '15.906.334-8', 'Andrea Contreras Silva', 'acontreras@induztek.ejemplo', '+56900000005', '$CLAVE_DEFAULT_HASH', 1, NULL, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(6, 2, '16.553.270-8', 'Sebastián Morales Gutiérrez', 'smorales@induztek.ejemplo', '+56900000006', '$CLAVE_DEFAULT_HASH', 1, NULL, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(7, 3, '13.871.905-7', 'Ricardo Espinoza Lagos', 'respinoza@induztek.ejemplo', '+56900000007', '$CLAVE_DEFAULT_HASH', 1, NULL, '¿Mascota?', '$RESP_DEFAULT_HASH')")

        db.execSQL("INSERT INTO $TABLA_USUARIO ($USR_ID, $USR_ROL_ID, $USR_RUT, $USR_NOMBRE, $USR_CORREO, $USR_TELEFONO, $USR_PASS, $USR_ACTIVO, $USR_CLIENTE_ID, $USR_PREGUNTA, $USR_RESPUESTA) VALUES " +
                "(8, 4, '14.226.791-8', 'Marcela Fuentes Rojas', 'mfuentes@envpacifico.ejemplo', '+56900000101', '$CLAVE_DEFAULT_HASH', 1, 1, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(9, 4, '96.731.540-0', 'Jorge Arancibia Leiva', 'jarancibia@svcentral.ejemplo', '+56900000102', '$CLAVE_DEFAULT_HASH', 1, 2, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(10, 4, '12.645.308-6', 'Patricio Navarro Soto', 'pnavarro@cerroalto.ejemplo', '+56900000103', '$CLAVE_DEFAULT_HASH', 1, 3, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(11, 4, '18.390.452-3', 'Carolina Méndez Pizarro', 'cmendez@losaromos.ejemplo', '+56900000104', '$CLAVE_DEFAULT_HASH', 1, 4, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(12, 4, '99.524.170-6', 'Luis Bravo Castillo', 'lbravo@lomasblancas.ejemplo', '+56900000105', '$CLAVE_DEFAULT_HASH', 1, 5, '¿Mascota?', '$RESP_DEFAULT_HASH'), " +
                "(13, 4, '78.214.635-1', 'Felipe Órdenes Vera', 'fordenes@andesnorte.ejemplo', '+56900000106', '$CLAVE_DEFAULT_HASH', 1, 6, '¿Mascota?', '$RESP_DEFAULT_HASH')")

        db.execSQL("INSERT INTO $TABLA_INSTALACION ($INS_ID, $INS_CLIENTE_ID, $INS_NOMBRE) VALUES (1, 8, 'Planta Lampa'), (2, 9, 'Planta Elevadora Sur'), (3, 10, 'Faena Til Til'), (4, 11, 'Edificio Clínico'), (5, 12, 'Planta Molienda'), (6, 13, 'Site Huechuraba')")

        db.execSQL("INSERT INTO $TABLA_TIPO_EQUIPO ($TEQ_ID, $TEQ_NOMBRE, $TEQ_CLASIFICACION) VALUES (1, 'Transformador', 'PRIMARIO'), (2, 'Celda MT', 'PRIMARIO'), (3, 'Interruptor', 'PRIMARIO'), (4, 'Cable de poder', 'PRIMARIO'), (5, 'Relé de protección', 'SECUNDARIO')")
        insertarInstrumentosIniciales(db)

        db.execSQL("INSERT INTO $TABLA_EQUIPO ($EQP_ID, $EQP_INSTALACION, $EQP_TAG, $EQP_TIPO_ID, $EQP_MARCA, $EQP_MODELO, $EQP_SERIE, $EQP_POTENCIA, $EQP_TENSIONES, $EQP_ANO, $EQP_TAP, $EQP_GRUPO, $EQP_MAX_CONTACTO) VALUES " +
                "(1, 1, 'TR-01', 1, 'Rhona', '1.500 kVA', 'RH-18733', 1500.0, '12 / 0.4 kV', '2012', '3', 'Dyn11', NULL), " +
                "(2, 1, 'TR-02', 1, 'Rhona', '1.000 kVA', 'RH-21904', 1000.0, '12 / 0.4 kV', '2017', '2', 'Dyn11', NULL), " +
                "(3, 2, 'TR-A', 1, 'Tusan', '2.500 kVA', 'TS-5521', 2500.0, '23 / 0.4 kV', '2009', '1', 'Dyn1', NULL), " +
                "(4, 4, 'CEL-MT-03', 2, 'Ormazabal', '12 kV, 630 A', 'OR-77812', 0, '12 kV', '2018', '', '', 60.0), " +
                "(5, 3, 'INT-52-1', 3, 'Schneider', '23 kV, 1.250 A', 'SE-449021', 0, '23 kV', '2015', '', '', 60.0), " +
                "(6, 5, 'REL-50/51-A', 5, 'SEL', 'Sobrecorriente', 'SEL-751-30219', 0, 'Ajuste 5 A', '2020', '', '', NULL), " +
                "(7, 6, 'CAB-MT-02', 4, 'Nexans', '15 kV, 3x240 mm2', 'NX-2201-B', 0, '15 kV', '2019', '', '', NULL), " +
                "(8, 3, 'TR-SS-01', 1, 'ABB', '5.000 kVA', 'ABB-1180442', 5000.0, '23 / 6.6 kV', '2011', '4', 'Dyn11', NULL)")

        db.execSQL("INSERT INTO $TABLA_PROTOCOLO ($PRT_ID, $PRT_NUMERO, $PRT_UUID, $PRT_VISITA, $PRT_EQUIPO_ID, $PRT_USUARIO_REGISTRA, $PRT_FECHA, $PRT_TEMP, $PRT_HUMEDAD, $PRT_OBSERVACIONES, $PRT_ESTADO, $PRT_RESULTADO_GLOBAL, lectura_1min, lectura_10min) VALUES " +
                "(1, 'PT-2024-0312', 'uuid-1', 1001, 1, 1, '2024-09-14 10:00:00', 18.0, 55.0, 'Pruebas iniciales', 'FORMALIZADO', 'APROBADO', 12500, 31250), " +
                "(2, 'PT-2025-0288', 'uuid-2', 1002, 2, 2, '2025-09-13 11:00:00', 21.0, 48.0, 'Pruebas anuales', 'FORMALIZADO', 'APROBADO', 9800, 21560), " +
                "(3, 'PT-2026-0401', 'uuid-3', 1187, 1, 1, '2026-09-26 13:42:00', 19.0, 60.0, 'Variación detectada', 'REGISTRADO_TERRENO', 'OBSERVADO', 6100, 10980), " +
                "(4, 'PT-2026-0402', 'uuid-4', 1187, 2, 1, '2026-09-26 15:00:00', 19.0, 60.0, 'TR-02 normal', 'REGISTRADO_TERRENO', 'APROBADO', 15000, 35000), " +
                "(5, 'PT-2026-0389', 'uuid-5', 1154, 4, 3, '2026-09-20 09:55:00', 22.0, 45.0, 'Falta nº serie instrumento', 'OBSERVADO', 'OBSERVADO', 0, 0), " +
                "(6, 'PT-2026-0377', 'uuid-6', 1003, 6, 4, '2026-09-15 10:00:00', 24.0, 35.0, 'Prueba de relé', 'FORMALIZADO', 'APROBADO', 0, 0), " +
                "(7, 'PT-2026-0360', 'uuid-7', 1004, 5, 2, '2026-09-08 12:00:00', 27.0, 20.0, 'Falla en contactos', 'FORMALIZADO', 'RECHAZADO', 0, 0)")
        asegurarHistorialInicial(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        crearTablasProtocoloExtendido(db)
        if (oldVersion == 12 && newVersion >= 13) {
            migrarAsignacionesExclusivas(db)
        }
        asegurarColumna(db, TABLA_PROTOCOLO, "id_usuario_formaliza", "INTEGER")
        asegurarColumna(db, TABLA_PROTOCOLO, "fecha_formalizacion", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, "motivo_observacion", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, PRT_OBSERVACIONES, "TEXT")
        db.execSQL(
            "UPDATE $TABLA_PROTOCOLO SET $PRT_OBSERVACIONES = descripcion_mantenimiento " +
                    "WHERE $PRT_OBSERVACIONES IS NULL AND descripcion_mantenimiento IS NOT NULL"
        )
        insertarInstrumentosIniciales(db)
        asegurarHistorialInicial(db)
    }

    private fun migrarAsignacionesExclusivas(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE asignacion_tecnico_instalacion_nueva (" +
                    "id_tecnico INTEGER NOT NULL REFERENCES $TABLA_USUARIO($USR_ID) ON DELETE CASCADE, " +
                    "id_instalacion INTEGER NOT NULL UNIQUE REFERENCES $TABLA_INSTALACION($INS_ID) ON DELETE CASCADE, " +
                    "asignado_por INTEGER NOT NULL REFERENCES $TABLA_USUARIO($USR_ID), " +
                    "fecha_asignacion TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (id_tecnico, id_instalacion))"
        )
        db.execSQL(
            "INSERT INTO asignacion_tecnico_instalacion_nueva " +
                    "(id_tecnico, id_instalacion, asignado_por, fecha_asignacion) " +
                    "SELECT a.id_tecnico, a.id_instalacion, a.asignado_por, a.fecha_asignacion " +
                    "FROM $TABLA_ASIGNACION_TECNICO a WHERE a.rowid = (" +
                    "SELECT MIN(a2.rowid) FROM $TABLA_ASIGNACION_TECNICO a2 " +
                    "WHERE a2.id_instalacion = a.id_instalacion)"
        )
        db.execSQL("DROP TABLE $TABLA_ASIGNACION_TECNICO")
        db.execSQL(
            "ALTER TABLE asignacion_tecnico_instalacion_nueva " +
                    "RENAME TO $TABLA_ASIGNACION_TECNICO"
        )
    }

    private fun asegurarHistorialInicial(db: SQLiteDatabase) {
        db.execSQL(
            "INSERT INTO $TABLA_HISTORIAL_ESTADO " +
                    "(id_protocolo, estado_anterior, estado_nuevo, id_usuario, fecha_hora, motivo) " +
                    "SELECT p.$PRT_ID, NULL, p.$PRT_ESTADO, p.$PRT_USUARIO_REGISTRA, p.$PRT_FECHA, " +
                    "'Historial inicial migrado' FROM $TABLA_PROTOCOLO p " +
                    "WHERE NOT EXISTS (SELECT 1 FROM $TABLA_HISTORIAL_ESTADO h " +
                    "WHERE h.id_protocolo = p.$PRT_ID)"
        )
    }

    private fun crearTablasProtocoloExtendido(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLA_INSTRUMENTO (" +
                    "id_instrumento INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL DEFAULT '', " +
                    "modelo TEXT NOT NULL, " +
                    "numero_serie TEXT NOT NULL UNIQUE, tipo_prueba TEXT NOT NULL, " +
                    "fecha_ultima_calibracion TEXT NOT NULL DEFAULT '', fecha_vencimiento TEXT NOT NULL, " +
                    "activo INTEGER NOT NULL DEFAULT 1)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLA_VISITA (" +
                    "id_visita INTEGER PRIMARY KEY AUTOINCREMENT, numero_ot TEXT NOT NULL, " +
                    "fecha_visita TEXT NOT NULL, id_instalacion INTEGER NOT NULL REFERENCES $TABLA_INSTALACION($INS_ID), " +
                    "id_tecnico INTEGER NOT NULL REFERENCES $TABLA_USUARIO($USR_ID), " +
                    "temperatura_c REAL NOT NULL, humedad_pct REAL NOT NULL, tipo_visita TEXT NOT NULL, " +
                    "descripcion_mantenimiento TEXT, receptor_nombre TEXT NOT NULL, receptor_rut TEXT NOT NULL, " +
                    "receptor_cargo TEXT NOT NULL, fecha_recepcion TEXT NOT NULL, fecha_cierre TEXT)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLA_MEDICION (" +
                    "id_medicion INTEGER PRIMARY KEY AUTOINCREMENT, id_protocolo INTEGER NOT NULL " +
                    "REFERENCES $TABLA_PROTOCOLO($PRT_ID) ON DELETE CASCADE, tipo_prueba TEXT NOT NULL, " +
                    "nombre TEXT NOT NULL, valor_1 TEXT NOT NULL, valor_2 TEXT NOT NULL DEFAULT '', " +
                    "valor_3 TEXT NOT NULL DEFAULT '', valor_4 TEXT NOT NULL DEFAULT '', " +
                    "referencia TEXT NOT NULL DEFAULT '', " +
                    "unidad TEXT NOT NULL DEFAULT '', resultado TEXT NOT NULL, " +
                    "id_instrumento INTEGER REFERENCES $TABLA_INSTRUMENTO(id_instrumento))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLA_ASIGNACION_TECNICO (" +
                    "id_tecnico INTEGER NOT NULL REFERENCES $TABLA_USUARIO($USR_ID) ON DELETE CASCADE, " +
                    "id_instalacion INTEGER NOT NULL UNIQUE REFERENCES $TABLA_INSTALACION($INS_ID) ON DELETE CASCADE, " +
                    "asignado_por INTEGER NOT NULL REFERENCES $TABLA_USUARIO($USR_ID), " +
                    "fecha_asignacion TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (id_tecnico, id_instalacion))"
        )
        asegurarColumna(db, TABLA_PROTOCOLO, "id_usuario_formaliza", "INTEGER")
        asegurarColumna(db, TABLA_PROTOCOLO, "fecha_formalizacion", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, "motivo_observacion", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, "tipo_visita", "TEXT NOT NULL DEFAULT 'SOLO_PRUEBAS'")
        asegurarColumna(db, TABLA_PROTOCOLO, "descripcion_mantenimiento", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, "receptor_nombre", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, "receptor_rut", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, "receptor_cargo", "TEXT")
        asegurarColumna(db, TABLA_PROTOCOLO, "fecha_recepcion", "TEXT")
        asegurarColumna(db, TABLA_EQUIPO, EQP_ANO, "TEXT NOT NULL DEFAULT ''")
        asegurarColumna(db, TABLA_EQUIPO, EQP_TAP, "TEXT NOT NULL DEFAULT ''")
        asegurarColumna(db, TABLA_EQUIPO, EQP_GRUPO, "TEXT NOT NULL DEFAULT ''")
        asegurarColumna(db, TABLA_EQUIPO, EQP_MAX_CONTACTO, "REAL")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLA_EMPRESA (id_empresa_cliente INTEGER PRIMARY KEY, razon_social TEXT NOT NULL UNIQUE, rut TEXT NOT NULL UNIQUE, giro TEXT NOT NULL)")
        insertarEmpresasIniciales(db)
        listOf(8L to 1L, 9L to 2L, 10L to 3L, 11L to 4L, 12L to 5L, 13L to 6L).forEach { (idUsuario, idEmpresa) ->
            db.execSQL(
                "UPDATE $TABLA_USUARIO SET $USR_CLIENTE_ID = ? WHERE $USR_ID = ? AND $USR_ROL_ID = ?",
                arrayOf(idEmpresa, idUsuario, Rol.ID_CLIENTE)
            )
        }
        db.execSQL("UPDATE $TABLA_EQUIPO SET $EQP_ANO = CASE $EQP_ID WHEN 1 THEN '2012' WHEN 2 THEN '2017' WHEN 3 THEN '2009' WHEN 4 THEN '2018' WHEN 5 THEN '2015' WHEN 6 THEN '2020' WHEN 7 THEN '2019' WHEN 8 THEN '2011' ELSE $EQP_ANO END WHERE $EQP_ANO = ''")
        db.execSQL("UPDATE $TABLA_EQUIPO SET $EQP_TAP = CASE $EQP_ID WHEN 1 THEN '3' WHEN 2 THEN '2' WHEN 3 THEN '1' WHEN 8 THEN '4' ELSE $EQP_TAP END WHERE $EQP_TAP = ''")
        db.execSQL("UPDATE $TABLA_EQUIPO SET $EQP_GRUPO = CASE $EQP_ID WHEN 1 THEN 'Dyn11' WHEN 2 THEN 'Dyn11' WHEN 3 THEN 'Dyn1' WHEN 8 THEN 'Dyn11' ELSE $EQP_GRUPO END WHERE $EQP_GRUPO = ''")
        db.execSQL("UPDATE $TABLA_EQUIPO SET $EQP_MAX_CONTACTO = 60.0 WHERE $EQP_ID IN (4, 5) AND $EQP_MAX_CONTACTO IS NULL")
    }

    private fun asegurarColumna(db: SQLiteDatabase, tabla: String, columna: String, definicion: String) {
        db.rawQuery("PRAGMA table_info($tabla)", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == columna) return
            }
        }
        db.execSQL("ALTER TABLE $tabla ADD COLUMN $columna $definicion")
    }

    private fun insertarInstrumentosIniciales(db: SQLiteDatabase) {
        val instrumentos = listOf(
            arrayOf("Megóhmetro 5 kV", "Megger MIT525", "MIT-0045821", "AISLACION", "15-03-2026", "15-03-2027"),
            arrayOf("Megóhmetro 10 kV", "Megger MIT1025", "MIT-0071233", "AISLACION", "10-10-2025", "10-10-2026"),
            arrayOf("Medidor de relación (TTR)", "Megger TTR300", "TTR-30044", "TTR", "02-06-2026", "02-06-2027"),
            arrayOf("Micro-ohmímetro", "Megger DLRO10X", "DLR-10882", "DEVANADOS", "20-08-2025", "20-08-2026"),
            arrayOf("Micro-ohmímetro", "Megger DLRO10X", "DLR-20914", "DEVANADOS", "20-08-2026", "20-08-2027"),
            arrayOf("Equipo de inyección secundaria", "Omicron CMC 356", "CMC-HB411", "RELE", "05-01-2026", "05-01-2027")
        )
        instrumentos.forEach { instrumento ->
            val nombre = instrumento[0]
            val modelo = instrumento[1]
            val serie = instrumento[2]
            val tipo = instrumento[3]
            val ultimaCalibracion = instrumento[4]
            val vencimiento = instrumento[5]
            val valores = ContentValues().apply {
                put("nombre", nombre)
                put("modelo", modelo)
                put("numero_serie", serie)
                put("tipo_prueba", tipo)
                put("fecha_ultima_calibracion", ultimaCalibracion)
                put("fecha_vencimiento", vencimiento)
            }
            db.insertWithOnConflict(TABLA_INSTRUMENTO, null, valores, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun insertarEmpresasIniciales(db: SQLiteDatabase) {
        val empresas = listOf(
            arrayOf("1", "Envasadora del Pacífico S.A.", "76.482.915-8", "Agroindustria"),
            arrayOf("2", "Sanitaria Valle Central S.A.", "96.731.540-0", "Servicios sanitarios"),
            arrayOf("3", "Minera Cerro Alto SpA", "77.105.362-9", "Minería"),
            arrayOf("4", "Clínica Los Aromos Ltda.", "76.903.218-5", "Salud"),
            arrayOf("5", "Cementos Lomas Blancas S.A.", "99.524.170-6", "Construcción"),
            arrayOf("6", "Data Center Andes Norte SpA", "78.214.635-1", "Centro de datos")
        )
        empresas.forEach { empresa ->
            val id = empresa[0]
            val nombre = empresa[1]
            val rut = empresa[2]
            val giro = empresa[3]
            val values = ContentValues().apply {
                put("id_empresa_cliente", id.toLong())
                put("razon_social", nombre)
                put("rut", rut)
                put("giro", giro)
            }
            db.insertWithOnConflict(TABLA_EMPRESA, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun autenticarUsuario(correoInput: String, claveInput: String): Usuario? {
        val claveHash = claveInput.trim().toSha256()
        val sql = "$SQL_SELECT_USUARIOS WHERE LOWER(u.$USR_CORREO) = LOWER(?) AND u.$USR_PASS = ? AND u.$USR_ACTIVO = 1"
        return consultarUsuarios(sql, arrayOf(correoInput.trim(), claveHash)).firstOrNull()
    }

    fun registrarNuevoUsuario(
        rut: String,
        nombre: String,
        correo: String,
        telefono: String,
        clavePlana: String,
        rolCodigo: String,
        pregunta: String,
        respuestaPlana: String,
        idEmpresaCliente: Long? = null
    ): Long {
        val valores = ContentValues().apply {
            put(USR_RUT, rut.trim())
            put(USR_NOMBRE, nombre.trim())
            put(USR_CORREO, correo.trim().lowercase())
            put(USR_TELEFONO, telefono.trim())
            put(USR_PASS, clavePlana.trim().toSha256())
            put(USR_ROL_ID, if (rolCodigo == "TECNICO") 1L else if (rolCodigo == "INGENIERO") 2L else if (rolCodigo == "JEFE_TECNICO") 3L else 4L)
            put(USR_ACTIVO, 1)
            put(USR_CLIENTE_ID, idEmpresaCliente)
            put(USR_PREGUNTA, pregunta)
            put(USR_RESPUESTA, respuestaPlana.trim().lowercase().toSha256())
        }
        return writableDatabase.insert(TABLA_USUARIO, null, valores)
    }

    fun obtenerPreguntaSeguridad(correo: String): String? {
        val cursor = readableDatabase.rawQuery("SELECT $USR_PREGUNTA FROM $TABLA_USUARIO WHERE LOWER($USR_CORREO) = LOWER(?) AND $USR_ACTIVO = 1", arrayOf(correo.trim()))
        var pregunta: String? = null
        if (cursor.moveToFirst()) pregunta = cursor.getString(0)
        cursor.close()
        return pregunta
    }

    fun recuperarClaveConRespuesta(correo: String, respuestaPlana: String, nuevaClavePlana: String): Boolean {
        val db = writableDatabase
        val respuestaHash = respuestaPlana.trim().lowercase().toSha256()
        val nuevaClaveHash = nuevaClavePlana.trim().toSha256()
        val cursor = db.rawQuery("SELECT $USR_ID FROM $TABLA_USUARIO WHERE LOWER($USR_CORREO) = LOWER(?) AND $USR_RESPUESTA = ?", arrayOf(correo.trim(), respuestaHash))
        val valido = cursor.moveToFirst()
        cursor.close()
        if (!valido) return false
        val valores = ContentValues().apply { put(USR_PASS, nuevaClaveHash) }
        return db.update(TABLA_USUARIO, valores, "LOWER($USR_CORREO) = LOWER(?)", arrayOf(correo.trim())) > 0
    }

    fun cambiarClave(idUsuario: Long, claveActualPlana: String, claveNuevaPlana: String): Boolean {
        val db = writableDatabase
        val hashActual = claveActualPlana.trim().toSha256()
        val hashNuevo = claveNuevaPlana.trim().toSha256()
        val cursor = db.rawQuery("SELECT $USR_ID FROM $TABLA_USUARIO WHERE $USR_ID = ? AND $USR_PASS = ?", arrayOf(idUsuario.toString(), hashActual))
        val claveValida = cursor.moveToFirst()
        cursor.close()
        if (!claveValida) return false
        val valores = ContentValues().apply { put(USR_PASS, hashNuevo) }
        return db.update(TABLA_USUARIO, valores, "$USR_ID = ?", arrayOf(idUsuario.toString())) > 0
    }

    fun actualizarFotoUsuario(idUsuario: Long, fotoBytes: ByteArray): Boolean {
        val valores = ContentValues().apply { put(USR_FOTO, fotoBytes) }
        return writableDatabase.update(TABLA_USUARIO, valores, "$USR_ID = ?", arrayOf(idUsuario.toString())) > 0
    }

    fun actualizarPerfilUsuario(idUsuario: Long, nombre: String, telefono: String): Boolean {
        val nombreLimpio = nombre.trim()
        val telefonoLimpio = telefono.trim()
        if (nombreLimpio.isBlank() || telefonoLimpio.isBlank()) return false
        val valores = ContentValues().apply {
            put(USR_NOMBRE, nombreLimpio)
            put(USR_TELEFONO, telefonoLimpio)
        }
        return writableDatabase.update(
            TABLA_USUARIO,
            valores,
            "$USR_ID = ? AND $USR_ACTIVO = 1",
            arrayOf(idUsuario.toString())
        ) == 1
    }

    fun buscarPorIdUsuario(idUsuario: Long): Usuario? {
        val sql = "$SQL_SELECT_USUARIOS WHERE u.$USR_ID = ?"
        return consultarUsuarios(sql, arrayOf(idUsuario.toString())).firstOrNull()
    }

    fun listarProtocolosPorEquipo(idEquipo: Long): List<ProtocoloTerreno> =
        consultarProtocolos("$SQL_SELECT_PROTOCOLOS WHERE p.$PRT_EQUIPO_ID = ? ORDER BY p.$PRT_FECHA DESC", arrayOf(idEquipo.toString()))

    fun listarClientes(): List<ClienteCatalogoLocal> =
        readableDatabase.rawQuery(
            "SELECT DISTINCT ec.id_empresa_cliente, ec.razon_social FROM $TABLA_EMPRESA ec " +
                    "INNER JOIN $TABLA_USUARIO uc ON uc.$USR_CLIENTE_ID = ec.id_empresa_cliente " +
                    "INNER JOIN $TABLA_INSTALACION i ON i.$INS_CLIENTE_ID = uc.$USR_ID " +
                    "ORDER BY ec.razon_social",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(ClienteCatalogoLocal(cursor.getLong(0), cursor.getString(1)))
                }
            }
        }

    fun listarClientesTecnico(idTecnico: Long): List<ClienteCatalogoLocal> =
        readableDatabase.rawQuery(
            "SELECT DISTINCT ec.id_empresa_cliente, ec.razon_social " +
                    "FROM $TABLA_ASIGNACION_TECNICO a " +
                    "INNER JOIN $TABLA_INSTALACION i ON a.id_instalacion = i.$INS_ID " +
                    "INNER JOIN $TABLA_USUARIO uc ON i.$INS_CLIENTE_ID = uc.$USR_ID " +
                    "INNER JOIN $TABLA_EMPRESA ec ON uc.$USR_CLIENTE_ID = ec.id_empresa_cliente " +
                    "WHERE a.id_tecnico = ? ORDER BY ec.razon_social",
            arrayOf(idTecnico.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(ClienteCatalogoLocal(cursor.getLong(0), cursor.getString(1)))
                }
            }
        }

    fun listarTecnicosActivos(): List<Usuario> =
        consultarUsuarios(
            "$SQL_SELECT_USUARIOS WHERE u.$USR_ROL_ID = ? AND u.$USR_ACTIVO = 1 ORDER BY u.$USR_NOMBRE",
            arrayOf(Rol.ID_TECNICO.toString())
        )

    fun listarInstalacionesParaAsignacion(): List<InstalacionAsignacionLocal> =
        readableDatabase.rawQuery(
            "SELECT i.$INS_ID, i.$INS_NOMBRE, ec.razon_social " +
                    "FROM $TABLA_INSTALACION i " +
                    "INNER JOIN $TABLA_USUARIO uc ON i.$INS_CLIENTE_ID = uc.$USR_ID " +
                    "LEFT JOIN $TABLA_EMPRESA ec ON uc.$USR_CLIENTE_ID = ec.id_empresa_cliente " +
                    "ORDER BY ec.razon_social, i.$INS_NOMBRE",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        InstalacionAsignacionLocal(
                            idInstalacion = cursor.getLong(0),
                            nombre = cursor.getString(1),
                            empresa = if (cursor.isNull(2)) "Sin empresa" else cursor.getString(2)
                        )
                    )
                }
            }
        }

    fun listarIdsInstalacionesTecnico(idTecnico: Long): Set<Long> =
        readableDatabase.rawQuery(
            "SELECT id_instalacion FROM $TABLA_ASIGNACION_TECNICO WHERE id_tecnico = ?",
            arrayOf(idTecnico.toString())
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getLong(0))
            }
        }

    fun listarTecnicosPropietariosDeInstalaciones(idTecnico: Long): Map<Long, String> =
        readableDatabase.rawQuery(
            "SELECT a.id_instalacion, u.$USR_NOMBRE " +
                    "FROM $TABLA_ASIGNACION_TECNICO a " +
                    "INNER JOIN $TABLA_USUARIO u ON a.id_tecnico = u.$USR_ID " +
                    "WHERE a.id_tecnico != ?",
            arrayOf(idTecnico.toString())
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) put(cursor.getLong(0), cursor.getString(1))
            }
        }

    fun listarInstalacionesTecnico(
        idEmpresa: Long,
        idTecnico: Long
    ): List<InstalacionCatalogoLocal> =
        readableDatabase.rawQuery(
            "SELECT i.$INS_ID, i.$INS_NOMBRE FROM $TABLA_INSTALACION i " +
                    "INNER JOIN $TABLA_USUARIO uc ON i.$INS_CLIENTE_ID = uc.$USR_ID " +
                    "INNER JOIN $TABLA_ASIGNACION_TECNICO a ON a.id_instalacion = i.$INS_ID " +
                    "WHERE uc.$USR_CLIENTE_ID = ? AND a.id_tecnico = ? " +
                    "ORDER BY i.$INS_NOMBRE",
            arrayOf(idEmpresa.toString(), idTecnico.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(InstalacionCatalogoLocal(cursor.getLong(0), cursor.getString(1)))
                }
            }
        }

    fun tecnicoTieneAccesoAInstalacion(idTecnico: Long, idInstalacion: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLA_ASIGNACION_TECNICO WHERE id_tecnico = ? AND id_instalacion = ?",
            arrayOf(idTecnico.toString(), idInstalacion.toString())
        ).use { it.moveToFirst() }

    fun tecnicoPuedeVerEquipo(idTecnico: Long, idEquipo: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLA_EQUIPO e " +
                    "INNER JOIN $TABLA_ASIGNACION_TECNICO a ON a.id_instalacion = e.$EQP_INSTALACION " +
                    "WHERE e.$EQP_ID = ? AND a.id_tecnico = ?",
            arrayOf(idEquipo.toString(), idTecnico.toString())
        ).use { it.moveToFirst() }

    fun tecnicoPuedeVerProtocolo(idTecnico: Long, idProtocolo: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLA_PROTOCOLO p " +
                    "INNER JOIN $TABLA_EQUIPO e ON p.$PRT_EQUIPO_ID = e.$EQP_ID " +
                    "INNER JOIN $TABLA_ASIGNACION_TECNICO a ON a.id_instalacion = e.$EQP_INSTALACION " +
                    "WHERE p.$PRT_ID = ? AND a.id_tecnico = ?",
            arrayOf(idProtocolo.toString(), idTecnico.toString())
        ).use { it.moveToFirst() }

    fun guardarAsignacionesTecnico(
        idTecnico: Long,
        idsInstalaciones: Set<Long>,
        idJefeTecnico: Long
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val rolesValidos = db.rawQuery(
                "SELECT " +
                        "(SELECT $USR_ROL_ID FROM $TABLA_USUARIO WHERE $USR_ID = ? AND $USR_ACTIVO = 1), " +
                        "(SELECT $USR_ROL_ID FROM $TABLA_USUARIO WHERE $USR_ID = ? AND $USR_ACTIVO = 1)",
                arrayOf(idTecnico.toString(), idJefeTecnico.toString())
            ).use { cursor ->
                cursor.moveToFirst() &&
                        !cursor.isNull(0) && cursor.getLong(0) == Rol.ID_TECNICO &&
                        !cursor.isNull(1) && cursor.getLong(1) == Rol.ID_JEFE_TECNICO
            }
            if (!rolesValidos) return false

            idsInstalaciones.forEach { idInstalacion ->
                val existe = db.rawQuery(
                    "SELECT 1 FROM $TABLA_INSTALACION WHERE $INS_ID = ?",
                    arrayOf(idInstalacion.toString())
                ).use { it.moveToFirst() }
                if (!existe) return false
                val asignadaAOtroTecnico = db.rawQuery(
                    "SELECT 1 FROM $TABLA_ASIGNACION_TECNICO " +
                            "WHERE id_instalacion = ? AND id_tecnico != ?",
                    arrayOf(idInstalacion.toString(), idTecnico.toString())
                ).use { it.moveToFirst() }
                if (asignadaAOtroTecnico) return false
            }

            db.delete(TABLA_ASIGNACION_TECNICO, "id_tecnico = ?", arrayOf(idTecnico.toString()))
            idsInstalaciones.forEach { idInstalacion ->
                val valores = ContentValues().apply {
                    put("id_tecnico", idTecnico)
                    put("id_instalacion", idInstalacion)
                    put("asignado_por", idJefeTecnico)
                }
                if (db.insert(TABLA_ASIGNACION_TECNICO, null, valores) == -1L) return false
            }
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun buscarContactoEmpresa(idEmpresa: Long): Usuario? =
        consultarUsuarios(
            "$SQL_SELECT_USUARIOS WHERE u.$USR_CLIENTE_ID = ? AND r.$ROL_CODIGO = 'CLIENTE' LIMIT 1",
            arrayOf(idEmpresa.toString())
        ).firstOrNull()

    fun listarInstalacionesCliente(idEmpresa: Long): List<InstalacionCatalogoLocal> =
        readableDatabase.rawQuery(
            "SELECT i.$INS_ID, i.$INS_NOMBRE FROM $TABLA_INSTALACION i " +
                    "INNER JOIN $TABLA_USUARIO uc ON i.$INS_CLIENTE_ID = uc.$USR_ID " +
                    "WHERE uc.$USR_CLIENTE_ID = ? ORDER BY i.$INS_NOMBRE",
            arrayOf(idEmpresa.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(InstalacionCatalogoLocal(cursor.getLong(0), cursor.getString(1)))
                }
            }
        }

    fun listarEquiposPorInstalacion(idInstalacion: Long): List<EquipoCatalogoLocal> =
        readableDatabase.rawQuery(
            "SELECT e.$EQP_ID, e.$EQP_INSTALACION, e.$EQP_TAG, te.$TEQ_NOMBRE, " +
                    "te.$TEQ_CLASIFICACION, e.$EQP_MARCA, e.$EQP_MODELO, e.$EQP_SERIE, " +
                    "e.$EQP_POTENCIA, e.$EQP_TENSIONES, e.$EQP_ANO, e.$EQP_TAP, e.$EQP_GRUPO, " +
                    "e.$EQP_MAX_CONTACTO " +
                    "FROM $TABLA_EQUIPO e INNER JOIN $TABLA_TIPO_EQUIPO te " +
                    "ON e.$EQP_TIPO_ID = te.$TEQ_ID WHERE e.$EQP_INSTALACION = ? " +
                    "ORDER BY e.$EQP_TAG",
            arrayOf(idInstalacion.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        EquipoCatalogoLocal(
                            idEquipo = cursor.getLong(0),
                            idInstalacion = cursor.getLong(1),
                            tag = cursor.getString(2),
                            tipo = cursor.getString(3),
                            clasificacion = cursor.getString(4),
                            marca = cursor.getString(5),
                            modelo = cursor.getString(6),
                            numeroSerie = cursor.getString(7),
                            potenciaKva = if (cursor.isNull(8)) null else cursor.getDouble(8),
                            tensiones = cursor.getString(9),
                            anoFabricacion = cursor.getString(10),
                            posicionTap = cursor.getString(11),
                            grupoConexion = cursor.getString(12),
                            resistenciaContactoMaxUohm = if (cursor.isNull(13)) null else cursor.getDouble(13)
                        )
                    )
                }
            }
        }

    fun listarInstrumentos(tipoPrueba: String): List<InstrumentoItem> =
        readableDatabase.rawQuery(
            "SELECT id_instrumento, nombre, modelo, numero_serie, fecha_ultima_calibracion, fecha_vencimiento " +
                    "FROM $TABLA_INSTRUMENTO WHERE tipo_prueba = ? AND activo = 1 ORDER BY modelo",
            arrayOf(tipoPrueba)
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        InstrumentoItem(
                            idInstrumento = cursor.getLong(0),
                            modelo = cursor.getString(2),
                            numeroSerie = cursor.getString(3),
                            fechaVencimiento = cursor.getString(5).replace('-', '/'),
                            nombre = cursor.getString(1),
                            fechaUltimaCalibracion = cursor.getString(4).replace('-', '/')
                        )
                    )
                }
            }
        }

    fun listarProtocolosCliente(idEmpresa: Long): List<ProtocoloTerreno> =
        consultarProtocolos(
            "$SQL_SELECT_PROTOCOLOS WHERE u_cli.$USR_CLIENTE_ID = ? " +
                    "AND p.$PRT_ESTADO NOT IN ('BORRADOR', 'ANULADO') " +
                    "ORDER BY p.$PRT_FECHA DESC",
            arrayOf(idEmpresa.toString())
        )

    fun listarProtocolosEquipoCliente(idEquipo: Long, idEmpresa: Long): List<ProtocoloTerreno> =
        consultarProtocolos(
            "$SQL_SELECT_PROTOCOLOS WHERE p.$PRT_EQUIPO_ID = ? AND u_cli.$USR_CLIENTE_ID = ? " +
                    "AND p.$PRT_ESTADO NOT IN ('BORRADOR', 'ANULADO') " +
                    "ORDER BY p.$PRT_FECHA DESC",
            arrayOf(idEquipo.toString(), idEmpresa.toString())
        )

    fun clientePuedeVerProtocolo(idProtocolo: Long, idEmpresa: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLA_PROTOCOLO p " +
                    "INNER JOIN $TABLA_EQUIPO e ON p.$PRT_EQUIPO_ID = e.$EQP_ID " +
                    "INNER JOIN $TABLA_INSTALACION i ON e.$EQP_INSTALACION = i.$INS_ID " +
                    "INNER JOIN $TABLA_USUARIO uc ON i.$INS_CLIENTE_ID = uc.$USR_ID " +
                    "WHERE p.$PRT_ID = ? AND uc.$USR_CLIENTE_ID = ? " +
                    "AND p.$PRT_ESTADO NOT IN ('BORRADOR', 'ANULADO')",
            arrayOf(idProtocolo.toString(), idEmpresa.toString())
        ).use { it.moveToFirst() }

    fun obtenerProtocoloPorId(idProtocolo: Long): ProtocoloTerreno? =
        consultarProtocolos("$SQL_SELECT_PROTOCOLOS WHERE p.$PRT_ID = ?", arrayOf(idProtocolo.toString())).firstOrNull()

    fun listarProtocolosPorEstado(estado: String? = null): List<ProtocoloTerreno> =
        if (estado.isNullOrBlank()) {
            consultarProtocolos("$SQL_SELECT_PROTOCOLOS ORDER BY p.$PRT_FECHA DESC", null)
        } else {
            consultarProtocolos(
                "$SQL_SELECT_PROTOCOLOS WHERE p.$PRT_ESTADO = ? ORDER BY p.$PRT_FECHA DESC",
                arrayOf(estado)
            )
        }

    fun registrarProtocoloLocal(registro: RegistroProtocolo): Long {
        return registrarProtocolosVisitaLocal(listOf(registro)).firstOrNull() ?: -1L
    }

    fun registrarProtocolosVisitaLocal(registros: List<RegistroProtocolo>): List<Long> {
        if (registros.isEmpty()) return emptyList()
        val db = writableDatabase
        db.beginTransaction()
        try {
            val visita = registros.first()
            val mismaVisita = registros.all {
                it.numeroOt == visita.numeroOt &&
                        it.fechaHora == visita.fechaHora &&
                        it.temperaturaC == visita.temperaturaC &&
                        it.humedadPct == visita.humedadPct &&
                        it.tipoVisita == visita.tipoVisita &&
                        it.descripcionMantenimiento == visita.descripcionMantenimiento &&
                        it.receptorNombre == visita.receptorNombre &&
                        it.receptorRut == visita.receptorRut &&
                        it.receptorCargo == visita.receptorCargo &&
                        it.fechaRecepcion == visita.fechaRecepcion &&
                        it.idUsuarioRegistra == visita.idUsuarioRegistra
            }
            if (!mismaVisita) return emptyList()
            if (
                visita.numeroOt.isBlank() ||
                visita.tipoVisita !in setOf("SOLO_PRUEBAS", "PRUEBAS_Y_MANTENIMIENTO") ||
                visita.humedadPct !in 0.0..100.0 ||
                !visita.temperaturaC.isFinite() ||
                !ValidarRut.validarRut(visita.receptorRut) ||
                visita.receptorNombre.isBlank() ||
                visita.receptorCargo.isBlank() ||
                (visita.tipoVisita == "PRUEBAS_Y_MANTENIMIENTO" && visita.descripcionMantenimiento.isNullOrBlank()) ||
                registros.any { it.estado != "REGISTRADO_TERRENO" || !it.temperaturaC.isFinite() }
            ) return emptyList()

            val rolUsuario = db.rawQuery(
                "SELECT $USR_ROL_ID FROM $TABLA_USUARIO WHERE $USR_ID = ? AND $USR_ACTIVO = 1",
                arrayOf(visita.idUsuarioRegistra.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
            if (rolUsuario == null || rolUsuario == Rol.ID_CLIENTE) return emptyList()

            val instalaciones = registros.map { registro ->
                db.rawQuery(
                    "SELECT $EQP_INSTALACION FROM $TABLA_EQUIPO WHERE $EQP_ID = ?",
                    arrayOf(registro.idEquipo.toString())
                ).use { cursor ->
                    if (!cursor.moveToFirst()) return emptyList()
                    cursor.getLong(0)
                }
            }
            if (instalaciones.any { it != instalaciones.first() }) return emptyList()
            val instalacionId = instalaciones.first()
            if (
                rolUsuario == Rol.ID_TECNICO &&
                !tecnicoTieneAccesoAInstalacion(visita.idUsuarioRegistra, instalacionId)
            ) return emptyList()

            registros.flatMap { it.mediciones }.mapNotNull { it.idInstrumento }.distinct().forEach { idInstrumento ->
                val vigencia = db.rawQuery(
                    "SELECT fecha_vencimiento, activo FROM $TABLA_INSTRUMENTO WHERE id_instrumento = ?",
                    arrayOf(idInstrumento.toString())
                ).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(1) == 1 &&
                            fechaCalibracionVigente(cursor.getString(0), visita.fechaHora)
                }
                if (!vigencia) return emptyList()
            }
            val tipoInstrumentoEsperado = mapOf(
                "AISLACION" to "AISLACION",
                "TTR" to "TTR",
                "DEVANADOS" to "DEVANADOS",
                "CONTACTOS" to "DEVANADOS",
                "RELE" to "RELE"
            )
            registros.flatMap { it.mediciones }.forEach { medicion ->
                val tipoEsperado = tipoInstrumentoEsperado[medicion.tipoPrueba] ?: return emptyList()
                val tipoReal = medicion.idInstrumento?.let { idInstrumento ->
                    db.rawQuery(
                        "SELECT tipo_prueba FROM $TABLA_INSTRUMENTO WHERE id_instrumento = ? AND activo = 1",
                        arrayOf(idInstrumento.toString())
                    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                }
                if (tipoReal != tipoEsperado) return emptyList()
            }

            val valoresVisita = ContentValues().apply {
                put("numero_ot", visita.numeroOt)
                put("fecha_visita", visita.fechaHora)
                put("id_instalacion", instalacionId)
                put("id_tecnico", visita.idUsuarioRegistra)
                put("temperatura_c", visita.temperaturaC)
                put("humedad_pct", visita.humedadPct)
                put("tipo_visita", visita.tipoVisita)
                put("descripcion_mantenimiento", visita.descripcionMantenimiento)
                put("receptor_nombre", visita.receptorNombre)
                put("receptor_rut", visita.receptorRut)
                put("receptor_cargo", visita.receptorCargo)
                put("fecha_recepcion", visita.fechaRecepcion)
                put("fecha_cierre", visita.fechaRecepcion)
            }
            val idVisita = db.insert(TABLA_VISITA, null, valoresVisita)
            if (idVisita == -1L) return emptyList()

            val year = visita.fechaHora.take(4).toIntOrNull()
                ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val idsProtocolo = mutableListOf<Long>()
            registros.forEach { registro ->
                val nextNumber = db.rawQuery(
                    "SELECT COALESCE(MAX(CAST(SUBSTR($PRT_NUMERO, 9) AS INTEGER)), 0) + 1 " +
                            "FROM $TABLA_PROTOCOLO WHERE $PRT_NUMERO LIKE ?",
                    arrayOf("PT-$year-%")
                ).use { cursor ->
                    cursor.moveToFirst()
                    cursor.getInt(0)
                }
                val numeroProtocolo = "PT-$year-${nextNumber.toString().padStart(4, '0')}"
            val valoresProtocolo = ContentValues().apply {
                put(PRT_NUMERO, numeroProtocolo)
                put(PRT_UUID, UUID.randomUUID().toString())
                put(PRT_VISITA, idVisita)
                put(PRT_EQUIPO_ID, registro.idEquipo)
                put(PRT_USUARIO_REGISTRA, registro.idUsuarioRegistra)
                put(PRT_FECHA, registro.fechaHora)
                put(PRT_TEMP, registro.temperaturaC)
                put(PRT_HUMEDAD, registro.humedadPct)
                put(PRT_OBSERVACIONES, registro.observaciones)
                put(PRT_ESTADO, registro.estado)
                put(PRT_RESULTADO_GLOBAL, registro.resultadoGlobal)
                put(PRT_SYNC, 0)
                put("tipo_visita", registro.tipoVisita)
                put("descripcion_mantenimiento", registro.descripcionMantenimiento)
                put("receptor_nombre", registro.receptorNombre)
                put("receptor_rut", registro.receptorRut)
                put("receptor_cargo", registro.receptorCargo)
                put("fecha_recepcion", registro.fechaRecepcion)
                put("lectura_1min", registro.mediciones.firstOrNull { it.tipoPrueba == "AISLACION" }
                    ?.valor1?.replace(',', '.')?.toDoubleOrNull())
                put("lectura_10min", registro.mediciones.firstOrNull { it.tipoPrueba == "AISLACION" }
                    ?.valor2?.replace(',', '.')?.toDoubleOrNull())
            }
            val idProtocolo = db.insert(TABLA_PROTOCOLO, null, valoresProtocolo)
            if (idProtocolo == -1L) return emptyList()
            val historialInicial = ContentValues().apply {
                put("id_protocolo", idProtocolo)
                putNull("estado_anterior")
                put("estado_nuevo", registro.estado)
                put("id_usuario", registro.idUsuarioRegistra)
                put("fecha_hora", registro.fechaHora)
                put("motivo", "Registro inicial en terreno")
            }
            if (db.insert(TABLA_HISTORIAL_ESTADO, null, historialInicial) == -1L) return emptyList()
            idsProtocolo.add(idProtocolo)

            registro.mediciones.forEach { medicion ->
                val valoresMedicion = ContentValues().apply {
                    put("id_protocolo", idProtocolo)
                    put("tipo_prueba", medicion.tipoPrueba)
                    put("nombre", medicion.nombre)
                    put("valor_1", medicion.valor1)
                    put("valor_2", medicion.valor2)
                    put("valor_3", medicion.valor3)
                    put("valor_4", medicion.valor4)
                    put("referencia", medicion.referencia)
                    put("unidad", medicion.unidad)
                    put("resultado", medicion.resultado)
                    if (medicion.idInstrumento == null) putNull("id_instrumento")
                    else put("id_instrumento", medicion.idInstrumento)
                }
                if (db.insert(TABLA_MEDICION, null, valoresMedicion) == -1L) return emptyList()
            }
            }

            db.setTransactionSuccessful()
            return idsProtocolo
        } finally {
            db.endTransaction()
        }
    }

    private fun fechaCalibracionVigente(fechaVencimiento: String, fechaPrueba: String): Boolean {
        val formatoVencimiento = SimpleDateFormat("dd-MM-yyyy", Locale.ROOT).apply { isLenient = false }
        val formatoEnsayo = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        val fechaVenc = runCatching { formatoVencimiento.parse(fechaVencimiento) }.getOrNull() ?: return false
        val fechaEnsayo = runCatching {
            formatoEnsayo.parse(fechaPrueba.take(10))
        }.getOrNull() ?: return false
        val formatoComparable = SimpleDateFormat("yyyyMMdd", Locale.ROOT)
        return formatoComparable.format(fechaVenc) >= formatoComparable.format(fechaEnsayo)
    }

    private fun listarMediciones(idProtocolo: Long): List<MedicionProtocolo> =
        readableDatabase.rawQuery(
            "SELECT m.tipo_prueba, m.nombre, m.valor_1, m.valor_2, m.valor_3, m.valor_4, " +
                    "m.referencia, m.unidad, m.resultado, m.id_instrumento, " +
                    "i.modelo || ' (S/N: ' || i.numero_serie || ')' AS instrumento " +
                    "FROM $TABLA_MEDICION m LEFT JOIN $TABLA_INSTRUMENTO i " +
                    "ON m.id_instrumento = i.id_instrumento " +
                    "WHERE m.id_protocolo = ? ORDER BY m.id_medicion",
            arrayOf(idProtocolo.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MedicionProtocolo(
                            tipoPrueba = cursor.getString(0),
                            nombre = cursor.getString(1),
                            valor1 = cursor.getString(2),
                            valor2 = cursor.getString(3),
                            valor3 = cursor.getString(4),
                            valor4 = cursor.getString(5),
                            referencia = cursor.getString(6),
                            unidad = cursor.getString(7),
                            resultado = cursor.getString(8),
                            idInstrumento = if (cursor.isNull(9)) null else cursor.getLong(9),
                            instrumento = if (cursor.isNull(10)) "" else cursor.getString(10)
                        )
                    )
                }
            }
        }

    fun cambiarEstadoProtocolo(idProtocolo: Long, nuevoEstado: String, idUsuario: Long, motivo: String? = null): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val estadoAnt = db.rawQuery(
                "SELECT $PRT_ESTADO FROM $TABLA_PROTOCOLO WHERE $PRT_ID = ?",
                arrayOf(idProtocolo.toString())
            ).use { cursor ->
                if (!cursor.moveToFirst()) return false
                cursor.getString(0)
            }
            val rolId = db.rawQuery(
                "SELECT $USR_ROL_ID FROM $TABLA_USUARIO WHERE $USR_ID = ? AND $USR_ACTIVO = 1",
                arrayOf(idUsuario.toString())
            ).use { cursor ->
                if (!cursor.moveToFirst()) return false
                cursor.getLong(0)
            }
            val transicionValida = when (estadoAnt) {
                "REGISTRADO_TERRENO" -> nuevoEstado in setOf("FORMALIZADO", "OBSERVADO", "ANULADO")
                "OBSERVADO" -> nuevoEstado in setOf("FORMALIZADO", "ANULADO")
                else -> false
            }
            if (!transicionValida) return false
            if (rolId !in setOf(Rol.ID_INGENIERO, Rol.ID_JEFE_TECNICO)) return false
            if (nuevoEstado in setOf("OBSERVADO", "ANULADO") && motivo.isNullOrBlank()) return false

            val valores = ContentValues().apply { put(PRT_ESTADO, nuevoEstado) }
            if (nuevoEstado == "OBSERVADO") {
                valores.put("motivo_observacion", motivo?.trim())
            }
            if (nuevoEstado == "FORMALIZADO") {
                valores.put("id_usuario_formaliza", idUsuario)
                valores.put("fecha_formalizacion", fechaActual)
            }
            val filasAfectadas = db.update(TABLA_PROTOCOLO, valores, "$PRT_ID = ?", arrayOf(idProtocolo.toString()))
            if (filasAfectadas != 1) return false

            val valoresHist = ContentValues().apply {
                put("id_protocolo", idProtocolo)
                put("estado_anterior", estadoAnt)
                put("estado_nuevo", nuevoEstado)
                put("id_usuario", idUsuario)
                put("fecha_hora", fechaActual)
                put("motivo", motivo)
            }
            if (db.insert(TABLA_HISTORIAL_ESTADO, null, valoresHist) == -1L) return false

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun obtenerUltimaLecturaAnterior(idEquipo: Long, tipoLectura: String): Double? {
        val columna = when (tipoLectura) {
            "LECTURA_1MIN" -> "lectura_1min"
            "LECTURA_10MIN" -> "lectura_10min"
            else -> return null
        }

        val sql = "SELECT $columna FROM $TABLA_PROTOCOLO WHERE $PRT_EQUIPO_ID = ? ORDER BY $PRT_FECHA DESC LIMIT 1"
        var lecturaAnterior: Double? = null

        readableDatabase.rawQuery(sql, arrayOf(idEquipo.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                if (!cursor.isNull(0)) {
                    lecturaAnterior = cursor.getDouble(0)
                }
            }
        }

        return lecturaAnterior
    }

    private fun consultarUsuarios(sql: String, args: Array<String>?): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                val fotoIndex = cursor.getColumnIndex(USR_FOTO)
                val fotoBytes = if (fotoIndex != -1 && !cursor.isNull(fotoIndex)) cursor.getBlob(fotoIndex) else null
                lista.add(Usuario(
                    idUsuario = cursor.getLong(cursor.getColumnIndexOrThrow(USR_ID)),
                    rut = cursor.getString(cursor.getColumnIndexOrThrow(USR_RUT)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(USR_NOMBRE)),
                    correo = cursor.getString(cursor.getColumnIndexOrThrow(USR_CORREO)),
                    telefono = cursor.getString(cursor.getColumnIndexOrThrow(USR_TELEFONO)),
                    rolCodigo = cursor.getString(cursor.getColumnIndexOrThrow("rol_codigo")),
                    idEmpresaCliente = if (cursor.isNull(cursor.getColumnIndexOrThrow(USR_CLIENTE_ID))) null else cursor.getLong(cursor.getColumnIndexOrThrow(USR_CLIENTE_ID)),
                    activo = cursor.getInt(cursor.getColumnIndexOrThrow(USR_ACTIVO)) == 1,
                    avatar = fotoBytes
                ))
            }
        }
        return lista
    }

    private fun consultarProtocolos(sql: String, args: Array<String>?): List<ProtocoloTerreno> {
        val lista = mutableListOf<ProtocoloTerreno>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                val obsIndex = cursor.getColumnIndex(PRT_OBSERVACIONES)
                val obsVal = if (obsIndex != -1 && !cursor.isNull(obsIndex)) cursor.getString(obsIndex) else null
                val fechaVal = cursor.getString(cursor.getColumnIndexOrThrow(PRT_FECHA))
                val resVal = cursor.getString(cursor.getColumnIndexOrThrow(PRT_RESULTADO_GLOBAL))

                lista.add(ProtocoloTerreno(
                    idProtocolo = cursor.getLong(cursor.getColumnIndexOrThrow(PRT_ID)),
                    idEquipo = cursor.getLong(cursor.getColumnIndexOrThrow(PRT_EQUIPO_ID)),
                    idVisita = cursor.getLong(cursor.getColumnIndexOrThrow(PRT_VISITA)),
                    numeroProtocolo = cursor.getString(cursor.getColumnIndexOrThrow(PRT_NUMERO)),
                    tagEquipo = cursor.getString(cursor.getColumnIndexOrThrow("tag_equipo")),
                    tipoEquipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo_equipo")),
                    marcaEquipo = cursor.getString(cursor.getColumnIndexOrThrow("marca_equipo")),
                    modeloEquipo = cursor.getString(cursor.getColumnIndexOrThrow("modelo_equipo")),
                    numeroSerieEquipo = cursor.getString(cursor.getColumnIndexOrThrow("serie_equipo")),
                    anoFabricacion = cursor.getString(cursor.getColumnIndexOrThrow("ano_equipo")),
                    potenciaKva = if (cursor.isNull(cursor.getColumnIndexOrThrow("potencia_equipo"))) "" else
                        String.format(Locale.ROOT, "%.0f kVA", cursor.getDouble(cursor.getColumnIndexOrThrow("potencia_equipo"))),
                    tensiones = cursor.getString(cursor.getColumnIndexOrThrow("tensiones_equipo")),
                    posicionTap = cursor.getString(cursor.getColumnIndexOrThrow("tap_equipo")),
                    grupoConexion = cursor.getString(cursor.getColumnIndexOrThrow("grupo_equipo")),
                    clienteNombre = cursor.getString(cursor.getColumnIndexOrThrow("cliente_nombre")),
                    instalacionNombre = cursor.getString(cursor.getColumnIndexOrThrow("instalacion_nombre")),
                    tecnicoNombre = cursor.getString(cursor.getColumnIndexOrThrow("tecnico_nombre")),
                    fechaHora = fechaVal,
                    fechaRegistro = fechaVal,
                    temperaturaC = cursor.getDouble(cursor.getColumnIndexOrThrow(PRT_TEMP)),
                    humedadPct = cursor.getDouble(cursor.getColumnIndexOrThrow(PRT_HUMEDAD)),
                    estado = cursor.getString(cursor.getColumnIndexOrThrow(PRT_ESTADO)),
                    resultadoGlobal = resVal,
                    resultadoEvaluado = resVal,
                    observaciones = obsVal,
                    tipoVisita = cursor.getString(cursor.getColumnIndexOrThrow("visita_tipo")) ?: "",
                    descripcionMantenimiento = cursor.getString(cursor.getColumnIndexOrThrow("visita_descripcion")),
                    receptorNombre = cursor.getString(cursor.getColumnIndexOrThrow("receptor_nombre")) ?: "",
                    receptorRut = cursor.getString(cursor.getColumnIndexOrThrow("receptor_rut")) ?: "",
                    receptorCargo = cursor.getString(cursor.getColumnIndexOrThrow("receptor_cargo")) ?: "",
                    numeroOt = cursor.getString(cursor.getColumnIndexOrThrow("numero_ot")) ?: "",
                    fechaRecepcion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_recepcion")) ?: "",
                    fechaFormalizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_formalizacion")),
                    usuarioFormalizaNombre = cursor.getString(cursor.getColumnIndexOrThrow("usuario_formaliza_nombre")),
                    motivoObservacion = cursor.getString(cursor.getColumnIndexOrThrow("motivo_observacion")),
                    lectura1Min = if (cursor.isNull(cursor.getColumnIndexOrThrow("lectura_1min"))) 0.0 else cursor.getDouble(cursor.getColumnIndexOrThrow("lectura_1min")),
                    lectura10Min = if (cursor.isNull(cursor.getColumnIndexOrThrow("lectura_10min"))) 0.0 else cursor.getDouble(cursor.getColumnIndexOrThrow("lectura_10min")),
                    mediciones = listarMediciones(cursor.getLong(cursor.getColumnIndexOrThrow(PRT_ID))),
                    historialEstados = listarHistorialEstados(cursor.getLong(cursor.getColumnIndexOrThrow(PRT_ID)))
                ))
            }
        }
        return lista
    }
}

fun String.toSha256(): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
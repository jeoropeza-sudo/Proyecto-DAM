package cl.duocuc.Induztek.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import cl.duocuc.Induztek.MainActivity
import cl.duocuc.Induztek.R
import cl.duocuc.Induztek.permisos.Permisos

object NotificacionesLocales {

    private const val ID_CANAL = "estado_protocolos"

    fun notificarEstadoProtocolo(
        context: Context,
        numeroProtocolo: String,
        estado: String
    ) {
        if (!Permisos.notificacionesHabilitadas(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Permisos.tiene(context, Manifest.permission.POST_NOTIFICATIONS)
        ) return

        val textoEstado = when (estado) {
            "REGISTRADO_TERRENO" -> "registrado y pendiente de formalización"
            "OBSERVADO" -> "observado; requiere revisión"
            "FORMALIZADO" -> "formalizado"
            "ANULADO" -> "anulado"
            else -> estado.lowercase().replace('_', ' ')
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ID_CANAL,
                    "Estado de protocolos",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            numeroProtocolo.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, ID_CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Actualización de protocolo")
            .setContentText("Protocolo $numeroProtocolo: $textoEstado.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("El protocolo $numeroProtocolo fue $textoEstado.")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(numeroProtocolo.hashCode(), notificacion)
    }
}

package com.example.triduofit.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.triduofit.MainActivity
import com.example.triduofit.R
import com.example.triduofit.data.SettingsRepository
import java.util.Calendar

object NotificationScheduler {

    const val CHANNEL_ID = "triduofit_daily_reminder"
    const val NOTIFICATION_ID = 9001
    const val REQUEST_CODE_DAILY = 1001

    // Messages humoristiques, piquants et ultra-motivants façon Duolingo par Coach Mouette
    val DUO_REMINDER_QUOTES = listOf(
        Pair("🦅 Coach Mouette (9h00) !", "Ces burpees ne vont pas se faire tout seuls ! Viens sauver ta série de flammes 🔥"),
        Pair("🍟 Ultimatum de Coach Mouette", "35 min de renfo triathlon ou je viens voler tes frites à midi ! À toi de choisir. 🍟"),
        Pair("🚴‍♂️ Ton vélo elliptique pleure...", "Il m'a dit que tu lui manquais... Viens faire tourner les pédales avant qu'il ne prenne la poussière !"),
        Pair("🌊 Prêt pour l'Ironman ?", "Même les triathlètes d'Hawaï sont déjà debout ! Allez, un peu de gainage pour nager comme un requin 🦈"),
        Pair("🔥 ALERTE SÉRIE EN DANGER !", "Ta flamme de streak vacille ! 5 minutes de renfo valent 100 fois mieux que 5 heures de remords."),
        Pair("🎯 9h : L'heure des guerriers", "Tes abdos et tes ischios ont rendez-vous avec leur destin. Ne me laisse pas attendre sur mon rocher !"),
        Pair("😉 Petit rappel amical...", "Une séance manquée aujourd'hui = 15 burpees de pénalité demain... Je dis ça, je dis rien !"),
        Pair("🦅 Coach Mouette est dans la place", "Tu croyais pouvoir m'échapper ? Pose ton café, enfile tes baskets, c'est l'heure du renfo !"),
        Pair("🏊‍♂️ La combi néoprène n'attend pas", "Pour fendre l'eau comme un dauphin, il te faut le dos en acier. Hop hop hop, sur le tapis !"),
        Pair("⏱️ 9h pétantes ! Pas d'excuses", "Tu as 30 secondes pour rejoindre le tapis de gym. Coach Mouette commence le décompte !")
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rappels Quotidiens Coach Mouette"
            val descriptionText = "Rappels matinaux à 9h00 style Duolingo pour motiver tes séances de renfo triathlon"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context) {
        val settings = SettingsRepository(context)
        if (!settings.isDailyReminderEnabled()) return

        val hour = settings.getReminderHour() // 9h par défaut
        val minute = settings.getReminderMinute() // 00 par défaut

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = "com.example.familiyfit.ACTION_DAILY_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Si l'heure est déjà passée aujourd'hui, programmer pour demain
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback si permission exacte restreinte
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun sendNotification(context: Context, isTest: Boolean = false) {
        createNotificationChannel(context)

        val quote = if (isTest) {
            Pair(
                "🧪 Coach Mouette en direct !",
                "Test réussi ! Tous les matins à 9h00, je serai là pour te botter les fesses avec le sourire ! 🦅🔥💪"
            )
        } else {
            DUO_REMINDER_QUOTES.random()
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(quote.first)
            .setContentText(quote.second)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote.second))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Android 13+ permission pas encore accordée
        }
    }
}

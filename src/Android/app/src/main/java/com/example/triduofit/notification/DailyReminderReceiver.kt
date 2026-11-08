package com.example.triduofit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Réarmement après reboot du téléphone ou mise à jour APK
            NotificationScheduler.scheduleDailyReminder(context)
            return
        }

        // Envoi de la notification Duolingo marrante de Coach Mouette
        NotificationScheduler.sendNotification(context, isTest = false)

        // Réarmement automatique pour le lendemain à 9h00
        NotificationScheduler.scheduleDailyReminder(context)
    }
}

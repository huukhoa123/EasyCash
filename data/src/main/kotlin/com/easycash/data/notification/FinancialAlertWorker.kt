package com.easycash.data.notification

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.os.Build
import androidx.work.*
import dagger.hilt.android.EntryPointAccessors
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class FinancialAlertWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        recoverNotificationListener(applicationContext)
        return try {
            val database = EntryPointAccessors.fromApplication(applicationContext, NotificationProcessorEntryPoint::class.java).database()
            FinancialAlertEngine(database).check(System.currentTimeMillis(), ZoneId.systemDefault(), ::publish)
            Result.success()
        } catch (_: SQLiteException) {
            Result.retry()
        }
    }

    private fun publish(key: String, title: String, body: String): Boolean {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        val channel = "finance-reminders"
        manager.createNotificationChannel(NotificationChannel(channel, "Ngân sách và khoản định kỳ", NotificationManager.IMPORTANCE_DEFAULT))
        if (manager.getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, channel).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body).setContentIntent(pending).setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setOnlyAlertOnce(true).build()
        return try {
            manager.notify(key, 0, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

fun scheduleFinancialAlerts(context: Context) {
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("financial-alerts", ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<FinancialAlertWorker>(15, TimeUnit.MINUTES).build())
}

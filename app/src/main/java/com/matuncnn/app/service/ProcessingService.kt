package com.matuncnn.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.matuncnn.app.R
import com.matuncnn.app.processor.ImageProcessor
import com.matuncnn.app.ui.navigation.MainActivity
import com.matuncnn.app.util.ProgressLogHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProcessingService : Service() {
    private val binder = LocalBinder()
    private val imageProcessor = ImageProcessor()
    private lateinit var notificationManager: NotificationManager
    private var notifySetting = 2

    companion object {
        const val ACTION_STOP_TASK = "com.matuncnn.app.ACTION_STOP_TASK"
        private const val CHANNEL_ID_PROGRESS = "channel_progress"
        private const val CHANNEL_ID_ALIVE = "channel_service_alive"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 500L
        private const val TAG = "ProcessingService"
    }

    inner class LocalBinder : Binder() {
        fun getService(): ProcessingService = this@ProcessingService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_TASK) {
            Log.i(TAG, "Received stop action")
            imageProcessor.cancelCurrentTask()
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        imageProcessor.shutdown()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                getString(R.string.notification_channel_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_progress_description)
            }

            val aliveChannel = NotificationChannel(
                CHANNEL_ID_ALIVE,
                getString(R.string.notification_channel_alive),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_alive_description)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(aliveChannel)
        }
    }

    fun startTask(
        command: String,
        workingDir: String,
        notifySetting: Int = 2,
        callback: ImageProcessor.ProcessCallback
    ) {
        this.notifySetting = notifySetting

        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.processing)))
    }
    private var lastNotificationUpdateTime = 0L

    fun executeTask(
        command: String,
        workingDir: String,
        extraSetup: String = "",
        notifySetting: Int = 2,
        callback: ImageProcessor.ProcessCallback
    ) {
        this.notifySetting = notifySetting
        lastNotificationUpdateTime = 0L
        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.processing)))

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            imageProcessor.executeCommand(
                command = command,
                workingDir = workingDir,
                extraSetup = extraSetup,
                callback = object : ImageProcessor.ProcessCallback {
                    override fun onProgress(line: String) {
                        updateNotification(line)
                        callback.onProgress(line)
                    }

                    override fun onCompleted(result: String, success: Boolean) {
                        forceUpdateNotification(
                            if (success) getString(R.string.done)
                            else getString(R.string.notification_fail)
                        )
                        val removeNotification = notifySetting == 0 || notifySetting == 3
                        stopForeground(removeNotification)
                        callback.onCompleted(result, success)
                    }

                    override fun onError(error: String) {
                        forceUpdateNotification(getString(R.string.notification_fail))
                        val removeNotification = notifySetting == 0 || notifySetting == 3
                        stopForeground(removeNotification)
                        callback.onError(error)
                    }
                }
            )
        }
    }

    fun cancelTask() {
        imageProcessor.cancelCurrentTask()
    }

    private fun updateNotification(text: String) {
        if (notifySetting != 2 && notifySetting != 3) return
        if (!ProgressLogHelper.isProgressLine(text)) return

        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime < NOTIFICATION_UPDATE_INTERVAL_MS) return
        lastNotificationUpdateTime = now

        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun forceUpdateNotification(text: String) {
        if (notifySetting == 0) return
        lastNotificationUpdateTime = System.currentTimeMillis()
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun createNotification(text: String): Notification {
        val channelId = if (notifySetting == 2 || notifySetting == 3) CHANNEL_ID_PROGRESS else CHANNEL_ID_ALIVE

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }
}

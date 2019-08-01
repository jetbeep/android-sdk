package com.jetbeep.jetbeepexample.notification

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.jetbeep.jetbeepexample.MainActivity
import com.jetbeep.jetbeepexample.R

open class BaseNotification(
    private val context: Context, val notificationId: Int,
    private val notificationChannel: NotificationChannels
) {

    companion object {
        const val TAG = "JB_BaseNotification"

        val VIBRATE_PATTERN = longArrayOf(0, 100, 300, 400, 300, 50, 170, 50, 170, 50, 170, 400, 0)
    }

    private val cancelAction = "cancel_notification$notificationId"
    protected val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val cancelIntent = Intent(cancelAction).apply {
        putExtra("notification_id", notificationId)
    }

    protected val cancelPendingIntent =
        PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "receive")
            intent?.let {
                val notificationId = intent.getIntExtra("notification_id", -1)

                if (notificationId > 0) {
                    val notificationManager =
                        context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }
            }
        }
    }

    init {
        setupChannel()

        val filter = IntentFilter(cancelAction)
        context.registerReceiver(cancelReceiver, filter)
    }

    protected fun getBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, notificationChannel.channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && notificationChannel != NotificationChannels.SILENT_EVENT) {
                    val resourceEntryName = context.resources.getResourceEntryName(notificationChannel.sound)
                    val uri = Uri.parse("android.resource://${context.packageName}/raw/$resourceEntryName")
                    setVibrate(VIBRATE_PATTERN)
                    setSound(uri)
                }
            }
    }

    protected fun setupIntent(merchantId: Int): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, merchantId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun setupChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val id = notificationChannel.channelId

            var channel = notificationManager.getNotificationChannel(id)
            channel?.let {
                return
            }

            channel = NotificationChannel(
                id, notificationChannel.channelName,
                notificationChannel.importance
            ).apply {
                description = notificationChannel.channelDescription
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = VIBRATE_PATTERN

                try {
                    val resourceEntryName = context.resources.getResourceEntryName(notificationChannel.sound)
                    val uri = Uri.parse("android.resource://${context.packageName}/raw/$resourceEntryName")
                    setSound(uri, this.audioAttributes)
                } catch (e: Exception) {
                    Log.d(TAG, "BaseNotification sound is -1")
                }
            }

            notificationManager.createNotificationChannel(channel)
        }
    }
}

enum class NotificationChannels(
    val sound: Int, val channelId: String,
    val channelName: String, val channelDescription: String,
    val importance: Int = NotificationManager.IMPORTANCE_HIGH
) {

    REGION_EVENT(
        R.raw.enter_region, "REGION_EVENT_CHANNEL",
        "Entry and exit events",
        "Channel for entry and exit events", NotificationManager.IMPORTANCE_DEFAULT
    ),

    PAYMENT_SUCCESS_EVENT(
        R.raw.coins, "PAYMENT_SUCCESS_EVENT_CHANNEL",
        "Successful payment events",
        "Channel for reporting successful payment events"
    ),

    PAYMENT_FAIL_EVENT(
        R.raw.failed, "PAYMENT_FAIL_EVENT_CHANNEL",
        "Unsuccessful payment events",
        "Channel for reporting unsuccessful payment events"
    ),
    SILENT_EVENT(
        -1, "SILENT_EVENT_CHANNEL",
        "Entry and exit events",
        "Channel for entry and exit events", NotificationManager.IMPORTANCE_LOW
    )
}
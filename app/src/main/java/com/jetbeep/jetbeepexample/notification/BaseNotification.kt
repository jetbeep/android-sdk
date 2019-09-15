package com.jetbeep.jetbeepexample.notification

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

        //val VIBRATE_PATTERN = longArrayOf(0, 100, 300, 400, 300, 50, 170, 50, 170, 50, 170, 400, 0)
    }

    private val cancelAction = "cancel_notification$notificationId"
    protected val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
    }

    protected fun setupIntent(merchantId: Int): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            merchantId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
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
            }

            notificationManager.createNotificationChannel(channel)
        }
    }
}
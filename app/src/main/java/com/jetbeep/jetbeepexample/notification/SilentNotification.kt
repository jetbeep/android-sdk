package com.jetbeep.jetbeepexample.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.jetbeep.locations.PushNotificationManager
import com.jetbeep.model.MerchantType

class SilentNotification(val context: Context, notificationId: Int) :
    BaseNotification(context, notificationId, NotificationChannels.SILENT_EVENT) {

    fun show(info: PushNotificationManager.NotificationInfo) {
        val merchant = info.merchant
        val type = info.merchant.type

        val openActivityIntent = setupIntent(merchant.id)

        val title = if (!info.title.isEmpty())
            info.title
        else {
            when (type) {
                MerchantType.VENDING.name -> "You are at the coffee machine"
                MerchantType.TRANSPORT.name -> "You are in tram"
                else -> "Welcome to ${info.shop.name}"
            }
        }

        val text = if (!info.body.isEmpty())
            info.body
        else {
            when (type) {
                MerchantType.VENDING.name -> "Would you like a cup of coffee?"
                MerchantType.TRANSPORT.name -> "Tap to buy a ticket"
                else -> "Welcome to ${info.shop.name}"
            }
        }

        val notification = getBuilder()
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openActivityIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
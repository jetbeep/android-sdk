package com.jetbeep.jetbeepexample.notification

import android.app.NotificationManager
import android.content.Context
import com.jetbeep.locations.PushNotificationManager
import com.jetbeep.model.MerchantType

class SilentNotificationHolder(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val shownNotifications = mutableSetOf<Int>()

    fun showNotification(info: PushNotificationManager.NotificationInfo) {
        val notificationId = if(info.merchant.type == MerchantType.VENDING.name) {
            info.merchant.id
        } else {
            info.shop.id
        }

        shownNotifications.add(notificationId)

        SilentNotification(context, notificationId).show(info)
    }

    fun hideNotification(notificationId: Int) {
        shownNotifications.remove(notificationId)
        notificationManager.cancel(notificationId)
    }
}
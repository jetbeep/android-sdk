package com.jetbeep.jetbeepexample.notification

import android.app.NotificationManager
import com.jetbeep.jetbeepexample.R

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
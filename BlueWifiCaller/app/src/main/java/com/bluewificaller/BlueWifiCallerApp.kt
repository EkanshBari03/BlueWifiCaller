package com.bluewificaller

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BlueWifiCallerApp : Application() {

    companion object {
        const val CHANNEL_CALL = "channel_call"
        const val CHANNEL_DISCOVERY = "channel_discovery"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        val callChannel = NotificationChannel(
            CHANNEL_CALL,
            "Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming and ongoing calls"
            enableVibration(true)
            setShowBadge(true)
        }

        val discoveryChannel = NotificationChannel(
            CHANNEL_DISCOVERY,
            "Device Discovery",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Peer device discovery status"
        }

        nm.createNotificationChannels(listOf(callChannel, discoveryChannel))
    }
}

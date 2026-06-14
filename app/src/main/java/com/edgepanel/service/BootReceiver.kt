package com.edgepanel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.edgepanel.utils.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Prefs.isServiceEnabled(context)) {
                val serviceIntent = Intent(context, EdgePanelService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}

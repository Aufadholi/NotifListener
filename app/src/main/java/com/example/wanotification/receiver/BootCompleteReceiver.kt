package com.example.wanotification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompleteReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Notification listener is managed by the system; do not launch UI at boot.
            Log.d(TAG, "Boot completed; no action needed")
        }
    }
}


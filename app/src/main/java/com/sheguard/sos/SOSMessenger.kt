package com.sheguard.sos

import android.content.Context
import android.content.Intent
import android.util.Log

class SOSMessenger(private val context: Context) {

    fun openWhatsAppChooser(message: String): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = Intent.createChooser(sendIntent, "Send SOS alert via WhatsApp").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
            true
        } catch (exception: Exception) {
            Log.e("SOSMessenger", "Unable to open WhatsApp", exception)
            false
        }
    }
}

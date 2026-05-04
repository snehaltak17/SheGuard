package com.sheguard.sos

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.sheguard.R
import com.sheguard.camera.CaptureActivity
import com.sheguard.db.AppDatabase
import com.sheguard.db.Incident
import com.sheguard.network.ImageBBUploader
import com.sheguard.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SOSManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val appDatabase: AppDatabase = AppDatabase.getDatabase(context)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val appSettings = AppSettings(context)
    private val imageBBUploader = ImageBBUploader(context)
    private val messenger = SOSMessenger(context)

    @Volatile
    private var sosActive = false

    @SuppressLint("MissingPermission")
    fun triggerSOS(
        shareOnlyLocation: Boolean = false,
        evidenceUri: Uri? = null,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        if (!shareOnlyLocation && evidenceUri == null) {
            startSirenIfEnabled()
            sosActive = true

            if (launchCaptureActivity()) {
                return
            }
        }

        startSirenIfEnabled(enabled = !shareOnlyLocation)
        sosActive = true

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location == null) {
                    onComplete?.invoke(false, context.getString(R.string.location_unavailable))
                    return@addOnSuccessListener
                }
                CoroutineScope(Dispatchers.IO).launch {
                    handleUnifiedPipeline(location, shareOnlyLocation, evidenceUri, onComplete)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SOSManager", "Failed to obtain location", exception)
                onComplete?.invoke(false, context.getString(R.string.location_unavailable))
            }
    }

    fun stopSOS() {
        sosActive = false
        SirenManager.stop()
    }

    fun isSosActive(): Boolean = sosActive

    private suspend fun handleUnifiedPipeline(
        location: Location,
        shareOnlyLocation: Boolean,
        evidenceUri: Uri?,
        onComplete: ((Boolean, String) -> Unit)?
    ) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            onComplete?.invoke(false, "User session unavailable")
            return
        }

        val contacts = appDatabase.contactDao().getContacts(user.uid).first()
        if (contacts.isEmpty()) {
            onComplete?.invoke(false, context.getString(R.string.no_contacts_available))
            return
        }

        val mapLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val evidenceUrl = if (shareOnlyLocation) null else imageBBUploader.uploadImage(evidenceUri)
        val whatsappMessage = buildWhatsAppMessage(mapLink, evidenceUrl)
        val whatsappOpened = messenger.openWhatsAppChooser(whatsappMessage)

        appDatabase.incidentDao().insert(
            Incident(
                timestamp = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude,
                contactsNotified = if (whatsappOpened) contacts.size else 0,
                status = if (whatsappOpened) {
                    context.getString(R.string.status_sent)
                } else {
                    context.getString(R.string.status_failed)
                },
                userId = user.uid
            )
        )

        pushSosEvent(
            userId = user.uid,
            userName = user.displayName ?: user.email ?: "SheGuard user",
            latitude = location.latitude,
            longitude = location.longitude,
            status = if (whatsappOpened) context.getString(R.string.status_sent) else context.getString(R.string.status_failed),
            contactsNotified = if (whatsappOpened) contacts.size else 0,
            evidenceUrl = evidenceUrl
        )

        onComplete?.invoke(
            whatsappOpened,
            if (whatsappOpened) context.getString(R.string.preparing_whatsapp) else mapLink
        )
    }

    private fun buildWhatsAppMessage(mapLink: String, imageUrl: String?): String {
        return buildString {
            append("🚨 SOS ALERT\n\n")
            append("I may be in danger.\n\n")
            append("Location:\n")
            append(mapLink)
            if (!imageUrl.isNullOrBlank()) {
                append("\n\nEvidence Photo:\n")
                append(imageUrl)
            }
        }
    }

    private fun startSirenIfEnabled(enabled: Boolean = true) {
        if (enabled && appSettings.sirenEnabled) {
            runCatching { SirenManager.start(context) }
                .onFailure { exception -> Log.e("SOSManager", "Unable to start siren", exception) }
        }
    }

    private fun launchCaptureActivity(): Boolean {
        return runCatching {
            val intent = Intent(context, CaptureActivity::class.java).apply {
                putExtra(CaptureActivity.EXTRA_TRIGGER_SOS_ON_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrElse { exception ->
            Log.e("SOSManager", "Unable to launch capture activity", exception)
            false
        }
    }

    private fun pushSosEvent(
        userId: String,
        userName: String,
        latitude: Double,
        longitude: Double,
        status: String,
        contactsNotified: Int,
        evidenceUrl: String?
    ) {
        val eventData = mapOf(
            "userId" to userId,
            "userName" to userName,
            "latitude" to latitude,
            "longitude" to longitude,
            "status" to status,
            "contactsNotified" to contactsNotified,
            "evidenceUrl" to evidenceUrl.orEmpty(),
            "zoneId" to appSettings.currentZoneId,
            "zoneName" to appSettings.currentZoneName,
            "zoneState" to appSettings.currentZoneState,
            "timestamp" to ServerValue.TIMESTAMP
        )

        firebaseDatabase.getReference("sosEvents")
            .push()
            .setValue(eventData)
            .addOnFailureListener { exception ->
                Log.e("SOSManager", "Failed to store SOS event for admin dashboard", exception)
            }
    }
}

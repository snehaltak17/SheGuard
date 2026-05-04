package com.sheguard.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.sheguard.R
import com.sheguard.dashboard.DashboardActivity
import com.sheguard.notifications.SafetyNotificationManager
import com.sheguard.settings.AppSettings
import com.sheguard.sensors.ShakeDetector
import com.sheguard.sos.SOSManager
import com.sheguard.zone.SafetyZone
import com.sheguard.zone.ZoneMonitor

class BackgroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "SheGuardChannel"
        private const val TAG = "BackgroundService"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var sosManager: SOSManager
    private lateinit var appSettings: AppSettings
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var safetyNotificationManager: SafetyNotificationManager
    private var zones: List<SafetyZone> = emptyList()
    private var zoneListener: ValueEventListener? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let(::handleLocationUpdate)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        appSettings = AppSettings(this)
        sosManager = SOSManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        safetyNotificationManager = SafetyNotificationManager(this)
        shakeDetector = ShakeDetector {
            if (appSettings.monitoringEnabled && appSettings.shakeTriggerEnabled) {
                vibrateBriefly()
                sosManager.triggerSOS()
            }
        }
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        observeSafetyZones()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notificationIntent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_enabled))
            .setContentText(getString(R.string.monitoring_enabled_subtitle))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        startLocationTracking()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun vibrateBriefly() {
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(300)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SheGuard Monitoring",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun observeSafetyZones() {
        val databaseReference = firebaseDatabase.getReference("zones")
        zoneListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                zones = snapshot.children.mapNotNull { child ->
                    child.getValue(SafetyZone::class.java)?.copy(id = child.key ?: "")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Safety zone sync failed: ${error.message}")
            }
        }
        zoneListener?.let(databaseReference::addValueEventListener)
    }

    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Skipping location tracking because permission is missing")
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000L)
            .setMinUpdateIntervalMillis(10_000L)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun handleLocationUpdate(location: Location) {
        appSettings.lastTrackedLatitude = location.latitude.toString()
        appSettings.lastTrackedLongitude = location.longitude.toString()
        appSettings.lastTrackedAt = System.currentTimeMillis()

        val previousState = appSettings.currentZoneState
        val previousZoneId = appSettings.currentZoneId
        val previousZoneName = appSettings.currentZoneName
        val activeZone = ZoneMonitor.resolveZone(location, zones)
        val newState = ZoneMonitor.resolveState(activeZone)
        notifyZoneTransition(
            previousState = previousState,
            previousZoneId = previousZoneId,
            activeZone = activeZone,
            newState = newState
        )
        logZoneHistory(
            previousState = previousState,
            previousZoneId = previousZoneId,
            previousZoneName = previousZoneName,
            activeZone = activeZone,
            newState = newState,
            location = location
        )

        appSettings.currentZoneState = newState
        appSettings.currentZoneId = activeZone?.id.orEmpty()
        appSettings.currentZoneName = activeZone?.name.orEmpty()

        uploadTracking(location, activeZone, newState)
    }

    private fun notifyZoneTransition(
        previousState: String,
        previousZoneId: String,
        activeZone: SafetyZone?,
        newState: String
    ) {
        val zoneChanged = previousZoneId != activeZone?.id
        when {
            newState == ZoneMonitor.STATE_DANGER && (previousState != newState || zoneChanged) -> {
                safetyNotificationManager.showDangerZone(activeZone?.name)
            }
            newState == ZoneMonitor.STATE_SAFE && (previousState != newState || zoneChanged) -> {
                if (previousState == ZoneMonitor.STATE_DANGER) {
                    safetyNotificationManager.showMovedOutOfDanger()
                }
                safetyNotificationManager.showSafeZone(activeZone?.name)
            }
            previousState == ZoneMonitor.STATE_DANGER && newState != ZoneMonitor.STATE_DANGER -> {
                safetyNotificationManager.showMovedOutOfDanger()
            }
        }
    }

    private fun uploadTracking(location: Location, activeZone: SafetyZone?, zoneState: String) {
        val currentUser = firebaseAuth.currentUser ?: return
        val trackingData = mapOf(
            "uid" to currentUser.uid,
            "name" to (currentUser.displayName ?: currentUser.email ?: "SheGuard user"),
            "email" to (currentUser.email ?: ""),
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "zoneId" to activeZone?.id.orEmpty(),
            "zoneName" to activeZone?.name.orEmpty(),
            "zoneType" to activeZone?.type.orEmpty(),
            "zoneState" to zoneState,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        firebaseDatabase.getReference("tracking")
            .child(currentUser.uid)
            .setValue(trackingData)
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to push tracking update", exception)
            }
    }

    private fun logZoneHistory(
        previousState: String,
        previousZoneId: String,
        previousZoneName: String,
        activeZone: SafetyZone?,
        newState: String,
        location: Location
    ) {
        val zoneChanged = previousZoneId != activeZone?.id
        if (!zoneChanged && previousState == newState) {
            return
        }

        if (previousZoneId.isBlank() && activeZone == null && previousState == newState) {
            return
        }

        val currentUser = firebaseAuth.currentUser ?: return
        val eventType = when {
            previousZoneId.isBlank() && activeZone != null -> "entered_zone"
            previousZoneId.isNotBlank() && activeZone == null -> "left_zone"
            previousZoneId.isNotBlank() && activeZone != null && zoneChanged -> "changed_zone"
            previousState != newState -> "state_changed"
            else -> "zone_update"
        }

        val historyData = mapOf(
            "userId" to currentUser.uid,
            "userName" to (currentUser.displayName ?: currentUser.email ?: "SheGuard user"),
            "userEmail" to (currentUser.email ?: ""),
            "eventType" to eventType,
            "summary" to buildZoneHistorySummary(previousZoneName, previousState, activeZone, newState),
            "fromZoneId" to previousZoneId,
            "fromZoneName" to previousZoneName,
            "fromState" to previousState,
            "toZoneId" to activeZone?.id.orEmpty(),
            "toZoneName" to activeZone?.name.orEmpty(),
            "toState" to newState,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to ServerValue.TIMESTAMP
        )

        firebaseDatabase.getReference("zoneHistory")
            .child(currentUser.uid)
            .push()
            .setValue(historyData)
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to store zone history", exception)
            }
    }

    private fun buildZoneHistorySummary(
        previousZoneName: String,
        previousState: String,
        activeZone: SafetyZone?,
        newState: String
    ): String {
        val previousLabel = readableZoneLabel(previousZoneName, previousState)
        val nextLabel = readableZoneLabel(activeZone?.name.orEmpty(), newState)
        return when {
            previousZoneName.isBlank() && activeZone != null -> "Entered $nextLabel"
            previousZoneName.isNotBlank() && activeZone == null -> "Left $previousLabel"
            previousZoneName.isNotBlank() && activeZone != null && previousZoneName != activeZone.name -> {
                "Moved from $previousLabel to $nextLabel"
            }
            else -> "Zone status updated to $nextLabel"
        }
    }

    private fun readableZoneLabel(zoneName: String, zoneState: String): String {
        if (zoneName.isNotBlank()) {
            return zoneName
        }

        return when (zoneState) {
            ZoneMonitor.STATE_DANGER -> "danger zone"
            ZoneMonitor.STATE_SAFE -> "safe zone"
            else -> "outside mapped zones"
        }
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(shakeDetector)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        zoneListener?.let { listener ->
            firebaseDatabase.getReference("zones").removeEventListener(listener)
        }
        super.onDestroy()
    }
}

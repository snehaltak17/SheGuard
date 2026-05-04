package com.sheguard.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.sheguard.R
import com.sheguard.auth.LoginActivity
import com.sheguard.camera.CaptureActivity
import com.sheguard.contacts.ContactListActivity
import com.sheguard.databinding.ActivityDashboardBinding
import com.sheguard.db.AppDatabase
import com.sheguard.db.Incident
import com.sheguard.helpline.HelplineActivity
import com.sheguard.history.HistoryActivity
import com.sheguard.receivers.VolumePatternReceiver
import com.sheguard.services.BackgroundService
import com.sheguard.settings.AppSettings
import com.sheguard.settings.SettingsActivity
import com.sheguard.sos.SOSManager
import com.sheguard.zone.ZoneMonitor
import java.text.DateFormat
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DashboardActivity"
    }

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var appDatabase: AppDatabase
    private lateinit var incidentAdapter: IncidentAdapter
    private lateinit var sosManager: SOSManager
    private lateinit var appSettings: AppSettings
    private lateinit var volumePatternReceiver: VolumePatternReceiver
    private val incidents = mutableListOf<Incident>()

    private val locationPermissionRequestCode = 1001
    private val cameraPermissionRequestCode = 1002
    private val notificationPermissionRequestCode = 1003
    private var pendingShareOnlyLocation = false

    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(CaptureActivity.EXTRA_IMAGE_URI)
            val imageUri = uriString?.let(Uri::parse)
            executeSosPipeline(pendingShareOnlyLocation, imageUri)
        } else {
            executeSosPipeline(true, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        firebaseAuth = FirebaseAuth.getInstance()
        appDatabase = AppDatabase.getDatabase(this)
        sosManager = SOSManager(this)
        appSettings = AppSettings(this)
        volumePatternReceiver = VolumePatternReceiver {
            if (appSettings.volumePatternEnabled) {
                triggerEmergency(false)
            }
        }

        incidentAdapter = IncidentAdapter(incidents)
        binding.incidentHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.incidentHistoryRecyclerView.adapter = incidentAdapter

        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_contacts -> {
                    startActivity(Intent(this, ContactListActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> true
            }
        }

        bindDashboardActions()
        updateGreeting()
        updateSosUiState()
        updateZoneDashboard()
        loadIncidentHistory()
        ensureNotificationPermissionIfNeeded()

        if (appSettings.monitoringEnabled) {
            startMonitoringServiceIfPossible()
        }
    }

    override fun onResume() {
        super.onResume()
        updateZoneDashboard()
    }

    private fun bindDashboardActions() {
        binding.sosButton.setOnClickListener { triggerEmergency(false) }
        binding.shareLocationButton.setOnClickListener { triggerEmergency(true) }
        binding.stopSosButton.setOnClickListener { stopSosFlow() }
        binding.contactsButton.setOnClickListener { startActivity(Intent(this, ContactListActivity::class.java)) }
        binding.helplineButton.setOnClickListener { startActivity(Intent(this, HelplineActivity::class.java)) }
        binding.openTrackedLocationButton.setOnClickListener { openTrackedLocation() }

        binding.sirenButton.setOnClickListener {
            if (sosManager.isSosActive()) {
                stopSosFlow()
            } else {
                triggerEmergency(false)
            }
        }

        binding.policeButton.setOnClickListener { dialNumber("100") }
        binding.womenHelplineButton.setOnClickListener { dialNumber("1091") }
        binding.ambulanceButton.setOnClickListener { dialNumber("108") }
        binding.fireButton.setOnClickListener { dialNumber("101") }
    }

    private fun triggerEmergency(shareOnlyLocation: Boolean) {
        pendingShareOnlyLocation = shareOnlyLocation

        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }

        if (!shareOnlyLocation && !checkCameraPermission()) {
            requestCameraPermission()
            return
        }

        if (shareOnlyLocation) {
            executeSosPipeline(true, null)
        } else {
            captureLauncher.launch(Intent(this, CaptureActivity::class.java))
        }
    }

    private fun executeSosPipeline(shareOnlyLocation: Boolean, evidenceUri: Uri?) {
        sosManager.triggerSOS(shareOnlyLocation, evidenceUri) { success, message ->
            runOnUiThread {
                updateSosUiState()
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                if (!success) {
                    Toast.makeText(this, getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopSosFlow() {
        sosManager.stopSOS()
        appSettings.monitoringEnabled = false
        stopService(Intent(this, BackgroundService::class.java))
        updateSosUiState()
        Toast.makeText(this, getString(R.string.siren_stopped), Toast.LENGTH_SHORT).show()
    }

    private fun updateSosUiState() {
        val active = sosManager.isSosActive()
        binding.monitoringStatusChip.text = when {
            active -> getString(R.string.monitoring_status_sos)
            appSettings.monitoringEnabled -> getString(R.string.monitoring_status_active)
            else -> getString(R.string.monitoring_status_paused)
        }
        binding.stopSosButton.visibility = if (active) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun updateGreeting() {
        val currentUser = firebaseAuth.currentUser ?: run {
            binding.greetingTextView.text = getString(R.string.dashboard_title)
            return
        }

        FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").getValue(String::class.java)
                binding.greetingTextView.text = if (name.isNullOrBlank()) {
                    getString(R.string.dashboard_title)
                } else {
                    getString(R.string.welcome_back_user, name)
                }
            }
            .addOnFailureListener {
                binding.greetingTextView.text = getString(R.string.dashboard_title)
            }
    }

    private fun updateZoneDashboard() {
        val zoneState = appSettings.currentZoneState
        val zoneName = appSettings.currentZoneName
        val latitude = appSettings.lastTrackedLatitude
        val longitude = appSettings.lastTrackedLongitude
        val lastTrackedAt = appSettings.lastTrackedAt

        binding.zoneStateTextView.text = when (zoneState) {
            ZoneMonitor.STATE_DANGER -> getString(R.string.zone_state_danger)
            ZoneMonitor.STATE_SAFE -> getString(R.string.zone_state_safe)
            else -> getString(R.string.zone_state_unmapped)
        }

        binding.zoneNameTextView.text = if (zoneName.isBlank()) {
            getString(R.string.zone_name_placeholder)
        } else {
            zoneName
        }

        binding.zoneLastLocationTextView.text = if (latitude.isBlank() || longitude.isBlank()) {
            getString(R.string.zone_tracking_waiting)
        } else {
            getString(R.string.zone_last_location_value, latitude, longitude)
        }

        binding.zoneLastUpdatedTextView.text = if (lastTrackedAt == 0L) {
            getString(R.string.zone_last_updated_waiting)
        } else {
            getString(
                R.string.zone_last_updated_value,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(lastTrackedAt)
            )
        }
    }

    private fun openTrackedLocation() {
        val latitude = appSettings.lastTrackedLatitude
        val longitude = appSettings.lastTrackedLongitude
        if (latitude.isBlank() || longitude.isBlank()) {
            Toast.makeText(this, getString(R.string.zone_tracking_waiting), Toast.LENGTH_SHORT).show()
            return
        }

        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$latitude,$longitude")))
    }

    private fun loadIncidentHistory() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        lifecycleScope.launch {
            appDatabase.incidentDao().getIncidents(userId).collect { incidentList ->
                incidents.clear()
                incidents.addAll(incidentList)
                binding.emptyHistoryTextView.visibility = if (incidentList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.contactsNotifiedTextView.text = getString(
                    R.string.contacts_notified,
                    incidentList.firstOrNull()?.contactsNotified ?: 0
                )
                incidentAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun dialNumber(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode)
    }

    private fun checkCameraPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
    }

    private fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            notificationPermissionRequestCode
        )
    }

    private fun startMonitoringServiceIfPossible() {
        if (!checkLocationPermission()) {
            Log.w(TAG, "Skipping BackgroundService startup because location permission is not granted")
            return
        }

        val serviceIntent = Intent(this, BackgroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to start BackgroundService", exception)
            Toast.makeText(this, getString(R.string.monitoring_start_failed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startMonitoringServiceIfPossible()
                    triggerEmergency(pendingShareOnlyLocation)
                } else {
                    Toast.makeText(this, getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
                }
            }
            cameraPermissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    triggerEmergency(pendingShareOnlyLocation)
                } else {
                    Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
                }
            }
            notificationPermissionRequestCode -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.notification_permission_required), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            volumePatternReceiver.registerKey(event.keyCode)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                firebaseAuth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

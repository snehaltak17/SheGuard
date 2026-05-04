package com.sheguard.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sheguard.databinding.ActivitySettingsBinding
import com.sheguard.services.BackgroundService

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appSettings = AppSettings(this)

        bindState()
        bindListeners()
    }

    private fun bindState() {
        binding.monitoringSwitch.isChecked = appSettings.monitoringEnabled
        binding.sirenSwitch.isChecked = appSettings.sirenEnabled
        binding.shakeSwitch.isChecked = appSettings.shakeTriggerEnabled
        binding.powerSwitch.isChecked = appSettings.powerTriggerEnabled
    }

    private fun bindListeners() {
        binding.monitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
            appSettings.monitoringEnabled = isChecked
            if (isChecked) {
                startMonitoringService()
            } else {
                stopService(Intent(this, BackgroundService::class.java))
            }
            showSavedToast()
        }

        binding.sirenSwitch.setOnCheckedChangeListener { _, isChecked ->
            appSettings.sirenEnabled = isChecked
            showSavedToast()
        }

        binding.shakeSwitch.setOnCheckedChangeListener { _, isChecked ->
            appSettings.shakeTriggerEnabled = isChecked
            showSavedToast()
        }

        binding.powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            appSettings.powerTriggerEnabled = isChecked
            showSavedToast()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun showSavedToast() {
        Toast.makeText(this, getString(com.sheguard.R.string.settings_saved), Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

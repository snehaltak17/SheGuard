package com.sheguard.settings

import android.content.Context

class AppSettings(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var monitoringEnabled: Boolean
        get() = preferences.getBoolean(KEY_MONITORING_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_MONITORING_ENABLED, value).apply()

    var sirenEnabled: Boolean
        get() = preferences.getBoolean(KEY_SIREN_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_SIREN_ENABLED, value).apply()

    var shakeTriggerEnabled: Boolean
        get() = preferences.getBoolean(KEY_SHAKE_TRIGGER_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_SHAKE_TRIGGER_ENABLED, value).apply()

    var powerTriggerEnabled: Boolean
        get() = preferences.getBoolean(KEY_POWER_TRIGGER_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_POWER_TRIGGER_ENABLED, value).apply()

    var volumePatternEnabled: Boolean
        get() = preferences.getBoolean(KEY_VOLUME_PATTERN_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_VOLUME_PATTERN_ENABLED, value).apply()

    var currentZoneState: String
        get() = preferences.getString(KEY_CURRENT_ZONE_STATE, "unmapped") ?: "unmapped"
        set(value) = preferences.edit().putString(KEY_CURRENT_ZONE_STATE, value).apply()

    var currentZoneName: String
        get() = preferences.getString(KEY_CURRENT_ZONE_NAME, "") ?: ""
        set(value) = preferences.edit().putString(KEY_CURRENT_ZONE_NAME, value).apply()

    var currentZoneId: String
        get() = preferences.getString(KEY_CURRENT_ZONE_ID, "") ?: ""
        set(value) = preferences.edit().putString(KEY_CURRENT_ZONE_ID, value).apply()

    var lastTrackedLatitude: String
        get() = preferences.getString(KEY_LAST_TRACKED_LATITUDE, "") ?: ""
        set(value) = preferences.edit().putString(KEY_LAST_TRACKED_LATITUDE, value).apply()

    var lastTrackedLongitude: String
        get() = preferences.getString(KEY_LAST_TRACKED_LONGITUDE, "") ?: ""
        set(value) = preferences.edit().putString(KEY_LAST_TRACKED_LONGITUDE, value).apply()

    var lastTrackedAt: Long
        get() = preferences.getLong(KEY_LAST_TRACKED_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_TRACKED_AT, value).apply()

    companion object {
        private const val PREFS_NAME = "sheguard_settings"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_SIREN_ENABLED = "siren_enabled"
        private const val KEY_SHAKE_TRIGGER_ENABLED = "shake_trigger_enabled"
        private const val KEY_POWER_TRIGGER_ENABLED = "power_trigger_enabled"
        private const val KEY_VOLUME_PATTERN_ENABLED = "volume_pattern_enabled"
        private const val KEY_CURRENT_ZONE_STATE = "current_zone_state"
        private const val KEY_CURRENT_ZONE_NAME = "current_zone_name"
        private const val KEY_CURRENT_ZONE_ID = "current_zone_id"
        private const val KEY_LAST_TRACKED_LATITUDE = "last_tracked_latitude"
        private const val KEY_LAST_TRACKED_LONGITUDE = "last_tracked_longitude"
        private const val KEY_LAST_TRACKED_AT = "last_tracked_at"
    }
}

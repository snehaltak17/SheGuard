package com.sheguard.zone

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SafetyZone(
    val id: String = "",
    val name: String = "",
    val type: String = TYPE_DANGER,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Double = 0.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val createdBy: String = ""
) {
    companion object {
        const val TYPE_DANGER = "danger"
        const val TYPE_SAFE = "safe"
    }
}

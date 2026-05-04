package com.sheguard.zone

import android.location.Location

object ZoneMonitor {

    const val STATE_DANGER = "danger"
    const val STATE_SAFE = "safe"
    const val STATE_UNMAPPED = "unmapped"

    fun resolveZone(location: Location, zones: List<SafetyZone>): SafetyZone? {
        val matchingZones = zones.filter { zone ->
            if (zone.radiusMeters <= 0.0) {
                return@filter false
            }

            val distanceResult = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                zone.latitude,
                zone.longitude,
                distanceResult
            )
            distanceResult[0] <= zone.radiusMeters
        }

        return matchingZones
            .sortedWith(compareBy<SafetyZone> { if (it.type == SafetyZone.TYPE_DANGER) 0 else 1 }
                .thenBy { distanceTo(location, it) })
            .firstOrNull()
    }

    fun resolveState(zone: SafetyZone?): String {
        return when (zone?.type) {
            SafetyZone.TYPE_DANGER -> STATE_DANGER
            SafetyZone.TYPE_SAFE -> STATE_SAFE
            else -> STATE_UNMAPPED
        }
    }

    private fun distanceTo(location: Location, zone: SafetyZone): Float {
        val distanceResult = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            zone.latitude,
            zone.longitude,
            distanceResult
        )
        return distanceResult[0]
    }
}

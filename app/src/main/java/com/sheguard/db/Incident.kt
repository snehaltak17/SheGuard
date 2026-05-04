package com.sheguard.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey(autoGenerate = true)
    val incidentId: Int = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val contactsNotified: Int,
    val status: String,
    val userId: String
)

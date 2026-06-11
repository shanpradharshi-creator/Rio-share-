package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_history")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val fileType: String, // "PHOTO", "VIDEO", "DOCUMENT", "APK", "OTHER"
    val size: Long,
    val status: String, // "COMPLETED", "FAILED", "PAUSED", "CANCELLED"
    val speed: Long, // Average speed in bytes per second
    val peerName: String,
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

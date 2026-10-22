package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: Long, // Millisecond timestamp matching prototype
    val uriString: String,
    val fileName: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val width: Int = 0,
    val height: Int = 0
)

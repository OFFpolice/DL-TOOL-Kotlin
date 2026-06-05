package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val filename: String,
    val filePath: String,
    val fileSize: Long = 0,
    val status: String, // COMPLETED, DOWNLOADING, FAILED
    val timestamp: Long = System.currentTimeMillis()
)

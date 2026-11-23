package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val system: String,
    val year: Int,
    val genre: String,
    val colorHex: String,
    val romAsset: String = "",
    val localRomPath: String = "",
    val coverPath: String = "",
    val isFavorite: Boolean = false,
    val playTimeSeconds: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "save_states")
data class SaveStateEntity(
    @PrimaryKey val id: String, // "{gameId}_slot_{slotIndex}"
    val gameId: String,
    val slotIndex: Int, // 0 = Auto, 1..10 = Manual slots
    val filePath: String,
    val screenshotPath: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cheats")
data class CheatEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val name: String,
    val code: String, // e.g. "00A4:09" (RAM addr:hex value)
    val enabled: Boolean = true
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

@Entity(tableName = "downloads")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val localTempPath: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val status: String = DownloadStatus.QUEUED.name,
    val detectedSystem: String = AtariSystem.ATARI_2600.name,
    val importedGameId: String = "",
    val errorMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

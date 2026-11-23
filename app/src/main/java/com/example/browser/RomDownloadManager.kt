package com.example.browser

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AtariSystem
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTaskEntity
import com.example.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class BrowserBookmark(
    val title: String,
    val url: String,
    val badge: String
)

object RomBookmarks {
    val presets = listOf(
        BrowserBookmark("Internet Archive 2600", "https://archive.org/details/atari_2600_library", "2600"),
        BrowserBookmark("AtariAge Homebrew", "https://atariage.com/2600/index.php", "HOMEBREW"),
        BrowserBookmark("PDROMS Atari", "https://pdroms.de/files/atari-2600/", "FREE"),
        BrowserBookmark("Archive Atari 7800", "https://archive.org/details/atari_7800_library", "7800"),
        BrowserBookmark("Archive Atari Lynx", "https://archive.org/details/atari_lynx_library", "LYNX")
    )

    val romUrlExtensions = setOf("zip", "a26", "bin", "a78", "a52", "lnx", "lyx", "j64", "jag", "atr", "xex")

    fun isRomLink(url: String, contentDisposition: String? = null): Boolean {
        val candidate = (contentDisposition ?: url).lowercase()
        val cleanPath = candidate.substringBefore('?').substringBefore('#')
        val ext = cleanPath.substringAfterLast('.', "")
        return ext in romUrlExtensions
    }

    fun extractFileName(url: String, contentDisposition: String? = null): String {
        if (!contentDisposition.isNullOrBlank()) {
            val match = Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE).find(contentDisposition)
            if (match != null) return match.groupValues[1].trim()
        }
        val clean = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        return clean.ifBlank { "atari_rom_${System.currentTimeMillis()}.zip" }
    }
}

class RomDownloadManager(
    private val context: Context,
    private val repository: GameRepository,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val downloadDir: File by lazy { File(context.cacheDir, "rom_downloads").apply { mkdirs() } }
    private val channelId = "atari_rom_downloads"

    init {
        ensureNotificationChannel()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "ROM Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }

    fun enqueueDownload(url: String, contentDisposition: String? = null): String {
        val fileName = RomBookmarks.extractFileName(url, contentDisposition)
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val id = UUID.randomUUID().toString().take(8)
        val tempFile = File(downloadDir, "${id}_$fileName")
        val task = DownloadTaskEntity(
            id = id,
            url = url,
            fileName = fileName,
            localTempPath = tempFile.absolutePath,
            status = DownloadStatus.QUEUED.name,
            detectedSystem = AtariSystem.fromExtension(ext).name
        )
        scope.launch(Dispatchers.IO) {
            repository.downloadDao.upsert(task)
            startDownloadJob(task)
        }
        return id
    }

    fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        scope.launch(Dispatchers.IO) {
            val task = repository.downloadDao.getById(taskId) ?: return@launch
            repository.downloadDao.upsert(task.copy(status = DownloadStatus.PAUSED.name))
        }
    }

    fun resumeDownload(taskId: String) {
        scope.launch(Dispatchers.IO) {
            val task = repository.downloadDao.getById(taskId) ?: return@launch
            if (task.status != DownloadStatus.COMPLETED.name) {
                startDownloadJob(task)
            }
        }
    }

    fun deleteDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        scope.launch(Dispatchers.IO) {
            val task = repository.downloadDao.getById(taskId)
            if (task != null) {
                File(task.localTempPath).delete()
                repository.downloadDao.deleteById(taskId)
            }
        }
    }

    private fun startDownloadJob(initialTask: DownloadTaskEntity) {
        activeJobs[initialTask.id]?.cancel()
        val job = scope.launch(Dispatchers.IO) {
            val tempFile = File(initialTask.localTempPath)
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
            var currentTask = initialTask.copy(
                bytesDownloaded = existingBytes,
                status = DownloadStatus.DOWNLOADING.name,
                errorMessage = ""
            )
            repository.downloadDao.upsert(currentTask)

            try {
                val reqBuilder = Request.Builder().url(initialTask.url)
                if (existingBytes > 0L) {
                    reqBuilder.addHeader("Range", "bytes=$existingBytes-")
                }
                val response = client.newCall(reqBuilder.build()).execute()
                if (!response.isSuccessful && response.code != 206) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Empty response")
                val isPartial = response.code == 206
                val contentLength = body.contentLength()
                val totalBytes = when {
                    contentLength <= 0L -> -1L
                    isPartial -> existingBytes + contentLength
                    else -> contentLength
                }

                if (!isPartial && tempFile.exists()) {
                    tempFile.delete()
                }

                RandomAccessFile(tempFile, "rw").use { raf ->
                    if (isPartial) raf.seek(existingBytes)
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = if (isPartial) existingBytes else 0L
                    var lastNotify = 0L

                    body.byteStream().use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            raf.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastNotify > 350L) {
                                lastNotify = now
                                currentTask = currentTask.copy(
                                    bytesDownloaded = downloaded,
                                    totalBytes = totalBytes,
                                    status = DownloadStatus.DOWNLOADING.name
                                )
                                repository.downloadDao.upsert(currentTask)
                                postNotification(currentTask.id.hashCode(), currentTask.fileName, downloaded, totalBytes, false)
                            }
                        }
                    }

                    // Completed: auto unzip & import into library
                    val imported = repository.importRomBytes(currentTask.fileName, tempFile.readBytes())
                    tempFile.delete()
                    val finalTask = currentTask.copy(
                        bytesDownloaded = downloaded,
                        totalBytes = if (totalBytes > 0) totalBytes else downloaded,
                        status = if (imported != null) DownloadStatus.COMPLETED.name else DownloadStatus.FAILED.name,
                        detectedSystem = imported?.system ?: currentTask.detectedSystem,
                        importedGameId = imported?.id ?: "",
                        errorMessage = if (imported == null) "Unsupported ROM archive" else ""
                    )
                    repository.downloadDao.upsert(finalTask)
                    postNotification(currentTask.id.hashCode(), currentTask.fileName, downloaded, downloaded, true)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                repository.downloadDao.upsert(
                    currentTask.copy(
                        status = DownloadStatus.FAILED.name,
                        errorMessage = e.message ?: "Download error"
                    )
                )
            } finally {
                activeJobs.remove(initialTask.id)
            }
        }
        activeJobs[initialTask.id] = job
    }

    private fun postNotification(
        notifId: Int,
        fileName: String,
        downloaded: Long,
        total: Long,
        finished: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(fileName)
            .setOngoing(!finished)
            .setAutoCancel(finished)
        if (finished) {
            builder.setContentText("تم استيراد اللعبة إلى المكتبة")
                .setProgress(0, 0, false)
        } else if (total > 0L) {
            val pct = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
            builder.setContentText("$pct%")
                .setProgress(100, pct, false)
        } else {
            builder.setContentText("${downloaded / 1024} KB")
                .setProgress(0, 0, true)
        }
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }
}

package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.local.AtariDatabase
import com.example.data.model.AtariSystem
import com.example.data.model.CheatEntity
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class GameRepository(private val context: Context) {
    private val db = AtariDatabase.getInstance(context)
    val gameDao = db.gameDao()
    val saveStateDao = db.saveStateDao()
    val cheatDao = db.cheatDao()
    val downloadDao = db.downloadDao()

    private val romsDir: File by lazy { File(context.filesDir, "roms").apply { mkdirs() } }
    private val savesDir: File by lazy { File(context.filesDir, "saves").apply { mkdirs() } }
    private val shotsDir: File by lazy { File(context.filesDir, "screenshots").apply { mkdirs() } }

    fun ensureCatalogLoaded(): Flow<Float> = flow {
        val count = withContext(Dispatchers.IO) { gameDao.getCount() }
        if (count >= 2600) {
            emit(1f)
            return@flow
        }
        emit(0.08f)
        val jsonText = withContext(Dispatchers.IO) {
            context.assets.open("games.json").bufferedReader().use { it.readText() }
        }
        emit(0.28f)
        val array = JSONArray(jsonText)
        val total = array.length()
        val batch = ArrayList<GameEntity>(500)
        for (i in 0 until total) {
            val obj = array.getJSONObject(i)
            batch.add(
                GameEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    system = obj.getString("system"),
                    year = obj.optInt("year", 1982),
                    genre = obj.optString("genre", "أركيد"),
                    colorHex = obj.optString("colorHex", "#FF5A1F"),
                    romAsset = obj.optString("romAsset", ""),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    playTimeSeconds = obj.optLong("playTimeSeconds", 0L),
                    lastPlayedAt = obj.optLong("lastPlayedAt", 0L),
                    addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                )
            )
            if (batch.size >= 450 || i == total - 1) {
                withContext(Dispatchers.IO) { gameDao.insertAll(batch) }
                batch.clear()
                emit(0.28f + 0.70f * ((i + 1).toFloat() / total.toFloat()))
            }
        }
        emit(1f)
    }

    suspend fun getRomBytes(game: GameEntity): ByteArray? = withContext(Dispatchers.IO) {
        if (game.localRomPath.isNotBlank()) {
            val f = File(game.localRomPath)
            if (f.exists()) return@withContext f.readBytes()
        }
        if (game.romAsset.isNotBlank()) {
            return@withContext runCatching {
                context.assets.open(game.romAsset).use { it.readBytes() }
            }.getOrNull()
        }
        null
    }

    suspend fun importRomFromUri(uri: Uri, overrideFileName: String? = null): GameEntity? = withContext(Dispatchers.IO) {
        importAllRomsFromUri(uri, overrideFileName).firstOrNull()
    }

    suspend fun importAllRomsFromUri(
        uri: Uri,
        overrideFileName: String? = null,
        skipExistingSuffixes: Set<String>? = null
    ): List<GameEntity> = withContext(Dispatchers.IO) {
        val rawName = overrideFileName ?: resolveFileName(uri) ?: "imported_${System.currentTimeMillis()}.a26"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext emptyList()
        importAllRomsFromBytes(rawName, bytes, skipExistingSuffixes)
    }

    suspend fun importRomFromFile(file: File): GameEntity? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        importRomBytes(file.name, file.readBytes())
    }

    suspend fun importRomBytes(fileName: String, rawBytes: ByteArray): GameEntity? = withContext(Dispatchers.IO) {
        importAllRomsFromBytes(fileName, rawBytes).firstOrNull()
    }

    private suspend fun importAllRomsFromBytes(
        fileName: String,
        rawBytes: ByteArray,
        skipExistingSuffixes: Set<String>? = null
    ): List<GameEntity> = withContext(Dispatchers.IO) {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val entries: List<Pair<String, ByteArray>> = if (ext == "zip") {
            extractAllRomsFromZip(rawBytes)
        } else {
            if (ext !in AtariSystem.supportedExtensions) return@withContext emptyList()
            listOf(fileName to rawBytes)
        }
        if (entries.isEmpty()) return@withContext emptyList()

        val palette = listOf("#FF5A1F", "#0088CC", "#7C3AED", "#15803D", "#DC2628", "#EAB308", "#00B4D8")
        val imported = mutableListOf<GameEntity>()
        val now = System.currentTimeMillis()

        for ((entryName, entryBytes) in entries) {
            val romExt = entryName.substringAfterLast('.', "").lowercase()
            if (romExt !in AtariSystem.supportedExtensions && ext != "zip") continue
            if (entryBytes.isEmpty()) continue

            val sanitizedFileName = entryName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            if (skipExistingSuffixes != null && sanitizedFileName in skipExistingSuffixes) {
                continue
            }

            val detectedSystem = AtariSystem.fromExtension(romExt)
            val cleanTitle = entryName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifBlank { "Atari ROM" }
                .uppercase()

            val id = "user_${UUID.randomUUID().toString().take(8)}"
            val destFile = File(romsDir, "${id}_${sanitizedFileName}")
            FileOutputStream(destFile).use { it.write(entryBytes) }

            val game = GameEntity(
                id = id,
                title = cleanTitle,
                system = detectedSystem.name,
                year = when (detectedSystem) {
                    AtariSystem.ATARI_2600 -> 1982
                    AtariSystem.ATARI_5200 -> 1983
                    AtariSystem.ATARI_7800 -> 1987
                    AtariSystem.ATARI_LYNX -> 1990
                    AtariSystem.ATARI_JAGUAR -> 1994
                    AtariSystem.ATARI_800 -> 1984
                },
                genre = "مستورد",
                colorHex = palette[kotlin.math.abs(cleanTitle.hashCode()) % palette.size],
                localRomPath = destFile.absolutePath,
                isFavorite = false,
                addedAt = now + imported.size
            )
            val coverPath = CoverGenerator.ensureCoverCached(context, game)
            val withCover = game.copy(coverPath = coverPath)
            gameDao.insert(withCover)
            imported.add(withCover)
        }
        imported
    }

    private fun extractAllRomsFromZip(zipBytes: ByteArray): List<Pair<String, ByteArray>> {
        val supported = mutableListOf<Pair<String, ByteArray>>()
        var fallbackEntry: Pair<String, ByteArray>? = null
        runCatching {
            ZipInputStream(BufferedInputStream(zipBytes.inputStream())).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.substringAfterLast('/')
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in AtariSystem.supportedExtensions) {
                            supported.add(name to zis.readBytes())
                        } else if (fallbackEntry == null && ext.isNotBlank()) {
                            fallbackEntry = name to zis.readBytes()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        if (supported.isNotEmpty()) return supported
        return listOfNotNull(fallbackEntry)
    }

    private suspend fun existingImportedFileSuffixes(): MutableSet<String> {
        val suffixes = HashSet<String>()
        gameDao.getAllSnapshot().forEach { g ->
            val path = g.localRomPath
            if (path.isNotBlank()) {
                val fileName = File(path).name
                // Strip "user_xxxxxxxx_" prefix if present
                val stripped = if (fileName.startsWith("user_") && fileName.length > 14 && fileName[13] == '_') {
                    fileName.substring(14)
                } else {
                    fileName
                }
                suffixes.add(stripped)
            }
        }
        return suffixes
    }

    suspend fun collectRomUrisFromTree(treeUri: Uri): List<Pair<Uri, String>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<Uri, String>>()
        val queue = ArrayDeque<String>()
        val visited = HashSet<String>()
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return@withContext emptyList()
        queue.add(rootDocId)
        visited.add(rootDocId)

        while (queue.isNotEmpty()) {
            val parentDocId = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            runCatching {
                context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    while (cursor.moveToNext()) {
                        val docId = (if (idCol >= 0) cursor.getString(idCol) else null) ?: continue
                        val name = (if (nameCol >= 0) cursor.getString(nameCol) else null) ?: continue
                        val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (visited.add(docId)) {
                                queue.add(docId)
                            }
                        } else {
                            val ext = name.substringAfterLast('.', "").lowercase()
                            if (ext in AtariSystem.supportedExtensions || ext == "zip") {
                                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                results.add(docUri to name)
                            }
                        }
                    }
                }
            }
        }
        results.sortBy { it.second.lowercase() }
        results
    }

    suspend fun importMultipleRoms(
        uris: List<Uri>,
        onProgress: (imported: Int, processed: Int, total: Int) -> Unit = { _, _, _ -> }
    ): List<GameEntity> = withContext(Dispatchers.IO) {
        val importedGames = mutableListOf<GameEntity>()
        val total = uris.size
        if (total == 0) return@withContext emptyList()
        onProgress(0, 0, total)
        uris.forEachIndexed { idx, uri ->
            val added = runCatching { importAllRomsFromUri(uri) }.getOrDefault(emptyList())
            importedGames.addAll(added)
            onProgress(importedGames.size, idx + 1, total)
        }
        importedGames
    }

    suspend fun importRomFolder(
        treeUri: Uri,
        onProgress: (imported: Int, processed: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val romEntries = collectRomUrisFromTree(treeUri)
        val total = romEntries.size
        if (total == 0) return@withContext 0
        val existingSuffixes = existingImportedFileSuffixes()
        var importedCount = 0
        onProgress(0, 0, total)
        romEntries.forEachIndexed { idx, (docUri, name) ->
            val added = runCatching {
                importAllRomsFromUri(docUri, name, existingSuffixes)
            }.getOrDefault(emptyList())
            if (added.isNotEmpty()) {
                importedCount += added.size
                val sanitized = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                existingSuffixes.add(sanitized)
                added.forEach { g ->
                    val fn = File(g.localRomPath).name
                    if (fn.startsWith("user_") && fn.length > 14 && fn[13] == '_') {
                        existingSuffixes.add(fn.substring(14))
                    }
                }
            }
            onProgress(importedCount, idx + 1, total)
        }
        importedCount
    }

    suspend fun scanWatchedSafTree(treeUriString: String): Int = withContext(Dispatchers.IO) {
        if (treeUriString.isBlank()) return@withContext 0
        val treeUri = Uri.parse(treeUriString)
        importRomFolder(treeUri)
    }

    fun getSaveFile(gameId: String, slotIndex: Int): File =
        File(savesDir, "${gameId}_slot_${slotIndex}.state")

    fun getScreenshotsDir(): File = shotsDir

    suspend fun getCoverFile(game: GameEntity): String = withContext(Dispatchers.IO) {
        if (game.coverPath.isNotBlank() && File(game.coverPath).exists()) {
            return@withContext game.coverPath
        }
        val path = CoverGenerator.ensureCoverCached(context, game)
        gameDao.updateCoverPath(game.id, path)
        path
    }

    suspend fun clearImageCache(): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        listOf(File(context.cacheDir, "covers"), File(context.cacheDir, "image_cache")).forEach { dir ->
            if (dir.exists()) {
                dir.walkBottomUp().forEach { f ->
                    if (f.isFile) freed += f.length()
                    f.delete()
                }
            }
        }
        freed
    }

    suspend fun exportBackupToUri(destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val saves = saveStateDao.getAllSnapshot()
            val cheats = cheatDao.getAllSnapshot()
            val games = gameDao.getAllSnapshot().filter { it.isFavorite || it.playTimeSeconds > 0L }

            val manifest = JSONObject().apply {
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("games", JSONArray().apply {
                    games.forEach { g ->
                        put(JSONObject().apply {
                            put("id", g.id)
                            put("isFavorite", g.isFavorite)
                            put("playTimeSeconds", g.playTimeSeconds)
                            put("lastPlayedAt", g.lastPlayedAt)
                        })
                    }
                })
                put("cheats", JSONArray().apply {
                    cheats.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("gameId", c.gameId)
                            put("name", c.name)
                            put("code", c.code)
                            put("enabled", c.enabled)
                        })
                    }
                })
                put("saves", JSONArray().apply {
                    saves.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("gameId", s.gameId)
                            put("slotIndex", s.slotIndex)
                            put("updatedAt", s.updatedAt)
                        })
                    }
                })
            }

            context.contentResolver.openOutputStream(destUri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifest.toString(2).toByteArray())
                    zos.closeEntry()

                    saves.forEach { s ->
                        val f = File(s.filePath)
                        if (f.exists()) {
                            zos.putNextEntry(ZipEntry("saves/${f.name}"))
                            FileInputStream(f).use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            } ?: return@withContext false
            true
        }.getOrDefault(false)
    }

    suspend fun restoreBackupFromUri(srcUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(srcUri)?.use { it.readBytes() } ?: return@withContext false
            var manifestJson: String? = null
            val restoredFiles = mutableMapOf<String, ByteArray>()

            ZipInputStream(raw.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zis.readBytes()
                        if (entry.name == "manifest.json") {
                            manifestJson = bytes.decodeToString()
                        } else if (entry.name.startsWith("saves/")) {
                            restoredFiles[entry.name.removePrefix("saves/")] = bytes
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val manifest = JSONObject(manifestJson ?: return@withContext false)
            val gamesArr = manifest.optJSONArray("games") ?: JSONArray()
            for (i in 0 until gamesArr.length()) {
                val obj = gamesArr.getJSONObject(i)
                val existing = gameDao.getById(obj.getString("id"))
                if (existing != null) {
                    gameDao.insert(
                        existing.copy(
                            isFavorite = obj.optBoolean("isFavorite", existing.isFavorite),
                            playTimeSeconds = maxOf(existing.playTimeSeconds, obj.optLong("playTimeSeconds", 0L)),
                            lastPlayedAt = maxOf(existing.lastPlayedAt, obj.optLong("lastPlayedAt", 0L))
                        )
                    )
                }
            }

            val cheatsArr = manifest.optJSONArray("cheats") ?: JSONArray()
            for (i in 0 until cheatsArr.length()) {
                val obj = cheatsArr.getJSONObject(i)
                cheatDao.insert(
                    CheatEntity(
                        id = obj.getString("id"),
                        gameId = obj.getString("gameId"),
                        name = obj.getString("name"),
                        code = obj.getString("code"),
                        enabled = obj.optBoolean("enabled", true)
                    )
                )
            }

            val savesArr = manifest.optJSONArray("saves") ?: JSONArray()
            for (i in 0 until savesArr.length()) {
                val obj = savesArr.getJSONObject(i)
                val gameId = obj.getString("gameId")
                val slot = obj.getInt("slotIndex")
                val file = getSaveFile(gameId, slot)
                val payload = restoredFiles[file.name]
                if (payload != null) {
                    file.writeBytes(payload)
                    saveStateDao.upsert(
                        SaveStateEntity(
                            id = obj.getString("id"),
                            gameId = gameId,
                            slotIndex = slot,
                            filePath = file.absolutePath,
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun resolveFileName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}

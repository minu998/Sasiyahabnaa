package com.example.data.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.Settings
import com.example.data.model.CacheCategory
import com.example.data.model.CacheItemEntry
import com.example.data.model.CacheScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CacheCleanerManager(private val context: Context) {

    suspend fun scanAccessibleCache(safTreeUriString: String?): CacheScanResult = withContext(Dispatchers.IO) {
        val items = mutableListOf<CacheItemEntry>()

        // 1. Internal app cacheDir
        context.cacheDir?.let { dir ->
            val bytes = calculateDirSize(dir)
            if (bytes > 0L) {
                items.add(
                    CacheItemEntry(
                        id = "internal_cache",
                        name = "Optimizer Internal Cache",
                        pathDisplay = dir.name,
                        sizeBytes = bytes,
                        category = CacheCategory.APP_CACHE,
                        isSafeToClean = true
                    )
                )
            }
        }

        // 2. Code / shader cache
        context.codeCacheDir?.let { dir ->
            val bytes = calculateDirSize(dir)
            if (bytes > 0L) {
                items.add(
                    CacheItemEntry(
                        id = "code_cache",
                        name = "Compiled Code & Shader Cache",
                        pathDisplay = dir.name,
                        sizeBytes = bytes,
                        category = CacheCategory.CODE_CACHE,
                        isSafeToClean = true
                    )
                )
            }
        }

        // 3. External app cache
        context.externalCacheDir?.let { dir ->
            val bytes = calculateDirSize(dir)
            if (bytes > 0L) {
                items.add(
                    CacheItemEntry(
                        id = "external_cache",
                        name = "External Temp & Cache",
                        pathDisplay = "Android/data/.../cache",
                        sizeBytes = bytes,
                        category = CacheCategory.EXTERNAL_CACHE,
                        isSafeToClean = true
                    )
                )
            }
        }

        // 4. Temp files inside accessible filesDir (*.tmp, *.log)
        val tempFilesBytes = calculateMatchingTempFilesSize(context.filesDir)
        if (tempFilesBytes > 0L) {
            items.add(
                CacheItemEntry(
                    id = "temp_logs",
                    name = "Diagnostic Temp & Log Buffers",
                    pathDisplay = "files/*.tmp, *.log",
                    sizeBytes = tempFilesBytes,
                    category = CacheCategory.TEMP_FILES,
                    isSafeToClean = true
                )
            )
        }

        // 5. Optional SAF user-selected folder (only temporary/cache files, never game data/OBB/maps)
        var safFolderLabel: String? = null
        if (!safTreeUriString.isNullOrBlank()) {
            runCatching {
                val treeUri = Uri.parse(safTreeUriString)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                safFolderLabel = docId.substringAfterLast(':')
                val safBytes = scanSafTempFiles(treeUri)
                if (safBytes > 0L) {
                    items.add(
                        CacheItemEntry(
                            id = "saf_temp",
                            name = "User Folder Temp Files ($safFolderLabel)",
                            pathDisplay = safFolderLabel ?: "Selected Folder",
                            sizeBytes = safBytes,
                            category = CacheCategory.USER_SELECTED_FOLDER,
                            isSafeToClean = true
                        )
                    )
                }
            }
        }

        val total = items.sumOf { it.sizeBytes }
        CacheScanResult(
            items = items,
            totalReclaimableBytes = total,
            lastScanTimeMs = System.currentTimeMillis(),
            safFolderName = safFolderLabel
        )
    }

    suspend fun cleanAccessibleCache(safTreeUriString: String?): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L

        context.cacheDir?.let { dir ->
            reclaimed += deleteChildrenSafe(dir)
        }
        context.codeCacheDir?.let { dir ->
            reclaimed += deleteChildrenSafe(dir)
        }
        context.externalCacheDir?.let { dir ->
            reclaimed += deleteChildrenSafe(dir)
        }
        reclaimed += deleteMatchingTempFiles(context.filesDir)

        if (!safTreeUriString.isNullOrBlank()) {
            runCatching {
                val treeUri = Uri.parse(safTreeUriString)
                reclaimed += deleteSafTempFiles(treeUri)
            }
        }
        reclaimed
    }

    fun buildSystemClearCacheIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(StorageManager.ACTION_CLEAR_APP_CACHE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun buildAppStorageSettingsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var sum = 0L
        dir.listFiles()?.forEach { child ->
            sum += if (child.isDirectory) calculateDirSize(child) else child.length()
        }
        return sum
    }

    private fun deleteChildrenSafe(dir: File): Long {
        if (!dir.exists()) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { child ->
            freed += deleteRecursiveCounting(child)
        }
        return freed
    }

    private fun deleteRecursiveCounting(file: File): Long {
        if (!file.exists()) return 0L
        var freed = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                freed += deleteRecursiveCounting(child)
            }
            file.delete()
        } else {
            val len = file.length()
            if (file.delete()) freed += len
        }
        return freed
    }

    private fun calculateMatchingTempFilesSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var sum = 0L
        dir.listFiles()?.forEach { f ->
            if (f.isFile && isSafeTempFileName(f.name)) {
                sum += f.length()
            }
        }
        return sum
    }

    private fun deleteMatchingTempFiles(dir: File): Long {
        if (!dir.exists()) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { f ->
            if (f.isFile && isSafeTempFileName(f.name)) {
                val len = f.length()
                if (f.delete()) freed += len
            }
        }
        return freed
    }

    private fun isSafeTempFileName(name: String): Boolean {
        val lower = name.lowercase()
        return (lower.endsWith(".tmp") || lower.endsWith(".temp") || lower.endsWith(".log") || lower.endsWith(".cache")) &&
            !lower.contains("account") && !lower.contains("login") && !lower.contains("token")
    }

    private fun scanSafTempFiles(treeUri: Uri): Long {
        var bytes = 0L
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                val size = cursor.getLong(2)
                val mime = cursor.getString(3) ?: ""
                if (mime != DocumentsContract.Document.MIME_TYPE_DIR && isSafeTempFileName(name)) {
                    bytes += size.coerceAtLeast(0L)
                }
            }
        }
        return bytes
    }

    private fun deleteSafTempFiles(treeUri: Uri): Long {
        var freed = 0L
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val size = cursor.getLong(2)
                val mime = cursor.getString(3) ?: ""
                if (mime != DocumentsContract.Document.MIME_TYPE_DIR && isSafeTempFileName(name)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val deleted = runCatching { DocumentsContract.deleteDocument(context.contentResolver, docUri) }
                        .getOrDefault(false)
                    if (deleted) freed += size.coerceAtLeast(0L)
                }
            }
        }
        return freed
    }
}

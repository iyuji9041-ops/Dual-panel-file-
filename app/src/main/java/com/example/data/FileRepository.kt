package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.StatFs
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.model.BreadcrumbSegment
import com.example.model.CategoryStorageStat
import com.example.model.DropActionMode
import com.example.model.FileCategory
import com.example.model.FileDetailsInfo
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.StorageRoot
import com.example.model.TransferRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** One item sitting in the Recycle Bin. */
data class TrashEntry(
    val id: String,
    val name: String,
    val originalPath: String,
    val deletedAt: Long,
    val sizeBytes: Long,
    val isDirectory: Boolean
)

class FileRepository(private val context: Context) {

    private val baseWorkspaceDir: File by lazy {
        File(context.filesDir, "dual_storage_root").apply { mkdirs() }
    }

    val driveADir: File by lazy {
        val externalA = context.getExternalFilesDir(null)
        if (externalA != null && externalA.canWrite()) {
            File(externalA, "Storage_A").apply { mkdirs() }
        } else {
            File(baseWorkspaceDir, "Storage_A").apply { mkdirs() }
        }
    }

    val driveBDir: File by lazy {
        val externalB = context.getExternalFilesDir(null)
        if (externalB != null && externalB.canWrite()) {
            File(externalB, "Storage_B").apply { mkdirs() }
        } else {
            File(baseWorkspaceDir, "Storage_B").apply { mkdirs() }
        }
    }

    private val favoritesSet = mutableSetOf<String>()

    init {
        removeLegacyDemoFilesIfPresent()
        ensureCleanStandardDirectories()
    }

    private fun removeLegacyDemoFilesIfPresent() {
        val legacyMarker = File(baseWorkspaceDir, ".seeded_v1")
        if (legacyMarker.exists()) {
            File(baseWorkspaceDir, "Drive_A_Workspace").deleteRecursively()
            File(baseWorkspaceDir, "Drive_B_Vault").deleteRecursively()
            legacyMarker.delete()
        }
    }

    private fun ensureCleanStandardDirectories() {
        // Only create standard real empty directories if nothing exists yet — zero fake/demo files
        listOf("Photos", "Videos", "Documents", "Downloads").forEach { dirName ->
            File(driveADir, dirName).mkdirs()
        }
        listOf("Backup", "Archives", "Media").forEach { dirName ->
            File(driveBDir, dirName).mkdirs()
        }
    }

    /** Mounted non-primary volumes (SD card / USB OTG) as (display name, root dir). */
    private fun removableVolumes(): List<Pair<String, File>> {
        val result = mutableListOf<Pair<String, File>>()
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                for (vol in sm.storageVolumes) {
                    if (vol.isPrimary) continue
                    val mounted = vol.state == Environment.MEDIA_MOUNTED ||
                        vol.state == Environment.MEDIA_MOUNTED_READ_ONLY
                    val dir = vol.directory
                    if (mounted && dir != null && dir.exists()) {
                        result.add((vol.getDescription(context) ?: "SD Card") to dir)
                    }
                }
            } else {
                // API 24-29: derive the volume root from the app's per-volume files dirs.
                context.getExternalFilesDirs(null).drop(1).filterNotNull().forEach { f ->
                    val idx = f.absolutePath.indexOf("/Android/data")
                    if (idx > 0) {
                        val root = File(f.absolutePath.substring(0, idx))
                        if (root.exists()) result.add("SD Card" to root)
                    }
                }
            }
        } catch (e: Exception) {
            // A storage query must never crash the app; just show no extra volumes.
        }
        return result
    }

    fun getStorageRoots(): List<StorageRoot> {
        val roots = mutableListOf<StorageRoot>()

        roots.add(
            StorageRoot(
                id = "storage_a",
                title = "Storage Drive A",
                subtitle = driveADir.absolutePath,
                rootPath = driveADir.absolutePath,
                iconType = "storage_primary"
            )
        )

        roots.add(
            StorageRoot(
                id = "storage_b",
                title = "Storage Drive B",
                subtitle = driveBDir.absolutePath,
                rootPath = driveBDir.absolutePath,
                iconType = "storage_vault"
            )
        )

        // Include real device shared storage folders if accessible on the device
        val sharedRoot = Environment.getExternalStorageDirectory()
        if (sharedRoot != null && sharedRoot.exists()) {
            if (sharedRoot.canRead()) {
                roots.add(
                    StorageRoot(
                        id = "device_internal",
                        title = "Internal Shared Storage",
                        subtitle = sharedRoot.absolutePath,
                        rootPath = sharedRoot.absolutePath,
                        iconType = "storage_device"
                    )
                )
            }

            val publicFolders = listOf(
                Triple("dcim", "Phone Camera (DCIM)", Environment.DIRECTORY_DCIM),
                Triple("pictures", "Phone Pictures", Environment.DIRECTORY_PICTURES),
                Triple("movies", "Phone Videos (Movies)", Environment.DIRECTORY_MOVIES),
                Triple("downloads", "Phone Downloads", Environment.DIRECTORY_DOWNLOADS),
                Triple("documents", "Phone Documents", Environment.DIRECTORY_DOCUMENTS)
            )

            for ((id, label, envType) in publicFolders) {
                val pubDir = Environment.getExternalStoragePublicDirectory(envType)
                if (pubDir != null && pubDir.exists() && pubDir.canRead()) {
                    roots.add(
                        StorageRoot(
                            id = id,
                            title = label,
                            subtitle = pubDir.absolutePath,
                            rootPath = pubDir.absolutePath,
                            iconType = "folder_device"
                        )
                    )
                }
            }
        }

        // Real removable volumes (SD card, USB OTG) - shown as their own roots.
        for ((title, dir) in removableVolumes()) {
            roots.add(
                StorageRoot(
                    id = "volume_${dir.name}",
                    title = title,
                    subtitle = dir.absolutePath,
                    rootPath = dir.absolutePath,
                    iconType = "storage_device"
                )
            )
        }

        // Standard sub-volumes inside Drive A & Drive B
        val photosA = File(driveADir, "Photos").apply { mkdirs() }
        val videosA = File(driveADir, "Videos").apply { mkdirs() }
        val docsA = File(driveADir, "Documents").apply { mkdirs() }

        roots.add(
            StorageRoot(
                id = "photos_a",
                title = "Photos Folder",
                subtitle = "Drive A / Photos",
                rootPath = photosA.absolutePath,
                iconType = "folder_media"
            )
        )
        roots.add(
            StorageRoot(
                id = "videos_a",
                title = "Videos Folder",
                subtitle = "Drive A / Videos",
                rootPath = videosA.absolutePath,
                iconType = "folder_video"
            )
        )
        roots.add(
            StorageRoot(
                id = "docs_a",
                title = "Documents Folder",
                subtitle = "Drive A / Documents",
                rootPath = docsA.absolutePath,
                iconType = "folder_docs"
            )
        )

        return roots
    }

    suspend fun listFiles(
        dirPath: String,
        sortOption: SortOption,
        searchQuery: String,
        showHidden: Boolean,
        categoryFilter: FileCategory? = null
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val rawFiles = if (searchQuery.isBlank()) {
            dir.listFiles()?.toList().orEmpty()
        } else {
            dir.walkTopDown()
                .maxDepth(5)
                .filter { it.absolutePath != dir.absolutePath }
                .filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
                .toList()
        }

        val filtered = rawFiles.filter { file ->
            if (!showHidden && file.name.startsWith(".")) false else true
        }.map { file ->
            file.toFileItem()
        }.filter { item ->
            // Category chip: keep folders visible so navigation still works.
            categoryFilter == null || item.isDirectory || item.category == categoryFilter
        }

        val comparator = when (sortOption) {
            SortOption.NAME_ASC -> compareBy<FileItem> { !it.isDirectory }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortOption.NAME_DESC -> compareBy<FileItem> { !it.isDirectory }
                .thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortOption.SIZE_DESC -> compareBy<FileItem> { !it.isDirectory }
                .thenByDescending { if (it.isDirectory) it.childCount.toLong() else it.sizeBytes }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortOption.DATE_DESC -> compareBy<FileItem> { !it.isDirectory }
                .thenByDescending { it.lastModified }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortOption.TYPE -> compareBy<FileItem> { !it.isDirectory }
                .thenBy { it.category.ordinal }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }

        filtered.sortedWith(comparator)
    }

    fun buildBreadcrumbs(currentPath: String, rootPath: String, rootTitle: String): List<BreadcrumbSegment> {
        val segments = mutableListOf<BreadcrumbSegment>()
        val cleanRoot = runCatching { File(rootPath).canonicalPath }.getOrDefault(rootPath)
        val cleanCurrent = runCatching { File(currentPath).canonicalPath }.getOrDefault(currentPath)

        segments.add(BreadcrumbSegment(name = rootTitle, path = cleanRoot))

        if (cleanCurrent.startsWith(cleanRoot) && cleanCurrent.length > cleanRoot.length) {
            val relative = cleanCurrent.substring(cleanRoot.length).trimStart(File.separatorChar)
            var accumulator = cleanRoot
            relative.split(File.separatorChar).filter { it.isNotEmpty() }.forEach { part ->
                accumulator = "$accumulator${File.separator}$part"
                segments.add(BreadcrumbSegment(name = part, path = accumulator))
            }
        }
        return segments
    }

    fun getStorageSpace(path: String): Pair<Long, Long> {
        return try {
            val stat = StatFs(File(path).absolutePath)
            Pair(stat.availableBytes, stat.totalBytes)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }

    suspend fun transferItems(
        sourcePaths: List<String>,
        targetDirPath: String,
        actionMode: DropActionMode
    ): Result<TransferRecord> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = File(targetDirPath)
            require(targetDir.exists() && targetDir.isDirectory) {
                "Target folder does not exist."
            }

            val movedPairs = mutableListOf<Pair<String, String>>()
            val itemNames = mutableListOf<String>()
            var totalBytes = 0L
            var primarySourceParent = ""

            for (srcPath in sourcePaths) {
                val srcFile = File(srcPath)
                if (!srcFile.exists()) continue

                if (primarySourceParent.isEmpty()) {
                    primarySourceParent = srcFile.parentFile?.name ?: "Storage"
                }

                val srcCanonical = srcFile.canonicalPath
                val targetCanonical = targetDir.canonicalPath
                if (srcCanonical == targetCanonical || targetCanonical.startsWith("$srcCanonical${File.separator}")) {
                    throw IllegalArgumentException("Cannot ${actionMode.name.lowercase()} folder '${srcFile.name}' into itself.")
                }

                if (actionMode == DropActionMode.MOVE && srcFile.parentFile?.canonicalPath == targetCanonical) {
                    throw IllegalArgumentException("'${srcFile.name}' is already in ${targetDir.name}.")
                }

                val destinationFile = resolveDestinationFile(targetDir, srcFile.name)
                val itemSize = computeSizeRecursively(srcFile)

                if (actionMode == DropActionMode.MOVE) {
                    val renamedImmediately = srcFile.renameTo(destinationFile)
                    if (!renamedImmediately) {
                        srcFile.copyRecursively(destinationFile, overwrite = true)
                        srcFile.deleteRecursively()
                    }
                } else {
                    srcFile.copyRecursively(destinationFile, overwrite = true)
                }

                movedPairs.add(srcFile.absolutePath to destinationFile.absolutePath)
                itemNames.add(srcFile.name)
                totalBytes += itemSize
            }

            if (movedPairs.isEmpty()) {
                throw IllegalStateException("No valid files were transferred.")
            }

            TransferRecord(
                id = UUID.randomUUID().toString(),
                actionMode = actionMode,
                itemNames = itemNames,
                sourceDir = primarySourceParent,
                targetDir = targetDir.name,
                movedPairs = movedPairs,
                totalBytes = totalBytes,
                canUndo = true
            )
        }
    }

    suspend fun undoTransfer(record: TransferRecord): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (record.actionMode) {
                DropActionMode.MOVE -> {
                    for ((originalPath, currentPath) in record.movedPairs) {
                        val movedFile = File(currentPath)
                        val originalFile = File(originalPath)
                        originalFile.parentFile?.mkdirs()
                        if (movedFile.exists()) {
                            val restored = movedFile.renameTo(originalFile)
                            if (!restored) {
                                movedFile.copyRecursively(originalFile, overwrite = true)
                                movedFile.deleteRecursively()
                            }
                        }
                    }
                    "Restored ${record.itemNames.size} item(s) to ${record.sourceDir}"
                }
                DropActionMode.COPY -> {
                    for ((_, copiedPath) in record.movedPairs) {
                        val copiedFile = File(copiedPath)
                        if (copiedFile.exists()) {
                            copiedFile.deleteRecursively()
                        }
                    }
                    "Removed ${record.itemNames.size} copied item(s) from ${record.targetDir}"
                }
            }
        }
    }

    suspend fun createFolder(parentPath: String, folderName: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sanitized = sanitizeFileName(folderName)
                require(sanitized.isNotBlank()) { "Folder name cannot be empty." }
                val target = resolveDestinationFile(File(parentPath), sanitized)
                if (!target.mkdirs()) {
                    throw IllegalStateException("Could not create folder '$sanitized'.")
                }
                target.toFileItem()
            }
        }

    suspend fun createFile(parentPath: String, fileName: String, content: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sanitized = sanitizeFileName(fileName)
                require(sanitized.isNotBlank()) { "File name cannot be empty." }
                val target = resolveDestinationFile(File(parentPath), sanitized)
                target.writeText(content)
                target.toFileItem()
            }
        }

    suspend fun renameItem(path: String, newName: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val src = File(path)
                require(src.exists()) { "Item not found." }
                val sanitized = sanitizeFileName(newName)
                require(sanitized.isNotBlank()) { "New name cannot be empty." }
                val target = File(src.parentFile, sanitized)
                require(!target.exists()) { "An item named '$sanitized' already exists here." }
                if (!src.renameTo(target)) {
                    throw IllegalStateException("Failed to rename '${src.name}'.")
                }
                target.toFileItem()
            }
        }

    // ------------------------------------------------------------------
    // RECYCLE BIN (offline, lives in app-specific storage)
    // Layout: .recycle_bin/<id>/<original name>  +  .recycle_bin/<id>/.meta
    // .meta = line 1: original absolute path, line 2: deletion time (epoch ms)
    // ------------------------------------------------------------------
    private val trashRoot: File by lazy {
        File(context.getExternalFilesDir(null) ?: context.filesDir, ".recycle_bin").apply { mkdirs() }
    }

    /** rename is instant on the same volume; otherwise copy + delete (e.g. SD card -> internal). */
    private fun moveOrCopy(src: File, dst: File): Boolean {
        if (src.renameTo(dst)) return true
        return try {
            src.copyRecursively(dst, overwrite = false) && src.deleteRecursively()
        } catch (e: Exception) {
            dst.deleteRecursively()
            false
        }
    }

    suspend fun moveToTrash(paths: Collection<String>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                var moved = 0
                for ((index, path) in paths.withIndex()) {
                    val src = File(path)
                    if (!src.exists()) continue
                    val now = System.currentTimeMillis()
                    val dir = File(trashRoot, "${now}_$index").apply { mkdirs() }
                    if (moveOrCopy(src, File(dir, src.name))) {
                        File(dir, ".meta").writeText("${src.absolutePath}\n$now")
                        moved++
                    } else {
                        dir.deleteRecursively()
                    }
                }
                moved
            }
        }

    suspend fun listTrash(): List<TrashEntry> = withContext(Dispatchers.IO) {
        (trashRoot.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { dir ->
                val item = dir.listFiles()?.firstOrNull { it.name != ".meta" } ?: return@mapNotNull null
                val meta = File(dir, ".meta")
                val lines = if (meta.exists()) meta.readLines() else emptyList()
                TrashEntry(
                    id = dir.name,
                    name = item.name,
                    originalPath = lines.getOrNull(0) ?: item.absolutePath,
                    deletedAt = lines.getOrNull(1)?.toLongOrNull() ?: dir.lastModified(),
                    sizeBytes = if (item.isDirectory) {
                        item.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    } else item.length(),
                    isDirectory = item.isDirectory
                )
            }
            .sortedByDescending { it.deletedAt }
    }

    /** Puts the item back where it came from (adds "(restored)" if that name is taken now). */
    suspend fun restoreFromTrash(id: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(trashRoot, id)
            val item = dir.listFiles()?.firstOrNull { it.name != ".meta" }
                ?: error("Item not found in Recycle Bin")
            val originalPath = File(dir, ".meta").takeIf { it.exists() }?.readLines()?.firstOrNull()
                ?: error("Original location unknown")
            val original = File(originalPath)
            val parent = original.parentFile ?: error("Original folder missing")
            parent.mkdirs()

            var dest = original
            var n = 1
            while (dest.exists()) {
                val suffix = if (n == 1) " (restored)" else " (restored $n)"
                val ext = if (original.extension.isNotEmpty() && original.isFile.not() && item.isFile) ".${original.extension}"
                else if (item.isFile && original.extension.isNotEmpty()) ".${original.extension}" else ""
                dest = File(parent, original.nameWithoutExtension + suffix + ext)
                n++
            }
            if (!moveOrCopy(item, dest)) error("Could not restore item")
            dir.deleteRecursively()
            dest.absolutePath
        }
    }

    suspend fun purgeFromTrash(id: String): Boolean = withContext(Dispatchers.IO) {
        File(trashRoot, id).deleteRecursively()
    }

    suspend fun emptyTrash(): Int = withContext(Dispatchers.IO) {
        var n = 0
        trashRoot.listFiles()?.forEach { if (it.deleteRecursively()) n++ }
        n
    }

    /** Auto-clean: permanently remove items that sat in the bin longer than [days]. */
    suspend fun purgeOldTrash(days: Int = 30) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        listTrash().filter { it.deletedAt < cutoff }.forEach { File(trashRoot, it.id).deleteRecursively() }
    }

    suspend fun deleteItems(paths: Collection<String>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                var deletedCount = 0
                for (path in paths) {
                    val file = File(path)
                    if (file.exists() && file.deleteRecursively()) {
                        deletedCount++
                    }
                }
                deletedCount
            }
        }

    suspend fun compressToZip(
        sourcePaths: List<String>,
        destinationDirPath: String,
        archiveName: String
    ): Result<FileItem> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = sanitizeFileName(archiveName).let {
                if (it.endsWith(".zip", ignoreCase = true)) it else "$it.zip"
            }
            val zipFile = resolveDestinationFile(File(destinationDirPath), cleanName)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                for (path in sourcePaths) {
                    val srcFile = File(path)
                    if (srcFile.exists()) {
                        addFileToZip(zos, srcFile, srcFile.name)
                    }
                }
            }
            zipFile.toFileItem()
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryPath: String) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children.isNullOrEmpty()) {
                zos.putNextEntry(ZipEntry("$entryPath/"))
                zos.closeEntry()
            } else {
                children.forEach { child ->
                    addFileToZip(zos, child, "$entryPath/${child.name}")
                }
            }
        } else {
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { origin ->
                    val entry = ZipEntry(entryPath)
                    entry.time = file.lastModified()
                    zos.putNextEntry(entry)
                    origin.copyTo(zos, bufferSize = 8192)
                    zos.closeEntry()
                }
            }
        }
    }

    suspend fun extractZipArchive(zipPath: String, targetDirPath: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val zipFile = File(zipPath)
                require(zipFile.exists() && zipFile.extension.equals("zip", ignoreCase = true)) {
                    "Selected file is not a valid ZIP archive."
                }
                val baseFolderName = zipFile.nameWithoutExtension.ifBlank { "Extracted_Archive" }
                val extractRoot = resolveDestinationFile(File(targetDirPath), baseFolderName)
                extractRoot.mkdirs()

                val rootCanonical = extractRoot.canonicalPath
                ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(extractRoot, entry.name)
                        if (!outFile.canonicalPath.startsWith(rootCanonical)) {
                            throw SecurityException("Blocked unsafe ZIP entry path: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos, bufferSize = 8192)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                extractRoot.toFileItem()
            }
        }

    suspend fun importExternalUris(uris: List<Uri>, targetDirPath: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetDir = File(targetDirPath)
                var importedCount = 0
                val resolver = context.contentResolver

                for (uri in uris) {
                    val mimeType = resolver.getType(uri)
                    val fallbackExt = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "dat"
                    val displayName = queryDisplayName(uri)
                        ?: "Media_${System.currentTimeMillis()}.$fallbackExt"
                    val destFile = resolveDestinationFile(targetDir, sanitizeFileName(displayName))
                    resolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                        importedCount++
                    }
                }
                importedCount
            }
        }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        }.getOrNull()
    }

    /**
     * Launches the phone's native Gallery or Video Player app via FileProvider Intent.ACTION_VIEW.
     */
    fun openInExternalSystemViewer(path: String): Result<Unit> {
        return runCatching {
            val file = File(path)
            require(file.exists()) { "File not found." }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val ext = file.extension.lowercase()
            val mimeType = resolveMimeType(ext)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun inspectFileDetails(path: String): Result<FileDetailsInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                require(file.exists()) { "File no longer exists." }
                val item = file.toFileItem()

                if (file.isDirectory) {
                    val totalSize = computeSizeRecursively(file)
                    return@runCatching FileDetailsInfo(
                        item = item.copy(sizeBytes = totalSize),
                        md5Checksum = "Directory (${item.childCount} direct items)",
                        sha256Checksum = "Total recursive size: ${com.example.model.formatBytes(totalSize)}",
                        mimeType = "inode/directory",
                        readableTextContent = null,
                        isEditableText = false
                    )
                }

                val md5 = computeHash(file, "MD5")
                val sha256 = computeHash(file, "SHA-256")
                val ext = file.extension.lowercase()
                val textExtensions = setOf(
                    "txt", "md", "json", "csv", "kt", "java", "py", "xml",
                    "svg", "html", "css", "js", "sql", "yaml", "yml", "log", "ini"
                )
                val isText = ext in textExtensions && file.length() <= 256 * 1024

                val textContent = if (isText) {
                    runCatching { file.readText() }.getOrNull()
                } else null

                val archiveEntries = if (ext == "zip") {
                    runCatching {
                        ZipFile(file).use { zip ->
                            zip.entries().asSequence().take(40).map { entry ->
                                if (entry.isDirectory) "📁 ${entry.name}"
                                else "📄 ${entry.name} (${com.example.model.formatBytes(entry.size.coerceAtLeast(0L))})"
                            }.toList()
                        }
                    }.getOrDefault(emptyList())
                } else emptyList()

                FileDetailsInfo(
                    item = item,
                    md5Checksum = md5,
                    sha256Checksum = sha256,
                    mimeType = resolveMimeType(ext),
                    readableTextContent = textContent,
                    isEditableText = isText,
                    archiveEntries = archiveEntries
                )
            }
        }

    suspend fun saveFileTextContent(path: String, newContent: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                require(file.exists() && file.isFile) { "Target file is not editable." }
                file.writeText(newContent)
                file.toFileItem()
            }
        }

    suspend fun computeWorkspaceStorageStats(): List<CategoryStorageStat> =
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<FileCategory, Pair<Long, Int>>()
            FileCategory.entries.forEach { cat ->
                if (cat != FileCategory.FOLDER) map[cat] = 0L to 0
            }

            listOf(driveADir, driveBDir).forEach { root ->
                root.walkTopDown().forEach { file ->
                    if (file.isFile && !file.name.startsWith(".")) {
                        val cat = categorizeFile(file)
                        val prev = map[cat] ?: (0L to 0)
                        map[cat] = (prev.first + file.length()) to (prev.second + 1)
                    }
                }
            }

            val totalBytes = map.values.sumOf { it.first }.coerceAtLeast(1L)
            map.entries
                .map { (cat, pair) ->
                    CategoryStorageStat(
                        category = cat,
                        bytes = pair.first,
                        count = pair.second,
                        fraction = (pair.first.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    )
                }
                .sortedByDescending { it.bytes }
        }

    fun toggleFavorite(path: String): Boolean {
        return if (favoritesSet.contains(path)) {
            favoritesSet.remove(path)
            false
        } else {
            favoritesSet.add(path)
            true
        }
    }

    private fun File.toFileItem(): FileItem {
        val isDir = isDirectory
        val ext = if (isDir) "" else extension.lowercase()
        val count = if (isDir) (listFiles()?.size ?: 0) else 0
        val perms = buildString {
            append(if (isDir) "d" else "-")
            append(if (canRead()) "r" else "-")
            append(if (canWrite()) "w" else "-")
            append(if (canExecute()) "x" else "-")
            append("r--r--")
        }
        return FileItem(
            path = absolutePath,
            name = name,
            isDirectory = isDir,
            sizeBytes = if (isDir) 0L else length(),
            lastModified = lastModified(),
            extension = ext,
            category = categorizeFile(this),
            childCount = count,
            permissions = perms,
            isFavorite = favoritesSet.contains(absolutePath)
        )
    }

    private fun categorizeFile(file: File): FileCategory {
        if (file.isDirectory) return FileCategory.FOLDER
        return when (file.extension.lowercase()) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> FileCategory.IMAGE
            "mp4", "mkv", "webm", "mov", "3gp", "avi", "m4v" -> FileCategory.VIDEO
            "mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "amr", "wma", "mid", "midi" -> FileCategory.AUDIO
            "pdf" -> FileCategory.PDF
            "txt", "md" -> FileCategory.TEXT
            "doc", "docx", "rtf", "odt", "xls", "xlsx", "ods", "ppt", "pptx", "odp", "epub" -> FileCategory.DOCUMENT
            "apk", "xapk", "apks", "apkm" -> FileCategory.APK
            "kt", "java", "py", "js", "ts", "html", "css", "sh", "sql", "xml" -> FileCategory.CODE
            "zip", "tar", "gz", "7z", "rar" -> FileCategory.ARCHIVE
            "json", "csv", "yaml", "yml", "ini", "toml", "log" -> FileCategory.DATA
            else -> FileCategory.OTHER
        }
    }

    fun resolveMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            "avi" -> "video/x-msvideo"
            "md" -> "text/markdown"
            "txt", "log" -> "text/plain"
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            "csv" -> "text/csv"
            "kt" -> "text/x-kotlin"
            "py" -> "text/x-python"
            "sql" -> "application/sql"
            "zip" -> "application/zip"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
                ?: "application/octet-stream"
        }
    }

    private fun resolveDestinationFile(targetDir: File, originalName: String): File {
        var candidate = File(targetDir, originalName)
        if (!candidate.exists()) return candidate

        val dotIndex = originalName.lastIndexOf('.')
        val base = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val ext = if (dotIndex > 0) originalName.substring(dotIndex) else ""

        var counter = 1
        while (candidate.exists()) {
            candidate = File(targetDir, "${base}_$counter$ext")
            counter++
        }
        return candidate
    }

    private fun computeSizeRecursively(file: File): Long {
        if (file.isFile) return file.length()
        return file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun computeHash(file: File, algorithm: String): String {
        return runCatching {
            val digest = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read = fis.read(buffer)
                while (read > 0) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrDefault("Unavailable")
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}

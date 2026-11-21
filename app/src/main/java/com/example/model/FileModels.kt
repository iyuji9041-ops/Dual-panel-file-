package com.example.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

enum class PaneId(val title: String, val badge: String) {
    LEFT("Pane A", "A"),
    RIGHT("Pane B", "B");

    fun opposite(): PaneId = if (this == LEFT) RIGHT else LEFT
}

enum class SplitOrientation(val label: String) {
    VERTICAL("Top / Bottom Split"),
    HORIZONTAL("Side-by-Side Split")
}

enum class DropActionMode(val label: String, val verb: String) {
    MOVE("Move", "Moved"),
    COPY("Copy", "Copied")
}

enum class SortOption(val label: String) {
    NAME_ASC("Name (A → Z)"),
    NAME_DESC("Name (Z → A)"),
    SIZE_DESC("Size (Largest)"),
    DATE_DESC("Modified (Newest)"),
    TYPE("Category / Type")
}

enum class ViewLayoutMode {
    LIST,
    GRID
}

enum class FileCategory(val label: String) {
    FOLDER("Folder"),
    IMAGE("Photo"),
    VIDEO("Video"),
    AUDIO("Audio"),
    PDF("PDF"),
    DOCUMENT("Document"),
    TEXT("Text"),
    CODE("Code & Script"),
    ARCHIVE("ZIP Archive"),
    APK("APK"),
    DATA("Config & Data"),
    OTHER("File")
}

enum class NewFileTemplate(
    val title: String,
    val defaultName: String,
    val defaultContent: String
) {
    PLAIN_TEXT("Text Document (.txt)", "Untitled.txt", ""),
    MARKDOWN("Markdown Note (.md)", "Notes.md", "# "),
    JSON_CONFIG("JSON File (.json)", "data.json", "{\n  \n}\n"),
    CSV_SHEET("CSV Sheet (.csv)", "sheet.csv", "")
}

data class FileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String,
    val category: FileCategory,
    val childCount: Int = 0,
    val permissions: String = "rw-r--r--",
    val isFavorite: Boolean = false
) {
    val file: File get() = File(path)

    val isPhoto: Boolean
        get() = category == FileCategory.IMAGE

    val isVideo: Boolean
        get() = category == FileCategory.VIDEO

    fun formattedSize(): String {
        if (isDirectory) {
            return "$childCount item${if (childCount == 1) "" else "s"}"
        }
        return formatBytes(sizeBytes)
    }

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(Date(lastModified))
    }
}

data class StorageRoot(
    val id: String,
    val title: String,
    val subtitle: String,
    val rootPath: String,
    val iconType: String
)

data class BreadcrumbSegment(
    val name: String,
    val path: String
)

data class PaneState(
    val paneId: PaneId,
    val currentPath: String = "",
    val rootPath: String = "",
    val rootTitle: String = "",
    val items: List<FileItem> = emptyList(),
    val breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val sortOption: SortOption = SortOption.NAME_ASC,
    val viewMode: ViewLayoutMode = ViewLayoutMode.LIST,
    val searchQuery: String = "",
    val showHidden: Boolean = false,
    val categoryFilter: FileCategory? = null,
    val freeSpaceBytes: Long = 0L,
    val totalSpaceBytes: Long = 0L
)

data class TransferRecord(
    val id: String,
    val actionMode: DropActionMode,
    val itemNames: List<String>,
    val sourceDir: String,
    val targetDir: String,
    val movedPairs: List<Pair<String, String>>,
    val totalBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val canUndo: Boolean = true
)

data class FileDetailsInfo(
    val item: FileItem,
    val md5Checksum: String,
    val sha256Checksum: String,
    val mimeType: String,
    val readableTextContent: String?,
    val isEditableText: Boolean,
    val archiveEntries: List<String> = emptyList()
)

data class CategoryStorageStat(
    val category: FileCategory,
    val bytes: Long,
    val count: Int,
    val fraction: Float
)

data class PendingDropRequest(
    val sourcePane: PaneId,
    val targetPane: PaneId,
    val items: List<FileItem>,
    val targetDirPath: String,
    val targetDisplayName: String
)

data class MediaGallerySession(
    val paneId: PaneId,
    val mediaItems: List<FileItem>,
    val initialIndex: Int
)

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(4)
    val pre = "KMGT"[exp - 1]
    return String.format(Locale.US, "%.1f %sB", bytes / 1024.0.pow(exp.toDouble()), pre)
}

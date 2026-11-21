package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileRepository
import com.example.model.CategoryStorageStat
import com.example.model.DropActionMode
import com.example.model.FileDetailsInfo
import com.example.model.FileItem
import com.example.model.MediaGallerySession
import com.example.model.NewFileTemplate
import com.example.model.PaneId
import com.example.model.PaneState
import com.example.model.PendingDropRequest
import com.example.model.SortOption
import com.example.model.SplitOrientation
import com.example.model.StorageRoot
import com.example.model.TransferRecord
import com.example.model.ViewLayoutMode
import com.example.ui.theme.MaterialPaletteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class DragPayload(
    val sourcePane: PaneId,
    val items: List<FileItem>,
    val primaryItem: FileItem
)

data class DualFileUiState(
    val leftPane: PaneState = PaneState(paneId = PaneId.LEFT),
    val rightPane: PaneState = PaneState(paneId = PaneId.RIGHT),
    val activePane: PaneId = PaneId.LEFT,
    val splitOrientation: SplitOrientation = SplitOrientation.VERTICAL,
    val hasUserOverriddenSplit: Boolean = false,
    val storageRoots: List<StorageRoot> = emptyList(),
    val transferHistory: List<TransferRecord> = emptyList(),
    val latestUndoRecord: TransferRecord? = null,
    val statusMessage: String? = null,
    val pendingDropRequest: PendingDropRequest? = null,
    val mediaGallerySession: MediaGallerySession? = null,
    val inspectedFileDetails: FileDetailsInfo? = null,
    val storageStats: List<CategoryStorageStat> = emptyList(),
    val isStorageSheetVisible: Boolean = false,
    val isTransferLogVisible: Boolean = false,
    val paletteStyle: MaterialPaletteStyle = MaterialPaletteStyle.FROSTED_AMOLED,
    val darkThemeOverride: Boolean? = true // Default to Frosted Matte AMOLED Dark
) {
    fun pane(id: PaneId): PaneState = if (id == PaneId.LEFT) leftPane else rightPane
}

class DualFileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(DualFileUiState())
    val uiState: StateFlow<DualFileUiState> = _uiState.asStateFlow()

    // Recycle Bin contents (refreshed when the bin dialog opens or an item is restored/purged).
    private val _trash = MutableStateFlow<List<com.example.data.TrashEntry>>(emptyList())
    val trash: StateFlow<List<com.example.data.TrashEntry>> = _trash.asStateFlow()

    init {
        initializeWorkspaces()
        // Housekeeping: anything older than 30 days in the bin is removed for good.
        viewModelScope.launch { repository.purgeOldTrash(30) }
        startLiveStorageTicker()
    }

    /**
     * Live free/used numbers: re-reads StatFs for both panes every few seconds so the storage
     * strip stays correct while files are downloaded/deleted by other apps too.
     */
    private fun startLiveStorageTicker() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(4000)
                for (paneId in listOf(PaneId.LEFT, PaneId.RIGHT)) {
                    val path = _uiState.value.pane(paneId).currentPath
                    if (path.isBlank()) continue
                    val (free, total) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        repository.getStorageSpace(path)
                    }
                    val pane = _uiState.value.pane(paneId)
                    if (pane.freeSpaceBytes != free || pane.totalSpaceBytes != total) {
                        updatePaneState(paneId) { it.copy(freeSpaceBytes = free, totalSpaceBytes = total) }
                    }
                }
            }
        }
    }

    /** Call after the user grants "All files access" so the SD card / shared storage appear. */
    fun reloadStorageRoots() {
        _uiState.update { it.copy(storageRoots = repository.getStorageRoots()) }
        refreshBothPanes()
    }

    private fun initializeWorkspaces() {
        val roots = repository.getStorageRoots()
        val driveA = roots.firstOrNull { it.id == "storage_a" } ?: roots.first()
        val driveB = roots.firstOrNull { it.id == "storage_b" } ?: roots.last()

        val (freeA, totalA) = repository.getStorageSpace(driveA.rootPath)
        val (freeB, totalB) = repository.getStorageSpace(driveB.rootPath)

        _uiState.update { state ->
            state.copy(
                storageRoots = roots,
                leftPane = state.leftPane.copy(
                    currentPath = driveA.rootPath,
                    rootPath = driveA.rootPath,
                    rootTitle = driveA.title,
                    freeSpaceBytes = freeA,
                    totalSpaceBytes = totalA
                ),
                rightPane = state.rightPane.copy(
                    currentPath = driveB.rootPath,
                    rootPath = driveB.rootPath,
                    rootTitle = driveB.title,
                    freeSpaceBytes = freeB,
                    totalSpaceBytes = totalB
                )
            )
        }
        refreshBothPanes()
    }

    fun refreshBothPanes() {
        refreshPane(PaneId.LEFT)
        refreshPane(PaneId.RIGHT)
    }

    fun refreshPane(paneId: PaneId) {
        viewModelScope.launch {
            val currentPane = _uiState.value.pane(paneId)
            if (currentPane.currentPath.isBlank()) return@launch

            val items = repository.listFiles(
                dirPath = currentPane.currentPath,
                sortOption = currentPane.sortOption,
                searchQuery = currentPane.searchQuery,
                showHidden = currentPane.showHidden,
                categoryFilter = currentPane.categoryFilter
            )
            val breadcrumbs = repository.buildBreadcrumbs(
                currentPath = currentPane.currentPath,
                rootPath = currentPane.rootPath,
                rootTitle = currentPane.rootTitle
            )
            val (free, total) = repository.getStorageSpace(currentPane.currentPath)

            updatePaneState(paneId) { pane ->
                val validPaths = items.map { it.path }.toSet()
                pane.copy(
                    items = items,
                    breadcrumbs = breadcrumbs,
                    selectedPaths = pane.selectedPaths.intersect(validPaths),
                    freeSpaceBytes = free,
                    totalSpaceBytes = total
                )
            }
        }
    }

    fun setCategoryFilter(paneId: PaneId, category: com.example.model.FileCategory?) {
        updatePaneState(paneId) { it.copy(categoryFilter = category) }
        refreshPane(paneId)
    }

    fun setActivePane(paneId: PaneId) {
        _uiState.update { it.copy(activePane = paneId) }
    }

    fun navigateToDirectory(paneId: PaneId, path: String) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return
        setActivePane(paneId)
        updatePaneState(paneId) { pane ->
            pane.copy(
                currentPath = dir.absolutePath,
                selectedPaths = emptySet(),
                searchQuery = ""
            )
        }
        refreshPane(paneId)
    }

    fun switchStorageRoot(paneId: PaneId, root: StorageRoot) {
        setActivePane(paneId)
        updatePaneState(paneId) { pane ->
            pane.copy(
                currentPath = root.rootPath,
                rootPath = root.rootPath,
                rootTitle = root.title,
                selectedPaths = emptySet(),
                searchQuery = ""
            )
        }
        refreshPane(paneId)
    }

    fun navigateUp(paneId: PaneId): Boolean {
        val pane = _uiState.value.pane(paneId)
        val currentFile = File(pane.currentPath)
        val rootFile = File(pane.rootPath)
        if (runCatching { currentFile.canonicalPath == rootFile.canonicalPath }.getOrDefault(false)) {
            return false
        }
        val parent = currentFile.parentFile ?: return false
        navigateToDirectory(paneId, parent.absolutePath)
        return true
    }

    fun toggleSelection(paneId: PaneId, itemPath: String) {
        setActivePane(paneId)
        updatePaneState(paneId) { pane ->
            val updated = pane.selectedPaths.toMutableSet()
            if (updated.contains(itemPath)) {
                updated.remove(itemPath)
            } else {
                updated.add(itemPath)
            }
            pane.copy(selectedPaths = updated)
        }
    }

    fun selectAllInPane(paneId: PaneId) {
        setActivePane(paneId)
        updatePaneState(paneId) { pane ->
            val allPaths = pane.items.map { it.path }.toSet()
            val nextSelection = if (pane.selectedPaths.size == allPaths.size) emptySet() else allPaths
            pane.copy(selectedPaths = nextSelection)
        }
    }

    fun clearSelection(paneId: PaneId) {
        updatePaneState(paneId) { it.copy(selectedPaths = emptySet()) }
    }

    fun updateSearchQuery(paneId: PaneId, query: String) {
        updatePaneState(paneId) { it.copy(searchQuery = query) }
        refreshPane(paneId)
    }

    fun updateSortOption(paneId: PaneId, sortOption: SortOption) {
        updatePaneState(paneId) { it.copy(sortOption = sortOption) }
        refreshPane(paneId)
    }

    fun toggleViewLayoutMode(paneId: PaneId) {
        updatePaneState(paneId) { pane ->
            val next = if (pane.viewMode == ViewLayoutMode.LIST) ViewLayoutMode.GRID else ViewLayoutMode.LIST
            pane.copy(viewMode = next)
        }
    }

    fun toggleSplitOrientation() {
        _uiState.update { state ->
            val next = if (state.splitOrientation == SplitOrientation.VERTICAL) {
                SplitOrientation.HORIZONTAL
            } else {
                SplitOrientation.VERTICAL
            }
            state.copy(splitOrientation = next, hasUserOverriddenSplit = true)
        }
    }

    fun applyResponsiveSplitDefault(isWideScreen: Boolean) {
        _uiState.update { state ->
            if (state.hasUserOverriddenSplit) state
            else state.copy(
                // MP-Manager style: panes are ALWAYS side-by-side by default, even on phones.
                // (Old behaviour stacked them top/bottom on narrow screens and ate the whole screen.)
                // The user can still switch to top/bottom manually from the top-bar menu.
                splitOrientation = SplitOrientation.HORIZONTAL
            )
        }
    }

    fun swapPanes() {
        _uiState.update { state ->
            val leftCopy = state.leftPane
            val rightCopy = state.rightPane
            state.copy(
                leftPane = rightCopy.copy(paneId = PaneId.LEFT),
                rightPane = leftCopy.copy(paneId = PaneId.RIGHT)
            )
        }
        refreshBothPanes()
        postMessage("Swapped Pane A and Pane B")
    }

    fun mirrorActivePaneToOpposite() {
        val activeId = _uiState.value.activePane
        val oppositeId = activeId.opposite()
        val sourcePane = _uiState.value.pane(activeId)
        updatePaneState(oppositeId) { opposite ->
            opposite.copy(
                currentPath = sourcePane.currentPath,
                rootPath = sourcePane.rootPath,
                rootTitle = sourcePane.rootTitle,
                selectedPaths = emptySet()
            )
        }
        refreshPane(oppositeId)
        postMessage("Synced ${activeId.title} folder to ${oppositeId.title}")
    }

    fun buildDragPayload(paneId: PaneId, draggedItem: FileItem): DragPayload {
        val pane = _uiState.value.pane(paneId)
        val batchItems = if (pane.selectedPaths.contains(draggedItem.path) && pane.selectedPaths.size > 1) {
            pane.items.filter { pane.selectedPaths.contains(it.path) }
        } else {
            listOf(draggedItem)
        }
        return DragPayload(
            sourcePane = paneId,
            items = batchItems,
            primaryItem = draggedItem
        )
    }

    /**
     * When the user drops dragged file(s) onto a folder or opposite pane,
     * prompt the user to choose whether to COPY or MOVE before executing the transfer.
     */
    fun onDropItems(
        payload: DragPayload,
        targetPaneId: PaneId,
        targetFolderOverridePath: String? = null
    ) {
        val targetPaneState = _uiState.value.pane(targetPaneId)
        val destinationDir = targetFolderOverridePath ?: targetPaneState.currentPath
        val folderName = File(destinationDir).name.ifBlank { targetPaneState.rootTitle }

        // Dropping back into the very folder the items came from (same pane) = user changed their
        // mind (a "cancel"), so don't pop a pointless Copy/Move dialog.
        if (payload.sourcePane == targetPaneId &&
            payload.items.all { File(it.path).parent == destinationDir }
        ) return

        _uiState.update {
            it.copy(
                pendingDropRequest = PendingDropRequest(
                    sourcePane = payload.sourcePane,
                    targetPane = targetPaneId,
                    items = payload.items,
                    targetDirPath = destinationDir,
                    targetDisplayName = "${targetPaneId.title} / $folderName"
                )
            )
        }
    }

    /**
     * Also prompt "Copy or Move?" when the user taps the Center Bridge transfer button with selected items.
     */
    fun requestBridgeTransferPrompt(sourcePaneId: PaneId) {
        val sourcePane = _uiState.value.pane(sourcePaneId)
        val targetPaneId = sourcePaneId.opposite()
        val targetPane = _uiState.value.pane(targetPaneId)

        if (sourcePane.selectedPaths.isEmpty()) {
            postMessage("Long-press to select items or drag any item across panes.")
            return
        }
        val selectedItems = sourcePane.items.filter { sourcePane.selectedPaths.contains(it.path) }
        if (selectedItems.isEmpty()) return

        val folderName = File(targetPane.currentPath).name.ifBlank { targetPane.rootTitle }
        _uiState.update {
            it.copy(
                pendingDropRequest = PendingDropRequest(
                    sourcePane = sourcePaneId,
                    targetPane = targetPaneId,
                    items = selectedItems,
                    targetDirPath = targetPane.currentPath,
                    targetDisplayName = "${targetPaneId.title} / $folderName"
                )
            )
        }
    }

    fun confirmPendingDrop(chosenAction: DropActionMode) {
        val pending = _uiState.value.pendingDropRequest ?: return
        _uiState.update { it.copy(pendingDropRequest = null) }
        executeTransfer(
            sourcePaths = pending.items.map { it.path },
            targetDirPath = pending.targetDirPath,
            actionMode = chosenAction,
            sourcePaneToClear = pending.sourcePane
        )
    }

    fun dismissPendingDrop() {
        _uiState.update { it.copy(pendingDropRequest = null) }
    }

    fun transferSelectedToOppositePane(sourcePaneId: PaneId, actionMode: DropActionMode) {
        val sourcePane = _uiState.value.pane(sourcePaneId)
        val targetPane = _uiState.value.pane(sourcePaneId.opposite())
        if (sourcePane.selectedPaths.isEmpty()) {
            postMessage("Long-press files to select or drag any item to transfer.")
            return
        }
        executeTransfer(
            sourcePaths = sourcePane.selectedPaths.toList(),
            targetDirPath = targetPane.currentPath,
            actionMode = actionMode,
            sourcePaneToClear = sourcePaneId
        )
    }

    fun transferSingleItemToOppositePane(sourcePaneId: PaneId, item: FileItem, actionMode: DropActionMode) {
        val targetPane = _uiState.value.pane(sourcePaneId.opposite())
        executeTransfer(
            sourcePaths = listOf(item.path),
            targetDirPath = targetPane.currentPath,
            actionMode = actionMode,
            sourcePaneToClear = sourcePaneId
        )
    }

    private fun executeTransfer(
        sourcePaths: List<String>,
        targetDirPath: String,
        actionMode: DropActionMode,
        sourcePaneToClear: PaneId
    ) {
        viewModelScope.launch {
            val result = repository.transferItems(
                sourcePaths = sourcePaths,
                targetDirPath = targetDirPath,
                actionMode = actionMode
            )
            result.onSuccess { record ->
                clearSelection(sourcePaneToClear)
                _uiState.update { state ->
                    val label = if (record.itemNames.size == 1) {
                        "'${record.itemNames.first()}'"
                    } else {
                        "${record.itemNames.size} items"
                    }
                    state.copy(
                        transferHistory = listOf(record) + state.transferHistory,
                        latestUndoRecord = record,
                        statusMessage = "${actionMode.verb} $label to ${record.targetDir}"
                    )
                }
                refreshBothPanes()
            }.onFailure { error ->
                postMessage(error.message ?: "Transfer could not be completed.")
            }
        }
    }

    fun undoLatestTransfer() {
        val record = _uiState.value.latestUndoRecord ?: return
        undoTransferRecord(record)
    }

    fun undoTransferRecord(record: TransferRecord) {
        viewModelScope.launch {
            repository.undoTransfer(record).onSuccess { msg ->
                _uiState.update { state ->
                    state.copy(
                        transferHistory = state.transferHistory.map {
                            if (it.id == record.id) it.copy(canUndo = false) else it
                        },
                        latestUndoRecord = null,
                        statusMessage = msg
                    )
                }
                refreshBothPanes()
            }.onFailure { err ->
                postMessage(err.message ?: "Could not undo transfer.")
            }
        }
    }

    fun createNewFolder(paneId: PaneId, folderName: String) {
        val parentPath = _uiState.value.pane(paneId).currentPath
        viewModelScope.launch {
            repository.createFolder(parentPath, folderName).onSuccess { created ->
                refreshBothPanes()
                postMessage("Created folder '${created.name}' in ${paneId.title}")
            }.onFailure { err ->
                postMessage(err.message ?: "Failed to create folder.")
            }
        }
    }

    fun createFromTemplate(paneId: PaneId, template: NewFileTemplate, customName: String) {
        val finalName = customName.ifBlank { template.defaultName }
        val parentPath = _uiState.value.pane(paneId).currentPath
        viewModelScope.launch {
            repository.createFile(parentPath, finalName, template.defaultContent).onSuccess { created ->
                refreshBothPanes()
                postMessage("Created '${created.name}' in ${paneId.title}")
            }.onFailure { err ->
                postMessage(err.message ?: "Failed to create file.")
            }
        }
    }

    fun renameFileItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            repository.renameItem(item.path, newName).onSuccess { renamed ->
                refreshBothPanes()
                postMessage("Renamed to '${renamed.name}'")
            }.onFailure { err ->
                postMessage(err.message ?: "Rename failed.")
            }
        }
    }

    /** Delete = move to Recycle Bin (restorable for 30 days). */
    fun deleteItemsInPane(paneId: PaneId, paths: Collection<String>) {
        viewModelScope.launch {
            repository.moveToTrash(paths).onSuccess { count ->
                clearSelection(paneId)
                refreshBothPanes()
                refreshTrash()
                postMessage("Moved $count item${if (count == 1) "" else "s"} to Recycle Bin")
            }.onFailure { err ->
                postMessage(err.message ?: "Delete failed.")
            }
        }
    }

    fun refreshTrash() {
        viewModelScope.launch { _trash.value = repository.listTrash() }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(id).onSuccess { restoredPath ->
                refreshTrash()
                refreshBothPanes()
                postMessage("Restored to ${File(restoredPath).parent ?: restoredPath}")
            }.onFailure { err ->
                postMessage(err.message ?: "Restore failed.")
            }
        }
    }

    fun purgeFromTrash(id: String) {
        viewModelScope.launch {
            repository.purgeFromTrash(id)
            refreshTrash()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val n = repository.emptyTrash()
            refreshTrash()
            postMessage("Recycle Bin emptied ($n item${if (n == 1) "" else "s"} removed)")
        }
    }

    fun compressItemsToZip(paneId: PaneId, paths: List<String>, archiveName: String, toOppositePane: Boolean) {
        val destDir = if (toOppositePane) {
            _uiState.value.pane(paneId.opposite()).currentPath
        } else {
            _uiState.value.pane(paneId).currentPath
        }
        viewModelScope.launch {
            repository.compressToZip(paths, destDir, archiveName).onSuccess { zipItem ->
                clearSelection(paneId)
                refreshBothPanes()
                postMessage("Created archive '${zipItem.name}' (${zipItem.formattedSize()})")
            }.onFailure { err ->
                postMessage(err.message ?: "ZIP compression failed.")
            }
        }
    }

    fun extractZip(paneId: PaneId, zipItem: FileItem, extractToOppositePane: Boolean) {
        val targetPane = if (extractToOppositePane) paneId.opposite() else paneId
        val destDir = _uiState.value.pane(targetPane).currentPath
        viewModelScope.launch {
            repository.extractZipArchive(zipItem.path, destDir).onSuccess { extractedFolder ->
                refreshBothPanes()
                postMessage("Extracted to '${extractedFolder.name}' in ${targetPane.title}")
            }.onFailure { err ->
                postMessage(err.message ?: "Failed to extract ZIP archive.")
            }
        }
    }

    fun importFilesFromSaf(paneId: PaneId, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val destDir = _uiState.value.pane(paneId).currentPath
        viewModelScope.launch {
            repository.importExternalUris(uris, destDir).onSuccess { count ->
                refreshBothPanes()
                postMessage("Added $count file(s) into ${paneId.title}")
            }.onFailure { err ->
                postMessage(err.message ?: "Failed to add files.")
            }
        }
    }

    /**
     * Opens a file when tapped:
     * - If it's a Photo or Video, opens the full-screen Media Gallery & Video Player (with swipe between all media in the pane).
     * - Otherwise opens the File Inspector & Text/Code Editor.
     */
    fun openFileOrMedia(paneId: PaneId, item: FileItem) {
        if (item.isPhoto || item.isVideo) {
            val paneItems = _uiState.value.pane(paneId).items
            val mediaItems = paneItems.filter { it.isPhoto || it.isVideo }
            val index = mediaItems.indexOfFirst { it.path == item.path }.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    mediaGallerySession = MediaGallerySession(
                        paneId = paneId,
                        mediaItems = mediaItems.ifEmpty { listOf(item) },
                        initialIndex = index
                    )
                )
            }
        } else {
            openFileInspector(item)
        }
    }

    fun closeMediaGallery() {
        _uiState.update { it.copy(mediaGallerySession = null) }
    }

    fun openInPhoneGalleryApp(path: String) {
        repository.openInExternalSystemViewer(path).onFailure {
            postMessage("No external gallery or player found for this file.")
        }
    }

    fun openFileInspector(item: FileItem) {
        viewModelScope.launch {
            repository.inspectFileDetails(item.path).onSuccess { details ->
                _uiState.update { it.copy(inspectedFileDetails = details) }
            }.onFailure { err ->
                postMessage(err.message ?: "Unable to inspect file.")
            }
        }
    }

    fun closeFileInspector() {
        _uiState.update { it.copy(inspectedFileDetails = null) }
    }

    fun saveEditedFileContent(path: String, updatedContent: String) {
        viewModelScope.launch {
            repository.saveFileTextContent(path, updatedContent).onSuccess { updatedItem ->
                refreshBothPanes()
                openFileInspector(updatedItem)
                postMessage("Saved changes to '${updatedItem.name}'")
            }.onFailure { err ->
                postMessage(err.message ?: "Failed to save file.")
            }
        }
    }

    fun openStorageInsights() {
        viewModelScope.launch {
            val stats = repository.computeWorkspaceStorageStats()
            _uiState.update {
                it.copy(
                    storageStats = stats,
                    isStorageSheetVisible = true
                )
            }
        }
    }

    fun closeStorageInsights() {
        _uiState.update { it.copy(isStorageSheetVisible = false) }
    }

    fun setTransferLogVisible(visible: Boolean) {
        _uiState.update { it.copy(isTransferLogVisible = visible) }
    }

    fun setPaletteStyle(style: MaterialPaletteStyle) {
        _uiState.update { it.copy(paletteStyle = style) }
    }

    fun toggleDarkTheme() {
        _uiState.update { state ->
            val current = state.darkThemeOverride ?: true
            state.copy(darkThemeOverride = !current)
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, latestUndoRecord = null) }
    }

    private fun postMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg, latestUndoRecord = null) }
    }

    private fun updatePaneState(paneId: PaneId, transform: (PaneState) -> PaneState) {
        _uiState.update { state ->
            if (paneId == PaneId.LEFT) {
                state.copy(leftPane = transform(state.leftPane))
            } else {
                state.copy(rightPane = transform(state.rightPane))
            }
        }
    }
}

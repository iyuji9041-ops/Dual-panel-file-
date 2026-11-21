package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FileItem
import com.example.model.PaneId
import com.example.model.SplitOrientation
import com.example.ui.components.CompressZipDialog
import com.example.ui.components.CreateFileFromTemplateDialog
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.DropCopyOrMovePromptDialog
import com.example.ui.components.FileInspectorModalSheet
import com.example.ui.components.FilePaneView
import com.example.ui.components.FloatingDragPreviewOverlay
import com.example.ui.components.MediaGalleryViewerOverlay
import com.example.ui.components.RecycleBinDialog
import com.example.ui.components.RenameItemDialog
import com.example.ui.components.StorageInsightsModalSheet
import com.example.ui.components.TransferHistoryModalSheet
import com.example.ui.components.rememberDualPaneDragDropState
import com.example.viewmodel.DualFileViewModel
import java.io.File

@Composable
fun DualFileManagerScreen(
    viewModel: DualFileViewModel,
    isDarkTheme: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var createFolderTargetPane by remember { mutableStateOf<PaneId?>(null) }
    var createFileTargetPane by remember { mutableStateOf<PaneId?>(null) }
    var renameTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var compressTarget by remember { mutableStateOf<Pair<PaneId, List<String>>?>(null) }
    var safImportTargetPane by remember { mutableStateOf(PaneId.LEFT) }
    var mediaPickerTargetPane by remember { mutableStateOf(PaneId.LEFT) }

    // SAF General File Picker
    val safDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFilesFromSaf(safImportTargetPane, uris)
        }
    }

    // Zero-Permission Android Photo & Video Picker
    val photoVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFilesFromSaf(mediaPickerTargetPane, uris)
        }
    }

    val activePaneState = uiState.pane(uiState.activePane)
    val canHandleBack = activePaneState.selectedPaths.isNotEmpty() ||
        activePaneState.breadcrumbs.size > 1

    BackHandler(enabled = canHandleBack) {
        if (activePaneState.selectedPaths.isNotEmpty()) {
            viewModel.clearSelection(uiState.activePane)
        } else {
            viewModel.navigateUp(uiState.activePane)
        }
    }

    // Status snackbar with Undo button
    LaunchedEffect(uiState.statusMessage, uiState.latestUndoRecord) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        val hasUndo = uiState.latestUndoRecord != null
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = if (hasUndo) "Undo" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && hasUndo) {
            viewModel.undoLatestTransfer()
        } else {
            viewModel.clearStatusMessage()
        }
    }

    val dragDropState = rememberDualPaneDragDropState { payload, targetPane, targetFolderPath ->
        viewModel.onDropItems(
            payload = payload,
            targetPaneId = targetPane,
            targetFolderOverridePath = targetFolderPath
        )
    }

    var showRecycleBin by remember { mutableStateOf(false) }
    val primaryGlowColor = MaterialTheme.colorScheme.primary
    val secondaryGlowColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            // Explicitly handle Android 16 / 17 safe drawing insets so nothing clips or sticks to edges
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    )
                )
            },
            topBar = {
                // MP Manager style: ONE slim bar. Shows the ACTIVE pane's folder + item count.
                DualFilesTopControlBar(
                    activePath = activePaneState.currentPath,
                    activeRootTitle = activePaneState.rootTitle,
                    itemCount = activePaneState.items.size,
                    splitOrientation = uiState.splitOrientation,
                    transferCount = uiState.transferHistory.size,
                    isDarkTheme = isDarkTheme,
                    onQuickAddMedia = {
                        mediaPickerTargetPane = uiState.activePane
                        photoVideoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onToggleSplitOrientation = viewModel::toggleSplitOrientation,
                    onOpenTransferLog = { viewModel.setTransferLogVisible(true) },
                    onOpenStorageInsights = viewModel::openStorageInsights,
                    onOpenRecycleBin = {
                        viewModel.refreshTrash()
                        showRecycleBin = true
                    },
                    onToggleTheme = viewModel::toggleDarkTheme
                )
            },
            bottomBar = {
                // MP Manager style bottom bar: acts on the ACTIVE pane. Replaces the big
                // "transfer bridge" that used to sit between the panes and waste screen space.
                DualBottomActionBar(
                    activeHasSelection = activePaneState.selectedPaths.isNotEmpty(),
                    activePaneTitle = uiState.activePane.title,
                    onUp = { viewModel.navigateUp(uiState.activePane) },
                    onTransfer = { viewModel.requestBridgeTransferPrompt(uiState.activePane) },
                    onNewFolder = { createFolderTargetPane = uiState.activePane },
                    onNewFile = { createFileTargetPane = uiState.activePane },
                    onImportFiles = {
                        safImportTargetPane = uiState.activePane
                        safDocumentLauncher.launch(arrayOf("*/*"))
                    },
                    onMirror = viewModel::mirrorActivePaneToOpposite,
                    onSwap = viewModel::swapPanes
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(
                        WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                    )
            ) {
                val isWideLayout = maxWidth >= 680.dp
                LaunchedEffect(isWideLayout) {
                    viewModel.applyResponsiveSplitDefault(isWideLayout)
                }

                var splitFraction by remember { mutableFloatStateOf(0.5f) }
                val density = LocalDensity.current
                val totalHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
                val totalWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.splitOrientation == SplitOrientation.VERTICAL) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            FilePaneView(
                                paneState = uiState.leftPane,
                                isActive = uiState.activePane == PaneId.LEFT,
                                storageRoots = uiState.storageRoots,
                                dragDropState = dragDropState,
                                onFocusPane = { viewModel.setActivePane(PaneId.LEFT) },
                                onNavigateToDir = { viewModel.navigateToDirectory(PaneId.LEFT, it) },
                                onNavigateUp = { viewModel.navigateUp(PaneId.LEFT) },
                                onSwitchRoot = { viewModel.switchStorageRoot(PaneId.LEFT, it) },
                                onToggleSelection = { viewModel.toggleSelection(PaneId.LEFT, it) },
                                onSelectAll = { viewModel.selectAllInPane(PaneId.LEFT) },
                                onClearSelection = { viewModel.clearSelection(PaneId.LEFT) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(PaneId.LEFT, it) },
                                onSortOptionChange = { viewModel.updateSortOption(PaneId.LEFT, it) },
                                onToggleViewMode = { viewModel.toggleViewLayoutMode(PaneId.LEFT) },
                                onStartDragItem = { item, startOffset ->
                                    val payload = viewModel.buildDragPayload(PaneId.LEFT, item)
                                    dragDropState.startDrag(payload, startOffset)
                                },
                                onOpenItem = { item ->
                                    viewModel.openFileOrMedia(PaneId.LEFT, item)
                                },
                                onOpenInPhoneGallery = { item ->
                                    viewModel.openInPhoneGalleryApp(item.path)
                                },
                                onInspectDetails = viewModel::openFileInspector,
                                onRequestCreateFolder = { createFolderTargetPane = PaneId.LEFT },
                                onRequestCreateFile = { createFileTargetPane = PaneId.LEFT },
                                onRequestPickPhotosVideos = {
                                    mediaPickerTargetPane = PaneId.LEFT
                                    photoVideoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onRequestImportFiles = {
                                    safImportTargetPane = PaneId.LEFT
                                    safDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                onRequestRenameItem = { renameTargetItem = it },
                                onRequestDeleteItems = { viewModel.deleteItemsInPane(PaneId.LEFT, it) },
                                onCategoryFilterChange = { viewModel.setCategoryFilter(PaneId.LEFT, it) },
                                onRequestCompressItems = { paths ->
                                    compressTarget = PaneId.LEFT to paths
                                },
                                onRequestExtractZip = { zipItem, toOpposite ->
                                    viewModel.extractZip(PaneId.LEFT, zipItem, toOpposite)
                                },
                                onTransferSelectedToOpposite = { mode ->
                                    viewModel.transferSelectedToOppositePane(PaneId.LEFT, mode)
                                },
                                onTransferSingleToOpposite = { item, mode ->
                                    viewModel.transferSingleItemToOppositePane(PaneId.LEFT, item, mode)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(splitFraction)
                            )

                            // Interactive Frosted Matte Center Transfer Bridge (Vertical Split)
                            CenterTransferBridgeHorizontal(
                                leftSelectedCount = uiState.leftPane.selectedPaths.size,
                                rightSelectedCount = uiState.rightPane.selectedPaths.size,
                                onTransferAToB = {
                                    viewModel.requestBridgeTransferPrompt(PaneId.LEFT)
                                },
                                onTransferBToA = {
                                    viewModel.requestBridgeTransferPrompt(PaneId.RIGHT)
                                },
                                onMirrorActiveToOpposite = viewModel::mirrorActivePaneToOpposite,
                                onResetSplitRatio = { splitFraction = 0.5f },
                                onDragSplitDelta = { deltaY ->
                                    splitFraction = (splitFraction + (deltaY / totalHeightPx))
                                        .coerceIn(0.28f, 0.72f)
                                }
                            )

                            FilePaneView(
                                paneState = uiState.rightPane,
                                isActive = uiState.activePane == PaneId.RIGHT,
                                storageRoots = uiState.storageRoots,
                                dragDropState = dragDropState,
                                onFocusPane = { viewModel.setActivePane(PaneId.RIGHT) },
                                onNavigateToDir = { viewModel.navigateToDirectory(PaneId.RIGHT, it) },
                                onNavigateUp = { viewModel.navigateUp(PaneId.RIGHT) },
                                onSwitchRoot = { viewModel.switchStorageRoot(PaneId.RIGHT, it) },
                                onToggleSelection = { viewModel.toggleSelection(PaneId.RIGHT, it) },
                                onSelectAll = { viewModel.selectAllInPane(PaneId.RIGHT) },
                                onClearSelection = { viewModel.clearSelection(PaneId.RIGHT) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(PaneId.RIGHT, it) },
                                onSortOptionChange = { viewModel.updateSortOption(PaneId.RIGHT, it) },
                                onToggleViewMode = { viewModel.toggleViewLayoutMode(PaneId.RIGHT) },
                                onStartDragItem = { item, startOffset ->
                                    val payload = viewModel.buildDragPayload(PaneId.RIGHT, item)
                                    dragDropState.startDrag(payload, startOffset)
                                },
                                onOpenItem = { item ->
                                    viewModel.openFileOrMedia(PaneId.RIGHT, item)
                                },
                                onOpenInPhoneGallery = { item ->
                                    viewModel.openInPhoneGalleryApp(item.path)
                                },
                                onInspectDetails = viewModel::openFileInspector,
                                onRequestCreateFolder = { createFolderTargetPane = PaneId.RIGHT },
                                onRequestCreateFile = { createFileTargetPane = PaneId.RIGHT },
                                onRequestPickPhotosVideos = {
                                    mediaPickerTargetPane = PaneId.RIGHT
                                    photoVideoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onRequestImportFiles = {
                                    safImportTargetPane = PaneId.RIGHT
                                    safDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                onRequestRenameItem = { renameTargetItem = it },
                                onRequestDeleteItems = { viewModel.deleteItemsInPane(PaneId.RIGHT, it) },
                                onCategoryFilterChange = { viewModel.setCategoryFilter(PaneId.RIGHT, it) },
                                onRequestCompressItems = { paths ->
                                    compressTarget = PaneId.RIGHT to paths
                                },
                                onRequestExtractZip = { zipItem, toOpposite ->
                                    viewModel.extractZip(PaneId.RIGHT, zipItem, toOpposite)
                                },
                                onTransferSelectedToOpposite = { mode ->
                                    viewModel.transferSelectedToOppositePane(PaneId.RIGHT, mode)
                                },
                                onTransferSingleToOpposite = { item, mode ->
                                    viewModel.transferSingleItemToOppositePane(PaneId.RIGHT, item, mode)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f - splitFraction)
                            )
                        }
                    } else {
                        // Side-by-Side (Horizontal) Dual-Pane Layout
                        Row(modifier = Modifier.fillMaxSize()) {
                            FilePaneView(
                                paneState = uiState.leftPane,
                                isActive = uiState.activePane == PaneId.LEFT,
                                storageRoots = uiState.storageRoots,
                                dragDropState = dragDropState,
                                onFocusPane = { viewModel.setActivePane(PaneId.LEFT) },
                                onNavigateToDir = { viewModel.navigateToDirectory(PaneId.LEFT, it) },
                                onNavigateUp = { viewModel.navigateUp(PaneId.LEFT) },
                                onSwitchRoot = { viewModel.switchStorageRoot(PaneId.LEFT, it) },
                                onToggleSelection = { viewModel.toggleSelection(PaneId.LEFT, it) },
                                onSelectAll = { viewModel.selectAllInPane(PaneId.LEFT) },
                                onClearSelection = { viewModel.clearSelection(PaneId.LEFT) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(PaneId.LEFT, it) },
                                onSortOptionChange = { viewModel.updateSortOption(PaneId.LEFT, it) },
                                onToggleViewMode = { viewModel.toggleViewLayoutMode(PaneId.LEFT) },
                                onStartDragItem = { item, startOffset ->
                                    val payload = viewModel.buildDragPayload(PaneId.LEFT, item)
                                    dragDropState.startDrag(payload, startOffset)
                                },
                                onOpenItem = { item ->
                                    viewModel.openFileOrMedia(PaneId.LEFT, item)
                                },
                                onOpenInPhoneGallery = { item ->
                                    viewModel.openInPhoneGalleryApp(item.path)
                                },
                                onInspectDetails = viewModel::openFileInspector,
                                onRequestCreateFolder = { createFolderTargetPane = PaneId.LEFT },
                                onRequestCreateFile = { createFileTargetPane = PaneId.LEFT },
                                onRequestPickPhotosVideos = {
                                    mediaPickerTargetPane = PaneId.LEFT
                                    photoVideoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onRequestImportFiles = {
                                    safImportTargetPane = PaneId.LEFT
                                    safDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                onRequestRenameItem = { renameTargetItem = it },
                                onRequestDeleteItems = { viewModel.deleteItemsInPane(PaneId.LEFT, it) },
                                onCategoryFilterChange = { viewModel.setCategoryFilter(PaneId.LEFT, it) },
                                onRequestCompressItems = { paths ->
                                    compressTarget = PaneId.LEFT to paths
                                },
                                onRequestExtractZip = { zipItem, toOpposite ->
                                    viewModel.extractZip(PaneId.LEFT, zipItem, toOpposite)
                                },
                                onTransferSelectedToOpposite = { mode ->
                                    viewModel.transferSelectedToOppositePane(PaneId.LEFT, mode)
                                },
                                onTransferSingleToOpposite = { item, mode ->
                                    viewModel.transferSingleItemToOppositePane(PaneId.LEFT, item, mode)
                                },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(splitFraction)
                            )

                            // Thin divider (MP style). Drag to resize, double-tap-free: tap to reset 50/50.
                            PaneSplitDividerVertical(
                                onResetSplitRatio = { splitFraction = 0.5f },
                                onDragSplitDelta = { deltaX ->
                                    splitFraction = (splitFraction + (deltaX / totalWidthPx))
                                        .coerceIn(0.30f, 0.70f)
                                }
                            )

                            FilePaneView(
                                paneState = uiState.rightPane,
                                isActive = uiState.activePane == PaneId.RIGHT,
                                storageRoots = uiState.storageRoots,
                                dragDropState = dragDropState,
                                onFocusPane = { viewModel.setActivePane(PaneId.RIGHT) },
                                onNavigateToDir = { viewModel.navigateToDirectory(PaneId.RIGHT, it) },
                                onNavigateUp = { viewModel.navigateUp(PaneId.RIGHT) },
                                onSwitchRoot = { viewModel.switchStorageRoot(PaneId.RIGHT, it) },
                                onToggleSelection = { viewModel.toggleSelection(PaneId.RIGHT, it) },
                                onSelectAll = { viewModel.selectAllInPane(PaneId.RIGHT) },
                                onClearSelection = { viewModel.clearSelection(PaneId.RIGHT) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(PaneId.RIGHT, it) },
                                onSortOptionChange = { viewModel.updateSortOption(PaneId.RIGHT, it) },
                                onToggleViewMode = { viewModel.toggleViewLayoutMode(PaneId.RIGHT) },
                                onStartDragItem = { item, startOffset ->
                                    val payload = viewModel.buildDragPayload(PaneId.RIGHT, item)
                                    dragDropState.startDrag(payload, startOffset)
                                },
                                onOpenItem = { item ->
                                    viewModel.openFileOrMedia(PaneId.RIGHT, item)
                                },
                                onOpenInPhoneGallery = { item ->
                                    viewModel.openInPhoneGalleryApp(item.path)
                                },
                                onInspectDetails = viewModel::openFileInspector,
                                onRequestCreateFolder = { createFolderTargetPane = PaneId.RIGHT },
                                onRequestCreateFile = { createFileTargetPane = PaneId.RIGHT },
                                onRequestPickPhotosVideos = {
                                    mediaPickerTargetPane = PaneId.RIGHT
                                    photoVideoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onRequestImportFiles = {
                                    safImportTargetPane = PaneId.RIGHT
                                    safDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                onRequestRenameItem = { renameTargetItem = it },
                                onRequestDeleteItems = { viewModel.deleteItemsInPane(PaneId.RIGHT, it) },
                                onCategoryFilterChange = { viewModel.setCategoryFilter(PaneId.RIGHT, it) },
                                onRequestCompressItems = { paths ->
                                    compressTarget = PaneId.RIGHT to paths
                                },
                                onRequestExtractZip = { zipItem, toOpposite ->
                                    viewModel.extractZip(PaneId.RIGHT, zipItem, toOpposite)
                                },
                                onTransferSelectedToOpposite = { mode ->
                                    viewModel.transferSelectedToOppositePane(PaneId.RIGHT, mode)
                                },
                                onTransferSingleToOpposite = { item, mode ->
                                    viewModel.transferSingleItemToOppositePane(PaneId.RIGHT, item, mode)
                                },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f - splitFraction)
                            )
                        }
                    }
                }
            }
        }

        // Global Floating Drag Preview Card rendered above both panes
        FloatingDragPreviewOverlay(
            dragDropState = dragDropState,
            leftPaneTitle = File(uiState.leftPane.currentPath).name.ifBlank { uiState.leftPane.rootTitle },
            rightPaneTitle = File(uiState.rightPane.currentPath).name.ifBlank { uiState.rightPane.rootTitle }
        )

        // Full-Screen Photo Gallery & Video Player Overlay (when a photo or video is tapped)
        uiState.mediaGallerySession?.let { session ->
            MediaGalleryViewerOverlay(
                session = session,
                onClose = viewModel::closeMediaGallery,
                onOpenInPhoneGallery = { item ->
                    viewModel.openInPhoneGalleryApp(item.path)
                },
                onInspectDetails = { item ->
                    viewModel.closeMediaGallery()
                    viewModel.openFileInspector(item)
                },
                onDeleteItem = { item ->
                    viewModel.deleteItemsInPane(session.paneId, listOf(item.path))
                }
            )
        }
    }

    // Prompt "Copy or Move?" Dialog right when the user drops dragged file(s)
    uiState.pendingDropRequest?.let { pendingDrop ->
        DropCopyOrMovePromptDialog(
            pendingDrop = pendingDrop,
            onConfirmAction = viewModel::confirmPendingDrop,
            onDismiss = viewModel::dismissPendingDrop
        )
    }

    // File Inspector & Code/Text Editor Bottom Sheet
    uiState.inspectedFileDetails?.let { details ->
        FileInspectorModalSheet(
            details = details,
            activePane = uiState.activePane,
            onDismiss = viewModel::closeFileInspector,
            onSaveTextContent = viewModel::saveEditedFileContent,
            onOpenInExternalApp = viewModel::openInPhoneGalleryApp,
            onExtractZip = { item, toOpposite ->
                viewModel.extractZip(uiState.activePane, item, toOpposite)
            }
        )
    }

    if (showRecycleBin) {
        val trashEntries by viewModel.trash.collectAsStateWithLifecycle()
        RecycleBinDialog(
            entries = trashEntries,
            onRestore = viewModel::restoreFromTrash,
            onDeleteForever = viewModel::purgeFromTrash,
            onEmptyBin = viewModel::emptyTrash,
            onDismiss = { showRecycleBin = false }
        )
    }

    if (uiState.isStorageSheetVisible) {
        StorageInsightsModalSheet(
            stats = uiState.storageStats,
            paletteStyle = uiState.paletteStyle,
            onSelectPalette = viewModel::setPaletteStyle,
            onDismiss = viewModel::closeStorageInsights
        )
    }

    if (uiState.isTransferLogVisible) {
        TransferHistoryModalSheet(
            history = uiState.transferHistory,
            onUndoRecord = viewModel::undoTransferRecord,
            onDismiss = { viewModel.setTransferLogVisible(false) }
        )
    }

    createFolderTargetPane?.let { targetPane ->
        CreateFolderDialog(
            paneTitle = targetPane.title,
            onDismiss = { createFolderTargetPane = null },
            onConfirm = { folderName ->
                viewModel.createNewFolder(targetPane, folderName)
            }
        )
    }

    createFileTargetPane?.let { targetPane ->
        CreateFileFromTemplateDialog(
            paneTitle = targetPane.title,
            onDismiss = { createFileTargetPane = null },
            onConfirm = { template, customName ->
                viewModel.createFromTemplate(targetPane, template, customName)
            }
        )
    }

    renameTargetItem?.let { item ->
        RenameItemDialog(
            item = item,
            onDismiss = { renameTargetItem = null },
            onConfirm = { newName ->
                viewModel.renameFileItem(item, newName)
            }
        )
    }

    compressTarget?.let { (paneId, paths) ->
        val defaultZipName = if (paths.size == 1) {
            "${File(paths.first()).nameWithoutExtension}_Archive.zip"
        } else {
            "Batch_${paths.size}_Items.zip"
        }
        CompressZipDialog(
            sourcePane = paneId,
            itemCount = paths.size,
            defaultArchiveName = defaultZipName,
            onDismiss = { compressTarget = null },
            onConfirm = { archiveName, toOpposite ->
                viewModel.compressItemsToZip(paneId, paths, archiveName, toOpposite)
            }
        )
    }
}

/**
 * Slim top bar (MP Manager style): active folder name + item count on the left,
 * a single overflow menu on the right. ~48dp tall instead of the old 2-row card.
 */
@Composable
private fun DualFilesTopControlBar(
    activePath: String,
    activeRootTitle: String,
    itemCount: Int,
    splitOrientation: SplitOrientation,
    transferCount: Int,
    isDarkTheme: Boolean,
    onQuickAddMedia: () -> Unit,
    onToggleSplitOrientation: () -> Unit,
    onOpenTransferLog: () -> Unit,
    onOpenStorageInsights: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onToggleTheme: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val folderName = File(activePath).name.ifBlank { activeRootTitle }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Transfer history keeps its badge because it is the "undo" entry point.
            IconButton(
                onClick = onOpenTransferLog,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("transfer_history_button")
            ) {
                BadgedBox(
                    badge = {
                        if (transferCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) { Text(transferCount.toString()) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Transfer history and undo",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_overflow_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Add photos & videos") },
                        leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        onClick = { menuOpen = false; onQuickAddMedia() },
                        modifier = Modifier.testTag("quick_add_media_button")
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (splitOrientation == SplitOrientation.HORIZONTAL) "Top / bottom panes"
                                else "Side-by-side panes"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (splitOrientation == SplitOrientation.HORIZONTAL) Icons.Default.ViewStream
                                else Icons.Default.VerticalSplit,
                                contentDescription = null
                            )
                        },
                        onClick = { menuOpen = false; onToggleSplitOrientation() },
                        modifier = Modifier.testTag("toggle_split_orientation_button")
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Recycle bin") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        onClick = { menuOpen = false; onOpenRecycleBin() },
                        modifier = Modifier.testTag("recycle_bin_button")
                    )
                    DropdownMenuItem(
                        text = { Text("Storage & theme") },
                        leadingIcon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                        onClick = { menuOpen = false; onOpenStorageInsights() },
                        modifier = Modifier.testTag("storage_insights_button")
                    )
                    DropdownMenuItem(
                        text = { Text(if (isDarkTheme) "Light theme" else "Dark theme") },
                        leadingIcon = {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null
                            )
                        },
                        onClick = { menuOpen = false; onToggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    )
                }
            }
        }
    }
}

/**
 * Bottom action bar (MP Manager style). Five equal-weight icons that act on the ACTIVE pane:
 * Up | Copy/Move to other pane | New (menu) | Mirror path | Swap panes.
 */
@Composable
private fun DualBottomActionBar(
    activeHasSelection: Boolean,
    activePaneTitle: String,
    onUp: () -> Unit,
    onTransfer: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onImportFiles: () -> Unit,
    onMirror: () -> Unit,
    onSwap: () -> Unit
) {
    var addMenuOpen by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarIcon(Icons.Default.ArrowUpward, "Go to parent folder in $activePaneTitle", "bottom_up", onClick = onUp)
            BottomBarIcon(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                description = "Copy or move to the other pane",
                tag = "bottom_transfer",
                tint = if (activeHasSelection) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onTransfer
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = { addMenuOpen = true }, modifier = Modifier.testTag("bottom_add")) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New file or folder in $activePaneTitle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = addMenuOpen, onDismissRequest = { addMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("New folder") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        onClick = { addMenuOpen = false; onNewFolder() }
                    )
                    DropdownMenuItem(
                        text = { Text("New empty file") },
                        leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                        onClick = { addMenuOpen = false; onNewFile() }
                    )
                    DropdownMenuItem(
                        text = { Text("Import files") },
                        leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                        onClick = { addMenuOpen = false; onImportFiles() }
                    )
                }
            }
            BottomBarIcon(Icons.Default.SyncAlt, "Mirror this folder to the other pane", "bridge_sync_dir", onClick = onMirror)
            BottomBarIcon(Icons.Default.SwapHoriz, "Swap panes", "swap_panes_button", onClick = onSwap)
        }
    }
}

@Composable
private fun RowScope.BottomBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tag: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
            Icon(imageVector = icon, contentDescription = description, tint = tint)
        }
    }
}

/**
 * Thin vertical divider between the two side-by-side panes. Keeps the "drag to resize"
 * feature of the old bridge, but costs ~1dp of layout width (the touch area is wider, 20dp,
 * and overlaps the panes via negative-free wrapper so it is still easy to grab).
 */
@Composable
private fun PaneSplitDividerVertical(
    onResetSplitRatio: () -> Unit,
    onDragSplitDelta: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(10.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragSplitDelta(dragAmount.x)
                }
            }
            .clickable(interactionSource = null, indication = null) { onResetSplitRatio() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun CenterTransferBridgeHorizontal(
    leftSelectedCount: Int,
    rightSelectedCount: Int,
    onTransferAToB: () -> Unit,
    onTransferBToA: () -> Unit,
    onMirrorActiveToOpposite: () -> Unit,
    onResetSplitRatio: () -> Unit,
    onDragSplitDelta: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BridgeChipButton(
                    icon = Icons.Default.ArrowDownward,
                    label = if (leftSelectedCount > 0) "Drop ($leftSelectedCount) to B" else "A → B",
                    highlighted = leftSelectedCount > 0,
                    onClick = onTransferAToB,
                    testTag = "bridge_a_to_b"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onResetSplitRatio() }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragSplitDelta(dragAmount.y)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to resize panes or tap to center",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                BridgeChipButton(
                    icon = Icons.Default.ArrowUpward,
                    label = if (rightSelectedCount > 0) "Drop ($rightSelectedCount) to A" else "B → A",
                    highlighted = rightSelectedCount > 0,
                    onClick = onTransferBToA,
                    testTag = "bridge_b_to_a"
                )

                BridgeChipButton(
                    icon = Icons.Default.SyncAlt,
                    label = "Sync Dir",
                    highlighted = false,
                    onClick = onMirrorActiveToOpposite,
                    testTag = "bridge_sync_dir"
                )
            }
        }
    }
}

@Composable
private fun BridgeChipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (highlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

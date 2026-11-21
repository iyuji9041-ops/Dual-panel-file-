package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.FilterChip
import com.example.model.FileCategory
import com.example.ui.theme.CategoryAudioTone
import com.example.ui.theme.CategoryPdfTone
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.data.FavoritesStore
import com.example.data.FileActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import coil.compose.AsyncImage
import com.example.model.DropActionMode
import com.example.model.FileItem
import com.example.model.PaneId
import com.example.model.PaneState
import com.example.model.SortOption
import com.example.model.StorageRoot
import com.example.model.ViewLayoutMode
import com.example.model.formatBytes
import com.example.ui.theme.CategoryArchiveTone
import com.example.ui.theme.CategoryCodeTone
import com.example.ui.theme.CategoryDataTone
import com.example.ui.theme.CategoryDocumentTone
import com.example.ui.theme.CategoryFolderTone
import com.example.ui.theme.CategoryMediaTone
import com.example.ui.theme.CategoryVideoTone
import java.io.File

fun fileCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.FOLDER -> Icons.Default.Folder
        FileCategory.IMAGE -> Icons.Default.Image
        FileCategory.VIDEO -> Icons.Default.Videocam
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.PDF -> Icons.Default.PictureAsPdf
        FileCategory.TEXT -> Icons.Default.TextSnippet
        FileCategory.APK -> Icons.Default.Android
        FileCategory.DOCUMENT -> Icons.Default.Description
        FileCategory.CODE -> Icons.Default.Code
        FileCategory.ARCHIVE -> Icons.Default.FolderZip
        FileCategory.DATA -> Icons.Default.TableChart
        FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

@Composable
fun fileCategoryTint(category: FileCategory): Color {
    return when (category) {
        FileCategory.FOLDER -> CategoryFolderTone
        FileCategory.IMAGE -> CategoryMediaTone
        FileCategory.VIDEO -> CategoryVideoTone
        FileCategory.AUDIO -> CategoryAudioTone
        FileCategory.PDF -> CategoryPdfTone
        FileCategory.TEXT -> CategoryDocumentTone
        FileCategory.APK -> CategoryCodeTone
        FileCategory.DOCUMENT -> CategoryDocumentTone
        FileCategory.CODE -> CategoryCodeTone
        FileCategory.ARCHIVE -> CategoryArchiveTone
        FileCategory.DATA -> CategoryDataTone
        FileCategory.OTHER -> MaterialTheme.colorScheme.secondary
    }
}

@Composable
fun FilePaneView(
    paneState: PaneState,
    isActive: Boolean,
    storageRoots: List<StorageRoot>,
    dragDropState: DualPaneDragDropState,
    onFocusPane: () -> Unit,
    onNavigateToDir: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSwitchRoot: (StorageRoot) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleViewMode: () -> Unit,
    onStartDragItem: (FileItem, Offset) -> Unit,
    onOpenItem: (FileItem) -> Unit,
    onOpenInPhoneGallery: (FileItem) -> Unit,
    onInspectDetails: (FileItem) -> Unit,
    onRequestCreateFolder: () -> Unit,
    onRequestCreateFile: () -> Unit,
    onRequestPickPhotosVideos: () -> Unit,
    onRequestImportFiles: () -> Unit,
    onRequestRenameItem: (FileItem) -> Unit,
    onRequestDeleteItems: (Collection<String>) -> Unit,
    onCategoryFilterChange: (FileCategory?) -> Unit,
    onRequestCompressItems: (List<String>) -> Unit,
    onRequestExtractZip: (FileItem, Boolean) -> Unit,
    onTransferSelectedToOpposite: (DropActionMode) -> Unit,
    onTransferSingleToOpposite: (FileItem, DropActionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val paneId = paneState.paneId
    val haptics = LocalHapticFeedback.current
    var showSearchField by remember { mutableStateOf(false) }
    var showRootDropdown by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var listOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    // Parent folder (shown as a drop row while dragging, so you can walk back OUT of a folder).
    val parentPath: String? = remember(paneState.currentPath, paneState.rootPath) {
        if (paneState.currentPath == paneState.rootPath) null else File(paneState.currentPath).parent
    }

    // Lets drag logic know which folder each pane is showing right now.
    SideEffect { dragDropState.registerPanePath(paneId, paneState.currentPath) }

    // AUTO-SCROLL: while dragging, scroll this pane when the finger nears its top/bottom edge.
    LaunchedEffect(dragDropState.isDragging, paneState.viewMode) {
        if (!dragDropState.isDragging) return@LaunchedEffect
        val edgePx = with(density) { 84.dp.toPx() }
        val maxStepPx = with(density) { 26.dp.toPx() }
        while (isActive && dragDropState.isDragging) {
            val area = dragDropState.listAreaOf(paneId)
            val p = dragDropState.pointerInRoot
            if (area != null && area.contains(p)) {
                val fromTop = p.y - area.top
                val fromBottom = area.bottom - p.y
                val dy = when {
                    fromTop < edgePx -> -maxStepPx * (1f - fromTop / edgePx).coerceIn(0f, 1f)
                    fromBottom < edgePx -> maxStepPx * (1f - fromBottom / edgePx).coerceIn(0f, 1f)
                    else -> 0f
                }
                if (dy != 0f) {
                    if (paneState.viewMode == ViewLayoutMode.LIST) listState.scrollBy(dy) else gridState.scrollBy(dy)
                }
            }
            // Content moved under a still finger -> re-evaluate which folder/pane is hovered.
            dragDropState.refreshHover()
            delay(16)
        }
    }

    // SPRING-LOADED FOLDERS: hover a folder ~0.75s while dragging and this pane opens it.
    // Keep holding to go deeper; release to drop into whatever folder is open.
    val hoveredFolderForSpring = dragDropState.hoveredFolder
    LaunchedEffect(hoveredFolderForSpring?.paneId, hoveredFolderForSpring?.folderPath) {
        val target = hoveredFolderForSpring ?: return@LaunchedEffect
        if (target.paneId != paneId) return@LaunchedEffect
        delay(750)
        if (dragDropState.isDragging && dragDropState.hoveredFolder?.folderPath == target.folderPath) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onNavigateToDir(target.folderPath)
            delay(150)
            dragDropState.refreshHover()
        }
    }

    val isDropTargetPane = dragDropState.isDragging &&
        dragDropState.hoveredPane == paneId &&
        dragDropState.canDropOnPane(paneId) &&
        dragDropState.hoveredFolder == null

    // MP Manager style: flat pane, no card border. Only a DROP TARGET gets an outline,
    // the active pane is shown by a 3dp underline under its header (like MP's pane highlight).
    val containerBgColor by animateColorAsState(
        targetValue = when {
            isDropTargetPane -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "paneBgColor"
    )

    Surface(
        modifier = modifier
            .testTag("pane_container_${paneId.name.lowercase()}")
            .onGloballyPositioned { coords ->
                dragDropState.registerPaneBounds(paneId, coords.boundsInRoot())
            }
            .then(
                if (isDropTargetPane) Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                else Modifier
            )
            .clickable(interactionSource = null, indication = null) {
                onFocusPane()
            },
        color = containerBgColor,
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Compact pane header (MP style): [storage v] [breadcrumb ........] [more]
            //    Search / sort / grid / add all live in ONE overflow menu so a half-width pane fits.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(start = 6.dp, end = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Storage root selector (icon only + chevron to save width)
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onFocusPane()
                                        showRootDropdown = true
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .testTag("root_selector_${paneId.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Select storage location (${paneState.rootTitle})",
                                    tint = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showRootDropdown,
                                onDismissRequest = { showRootDropdown = false }
                            ) {
                                storageRoots.forEach { root ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = root.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = root.subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (root.iconType.startsWith("storage")) {
                                                    Icons.Default.Storage
                                                } else {
                                                    Icons.Default.Folder
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {
                                            showRootDropdown = false
                                            onSwitchRoot(root)
                                        }
                                    )
                                }
                            }
                        }

                        // Breadcrumbs: horizontally scrollable, auto-scrolled to the END so the
                        // current folder is always visible even in a narrow half-screen pane.
                        val crumbScroll = rememberScrollState()
                        LaunchedEffect(paneState.breadcrumbs.size, paneState.currentPath) {
                            crumbScroll.scrollTo(crumbScroll.maxValue)
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(crumbScroll),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            paneState.breadcrumbs.forEachIndexed { index, crumb ->
                                val isLast = index == paneState.breadcrumbs.lastIndex
                                Text(
                                    text = crumb.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isLast) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onNavigateToDir(crumb.path) }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                                if (!isLast) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        // ONE overflow menu: Search, Sort, List/Grid, Add...
                        Box {
                            IconButton(
                                onClick = {
                                    onFocusPane()
                                    showAddMenu = true
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("add_item_button_${paneId.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Pane options for ${paneId.title}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (showSearchField) "Close search" else "Search") },
                                    leadingIcon = {
                                        Icon(
                                            if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        showSearchField = !showSearchField
                                        if (!showSearchField && paneState.searchQuery.isNotEmpty()) {
                                            onSearchQueryChange("")
                                        }
                                    },
                                    modifier = Modifier.testTag("search_toggle_${paneId.name.lowercase()}")
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (paneState.viewMode == ViewLayoutMode.LIST) "Grid view"
                                            else "List view"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (paneState.viewMode == ViewLayoutMode.LIST) Icons.Default.GridView
                                            else Icons.AutoMirrored.Filled.ViewList,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        onToggleViewMode()
                                    },
                                    modifier = Modifier.testTag("view_mode_${paneId.name.lowercase()}")
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by…") },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        showSortDropdown = true
                                    },
                                    modifier = Modifier.testTag("sort_button_${paneId.name.lowercase()}")
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Add photos & videos") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        onRequestPickPhotosVideos()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import files") },
                                    leadingIcon = {
                                        Icon(Icons.Default.FileOpen, contentDescription = null)
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        onRequestImportFiles()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New folder") },
                                    leadingIcon = {
                                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        onRequestCreateFolder()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New empty file") },
                                    leadingIcon = {
                                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        onRequestCreateFile()
                                    }
                                )
                            }

                            // Sort picker, anchored to the same overflow button
                            DropdownMenu(
                                expanded = showSortDropdown,
                                onDismissRequest = { showSortDropdown = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (paneState.sortOption == option) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            )
                                        },
                                        onClick = {
                                            showSortDropdown = false
                                            onSortOptionChange(option)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Expandable search field (only when toggled from the overflow menu)
                    AnimatedVisibility(
                        visible = showSearchField,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = paneState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text("Filter…", style = MaterialTheme.typography.bodySmall)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (paneState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("search_input_${paneId.name.lowercase()}")
                        )
                    }

                    // Active-pane indicator: 3dp underline (same idea as MP's PaneHighlightView)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                    )
                }
            }

            // 2. Multi-Selection Contextual Bar (Triggered via Long-Press or Checkbox tap)
            AnimatedVisibility(
                visible = paneState.selectedPaths.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onClearSelection,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear selection",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${paneState.selectedPaths.size} selected",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onSelectAll,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.SelectAll,
                                    contentDescription = "Select all in pane",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onTransferSelectedToOpposite(DropActionMode.MOVE) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("batch_move_${paneId.name.lowercase()}")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = "Move selected to other pane",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onTransferSelectedToOpposite(DropActionMode.COPY) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("batch_copy_${paneId.name.lowercase()}")
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy selected to other pane",
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRequestCompressItems(paneState.selectedPaths.toList()) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("batch_zip_${paneId.name.lowercase()}")
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = "Compress selected to ZIP",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { FileActions.share(context, paneState.selectedPaths) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("batch_share_${paneId.name.lowercase()}")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share selected files",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRequestDeleteItems(paneState.selectedPaths) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("batch_delete_${paneId.name.lowercase()}")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete selected items",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Drop Target Banner when dragging from the opposite pane
            AnimatedVisibility(
                visible = isDropTargetPane,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val folderName = File(paneState.currentPath).name.ifBlank { paneState.rootTitle }
                        Text(
                            text = "Release to Copy or Move into \"$folderName\"",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 4. File List or Grid Content
            CategoryFilterRow(
                selected = paneState.categoryFilter,
                onSelect = onCategoryFilterChange
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        listOriginInRoot = coords.positionInRoot()
                        dragDropState.registerListArea(paneId, coords.boundsInRoot())
                    }
                    // LONG-PRESS then MOVE = drag (any row/card). Lives on the pane container so it
                    // survives folder navigation while dragging (spring-loaded folders).
                    .paneLongPressDrag(
                        paneId = paneId,
                        dragDropState = dragDropState,
                        containerOriginInRoot = { listOriginInRoot },
                        onLongPressItem = { path ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!paneState.selectedPaths.contains(path)) onToggleSelection(path)
                        },
                        onDragStart = { path, rootPoint ->
                            paneState.items.firstOrNull { it.path == path }?.let { onStartDragItem(it, rootPoint) }
                        },
                        onDragFinished = { dropped ->
                            if (dropped) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
            ) {
                if (paneState.items.isEmpty() && dragDropState.isDragging) {
                    // Empty folder reached while dragging: still allow going back up or dropping here.
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (parentPath != null) {
                            ParentFolderDropRow(paneId, parentPath, dragDropState)
                        }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Release to drop into this folder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (paneState.items.isEmpty()) {
                    EmptyPaneState(
                        paneTitle = paneId.title,
                        onPickPhotosVideos = onRequestPickPhotosVideos,
                        onImportFiles = onRequestImportFiles,
                        onCreateFolder = onRequestCreateFolder,
                        onCreateFile = onRequestCreateFile
                    )
                } else if (paneState.viewMode == ViewLayoutMode.LIST) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("file_list_${paneId.name.lowercase()}"),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (dragDropState.isDragging && parentPath != null) {
                            item(key = "__parent_drop__") {
                                ParentFolderDropRow(paneId, parentPath, dragDropState)
                            }
                        }
                        items(
                            items = paneState.items,
                            key = { it.path }
                        ) { item ->
                            val isSelected = paneState.selectedPaths.contains(item.path)
                            val isHoveredFolder = dragDropState.hoveredFolder?.paneId == paneId &&
                                dragDropState.hoveredFolder?.folderPath == item.path

                            FileListRowItem(
                                paneId = paneId,
                                item = item,
                                isSelected = isSelected,
                                isHoveredAsDropFolder = isHoveredFolder,
                                dragDropState = dragDropState,
                                onTapItem = {
                                    onFocusPane()
                                    if (paneState.selectedPaths.isNotEmpty()) {
                                        onToggleSelection(item.path)
                                    } else if (item.isDirectory) {
                                        onNavigateToDir(item.path)
                                    } else {
                                        onOpenItem(item)
                                    }
                                },
                                onLongPressSelect = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleSelection(item.path)
                                },
                                onToggleSelect = { onToggleSelection(item.path) },
                                onStartDrag = { startRootOffset ->
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onStartDragItem(item, startRootOffset)
                                },
                                onMoveToOpposite = {
                                    onTransferSingleToOpposite(item, DropActionMode.MOVE)
                                },
                                onCopyToOpposite = {
                                    onTransferSingleToOpposite(item, DropActionMode.COPY)
                                },
                                onOpenInPhoneGallery = { onOpenInPhoneGallery(item) },
                                onInspect = { onInspectDetails(item) },
                                onRename = { onRequestRenameItem(item) },
                                onCompressZip = { onRequestCompressItems(listOf(item.path)) },
                                onExtractZipHere = { onRequestExtractZip(item, false) },
                                onExtractZipOpposite = { onRequestExtractZip(item, true) },
                                onDelete = { onRequestDeleteItems(listOf(item.path)) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("file_grid_${paneId.name.lowercase()}"),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (dragDropState.isDragging && parentPath != null) {
                            item(key = "__parent_drop__", span = { GridItemSpan(maxLineSpan) }) {
                                ParentFolderDropRow(paneId, parentPath, dragDropState)
                            }
                        }
                        items(
                            items = paneState.items,
                            key = { it.path }
                        ) { item ->
                            val isSelected = paneState.selectedPaths.contains(item.path)
                            val isHoveredFolder = dragDropState.hoveredFolder?.paneId == paneId &&
                                dragDropState.hoveredFolder?.folderPath == item.path

                            FileGridCardItem(
                                paneId = paneId,
                                item = item,
                                isSelected = isSelected,
                                isHoveredAsDropFolder = isHoveredFolder,
                                dragDropState = dragDropState,
                                onTapItem = {
                                    onFocusPane()
                                    if (paneState.selectedPaths.isNotEmpty()) {
                                        onToggleSelection(item.path)
                                    } else if (item.isDirectory) {
                                        onNavigateToDir(item.path)
                                    } else {
                                        onOpenItem(item)
                                    }
                                },
                                onLongPressSelect = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleSelection(item.path)
                                },
                                onToggleSelect = { onToggleSelection(item.path) },
                                onStartDrag = { startRootOffset ->
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onStartDragItem(item, startRootOffset)
                                }
                            )
                        }
                    }
                }
            }

            // 5. Frosted Matte Storage Footer Bar
            StorageFooterStrip(
                freeBytes = paneState.freeSpaceBytes,
                totalBytes = paneState.totalSpaceBytes,
                currentPath = paneState.currentPath
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListRowItem(
    paneId: PaneId,
    item: FileItem,
    isSelected: Boolean,
    isHoveredAsDropFolder: Boolean,
    dragDropState: DualPaneDragDropState,
    onTapItem: () -> Unit,
    onLongPressSelect: () -> Unit,
    onToggleSelect: () -> Unit,
    onStartDrag: (Offset) -> Unit,
    onMoveToOpposite: () -> Unit,
    onCopyToOpposite: () -> Unit,
    onOpenInPhoneGallery: () -> Unit,
    onInspect: () -> Unit,
    onRename: () -> Unit,
    onCompressZip: () -> Unit,
    onExtractZipHere: () -> Unit,
    onExtractZipOpposite: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var handlePositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var showItemMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var isFav by remember(item.path) { mutableStateOf(FavoritesStore.isFavorite(context, item.path)) }

    val isBeingDragged = dragDropState.activePayload?.items?.any { it.path == item.path } == true

    DisposableEffect(paneId, item.path, item.isDirectory) {
        onDispose {
            dragDropState.unregisterItemBounds(paneId, item.path)
            if (item.isDirectory) {
                dragDropState.unregisterFolderTarget(paneId, item.path)
            }
        }
    }

    // MP Manager row: flat (no Card / border), 4dp padding, 30dp icon.
    val rowColor by animateColorAsState(
        targetValue = when {
            isHoveredAsDropFolder -> MaterialTheme.colorScheme.primaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isBeingDragged -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        label = "rowColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_item_${item.name}")
            .onGloballyPositioned { coords ->
                dragDropState.registerItemBounds(paneId, item.path, coords.boundsInRoot())
                if (item.isDirectory) {
                    dragDropState.registerFolderTarget(
                        paneId = paneId,
                        folderPath = item.path,
                        folderName = item.name,
                        rect = coords.boundsInRoot()
                    )
                }
            }
            .background(rowColor)
            .combinedClickable(
                onClick = onTapItem
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Category Icon / Thumbnail / Selection Checkbox (tap = toggle selection)
            val categoryColor = fileCategoryTint(item.category)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else categoryColor.copy(alpha = 0.14f)
                    )
                    .clickable { onToggleSelect() }
                    .testTag("select_icon_${item.name}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected ${item.name}",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (item.isPhoto) {
                    AsyncImage(
                        model = File(item.path),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (item.isVideo) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Video ${item.name}",
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isHoveredAsDropFolder) {
                            Icons.Default.FolderOpen
                        } else {
                            fileCategoryIcon(item.category)
                        },
                        contentDescription = item.category.label,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // File Name (14sp bold) & one-line meta (10sp) - same sizes as MP's list_file.xml
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isHoveredAsDropFolder) "Drop inside" else "${item.formattedSize()} • ${item.formattedDate()}",
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (isHoveredAsDropFolder) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Item Context Menu Button
            Box {
                IconButton(
                    onClick = { showItemMenu = true },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("item_menu_${item.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions for ${item.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false }
                ) {
                    if (item.isPhoto || item.isVideo) {
                        DropdownMenuItem(
                            text = { Text(if (item.isVideo) "Play Video" else "View in Gallery") },
                            leadingIcon = {
                                Icon(
                                    if (item.isVideo) Icons.Default.PlayCircleFilled else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showItemMenu = false
                                onTapItem()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in Phone Gallery App") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            },
                            onClick = {
                                showItemMenu = false
                                onOpenInPhoneGallery()
                            }
                        )
                        HorizontalDivider()
                    }
                    if (!item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showItemMenu = false
                                FileActions.share(context, listOf(item.path))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open with…") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            },
                            onClick = {
                                showItemMenu = false
                                FileActions.openWith(context, item.path)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (isFav) "Remove from favorites" else "Add to favorites") },
                        leadingIcon = {
                            Icon(
                                if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            showItemMenu = false
                            isFav = FavoritesStore.toggle(context, item.path)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Move to ${paneId.opposite().title}") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                        },
                        onClick = {
                            showItemMenu = false
                            onMoveToOpposite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy to ${paneId.opposite().title}") },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                        },
                        onClick = {
                            showItemMenu = false
                            onCopyToOpposite()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (item.isDirectory) "Folder Properties" else "File Details & Checksum") },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
                        onClick = {
                            showItemMenu = false
                            onInspect()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                        },
                        onClick = {
                            showItemMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compress to ZIP") },
                        leadingIcon = {
                            Icon(Icons.Default.Archive, contentDescription = null)
                        },
                        onClick = {
                            showItemMenu = false
                            onCompressZip()
                        }
                    )
                    if (item.extension.equals("zip", ignoreCase = true)) {
                        DropdownMenuItem(
                            text = { Text("Extract ZIP Here") },
                            leadingIcon = {
                                Icon(Icons.Default.Unarchive, contentDescription = null)
                            },
                            onClick = {
                                showItemMenu = false
                                onExtractZipHere()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Extract ZIP to ${paneId.opposite().title}") },
                            leadingIcon = {
                                Icon(Icons.Default.Unarchive, contentDescription = null)
                            },
                            onClick = {
                                showItemMenu = false
                                onExtractZipOpposite()
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showItemMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridCardItem(
    paneId: PaneId,
    item: FileItem,
    isSelected: Boolean,
    isHoveredAsDropFolder: Boolean,
    dragDropState: DualPaneDragDropState,
    onTapItem: () -> Unit,
    onLongPressSelect: () -> Unit,
    onToggleSelect: () -> Unit,
    onStartDrag: (Offset) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var handlePositionInRoot by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(paneId, item.path, item.isDirectory) {
        onDispose {
            dragDropState.unregisterItemBounds(paneId, item.path)
            if (item.isDirectory) {
                dragDropState.unregisterFolderTarget(paneId, item.path)
            }
        }
    }

    val categoryColor = fileCategoryTint(item.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                dragDropState.registerItemBounds(paneId, item.path, coords.boundsInRoot())
                if (item.isDirectory) {
                    dragDropState.registerFolderTarget(
                        paneId = paneId,
                        folderPath = item.path,
                        folderName = item.name,
                        rect = coords.boundsInRoot()
                    )
                }
            }
            .border(
                width = if (isHoveredAsDropFolder || isSelected) 1.5.dp else 1.dp,
                color = when {
                    isHoveredAsDropFolder -> MaterialTheme.colorScheme.primary
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(15.dp)
            )
            .clip(RoundedCornerShape(15.dp))
            .combinedClickable(
                onClick = onTapItem
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isHoveredAsDropFolder -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (item.isPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = File(item.path),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else categoryColor.copy(alpha = 0.16f)
                            )
                            .clickable { onToggleSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) {
                                Icons.Default.CheckCircle
                            } else if (item.isVideo) {
                                Icons.Default.PlayCircleFilled
                            } else {
                                fileCategoryIcon(item.category)
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .onGloballyPositioned { coords ->
                                handlePositionInRoot = coords.positionInRoot()
                            }
                            .pointerInput(item.path) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onStartDrag(handlePositionInRoot + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDropState.updateDrag(dragAmount)
                                    },
                                    onDragEnd = {
                                        if (dragDropState.endDrag()) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onDragCancel = { dragDropState.cancelDrag() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Drag ${item.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.formattedSize(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPaneState(
    paneTitle: String,
    onPickPhotosVideos: () -> Unit,
    onImportFiles: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No files in this folder",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Drop files here from the other pane, or add real photos, videos, and files from your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onPickPhotosVideos() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Photos/Videos", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onImportFiles() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Files", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onCreateFolder() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("New Folder", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onCreateFile() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("New File", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StorageFooterStrip(
    freeBytes: Long,
    totalBytes: Long,
    currentPath: String
) {
    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedFraction = if (totalBytes > 0) {
        (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.98f)
    } else 0.18f

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                LinearProgressIndicator(
                    progress = { usedFraction },
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (totalBytes > 0) {
                    Text(
                        text = "${formatBytes(freeBytes)} free",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "/${File(currentPath).name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


/**
 * Shown at the top of the list while dragging inside a sub-folder. Hover it to go up one level,
 * or release on it to drop the items into the parent folder.
 */
@Composable
private fun ParentFolderDropRow(
    paneId: PaneId,
    parentPath: String,
    dragDropState: DualPaneDragDropState
) {
    val isHovered = dragDropState.hoveredFolder?.paneId == paneId &&
        dragDropState.hoveredFolder?.folderPath == parentPath

    DisposableEffect(paneId, parentPath) {
        onDispose { dragDropState.unregisterFolderTarget(paneId, parentPath) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                dragDropState.registerFolderTarget(
                    paneId = paneId,
                    folderPath = parentPath,
                    folderName = "parent folder",
                    rect = coords.boundsInRoot()
                )
            },
        shape = androidx.compose.ui.graphics.RectangleShape,
        color = if (isHovered) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Hold here to go up",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


/** Quick type filter: All / Photos / Videos / Audio / PDF / Documents / Text / ZIP / APK. */
@Composable
private fun CategoryFilterRow(
    selected: FileCategory?,
    onSelect: (FileCategory?) -> Unit
) {
    val options = listOf<FileCategory?>(
        null, FileCategory.IMAGE, FileCategory.VIDEO, FileCategory.AUDIO, FileCategory.PDF,
        FileCategory.DOCUMENT, FileCategory.TEXT, FileCategory.ARCHIVE, FileCategory.APK
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(options) { category ->
            FilterChip(
                modifier = Modifier.height(28.dp),
                selected = selected == category,
                onClick = { onSelect(category) },
                label = {
                    Text(
                        text = category?.label ?: "All",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

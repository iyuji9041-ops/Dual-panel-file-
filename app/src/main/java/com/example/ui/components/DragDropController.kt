package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.PaneId
import com.example.viewmodel.DragPayload
import kotlin.math.roundToInt

data class FolderDropTarget(
    val paneId: PaneId,
    val folderPath: String,
    val folderName: String,
    val boundsInRoot: Rect
)

@Stable
class DualPaneDragDropState(
    private val onExecuteDrop: (payload: DragPayload, targetPane: PaneId, targetFolderPath: String?) -> Unit
) {
    var activePayload by mutableStateOf<DragPayload?>(null)
        private set

    var pointerInRoot by mutableStateOf(Offset.Zero)
        private set

    var hoveredPane by mutableStateOf<PaneId?>(null)
        private set

    var hoveredFolder by mutableStateOf<FolderDropTarget?>(null)
        private set

    private val paneBounds = mutableStateMapOf<PaneId, Rect>()
    private val folderTargets = mutableStateMapOf<String, FolderDropTarget>()

    // Bounds of EVERY visible row/card (files + folders) so a pane-level long-press
    // gesture can find which item sits under the finger. Plain maps (not state) on purpose:
    // they change on every scroll frame and must not trigger recomposition.
    private val itemBounds = HashMap<String, Pair<String, Rect>>()
    private val listAreaBounds = HashMap<PaneId, Rect>()
    private val paneCurrentPaths = HashMap<PaneId, String>()

    val isDragging: Boolean
        get() = activePayload != null

    fun registerPaneBounds(paneId: PaneId, rect: Rect) {
        paneBounds[paneId] = rect
    }

    fun registerItemBounds(paneId: PaneId, path: String, rect: Rect) {
        itemBounds["${paneId.name}::$path"] = path to rect
    }

    fun unregisterItemBounds(paneId: PaneId, path: String) {
        itemBounds.remove("${paneId.name}::$path")
    }

    /** Path of the item under [rootPoint] inside [paneId], or null. */
    fun itemPathAt(paneId: PaneId, rootPoint: Offset): String? {
        val prefix = "${paneId.name}::"
        return itemBounds.entries
            .firstOrNull { it.key.startsWith(prefix) && it.value.second.contains(rootPoint) }
            ?.value?.first
    }

    fun registerListArea(paneId: PaneId, rect: Rect) {
        listAreaBounds[paneId] = rect
    }

    fun listAreaOf(paneId: PaneId): Rect? = listAreaBounds[paneId]

    fun registerPanePath(paneId: PaneId, path: String) {
        paneCurrentPaths[paneId] = path
    }

    /**
     * A drop on a pane's empty area is meaningful when it goes to the OTHER pane, or when the
     * same pane has been navigated (spring-loaded folders) away from the folder the items came from.
     */
    fun canDropOnPane(paneId: PaneId): Boolean {
        val payload = activePayload ?: return false
        if (paneId != payload.sourcePane) return true
        val sourceParent = java.io.File(payload.primaryItem.path).parent ?: return false
        val current = paneCurrentPaths[paneId] ?: return false
        return current != sourceParent
    }

    fun registerFolderTarget(paneId: PaneId, folderPath: String, folderName: String, rect: Rect) {
        folderTargets["${paneId.name}::$folderPath"] = FolderDropTarget(
            paneId = paneId,
            folderPath = folderPath,
            folderName = folderName,
            boundsInRoot = rect
        )
    }

    fun unregisterFolderTarget(paneId: PaneId, folderPath: String) {
        folderTargets.remove("${paneId.name}::$folderPath")
    }

    fun startDrag(payload: DragPayload, initialRootOffset: Offset) {
        activePayload = payload
        pointerInRoot = initialRootOffset
        evaluateHoverTargets(initialRootOffset)
    }

    fun updateDrag(delta: Offset) {
        if (activePayload == null) return
        val next = pointerInRoot + delta
        pointerInRoot = next
        evaluateHoverTargets(next)
    }

    /** Absolute pointer position in root coordinates (used by the pane-level gesture). */
    fun moveDragTo(rootPoint: Offset) {
        if (activePayload == null) return
        pointerInRoot = rootPoint
        evaluateHoverTargets(rootPoint)
    }

    /** Re-run hit-testing without moving the finger (after auto-scroll / folder open). */
    fun refreshHover() {
        if (activePayload == null) return
        evaluateHoverTargets(pointerInRoot)
    }

    fun endDrag(): Boolean {
        val payload = activePayload ?: return false
        val targetFolder = hoveredFolder
        val targetPane = hoveredPane

        var droppedSuccessfully = false
        if (targetFolder != null) {
            onExecuteDrop(payload, targetFolder.paneId, targetFolder.folderPath)
            droppedSuccessfully = true
        } else if (targetPane != null && canDropOnPane(targetPane)) {
            onExecuteDrop(payload, targetPane, null)
            droppedSuccessfully = true
        }

        cancelDrag()
        return droppedSuccessfully
    }

    fun cancelDrag() {
        activePayload = null
        hoveredPane = null
        hoveredFolder = null
    }

    private fun evaluateHoverTargets(point: Offset) {
        val payload = activePayload ?: return
        val draggedPaths = payload.items.map { it.path }.toSet()

        val matchedFolder = folderTargets.values.firstOrNull { target ->
            target.boundsInRoot.contains(point) && !draggedPaths.contains(target.folderPath)
        }
        hoveredFolder = matchedFolder

        val matchedPane = paneBounds.entries.firstOrNull { (_, rect) ->
            rect.contains(point)
        }?.key
        hoveredPane = matchedPane
    }
}

@Composable
fun rememberDualPaneDragDropState(
    onExecuteDrop: (payload: DragPayload, targetPane: PaneId, targetFolderPath: String?) -> Unit
): DualPaneDragDropState {
    return remember {
        DualPaneDragDropState(onExecuteDrop = onExecuteDrop)
    }
}

@Composable
fun FloatingDragPreviewOverlay(
    dragDropState: DualPaneDragDropState,
    leftPaneTitle: String,
    rightPaneTitle: String
) {
    val payload = dragDropState.activePayload
    val density = LocalDensity.current

    val scale by animateFloatAsState(
        targetValue = if (payload != null) 1.04f else 0.85f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "dragScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = payload != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            if (payload != null) {
                val offsetX = with(density) { (dragDropState.pointerInRoot.x - 105.dp.toPx()).roundToInt() }
                val offsetY = with(density) { (dragDropState.pointerInRoot.y - 80.dp.toPx()).roundToInt() }

                val folderTarget = dragDropState.hoveredFolder
                val paneTarget = dragDropState.hoveredPane
                val isValidDrop = folderTarget != null || (paneTarget != null && dragDropState.canDropOnPane(paneTarget))

                val destinationText = when {
                    folderTarget != null -> {
                        "Drop into \"${folderTarget.folderName}\" (Copy / Move)"
                    }
                    paneTarget != null && dragDropState.canDropOnPane(paneTarget) -> {
                        val paneName = if (paneTarget == PaneId.LEFT) leftPaneTitle else rightPaneTitle
                        "Drop in $paneName (Copy / Move)"
                    }
                    else -> "Hold over a folder to open it"
                }

                val containerColor = if (isValidDrop) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .widthIn(min = 210.dp, max = 300.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(18.dp),
                            clip = false
                        )
                        .border(
                            width = if (isValidDrop) 1.5.dp else 1.dp,
                            color = if (isValidDrop) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = containerColor,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (folderTarget != null) {
                                        Icons.Default.FolderOpen
                                    } else {
                                        fileCategoryIcon(payload.primaryItem.category)
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (payload.items.size > 1) {
                                        "${payload.primaryItem.name} +${payload.items.size - 1} more"
                                    } else {
                                        payload.primaryItem.name
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = payload.primaryItem.formattedSize(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (payload.items.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = payload.items.size.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .background(
                                    color = if (isValidDrop) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = destinationText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.PaneId

/**
 * Pane-level "long-press, then move" drag.
 *
 * WHY at pane level (not per row): while dragging we may open folders (spring-loaded folders),
 * which replaces the rows of the list. A per-row gesture would be cancelled the moment its row
 * leaves composition and the drop would never fire. This container survives navigation.
 *
 * Flow: finger down -> hold ~long-press timeout (no movement) -> item under finger is selected
 * -> move finger = drag starts (all selected items go along) -> release = drop.
 * Events are observed in the Initial pass so we see them before the row's click / list scroll.
 */
@Composable
fun Modifier.paneLongPressDrag(
    paneId: PaneId,
    dragDropState: DualPaneDragDropState,
    containerOriginInRoot: () -> Offset,
    onLongPressItem: (path: String) -> Unit,
    onDragStart: (path: String, rootPoint: Offset) -> Unit,
    onDragFinished: (dropped: Boolean) -> Unit
): Modifier {
    // The pointerInput block below is created once; these keep it pointed at the newest lambdas.
    val currentOrigin by rememberUpdatedState(containerOriginInRoot)
    val currentLongPress by rememberUpdatedState(onLongPressItem)
    val currentDragStart by rememberUpdatedState(onDragStart)
    val currentFinished by rememberUpdatedState(onDragFinished)

    return this.pointerInput(paneId, dragDropState) {
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val slop = viewConfiguration.touchSlop

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val startLocal = down.position

            // Phase 1: finger must stay down and still until the long-press timeout elapses.
            // withTimeoutOrNull returns null on timeout (= long press), non-null if released/moved early.
            val endedEarly = withTimeoutOrNull(longPressTimeout) {
                var abort = false
                while (!abort) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null ||
                        !change.pressed ||
                        (change.position - startLocal).getDistance() > slop
                    ) {
                        abort = true
                    }
                }
                true
            }
            if (endedEarly != null) return@awaitEachGesture // plain tap or scroll: leave to children

            val rootDown = currentOrigin() + startLocal
            val path = dragDropState.itemPathAt(paneId, rootDown) ?: return@awaitEachGesture
            currentLongPress(path)

            // Phase 2: long press happened. Swallow everything from now on so the row doesn't
            // register a click on release and the list doesn't scroll under the drag.
            var dragging = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) break

                    val rootPos = currentOrigin() + change.position
                    if (!dragging) {
                        if ((change.position - startLocal).getDistance() > slop) {
                            dragging = true
                            currentDragStart(path, rootPos)
                        }
                    } else {
                        dragDropState.moveDragTo(rootPos)
                    }
                }
                if (dragging) {
                    val dropped = dragDropState.endDrag()
                    currentFinished(dropped)
                }
            } finally {
                // Coroutine cancelled mid-drag (pane left composition etc.): never leave a ghost drag.
                if (dragDropState.isDragging) dragDropState.cancelDrag()
            }
        }
    }
}

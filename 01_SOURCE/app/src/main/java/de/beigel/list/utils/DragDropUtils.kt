package de.beigel.list.ui.utils

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Drag State für die Drag & Drop Funktionalität
 */
data class DragDropState(
    val draggedIndex: Int = -1,
    val dragOffset: Offset = Offset.Zero,
    val isDragging: Boolean = false
)

/**
 * Composable für Drag & Drop Liste
 */
@Composable
fun <T> rememberDragDropState(): MutableState<DragDropState> {
    return remember { mutableStateOf(DragDropState()) }
}

/**
 * Modifier für draggable Items
 */
fun Modifier.draggableItem(
    dragDropState: MutableState<DragDropState>,
    index: Int,
    onMove: (Int, Int) -> Unit
): Modifier {
    val dragState = dragDropState.value
    val isDragged = dragState.draggedIndex == index

    return this
        .zIndex(if (isDragged) 1f else 0f)
        .graphicsLayer {
            scaleX = if (isDragged) 1.05f else 1f
            scaleY = if (isDragged) 1.05f else 1f
            shadowElevation = if (isDragged) 8f else 0f
        }
        .offset {
            if (isDragged) {
                IntOffset(
                    dragState.dragOffset.x.roundToInt(),
                    dragState.dragOffset.y.roundToInt()
                )
            } else {
                IntOffset.Zero
            }
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    dragDropState.value = dragState.copy(
                        draggedIndex = index,
                        isDragging = true,
                        dragOffset = offset
                    )
                },
                onDragEnd = {
                    val finalState = dragDropState.value
                    if (finalState.isDragging) {
                        // Hier würde normalerweise die Zielposition berechnet werden
                        // Für Simplizität nehmen wir an, dass das onMove bereits aufgerufen wurde
                    }
                    dragDropState.value = DragDropState()
                },
                onDrag = { _, dragAmount ->
                    val currentState = dragDropState.value
                    dragDropState.value = currentState.copy(
                        dragOffset = currentState.dragOffset + dragAmount
                    )

                    // Vereinfachte Move-Logic
                    // In einer vollständigen Implementierung würde hier die Zielposition
                    // basierend auf der aktuellen Drag-Position berechnet werden
                }
            )
        }
}

/**
 * Animation Specifications für verschiedene UI Elemente
 */
object AnimationSpecs {
    const val FAST_ANIMATION_DURATION = 150
    const val MEDIUM_ANIMATION_DURATION = 300
    const val SLOW_ANIMATION_DURATION = 500

    const val SPRING_DAMPING_RATIO = 0.8f
    const val SPRING_STIFFNESS = 300f
}

/**
 * Reorder Logic für Listen
 */
fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this

    val mutableList = this.toMutableList()
    val item = mutableList.removeAt(from)
    mutableList.add(to, item)
    return mutableList
}

/**
 * Animation Helper für List Item Changes
 */
@Composable
fun <T> animatedItemKey(item: T, keySelector: (T) -> Any): Any {
    return keySelector(item)
}

/**
 * Haptic Feedback für Drag Operations
 */
fun performDragHapticFeedback() {
    // Hier könnte HapticFeedback implementiert werden
    // z.B. mit LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Calculate drop position based on drag offset
 */
fun calculateDropPosition(
    dragOffset: Offset,
    itemHeight: Float,
    listSize: Int
): Int {
    val draggedOverIndex = (dragOffset.y / itemHeight).roundToInt()
    return draggedOverIndex.coerceIn(0, listSize - 1)
}
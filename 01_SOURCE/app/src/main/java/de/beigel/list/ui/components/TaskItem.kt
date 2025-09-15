package de.beigel.list.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity

@Composable
fun MinimalTaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onLongPress: () -> Unit,
    isUpdating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isUpdating) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isUpdating) 0.6f else 1f,
        animationSpec = tween(150),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .pointerInput(isUpdating) {
                if (!isUpdating) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleComplete()
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        },
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                }
            }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed && !isUpdating) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isUpdating -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator (vertikaler Balken)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        Color(task.priority.color).copy(alpha = if (isUpdating) 0.5f else 1f),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Animated Checkbox with updating state
            AnimatedCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { if (!isUpdating) onToggleComplete() },
                isUpdating = isUpdating
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Task Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        isUpdating -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                AnimatedVisibility(
                    visible = task.description.isNotBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isUpdating) 0.3f else 0.7f
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Metadata Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority Chip
                        Surface(
                            color = Color(task.priority.color).copy(
                                alpha = if (isUpdating) 0.05f else 0.1f
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = task.priority.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(task.priority.color).copy(
                                    alpha = if (isUpdating) 0.5f else 1f
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Location Indicator
                        if (!task.isInDailyList) {
                            Surface(
                                color = MaterialTheme.colorScheme.outline.copy(
                                    alpha = if (isUpdating) 0.05f else 0.1f
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Backlog",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline.copy(
                                        alpha = if (isUpdating) 0.3f else 1f
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Loading indicator or More icon
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "⋮",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isUpdating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_animation"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isUpdating -> 0.9f
            checked -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_scale"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .pointerInput(isUpdating) {
                if (!isUpdating) {
                    detectTapGestures(
                        onTap = { onCheckedChange(!checked) }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Background Circle
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val color = when {
                isUpdating -> Color(0xFF009966).copy(alpha = 0.4f)
                checked -> Color(0xFF009966)
                else -> Color(0xFF009966).copy(alpha = 0.3f)
            }

            drawCircle(
                color = color,
                radius = size.minDimension / 2
            )
        }

        // Checkmark with updating state
        AnimatedVisibility(
            visible = checked && !isUpdating,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Loading indicator when updating
        AnimatedVisibility(
            visible = isUpdating,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = Color.White
            )
        }
    }
}
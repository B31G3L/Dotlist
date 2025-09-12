package de.beigel.list.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Animation specifications
object AnimationSpecs {
    val FastEasing = FastOutSlowInEasing
    val BounceEasing = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
    val ElasticEasing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

    val QuickSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val GentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}

// Task completion celebration animation
@Composable
fun TaskCompletionCelebration(
    isVisible: Boolean,
    onAnimationEnd: () -> Unit = {}
) {
    var particles by remember { mutableStateOf(emptyList<Particle>()) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            particles = List(20) {
                Particle(
                    x = Random.nextFloat() * 400,
                    y = Random.nextFloat() * 400,
                    velocityX = Random.nextFloat() * 400 - 200,
                    velocityY = Random.nextFloat() * 400 - 200,
                    color = listOf(
                        Color(0xFF4CAF50),
                        Color(0xFF2196F3),
                        Color(0xFFFF9800),
                        Color(0xFFE91E63)
                    ).random(),
                    size = Random.nextFloat() * 8 + 4
                )
            }
            delay(2000)
            onAnimationEnd()
        }
    }

    if (isVisible) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Celebration particles
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                particles.forEach { particle ->
                    drawCircle(
                        color = particle.color,
                        radius = particle.size,
                        center = androidx.compose.ui.geometry.Offset(particle.x, particle.y)
                    )
                }
            }

            // Success icon with bounce
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

data class Particle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float
)

// Smooth list item animations
@Composable
fun AnimatedListItem(
    visible: Boolean,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delay,
                easing = AnimationSpecs.FastEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delay
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutLinearInEasing
            )
        ) + fadeOut(
            animationSpec = tween(200)
        )
    ) {
        content()
    }
}

// Pulsing animation for important elements
@Composable
fun PulsingIcon(
    modifier: Modifier = Modifier,
    scale: Float = 1.1f,
    duration: Int = 1000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleValue by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = scale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = modifier.scale(scaleValue)
    ) {
        content()
    }
}

// Floating animation for FAB
@Composable
fun FloatingAnimation(
    modifier: Modifier = Modifier,
    offsetY: Float = 8f,
    duration: Int = 2000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = offsetY,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationY = offsetValue
        }
    ) {
        content()
    }
}

// Shimmer loading animation
@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color.Gray.copy(alpha = 0.3f),
        Color.Gray.copy(alpha = 0.5f),
        Color.Gray.copy(alpha = 0.3f)
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateX by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "translateX"
    )

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .graphicsLayer {
                this.translationX = translateX
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = colors,
                startX = 0f,
                endX = size.width
            )
            drawRect(brush = gradient)
        }
    }
}

// Bouncy button press animation
@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AnimationSpecs.QuickSpring, label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
    ) {
        Button(
            onClick = {
                isPressed = true
                onClick()
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

// Progress indicator with smooth animation
@Composable
fun AnimatedProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = AnimationSpecs.FastEasing
        ), label = "progress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier,
        color = color,
        trackColor = backgroundColor
    )
}

// Number counting animation
@Composable
fun CountingNumber(
    targetValue: Int,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    var currentValue by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetValue) {
        val duration = 1000
        val steps = 50
        val stepDuration = duration / steps
        val stepValue = (targetValue - currentValue).toFloat() / steps

        repeat(steps) {
            delay(stepDuration.toLong())
            currentValue = (currentValue + stepValue).toInt().coerceAtMost(targetValue)
        }
        currentValue = targetValue
    }

    Text(
        text = currentValue.toString(),
        style = textStyle,
        modifier = modifier
    )
}
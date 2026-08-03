package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Vector Icon Entrance Animation states
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.2f) }
    val arrowDrop = remember { Animatable(-35f) } // Drop down from top
    val trayScale = remember { Animatable(0f) }   // Tray expands horizontally

    // DL Animation state (First text)
    val dlAlpha = remember { Animatable(0f) }
    val dlScale = remember { Animatable(0.4f) }

    // TOOL Animation state (Second text)
    val toolAlpha = remember { Animatable(0f) }
    val toolScale = remember { Animatable(0.4f) }

    // Subtitle Animation state (Third text)
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(30f) }

    // Progress indicator alpha (Fourth)
    val progressAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 0: Vector Icon Entrance Animation
        // 0a. Badge scale & alpha pop
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.58f, stiffness = 280f)
            )
        }
        launch {
            iconAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
        // 0b. Tray expands horizontally
        launch {
            delay(100)
            trayScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f)
            )
        }
        // 0c. Download arrow drops down into tray with a soft bounce
        launch {
            delay(120)
            arrowDrop.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f)
            )
        }

        delay(350)

        // Step 1: Animate "DL" in Blue
        launch {
            dlScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f)
            )
        }
        launch {
            dlAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }

        delay(220)

        // Step 2: Animate "TOOL" in White
        launch {
            toolScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f)
            )
        }
        launch {
            toolAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }

        delay(260)

        // Step 3: Animate "download video apps" from below
        launch {
            subtitleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)
            )
        }
        launch {
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350)
            )
        }

        delay(220)

        // Step 4: Show subtle loading indicator
        launch {
            progressAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350)
            )
        }

        delay(1000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Vector Icon
            AnimatedVectorAppIcon(
                iconScale = iconScale.value,
                iconAlpha = iconAlpha.value,
                arrowDropOffset = arrowDrop.value,
                trayWidthFactor = trayScale.value
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Title Row: DL (Blue) + TOOL (White)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DL",
                    color = StatusBlue, // Vibrant Blue (#3B82F6)
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = dlAlpha.value
                        scaleX = dlScale.value
                        scaleY = dlScale.value
                    }
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "TOOL",
                    color = TextWhite, // Pure White (#FFFFFF)
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = toolAlpha.value
                        scaleX = toolScale.value
                        scaleY = toolScale.value
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle: "download video apps"
            Text(
                text = "download video apps",
                color = TextGray,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = subtitleAlpha.value
                    translationY = subtitleOffsetY.value
                }
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Subtle loading indicator at bottom
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = progressAlpha.value },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = StatusBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedVectorAppIcon(
    iconScale: Float,
    iconAlpha: Float,
    arrowDropOffset: Float,
    trayWidthFactor: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VectorIconAnim")

    // Continuous smooth bouncing/dropping arrow animation after entrance
    val arrowBounce by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ArrowBounce"
    )

    // Pulsing outer aura
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    Box(
        modifier = modifier
            .size(110.dp)
            .graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
                alpha = iconAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPx = size.width / 2f
            val radiusPx = size.width / 2f

            // 1. Outer Glow Circle (Pulsing)
            drawCircle(
                color = StatusBlue.copy(alpha = 0.15f),
                radius = radiusPx * 0.95f * glowScale,
                center = center
            )

            // 2. Dark Circular Badge Base (#131925 - matching app dark bg)
            drawCircle(
                color = Color(0xFF131925),
                radius = radiusPx * 0.72f,
                center = center
            )

            // 3. Inner Accent Border Ring
            drawCircle(
                color = StatusBlue.copy(alpha = 0.35f),
                radius = radiusPx * 0.72f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 4. Tray Base Line (White Accent) expanding horizontally
            val trayY = centerPx + 17.dp.toPx()
            val fullTrayWidth = 30.dp.toPx()
            val currentTrayWidth = fullTrayWidth * trayWidthFactor.coerceIn(0f, 1f)
            if (currentTrayWidth > 0f) {
                drawLine(
                    color = Color.White,
                    start = Offset(centerPx - currentTrayWidth / 2f, trayY),
                    end = Offset(centerPx + currentTrayWidth / 2f, trayY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. Animated Download Arrow (Blue #3B82F6) dropping down into place + bouncing
            val totalArrowOffsetY = arrowDropOffset.dp.toPx() + arrowBounce.dp.toPx()
            val arrowCenterY = centerPx - 4.dp.toPx() + totalArrowOffsetY

            val arrowPath = Path().apply {
                // Top vertical bar stem
                moveTo(centerPx - 3.5.dp.toPx(), arrowCenterY - 14.dp.toPx())
                lineTo(centerPx + 3.5.dp.toPx(), arrowCenterY - 14.dp.toPx())
                lineTo(centerPx + 3.5.dp.toPx(), arrowCenterY)
                // Right arrowhead wing
                lineTo(centerPx + 11.dp.toPx(), arrowCenterY)
                // Tip pointing down
                lineTo(centerPx, arrowCenterY + 12.dp.toPx())
                // Left arrowhead wing
                lineTo(centerPx - 11.dp.toPx(), arrowCenterY)
                // Left stem connect
                lineTo(centerPx - 3.5.dp.toPx(), arrowCenterY)
                close()
            }

            drawPath(
                path = arrowPath,
                color = StatusBlue
            )
        }
    }
}

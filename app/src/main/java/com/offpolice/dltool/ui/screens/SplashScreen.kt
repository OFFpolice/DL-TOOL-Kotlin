package com.offpolice.dltool.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offpolice.dltool.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val dlAlpha = remember { Animatable(0f) }
    val dlScale = remember { Animatable(0.4f) }

    val toolAlpha = remember { Animatable(0f) }
    val toolScale = remember { Animatable(0.4f) }

    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(30f) }

    val progressAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            dlScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f)
            )
        }
        launch {
            dlAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        delay(300)

        launch {
            toolScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f)
            )
        }
        launch {
            toolAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        delay(350)

        launch {
            subtitleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)
            )
        }
        launch {
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
        }

        delay(300)

        launch {
            progressAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
        }

        delay(1100)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DL",
                    color = StatusBlue,
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
                    color = TextWhite,
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

            Spacer(modifier = Modifier.height(14.dp))

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

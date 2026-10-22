package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.AppStrings
import com.example.ui.animation.IosEase
import com.example.ui.animation.IosSpring
import com.example.ui.animation.areAnimationsEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    language: String,
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }

    val logoScale = remember { Animatable(0.4f) }
    val logoRotation = remember { Animatable(-120f) }
    val logoAlpha = remember { Animatable(0f) }

    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(16f) }

    val tagAlpha = remember { Animatable(0f) }
    val tagOffsetY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        if (!animsEnabled) {
            logoScale.snapTo(1f)
            logoRotation.snapTo(0f)
            logoAlpha.snapTo(1f)
            titleAlpha.snapTo(1f)
            titleOffsetY.snapTo(0f)
            tagAlpha.snapTo(1f)
            tagOffsetY.snapTo(0f)
            delay(1000)
            onNavigateNext()
            return@LaunchedEffect
        }

        // 1. Logo pop: scale 0.4 to 1, rotation -120 to 0, opacity 0 to 1 over first 50% (total 1100 ms)
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(550))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = IosSpring)
        }
        launch {
            logoRotation.animateTo(0f, animationSpec = IosSpring)
        }

        // 2. App name slides up 16px and fades in over 800 ms starting 500 ms after start
        launch {
            delay(500)
            launch {
                titleAlpha.animateTo(1f, animationSpec = tween(800, easing = IosEase))
            }
            launch {
                titleOffsetY.animateTo(0f, animationSpec = tween(800, easing = IosEase))
            }
        }

        // 3. Tagline slides up 16px and fades in over 800 ms starting 700 ms after start
        launch {
            delay(700)
            launch {
                tagAlpha.animateTo(1f, animationSpec = tween(800, easing = IosEase))
            }
            launch {
                tagOffsetY.animateTo(0f, animationSpec = tween(800, easing = IosEase))
            }
        }

        // After about 2.4s navigate to next screen
        delay(2400)
        onNavigateNext()
    }

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }

    // Radial gradient background from #2b2d3d (center, 38% height) to #0d0e12
    val radialBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF2B2D3D), Color(0xFF0D0E12)),
        center = Offset(screenWidthPx / 2f, screenHeightPx * 0.38f),
        radius = screenHeightPx * 0.65f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(radialBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lens_logo),
                contentDescription = "Lens Logo",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = logoRotation.value
                        alpha = logoAlpha.value
                    }
            )

            Text(
                text = AppStrings.t("app", language),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer {
                    translationY = titleOffsetY.value * density.density
                    alpha = titleAlpha.value
                }
            )

            Text(
                text = AppStrings.t("tag", language),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.graphicsLayer {
                    translationY = tagOffsetY.value * density.density
                    alpha = tagAlpha.value
                }
            )
        }
    }
}

package com.example.ui.animation

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Standard iOS ease curve matching cubic-bezier(0.32, 0.72, 0, 1) from lens-camera.html
 */
val IosEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * Standard iOS spring spec with overshoot matching cubic-bezier(0.34, 1.56, 0.64, 1)
 */
val IosSpring: AnimationSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 380f)

/**
 * Material 3 standard ease curve matching cubic-bezier(0.2, 0, 0, 1)
 */
val MaterialEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Checks system "Remove animations" accessibility setting.
 */
fun areAnimationsEnabled(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        scale > 0f
    } catch (_: Exception) {
        true
    }
}

/**
 * Spring press bounce modifier scaling down on press (e.g. to 0.92f or 0.85f).
 * Respects iOS look vs Material look and system animations enabled setting.
 */
@Composable
fun Modifier.pressScale(
    targetScale: Float = 0.92f,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
): Modifier {
    val context = LocalContext.current
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1.0f,
        animationSpec = if (!animsEnabled) snap() else IosSpring,
        label = "pressScale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.animation.IosEase
import com.example.ui.animation.IosSpring
import com.example.ui.animation.MaterialEase
import com.example.ui.animation.areAnimationsEnabled
import com.example.ui.animation.pressScale
import com.example.ui.theme.LensTheme
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Separate background layer for glass surfaces.
 * Blur and translucent color live ONLY here so icons and text remain 100% sharp.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    hazeState: HazeState? = LocalHazeState.current,
    backgroundColor: Color = LensTheme.tokens.glass,
    fallbackColor: Color = LensTheme.tokens.fallbackColor,
    topBorderColor: Color? = null,
    borderWidth: Dp = 0.5.dp
) {
    val isBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val finalBg = if (isBlurSupported) backgroundColor else fallbackColor

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (isBlurSupported && hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = backgroundColor,
                            tints = emptyList(),
                            blurRadius = 24.dp
                        )
                    )
                } else {
                    Modifier.background(finalBg)
                }
            )
            .then(
                if (topBorderColor != null) {
                    Modifier.drawBehind {
                        val strokeWidthPx = borderWidth.toPx()
                        drawLine(
                            color = topBorderColor,
                            start = Offset(0f, strokeWidthPx / 2f),
                            end = Offset(size.width, strokeWidthPx / 2f),
                            strokeWidth = strokeWidthPx
                        )
                    }
                } else {
                    Modifier
                }
            )
    )
}

/**
 * Shared GlassSurface composable used across the app.
 * Structure guarantees crisp content:
 * Box {
 *   GlassBackground(Modifier.matchParentSize())  // blur + translucent color live here only
 *   Content()                                     // icons and text, drawn on top, sharp, no blur modifier
 * }
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    hazeState: HazeState? = LocalHazeState.current,
    backgroundColor: Color = LensTheme.tokens.glass,
    fallbackColor: Color = LensTheme.tokens.fallbackColor,
    topBorderColor: Color? = null,
    borderWidth: Dp = 0.5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        GlassBackground(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            hazeState = hazeState,
            backgroundColor = backgroundColor,
            fallbackColor = fallbackColor,
            topBorderColor = topBorderColor,
            borderWidth = borderWidth
        )
        content()
    }
}

/**
 * Segmented Control matching iOS / Material style with sliding indicator (Animation 14).
 */
@Composable
fun LensSegmentedControl(
    options: List<Pair<String, String>>, // (key, label)
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }
    val tokens = LensTheme.tokens
    val isIos = tokens.isIos

    val containerShape = if (isIos) RoundedCornerShape(9.dp) else RoundedCornerShape(20.dp)
    val itemShape = if (isIos) RoundedCornerShape(7.dp) else RoundedCornerShape(18.dp)
    val padding = if (isIos) 2.dp else 3.dp

    val selectedIndex = remember(options, selectedKey) {
        val idx = options.indexOfFirst { it.first == selectedKey }
        if (idx >= 0) idx else 0
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (!animsEnabled) snap() else IosSpring,
        label = "segmentedIndicator"
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(containerShape)
            .background(tokens.fill)
            .padding(padding)
    ) {
        val count = options.size.coerceAtLeast(1)
        val itemWidth = maxWidth / count

        // Sliding active background indicator
        Box(
            modifier = Modifier
                .offset(x = itemWidth * animatedIndex)
                .width(itemWidth)
                .height(34.dp)
                .clip(itemShape)
                .then(
                    if (isIos) {
                        Modifier.shadow(2.dp, itemShape, spotColor = Color(0x33000000))
                    } else {
                        Modifier
                    }
                )
                .background(tokens.segmentActive)
        )

        // Option text buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, (key, label) ->
                val isSelected = key == selectedKey
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .pressScale(0.96f, enabled = isIos, interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onOptionSelected(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) tokens.text else tokens.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Pill toggle switch (51x31dp with 27dp knob) matching prototype (Animation 7).
 * Knob moves with IosSpring (overshoot); track cross-fades in 300 ms.
 */
@Composable
fun LensSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }
    val tokens = LensTheme.tokens
    val isIos = tokens.isIos
    val activeColor = tokens.switchActive
    val inactiveColor = tokens.fill

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = if (!animsEnabled) snap() else IosSpring,
        label = "switchThumb"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = if (!animsEnabled) snap() else tween(300),
        label = "switchTrack"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .pressScale(0.94f, enabled = isIos, interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(27.dp)
                .shadow(3.dp, CircleShape, spotColor = Color(0x40000000))
                .background(Color.White, CircleShape)
        )
    }
}

/**
 * Universal confirmation dialog (Animation 6) using GlassSurface structure.
 * iOS: bottom sheet sliding from 120% to 0 in 550 ms with IosEase over 300 ms scrim fade.
 * Material: centered dialog scaling from 0.85 to 1 with fade (300 ms).
 */
@Composable
fun LensDialog(
    visible: Boolean,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }
    val tokens = LensTheme.tokens
    val isIos = tokens.isIos

    val scrimAlpha = remember { Animatable(0f) }
    val sheetOffsetY = remember { Animatable(120f) }
    val materialScale = remember { Animatable(0.85f) }
    val materialAlpha = remember { Animatable(0f) }

    LaunchedEffect(visible, isIos) {
        if (isIos) {
            scrimAlpha.snapTo(0f)
            sheetOffsetY.snapTo(120f)
            scrimAlpha.animateTo(1f, if (!animsEnabled) snap() else tween(300))
            sheetOffsetY.animateTo(0f, if (!animsEnabled) snap() else tween(550, easing = IosEase))
        } else {
            scrimAlpha.snapTo(0f)
            materialScale.snapTo(0.85f)
            materialAlpha.snapTo(0f)
            scrimAlpha.animateTo(1f, if (!animsEnabled) snap() else tween(300))
            materialScale.animateTo(1f, if (!animsEnabled) snap() else tween(300, easing = MaterialEase))
            materialAlpha.animateTo(1f, if (!animsEnabled) snap() else tween(300))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !isIos)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * scrimAlpha.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = if (isIos) Alignment.BottomCenter else Alignment.Center
        ) {
            if (isIos) {
                // iOS bottom sheet: uses GlassSurface with separate background
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                        .graphicsLayer {
                            translationY = sheetOffsetY.value * 5f
                        },
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = tokens.surface,
                    fallbackColor = tokens.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* consume */ }
                            .padding(top = 22.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = message,
                            fontSize = 16.sp,
                            color = tokens.text,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(tokens.line)
                                .padding(top = 14.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .pressScale(0.94f, enabled = true)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cancelText,
                                    fontSize = 17.sp,
                                    color = tokens.accent
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(52.dp)
                                    .background(tokens.line)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .pressScale(0.94f, enabled = true)
                                    .clickable {
                                        onConfirm()
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = confirmText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF3B30)
                                )
                            }
                        }
                    }
                }
            } else {
                // Material 3 centered modal: scales from 0.85 to 1 with fade (300 ms)
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .wrapContentHeight()
                        .graphicsLayer {
                            scaleX = materialScale.value
                            scaleY = materialScale.value
                            alpha = materialAlpha.value
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* consume */ },
                    shape = RoundedCornerShape(28.dp),
                    color = tokens.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = message,
                            fontSize = 16.sp,
                            color = tokens.text,
                            lineHeight = 24.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable { onDismiss() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = cancelText,
                                    color = tokens.accent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable {
                                        onConfirm()
                                        onDismiss()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = confirmText,
                                    color = Color(0xFFFF3B30),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

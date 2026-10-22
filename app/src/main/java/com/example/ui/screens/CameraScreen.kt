package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.filter.ImageFilter
import com.example.localization.AppStrings
import com.example.ui.CameraViewModel
import com.example.ui.animation.IosEase
import com.example.ui.animation.IosSpring
import com.example.ui.animation.areAnimationsEnabled
import com.example.ui.animation.pressScale
import com.example.ui.components.GlassSurface
import com.example.ui.theme.LensTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateGallery: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tokens = LensTheme.tokens
    val isIos = tokens.isIos
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }
    val density = LocalDensity.current

    val settings by viewModel.settings.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val facing by viewModel.facing.collectAsState()
    val flashEnabled by viewModel.flashEnabled.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val isFilterStripVisible by viewModel.isFilterStripVisible.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val showScreenFlash by viewModel.showScreenFlash.collectAsState()

    val language = settings.language
    val cameraHighlight = tokens.cameraHighlight

    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Flip camera rotation angle (Animation 9)
    var flipAngle by remember { mutableFloatStateOf(0f) }
    val animatedFlipAngle by animateFloatAsState(
        targetValue = flipAngle,
        animationSpec = if (!animsEnabled) androidx.compose.animation.core.snap() else IosSpring,
        label = "flipCameraRotation"
    )

    // Capture flash overlay (Animation 8)
    val flashOverlayAlpha = remember { Animatable(0f) }
    LaunchedEffect(showScreenFlash) {
        if (showScreenFlash) {
            flashOverlayAlpha.snapTo(0.9f)
            flashOverlayAlpha.animateTo(0f, animationSpec = tween(400))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Bind Camera lifecycle
    LaunchedEffect(facing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = if (facing == "front") {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView?.surfaceProvider
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
                cameraControl = camera.cameraControl
                imageCapture = capture

                camera.cameraControl.setZoomRatio(zoomRatio)
                if (facing == "back") {
                    camera.cameraControl.enableTorch(flashEnabled)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Sync torch with flashEnabled
    LaunchedEffect(flashEnabled, facing) {
        if (facing == "back") {
            try {
                cameraControl?.enableTorch(flashEnabled)
            } catch (_: Exception) {}
        }
    }

    // Sync camera zoom
    LaunchedEffect(zoomRatio) {
        try {
            cameraControl?.setZoomRatio(zoomRatio)
        } catch (_: Exception) {}
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val containerWidthPx = constraints.maxWidth
        val containerHeightPx = constraints.maxHeight

        // Fullscreen Live Camera Preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val newZoom = (zoomRatio * zoomChange).coerceIn(1.0f, 4.0f)
                        viewModel.setZoom(newZoom)
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        previewView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Apply live ColorMatrix filter to preview
                        if (currentFilter != ImageFilter.ORIGINAL) {
                            try {
                                renderEffect = android.graphics.RenderEffect.createColorFilterEffect(
                                    android.graphics.ColorMatrixColorFilter(currentFilter.androidMatrix)
                                ).asComposeRenderEffect()
                            } catch (_: Exception) {}
                        }
                    }
            )

            // Rule of thirds grid overlay
            if (settings.gridEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gridColor = Color.White.copy(alpha = 0.45f)
                    val strokeW = 1.dp.toPx()

                    drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = strokeW)
                }
            }
        }

        // Countdown Display Overlay (Animation 8)
        countdown?.let { count ->
            val countScale = remember { Animatable(1.6f) }
            val countAlpha = remember { Animatable(0f) }

            LaunchedEffect(count) {
                countScale.snapTo(1.6f)
                countAlpha.snapTo(0f)
                launch {
                    countAlpha.animateTo(1f, animationSpec = tween(300))
                }
                launch {
                    countScale.animateTo(1.0f, animationSpec = tween(1000, easing = IosEase))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.cancelCountdown() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 130.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = countScale.value
                        scaleY = countScale.value
                        alpha = countAlpha.value
                    }
                )
            }
        }

        // White screen flash overlay (Animation 8)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = flashOverlayAlpha.value
                }
                .background(Color.White)
        )

        // Top Glass Row: Flash, Timer, Grid, Filters, Settings using GlassSurface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraTopButton(
                iconRes = if (flashEnabled) R.drawable.ic_cam_flash else R.drawable.ic_cam_flashoff,
                isActive = flashEnabled,
                activeColor = cameraHighlight,
                isIos = isIos,
                onClick = { viewModel.toggleFlash() }
            )

            CameraTopButton(
                iconRes = R.drawable.ic_cam_timer,
                isActive = timerSeconds > 0,
                activeColor = cameraHighlight,
                isIos = isIos,
                label = if (timerSeconds > 0) "${timerSeconds}s" else null,
                onClick = { viewModel.cycleTimer() }
            )

            CameraTopButton(
                iconRes = R.drawable.ic_cam_grid,
                isActive = settings.gridEnabled,
                activeColor = cameraHighlight,
                isIos = isIos,
                onClick = { viewModel.toggleGrid() }
            )

            CameraTopButton(
                iconRes = R.drawable.ic_cam_filter,
                isActive = isFilterStripVisible || currentFilter != ImageFilter.ORIGINAL,
                activeColor = cameraHighlight,
                isIos = isIos,
                onClick = { viewModel.toggleFilterStrip() }
            )

            CameraTopButton(
                iconRes = R.drawable.ic_cam_set,
                isActive = false,
                activeColor = cameraHighlight,
                isIos = isIos,
                onClick = onNavigateSettings
            )
        }

        // Zoom Slider Pill (1.0x to 4.0x) using GlassSurface
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 160.dp)
                .width(220.dp)
                .height(34.dp),
            shape = RoundedCornerShape(99.dp),
            backgroundColor = Color(0x8028282C),
            fallbackColor = Color(0x8028282C)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = zoomRatio,
                    onValueChange = { viewModel.setZoom(it) },
                    valueRange = 1.0f..4.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = cameraHighlight,
                        activeTrackColor = cameraHighlight,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format("%.1fx", zoomRatio),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        // Filter Strip using GlassSurface chips (Animation 10)
        val slideOffsetPx = with(density) { 24.dp.roundToPx() }
        AnimatedVisibility(
            visible = isFilterStripVisible,
            enter = slideInVertically(
                animationSpec = if (!animsEnabled) androidx.compose.animation.core.snap() else tween(450, easing = IosEase),
                initialOffsetY = { slideOffsetPx }
            ) + fadeIn(
                animationSpec = if (!animsEnabled) androidx.compose.animation.core.snap() else tween(450, easing = IosEase)
            ),
            exit = slideOutVertically(
                animationSpec = if (!animsEnabled) androidx.compose.animation.core.snap() else tween(450, easing = IosEase),
                targetOffsetY = { slideOffsetPx }
            ) + fadeOut(
                animationSpec = if (!animsEnabled) androidx.compose.animation.core.snap() else tween(450, easing = IosEase)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 110.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageFilter.entries.forEach { filter ->
                    val isSelected = filter == currentFilter
                    GlassSurface(
                        modifier = Modifier
                            .pressScale(0.92f, enabled = isIos)
                            .clickable { viewModel.selectFilter(filter) },
                        shape = RoundedCornerShape(99.dp),
                        backgroundColor = if (isSelected) Color.White else Color(0x8028282C),
                        fallbackColor = if (isSelected) Color.White else Color(0x8028282C)
                    ) {
                        Text(
                            text = AppStrings.t(filter.stringKey, language),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Bottom Bar: Last-photo thumbnail, Shutter button, Flip camera button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recent photo thumbnail (opens gallery)
            val latestPhoto = photos.firstOrNull()
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .pressScale(0.92f, enabled = isIos)
                    .clickable { onNavigateGallery() },
                contentAlignment = Alignment.Center
            ) {
                if (latestPhoto != null) {
                    AsyncImage(
                        model = latestPhoto.uriString,
                        contentDescription = "Recent Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_image),
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 78dp Shutter Button with inner circle scaling to 0.85 with IosSpring (Animation 8)
            val shutterInteraction = remember { MutableInteractionSource() }
            val isShutterPressed by shutterInteraction.collectIsPressedAsState()
            val innerScale by animateFloatAsState(
                targetValue = if (isShutterPressed) 0.85f else 1.0f,
                animationSpec = if (!animsEnabled) androidx.compose.animation.core.snap() else IosSpring,
                label = "shutterInnerScale"
            )

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = shutterInteraction,
                        indication = null
                    ) {
                        viewModel.triggerShutter {
                            imageCapture?.let { capture ->
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                            val rawBitmap = imageProxy.toBitmap()
                                            imageProxy.close()

                                            val matrix = Matrix().apply {
                                                postRotate(rotationDegrees.toFloat())
                                                if (facing == "front") {
                                                    postScale(-1f, 1f)
                                                }
                                            }

                                            val uprightBitmap = Bitmap.createBitmap(
                                                rawBitmap, 0, 0,
                                                rawBitmap.width, rawBitmap.height,
                                                matrix, true
                                            )

                                            val viewW = containerWidthPx.toFloat()
                                            val viewH = containerHeightPx.toFloat()
                                            val previewAspect = viewW / viewH
                                            val imageAspect = uprightBitmap.width.toFloat() / uprightBitmap.height.toFloat()

                                            var cropW = uprightBitmap.width.toFloat()
                                            var cropH = uprightBitmap.height.toFloat()

                                            if (previewAspect < imageAspect) {
                                                cropW = cropH * previewAspect
                                            } else {
                                                cropH = cropW / previewAspect
                                            }

                                            val currentZoom = zoomRatio
                                            cropW /= currentZoom
                                            cropH /= currentZoom

                                            val startX = ((uprightBitmap.width - cropW) / 2f).coerceAtLeast(0f).toInt()
                                            val startY = ((uprightBitmap.height - cropH) / 2f).coerceAtLeast(0f).toInt()
                                            val finalW = cropW.toInt().coerceAtMost(uprightBitmap.width - startX)
                                            val finalH = cropH.toInt().coerceAtMost(uprightBitmap.height - startY)

                                            val croppedBitmap = Bitmap.createBitmap(
                                                uprightBitmap,
                                                startX, startY,
                                                finalW, finalH
                                            )

                                            viewModel.onBitmapCaptured(croppedBitmap) {}
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                        }
                                    }
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer {
                            scaleX = innerScale
                            scaleY = innerScale
                        }
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            // Flip camera button using GlassSurface (Animation 9)
            GlassSurface(
                modifier = Modifier
                    .size(52.dp)
                    .pressScale(0.92f, enabled = isIos)
                    .clickable {
                        flipAngle += 180f
                        viewModel.flipCamera()
                    },
                shape = CircleShape,
                backgroundColor = Color(0x8028282C),
                fallbackColor = Color(0x8028282C)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cam_flip),
                    contentDescription = "Flip Camera",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = animatedFlipAngle }
                )
            }
        }
    }
}

@Composable
private fun CameraTopButton(
    iconRes: Int,
    isActive: Boolean,
    activeColor: Color,
    isIos: Boolean,
    label: String? = null,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .size(44.dp)
            .pressScale(0.92f, enabled = isIos)
            .clickable { onClick() },
        shape = CircleShape,
        backgroundColor = Color(0x8028282C),
        fallbackColor = Color(0x8028282C)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(24.dp)
            )
            label?.let {
                Text(
                    text = it,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) activeColor else Color.White,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }
        }
    }
}

package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.localization.AppStrings
import com.example.ui.CameraViewModel
import com.example.ui.animation.IosEase
import com.example.ui.animation.IosSpring
import com.example.ui.animation.MaterialEase
import com.example.ui.animation.areAnimationsEnabled
import com.example.ui.animation.pressScale
import com.example.ui.components.GlassSurface
import com.example.ui.components.LocalHazeState
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.LensAppTheme
import com.example.ui.theme.LensTheme
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val cameraViewModel: CameraViewModel = viewModel()
            LensApp(cameraViewModel)
        }
    }
}

@Composable
fun LensApp(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }

    val settings by viewModel.settings.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hazeState = remember { HazeState() }

    val layoutDirection = if (settings.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalHazeState provides hazeState
    ) {
        LensAppTheme(
            designLook = settings.designLook,
            appearance = settings.appearance
        ) {
            val tokens = LensTheme.tokens
            val isIos = tokens.isIos
            val isRtl = layoutDirection == LayoutDirection.Rtl

            // Toast animation state (Animation 13)
            val toastScale = remember { Animatable(0.9f) }
            val toastOffsetY = remember { Animatable(20f) }
            val toastAlpha = remember { Animatable(0f) }

            LaunchedEffect(toastMessage) {
                if (toastMessage != null) {
                    toastScale.snapTo(0.9f)
                    toastOffsetY.snapTo(20f)
                    toastAlpha.snapTo(0f)
                    launch {
                        toastAlpha.animateTo(1f, animationSpec = if (!animsEnabled) snap() else tween(300))
                    }
                    launch {
                        toastScale.animateTo(1.0f, animationSpec = if (!animsEnabled) snap() else IosSpring)
                    }
                    launch {
                        toastOffsetY.animateTo(0f, animationSpec = if (!animsEnabled) snap() else IosSpring)
                    }
                } else {
                    toastAlpha.animateTo(0f, animationSpec = if (!animsEnabled) snap() else tween(300))
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // NavHost with per-destination transitions (Animations 1 & 2)
                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val dir = if (isRtl) -1 else 1
                        if (isIos) {
                            slideInHorizontally(
                                animationSpec = if (!animsEnabled) snap() else tween(550, easing = IosEase),
                                initialOffsetX = { fullWidth -> fullWidth * dir }
                            )
                        } else {
                            scaleIn(
                                initialScale = 0.94f,
                                animationSpec = if (!animsEnabled) snap() else tween(350, easing = MaterialEase)
                            ) + fadeIn(animationSpec = if (!animsEnabled) snap() else tween(250))
                        }
                    },
                    exitTransition = {
                        val dir = if (isRtl) -1 else 1
                        if (isIos) {
                            slideOutHorizontally(
                                animationSpec = if (!animsEnabled) snap() else tween(550, easing = IosEase),
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.28f * dir).toInt() }
                            ) + fadeOut(animationSpec = if (!animsEnabled) snap() else tween(350))
                        } else {
                            scaleOut(
                                targetScale = 1.05f,
                                animationSpec = if (!animsEnabled) snap() else tween(350, easing = MaterialEase)
                            ) + fadeOut(animationSpec = if (!animsEnabled) snap() else tween(250))
                        }
                    },
                    popEnterTransition = {
                        val dir = if (isRtl) -1 else 1
                        if (isIos) {
                            slideInHorizontally(
                                animationSpec = if (!animsEnabled) snap() else tween(550, easing = IosEase),
                                initialOffsetX = { fullWidth -> (-fullWidth * 0.28f * dir).toInt() }
                            ) + fadeIn(animationSpec = if (!animsEnabled) snap() else tween(350))
                        } else {
                            scaleIn(
                                initialScale = 1.05f,
                                animationSpec = if (!animsEnabled) snap() else tween(350, easing = MaterialEase)
                            ) + fadeIn(animationSpec = if (!animsEnabled) snap() else tween(250))
                        }
                    },
                    popExitTransition = {
                        val dir = if (isRtl) -1 else 1
                        if (isIos) {
                            slideOutHorizontally(
                                animationSpec = if (!animsEnabled) snap() else tween(550, easing = IosEase),
                                targetOffsetX = { fullWidth -> fullWidth * dir }
                            )
                        } else {
                            scaleOut(
                                targetScale = 0.94f,
                                animationSpec = if (!animsEnabled) snap() else tween(350, easing = MaterialEase)
                            ) + fadeOut(animationSpec = if (!animsEnabled) snap() else tween(250))
                        }
                    }
                ) {
                    composable("splash") {
                        SplashScreen(
                            language = settings.language,
                            onNavigateNext = {
                                val hasCamera = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasCamera && settings.onboardingCompleted) {
                                    navController.navigate("camera") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("permissions") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("permissions") {
                        PermissionsScreen(
                            language = settings.language,
                            onContinue = {
                                scope.launch {
                                    viewModel.settingsRepo.setOnboardingCompleted(true)
                                    navController.navigate("camera") {
                                        popUpTo("permissions") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("camera") {
                        CameraScreen(
                            viewModel = viewModel,
                            onNavigateGallery = { navController.navigate("gallery") },
                            onNavigateSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("gallery") {
                        GalleryScreen(
                            viewModel = viewModel,
                            hazeState = hazeState,
                            onPhotoClick = { photoId ->
                                navController.navigate("viewer/$photoId")
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            hazeState = hazeState
                        )
                    }

                    composable(
                        route = "viewer/{photoId}",
                        arguments = listOf(navArgument("photoId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
                        PhotoViewerScreen(
                            initialPhotoId = photoId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // Bottom Tab Bar: Crisp icons & text over separate GlassBackground layer
                val showTabBar = (currentRoute == "gallery" || currentRoute == "settings") && !isSelectionMode

                AnimatedVisibility(
                    visible = showTabBar,
                    enter = slideInVertically(
                        animationSpec = if (!animsEnabled) snap() else tween(500, easing = IosEase),
                        initialOffsetY = { (it * 1.1f).toInt() }
                    ),
                    exit = slideOutVertically(
                        animationSpec = if (!animsEnabled) snap() else tween(500, easing = IosEase),
                        targetOffsetY = { (it * 1.1f).toInt() }
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    // GlassSurface structure: GlassBackground behind, sharp Content on top
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        shape = RectangleShape,
                        hazeState = hazeState,
                        backgroundColor = tokens.glass,
                        fallbackColor = tokens.fallbackColor,
                        topBorderColor = tokens.line,
                        borderWidth = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Camera Tab
                            LensTabBarItem(
                                iconRes = R.drawable.ic_cam_camera,
                                label = AppStrings.t("camera", settings.language),
                                isSelected = currentRoute == "camera",
                                tokens = tokens,
                                onClick = {
                                    navController.navigate("camera") {
                                        popUpTo("camera") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )

                            // Gallery Tab
                            LensTabBarItem(
                                iconRes = R.drawable.ic_cam_image,
                                label = AppStrings.t("gallery", settings.language),
                                isSelected = currentRoute == "gallery",
                                tokens = tokens,
                                onClick = {
                                    navController.navigate("gallery") {
                                        popUpTo("camera")
                                        launchSingleTop = true
                                    }
                                }
                            )

                            // Settings Tab
                            LensTabBarItem(
                                iconRes = R.drawable.ic_cam_set,
                                label = AppStrings.t("settings", settings.language),
                                isSelected = currentRoute == "settings",
                                tokens = tokens,
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo("camera")
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }

                // Toast Notification Overlay
                if (toastAlpha.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 80.dp)
                            .graphicsLayer {
                                scaleX = toastScale.value
                                scaleY = toastScale.value
                                translationY = toastOffsetY.value * density.density
                                alpha = toastAlpha.value
                            }
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0xCC28282C))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = toastMessage ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab bar item drawn with exact 24dp VectorDrawable, 11sp medium label, and bounce scale on icon only.
 */
@Composable
private fun RowScope.LensTabBarItem(
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    tokens: com.example.ui.theme.LensTokens,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }
    val isIos = tokens.isIos

    val iconBounce = remember { Animatable(1.0f) }

    LaunchedEffect(isSelected) {
        if (isSelected && animsEnabled) {
            iconBounce.snapTo(1.0f)
            iconBounce.animateTo(1.2f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
            iconBounce.animateTo(1.0f, animationSpec = IosSpring)
        }
    }

    val iconColor = if (isSelected) tokens.accent else tokens.textSecondary
    val labelColor = if (isSelected) tokens.accent else tokens.textSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .defaultMinSize(minHeight = 48.dp)
            .pressScale(0.92f, enabled = isIos)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        if (!isIos) {
            // Material look: selected icon sits on pill background (#e8def8 / #4a4458)
            val pillBg = if (isSelected) tokens.segmentActive else Color.Transparent
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(pillBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconBounce.value
                            scaleY = iconBounce.value
                        }
                )
            }
        } else {
            // iOS look: clean icon with bounce
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconBounce.value
                            scaleY = iconBounce.value
                        }
                )
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            maxLines = 1
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.localization.AppStrings
import com.example.ui.CameraViewModel
import com.example.ui.animation.IosEase
import com.example.ui.animation.IosSpring
import com.example.ui.animation.areAnimationsEnabled
import com.example.ui.animation.pressScale
import com.example.ui.components.GlassSurface
import com.example.ui.components.LensDialog
import com.example.ui.theme.LensTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun GalleryScreen(
    viewModel: CameraViewModel,
    hazeState: HazeState? = null,
    onPhotoClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val tokens = LensTheme.tokens
    val isIos = tokens.isIos
    val animsEnabled = remember(context) { areAnimationsEnabled(context) }

    val settings by viewModel.settings.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val selectedIds by viewModel.selectedPhotoIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    val language = settings.language
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val titleText = if (isSelectionMode) {
        AppStrings.t("selN", language, selectedIds.size)
    } else {
        AppStrings.t("gallery", language)
    }

    val selectButtonText = if (isSelectionMode) {
        AppStrings.t("cancel", language)
    } else {
        AppStrings.t("select", language)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row: Title with cross-fade (Animation 11) & Select / Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = titleText,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "galleryTitleCrossFade"
                ) { title ->
                    Text(
                        text = title,
                        fontSize = tokens.titleSize,
                        fontWeight = tokens.titleWeight,
                        letterSpacing = tokens.titleLetterSpacing,
                        color = tokens.text
                    )
                }

                if (photos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .pressScale(0.92f, enabled = isIos)
                            .clickable { viewModel.toggleSelectionMode() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectButtonText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.accent
                        )
                    }
                }
            }

            // Photos Grid or Empty State
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 30.dp, vertical = 90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cam_image),
                            contentDescription = null,
                            tint = tokens.textSecondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = AppStrings.t("empty", language),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                        Text(
                            text = AppStrings.t("emptyD", language),
                            fontSize = 14.sp,
                            color = tokens.textSecondary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier
                        )
                        .padding(bottom = if (isSelectionMode) 80.dp else 70.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        val isSelected = selectedIds.contains(photo.id)

                        // Animation 11: tile shrinks to 0.86 with IosSpring; corner radius 10dp
                        val tileScale by animateFloatAsState(
                            targetValue = if (isSelected) 0.86f else 1.0f,
                            animationSpec = if (!animsEnabled) snap() else IosSpring,
                            label = "galleryTileScale"
                        )
                        val tileCornerRadius by animateFloatAsState(
                            targetValue = if (isSelected) 10f else 0f,
                            animationSpec = if (!animsEnabled) snap() else tween(300),
                            label = "galleryTileRadius"
                        )

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(tokens.fill)
                                .pressScale(0.92f, enabled = isIos)
                                .clickable {
                                    if (isSelectionMode) {
                                        viewModel.togglePhotoSelection(photo.id)
                                    } else {
                                        onPhotoClick(photo.id)
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = photo.uriString,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = tileScale
                                        scaleY = tileScale
                                    }
                                    .clip(RoundedCornerShape(tileCornerRadius.dp))
                            )

                            // Check mark fades in (Animation 11)
                            val checkAlpha by animateFloatAsState(
                                targetValue = if (isSelectionMode) 1f else 0f,
                                animationSpec = tween(200),
                                label = "checkMarkAlpha"
                            )

                            if (checkAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .graphicsLayer { alpha = checkAlpha }
                                        .clip(CircleShape)
                                        .background(if (isSelected) tokens.accent else Color(0x66000000))
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_cam_check),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selection Action Bar with GlassSurface (Animation 5)
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(
                animationSpec = if (!animsEnabled) snap() else tween(500, easing = IosEase),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = if (!animsEnabled) snap() else tween(300)),
            exit = slideOutVertically(
                animationSpec = if (!animsEnabled) snap() else tween(500, easing = IosEase),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = if (!animsEnabled) snap() else tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            val selectedPhotos = photos.filter { selectedIds.contains(it.id) }
            val hasSelection = selectedPhotos.isNotEmpty()

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                hazeState = hazeState,
                backgroundColor = tokens.glass,
                fallbackColor = tokens.fallbackColor,
                topBorderColor = tokens.line,
                borderWidth = 0.5.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .pressScale(0.92f, enabled = isIos)
                            .clickable(enabled = hasSelection) {
                                viewModel.downloadPhotos(context, selectedPhotos)
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cam_download),
                            contentDescription = "Download",
                            tint = if (hasSelection) tokens.accent else tokens.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = AppStrings.t("dl", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasSelection) tokens.accent else tokens.textSecondary.copy(alpha = 0.4f),
                            maxLines = 1
                        )
                    }

                    // Share
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .pressScale(0.92f, enabled = isIos)
                            .clickable(enabled = hasSelection) {
                                viewModel.sharePhotos(context, selectedPhotos)
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cam_share),
                            contentDescription = "Share",
                            tint = if (hasSelection) tokens.accent else tokens.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = AppStrings.t("share", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasSelection) tokens.accent else tokens.textSecondary.copy(alpha = 0.4f),
                            maxLines = 1
                        )
                    }

                    // Delete
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .pressScale(0.92f, enabled = isIos)
                            .clickable(enabled = hasSelection) {
                                showDeleteConfirmDialog = true
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cam_trash),
                            contentDescription = "Delete",
                            tint = if (hasSelection) Color(0xFFFF3B30) else Color(0xFFFF3B30).copy(alpha = 0.35f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = AppStrings.t("del", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasSelection) Color(0xFFFF3B30) else Color(0xFFFF3B30).copy(alpha = 0.35f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteConfirmDialog) {
            val selectedPhotos = photos.filter { selectedIds.contains(it.id) }
            LensDialog(
                visible = true,
                message = AppStrings.t("delQ", language, selectedPhotos.size),
                confirmText = AppStrings.t("del", language),
                cancelText = AppStrings.t("cancel", language),
                onConfirm = {
                    viewModel.deletePhotos(context, selectedPhotos)
                    showDeleteConfirmDialog = false
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                }
            )
        }
    }
}

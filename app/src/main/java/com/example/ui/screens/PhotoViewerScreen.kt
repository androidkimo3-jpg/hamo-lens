package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.localization.AppStrings
import com.example.ui.CameraViewModel
import com.example.ui.animation.pressScale
import com.example.ui.components.GlassSurface
import com.example.ui.components.LensDialog
import com.example.ui.theme.LensTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoViewerScreen(
    initialPhotoId: Long,
    viewModel: CameraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LensTheme.tokens
    val isIos = tokens.isIos

    val photos by viewModel.photos.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val language = settings.language

    val initialIndex = remember(photos, initialPhotoId) {
        val idx = photos.indexOfFirst { it.id == initialPhotoId }
        if (idx != -1) idx else 0
    }

    if (photos.isEmpty()) {
        onBack()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
        pageCount = { photos.size }
    )

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Format date and time
    val locale = if (language == "ar") Locale("ar") else Locale.US
    val dateFormat = remember(locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
    }
    val dateText = remember(currentPhoto.dateTaken, locale) {
        dateFormat.format(Date(currentPhoto.dateTaken))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal Pager with contain-fit photo (Animation 12)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photo.uriString,
                    contentDescription = photo.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Row: Back button, Date pill, spacer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button using GlassSurface structure (Background separate, Icon crisp)
            GlassSurface(
                modifier = Modifier
                    .size(44.dp)
                    .pressScale(0.92f, enabled = isIos)
                    .clickable { onBack() },
                shape = CircleShape,
                backgroundColor = Color(0x8028282C),
                fallbackColor = Color(0xCC28282C)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cam_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            if (isRtl) scaleX = -1f
                        }
                )
            }

            // Date and Time Pill using GlassSurface structure
            GlassSurface(
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(99.dp),
                backgroundColor = Color(0x8028282C),
                fallbackColor = Color(0xCC28282C)
            ) {
                Text(
                    text = dateText,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 14.dp)
                )
            }

            Spacer(modifier = Modifier.size(44.dp))
        }

        // Bottom Action Pill: Download, Share, Delete using GlassSurface structure
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 22.dp)
                .width(230.dp)
                .height(56.dp),
            shape = RoundedCornerShape(99.dp),
            backgroundColor = Color(0x8028282C),
            fallbackColor = Color(0xCC28282C)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pressScale(0.92f, enabled = isIos)
                        .clickable { viewModel.downloadPhotos(context, listOf(currentPhoto)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_download),
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Share
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pressScale(0.92f, enabled = isIos)
                        .clickable { viewModel.sharePhotos(context, listOf(currentPhoto)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_share),
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Delete
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pressScale(0.92f, enabled = isIos)
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_trash),
                        contentDescription = "Delete",
                        tint = Color(0xFFFF6B61),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Confirmation dialog
        if (showDeleteDialog) {
            LensDialog(
                visible = true,
                message = AppStrings.t("delQ", language, 1),
                confirmText = AppStrings.t("del", language),
                cancelText = AppStrings.t("cancel", language),
                onConfirm = {
                    val wasLast = photos.size <= 1
                    viewModel.deletePhotos(context, listOf(currentPhoto))
                    showDeleteDialog = false
                    if (wasLast) onBack()
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }
    }
}

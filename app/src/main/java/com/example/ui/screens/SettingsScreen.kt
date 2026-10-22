package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.localization.AppStrings
import com.example.ui.CameraViewModel
import com.example.ui.animation.pressScale
import com.example.ui.components.LensDialog
import com.example.ui.components.LensSegmentedControl
import com.example.ui.components.LensSwitch
import com.example.ui.theme.LensTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: CameraViewModel,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LensTheme.tokens

    val settings by viewModel.settings.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val language = settings.language

    var showClearDialog by remember { mutableStateOf(false) }

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var storageGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storageGranted = granted
    }

    val totalBytes = remember(photos) {
        photos.sumOf { it.sizeBytes }
    }
    val storageMbText = remember(totalBytes) {
        String.format("%.1f MB", totalBytes.toDouble() / (1024.0 * 1024.0))
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
                .then(
                    if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier
                )
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = AppStrings.t("settings", language),
                    fontSize = tokens.titleSize,
                    fontWeight = tokens.titleWeight,
                    letterSpacing = tokens.titleLetterSpacing,
                    color = tokens.text
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Group 1: Language, Design, Appearance
            SettingsGroup(cardRadius = tokens.cardRadius, surface = tokens.surface, line = tokens.line) {
                // Language
                SettingsRowWithControl(
                    iconRes = R.drawable.ic_cam_globe,
                    label = AppStrings.t("lang", language),
                    tokens = tokens
                ) {
                    LensSegmentedControl(
                        options = listOf("en" to "English", "ar" to "العربية"),
                        selectedKey = settings.language,
                        onOptionSelected = { scope.launch { viewModel.settingsRepo.updateLanguage(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsDivider(line = tokens.line)

                // Design
                SettingsRowWithControl(
                    iconRes = R.drawable.ic_cam_layers,
                    label = AppStrings.t("look", language),
                    tokens = tokens
                ) {
                    LensSegmentedControl(
                        options = listOf(
                            "md" to AppStrings.t("md", language),
                            "ios" to AppStrings.t("ios", language)
                        ),
                        selectedKey = settings.designLook,
                        onOptionSelected = { scope.launch { viewModel.settingsRepo.updateDesign(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsDivider(line = tokens.line)

                // Appearance
                SettingsRowWithControl(
                    iconRes = R.drawable.ic_cam_moon,
                    label = AppStrings.t("mode", language),
                    tokens = tokens
                ) {
                    LensSegmentedControl(
                        options = listOf(
                            "auto" to AppStrings.t("auto", language),
                            "light" to AppStrings.t("light", language),
                            "dark" to AppStrings.t("dark", language)
                        ),
                        selectedKey = settings.appearance,
                        onOptionSelected = { scope.launch { viewModel.settingsRepo.updateAppearance(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 2: Photo Quality, Default Camera, Shutter Sound
            SettingsGroup(cardRadius = tokens.cardRadius, surface = tokens.surface, line = tokens.line) {
                // Photo Quality
                SettingsRowWithControl(
                    iconRes = R.drawable.ic_cam_star,
                    label = AppStrings.t("qual", language),
                    tokens = tokens
                ) {
                    LensSegmentedControl(
                        options = listOf(
                            "std" to AppStrings.t("std", language),
                            "high" to AppStrings.t("high", language)
                        ),
                        selectedKey = settings.photoQuality,
                        onOptionSelected = { scope.launch { viewModel.settingsRepo.updateQuality(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsDivider(line = tokens.line)

                // Default Camera
                SettingsRowWithControl(
                    iconRes = R.drawable.ic_cam_camera,
                    label = AppStrings.t("dcam", language),
                    tokens = tokens
                ) {
                    LensSegmentedControl(
                        options = listOf(
                            "back" to AppStrings.t("back", language),
                            "front" to AppStrings.t("front", language)
                        ),
                        selectedKey = settings.defaultCamera,
                        onOptionSelected = { scope.launch { viewModel.settingsRepo.updateDefaultCamera(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsDivider(line = tokens.line)

                // Shutter Sound
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_vol),
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = AppStrings.t("snd", language),
                        fontSize = 16.sp,
                        color = tokens.text,
                        modifier = Modifier.weight(1f)
                    )
                    LensSwitch(
                        checked = settings.shutterSound,
                        onCheckedChange = { scope.launch { viewModel.settingsRepo.updateShutterSound(it) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 3: Permissions Status
            SettingsGroup(cardRadius = tokens.cardRadius, surface = tokens.surface, line = tokens.line) {
                // Camera Permission
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_camera),
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = AppStrings.t("camera", language),
                        fontSize = 16.sp,
                        color = tokens.text,
                        modifier = Modifier.weight(1f)
                    )
                    if (cameraGranted) {
                        Text(
                            text = AppStrings.t("ok", language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34C759)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(tokens.accent)
                                .pressScale(0.92f, enabled = tokens.isIos)
                                .clickable { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = AppStrings.t("allow", language),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.onAccent
                            )
                        }
                    }
                }

                SettingsDivider(line = tokens.line)

                // Storage Permission
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_lock),
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = AppStrings.t("stor", language),
                        fontSize = 16.sp,
                        color = tokens.text,
                        modifier = Modifier.weight(1f)
                    )
                    if (storageGranted) {
                        Text(
                            text = AppStrings.t("ok", language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34C759)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(tokens.accent)
                                .pressScale(0.92f, enabled = tokens.isIos)
                                .clickable {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = AppStrings.t("allow", language),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.onAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 4: Storage Used & Delete All Photos
            SettingsGroup(cardRadius = tokens.cardRadius, surface = tokens.surface, line = tokens.line) {
                // Storage Used
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_disk),
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = AppStrings.t("used", language),
                        fontSize = 16.sp,
                        color = tokens.text,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = storageMbText,
                        fontSize = 15.sp,
                        color = tokens.textSecondary
                    )
                }

                SettingsDivider(line = tokens.line)

                // Delete All Photos (Red danger row)
                val hasPhotos = photos.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(0.96f, enabled = tokens.isIos)
                        .clickable(enabled = hasPhotos) { showClearDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_trash),
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = AppStrings.t("clear", language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF3B30),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 5: About Card
            SettingsGroup(cardRadius = tokens.cardRadius, surface = tokens.surface, line = tokens.line) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_lens_logo),
                        contentDescription = "Lens Logo",
                        modifier = Modifier.size(48.dp)
                    )
                    Column {
                        Text(
                            text = AppStrings.t("about", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                        Text(
                            text = AppStrings.t("aboutD", language),
                            fontSize = 13.sp,
                            color = tokens.textSecondary
                        )
                    }
                }
            }
        }

        // Clear all photos dialog
        if (showClearDialog) {
            LensDialog(
                visible = true,
                message = AppStrings.t("clearQ", language),
                confirmText = AppStrings.t("del", language),
                cancelText = AppStrings.t("cancel", language),
                onConfirm = {
                    viewModel.deleteAllPhotos(context)
                    showClearDialog = false
                },
                onDismiss = {
                    showClearDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    cardRadius: androidx.compose.ui.unit.Dp,
    surface: Color,
    line: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(cardRadius))
            .background(surface)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider(line: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(line)
    )
}

@Composable
private fun SettingsRowWithControl(
    iconRes: Int,
    label: String,
    tokens: com.example.ui.theme.LensTokens,
    control: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 16.sp,
                color = tokens.text
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        control()
    }
}

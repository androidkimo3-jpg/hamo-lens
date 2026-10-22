package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.localization.AppStrings
import com.example.ui.animation.pressScale
import com.example.ui.theme.LensTheme

@Composable
fun PermissionsScreen(
    language: String,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LensTheme.tokens

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraDeniedPermanent by remember { mutableStateOf(false) }

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
        if (!granted) {
            cameraDeniedPermanent = true
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storageGranted = granted
    }

    LaunchedEffect(Unit) {
        cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        storageGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_lens_logo),
                contentDescription = null,
                modifier = Modifier.size(76.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = AppStrings.t("permT", language),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = AppStrings.t("permD", language),
                fontSize = 15.sp,
                color = tokens.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Permissions Card Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(tokens.cardRadius))
                    .background(tokens.surface)
            ) {
                // Camera Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_camera),
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.t("camera", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.text
                        )
                        Text(
                            text = AppStrings.t("camD", language),
                            fontSize = 13.sp,
                            color = tokens.textSecondary
                        )
                    }

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
                                .clickable {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.t("allow", language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.onAccent
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(tokens.line)
                )

                // Storage Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cam_lock),
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.t("stor", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.text
                        )
                        Text(
                            text = AppStrings.t("storD", language),
                            fontSize = 13.sp,
                            color = tokens.textSecondary
                        )
                    }

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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppStrings.t("allow", language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.onAccent
                            )
                        }
                    }
                }
            }

            if (cameraDeniedPermanent) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = AppStrings.t("dh", language),
                    fontSize = 13.sp,
                    color = Color(0xFFFF3B30),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .clip(CircleShape)
                        .background(tokens.fill)
                        .clickable {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = AppStrings.t("settings", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.accent
                    )
                }
            }
        }

        // Continue Button at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            val isEnabled = cameraGranted
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(tokens.buttonRadius))
                    .background(if (isEnabled) tokens.accent else tokens.accent.copy(alpha = 0.35f))
                    .pressScale(0.92f, enabled = isEnabled && tokens.isIos)
                    .clickable(enabled = isEnabled) { onContinue() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStrings.t("cont", language),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.onAccent
                )
            }
        }
    }
}

package com.ai.assistance.operit.ui.features.permission.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.assistance.operit.R
import com.ai.assistance.operit.core.tools.system.AndroidPermissionLevel
import com.ai.assistance.operit.ui.features.permission.viewmodel.PermissionGuideViewModel
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fork single-screen onboarding: liability notice + basic permissions +
 * permission level on one scrollable screen. The old 5-page pager
 * (PermissionGuideScreen) plus the separate Agreement screen were merged
 * here; the feature tour moved behind [FeatureTourDialog], opened by an
 * explicit button instead of blocking the flow.
 *
 * Why this shape: basic permissions and the level can both be granted or
 * changed later (settings, shell recipe), so nothing here blocks Continue.
 * If no level is selected, STANDARD is saved — the same default the app
 * used before the guide existed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
        viewModel: PermissionGuideViewModel = viewModel(),
        onComplete: () -> Unit
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    var showTour by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.checkPermissions(context) }

    val storagePermissionLauncher =
            rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    val readGranted =
                            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
                    val writeGranted =
                            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
                    if (readGranted && writeGranted) {
                        viewModel.checkPermissions(context)
                    }
                }
            }

    val locationPermissionLauncher =
            rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseGranted =
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                if (fineGranted || coarseGranted) {
                    viewModel.updateLocationPermission(true)
                }
            }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            delay(500)
            onComplete()
        }
    }

    if (showTour) {
        FeatureTourDialog(onDismiss = { showTour = false })
    }

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .systemBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LiabilityCard(onTourClick = { showTour = true })

        Spacer(modifier = Modifier.height(24.dp))

        BasicPermissionsSection(
                hasStoragePermission = uiState.hasStoragePermission,
                hasOverlayPermission = uiState.hasOverlayPermission,
                hasBatteryOptimizationExemption = uiState.hasBatteryOptimizationExemption,
                hasLocationPermission = uiState.hasLocationPermission,
                onStoragePermissionClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val intent =
                                    Intent(
                                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                    )
                                            .apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            AppLogger.e("Onboarding", "Cannot open storage settings page", e)
                            try {
                                val intent =
                                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(
                                                context,
                                                context.getString(
                                                        R.string
                                                                .permission_guide_storage_setting_failed
                                                ),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        }
                    } else {
                        storagePermissionLauncher.launch(
                                arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                        )
                    }
                },
                onOverlayPermissionClick = {
                    try {
                        val intent =
                                Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + context.packageName)
                                )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.permission_guide_overlay_setting_failed
                                        ),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                },
                onBatteryOptimizationClick = {
                    try {
                        val intent =
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .apply {
                                            data =
                                                    Uri.parse(
                                                            "package:" + context.packageName
                                                    )
                                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent =
                                    Intent(
                                            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                    )
                            context.startActivity(intent)
                            Toast.makeText(
                                            context,
                                            context.getString(
                                                    R.string.permission_guide_battery_hint
                                            ),
                                            Toast.LENGTH_LONG
                                    )
                                    .show()
                        } catch (e2: Exception) {
                            Toast.makeText(
                                            context,
                                            context.getString(
                                                    R.string.permission_guide_battery_setting_failed
                                            ),
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                },
                onLocationPermissionClick = {
                    locationPermissionLauncher.launch(
                            arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                    )
                },
                onRefresh = { viewModel.checkPermissions(context) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionLevelSection(
                selectedLevel = uiState.selectedPermissionLevel,
                onLevelSelected = { viewModel.selectPermissionLevel(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Continue is never blocked: missing permissions can be granted
        // later, and an unselected level falls back to STANDARD.
        Button(
                onClick = {
                    if (uiState.selectedPermissionLevel == null) {
                        viewModel.selectPermissionLevel(AndroidPermissionLevel.STANDARD)
                    }
                    viewModel.savePermissionLevel()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                    text = stringResource(R.string.onboarding_continue),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LiabilityCard(onTourClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                    text = stringResource(R.string.onboarding_liability_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = stringResource(R.string.onboarding_liability_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onTourClick, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(R.string.onboarding_tour_button))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeatureTourDialog(onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val pages =
            listOf(
                    stringResource(R.string.permission_guide_intro_1_title) to
                            stringResource(R.string.permission_guide_intro_1_desc),
                    stringResource(R.string.permission_guide_intro_2_title) to
                            stringResource(R.string.permission_guide_intro_2_desc),
                    stringResource(R.string.permission_guide_intro_3_title) to
                            stringResource(R.string.permission_guide_intro_3_desc)
            )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        userScrollEnabled = false
                ) { page ->
                    TourPage(
                            title = pages[page].first,
                            description = pages[page].second,
                            pageIndex = page
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(
                                                pagerState.currentPage - 1
                                        )
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                    ) {
                        Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription =
                                        stringResource(R.string.permission_guide_previous)
                        )
                    }

                    Text(
                            text =
                                    stringResource(
                                            R.string.permission_guide_intro_page_indicator,
                                            pagerState.currentPage + 1,
                                            3
                                    ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    if (pagerState.currentPage < 2) {
                        IconButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                                pagerState.currentPage + 1
                                        )
                                    }
                                }
                        ) {
                            Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription =
                                            stringResource(R.string.permission_guide_next)
                            )
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text(text = stringResource(R.string.onboarding_tour_close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TourPage(title: String, description: String, pageIndex: Int) {
    Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Box(
                modifier =
                        Modifier.size(64.dp)
                                .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        CircleShape
                                )
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
        ) {
            Text(
                    text = "#${pageIndex + 1}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BasicPermissionsSection(
        hasStoragePermission: Boolean,
        hasOverlayPermission: Boolean,
        hasBatteryOptimizationExemption: Boolean,
        hasLocationPermission: Boolean,
        onStoragePermissionClick: () -> Unit,
        onOverlayPermissionClick: () -> Unit,
        onBatteryOptimizationClick: () -> Unit,
        onLocationPermissionClick: () -> Unit,
        onRefresh: () -> Unit
) {
    var refreshRotation by remember { mutableStateOf(0f) }
    val rotationAngle by
            animateFloatAsState(
                    targetValue = refreshRotation,
                    animationSpec = tween(500),
                    label = "Refresh Rotation"
            )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = stringResource(R.string.permission_guide_basic_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
                text = stringResource(R.string.permission_guide_basic_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionItem(
                        title = stringResource(R.string.permission_guide_storage_title),
                        description = stringResource(R.string.permission_guide_storage_desc),
                        isGranted = hasStoragePermission,
                        onClick = onStoragePermissionClick
                )

                HorizontalDivider()

                PermissionItem(
                        title = stringResource(R.string.permission_guide_overlay_title),
                        description = stringResource(R.string.permission_guide_overlay_desc),
                        isGranted = hasOverlayPermission,
                        onClick = onOverlayPermissionClick
                )

                HorizontalDivider()

                PermissionItem(
                        title = stringResource(R.string.permission_guide_battery_title),
                        description = stringResource(R.string.permission_guide_battery_desc),
                        isGranted = hasBatteryOptimizationExemption,
                        onClick = onBatteryOptimizationClick
                )

                HorizontalDivider()

                PermissionItem(
                        title = stringResource(R.string.permission_guide_location_title),
                        description = stringResource(R.string.permission_guide_location_desc),
                        isGranted = hasLocationPermission,
                        onClick = onLocationPermissionClick
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
                onClick = {
                    refreshRotation += 360f
                    onRefresh()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription =
                            stringResource(R.string.permission_guide_check_permissions),
                    modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = rotationAngle }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.permission_guide_check_permissions))
        }

        Spacer(modifier = Modifier.height(8.dp))

        val allGranted =
                hasStoragePermission &&
                        hasOverlayPermission &&
                        hasBatteryOptimizationExemption &&
                        hasLocationPermission

        AnimatedVisibility(visible = allGranted, enter = fadeIn(), exit = fadeOut()) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(8.dp)
                                    .background(
                                            color =
                                                    MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.1f
                                                    ),
                                            shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = stringResource(R.string.permission_guide_all_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
        title: String,
        description: String,
        isGranted: Boolean,
        onClick: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Box(
                modifier =
                        Modifier.size(24.dp)
                                .background(
                                        color =
                                                if (isGranted)
                                                        MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.1f
                                                        )
                                                else
                                                        MaterialTheme.colorScheme.error.copy(
                                                                alpha = 0.1f
                                                        ),
                                        shape = CircleShape
                                )
                                .border(
                                        width = 1.dp,
                                        color =
                                                if (isGranted) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                ),
                contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.permission_guide_granted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.permission_guide_not_granted),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionLevelSection(
        selectedLevel: AndroidPermissionLevel?,
        onLevelSelected: (AndroidPermissionLevel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = stringResource(R.string.permission_guide_level_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
                text = stringResource(R.string.permission_guide_level_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PermissionLevelItem(
                    level = AndroidPermissionLevel.STANDARD,
                    title = stringResource(R.string.permission_guide_standard_title),
                    description = stringResource(R.string.permission_guide_standard_desc),
                    isSelected = selectedLevel == AndroidPermissionLevel.STANDARD,
                    onClick = { onLevelSelected(AndroidPermissionLevel.STANDARD) }
            )

            PermissionLevelItem(
                    level = AndroidPermissionLevel.ACCESSIBILITY,
                    title = stringResource(R.string.permission_guide_accessibility_title),
                    description = stringResource(R.string.permission_guide_accessibility_desc),
                    isSelected = selectedLevel == AndroidPermissionLevel.ACCESSIBILITY,
                    onClick = { onLevelSelected(AndroidPermissionLevel.ACCESSIBILITY) }
            )

            PermissionLevelItem(
                    level = AndroidPermissionLevel.DEBUGGER,
                    title = stringResource(R.string.permission_guide_debugger_title),
                    description = stringResource(R.string.permission_guide_debugger_desc),
                    isSelected = selectedLevel == AndroidPermissionLevel.DEBUGGER,
                    onClick = { onLevelSelected(AndroidPermissionLevel.DEBUGGER) }
            )

            PermissionLevelItem(
                    level = AndroidPermissionLevel.ROOT,
                    title = stringResource(R.string.permission_guide_root_title),
                    description = stringResource(R.string.permission_guide_root_desc),
                    isSelected = selectedLevel == AndroidPermissionLevel.ROOT,
                    onClick = { onLevelSelected(AndroidPermissionLevel.ROOT) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
                text = stringResource(R.string.onboarding_level_default_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Text(
                text = stringResource(R.string.permission_guide_change_anytime),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun PermissionLevelItem(
        level: AndroidPermissionLevel,
        title: String,
        description: String,
        isSelected: Boolean,
        onClick: () -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            color =
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface,
            border =
                    if (isSelected)
                            androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary
                            )
                    else null
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier =
                            Modifier.size(20.dp)
                                    .background(
                                            color =
                                                    if (isSelected)
                                                            MaterialTheme.colorScheme.primary
                                                    else Color.Transparent,
                                            shape = CircleShape
                                    )
                                    .border(
                                            width = 1.dp,
                                            color =
                                                    if (isSelected)
                                                            MaterialTheme.colorScheme.primary
                                                    else
                                                            MaterialTheme.colorScheme.onSurface
                                                                    .copy(alpha = 0.5f),
                                            shape = CircleShape
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.permission_guide_selected),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                )

                Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

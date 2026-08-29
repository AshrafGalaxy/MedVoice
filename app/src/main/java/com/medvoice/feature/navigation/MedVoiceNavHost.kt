package com.medvoice.feature.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.feature.cabinet.CabinetScreen
import com.medvoice.feature.history.CaregiverAuditScreen
import com.medvoice.feature.home.HomeScreen
import com.medvoice.feature.scanner.ScanScreen
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.feature.settings.SettingsScreen
import com.medvoice.ui.theme.AccentBorder
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite

enum class MedVoiceTab(
    val titleEn: String,
    val titleHi: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector
) {
    HOME("Home", "होम", Icons.Outlined.Home, Icons.Filled.Home),
    CABINET("Cabinet", "दवा पेटी", Icons.Outlined.Medication, Icons.Filled.Medication),
    SCANNER("Scan", "स्कैन", Icons.Filled.CenterFocusStrong, Icons.Filled.CenterFocusStrong),
    CAREGIVER("Caregiver", "केयरगिवर", Icons.Outlined.Shield, Icons.Filled.Shield),
    SETTINGS("Settings", "सेटिंग्स", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun MedVoiceNavHost(viewModel: ScanViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

    if (!isOnboardingCompleted) {
        com.medvoice.feature.onboarding.OnboardingScreen(
            viewModel = viewModel,
            onOnboardingComplete = { viewModel.completeOnboarding() }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundCharcoal,
        bottomBar = {
            ModernSleekNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.navigateToTab(it) },
                locale = locale
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundCharcoal)
        ) {
            when (currentTab) {
                MedVoiceTab.HOME -> HomeScreen(viewModel = viewModel)
                MedVoiceTab.SCANNER -> ScanScreen(viewModel = viewModel)
                MedVoiceTab.CABINET -> CabinetScreen(viewModel = viewModel)
                MedVoiceTab.CAREGIVER -> CaregiverAuditScreen(
                    viewModel = viewModel,
                    onBackToScanner = { viewModel.navigateToTab(MedVoiceTab.SCANNER) }
                )
                MedVoiceTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * Modern High-Contrast Edge-to-Edge Navigation Bar
 * Features:
 * - 5 mathematically balanced, equally weighted columns (weight = 1f)
 * - Micro-pill active indicator container with smooth alpha transition
 * - Animated icon scaling and dynamic high-contrast color transitions
 * - Crisp typography and haptic feedback on tab selection
 */
@Composable
private fun ModernSleekNavigationBar(
    currentTab: MedVoiceTab,
    onTabSelected: (MedVoiceTab) -> Unit,
    locale: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SurfaceCardDark.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, AccentBorder.copy(alpha = 0.6f)),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MedVoiceTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val label = if (locale == "hi") tab.titleHi else tab.titleEn

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconScale"
                )

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) SafeGreen else TextMuted,
                    label = "iconColor"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) SafeGreen else TextMuted,
                    label = "textColor"
                )

                val pillAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 0.18f else 0.0f,
                    label = "pillAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Subtle Active Micro-Pill behind the active icon
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SafeGreen.copy(alpha = pillAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
                                contentDescription = label,
                                tint = iconColor,
                                modifier = Modifier
                                    .scale(iconScale)
                                    .size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


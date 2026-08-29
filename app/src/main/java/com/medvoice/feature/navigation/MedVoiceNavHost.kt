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
    SCANNER("Scanner", "स्कैनर", Icons.Filled.CenterFocusStrong, Icons.Filled.CenterFocusStrong),
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
            FloatingCapsuleNavigationBar(
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
 * Ultra-Sleek Floating Glassmorphic Capsule Navigation Bar
 * Features:
 * - Floating rounded dock hovering above the gesture bar
 * - Prominent elevated Center Hero Scanner action with vibrant emerald gradient
 * - Dual-state outline/filled icons with spring scale micro-animations
 * - Hardware haptic feedback on tab selection
 */
@Composable
private fun FloatingCapsuleNavigationBar(
    currentTab: MedVoiceTab,
    onTabSelected: (MedVoiceTab) -> Unit,
    locale: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(34.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = Color.Black.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(34.dp),
            color = SurfaceCardDark.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, AccentBorder.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MedVoiceTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val label = if (locale == "hi") tab.titleHi else tab.titleEn

                    if (tab == MedVoiceTab.SCANNER) {
                        // Elevated Center Hero Scanner Button
                        HeroScannerButton(
                            isSelected = isSelected,
                            label = label,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabSelected(tab)
                            }
                        )
                    } else {
                        // Sleek Minimalist Side Tab
                        MinimalistNavItem(
                            tab = tab,
                            isSelected = isSelected,
                            label = label,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Center Hero Scanner Button with Emerald Gradient & Subtle Pulse Glow
 */
@Composable
private fun HeroScannerButton(
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "heroScale"
    )

    val gradientBrush = Brush.linearGradient(
        colors = if (isSelected) {
            listOf(Color(0xFF10B981), Color(0xFF047857))
        } else {
            listOf(Color(0xFF059669), Color(0xFF065F46))
        }
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(52.dp)
            .shadow(
                elevation = if (isSelected) 12.dp else 6.dp,
                shape = CircleShape,
                spotColor = SafeGreen.copy(alpha = if (isSelected) 0.8f else 0.4f)
            )
            .clip(CircleShape)
            .background(gradientBrush)
            .border(
                width = if (isSelected) 2.dp else 1.5.dp,
                color = if (isSelected) Color(0xFF6EE7B7) else Color(0xFF34D399).copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CenterFocusStrong,
            contentDescription = label,
            tint = TextWhite,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Sleek Minimalist Tab Item with Smooth Spring Scale & Active Glow Dot
 */
@Composable
private fun MinimalistNavItem(
    tab: MedVoiceTab,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) SafeGreen else TextMuted,
        label = "iconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) TextWhite else TextMuted.copy(alpha = 0.8f),
        label = "textColor"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(23.dp)
        )
        
        Spacer(modifier = Modifier.height(3.dp))
        
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Active Emerald Micro-Dot Indicator
        Box(
            modifier = Modifier
                .size(width = if (isSelected) 12.dp else 0.dp, height = 2.dp)
                .clip(CircleShape)
                .background(if (isSelected) SafeGreen else Color.Transparent)
        )
    }
}


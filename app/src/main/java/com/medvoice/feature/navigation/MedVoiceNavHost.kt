package com.medvoice.feature.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.feature.cabinet.CabinetScreen
import com.medvoice.feature.history.CaregiverAuditScreen
import com.medvoice.feature.home.HomeScreen
import com.medvoice.feature.scanner.ScanScreen
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.feature.settings.SettingsScreen
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite

enum class MedVoiceTab(
    val titleEn: String,
    val titleHi: String,
    val icon: ImageVector
) {
    HOME("Home", "होम", Icons.Default.Home),
    SCANNER("Scanner", "स्कैनर", Icons.Default.CameraAlt),
    CABINET("Cabinet", "दवा पेटी", Icons.Default.MedicalServices),
    CAREGIVER("Caregiver", "केयरगिवर", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", "सेटिंग्स", Icons.Default.Settings)
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
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .navigationBarsPadding(),
                containerColor = SurfaceCardDark,
                tonalElevation = 8.dp
            ) {
                MedVoiceTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val label = if (locale == "hi") tab.titleHi else tab.titleEn

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateToTab(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = label,
                                modifier = Modifier.size(if (tab == MedVoiceTab.SCANNER) 30.dp else 24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = if (isSelected) 12.sp else 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextWhite,
                            selectedTextColor = SafeGreen,
                            indicatorColor = SafeGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
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

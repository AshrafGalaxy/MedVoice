package com.medvoice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.ui.theme.AlertRed
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextWhite
import com.medvoice.ui.theme.WarningAmber

/**
 * Premium MedVoice Medical Logo Vector Branding
 */
@Composable
fun MedVoiceLogo(
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(SafeGreen, ReticleCyan)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalHospital,
            contentDescription = "MedVoice Logo",
            tint = TextWhite,
            modifier = Modifier.size((size * 0.65).dp)
        )
    }
}

/**
 * High-Contrast Clinical Status Pill Badge (Replaces text emojis)
 */
@Composable
fun StatusBadge(
    text: String,
    statusType: StatusType,
    modifier: Modifier = Modifier
) {
    val (bgColor, iconColor, icon) = when (statusType) {
        StatusType.SAFE -> Triple(SafeGreen.copy(alpha = 0.18f), SafeGreen, Icons.Default.CheckCircle)
        StatusType.WARNING -> Triple(WarningAmber.copy(alpha = 0.18f), WarningAmber, Icons.Default.Warning)
        StatusType.DANGER -> Triple(AlertRed.copy(alpha = 0.18f), AlertRed, Icons.Default.Warning)
        StatusType.INFO -> Triple(ReticleCyan.copy(alpha = 0.18f), ReticleCyan, Icons.Default.Info)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, iconColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

enum class StatusType {
    SAFE, WARNING, DANGER, INFO
}

/**
 * Dynamic Time-of-Day Weather/Sun Vector Icon
 */
@Composable
fun TimeOfDayIcon(hour: Int) {
    val (icon, tint) = when {
        hour < 12 -> Icons.Default.WbSunny to Color(0xFFFFB703)
        hour < 17 -> Icons.Default.LightMode to Color(0xFFFB8500)
        else -> Icons.Default.NightsStay to ReticleCyan
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
}

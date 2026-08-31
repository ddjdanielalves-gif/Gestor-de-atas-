package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DueSoonOrange
import com.example.ui.theme.DueSoonOrangeLight
import com.example.ui.theme.DueTodayAmber
import com.example.ui.theme.DueTodayAmberLight
import com.example.ui.theme.OnTrackGreen
import com.example.ui.theme.OnTrackGreenLight
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.OverdueRedLight
import com.example.util.DateUtils
import com.example.util.DeadlineUrgency

@Composable
fun DecisionTypeBadge(
    decisionType: String,
    typeLabel: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (decisionType) {
        "PERMANENT_PROCEDURE" -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), Icons.Default.CheckCircle)
        "ACTION_DEADLINE" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.AccessTime)
        else -> Triple(Color(0xFFCCFBF1), Color(0xFF0F766E), Icons.Default.WarningAmber)
    }

    val displayLabel = if (typeLabel.isNotBlank()) typeLabel else when (decisionType) {
        "PERMANENT_PROCEDURE" -> "Decisão permanente"
        "ACTION_DEADLINE" -> "Ação com prazo"
        else -> "Atribuição de acompanhamento"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = displayLabel,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DeadlineStatusBadge(
    dueDateMillis: Long,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val urgency = DateUtils.getUrgency(dueDateMillis, isCompleted)
    val badgeText = DateUtils.getDeadlineBadgeText(dueDateMillis, isCompleted)

    val (bgColor, textColor, icon) = when (urgency) {
        DeadlineUrgency.OVERDUE -> Triple(OverdueRedLight, OverdueRed, Icons.Default.ErrorOutline)
        DeadlineUrgency.TODAY -> Triple(DueTodayAmberLight, DueTodayAmber, Icons.Default.WarningAmber)
        DeadlineUrgency.SOON -> Triple(DueSoonOrangeLight, DueSoonOrange, Icons.Default.AccessTime)
        DeadlineUrgency.NORMAL -> Triple(OnTrackGreenLight, OnTrackGreen, Icons.Default.AccessTime)
        DeadlineUrgency.COMPLETED -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), Icons.Default.CheckCircle)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

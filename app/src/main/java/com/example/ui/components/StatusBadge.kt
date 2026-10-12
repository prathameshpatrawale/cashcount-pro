package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CountStatus
import com.example.ui.theme.StatusBalanced
import com.example.ui.theme.StatusBalancedBgLight
import com.example.ui.theme.StatusExcess
import com.example.ui.theme.StatusExcessBgLight
import com.example.ui.theme.StatusShortage
import com.example.ui.theme.StatusShortageBgLight

@Composable
fun StatusBadge(
    status: CountStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status) {
        CountStatus.BALANCED -> Quadruple(
            StatusBalancedBgLight,
            StatusBalanced,
            Icons.Default.CheckCircle,
            "Balanced"
        )
        CountStatus.EXCESS -> Quadruple(
            StatusExcessBgLight,
            StatusExcess,
            Icons.Default.ArrowUpward,
            "Excess"
        )
        CountStatus.SHORTAGE -> Quadruple(
            StatusShortageBgLight,
            StatusShortage,
            Icons.Default.ArrowDownward,
            "Shortage"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

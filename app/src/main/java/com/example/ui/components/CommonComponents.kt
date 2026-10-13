package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun HisabTripLogo(
  size: Dp = 44.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.28f))
      .background(PrimaryAction)
      .border(1.dp, Color(0xFF333333), RoundedCornerShape(size * 0.28f)),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_hisabtrip_logo),
      contentDescription = "HisabTrip Logo",
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(size)
    )
  }
}

@Composable
fun ScenicTripCover(
  title: String,
  dates: String,
  status: String,
  height: Dp = 160.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(20.dp))
      .border(0.8.dp, BorderSubtle, RoundedCornerShape(20.dp))
  ) {
    Image(
      painter = painterResource(id = R.drawable.scenic_travel_cover),
      contentDescription = title,
      contentScale = ContentScale.Crop,
      modifier = Modifier.matchParentSize()
    )

    // Warm dark gradient overlay for crisp text contrast
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0x22000000),
              Color(0xB3191919)
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .matchParentSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        StatusBadge(status = status)
      }

      Column {
        Text(
          text = title,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        if (dates.isNotEmpty()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = dates,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xCCFFFFFF)
          )
        }
      }
    }
  }
}

@Composable
fun StatusBadge(
  status: String,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor, borderColor) = when (status.uppercase()) {
    "APPROVED", "PAID", "SETTLED", "ACTIVE" -> Triple(StatusGreenBg, StatusGreen, StatusGreenBorder)
    "PENDING" -> Triple(StatusAmberBg, StatusAmber, StatusAmberBorder)
    "REJECTED", "OVER_BUDGET" -> Triple(StatusRedBg, StatusRed, StatusRedBorder)
    else -> Triple(SurfaceNeutral, TextSecondary, BorderSubtle)
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    modifier = modifier.border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      val icon: ImageVector? = when (status.uppercase()) {
        "APPROVED", "PAID", "SETTLED" -> Icons.Default.CheckCircle
        "PENDING" -> Icons.Default.Pending
        "REJECTED" -> Icons.Default.Warning
        else -> null
      }
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = textColor,
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = status.replace("_", " "),
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String? = null,
  valueColor: Color = TextPrimary,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardBackground),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.border(0.8.dp, BorderSubtle, RoundedCornerShape(16.dp))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = valueColor
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = TextTertiary
        )
      }
    }
  }
}

@Composable
fun EmptyStateView(
  title: String,
  message: String,
  icon: ImageVector = Icons.Default.Info,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(60.dp)
        .clip(CircleShape)
        .background(SurfaceNeutral),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TextSecondary,
        modifier = Modifier.size(28.dp)
      )
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text = title,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      color = TextPrimary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = message,
      fontSize = 13.sp,
      color = TextSecondary,
      lineHeight = 18.sp,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    if (actionLabel != null && onAction != null) {
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onAction,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryAction,
          contentColor = PureWhite
        ),
        modifier = Modifier.testTag("empty_state_action_button")
      ) {
        Text(text = actionLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

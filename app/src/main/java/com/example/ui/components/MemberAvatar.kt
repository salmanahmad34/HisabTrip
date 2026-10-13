package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightShelter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Graphite100
import com.example.ui.theme.Graphite200
import com.example.ui.theme.Graphite700
import com.example.ui.theme.Graphite800
import com.example.ui.theme.PureWhite

@Composable
fun MemberAvatar(
  avatarKey: String,
  name: String,
  size: Dp = 40.dp,
  modifier: Modifier = Modifier
) {
  val icon: ImageVector = when (avatarKey.lowercase()) {
    "compass" -> Icons.Default.Explore
    "mountain" -> Icons.Default.Landscape
    "tent" -> Icons.Default.NightShelter
    "backpack" -> Icons.Default.DirectionsWalk
    "bonfire" -> Icons.Default.LocalFireDepartment
    "camera" -> Icons.Default.CameraAlt
    "map" -> Icons.Default.Map
    "luggage" -> Icons.Default.Luggage
    else -> Icons.Default.AccountCircle
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(Graphite100)
      .border(1.dp, Graphite200, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = name,
      tint = Graphite700,
      modifier = Modifier.size(size * 0.58f)
    )
  }
}

@Composable
fun MemberAvatarWithInitial(
  name: String,
  size: Dp = 40.dp,
  backgroundColor: Color = Graphite800,
  textColor: Color = PureWhite,
  modifier: Modifier = Modifier
) {
  val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(backgroundColor),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initial,
      color = textColor,
      fontSize = (size.value * 0.42f).sp,
      lineHeight = (size.value * 0.42f).sp
    )
  }
}

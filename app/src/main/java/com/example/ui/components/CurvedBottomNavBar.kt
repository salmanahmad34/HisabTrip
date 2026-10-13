package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TripTab

@Composable
fun MainCurvedBottomNavBar(
  currentScreen: AppScreen,
  onNavigate: (AppScreen) -> Unit,
  onCreateTripClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 20.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(36.dp),
      color = CardBackground,
      shadowElevation = 8.dp,
      modifier = Modifier
        .shadow(10.dp, RoundedCornerShape(36.dp), spotColor = Color(0x22191919))
        .border(1.dp, BorderSubtle, RoundedCornerShape(36.dp))
    ) {
      Row(
        modifier = Modifier
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        NavBarItem(
          label = "Home",
          selected = currentScreen == AppScreen.HOME,
          activeIcon = Icons.Filled.Home,
          inactiveIcon = Icons.Outlined.Home,
          testTag = "nav_home",
          onClick = { onNavigate(AppScreen.HOME) }
        )

        NavBarItem(
          label = "Trips",
          selected = currentScreen == AppScreen.TRIPS_LIST || currentScreen == AppScreen.TRIP_DASHBOARD,
          activeIcon = Icons.Filled.FlightTakeoff,
          inactiveIcon = Icons.Outlined.FlightTakeoff,
          testTag = "nav_trips",
          onClick = { onNavigate(AppScreen.TRIPS_LIST) }
        )

        // Elevated Circular Central Create Button
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(PrimaryAction)
            .clickable(onClick = onCreateTripClick)
            .testTag("nav_central_create_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create Trip",
            tint = PureWhite,
            modifier = Modifier.size(24.dp)
          )
        }

        NavBarItem(
          label = "Profile",
          selected = currentScreen == AppScreen.PROFILE,
          activeIcon = Icons.Filled.Person,
          inactiveIcon = Icons.Outlined.Person,
          testTag = "nav_profile",
          onClick = { onNavigate(AppScreen.PROFILE) }
        )
      }
    }
  }
}

@Composable
private fun NavBarItem(
  label: String,
  selected: Boolean,
  activeIcon: ImageVector,
  inactiveIcon: ImageVector,
  testTag: String,
  onClick: () -> Unit
) {
  val iconColor by animateColorAsState(
    targetValue = if (selected) PrimaryAction else TextSecondary,
    label = "iconColor"
  )
  val backgroundColor by animateColorAsState(
    targetValue = if (selected) SurfaceNeutral else Color.Transparent,
    label = "bgColor"
  )

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(backgroundColor)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = PrimaryAction),
        onClick = onClick
      )
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = if (selected) activeIcon else inactiveIcon,
        contentDescription = label,
        tint = iconColor,
        modifier = Modifier.size(19.dp)
      )
      if (selected) {
        Text(
          text = label,
          color = TextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
fun TripSubNavBar(
  selectedTab: TripTab,
  onTabSelected: (TripTab) -> Unit,
  modifier: Modifier = Modifier
) {
  val tabs = listOf(
    TripTab.OVERVIEW to ("Overview" to Icons.Default.ListAlt),
    TripTab.WALLET to ("Wallet" to Icons.Default.AccountBalanceWallet),
    TripTab.EXPENSES to ("Expenses" to Icons.Default.ReceiptLong),
    TripTab.ANALYTICS to ("Analytics" to Icons.Default.PieChart),
    TripTab.SETTLEMENT to ("Settle" to Icons.Default.Calculate),
    TripTab.MEMBERS to ("Members" to Icons.Default.Group),
    TripTab.SETTINGS to ("Settings" to Icons.Default.Settings)
  )

  val scrollState = rememberScrollState()

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = CardBackground,
    shadowElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .border(0.8.dp, BorderSubtle, RoundedCornerShape(20.dp))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(scrollState)
        .padding(horizontal = 6.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      tabs.forEach { (tab, pair) ->
        val isSelected = selectedTab == tab
        val (label, icon) = pair

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) PrimaryAction else SurfaceNeutral)
            .clickable { onTabSelected(tab) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("trip_tab_${tab.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = icon,
              contentDescription = label,
              tint = if (isSelected) PureWhite else TextPrimary,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = label,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) PureWhite else TextPrimary
            )
          }
        }
      }
    }
  }
}

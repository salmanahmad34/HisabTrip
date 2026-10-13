package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TripEntity
import com.example.domain.money.MoneyUtils
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.components.ScenicTripCover
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.HisabTripViewModel

@Composable
fun HomeScreen(
  viewModel: HisabTripViewModel,
  onCreateTripClick: () -> Unit,
  onJoinTripClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val profile by viewModel.currentProfile.collectAsState()
  val trips by viewModel.trips.collectAsState()
  val unreadNotifications by viewModel.unreadNotificationsCount.collectAsState()
  val isSyncing by viewModel.isSyncingTrips.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Completed

  val filteredTrips = trips.filter { trip ->
    if (selectedTab == 0) trip.status == "ACTIVE" else trip.status == "COMPLETED"
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MainBackground)
      .statusBarsPadding()
  ) {
    // Top Bar Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        MemberAvatar(
          avatarKey = profile?.avatarKey ?: "compass",
          name = profile?.displayName ?: "Traveler",
          size = 46.dp
        )
        Column {
          Text(
            text = "Welcome, ${profile?.displayName ?: "Traveler"}",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "@${profile?.username ?: "salman7307"}",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }

      // Top Bar Actions (Sync & Notifications)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Sync with Firestore button
        IconButton(
          onClick = { viewModel.fetchUserTrips() },
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(SurfaceNeutral)
            .border(0.8.dp, BorderSubtle, CircleShape)
            .testTag("sync_firestore_trips_button")
        ) {
          if (isSyncing) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = PrimaryAction,
              strokeWidth = 2.dp
            )
          } else {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sync trips from Firestore",
              tint = TextPrimary,
              modifier = Modifier.size(19.dp)
            )
          }
        }

        // Notification Bell
        Box {
          IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(SurfaceNeutral)
              .border(0.8.dp, BorderSubtle, CircleShape)
              .testTag("notification_bell_button")
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Notifications",
              tint = TextPrimary,
              modifier = Modifier.size(19.dp)
            )
          }
          if (unreadNotifications > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .size(16.dp)
                .clip(CircleShape)
                .background(StatusRed),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                color = PureWhite,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    if (isSyncing) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp),
        color = PrimaryAction,
        trackColor = SurfaceNeutral
      )
    }

    // Quick Action Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onCreateTripClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryAction,
          contentColor = PureWhite
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("create_trip_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Create Trip", fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }

      OutlinedButton(
        onClick = onJoinTripClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = TextPrimary
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .border(0.8.dp, BorderSubtle, RoundedCornerShape(14.dp))
          .background(CardBackground, RoundedCornerShape(14.dp))
          .testTag("join_trip_button")
      ) {
        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Join Trip", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Header with Grid View link
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "YOUR JOURNEYS",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = TextSecondary
      )
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceNeutral,
        modifier = Modifier.clickable { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.TRIPS_LIST) }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Grid View (${trips.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryAction
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = PrimaryAction,
            modifier = Modifier.size(12.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Active vs Completed Segmented Tab Row
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color.Transparent,
      contentColor = TextPrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = PrimaryAction,
          height = 2.5.dp
        )
      },
      divider = {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderSubtle)
        )
      },
      modifier = Modifier.padding(horizontal = 20.dp)
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = {
          Text(
            text = "Active Trips (${trips.count { it.status == "ACTIVE" }})",
            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
          )
        }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Text(
            text = "Completed (${trips.count { it.status == "COMPLETED" }})",
            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
          )
        }
      )
    }

    // Trips List Container with Floating Action Button to create a new trip group
    Box(modifier = Modifier.weight(1f)) {
      if (filteredTrips.isEmpty()) {
        EmptyStateView(
          title = if (selectedTab == 0) "No active trips" else "No completed trips",
          message = if (selectedTab == 0)
            "Create a trip or enter an invite code to begin tracking shared group expenses."
          else
            "Closed trips and finalized settlements will appear here.",
          icon = Icons.Default.FlightTakeoff,
          actionLabel = if (selectedTab == 0) "+ Create First Trip" else null,
          onAction = if (selectedTab == 0) onCreateTripClick else null,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(filteredTrips, key = { it.id }) { trip ->
            TripScenicCard(
              trip = trip,
              onClick = { viewModel.openTrip(trip.id) }
            )
          }
          item {
            Spacer(modifier = Modifier.height(100.dp)) // padding for FAB & bottom nav
          }
        }
      }

      // Floating Action Button to create a new trip group
      ExtendedFloatingActionButton(
        onClick = onCreateTripClick,
        containerColor = PrimaryAction,
        contentColor = PureWhite,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
        icon = {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create Trip Group",
            modifier = Modifier.size(20.dp)
          )
        },
        text = {
          Text(
            text = "New Trip",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = 80.dp)
          .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33191919))
          .testTag("home_create_trip_fab")
      )
    }
  }
}

@Composable
fun TripScenicCard(
  trip: TripEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CardBackground),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderSubtle, RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .testTag("trip_card_${trip.id}")
  ) {
    Column {
      // Scenic Cover Header
      ScenicTripCover(
        title = trip.name,
        dates = com.example.domain.util.DateUtils.formatDateRange(trip.startDate, trip.endDate),
        status = trip.status,
        height = 130.dp
      )

      Column(modifier = Modifier.padding(16.dp)) {
        if (trip.description.isNotEmpty()) {
          Text(
            text = trip.description,
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 1
          )
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Budget Metrics Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "TRIP BUDGET",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextTertiary
            )
            Text(
              text = MoneyUtils.formatPaise(trip.budgetMinor),
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "INVITE CODE",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextTertiary
            )
            Text(
              text = trip.id,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Tap to open ledger",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "View Trip",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Icon(
              imageVector = Icons.Default.ArrowForward,
              contentDescription = null,
              tint = TextPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }
  }
}

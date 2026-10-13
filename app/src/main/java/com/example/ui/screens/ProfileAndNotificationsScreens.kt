package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.HisabTripRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HisabTripLogo
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HisabTripViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// NOTIFICATIONS SCREEN
// ==========================================
@Composable
fun NotificationsScreen(
  viewModel: HisabTripViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }
  val notifications by viewModel.notifications.collectAsState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Graphite100)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Graphite900)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Notifications",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Graphite900
        )
      }

      if (notifications.any { !it.read }) {
        TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = Graphite700)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mark all read", fontSize = 12.sp, color = Graphite700)
          }
        }
      }
    }

    if (notifications.isEmpty()) {
      EmptyStateView(
        title = "No notifications",
        message = "You're all caught up! Trip invitations, expense approvals, and settlement receipts will appear here.",
        icon = Icons.Default.NotificationsNone
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(notifications, key = { it.id }) { notif ->
          val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.createdAt))

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (notif.read) PureWhite else Graphite100,
            modifier = Modifier
              .fillMaxWidth()
              .border(0.5.dp, Graphite200, RoundedCornerShape(14.dp))
              .clickable { viewModel.markNotificationRead(notif.id) }
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.Top
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .padding(top = 4.dp)
                  .clip(CircleShape)
                  .background(if (notif.read) Graphite300 else StatusGreen)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = notif.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Graphite900
                  )
                  Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = Graphite400
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = notif.body,
                  fontSize = 12.sp,
                  color = Graphite600,
                  lineHeight = 17.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

// ==========================================
// PROFILE SCREEN
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  viewModel: HisabTripViewModel,
  onNavigateToHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  val profile by viewModel.currentProfile.collectAsState()

  var displayNameInput by remember(profile) { mutableStateOf(profile?.displayName ?: "Salman") }
  var usernameInput by remember(profile) { mutableStateOf(profile?.username ?: "salman7307") }
  var selectedAvatar by remember(profile) { mutableStateOf(profile?.avatarKey ?: "compass") }
  var showSignOutConfirm by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Your Profile",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Graphite900
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Identity Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Graphite200, RoundedCornerShape(20.dp))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              MemberAvatar(avatarKey = selectedAvatar, name = displayNameInput, size = 56.dp)
              Column {
                Text(text = displayNameInput, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Graphite900)
                Text(text = "@$usernameInput", fontSize = 13.sp, color = Graphite500)
                Text(
                  text = "ID: ${profile?.userId ?: "usr_salman_7307"}",
                  fontSize = 11.sp,
                  color = Graphite400
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Graphite100)
            Spacer(modifier = Modifier.height(14.dp))

            // Avatar picker
            Text(
              text = "CHOOSE AVATAR",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite500,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              HisabTripRepository.PREDEFINED_AVATARS.forEach { av ->
                val isSelected = selectedAvatar == av
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Graphite900 else Graphite100)
                    .border(1.5.dp, if (isSelected) Graphite900 else Graphite200, CircleShape)
                    .clickable { selectedAvatar = av },
                  contentAlignment = Alignment.Center
                ) {
                  MemberAvatar(avatarKey = av, name = av, size = 32.dp)
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Name field
            OutlinedTextField(
              value = displayNameInput,
              onValueChange = { displayNameInput = it },
              label = { Text("Display Name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Graphite900,
                unfocusedBorderColor = Graphite300
              )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Username field
            OutlinedTextField(
              value = usernameInput,
              onValueChange = { usernameInput = it },
              label = { Text("Unique Username") },
              singleLine = true,
              prefix = { Text("@") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Graphite900,
                unfocusedBorderColor = Graphite300
              )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
              onClick = {
                viewModel.updateProfile(displayNameInput, usernameInput, selectedAvatar)
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Graphite900, contentColor = PureWhite),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Save Profile", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Google Account Status
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "GOOGLE ACCOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Graphite500)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(text = profile?.email ?: "mrsalman7307@gmail.com", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite900)
                Text(text = "Authenticated with Google Identity", fontSize = 11.sp, color = StatusGreen)
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Graphite100,
                modifier = Modifier.clickable { showSignOutConfirm = true }
              ) {
                Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusRed, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
              }
            }
          }
        }
      }

      // Preferences Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "APP PREFERENCES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Graphite500)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Currency", fontSize = 13.sp, color = Graphite800)
              Text("INR (₹)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Graphite900)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Language", fontSize = 13.sp, color = Graphite800)
              Text("English (India)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Graphite900)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Theme", fontSize = 13.sp, color = Graphite800)
              Text("Neutral Graphite", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Graphite900)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Storage & Security", fontSize = 13.sp, color = Graphite800)
              Text("Local Room DB (Encrypted)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
            }
          }
        }
      }

      // App Version & About
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          HisabTripLogo(size = 36.dp)
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = "HisabTrip v1.0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Graphite700)
          Text(
            text = "Expenses, contributions, and settlements — all in one place.",
            fontSize = 11.sp,
            color = Graphite400,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }

      item { Spacer(modifier = Modifier.height(80.dp)) }
    }
  }

  if (showSignOutConfirm) {
    AlertDialog(
      onDismissRequest = { showSignOutConfirm = false },
      title = { Text("Sign Out of HisabTrip") },
      text = { Text("Are you sure you want to sign out? Your cached trip data and ledger will remain secure on this device.") },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirm = false
            viewModel.showToast("Signed out session")
            viewModel.navigateTo(AppScreen.SPLASH)
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
        ) {
          Text("Sign Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
      }
    )
  }
}

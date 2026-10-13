package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserProfileEntity
import com.example.domain.money.MoneyUtils
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTripDialog(
  availableCompanions: List<UserProfileEntity>,
  onDismiss: () -> Unit,
  onCreate: (name: String, desc: String, start: String, end: String, budgetPaise: Long, memberIds: List<String>) -> Unit
) {
  var tripName by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var startDate by remember { mutableStateOf("15 Oct 2026") }
  var endDate by remember { mutableStateOf("22 Oct 2026") }
  var budgetInput by remember { mutableStateOf("25000") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val selectedMembers = remember { mutableStateListOf<String>() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 24.dp)
        .border(1.dp, Graphite200, RoundedCornerShape(24.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Graphite900),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Flight, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Create New Trip",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite900
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Graphite500)
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Trip Name
        OutlinedTextField(
          value = tripName,
          onValueChange = { tripName = it },
          label = { Text("Trip Name (e.g., Goa Beach Retreat)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_trip_name_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Trip Purpose or Destination (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dates
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("Start Date") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Graphite900,
              unfocusedBorderColor = Graphite300
            )
          )
          OutlinedTextField(
            value = endDate,
            onValueChange = { endDate = it },
            label = { Text("End Date") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Graphite900,
              unfocusedBorderColor = Graphite300
            )
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total Budget (INR)
        OutlinedTextField(
          value = budgetInput,
          onValueChange = { budgetInput = it },
          label = { Text("Trip Budget (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Graphite900) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_trip_budget_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Add Travelers
        Text(
          text = "INVITE TRAVEL COMPANIONS",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          color = Graphite500
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          availableCompanions.filter { it.userId != "usr_salman_7307" }.forEach { comp ->
            val isSelected = selectedMembers.contains(comp.userId)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) Graphite900 else Graphite100,
              modifier = Modifier
                .border(1.dp, if (isSelected) Graphite900 else Graphite200, RoundedCornerShape(12.dp))
                .clickable {
                  if (isSelected) selectedMembers.remove(comp.userId) else selectedMembers.add(comp.userId)
                }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                MemberAvatar(avatarKey = comp.avatarKey, name = comp.displayName, size = 22.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = comp.displayName,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (isSelected) PureWhite else Graphite800
                )
                if (isSelected) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            }
          }
        }

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(text = errorMessage!!, color = StatusRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = {
            if (tripName.trim().isEmpty()) {
              errorMessage = "Please enter a trip name"
              return@Button
            }
            val parsedPaise = MoneyUtils.parseToPaise(budgetInput)
            if (parsedPaise == null || parsedPaise < 0L) {
              errorMessage = "Please enter a valid budget amount"
              return@Button
            }
            onCreate(
              tripName.trim(),
              description.trim(),
              startDate.trim(),
              endDate.trim(),
              parsedPaise,
              selectedMembers.toList()
            )
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Graphite900,
            contentColor = PureWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("submit_create_trip_button")
        ) {
          Text("Create Trip Ledger", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

@Composable
fun JoinTripDialog(
  onDismiss: () -> Unit,
  onJoin: (code: String) -> Unit
) {
  var tripCode by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Graphite200, RoundedCornerShape(20.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Graphite900),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.GroupAdd, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Join Trip",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite900
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Graphite500)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Enter the 8-character trip code or invitation ID provided by the trip creator.",
          fontSize = 13.sp,
          color = Graphite500,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = tripCode,
          onValueChange = { tripCode = it },
          label = { Text("Trip Code (e.g. trip_a1b2c3d4)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("join_trip_code_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorMessage!!, color = StatusRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            if (tripCode.trim().isEmpty()) {
              errorMessage = "Please enter a trip code"
              return@Button
            }
            onJoin(tripCode.trim())
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Graphite900,
            contentColor = PureWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_join_trip_button")
        ) {
          Text("Join Trip", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

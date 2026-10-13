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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.local.TripMemberEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.HisabTripRepository
import com.example.domain.money.MoneyUtils
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.*
import com.example.ui.theme.StatusRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
  members: List<TripMemberEntity>,
  profiles: Map<String, UserProfileEntity>,
  currentUserId: String,
  onDismiss: () -> Unit,
  onSubmit: (
    title: String,
    amountPaise: Long,
    category: String,
    location: String,
    description: String,
    payers: Map<String, Long>,
    participants: Map<String, Long>,
    receiptNote: String?
  ) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var amountInput by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(HisabTripRepository.PREDEFINED_CATEGORIES.first()) }
  var location by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var receiptNote by remember { mutableStateOf("") }

  // Payer selection: default current user pays 100%
  var isMultiplePayers by remember { mutableStateOf(false) }
  var singlePayerId by remember { mutableStateOf(currentUserId) }
  val multiplePayersInput = remember { mutableStateMapOf<String, String>() }

  // Split mode: 0 = Equal, 1 = Custom Amount, 2 = Percentage
  var splitModeTab by remember { mutableStateOf(0) }
  val selectedParticipants = remember {
    mutableStateMapOf<String, Boolean>().apply {
      members.forEach { put(it.userId, true) }
    }
  }
  val customSharesInput = remember { mutableStateMapOf<String, String>() }
  val percentageSharesInput = remember { mutableStateMapOf<String, String>() }

  var errorMessage by remember { mutableStateOf<String?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .padding(vertical = 20.dp)
        .border(1.dp, Graphite200, RoundedCornerShape(24.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
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
              Icon(Icons.Default.Receipt, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Add Trip Expense",
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

        // Title
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Expense Title (e.g., Dinner at Fisherman's Wharf)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_title_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Amount (INR)
        OutlinedTextField(
          value = amountInput,
          onValueChange = { amountInput = it },
          label = { Text("Total Amount (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Graphite900) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_amount_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description / Notes
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Expense Description / Notes (Optional)") },
          maxLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_description_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Category Picker
        Text(
          text = "CATEGORY",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          color = Graphite500
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          HisabTripRepository.PREDEFINED_CATEGORIES.forEach { cat ->
            val isSelected = selectedCategory == cat
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) Graphite900 else Graphite100,
              modifier = Modifier
                .clickable { selectedCategory = cat }
                .border(1.dp, if (isSelected) Graphite900 else Graphite200, RoundedCornerShape(10.dp))
            ) {
              Text(
                text = cat,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PureWhite else Graphite700,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payer Section
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "WHO PAID?",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Graphite500
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Graphite100,
            modifier = Modifier.clickable { isMultiplePayers = !isMultiplePayers }
          ) {
            Text(
              text = if (isMultiplePayers) "Multiple Payers (Split)" else "Single Payer",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = Graphite900,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isMultiplePayers) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            members.forEach { m ->
              val isSelected = singlePayerId == m.userId
              val name = profiles[m.userId]?.displayName ?: m.userId
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Graphite900 else Graphite100,
                modifier = Modifier
                  .clickable { singlePayerId = m.userId }
                  .border(1.dp, if (isSelected) Graphite900 else Graphite200, RoundedCornerShape(12.dp))
                  .testTag("payer_chip_${m.userId}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  MemberAvatar(avatarKey = profiles[m.userId]?.avatarKey ?: "compass", name = name, size = 20.dp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) PureWhite else Graphite800
                  )
                }
              }
            }
          }
        } else {
          // Multiple payers input
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            members.forEach { m ->
              val name = profiles[m.userId]?.displayName ?: m.userId
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  MemberAvatar(avatarKey = profiles[m.userId]?.avatarKey ?: "compass", name = name, size = 22.dp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                OutlinedTextField(
                  value = multiplePayersInput[m.userId] ?: "",
                  onValueChange = { multiplePayersInput[m.userId] = it },
                  label = { Text("Paid (₹)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.width(130.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Graphite900,
                    unfocusedBorderColor = Graphite300
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Split Mode Selector
        Text(
          text = "SPLIT AMONG PARTICIPANTS",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Graphite500
        )
        Spacer(modifier = Modifier.height(6.dp))

        TabRow(
          selectedTabIndex = splitModeTab,
          containerColor = Graphite100,
          contentColor = Graphite900,
          modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
          Tab(
            selected = splitModeTab == 0,
            onClick = { splitModeTab = 0 },
            text = { Text("Equal", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = splitModeTab == 1,
            onClick = { splitModeTab = 1 },
            text = { Text("Exact (₹)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = splitModeTab == 2,
            onClick = { splitModeTab = 2 },
            text = { Text("Percent (%)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Participant Rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          members.forEach { m ->
            val name = profiles[m.userId]?.displayName ?: m.userId
            val isIncluded = selectedParticipants[m.userId] == true

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isIncluded) PureWhite else Graphite100)
                .border(0.5.dp, Graphite200, RoundedCornerShape(12.dp))
                .clickable {
                  selectedParticipants[m.userId] = !isIncluded
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isIncluded) Graphite900 else Graphite200),
                  contentAlignment = Alignment.Center
                ) {
                  if (isIncluded) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                MemberAvatar(avatarKey = profiles[m.userId]?.avatarKey ?: "compass", name = name, size = 26.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = name,
                  fontSize = 13.sp,
                  fontWeight = if (isIncluded) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (isIncluded) Graphite900 else Graphite400
                )
              }

              if (isIncluded) {
                when (splitModeTab) {
                  0 -> {
                    // Equal preview
                    val totalPaise = MoneyUtils.parseToPaise(amountInput) ?: 0L
                    val includedCount = selectedParticipants.count { it.value }
                    val share = if (includedCount > 0) totalPaise / includedCount else 0L
                    Text(
                      text = MoneyUtils.formatPaise(share),
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = Graphite800
                    )
                  }
                  1 -> {
                    // Exact custom amount
                    OutlinedTextField(
                      value = customSharesInput[m.userId] ?: "",
                      onValueChange = { customSharesInput[m.userId] = it },
                      label = { Text("Share ₹") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier.width(110.dp)
                    )
                  }
                  2 -> {
                    // Percentage
                    OutlinedTextField(
                      value = percentageSharesInput[m.userId] ?: "",
                      onValueChange = { percentageSharesInput[m.userId] = it },
                      label = { Text("%") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier.width(90.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Receipt memo / note
        OutlinedTextField(
          value = receiptNote,
          onValueChange = { receiptNote = it },
          label = { Text("Receipt Memo / Reference (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Graphite900,
            unfocusedBorderColor = Graphite300
          )
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(text = errorMessage!!, color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button
        Button(
          onClick = {
            if (title.trim().isEmpty()) {
              errorMessage = "Please enter an expense title"
              return@Button
            }
            val totalPaise = MoneyUtils.parseToPaise(amountInput)
            if (totalPaise == null || totalPaise <= 0L) {
              errorMessage = "Please enter a valid expense amount"
              return@Button
            }

            // Build payers map
            val payersMap = mutableMapOf<String, Long>()
            if (!isMultiplePayers) {
              payersMap[singlePayerId] = totalPaise
            } else {
              var payerSum = 0L
              for (m in members) {
                val input = multiplePayersInput[m.userId]?.trim() ?: ""
                if (input.isNotEmpty()) {
                  val p = MoneyUtils.parseToPaise(input) ?: 0L
                  if (p > 0L) {
                    payersMap[m.userId] = p
                    payerSum += p
                  }
                }
              }
              if (payerSum != totalPaise) {
                errorMessage = "Payer amounts sum to ${MoneyUtils.formatPaise(payerSum)}, which doesn't equal total ${MoneyUtils.formatPaise(totalPaise)}"
                return@Button
              }
            }

            // Build participants map
            val activeParticipantIds = selectedParticipants.filter { it.value }.map { it.key }
            if (activeParticipantIds.isEmpty()) {
              errorMessage = "At least one participant must be included"
              return@Button
            }

            val participantsMap = when (splitModeTab) {
              0 -> MoneyUtils.calculateEqualSplit(totalPaise, activeParticipantIds)
              1 -> {
                val map = mutableMapOf<String, Long>()
                var sum = 0L
                for (uid in activeParticipantIds) {
                  val p = MoneyUtils.parseToPaise(customSharesInput[uid] ?: "") ?: 0L
                  map[uid] = p
                  sum += p
                }
                if (sum != totalPaise) {
                  errorMessage = "Participant shares sum to ${MoneyUtils.formatPaise(sum)}, must equal ${MoneyUtils.formatPaise(totalPaise)}"
                  return@Button
                }
                map
              }
              2 -> {
                val pctMap = mutableMapOf<String, Double>()
                for (uid in activeParticipantIds) {
                  val pct = (percentageSharesInput[uid] ?: "").toDoubleOrNull() ?: 0.0
                  pctMap[uid] = pct
                }
                MoneyUtils.calculatePercentageSplit(totalPaise, pctMap)
              }
              else -> emptyMap()
            }

            onSubmit(
              title.trim(),
              totalPaise,
              selectedCategory,
              location.trim(),
              description.trim(),
              payersMap,
              participantsMap,
              receiptNote.takeIf { it.isNotBlank() }
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
            .testTag("submit_expense_button")
        ) {
          Text("Save Expense", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

@Composable
fun AddContributionDialog(
  members: List<TripMemberEntity>,
  profiles: Map<String, UserProfileEntity>,
  onDismiss: () -> Unit,
  onSubmit: (memberUserId: String, amountPaise: Long, note: String) -> Unit
) {
  var selectedMemberId by remember { mutableStateOf(members.firstOrNull()?.userId ?: "") }
  var amountInput by remember { mutableStateOf("") }
  var noteInput by remember { mutableStateOf("Upfront trip pool contribution") }
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
          Text(
            text = "Record Contribution",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Graphite900
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Graphite500)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "MEMBER",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Graphite500
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          members.forEach { m ->
            val name = profiles[m.userId]?.displayName ?: m.userId
            val isSelected = selectedMemberId == m.userId
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) Graphite900 else Graphite100,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedMemberId = m.userId }
                .border(1.dp, if (isSelected) Graphite900 else Graphite200, RoundedCornerShape(12.dp))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                MemberAvatar(avatarKey = profiles[m.userId]?.avatarKey ?: "compass", name = name, size = 24.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = name,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (isSelected) PureWhite else Graphite900
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = amountInput,
          onValueChange = { amountInput = it },
          label = { Text("Contribution Amount (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Graphite900) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contribution_amount_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = noteInput,
          onValueChange = { noteInput = it },
          label = { Text("Note / Mode (e.g. UPI, Cash)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorMessage!!, color = StatusRed, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            val paise = MoneyUtils.parseToPaise(amountInput)
            if (paise == null || paise <= 0L) {
              errorMessage = "Please enter a valid amount"
              return@Button
            }
            onSubmit(selectedMemberId, paise, noteInput.trim())
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Graphite900,
            contentColor = PureWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_contribution_button")
        ) {
          Text("Record in Trip Wallet", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun RecordSettlementDialog(
  fromUserName: String,
  toUserName: String,
  amountPaise: Long,
  onDismiss: () -> Unit,
  onConfirm: (note: String) -> Unit
) {
  var noteInput by remember { mutableStateOf("Paid via UPI") }

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
          Text(
            text = "Record Settlement",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Graphite900
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Graphite500)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Graphite100,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = MoneyUtils.formatPaise(amountPaise),
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = StatusGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "$fromUserName pays $toUserName",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = Graphite800
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = noteInput,
          onValueChange = { noteInput = it },
          label = { Text("Payment Reference / Note") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = { onConfirm(noteInput.trim()) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Graphite900,
            contentColor = PureWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("confirm_settlement_button")
        ) {
          Text("Mark as Settled", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.HisabTripRepository
import com.example.domain.money.MoneyUtils
import com.example.domain.settlement.ProposedTransfer
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MetricCard
import com.example.ui.components.ScenicTripCover
import com.example.ui.components.StatusBadge
import com.example.ui.components.TripSubNavBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HisabTripViewModel
import com.example.ui.viewmodel.TripTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripDashboardScreen(
  viewModel: HisabTripViewModel,
  onBackToHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBackToHome() }

  val context = LocalContext.current
  val trip by viewModel.selectedTrip.collectAsState()
  val selectedTab by viewModel.selectedTripTab.collectAsState()
  val members by viewModel.selectedTripMembers.collectAsState()
  val allProfiles by viewModel.allProfiles.collectAsState()
  val expenses by viewModel.selectedTripExpenses.collectAsState()
  val payers by viewModel.selectedTripPayers.collectAsState()
  val participants by viewModel.selectedTripParticipants.collectAsState()
  val contributions by viewModel.selectedTripContributions.collectAsState()
  val settlements by viewModel.selectedTripSettlements.collectAsState()
  val settlementResult by viewModel.settlementResult.collectAsState()
  val auditLogs by viewModel.selectedTripAuditLogs.collectAsState()

  val profilesMap = remember(allProfiles) { allProfiles.associateBy { it.userId } }

  // Dialog States
  var showAddExpense by remember { mutableStateOf(false) }
  var showAddContribution by remember { mutableStateOf(false) }
  var activeSettlementTransfer by remember { mutableStateOf<ProposedTransfer?>(null) }
  var showAddMemberDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var exportText by remember { mutableStateOf("") }
  var selectedExpenseDetail by remember { mutableStateOf<ExpenseEntity?>(null) }

  if (trip == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Loading Trip...", color = Graphite500)
    }
    return
  }

  val currentTrip = trip!!
  val isCreator = members.any { it.userId == HisabTripRepository.CURRENT_USER_ID && it.role == "CREATOR" }
  val isManagerOrCreator = members.any {
    it.userId == HisabTripRepository.CURRENT_USER_ID && it.role in listOf("CREATOR", "MANAGER")
  }

  // Financial calculations
  val totalBudget = currentTrip.budgetMinor
  val totalSpent = expenses.filter { it.status == "APPROVED" }.sumOf { it.amountMinor }
  val totalCollected = contributions.sumOf { it.amountMinor }
  val remainingBudget = totalBudget - totalSpent

  val userBalance = settlementResult?.memberBalances?.find { it.userId == HisabTripRepository.CURRENT_USER_ID }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding(),
    floatingActionButton = {
      if (selectedTab == TripTab.EXPENSES && currentTrip.status == "ACTIVE") {
        FloatingActionButton(
          onClick = { showAddExpense = true },
          containerColor = Graphite900,
          contentColor = PureWhite,
          shape = CircleShape,
          modifier = Modifier.testTag("fab_add_expense")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Expense")
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Top Trip Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          IconButton(
            onClick = onBackToHome,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Graphite100)
              .testTag("back_to_home_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Graphite900,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = currentTrip.name,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite900,
              maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Code: ${currentTrip.id}",
                fontSize = 11.sp,
                color = Graphite500
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Code",
                tint = Graphite400,
                modifier = Modifier
                  .size(12.dp)
                  .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Trip Code", currentTrip.id))
                    viewModel.showToast("Trip code copied!")
                  }
              )
            }
          }
        }

        StatusBadge(status = currentTrip.status)
      }

      // Sub Nav Bar (Overview, Wallet, Expenses, Settle, Members, Settings)
      TripSubNavBar(
        selectedTab = selectedTab,
        onTabSelected = { viewModel.setTripTab(it) }
      )

      // Tab Content Body
      Box(modifier = Modifier.weight(1f)) {
        when (selectedTab) {
          TripTab.OVERVIEW -> {
            OverviewTab(
              trip = currentTrip,
              totalBudget = totalBudget,
              totalSpent = totalSpent,
              totalCollected = totalCollected,
              remainingBudget = remainingBudget,
              userNetPaise = userBalance?.netBalancePaise ?: 0L,
              members = members,
              profiles = profilesMap,
              onAddExpense = { showAddExpense = true },
              onWalletClick = { viewModel.setTripTab(TripTab.WALLET) },
              onSettleClick = { viewModel.setTripTab(TripTab.SETTLEMENT) },
              onMembersClick = { viewModel.setTripTab(TripTab.MEMBERS) },
              onAnalyticsClick = { viewModel.setTripTab(TripTab.ANALYTICS) }
            )
          }

          TripTab.WALLET -> {
            WalletTab(
              totalBudget = totalBudget,
              totalCollected = totalCollected,
              totalSpent = totalSpent,
              members = members,
              profiles = profilesMap,
              contributions = contributions,
              isManager = isManagerOrCreator,
              onAddContribution = { showAddContribution = true }
            )
          }

          TripTab.EXPENSES -> {
            ExpensesTab(
              expenses = expenses,
              payers = payers,
              participants = participants,
              profiles = profilesMap,
              isManager = isManagerOrCreator,
              onAddExpense = { showAddExpense = true },
              onExpenseClick = { selectedExpenseDetail = it },
              onApprove = { viewModel.approveExpense(it.id) },
              onReject = { viewModel.rejectExpense(it.id, "Rejected by manager") },
              onAnalyticsClick = { viewModel.setTripTab(TripTab.ANALYTICS) }
            )
          }

          TripTab.ANALYTICS -> {
            ExpenseAnalyticsTabContent(
              trip = currentTrip,
              expenses = expenses,
              payers = payers,
              participants = participants,
              members = members,
              profiles = profilesMap
            )
          }

          TripTab.SETTLEMENT -> {
            SettlementTab(
              settlementResult = settlementResult,
              recordedSettlements = settlements,
              onRecordPaid = { transfer -> activeSettlementTransfer = transfer }
            )
          }

          TripTab.MEMBERS -> {
            MembersTab(
              members = members,
              profiles = profilesMap,
              memberBalances = settlementResult?.memberBalances ?: emptyList(),
              isCreator = isCreator,
              onAddMember = { showAddMemberDialog = true },
              onRemoveMember = { viewModel.removeMember(it) },
              onUpdateRole = { uid, role -> viewModel.updateMemberRole(uid, role) }
            )
          }

          TripTab.SETTINGS -> {
            SettingsTab(
              trip = currentTrip,
              isCreator = isCreator,
              auditLogs = auditLogs,
              onUpdateBudget = { newBudget, recalculate ->
                viewModel.updateBudget(newBudget, recalculate)
              },
              onToggleTripStatus = { newStatus ->
                viewModel.updateTripStatus(newStatus)
              },
              onExport = {
                viewModel.getExportSummary { text ->
                  exportText = text
                  showExportDialog = true
                }
              }
            )
          }
        }
      }
    }
  }

  // --- Modals & Sheets ---

  if (showAddExpense) {
    AddExpenseDialog(
      members = members,
      profiles = profilesMap,
      currentUserId = HisabTripRepository.CURRENT_USER_ID,
      onDismiss = { showAddExpense = false },
      onSubmit = { title, amt, cat, loc, desc, payMap, partMap, note ->
        viewModel.createExpense(title, amt, cat, loc, desc, payMap, partMap, note) {
          showAddExpense = false
        }
      }
    )
  }

  if (showAddContribution) {
    AddContributionDialog(
      members = members,
      profiles = profilesMap,
      onDismiss = { showAddContribution = false },
      onSubmit = { uid, amt, note ->
        viewModel.recordContribution(uid, amt, note)
        showAddContribution = false
      }
    )
  }

  if (activeSettlementTransfer != null) {
    val transfer = activeSettlementTransfer!!
    RecordSettlementDialog(
      fromUserName = transfer.fromUserName,
      toUserName = transfer.toUserName,
      amountPaise = transfer.amountPaise,
      onDismiss = { activeSettlementTransfer = null },
      onConfirm = { note ->
        viewModel.recordSettlementPayment(
          fromUserId = transfer.fromUserId,
          toUserId = transfer.toUserId,
          amountPaise = transfer.amountPaise,
          note = note
        )
        activeSettlementTransfer = null
      }
    )
  }

  if (showAddMemberDialog) {
    AddMemberBottomSheet(
      tripId = currentTrip.id,
      currentMembers = members,
      onDismiss = { showAddMemberDialog = false },
      onLookupEmail = { email -> viewModel.lookupUserByEmail(email) },
      onAddUser = { userId, role ->
        viewModel.addMember(userId, role)
        showAddMemberDialog = false
      }
    )
  }

  if (showExportDialog) {
    ExportReportDialog(
      reportText = exportText,
      onDismiss = { showExportDialog = false },
      onCopy = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("HisabTrip Export", exportText))
        viewModel.showToast("Report copied to clipboard!")
      }
    )
  }

  if (selectedExpenseDetail != null) {
    ExpenseDetailDialog(
      expense = selectedExpenseDetail!!,
      payers = payers.filter { it.expenseId == selectedExpenseDetail!!.id },
      participants = participants.filter { it.expenseId == selectedExpenseDetail!!.id },
      profiles = profilesMap,
      isManager = isManagerOrCreator,
      onDismiss = { selectedExpenseDetail = null },
      onApprove = {
        viewModel.approveExpense(selectedExpenseDetail!!.id)
        selectedExpenseDetail = null
      },
      onReject = { reason ->
        viewModel.rejectExpense(selectedExpenseDetail!!.id, reason)
        selectedExpenseDetail = null
      },
      onDelete = {
        viewModel.deleteExpense(selectedExpenseDetail!!.id)
        selectedExpenseDetail = null
      }
    )
  }
}

// ==========================================
// TAB 1: OVERVIEW
// ==========================================
@Composable
private fun OverviewTab(
  trip: TripEntity,
  totalBudget: Long,
  totalSpent: Long,
  totalCollected: Long,
  remainingBudget: Long,
  userNetPaise: Long,
  members: List<TripMemberEntity>,
  profiles: Map<String, UserProfileEntity>,
  onAddExpense: () -> Unit,
  onWalletClick: () -> Unit,
  onSettleClick: () -> Unit,
  onMembersClick: () -> Unit,
  onAnalyticsClick: () -> Unit
) {
  val isOverBudget = remainingBudget < 0L

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Scenic Hero Cover Header
    item {
      ScenicTripCover(
        title = trip.name,
        dates = com.example.domain.util.DateUtils.formatDateRange(trip.startDate, trip.endDate),
        status = trip.status,
        height = 140.dp
      )
    }

    // Personal Balance Hero Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Graphite900),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          Text(
            text = "YOUR TRIP BALANCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Graphite400,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(6.dp))

          val balanceText = when {
            userNetPaise > 0L -> "You receive " + MoneyUtils.formatPaise(userNetPaise)
            userNetPaise < 0L -> "You owe " + MoneyUtils.formatPaise(-userNetPaise)
            else -> "All Settled Up"
          }
          val balanceColor = when {
            userNetPaise > 0L -> StatusGreen
            userNetPaise < 0L -> StatusRed
            else -> PureWhite
          }

          Text(
            text = balanceText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = balanceColor
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onAddExpense,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = Graphite900),
              modifier = Modifier.weight(1f)
            ) {
              Text("+ Add Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = onSettleClick,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
              modifier = Modifier
                .weight(1f)
                .border(1.dp, Graphite700, RoundedCornerShape(12.dp))
            ) {
              Text("Settle Up", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }

    // Budget Metric Cards Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Total Budget",
          value = MoneyUtils.formatPaise(totalBudget),
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Approved Spend",
          value = MoneyUtils.formatPaise(totalSpent),
          valueColor = if (isOverBudget) StatusRed else Graphite900,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Collected Kitty",
          value = MoneyUtils.formatPaise(totalCollected),
          valueColor = StatusGreen,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = if (isOverBudget) "Over Budget" else "Remaining",
          value = MoneyUtils.formatPaise(kotlin.math.abs(remainingBudget)),
          valueColor = if (isOverBudget) StatusRed else Graphite900,
          subtitle = if (isOverBudget) "Exceeded trip target" else null,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Budget Progress Bar
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Budget Utilization", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite700)
            val pct = if (totalBudget > 0L) (totalSpent.toFloat() / totalBudget.toFloat() * 100f).toInt() else 0
            Text(text = "$pct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isOverBudget) StatusRed else Graphite900)
          }
          Spacer(modifier = Modifier.height(8.dp))
          val progressFraction = if (totalBudget > 0L) (totalSpent.toFloat() / totalBudget.toFloat()).coerceIn(0f, 1f) else 0f
          LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(CircleShape),
            color = if (isOverBudget) StatusRed else Graphite900,
            trackColor = Graphite100
          )
        }
      }
    }

    // Travelers Roster Preview
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
          .clickable { onMembersClick() }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = "Travel Companions (${members.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Graphite900)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
              members.take(5).forEach { m ->
                MemberAvatar(
                  avatarKey = profiles[m.userId]?.avatarKey ?: "compass",
                  name = profiles[m.userId]?.displayName ?: m.userId,
                  size = 30.dp
                )
              }
            }
          }
          Text(text = "Manage →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite900)
        }
      }
    }

    // Analytics & D3 Charts Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
          .clickable { onAnalyticsClick() }
          .testTag("overview_analytics_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Graphite100),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = "Analytics",
                tint = Graphite900,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "Expense Analytics & D3 Charts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Graphite900
              )
              Text(
                text = "Interactive breakdown by category and member",
                fontSize = 11.sp,
                color = Graphite500
              )
            }
          }
          Text(text = "Explore →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite900)
        }
      }
    }

    item { Spacer(modifier = Modifier.height(30.dp)) }
  }
}

// ==========================================
// TAB 2: WALLET / CONTRIBUTIONS
// ==========================================
@Composable
private fun WalletTab(
  totalBudget: Long,
  totalCollected: Long,
  totalSpent: Long,
  members: List<TripMemberEntity>,
  profiles: Map<String, UserProfileEntity>,
  contributions: List<com.example.data.local.ContributionEntity>,
  isManager: Boolean,
  onAddContribution: () -> Unit
) {
  val remainingKitty = totalCollected - totalSpent

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Graphite900),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(text = "CENTRAL TRIP KITTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Graphite400)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = MoneyUtils.formatPaise(totalCollected), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PureWhite)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Available in pool: ${MoneyUtils.formatPaise(remainingKitty)} (Spent: ${MoneyUtils.formatPaise(totalSpent)})",
            fontSize = 12.sp,
            color = Graphite300
          )

          if (isManager) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = onAddContribution,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = Graphite900),
              modifier = Modifier.fillMaxWidth().testTag("record_contribution_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Record Member Contribution", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Member Contribution Targets & Status
    item {
      Text(text = "MEMBER TARGETS & RECEIVED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite500)
    }

    items(members, key = { it.userId }) { m ->
      val memberContributions = contributions.filter { it.userId == m.userId }.sumOf { it.amountMinor }
      val target = m.targetMinor
      val name = profiles[m.userId]?.displayName ?: m.userId

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              MemberAvatar(avatarKey = profiles[m.userId]?.avatarKey ?: "compass", name = name, size = 32.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite900)
                Text(text = "Target: ${MoneyUtils.formatPaise(target)}", fontSize = 11.sp, color = Graphite500)
              }
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = MoneyUtils.formatPaise(memberContributions),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (memberContributions >= target && target > 0) StatusGreen else Graphite900
              )
              Text(
                text = if (memberContributions >= target && target > 0) "Complete" else "Pending",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (memberContributions >= target && target > 0) StatusGreen else StatusAmber
              )
            }
          }
        }
      }
    }

    // Auditable contribution history
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Text(text = "CONTRIBUTION HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite500)
    }

    if (contributions.isEmpty()) {
      item {
        EmptyStateView(
          title = "No contributions yet",
          message = "Record upfront funds collected from travelers into the central trip wallet.",
          icon = Icons.Default.AccountBalanceWallet
        )
      }
    } else {
      items(contributions, key = { it.id }) { c ->
        val name = profiles[c.userId]?.displayName ?: c.userId
        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(c.recordedAt))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PureWhite,
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Graphite200, RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Graphite900)
              Text(text = if (c.note.isNotEmpty()) "${c.note} • $dateStr" else dateStr, fontSize = 11.sp, color = Graphite500)
            }
            Text(text = "+${MoneyUtils.formatPaise(c.amountMinor)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StatusGreen)
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(40.dp)) }
  }
}

// ==========================================
// TAB 3: EXPENSES
// ==========================================
@Composable
private fun ExpensesTab(
  expenses: List<ExpenseEntity>,
  payers: List<com.example.data.local.ExpensePayerEntity>,
  participants: List<com.example.data.local.ExpenseParticipantEntity>,
  profiles: Map<String, UserProfileEntity>,
  isManager: Boolean,
  onAddExpense: () -> Unit,
  onExpenseClick: (ExpenseEntity) -> Unit,
  onApprove: (ExpenseEntity) -> Unit,
  onReject: (ExpenseEntity) -> Unit,
  onAnalyticsClick: () -> Unit
) {
  var filterCategory by remember { mutableStateOf("All") }
  var filterStatus by remember { mutableStateOf("All") }

  val filtered = expenses.filter { e ->
    (filterCategory == "All" || e.category == filterCategory) &&
      (filterStatus == "All" || e.status == filterStatus)
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(text = "Trip Expenses (${expenses.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Graphite900)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Graphite100,
            modifier = Modifier
              .clickable { onAnalyticsClick() }
              .testTag("expenses_view_analytics_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = "Analytics",
                tint = Graphite900,
                modifier = Modifier.size(13.dp)
              )
              Text("Charts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Graphite900)
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf("All", "APPROVED", "PENDING").forEach { status ->
            val isSelected = filterStatus == status
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) Graphite900 else Graphite100,
              modifier = Modifier.clickable { filterStatus = status }
            ) {
              Text(
                text = if (status == "APPROVED") "Approved" else if (status == "PENDING") "Pending" else "All",
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PureWhite else Graphite700,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        EmptyStateView(
          title = "No expenses logged",
          message = "Tap the + button to record food, stays, tickets, or transport.",
          icon = Icons.Default.ReceiptLong,
          actionLabel = "+ Add First Expense",
          onAction = onAddExpense
        )
      }
    } else {
      items(filtered, key = { it.id }) { exp ->
        val expPayers = payers.filter { it.expenseId == exp.id }
        val payerNames = expPayers.joinToString(", ") { p ->
          profiles[p.userId]?.displayName ?: p.userId
        }
        val expParticipantsCount = participants.count { it.expenseId == exp.id }
        val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(exp.expenseDate))

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
            .clickable { onExpenseClick(exp) }
            .testTag("expense_item_${exp.id}")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = exp.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Graphite900)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${exp.category} • $dateStr",
                  fontSize = 11.sp,
                  color = Graphite500
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = MoneyUtils.formatPaise(exp.amountMinor),
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = Graphite900
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(status = exp.status)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Graphite100, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Paid by: ${if (payerNames.isEmpty()) "Self" else payerNames}",
                fontSize = 11.sp,
                color = Graphite700,
                maxLines = 1
              )
              Text(
                text = "Split with $expParticipantsCount members",
                fontSize = 11.sp,
                color = Graphite500
              )
            }

            // If pending and current user is manager, quick actions
            if (exp.status == "PENDING" && isManager) {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { onApprove(exp) },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = PureWhite),
                  modifier = Modifier.weight(1f).height(36.dp)
                ) {
                  Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = { onReject(exp) },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                  modifier = Modifier.weight(1f).height(36.dp).border(1.dp, StatusRed, RoundedCornerShape(8.dp))
                ) {
                  Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(80.dp)) }
  }
}

// ==========================================
// TAB 4: SETTLEMENT
// ==========================================
@Composable
private fun SettlementTab(
  settlementResult: com.example.domain.settlement.SettlementResult?,
  recordedSettlements: List<com.example.data.local.SettlementEntity>,
  onRecordPaid: (ProposedTransfer) -> Unit
) {
  val transfers = settlementResult?.transfers ?: emptyList()
  val totalOutstanding = transfers.sumOf { it.amountPaise }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Total settlement card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Graphite900),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(text = "TOTAL TO SETTLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Graphite400)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = MoneyUtils.formatPaise(totalOutstanding),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = if (totalOutstanding == 0L) StatusGreen else PureWhite
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (totalOutstanding == 0L) "All accounts balanced to ₹0.00" else "${transfers.size} optimized transfers needed to clear all debts",
            fontSize = 12.sp,
            color = Graphite300
          )
        }
      }
    }

    item {
      Text(text = "WHO PAYS WHOM (OPTIMIZED TRANSFERS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite500)
    }

    if (transfers.isEmpty()) {
      item {
        EmptyStateView(
          title = "Zero debts outstanding",
          message = "All members are completely even. As soon as new expenses are approved, optimized transfers appear here automatically.",
          icon = Icons.Default.DoneAll
        )
      }
    } else {
      items(transfers) { transfer ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${transfer.fromUserName} pays ${transfer.toUserName}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Graphite900
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = MoneyUtils.formatPaise(transfer.amountPaise),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen
              )
            }

            Button(
              onClick = { onRecordPaid(transfer) },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Graphite900, contentColor = PureWhite),
              modifier = Modifier.testTag("mark_paid_${transfer.fromUserId}_${transfer.toUserId}")
            ) {
              Text("Mark Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Past recorded payments
    if (recordedSettlements.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "SETTLEMENT PAYMENT HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite500)
      }

      items(recordedSettlements, key = { it.id }) { s ->
        val dateStr = s.paidAt?.let { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(it)) } ?: ""
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PureWhite,
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Graphite200, RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Paid: ${MoneyUtils.formatPaise(s.amountMinor)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusGreen)
              Text(text = "${s.paymentNote ?: "Direct settlement"} • $dateStr", fontSize = 11.sp, color = Graphite500)
            }
            StatusBadge(status = "SETTLED")
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(40.dp)) }
  }
}

// ==========================================
// TAB 5: MEMBERS
// ==========================================
@Composable
private fun MembersTab(
  members: List<TripMemberEntity>,
  profiles: Map<String, UserProfileEntity>,
  memberBalances: List<com.example.domain.settlement.MemberBalance>,
  isCreator: Boolean,
  onAddMember: () -> Unit,
  onRemoveMember: (String) -> Unit,
  onUpdateRole: (String, String) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "Trip Roster (${members.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Graphite900)
        if (isCreator) {
          Button(
            onClick = onAddMember,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Graphite900, contentColor = PureWhite),
            modifier = Modifier.testTag("add_traveler_button")
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Traveler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    items(members, key = { it.userId }) { m ->
      val p = profiles[m.userId]
      val name = p?.displayName ?: m.userId
      val bal = memberBalances.find { it.userId == m.userId }

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              MemberAvatar(avatarKey = p?.avatarKey ?: "compass", name = name, size = 38.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite900)
                Text(text = "@${p?.username ?: ""} • Role: ${m.role}", fontSize = 11.sp, color = Graphite500)
              }
            }

            if (bal != null) {
              val net = bal.netBalancePaise
              val netColor = if (net > 0) StatusGreen else if (net < 0) StatusRed else Graphite500
              val netText = if (net > 0) "+${MoneyUtils.formatPaise(net)}" else if (net < 0) "-${MoneyUtils.formatPaise(-net)}" else "₹0"
              Text(text = netText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = netColor)
            }
          }

          if (bal != null) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Graphite100, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = "Paid: ${MoneyUtils.formatPaise(bal.totalExpensesPaidPaise)}", fontSize = 11.sp, color = Graphite600)
              Text(text = "Share: ${MoneyUtils.formatPaise(bal.totalExpenseSharePaise)}", fontSize = 11.sp, color = Graphite600)
              Text(text = "Kitty: ${MoneyUtils.formatPaise(bal.totalContributionsPaidPaise)}", fontSize = 11.sp, color = Graphite600)
            }
          }

          // Creator controls for roles & removal (except self)
          if (isCreator && m.userId != HisabTripRepository.CURRENT_USER_ID) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(
                onClick = {
                  val newRole = if (m.role == "MANAGER") "MEMBER" else "MANAGER"
                  onUpdateRole(m.userId, newRole)
                }
              ) {
                Text(if (m.role == "MANAGER") "Demote to Member" else "Make Manager", fontSize = 11.sp)
              }
              TextButton(
                onClick = { onRemoveMember(m.userId) }
              ) {
                Text("Remove", fontSize = 11.sp, color = StatusRed)
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(40.dp)) }
  }
}

// ==========================================
// TAB 6: SETTINGS
// ==========================================
@Composable
private fun SettingsTab(
  trip: TripEntity,
  isCreator: Boolean,
  auditLogs: List<com.example.data.local.AuditLogEntity>,
  onUpdateBudget: (newBudgetPaise: Long, recalculate: Boolean) -> Unit,
  onToggleTripStatus: (newStatus: String) -> Unit,
  onExport: () -> Unit
) {
  var showBudgetDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(text = "TRIP CONTROLS & EXPORTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite500)
    }

    // Export Action Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
          .clickable { onExport() }
          .testTag("export_report_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Graphite100),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.FileDownload, contentDescription = null, tint = Graphite900, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(text = "Export Ledger Report", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite900)
              Text(text = "Generate CSV and text summary", fontSize = 11.sp, color = Graphite500)
            }
          }
          Text(text = "Export →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite900)
        }
      }
    }

    if (isCreator) {
      // Edit Budget
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
            .clickable { showBudgetDialog = true }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Edit Trip Budget", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Graphite900)
              Text(text = "Current: ${MoneyUtils.formatPaise(trip.budgetMinor)}", fontSize = 11.sp, color = Graphite500)
            }
            Icon(Icons.Default.Edit, contentDescription = null, tint = Graphite700, modifier = Modifier.size(18.dp))
          }
        }
      }

      // Complete or Reopen Trip
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Graphite200, RoundedCornerShape(16.dp))
            .clickable {
              val nextStatus = if (trip.status == "ACTIVE") "COMPLETED" else "ACTIVE"
              onToggleTripStatus(nextStatus)
            }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = if (trip.status == "ACTIVE") "Mark Trip as Completed" else "Reopen Completed Trip",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Graphite900
              )
              Text(
                text = if (trip.status == "ACTIVE") "Freeze mutations and finalize balances" else "Allow new expenses and contributions",
                fontSize = 11.sp,
                color = Graphite500
              )
            }
            StatusBadge(status = trip.status)
          }
        }
      }
    }

    // Audit Log Roster
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Text(text = "TRANSACTION AUDIT LOG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Graphite500)
    }

    if (auditLogs.isEmpty()) {
      item {
        Text("No audit logs recorded yet.", fontSize = 12.sp, color = Graphite400)
      }
    } else {
      items(auditLogs.take(15), key = { it.id }) { log ->
        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.createdAt))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = PureWhite,
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Graphite200, RoundedCornerShape(10.dp))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Graphite800)
              Text(text = dateStr, fontSize = 10.sp, color = Graphite400)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = log.details, fontSize = 11.sp, color = Graphite600)
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(40.dp)) }
  }

  if (showBudgetDialog) {
    EditBudgetDialog(
      currentBudgetPaise = trip.budgetMinor,
      onDismiss = { showBudgetDialog = false },
      onConfirm = { newBudget, recalc ->
        onUpdateBudget(newBudget, recalc)
        showBudgetDialog = false
      }
    )
  }
}

// Dialogs: EditBudget, AddMember, Export, ExpenseDetail

@Composable
fun EditBudgetDialog(
  currentBudgetPaise: Long,
  onDismiss: () -> Unit,
  onConfirm: (newBudgetPaise: Long, recalculate: Boolean) -> Unit
) {
  var budgetInput by remember { mutableStateOf((currentBudgetPaise / 100L).toString()) }
  var recalculateTargets by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Update Total Trip Budget") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = budgetInput,
          onValueChange = { budgetInput = it },
          label = { Text("Budget (₹)") },
          singleLine = true,
          prefix = { Text("₹ ") }
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { recalculateTargets = !recalculateTargets }
        ) {
          androidx.compose.material3.Checkbox(
            checked = recalculateTargets,
            onCheckedChange = { recalculateTargets = it }
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Recalculate member contribution targets evenly", fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val p = MoneyUtils.parseToPaise(budgetInput)
          if (p != null && p >= 0L) {
            onConfirm(p, recalculateTargets)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Graphite900)
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberBottomSheet(
  tripId: String,
  currentMembers: List<TripMemberEntity>,
  onDismiss: () -> Unit,
  onLookupEmail: suspend (String) -> Result<UserProfileEntity?>,
  onAddUser: (userId: String, role: String) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = androidx.compose.runtime.rememberCoroutineScope()

  var emailInput by remember { mutableStateOf("") }
  var selectedRole by remember { mutableStateOf("MEMBER") }
  var isSearching by remember { mutableStateOf(false) }
  var foundUser by remember { mutableStateOf<UserProfileEntity?>(null) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PureWhite,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 12.dp)
          .size(width = 44.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Graphite300)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp)
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
              .size(38.dp)
              .clip(CircleShape)
              .background(Graphite900),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PersonAdd,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Add Member to Trip",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite900
            )
            Text(
              text = "Invite travel companions by email address",
              fontSize = 12.sp,
              color = Graphite500
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Graphite500)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Email Input Field
      Text(
        text = "TRAVELER EMAIL",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = Graphite500
      )
      Spacer(modifier = Modifier.height(6.dp))

      OutlinedTextField(
        value = emailInput,
        onValueChange = {
          emailInput = it
          foundUser = null
          statusMessage = null
          isError = false
        },
        label = { Text("Email (e.g. companion@example.com)") },
        placeholder = { Text("Enter registered email") },
        leadingIcon = {
          Icon(Icons.Default.Email, contentDescription = null, tint = Graphite500)
        },
        trailingIcon = {
          if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Graphite900)
          } else {
            IconButton(
              onClick = {
                val cleanEmail = emailInput.trim()
                if (cleanEmail.isEmpty()) {
                  isError = true
                  statusMessage = "Please enter an email address"
                  return@IconButton
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                  isError = true
                  statusMessage = "Invalid email format. Please check and try again."
                  return@IconButton
                }
                isSearching = true
                statusMessage = null
                isError = false
                scope.launch {
                  val result = onLookupEmail(cleanEmail)
                  isSearching = false
                  result.onSuccess { user ->
                    if (user == null) {
                      isError = true
                      foundUser = null
                      statusMessage = "No user found with email '$cleanEmail'. Make sure they have registered in HisabTrip."
                    } else if (currentMembers.any { it.userId == user.userId }) {
                      isError = true
                      foundUser = user
                      statusMessage = "${user.displayName} is already a member of this trip."
                    } else {
                      isError = false
                      foundUser = user
                      statusMessage = "User found in Firestore directory!"
                    }
                  }.onFailure { err ->
                    isError = true
                    foundUser = null
                    statusMessage = err.localizedMessage ?: "Failed to lookup user"
                  }
                }
              },
              modifier = Modifier.testTag("lookup_user_by_email_button")
            ) {
              Icon(Icons.Default.Search, contentDescription = "Search", tint = Graphite900)
            }
          }
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
          keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("member_email_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Graphite900,
          unfocusedBorderColor = Graphite300
        )
      )

      // Lookup Status or Error Feedback
      if (statusMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = statusMessage!!,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = if (isError) StatusRed else StatusGreen,
          modifier = Modifier.testTag("lookup_status_message")
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // User Card Preview if found
      if (foundUser != null) {
        val user = foundUser!!
        val isAlreadyMember = currentMembers.any { it.userId == user.userId }

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = if (isAlreadyMember) Graphite100 else PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = if (isAlreadyMember) 1.dp else 1.5.dp,
              color = if (isAlreadyMember) Graphite300 else Graphite900,
              shape = RoundedCornerShape(16.dp)
            )
            .testTag("found_user_card")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              MemberAvatar(avatarKey = user.avatarKey, name = user.displayName, size = 42.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = user.displayName,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Graphite900
                )
                Text(
                  text = user.email,
                  fontSize = 12.sp,
                  color = Graphite500
                )
                Text(
                  text = "@${user.username}",
                  fontSize = 11.sp,
                  color = Graphite400
                )
              }
            }

            if (isAlreadyMember) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Graphite200
              ) {
                Text(
                  text = "Already In Trip",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Graphite600,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            } else {
              Surface(
                shape = CircleShape,
                color = StatusGreen.copy(alpha = 0.15f)
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = StatusGreen,
                  modifier = Modifier
                    .size(28.dp)
                    .padding(6.dp)
                )
              }
            }
          }
        }

        if (!isAlreadyMember) {
          Spacer(modifier = Modifier.height(18.dp))

          // Role Selection
          Text(
            text = "ASSIGN ROLE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = Graphite500
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            listOf("MEMBER" to "Traveler", "MANAGER" to "Trip Manager").forEach { (roleKey, label) ->
              val isSelected = selectedRole == roleKey
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Graphite900 else Graphite100,
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedRole = roleKey }
                  .border(1.dp, if (isSelected) Graphite900 else Graphite200, RoundedCornerShape(12.dp))
                  .testTag("role_select_${roleKey.lowercase()}")
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PureWhite else Graphite900
                  )
                  Text(
                    text = if (roleKey == "MEMBER") "Can add expenses & settle" else "Can approve & manage members",
                    fontSize = 10.sp,
                    color = if (isSelected) Graphite300 else Graphite500,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Add Button
          Button(
            onClick = {
              onAddUser(user.userId, selectedRole)
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Graphite900,
              contentColor = PureWhite
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("confirm_add_member_button")
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Add ${user.displayName} to Trip",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      } else {
        // Search Action Button when no user loaded yet
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = {
            val cleanEmail = emailInput.trim()
            if (cleanEmail.isEmpty()) {
              isError = true
              statusMessage = "Please enter an email address"
              return@Button
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
              isError = true
              statusMessage = "Invalid email format. Please check and try again."
              return@Button
            }
            isSearching = true
            statusMessage = null
            isError = false
            scope.launch {
              val result = onLookupEmail(cleanEmail)
              isSearching = false
              result.onSuccess { user ->
                if (user == null) {
                  isError = true
                  foundUser = null
                  statusMessage = "No user found with email '$cleanEmail'. Make sure they have registered in HisabTrip."
                } else if (currentMembers.any { it.userId == user.userId }) {
                  isError = true
                  foundUser = user
                  statusMessage = "${user.displayName} is already a member of this trip."
                } else {
                  isError = false
                  foundUser = user
                  statusMessage = "User found in Firestore directory!"
                }
              }.onFailure { err ->
                isError = true
                foundUser = null
                statusMessage = err.localizedMessage ?: "Failed to lookup user"
              }
            }
          },
          enabled = !isSearching && emailInput.isNotBlank(),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Graphite900,
            contentColor = PureWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("search_email_button")
        ) {
          if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureWhite, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Checking Firestore...")
          } else {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lookup User in Directory", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun ExportReportDialog(
  reportText: String,
  onDismiss: () -> Unit,
  onCopy: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Trip Ledger Report") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp)
          .verticalScroll(androidx.compose.foundation.rememberScrollState())
      ) {
        Text(text = reportText, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
      }
    },
    confirmButton = {
      Button(
        onClick = onCopy,
        colors = ButtonDefaults.buttonColors(containerColor = Graphite900)
      ) {
        Text("Copy Full Report")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    }
  )
}

@Composable
fun ExpenseDetailDialog(
  expense: ExpenseEntity,
  payers: List<com.example.data.local.ExpensePayerEntity>,
  participants: List<com.example.data.local.ExpenseParticipantEntity>,
  profiles: Map<String, UserProfileEntity>,
  isManager: Boolean,
  onDismiss: () -> Unit,
  onApprove: () -> Unit,
  onReject: (reason: String) -> Unit,
  onDelete: () -> Unit
) {
  var rejectReason by remember { mutableStateOf("") }
  var showRejectInput by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        StatusBadge(status = expense.status)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = MoneyUtils.formatPaise(expense.amountMinor),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Graphite900
        )
        Text(text = "Category: ${expense.category}", fontSize = 12.sp, color = Graphite600)
        if (expense.location.isNotEmpty()) {
          Text(text = "Location: ${expense.location}", fontSize = 12.sp, color = Graphite600)
        }
        if (!expense.receiptNote.isNullOrEmpty()) {
          Text(text = "Receipt Memo: ${expense.receiptNote}", fontSize = 12.sp, color = Graphite600)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "PAYERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Graphite500)
        payers.forEach { p ->
          val name = profiles[p.userId]?.displayName ?: p.userId
          Text("- $name: ${MoneyUtils.formatPaise(p.amountMinor)}", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "PARTICIPANTS & SHARES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Graphite500)
        participants.forEach { part ->
          val name = profiles[part.userId]?.displayName ?: part.userId
          Text("- $name: ${MoneyUtils.formatPaise(part.shareMinor)}", fontSize = 12.sp)
        }

        if (showRejectInput) {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = rejectReason,
            onValueChange = { rejectReason = it },
            label = { Text("Rejection Reason") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (expense.status == "PENDING" && isManager) {
          if (!showRejectInput) {
            Button(
              onClick = onApprove,
              colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
            ) {
              Text("Approve")
            }
            Button(
              onClick = { showRejectInput = true },
              colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
            ) {
              Text("Reject")
            }
          } else {
            Button(
              onClick = { onReject(rejectReason.ifBlank { "Rejected by manager" }) },
              colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
            ) {
              Text("Confirm Rejection")
            }
          }
        }
        if (isManager) {
          IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed)
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    }
  )
}

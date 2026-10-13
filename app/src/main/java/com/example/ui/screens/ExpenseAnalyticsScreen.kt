package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpenseParticipantEntity
import com.example.data.local.ExpensePayerEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.data.local.UserProfileEntity
import com.example.domain.analytics.AnalyticsDimension
import com.example.domain.analytics.CategoryBreakdownItem
import com.example.domain.analytics.ChartDataItem
import com.example.domain.analytics.ChartDisplayType
import com.example.domain.analytics.ExpenseAnalyticsCalculator
import com.example.domain.analytics.MemberAnalyticsMode
import com.example.domain.analytics.MemberBreakdownItem
import com.example.domain.analytics.TripExpenseAnalytics
import com.example.domain.money.MoneyUtils
import com.example.ui.components.D3ExpenseChart
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MetricCard
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardBackground
import com.example.ui.theme.Graphite400
import com.example.ui.theme.Graphite600
import com.example.ui.theme.Graphite700
import com.example.ui.theme.Graphite900
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.SurfaceNeutral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HisabTripViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalyticsScreen(
  viewModel: HisabTripViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  val trip by viewModel.selectedTrip.collectAsState()
  val members by viewModel.selectedTripMembers.collectAsState()
  val allProfiles by viewModel.allProfiles.collectAsState()
  val expenses by viewModel.selectedTripExpenses.collectAsState()
  val payers by viewModel.selectedTripPayers.collectAsState()
  val participants by viewModel.selectedTripParticipants.collectAsState()

  val profilesMap = remember(allProfiles) { allProfiles.associateBy { it.userId } }

  if (trip == null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MainBackground),
      contentAlignment = Alignment.Center
    ) {
      Text("No Trip Selected", color = TextSecondary)
    }
    return
  }

  val currentTrip = trip!!

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = currentTrip.name,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Expense Analytics & D3 Charts",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("analytics_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MainBackground)
      )
    },
    containerColor = MainBackground,
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    ExpenseAnalyticsTabContent(
      trip = currentTrip,
      expenses = expenses,
      payers = payers,
      participants = participants,
      members = members,
      profiles = profilesMap,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    )
  }
}

@Composable
fun ExpenseAnalyticsTabContent(
  trip: TripEntity,
  expenses: List<ExpenseEntity>,
  payers: List<ExpensePayerEntity>,
  participants: List<ExpenseParticipantEntity>,
  members: List<TripMemberEntity>,
  profiles: Map<String, UserProfileEntity>,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // State controls
  var dimension by remember { mutableStateOf(AnalyticsDimension.BY_CATEGORY) }
  var memberMode by remember { mutableStateOf(MemberAnalyticsMode.AMOUNT_PAID) }
  var chartType by remember { mutableStateOf(ChartDisplayType.DONUT_CHART) }
  var includePending by remember { mutableStateOf(false) }
  var selectedItemId by remember { mutableStateOf<String?>(null) }
  var expandedItemId by remember { mutableStateOf<String?>(null) }
  var showExportReportDialog by remember { mutableStateOf(false) }

  // Compute analytics data
  val analytics: TripExpenseAnalytics = remember(trip, expenses, payers, participants, members, profiles, includePending) {
    ExpenseAnalyticsCalculator.computeAnalytics(
      trip = trip,
      expenses = expenses,
      payers = payers,
      participants = participants,
      members = members,
      profiles = profiles,
      includePending = includePending
    )
  }

  // Active chart items based on selected dimension & member mode
  val chartItems: List<ChartDataItem> = remember(analytics, dimension, memberMode) {
    when (dimension) {
      AnalyticsDimension.BY_CATEGORY -> {
        analytics.categories.map { cat ->
          ChartDataItem(
            id = cat.category,
            label = cat.category,
            amountMinor = cat.totalAmountMinor,
            formattedAmount = MoneyUtils.formatCurrency(cat.totalAmountMinor, analytics.currencyCode),
            percentage = cat.percentage,
            colorHex = cat.colorHex,
            subtitle = "${cat.count} expense${if (cat.count == 1) "" else "s"}"
          )
        }
      }
      AnalyticsDimension.BY_MEMBER -> {
        val memberList = if (memberMode == MemberAnalyticsMode.AMOUNT_PAID) {
          analytics.membersPaid
        } else {
          analytics.membersShare
        }
        memberList.map { mem ->
          ChartDataItem(
            id = mem.userId,
            label = mem.displayName,
            amountMinor = mem.totalAmountMinor,
            formattedAmount = MoneyUtils.formatCurrency(mem.totalAmountMinor, analytics.currencyCode),
            percentage = mem.percentage,
            colorHex = mem.colorHex,
            subtitle = if (memberMode == MemberAnalyticsMode.AMOUNT_PAID) "Paid upfront" else "Share incurred"
          )
        }
      }
    }
  }

  if (expenses.isEmpty()) {
    EmptyStateView(
      icon = Icons.Default.ReceiptLong,
      title = "No Expenses Yet",
      message = "Add expenses to this trip to visualize breakdowns by category and member with interactive D3 charts.",
      modifier = modifier
        .fillMaxSize()
        .padding(24.dp)
    )
    return
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. KPI Summary Cards
    item {
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "TOTAL GROUP SPEND",
          value = MoneyUtils.formatCurrency(analytics.totalAllSpendMinor, analytics.currencyCode),
          subtitle = "${analytics.approvedExpenseCount} approved expenses",
          modifier = Modifier
            .weight(1f)
            .testTag("analytics_total_spend")
        )
        MetricCard(
          title = "AVG PER MEMBER",
          value = MoneyUtils.formatCurrency(analytics.averagePerMemberMinor, analytics.currencyCode),
          subtitle = "${members.size} trip members",
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
          title = "TOP CATEGORY",
          value = analytics.topCategory?.category ?: "None",
          subtitle = analytics.topCategory?.let { "${it.percentage}% (${MoneyUtils.formatCurrency(it.totalAmountMinor, analytics.currencyCode)})" },
          valueColor = Graphite900,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "TOP CONTRIBUTOR",
          value = analytics.topSpender?.displayName ?: "None",
          subtitle = analytics.topSpender?.let { "${it.percentage}% (${MoneyUtils.formatCurrency(it.totalAmountMinor, analytics.currencyCode)})" },
          valueColor = StatusGreen,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // 2. Interactive Chart Controls (Dimension & Chart Type)
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Row 1: Dimension Switcher [By Category] | [By Member]
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                dimension = AnalyticsDimension.BY_CATEGORY
                selectedItemId = null
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (dimension == AnalyticsDimension.BY_CATEGORY) PrimaryAction else SurfaceNeutral,
                contentColor = if (dimension == AnalyticsDimension.BY_CATEGORY) PureWhite else TextPrimary
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("toggle_by_category")
            ) {
              Icon(
                imageVector = Icons.Default.Category,
                contentDescription = "Category",
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("By Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                dimension = AnalyticsDimension.BY_MEMBER
                selectedItemId = null
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (dimension == AnalyticsDimension.BY_MEMBER) PrimaryAction else SurfaceNeutral,
                contentColor = if (dimension == AnalyticsDimension.BY_MEMBER) PureWhite else TextPrimary
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("toggle_by_member")
            ) {
              Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Member",
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("By Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          // Row 2: Sub-filters (Member mode if By Member, and Chart Type)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Chart type switch (Donut vs Bar)
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (chartType == ChartDisplayType.DONUT_CHART) PrimaryAction else SurfaceNeutral,
                modifier = Modifier
                  .clickable { chartType = ChartDisplayType.DONUT_CHART }
                  .testTag("toggle_donut_chart")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.DonutLarge,
                    contentDescription = "Donut",
                    tint = if (chartType == ChartDisplayType.DONUT_CHART) PureWhite else TextPrimary,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "Donut",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (chartType == ChartDisplayType.DONUT_CHART) PureWhite else TextPrimary
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (chartType == ChartDisplayType.BAR_CHART) PrimaryAction else SurfaceNeutral,
                modifier = Modifier
                  .clickable { chartType = ChartDisplayType.BAR_CHART }
                  .testTag("toggle_bar_chart")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.ViewStream,
                    contentDescription = "Bar",
                    tint = if (chartType == ChartDisplayType.BAR_CHART) PureWhite else TextPrimary,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "Bars",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (chartType == ChartDisplayType.BAR_CHART) PureWhite else TextPrimary
                  )
                }
              }
            }

            // Share / Export icon button
            IconButton(
              onClick = { showExportReportDialog = true },
              modifier = Modifier
                .size(36.dp)
                .testTag("analytics_share_button")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Report",
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          // If By Member: Toggle between Paid vs Consumed
          if (dimension == AnalyticsDimension.BY_MEMBER) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = memberMode == MemberAnalyticsMode.AMOUNT_PAID,
                onClick = { memberMode = MemberAnalyticsMode.AMOUNT_PAID; selectedItemId = null },
                label = { Text("Amount Paid (Who Fronted)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SurfaceNeutral,
                  selectedLabelColor = TextPrimary
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("toggle_amount_paid")
              )
              FilterChip(
                selected = memberMode == MemberAnalyticsMode.SHARE_INCURRED,
                onClick = { memberMode = MemberAnalyticsMode.SHARE_INCURRED; selectedItemId = null },
                label = { Text("Share Incurred (Consumed)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SurfaceNeutral,
                  selectedLabelColor = TextPrimary
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("toggle_share_incurred")
              )
            }
          }
        }
      }
    }

    // 3. Interactive D3 SVG Chart
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (dimension == AnalyticsDimension.BY_CATEGORY) "CATEGORY BREAKDOWN (D3)" else "MEMBER BREAKDOWN (D3)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Graphite600,
              letterSpacing = 0.5.sp
            )
            if (selectedItemId != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clickable { selectedItemId = null }
                  .padding(4.dp)
              ) {
                Text("Clear filter", fontSize = 11.sp, color = Graphite400)
                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = Graphite400)
              }
            }
          }

          // Embed D3 Chart WebView Component
          D3ExpenseChart(
            data = chartItems,
            displayType = chartType,
            selectedItemId = selectedItemId,
            onItemSelected = { id ->
              selectedItemId = if (selectedItemId == id) null else id
            },
            centerTitle = if (dimension == AnalyticsDimension.BY_CATEGORY) "Total Spend" else "Group Pool",
            centerAmountFormatted = MoneyUtils.formatCurrency(analytics.totalAllSpendMinor, analytics.currencyCode)
          )

          // Scrollable Quick Legend
          val legendScrollState = rememberScrollState()
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(legendScrollState)
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            chartItems.forEach { item ->
              val isSelected = selectedItemId == item.id
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) SurfaceNeutral else CardBackground,
                border = androidx.compose.foundation.BorderStroke(
                  width = if (isSelected) 1.5.dp else 0.8.dp,
                  color = if (isSelected) TextPrimary else BorderSubtle
                ),
                modifier = Modifier.clickable {
                  selectedItemId = if (selectedItemId == item.id) null else item.id
                }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(parseHexColor(item.colorHex))
                  )
                  Text(
                    text = "${item.label} (${item.percentage}%)",
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. Detail Breakdown Cards Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (dimension == AnalyticsDimension.BY_CATEGORY) "Categories Breakdown" else "Members Breakdown",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = "${chartItems.size} items",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
    }

    // 5. Individual Items Breakdown Cards
    if (dimension == AnalyticsDimension.BY_CATEGORY) {
      items(analytics.categories, key = { it.category }) { cat ->
        val isSelected = selectedItemId == cat.category
        val isExpanded = expandedItemId == cat.category

        CategoryBreakdownCard(
          item = cat,
          currencyCode = analytics.currencyCode,
          isSelected = isSelected,
          isExpanded = isExpanded,
          onCardClick = {
            selectedItemId = if (isSelected) null else cat.category
          },
          onToggleExpand = {
            expandedItemId = if (isExpanded) null else cat.category
          }
        )
      }
    } else {
      val memberItems = if (memberMode == MemberAnalyticsMode.AMOUNT_PAID) {
        analytics.membersPaid
      } else {
        analytics.membersShare
      }
      items(memberItems, key = { it.userId }) { member ->
        val isSelected = selectedItemId == member.userId
        val isExpanded = expandedItemId == member.userId

        MemberBreakdownCard(
          item = member,
          currencyCode = analytics.currencyCode,
          mode = memberMode,
          isSelected = isSelected,
          isExpanded = isExpanded,
          onCardClick = {
            selectedItemId = if (isSelected) null else member.userId
          },
          onToggleExpand = {
            expandedItemId = if (isExpanded) null else member.userId
          }
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }

  // Export report dialog
  if (showExportReportDialog) {
    val reportText = remember(analytics, dimension) {
      buildString {
        appendLine("📊 HisabTrip Expense Breakdown: ${trip.name}")
        appendLine("Total Spent: ${MoneyUtils.formatCurrency(analytics.totalAllSpendMinor, analytics.currencyCode)}")
        appendLine("Expenses Count: ${analytics.approvedExpenseCount}")
        appendLine()
        appendLine("📁 CATEGORY BREAKDOWN:")
        analytics.categories.forEach { cat ->
          appendLine("• ${cat.category}: ${MoneyUtils.formatCurrency(cat.totalAmountMinor, analytics.currencyCode)} (${cat.percentage}%) - ${cat.count} items")
        }
        appendLine()
        appendLine("👥 MEMBER BREAKDOWN (PAID UPFRONT):")
        analytics.membersPaid.forEach { mem ->
          appendLine("• ${mem.displayName}: ${MoneyUtils.formatCurrency(mem.totalAmountMinor, analytics.currencyCode)} (${mem.percentage}%)")
        }
      }
    }

    ExportReportDialog(
      reportText = reportText,
      onDismiss = { showExportReportDialog = false },
      onCopy = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("HisabTrip Analytics", reportText))
        showExportReportDialog = false
      }
    )
  }
}

@Composable
private fun CategoryBreakdownCard(
  item: CategoryBreakdownItem,
  currencyCode: String,
  isSelected: Boolean,
  isExpanded: Boolean,
  onCardClick: () -> Unit,
  onToggleExpand: () -> Unit
) {
  val catColor = parseHexColor(item.colorHex)

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardBackground),
    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TextPrimary) else null,
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize()
      .clickable { onCardClick() }
      .testTag("breakdown_item_${item.category.lowercase().replace(" ", "_")}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
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
              .background(catColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = getCategoryIcon(item.category),
              contentDescription = item.category,
              tint = catColor,
              modifier = Modifier.size(20.dp)
            )
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = item.category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceNeutral
              ) {
                Text(
                  text = "${item.percentage}%",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "${item.count} transaction${if (item.count == 1) "" else "s"}",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = MoneyUtils.formatCurrency(item.totalAmountMinor, currencyCode),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          IconButton(
            onClick = onToggleExpand,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Expand",
              tint = TextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // Linear Progress Bar showing relative weight
      Spacer(modifier = Modifier.height(10.dp))
      LinearProgressIndicator(
        progress = { (item.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
        color = catColor,
        trackColor = SurfaceNeutral,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
      )

      // Expanded list of transactions in this category
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Transactions in ${item.category}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
          )
          item.expenses.forEach { expense ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceNeutral.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = expense.title,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = formatExpenseDate(expense.expenseDate),
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
              Text(
                text = MoneyUtils.formatCurrency(expense.amountMinor, currencyCode),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MemberBreakdownCard(
  item: MemberBreakdownItem,
  currencyCode: String,
  mode: MemberAnalyticsMode,
  isSelected: Boolean,
  isExpanded: Boolean,
  onCardClick: () -> Unit,
  onToggleExpand: () -> Unit
) {
  val memberColor = parseHexColor(item.colorHex)

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardBackground),
    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TextPrimary) else null,
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize()
      .clickable { onCardClick() }
      .testTag("breakdown_item_${item.userId}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MemberAvatar(
            avatarKey = item.avatarKey,
            name = item.displayName,
            size = 38.dp
          )

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = item.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceNeutral
              ) {
                Text(
                  text = "${item.percentage}%",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = if (mode == MemberAnalyticsMode.AMOUNT_PAID) {
                "Paid in ${item.count} expense${if (item.count == 1) "" else "s"}"
              } else {
                "Participated in ${item.count} expense${if (item.count == 1) "" else "s"}"
              },
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = MoneyUtils.formatCurrency(item.totalAmountMinor, currencyCode),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          IconButton(
            onClick = onToggleExpand,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Expand",
              tint = TextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // Progress bar
      Spacer(modifier = Modifier.height(10.dp))
      LinearProgressIndicator(
        progress = { (item.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
        color = memberColor,
        trackColor = SurfaceNeutral,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
      )

      // Expanded list of transactions
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = if (mode == MemberAnalyticsMode.AMOUNT_PAID) "Expenses Paid by ${item.displayName}" else "Expenses Shared by ${item.displayName}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
          )
          if (item.expenses.isEmpty()) {
            Text("No individual expenses recorded", fontSize = 12.sp, color = TextSecondary)
          } else {
            item.expenses.forEach { expense ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(SurfaceNeutral.copy(alpha = 0.6f))
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = expense.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${expense.category} • ${formatExpenseDate(expense.expenseDate)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }
                Text(
                  text = MoneyUtils.formatCurrency(expense.amountMinor, currencyCode),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun getCategoryIcon(category: String): ImageVector {
  return when (category.trim().lowercase()) {
    "food", "food & dining", "dining" -> Icons.Default.Fastfood
    "travel", "travel & transport", "transport", "flight" -> Icons.Default.Flight
    "stay", "accommodation", "hotel" -> Icons.Default.Hotel
    "activity", "activities", "activities & sightseeing", "entertainment" -> Icons.Default.LocalActivity
    "shopping" -> Icons.Default.ShoppingBag
    else -> Icons.Default.ReceiptLong
  }
}

private fun parseHexColor(hex: String): Color {
  return try {
    val clean = hex.removePrefix("#")
    val colorLong = clean.toLong(16)
    if (clean.length == 6) {
      Color(0xFF000000 or colorLong)
    } else {
      Color(colorLong)
    }
  } catch (e: Exception) {
    Color(0xFF4D96FF)
  }
}

private fun formatExpenseDate(timestamp: Long): String {
  return try {
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
  } catch (e: Exception) {
    ""
  }
}

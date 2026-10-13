package com.example.domain.analytics

import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpenseParticipantEntity
import com.example.data.local.ExpensePayerEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.data.local.UserProfileEntity

object ExpenseAnalyticsCalculator {

  // Distinct, accessible Material colors for visualization
  private val CATEGORY_COLOR_MAP = mapOf(
    "Food" to "#FF6B6B",
    "Food & Dining" to "#FF6B6B",
    "Dining" to "#FF6B6B",
    "Travel" to "#4D96FF",
    "Travel & Transport" to "#4D96FF",
    "Transport" to "#4D96FF",
    "Flight" to "#38BDF8",
    "Stay" to "#10B981",
    "Accommodation" to "#10B981",
    "Hotel" to "#10B981",
    "Activity" to "#8B5CF6",
    "Activities" to "#8B5CF6",
    "Activities & Sightseeing" to "#8B5CF6",
    "Entertainment" to "#A855F7",
    "Shopping" to "#F59E0B",
    "General" to "#EC4899",
    "Miscellaneous" to "#64748B",
    "Other" to "#94A3B8"
  )

  private val PALETTE_FALLBACK = listOf(
    "#FF6B6B", // Coral Red
    "#4D96FF", // Sky Blue
    "#10B981", // Emerald
    "#8B5CF6", // Violet
    "#F59E0B", // Amber
    "#EC4899", // Rose Pink
    "#14B8A6", // Teal
    "#F97316", // Warm Orange
    "#6366F1", // Indigo
    "#84CC16"  // Lime
  )

  fun getCategoryColor(category: String, index: Int = 0): String {
    return CATEGORY_COLOR_MAP[category.trim()]
      ?: PALETTE_FALLBACK[kotlin.math.abs(category.hashCode() + index) % PALETTE_FALLBACK.size]
  }

  fun getMemberColor(userId: String, index: Int): String {
    return PALETTE_FALLBACK[index % PALETTE_FALLBACK.size]
  }

  /**
   * Computes comprehensive analytics breakdown for a trip group.
   */
  fun computeAnalytics(
    trip: TripEntity,
    expenses: List<ExpenseEntity>,
    payers: List<ExpensePayerEntity>,
    participants: List<ExpenseParticipantEntity>,
    members: List<TripMemberEntity>,
    profiles: Map<String, UserProfileEntity>,
    includePending: Boolean = false
  ): TripExpenseAnalytics {
    val relevantExpenses = if (includePending) {
      expenses.filter { it.status == "APPROVED" || it.status == "PENDING" }
    } else {
      expenses.filter { it.status == "APPROVED" }
    }

    val approvedExpenses = expenses.filter { it.status == "APPROVED" }
    val totalApprovedSpend = approvedExpenses.sumOf { it.amountMinor }
    val totalRelevantSpend = relevantExpenses.sumOf { it.amountMinor }

    val relevantExpenseIds = relevantExpenses.map { it.id }.toSet()
    val approvedExpenseCount = approvedExpenses.size
    val pendingExpenseCount = expenses.count { it.status == "PENDING" }

    // 1. Group By Category
    val categoryGroups = relevantExpenses.groupBy { it.category.ifBlank { "General" } }
    val categoryItems = categoryGroups.entries.mapIndexed { index, entry ->
      val catName = entry.key
      val catExpenses = entry.value
      val catTotal = catExpenses.sumOf { it.amountMinor }
      val pct = if (totalRelevantSpend > 0L) {
        (catTotal.toDouble() / totalRelevantSpend.toDouble()) * 100.0
      } else {
        0.0
      }
      CategoryBreakdownItem(
        category = catName,
        totalAmountMinor = catTotal,
        percentage = kotlin.math.round(pct * 10.0) / 10.0,
        count = catExpenses.size,
        colorHex = getCategoryColor(catName, index),
        expenses = catExpenses.sortedByDescending { it.amountMinor }
      )
    }.sortedByDescending { it.totalAmountMinor }

    // 2. Group By Member: Amount Paid (Who fronted the money)
    val relevantPayers = payers.filter { it.expenseId in relevantExpenseIds }
    val paidByUser = relevantPayers.groupBy { it.userId }
      .mapValues { (_, payerList) -> payerList.sumOf { it.amountMinor } }

    val memberPaidItems = members.mapIndexed { index, member ->
      val uid = member.userId
      val profile = profiles[uid]
      val name = profile?.displayName ?: "Member ${uid.take(4)}"
      val paidAmount = paidByUser[uid] ?: 0L
      val memberExpenses = relevantExpenses.filter { exp ->
        relevantPayers.any { it.expenseId == exp.id && it.userId == uid }
      }
      val pct = if (totalRelevantSpend > 0L) {
        (paidAmount.toDouble() / totalRelevantSpend.toDouble()) * 100.0
      } else {
        0.0
      }
      MemberBreakdownItem(
        userId = uid,
        displayName = name,
        avatarKey = profile?.avatarKey ?: "explorer",
        role = member.role,
        totalAmountMinor = paidAmount,
        percentage = kotlin.math.round(pct * 10.0) / 10.0,
        count = memberExpenses.size,
        colorHex = getMemberColor(uid, index),
        expenses = memberExpenses.sortedByDescending { it.amountMinor }
      )
    }.sortedByDescending { it.totalAmountMinor }

    // 3. Group By Member: Share Incurred (Who benefited / consumed)
    val relevantParticipants = participants.filter { it.expenseId in relevantExpenseIds }
    val shareByUser = relevantParticipants.groupBy { it.userId }
      .mapValues { (_, partList) -> partList.sumOf { it.shareMinor } }

    val memberShareItems = members.mapIndexed { index, member ->
      val uid = member.userId
      val profile = profiles[uid]
      val name = profile?.displayName ?: "Member ${uid.take(4)}"
      val shareAmount = shareByUser[uid] ?: 0L
      val memberExpenses = relevantExpenses.filter { exp ->
        relevantParticipants.any { it.expenseId == exp.id && it.userId == uid }
      }
      val pct = if (totalRelevantSpend > 0L) {
        (shareAmount.toDouble() / totalRelevantSpend.toDouble()) * 100.0
      } else {
        0.0
      }
      MemberBreakdownItem(
        userId = uid,
        displayName = name,
        avatarKey = profile?.avatarKey ?: "explorer",
        role = member.role,
        totalAmountMinor = shareAmount,
        percentage = kotlin.math.round(pct * 10.0) / 10.0,
        count = memberExpenses.size,
        colorHex = getMemberColor(uid, index),
        expenses = memberExpenses.sortedByDescending { it.amountMinor }
      )
    }.sortedByDescending { it.totalAmountMinor }

    val topCat = categoryItems.firstOrNull()
    val topSpender = memberPaidItems.firstOrNull()
    val avgPerMember = if (members.isNotEmpty()) {
      totalRelevantSpend / members.size
    } else {
      0L
    }

    return TripExpenseAnalytics(
      tripId = trip.id,
      tripName = trip.name,
      currencyCode = trip.currencyCode.ifBlank { "INR" },
      totalApprovedSpendMinor = totalApprovedSpend,
      totalAllSpendMinor = totalRelevantSpend,
      approvedExpenseCount = approvedExpenseCount,
      pendingExpenseCount = pendingExpenseCount,
      categories = categoryItems,
      membersPaid = memberPaidItems,
      membersShare = memberShareItems,
      topCategory = topCat,
      topSpender = topSpender,
      averagePerMemberMinor = avgPerMember
    )
  }
}

package com.example.domain.analytics

import com.example.data.local.ExpenseEntity

/**
 * Data representation for a single category's breakdown in a trip.
 */
data class CategoryBreakdownItem(
  val category: String,
  val totalAmountMinor: Long,
  val percentage: Double,
  val count: Int,
  val colorHex: String,
  val expenses: List<ExpenseEntity> = emptyList()
)

/**
 * Data representation for a single member's breakdown (either paid or incurred share).
 */
data class MemberBreakdownItem(
  val userId: String,
  val displayName: String,
  val avatarKey: String,
  val role: String,
  val totalAmountMinor: Long,
  val percentage: Double,
  val count: Int,
  val colorHex: String,
  val expenses: List<ExpenseEntity> = emptyList()
)

/**
 * Full calculated analytics dataset for a trip group.
 */
data class TripExpenseAnalytics(
  val tripId: String,
  val tripName: String,
  val currencyCode: String,
  val totalApprovedSpendMinor: Long,
  val totalAllSpendMinor: Long,
  val approvedExpenseCount: Int,
  val pendingExpenseCount: Int,
  val categories: List<CategoryBreakdownItem>,
  val membersPaid: List<MemberBreakdownItem>,
  val membersShare: List<MemberBreakdownItem>,
  val topCategory: CategoryBreakdownItem?,
  val topSpender: MemberBreakdownItem?,
  val averagePerMemberMinor: Long
)

enum class AnalyticsDimension {
  BY_CATEGORY,
  BY_MEMBER
}

enum class MemberAnalyticsMode {
  AMOUNT_PAID,
  SHARE_INCURRED
}

enum class ChartDisplayType {
  DONUT_CHART,
  BAR_CHART
}

data class ChartDataItem(
  val id: String,
  val label: String,
  val amountMinor: Long,
  val formattedAmount: String,
  val percentage: Double,
  val colorHex: String,
  val subtitle: String = ""
)

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class UserProfileEntity(
  @PrimaryKey val userId: String,
  val email: String,
  val displayName: String,
  val username: String,
  val avatarKey: String,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trips")
data class TripEntity(
  @PrimaryKey val id: String,
  val name: String,
  val description: String = "",
  val startDate: String = "",
  val endDate: String = "",
  val currencyCode: String = "INR",
  val budgetMinor: Long = 0L, // in paise
  val status: String = "ACTIVE", // ACTIVE or COMPLETED
  val createdBy: String,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "trip_members",
  primaryKeys = ["tripId", "userId"]
)
data class TripMemberEntity(
  val tripId: String,
  val userId: String,
  val role: String = "MEMBER", // CREATOR, MANAGER, MEMBER
  val targetMinor: Long = 0L,  // Expected contribution target
  val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contributions")
data class ContributionEntity(
  @PrimaryKey val id: String,
  val tripId: String,
  val userId: String,
  val amountMinor: Long, // in paise
  val recordedBy: String,
  val recordedAt: Long = System.currentTimeMillis(),
  val note: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
  @PrimaryKey val id: String,
  val tripId: String,
  val title: String,
  val amountMinor: Long, // in paise
  val category: String = "Food", // Food, Travel, Stay, Activity, Shopping, General
  val location: String = "",
  val description: String = "",
  val expenseDate: Long = System.currentTimeMillis(),
  val status: String = "APPROVED", // APPROVED, PENDING, REJECTED
  val createdBy: String,
  val approvedBy: String? = null,
  val approvedAt: Long? = null,
  val rejectionReason: String? = null,
  val receiptNote: String? = null,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "expense_payers",
  primaryKeys = ["expenseId", "userId"]
)
data class ExpensePayerEntity(
  val expenseId: String,
  val userId: String,
  val amountMinor: Long // in paise
)

@Entity(
  tableName = "expense_participants",
  primaryKeys = ["expenseId", "userId"]
)
data class ExpenseParticipantEntity(
  val expenseId: String,
  val userId: String,
  val shareMinor: Long // in paise
)

@Entity(tableName = "settlements")
data class SettlementEntity(
  @PrimaryKey val id: String,
  val tripId: String,
  val fromUserId: String,
  val toUserId: String,
  val amountMinor: Long, // in paise
  val status: String = "PENDING", // PENDING, PAID
  val paidAt: Long? = null,
  val paymentNote: String? = null,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
  @PrimaryKey val id: String,
  val tripId: String,
  val actorUserId: String,
  val action: String, // e.g. "CREATED_EXPENSE", "APPROVED_EXPENSE", "RECORDED_CONTRIBUTION"
  val details: String,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val tripId: String?,
  val type: String,
  val title: String,
  val body: String,
  val read: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

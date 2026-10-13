package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val displayName: String = "",
  val username: String = "",
  val avatarKey: String = "compass",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTrip(
  @DocumentId val id: String = "",
  val name: String = "",
  val description: String = "",
  val startDate: String = "",
  val endDate: String = "",
  val currencyCode: String = "INR",
  val budgetMinor: Long = 0L,
  val status: String = "ACTIVE",
  val createdBy: String = "",
  val memberIds: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTripMember(
  @DocumentId val id: String = "",
  val tripId: String = "",
  val userId: String = "",
  val role: String = "MEMBER",
  val targetMinor: Long = 0L,
  @ServerTimestamp val joinedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreExpense(
  @DocumentId val id: String = "",
  val tripId: String = "",
  val title: String = "",
  val amountMinor: Long = 0L,
  val category: String = "Food & Dining",
  val location: String = "",
  val description: String = "",
  val expenseDate: Long = 0L,
  val status: String = "APPROVED",
  val createdBy: String = "",
  val approvedBy: String? = null,
  val rejectionReason: String? = null,
  val receiptNote: String? = null,
  val payers: Map<String, Long> = emptyMap(),
  val participants: Map<String, Long> = emptyMap(),
  val payerIds: List<String> = emptyList(),
  val participantIds: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreContribution(
  @DocumentId val id: String = "",
  val tripId: String = "",
  val userId: String = "",
  val amountMinor: Long = 0L,
  val recordedBy: String = "",
  val note: String = "",
  @ServerTimestamp val recordedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSettlement(
  @DocumentId val id: String = "",
  val tripId: String = "",
  val fromUserId: String = "",
  val toUserId: String = "",
  val amountMinor: Long = 0L,
  val status: String = "PENDING",
  val paymentNote: String? = null,
  val paidAt: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val tripId: String? = null,
  val type: String = "INFO",
  val title: String = "",
  val body: String = "",
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

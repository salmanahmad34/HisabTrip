package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HisabTripDao {

  // --- Profiles ---
  @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
  fun getProfile(userId: String): Flow<UserProfileEntity?>

  @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
  suspend fun getProfileSync(userId: String): UserProfileEntity?

  @Query("SELECT * FROM profiles WHERE LOWER(email) = LOWER(:email) LIMIT 1")
  suspend fun getProfileByEmailSync(email: String): UserProfileEntity?

  @Query("SELECT * FROM profiles")
  fun getAllProfiles(): Flow<List<UserProfileEntity>>

  @Query("SELECT * FROM profiles")
  suspend fun getAllProfilesSync(): List<UserProfileEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProfile(profile: UserProfileEntity)

  // --- Trips ---
  @Query("SELECT * FROM trips ORDER BY createdAt DESC")
  fun getAllTrips(): Flow<List<TripEntity>>

  @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
  fun getTrip(tripId: String): Flow<TripEntity?>

  @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
  suspend fun getTripSync(tripId: String): TripEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrip(trip: TripEntity)

  @Query("UPDATE trips SET status = :status WHERE id = :tripId")
  suspend fun updateTripStatus(tripId: String, status: String)

  @Query("UPDATE trips SET budgetMinor = :budgetMinor WHERE id = :tripId")
  suspend fun updateTripBudget(tripId: String, budgetMinor: Long)

  @Query("DELETE FROM trips WHERE id = :tripId")
  suspend fun deleteTrip(tripId: String)

  // --- Members ---
  @Query("SELECT * FROM trip_members")
  fun getAllTripMembers(): Flow<List<TripMemberEntity>>

  @Query("SELECT * FROM trip_members WHERE tripId = :tripId ORDER BY joinedAt ASC")
  fun getTripMembers(tripId: String): Flow<List<TripMemberEntity>>

  @Query("SELECT * FROM trip_members WHERE tripId = :tripId ORDER BY joinedAt ASC")
  suspend fun getTripMembersSync(tripId: String): List<TripMemberEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTripMember(member: TripMemberEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTripMembers(members: List<TripMemberEntity>)

  @Query("DELETE FROM trip_members WHERE tripId = :tripId AND userId = :userId")
  suspend fun removeTripMember(tripId: String, userId: String)

  @Query("UPDATE trip_members SET role = :role WHERE tripId = :tripId AND userId = :userId")
  suspend fun updateMemberRole(tripId: String, userId: String, role: String)

  @Query("UPDATE trip_members SET targetMinor = :targetMinor WHERE tripId = :tripId AND userId = :userId")
  suspend fun updateMemberTarget(tripId: String, userId: String, targetMinor: Long)

  // --- Contributions ---
  @Query("SELECT * FROM contributions WHERE tripId = :tripId ORDER BY recordedAt DESC")
  fun getContributions(tripId: String): Flow<List<ContributionEntity>>

  @Query("SELECT * FROM contributions WHERE tripId = :tripId")
  suspend fun getContributionsSync(tripId: String): List<ContributionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContribution(contribution: ContributionEntity)

  @Query("DELETE FROM contributions WHERE id = :id")
  suspend fun deleteContribution(id: String)

  // --- Expenses ---
  @Query("SELECT * FROM expenses")
  fun getAllExpenses(): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY expenseDate DESC")
  fun getExpenses(tripId: String): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY expenseDate DESC")
  suspend fun getExpensesSync(tripId: String): List<ExpenseEntity>

  @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
  suspend fun getExpenseById(id: String): ExpenseEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpense(expense: ExpenseEntity)

  @Query("UPDATE expenses SET status = :status, approvedBy = :approvedBy, approvedAt = :approvedAt, rejectionReason = :reason WHERE id = :expenseId")
  suspend fun updateExpenseStatus(
    expenseId: String,
    status: String,
    approvedBy: String?,
    approvedAt: Long?,
    reason: String?
  )

  @Query("DELETE FROM expenses WHERE id = :expenseId")
  suspend fun deleteExpense(expenseId: String)

  // --- Expense Payers & Participants ---
  @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
  suspend fun getPayersForExpense(expenseId: String): List<ExpensePayerEntity>

  @Query("SELECT * FROM expense_participants WHERE expenseId = :expenseId")
  suspend fun getParticipantsForExpense(expenseId: String): List<ExpenseParticipantEntity>

  @Query("SELECT ep.* FROM expense_payers ep INNER JOIN expenses e ON ep.expenseId = e.id WHERE e.tripId = :tripId")
  fun getAllPayersForTrip(tripId: String): Flow<List<ExpensePayerEntity>>

  @Query("SELECT ep.* FROM expense_participants ep INNER JOIN expenses e ON ep.expenseId = e.id WHERE e.tripId = :tripId")
  fun getAllParticipantsForTrip(tripId: String): Flow<List<ExpenseParticipantEntity>>

  @Query("SELECT ep.* FROM expense_payers ep INNER JOIN expenses e ON ep.expenseId = e.id WHERE e.tripId = :tripId")
  suspend fun getAllPayersForTripSync(tripId: String): List<ExpensePayerEntity>

  @Query("SELECT ep.* FROM expense_participants ep INNER JOIN expenses e ON ep.expenseId = e.id WHERE e.tripId = :tripId")
  suspend fun getAllParticipantsForTripSync(tripId: String): List<ExpenseParticipantEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayers(payers: List<ExpensePayerEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertParticipants(participants: List<ExpenseParticipantEntity>)

  @Query("DELETE FROM expense_payers WHERE expenseId = :expenseId")
  suspend fun deletePayers(expenseId: String)

  @Query("DELETE FROM expense_participants WHERE expenseId = :expenseId")
  suspend fun deleteParticipants(expenseId: String)

  // Transaction for inserting complete expense with payers and participants
  @Transaction
  suspend fun createExpenseWithSplits(
    expense: ExpenseEntity,
    payers: List<ExpensePayerEntity>,
    participants: List<ExpenseParticipantEntity>
  ) {
    insertExpense(expense)
    insertPayers(payers)
    insertParticipants(participants)
  }

  // --- Settlements ---
  @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY createdAt DESC")
  fun getSettlements(tripId: String): Flow<List<SettlementEntity>>

  @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY createdAt DESC")
  suspend fun getSettlementsSync(tripId: String): List<SettlementEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSettlement(settlement: SettlementEntity)

  @Query("UPDATE settlements SET status = :status, paidAt = :paidAt, paymentNote = :note WHERE id = :id")
  suspend fun updateSettlementStatus(id: String, status: String, paidAt: Long?, note: String?)

  // --- Audit Logs ---
  @Query("SELECT * FROM audit_logs WHERE tripId = :tripId ORDER BY createdAt DESC")
  fun getAuditLogs(tripId: String): Flow<List<AuditLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAuditLog(log: AuditLogEntity)

  // --- Notifications ---
  @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
  fun getNotifications(userId: String): Flow<List<NotificationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: NotificationEntity)

  @Query("UPDATE notifications SET read = 1 WHERE id = :id")
  suspend fun markNotificationRead(id: String)

  @Query("UPDATE notifications SET read = 1 WHERE userId = :userId")
  suspend fun markAllNotificationsRead(userId: String)
}

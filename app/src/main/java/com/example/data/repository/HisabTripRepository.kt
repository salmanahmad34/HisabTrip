package com.example.data.repository

import com.example.data.local.AuditLogEntity
import com.example.data.local.ContributionEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpenseParticipantEntity
import com.example.data.local.ExpensePayerEntity
import com.example.data.local.HisabTripDao
import com.example.data.local.NotificationEntity
import com.example.data.local.SettlementEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.data.local.UserProfileEntity
import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.firebase.FirestoreContribution
import com.example.data.firebase.FirestoreExpense
import com.example.data.firebase.FirestoreSettlement
import com.example.data.firebase.FirestoreTrip
import com.example.data.firebase.FirestoreTripMember
import com.example.data.firebase.FirestoreUserProfile
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.domain.money.MoneyUtils
import com.example.domain.settlement.MemberInfo
import com.example.domain.settlement.SettledPayment
import com.example.domain.settlement.SettlementCalculator
import com.example.domain.settlement.SettlementResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HisabTripRepository(
  private val dao: HisabTripDao,
  val firestore: FirebaseFirestore
) {

  constructor(dao: HisabTripDao, context: Context) : this(
    dao,
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
  fun requireUserId(): String = auth.currentUser?.uid ?: CURRENT_USER_ID

  companion object {
    const val CURRENT_USER_ID = "usr_salman_7307"
    const val CURRENT_USER_EMAIL = "mrsalman7307@gmail.com"
    const val CURRENT_USER_NAME = "Salman"
    const val CURRENT_USERNAME = "salman7307"

    val PREDEFINED_AVATARS = listOf(
      "compass",
      "mountain",
      "tent",
      "backpack",
      "bonfire",
      "camera",
      "map",
      "luggage"
    )

    val PREDEFINED_CATEGORIES = listOf(
      "Food & Dining",
      "Transport & Fuel",
      "Stay & Hotel",
      "Activities & Tickets",
      "Shopping & Supplies",
      "General Expense"
    )
  }

  suspend fun ensureDefaultUserInitialized() {
    val existing = dao.getProfileSync(CURRENT_USER_ID)
    if (existing == null) {
      val defaultProfile = UserProfileEntity(
        userId = CURRENT_USER_ID,
        email = CURRENT_USER_EMAIL,
        displayName = CURRENT_USER_NAME,
        username = CURRENT_USERNAME,
        avatarKey = "compass"
      )
      dao.insertProfile(defaultProfile)

      // Also ensure standard traveler companions for group demo/testing
      val companion1 = UserProfileEntity(
        userId = "usr_aarav_101",
        email = "aarav.travel@example.com",
        displayName = "Aarav Patel",
        username = "aarav_p",
        avatarKey = "mountain"
      )
      val companion2 = UserProfileEntity(
        userId = "usr_priya_202",
        email = "priya.sharma@example.com",
        displayName = "Priya Sharma",
        username = "priyash",
        avatarKey = "camera"
      )
      val companion3 = UserProfileEntity(
        userId = "usr_vikram_303",
        email = "vikram.mehta@example.com",
        displayName = "Vikram Mehta",
        username = "vikram_m",
        avatarKey = "backpack"
      )
      dao.insertProfile(companion1)
      dao.insertProfile(companion2)
      dao.insertProfile(companion3)
    }
  }

  // --- Profiles ---
  fun getCurrentProfile(): Flow<UserProfileEntity?> = dao.getProfile(CURRENT_USER_ID)
  fun getAllProfiles(): Flow<List<UserProfileEntity>> = dao.getAllProfiles()
  suspend fun getAllProfilesSync(): List<UserProfileEntity> = dao.getAllProfilesSync()

  suspend fun updateProfile(displayName: String, username: String, avatarKey: String) {
    val current = dao.getProfileSync(CURRENT_USER_ID) ?: return
    val updated = current.copy(
      displayName = displayName.trim(),
      username = username.trim().lowercase(),
      avatarKey = avatarKey
    )
    dao.insertProfile(updated)
  }

  // --- Trips ---
  fun getAllTrips(): Flow<List<TripEntity>> = dao.getAllTrips()
  fun getTrip(tripId: String): Flow<TripEntity?> = dao.getTrip(tripId)
  suspend fun getTripSync(tripId: String): TripEntity? = dao.getTripSync(tripId)
  fun getAllTripMembers(): Flow<List<TripMemberEntity>> = dao.getAllTripMembers()
  fun getAllExpenses(): Flow<List<ExpenseEntity>> = dao.getAllExpenses()

  /**
   * Fetches current user's trip groups from Firestore where user is creator or member,
   * syncing them into local Room persistence for instant display and offline caching.
   */
  suspend fun fetchTripsFromFirestore(): Result<List<TripEntity>> {
    return try {
      val uid = requireUserId()
      // Fetch trips where user is in memberIds array
      val snapshot = firestore.collection("trips")
        .whereArrayContains("memberIds", uid)
        .get()
        .await()

      val fetchedTrips = mutableListOf<TripEntity>()
      for (doc in snapshot.documents) {
        val tripObj = doc.toObject(FirestoreTrip::class.java)
        if (tripObj != null) {
          val tripEntity = TripEntity(
            id = doc.id,
            name = tripObj.name.ifBlank { "Untitled Trip" },
            description = tripObj.description,
            startDate = tripObj.startDate.ifBlank { "Ongoing" },
            endDate = tripObj.endDate.ifBlank { "Ongoing" },
            currencyCode = tripObj.currencyCode.ifBlank { "INR" },
            budgetMinor = tripObj.budgetMinor,
            status = tripObj.status.ifBlank { "ACTIVE" },
            createdBy = tripObj.createdBy.ifBlank { uid }
          )
          dao.insertTrip(tripEntity)
          fetchedTrips.add(tripEntity)

          // Ensure membership entity exists in Room
          val currentMembers = dao.getTripMembersSync(doc.id)
          if (currentMembers.none { it.userId == uid }) {
            val role = if (tripObj.createdBy == uid) "CREATOR" else "MEMBER"
            dao.insertTripMember(
              TripMemberEntity(
                tripId = doc.id,
                userId = uid,
                role = role,
                targetMinor = 0L
              )
            )
          }
        }
      }
      Result.success(fetchedTrips)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "trips")
      Result.failure(e)
    }
  }

  suspend fun createTrip(
    name: String,
    description: String,
    startDate: String = "",
    endDate: String = "",
    budgetPaise: Long = 0L,
    currencyCode: String = "INR",
    initialMemberIds: List<String> = emptyList()
  ): String {
    val tripId = "trip_" + UUID.randomUUID().toString().take(8)
    val currentUid = requireUserId()
    val cleanCurrency = currencyCode.trim().uppercase().ifBlank { "INR" }
    val trip = TripEntity(
      id = tripId,
      name = name.trim(),
      description = description.trim(),
      startDate = startDate,
      endDate = endDate,
      currencyCode = cleanCurrency,
      budgetMinor = budgetPaise,
      createdBy = currentUid
    )
    dao.insertTrip(trip)

    // Add creator as CREATOR
    val membersToInsert = mutableListOf<TripMemberEntity>()
    membersToInsert.add(
      TripMemberEntity(
        tripId = tripId,
        userId = currentUid,
        role = "CREATOR",
        targetMinor = if (initialMemberIds.isEmpty()) budgetPaise else budgetPaise / (initialMemberIds.size + 1)
      )
    )

    // Add selected initial members
    val memberCount = initialMemberIds.size + 1
    val individualTarget = if (memberCount > 0) budgetPaise / memberCount else 0L

    for (uid in initialMemberIds) {
      if (uid != currentUid) {
        membersToInsert.add(
          TripMemberEntity(
            tripId = tripId,
            userId = uid,
            role = "MEMBER",
            targetMinor = individualTarget
          )
        )
      }
    }
    dao.insertTripMembers(membersToInsert)

    // Sync to Firestore
    try {
      val allMemberUids = (listOf(currentUid) + initialMemberIds).distinct()
      val tripMap = mapOf(
        "name" to name.trim(),
        "description" to description.trim(),
        "startDate" to startDate,
        "endDate" to endDate,
        "currencyCode" to cleanCurrency,
        "budgetMinor" to budgetPaise,
        "status" to "ACTIVE",
        "createdBy" to currentUid,
        "memberIds" to allMemberUids,
        "createdAt" to FieldValue.serverTimestamp()
      )
      firestore.collection("trips").document(tripId).set(tripMap).await()

      val memberMap = mapOf(
        "tripId" to tripId,
        "userId" to currentUid,
        "role" to "CREATOR",
        "targetMinor" to budgetPaise,
        "joinedAt" to FieldValue.serverTimestamp()
      )
      firestore.collection("trips").document(tripId).collection("members").document(currentUid).set(memberMap).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "trips/$tripId")
    }

    // Audit log
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = currentUid,
        action = "TRIP_CREATED",
        details = "Created trip '${name.trim()}' with budget ${MoneyUtils.formatPaise(budgetPaise)}"
      )
    )

    return tripId
  }

  suspend fun joinTrip(tripIdOrCode: String): Result<String> {
    val cleanId = tripIdOrCode.trim()
    val trip = dao.getTripSync(cleanId)
      ?: return Result.failure(Exception("Trip not found for code: $cleanId"))

    val existingMembers = dao.getTripMembersSync(trip.id)
    if (existingMembers.any { it.userId == CURRENT_USER_ID }) {
      return Result.success(trip.id)
    }

    // Add as member
    dao.insertTripMember(
      TripMemberEntity(
        tripId = trip.id,
        userId = CURRENT_USER_ID,
        role = "MEMBER",
        targetMinor = 0L
      )
    )

    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = trip.id,
        actorUserId = CURRENT_USER_ID,
        action = "MEMBER_JOINED",
        details = "User joined trip using code/link"
      )
    )

    return Result.success(trip.id)
  }

  /**
   * Looks up a user by email, first checking Firestore "users" collection,
   * then falling back to local cached profiles.
   */
  suspend fun findUserByEmail(email: String): Result<UserProfileEntity?> {
    val cleanEmail = email.trim().lowercase()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address"))
    }

    try {
      // Query Firestore users collection for matching email
      val snapshot = firestore.collection("users")
        .whereEqualTo("email", cleanEmail)
        .limit(1)
        .get()
        .await()

      if (!snapshot.isEmpty) {
        val doc = snapshot.documents.first()
        val uid = doc.getString("userId") ?: doc.id
        val profile = UserProfileEntity(
          userId = uid,
          email = doc.getString("email") ?: cleanEmail,
          displayName = doc.getString("displayName") ?: cleanEmail.substringBefore("@"),
          username = doc.getString("username") ?: cleanEmail.substringBefore("@").replace(".", "_"),
          avatarKey = doc.getString("avatarKey") ?: "compass"
        )
        // Cache profile locally
        dao.insertProfile(profile)
        return Result.success(profile)
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "users")
      // Non-fatal, check local storage next
    }

    // Check local Room profiles as fallback / offline cache
    val localUser = dao.getProfileByEmailSync(cleanEmail)
    return Result.success(localUser)
  }

  suspend fun addMemberToTrip(tripId: String, userId: String, role: String = "MEMBER"): Result<Unit> {
    return try {
      val existing = dao.getTripMembersSync(tripId)
      if (existing.any { it.userId == userId }) {
        return Result.failure(IllegalStateException("User is already a member of this trip"))
      }

      // Add to local Room
      dao.insertTripMember(
        TripMemberEntity(
          tripId = tripId,
          userId = userId,
          role = role,
          targetMinor = 0L
        )
      )

      dao.insertAuditLog(
        AuditLogEntity(
          id = UUID.randomUUID().toString(),
          tripId = tripId,
          actorUserId = CURRENT_USER_ID,
          action = "MEMBER_ADDED",
          details = "Added member $userId with role $role"
        )
      )

      // Sync member to Firestore trip and update memberIds array
      try {
        val tripDocRef = firestore.collection("trips").document(tripId)
        tripDocRef.update("memberIds", FieldValue.arrayUnion(userId)).await()

        val memberMap = mapOf(
          "tripId" to tripId,
          "userId" to userId,
          "role" to role,
          "targetMinor" to 0L,
          "joinedAt" to FieldValue.serverTimestamp()
        )
        tripDocRef.collection("members").document(userId).set(memberMap).await()
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "trips/$tripId/members/$userId")
      }

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun removeMemberFromTrip(tripId: String, userId: String) {
    dao.removeTripMember(tripId, userId)
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = CURRENT_USER_ID,
        action = "MEMBER_REMOVED",
        details = "Removed member $userId"
      )
    )
  }

  suspend fun updateMemberRole(tripId: String, userId: String, newRole: String) {
    dao.updateMemberRole(tripId, userId, newRole)
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = CURRENT_USER_ID,
        action = "ROLE_UPDATED",
        details = "Updated member $userId role to $newRole"
      )
    )
  }

  suspend fun updateTripBudget(tripId: String, newBudgetPaise: Long, recalculateTargets: Boolean) {
    dao.updateTripBudget(tripId, newBudgetPaise)
    if (recalculateTargets) {
      val members = dao.getTripMembersSync(tripId)
      if (members.isNotEmpty()) {
        val targetEach = newBudgetPaise / members.size
        for (m in members) {
          dao.updateMemberTarget(tripId, m.userId, targetEach)
        }
      }
    }
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = CURRENT_USER_ID,
        action = "BUDGET_UPDATED",
        details = "Budget changed to ${MoneyUtils.formatPaise(newBudgetPaise)}"
      )
    )
  }

  suspend fun updateTripStatus(tripId: String, status: String) {
    dao.updateTripStatus(tripId, status)
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = CURRENT_USER_ID,
        action = "STATUS_CHANGED",
        details = "Trip status updated to $status"
      )
    )
  }

  // --- Members Flow ---
  fun getTripMembers(tripId: String): Flow<List<TripMemberEntity>> = dao.getTripMembers(tripId)

  // --- Contributions ---
  fun getContributions(tripId: String): Flow<List<ContributionEntity>> = dao.getContributions(tripId)

  suspend fun recordContribution(
    tripId: String,
    memberUserId: String,
    amountPaise: Long,
    note: String
  ) {
    val contribution = ContributionEntity(
      id = "cnt_" + UUID.randomUUID().toString().take(8),
      tripId = tripId,
      userId = memberUserId,
      amountMinor = amountPaise,
      recordedBy = CURRENT_USER_ID,
      note = note.trim()
    )
    dao.insertContribution(contribution)

    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = CURRENT_USER_ID,
        action = "CONTRIBUTION_RECORDED",
        details = "Recorded contribution ${MoneyUtils.formatPaise(amountPaise)} for member $memberUserId"
      )
    )

    dao.insertNotification(
      NotificationEntity(
        id = UUID.randomUUID().toString(),
        userId = memberUserId,
        tripId = tripId,
        type = "CONTRIBUTION_RECORDED",
        title = "Contribution Recorded",
        body = "Received ${MoneyUtils.formatPaise(amountPaise)} into the trip wallet."
      )
    )
  }

  // --- Expenses ---
  fun getExpenses(tripId: String): Flow<List<ExpenseEntity>> = dao.getExpenses(tripId)
  fun getAllPayers(tripId: String): Flow<List<ExpensePayerEntity>> = dao.getAllPayersForTrip(tripId)
  fun getAllParticipants(tripId: String): Flow<List<ExpenseParticipantEntity>> = dao.getAllParticipantsForTrip(tripId)

  suspend fun getExpenseDetails(expenseId: String): Triple<ExpenseEntity?, List<ExpensePayerEntity>, List<ExpenseParticipantEntity>> {
    val expense = dao.getExpenseById(expenseId)
    val payers = dao.getPayersForExpense(expenseId)
    val participants = dao.getParticipantsForExpense(expenseId)
    return Triple(expense, payers, participants)
  }

  suspend fun createExpense(
    tripId: String,
    title: String,
    amountPaise: Long,
    category: String,
    location: String,
    description: String,
    payers: Map<String, Long>,
    participants: Map<String, Long>,
    receiptNote: String?
  ): Result<String> {
    // Validate payer amounts sum to total
    val payersSum = payers.values.sum()
    if (payersSum != amountPaise) {
      return Result.failure(Exception("Sum of payer amounts (${MoneyUtils.formatPaise(payersSum)}) must equal total (${MoneyUtils.formatPaise(amountPaise)})"))
    }

    // Validate participant shares sum to total
    val participantsSum = participants.values.sum()
    if (participantsSum != amountPaise) {
      return Result.failure(Exception("Sum of participant shares (${MoneyUtils.formatPaise(participantsSum)}) must equal total (${MoneyUtils.formatPaise(amountPaise)})"))
    }

    val currentUid = requireUserId()
    val members = dao.getTripMembersSync(tripId)
    val currentUserMember = members.find { it.userId == currentUid }
    val isAutoApproved = currentUserMember?.role in listOf("CREATOR", "MANAGER") || members.isEmpty()

    val expenseId = "exp_" + UUID.randomUUID().toString().take(8)
    val expense = ExpenseEntity(
      id = expenseId,
      tripId = tripId,
      title = title.trim(),
      amountMinor = amountPaise,
      category = category,
      location = location.trim(),
      description = description.trim(),
      status = if (isAutoApproved) "APPROVED" else "PENDING",
      createdBy = currentUid,
      approvedBy = if (isAutoApproved) currentUid else null,
      approvedAt = if (isAutoApproved) System.currentTimeMillis() else null,
      receiptNote = receiptNote?.trim()
    )

    val payerEntities = payers.map {
      ExpensePayerEntity(expenseId = expenseId, userId = it.key, amountMinor = it.value)
    }

    val participantEntities = participants.map {
      ExpenseParticipantEntity(expenseId = expenseId, userId = it.key, shareMinor = it.value)
    }

    dao.createExpenseWithSplits(expense, payerEntities, participantEntities)

    // Sync to Firestore
    try {
      val expenseMap = mutableMapOf<String, Any>(
        "tripId" to tripId,
        "title" to title.trim(),
        "amountMinor" to amountPaise,
        "category" to category,
        "location" to location.trim(),
        "description" to description.trim(),
        "expenseDate" to System.currentTimeMillis(),
        "status" to if (isAutoApproved) "APPROVED" else "PENDING",
        "createdBy" to currentUid,
        "payers" to payers,
        "participants" to participants,
        "payerIds" to payers.keys.toList(),
        "participantIds" to participants.keys.toList(),
        "createdAt" to FieldValue.serverTimestamp()
      )
      if (isAutoApproved) {
        expenseMap["approvedBy"] = currentUid
        expenseMap["approvedAt"] = System.currentTimeMillis()
      }
      if (receiptNote != null) {
        expenseMap["receiptNote"] = receiptNote.trim()
      }
      firestore.collection("trips").document(tripId).collection("expenses").document(expenseId).set(expenseMap).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "trips/$tripId/expenses/$expenseId")
    }

    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = currentUid,
        action = if (isAutoApproved) "EXPENSE_ADDED" else "EXPENSE_SUBMITTED_FOR_APPROVAL",
        details = "Added expense '$title' for ${MoneyUtils.formatPaise(amountPaise)}"
      )
    )

    if (!isAutoApproved) {
      // Notify creator/managers
      for (m in members.filter { it.role in listOf("CREATOR", "MANAGER") }) {
        dao.insertNotification(
          NotificationEntity(
            id = UUID.randomUUID().toString(),
            userId = m.userId,
            tripId = tripId,
            type = "EXPENSE_PENDING",
            title = "Expense Approval Needed",
            body = "Expense '$title' (${MoneyUtils.formatPaise(amountPaise)}) requires approval."
          )
        )
      }
    }

    return Result.success(expenseId)
  }

  suspend fun approveExpense(expenseId: String) {
    val expense = dao.getExpenseById(expenseId) ?: return
    dao.updateExpenseStatus(
      expenseId = expenseId,
      status = "APPROVED",
      approvedBy = CURRENT_USER_ID,
      approvedAt = System.currentTimeMillis(),
      reason = null
    )
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = expense.tripId,
        actorUserId = CURRENT_USER_ID,
        action = "EXPENSE_APPROVED",
        details = "Approved expense '${expense.title}'"
      )
    )
  }

  suspend fun rejectExpense(expenseId: String, reason: String) {
    val expense = dao.getExpenseById(expenseId) ?: return
    dao.updateExpenseStatus(
      expenseId = expenseId,
      status = "REJECTED",
      approvedBy = CURRENT_USER_ID,
      approvedAt = System.currentTimeMillis(),
      reason = reason.trim()
    )
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = expense.tripId,
        actorUserId = CURRENT_USER_ID,
        action = "EXPENSE_REJECTED",
        details = "Rejected expense '${expense.title}': $reason"
      )
    )
  }

  suspend fun deleteExpense(expenseId: String) {
    val expense = dao.getExpenseById(expenseId) ?: return
    dao.deleteExpense(expenseId)
    dao.deletePayers(expenseId)
    dao.deleteParticipants(expenseId)
    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = expense.tripId,
        actorUserId = CURRENT_USER_ID,
        action = "EXPENSE_DELETED",
        details = "Deleted expense '${expense.title}'"
      )
    )
  }

  // --- Settlements ---
  fun getSettlements(tripId: String): Flow<List<SettlementEntity>> = dao.getSettlements(tripId)

  suspend fun computeTripSettlement(tripId: String): SettlementResult {
    // 1. Attempt to compute directly from Firestore group expense data
    try {
      val expensesSnap = firestore.collection("trips").document(tripId)
        .collection("expenses")
        .whereEqualTo("status", "APPROVED")
        .get()
        .await()
      val firestoreExpenses = expensesSnap.documents.mapNotNull { it.toObject(FirestoreExpense::class.java) }

      val membersSnap = firestore.collection("trips").document(tripId)
        .collection("members")
        .get()
        .await()
      val firestoreMembers = membersSnap.documents.mapNotNull { it.toObject(FirestoreTripMember::class.java) }

      val contribsSnap = firestore.collection("trips").document(tripId)
        .collection("contributions")
        .get()
        .await()
      val firestoreContribs = contribsSnap.documents.mapNotNull { it.toObject(FirestoreContribution::class.java) }

      val settlementsSnap = firestore.collection("trips").document(tripId)
        .collection("settlements")
        .whereEqualTo("status", "PAID")
        .get()
        .await()
      val firestoreSettlements = settlementsSnap.documents.mapNotNull { it.toObject(FirestoreSettlement::class.java) }

      val profiles = dao.getAllProfilesSync().associateBy { it.userId }
      val firestoreProfiles = profiles.mapValues { (_, p) ->
        FirestoreUserProfile(
          userId = p.userId,
          displayName = p.displayName,
          email = p.email,
          avatarKey = p.avatarKey
        )
      }

      if (firestoreExpenses.isNotEmpty() || firestoreMembers.isNotEmpty()) {
        return SettlementCalculator.calculateFromFirestore(
          tripMembers = firestoreMembers,
          userProfiles = firestoreProfiles,
          expenses = firestoreExpenses,
          contributions = firestoreContribs,
          settlements = firestoreSettlements
        )
      }
    } catch (e: Exception) {
      Log.w("HisabTripRepo", "Failed to compute settlement from Firestore, falling back to local DB: ${e.message}")
    }

    // 2. Fallback to local Room database cache
    val members = dao.getTripMembersSync(tripId)
    val allProfiles = dao.getAllProfilesSync().associateBy { it.userId }
    val approvedExpenses = dao.getExpensesSync(tripId).filter { it.status == "APPROVED" }
    val approvedExpenseIds = approvedExpenses.map { it.id }.toSet()

    val allPayers = dao.getAllPayersForTripSync(tripId).filter { it.expenseId in approvedExpenseIds }
    val allParticipants = dao.getAllParticipantsForTripSync(tripId).filter { it.expenseId in approvedExpenseIds }
    val contributions = dao.getContributionsSync(tripId)
    val recordedSettlements = dao.getSettlementsSync(tripId).filter { it.status == "PAID" }

    val memberInfos = members.map { m ->
      val profile = allProfiles[m.userId]
      MemberInfo(
        userId = m.userId,
        displayName = profile?.displayName ?: m.userId,
        avatarKey = profile?.avatarKey ?: "compass"
      )
    }

    val expensesPaidMap = allPayers.groupBy { it.userId }
      .mapValues { entry -> entry.value.sumOf { it.amountMinor } }

    val expenseSharesMap = allParticipants.groupBy { it.userId }
      .mapValues { entry -> entry.value.sumOf { it.shareMinor } }

    val contributionsPaidMap = contributions.groupBy { it.userId }
      .mapValues { entry -> entry.value.sumOf { it.amountMinor } }

    val targetsMap = members.associate { it.userId to it.targetMinor }

    val settledPayments = recordedSettlements.map {
      SettledPayment(
        fromUserId = it.fromUserId,
        toUserId = it.toUserId,
        amountPaise = it.amountMinor
      )
    }

    return SettlementCalculator.calculate(
      members = memberInfos,
      expensesPaid = expensesPaidMap,
      expenseShares = expenseSharesMap,
      contributionsPaid = contributionsPaidMap,
      contributionTargets = targetsMap,
      settledPayments = settledPayments
    )
  }

  suspend fun recordSettlementPaid(
    tripId: String,
    fromUserId: String,
    toUserId: String,
    amountPaise: Long,
    note: String
  ) {
    val settlementId = "stl_" + UUID.randomUUID().toString().take(8)
    val settlement = SettlementEntity(
      id = settlementId,
      tripId = tripId,
      fromUserId = fromUserId,
      toUserId = toUserId,
      amountMinor = amountPaise,
      status = "PAID",
      paidAt = System.currentTimeMillis(),
      paymentNote = note.trim()
    )
    dao.insertSettlement(settlement)

    // Sync recorded settlement to Firestore
    try {
      val settlementMap = mapOf(
        "tripId" to tripId,
        "fromUserId" to fromUserId,
        "toUserId" to toUserId,
        "amountMinor" to amountPaise,
        "status" to "PAID",
        "paidAt" to settlement.paidAt,
        "paymentNote" to note.trim(),
        "createdAt" to FieldValue.serverTimestamp()
      )
      firestore.collection("trips").document(tripId)
        .collection("settlements").document(settlementId).set(settlementMap).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "trips/$tripId/settlements/$settlementId")
    }

    dao.insertAuditLog(
      AuditLogEntity(
        id = UUID.randomUUID().toString(),
        tripId = tripId,
        actorUserId = CURRENT_USER_ID,
        action = "SETTLEMENT_PAID",
        details = "Settled payment of ${MoneyUtils.formatPaise(amountPaise)} from $fromUserId to $toUserId"
      )
    )

    dao.insertNotification(
      NotificationEntity(
        id = UUID.randomUUID().toString(),
        userId = toUserId,
        tripId = tripId,
        type = "SETTLEMENT_RECORDED",
        title = "Settlement Payment Recorded",
        body = "Received ${MoneyUtils.formatPaise(amountPaise)} settlement."
      )
    )
  }

  // --- Audit Logs ---
  fun getAuditLogs(tripId: String): Flow<List<AuditLogEntity>> = dao.getAuditLogs(tripId)

  // --- Notifications ---
  fun getNotifications(): Flow<List<NotificationEntity>> = dao.getNotifications(CURRENT_USER_ID)
  suspend fun markNotificationRead(id: String) = dao.markNotificationRead(id)
  suspend fun markAllNotificationsRead() = dao.markAllNotificationsRead(CURRENT_USER_ID)

  // --- Export ---
  suspend fun generateTripExportSummary(tripId: String): String {
    val trip = dao.getTripSync(tripId) ?: return "Trip not found"
    val members = dao.getTripMembersSync(tripId)
    val profiles = dao.getAllProfilesSync().associateBy { it.userId }
    val expenses = dao.getExpensesSync(tripId)
    val contributions = dao.getContributionsSync(tripId)
    val settlement = computeTripSettlement(tripId)

    val totalSpent = expenses.filter { it.status == "APPROVED" }.sumOf { it.amountMinor }
    val totalCollected = contributions.sumOf { it.amountMinor }

    val sb = StringBuilder()
    sb.appendLine("=========================================")
    sb.appendLine("       HISABTRIP FINANCIAL REPORT        ")
    sb.appendLine("=========================================")
    sb.appendLine("Trip Name: ${trip.name}")
    sb.appendLine("Description: ${trip.description}")
    sb.appendLine("Dates: ${trip.startDate} to ${trip.endDate}")
    sb.appendLine("Status: ${trip.status}")
    sb.appendLine("Total Budget: ${MoneyUtils.formatPaise(trip.budgetMinor)}")
    sb.appendLine("Total Collected: ${MoneyUtils.formatPaise(totalCollected)}")
    sb.appendLine("Total Approved Spend: ${MoneyUtils.formatPaise(totalSpent)}")
    val diff = trip.budgetMinor - totalSpent
    if (diff >= 0) {
      sb.appendLine("Remaining Budget: ${MoneyUtils.formatPaise(diff)}")
    } else {
      sb.appendLine("OVER BUDGET BY: ${MoneyUtils.formatPaise(-diff)}")
    }
    sb.appendLine()
    sb.appendLine("---------------- MEMBERS ----------------")
    for (m in members) {
      val p = profiles[m.userId]
      sb.appendLine("- ${p?.displayName ?: m.userId} (@${p?.username ?: ""}) [Role: ${m.role}]")
    }
    sb.appendLine()
    sb.appendLine("--------- MEMBER NET BALANCES -----------")
    for (b in settlement.memberBalances) {
      val statusText = if (b.netBalancePaise >= 0) {
        "RECEIVES ${MoneyUtils.formatPaise(b.netBalancePaise)}"
      } else {
        "OWES ${MoneyUtils.formatPaise(-b.netBalancePaise)}"
      }
      sb.appendLine("${b.displayName}: Paid ${MoneyUtils.formatPaise(b.totalExpensesPaidPaise)}, Share ${MoneyUtils.formatPaise(b.totalExpenseSharePaise)} => $statusText")
    }
    sb.appendLine()
    sb.appendLine("------ OPTIMIZED SETTLEMENT TRANSFERS ---")
    if (settlement.transfers.isEmpty()) {
      sb.appendLine("All balances are settled (₹0.00 outstanding).")
    } else {
      for (t in settlement.transfers) {
        sb.appendLine("${t.fromUserName} -> pays -> ${t.toUserName} : ${MoneyUtils.formatPaise(t.amountPaise)}")
      }
    }
    sb.appendLine()
    sb.appendLine("--------------- EXPENSES ----------------")
    for (e in expenses) {
      sb.appendLine("[${e.status}] ${e.title} - ${MoneyUtils.formatPaise(e.amountMinor)} (${e.category})")
    }
    sb.appendLine("=========================================")
    return sb.toString()
  }

  suspend fun getFirestoreTrip(tripId: String): FirestoreTrip? {
    val snapshot = firestore.collection("trips").document(tripId).get().await()
    return snapshot.toObject(FirestoreTrip::class.java)
  }

  suspend fun getFirestoreExpense(tripId: String, expenseId: String): FirestoreExpense? {
    val snapshot = firestore.collection("trips").document(tripId)
      .collection("expenses").document(expenseId).get().await()
    return snapshot.toObject(FirestoreExpense::class.java)
  }
}

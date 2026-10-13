package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuditLogEntity
import com.example.data.local.ContributionEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpenseParticipantEntity
import com.example.data.local.ExpensePayerEntity
import com.example.data.local.HisabTripDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.SettlementEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.HisabTripRepository
import com.example.domain.settlement.SettlementResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
  SPLASH,
  SIGN_IN,
  HOME,
  TRIPS_LIST,
  TRIP_DASHBOARD,
  NOTIFICATIONS,
  PROFILE,
  CREATE_TRIP,
  EXPENSE_ANALYTICS
}

enum class TripTab {
  OVERVIEW,
  WALLET,
  EXPENSES,
  ANALYTICS,
  SETTLEMENT,
  MEMBERS,
  SETTINGS
}

class HisabTripViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: HisabTripRepository = run {
    val db = HisabTripDatabase.getDatabase(application)
    val databaseId = application.getString(com.example.R.string.firestore_database_id)
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(databaseId)
    HisabTripRepository(db.hisabTripDao(), firestore)
  }

  private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _selectedTripId = MutableStateFlow<String?>(null)
  val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

  private val _selectedTripTab = MutableStateFlow(TripTab.OVERVIEW)
  val selectedTripTab: StateFlow<TripTab> = _selectedTripTab.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  val currentProfile: StateFlow<UserProfileEntity?> = repository.getCurrentProfile()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val allProfiles: StateFlow<List<UserProfileEntity>> = repository.getAllProfiles()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val trips: StateFlow<List<TripEntity>> = repository.getAllTrips()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allTripMembers: StateFlow<List<TripMemberEntity>> = repository.getAllTripMembers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allExpenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val notifications: StateFlow<List<NotificationEntity>> = repository.getNotifications()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val unreadNotificationsCount: StateFlow<Int> = notifications
    .map { list -> list.count { !it.read } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val selectedTrip: StateFlow<TripEntity?> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getTrip(id) else flowOf(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val selectedTripMembers: StateFlow<List<TripMemberEntity>> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getTripMembers(id) else flowOf(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val selectedTripExpenses: StateFlow<List<ExpenseEntity>> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getExpenses(id) else flowOf(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val selectedTripPayers: StateFlow<List<ExpensePayerEntity>> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getAllPayers(id) else flowOf(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val selectedTripParticipants: StateFlow<List<ExpenseParticipantEntity>> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getAllParticipants(id) else flowOf(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val selectedTripContributions: StateFlow<List<ContributionEntity>> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getContributions(id) else flowOf(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val selectedTripSettlements: StateFlow<List<SettlementEntity>> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getSettlements(id) else flowOf(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val selectedTripAuditLogs: StateFlow<List<AuditLogEntity>> = _selectedTripId
    .flatMapLatest { id -> if (id != null) repository.getAuditLogs(id) else flowOf(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _settlementResult = MutableStateFlow<SettlementResult?>(null)
  val settlementResult: StateFlow<SettlementResult?> = _settlementResult.asStateFlow()

  private val _isSyncingTrips = MutableStateFlow(false)
  val isSyncingTrips: StateFlow<Boolean> = _isSyncingTrips.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureDefaultUserInitialized()
      fetchUserTrips()
    }
  }

  fun fetchUserTrips() {
    viewModelScope.launch {
      _isSyncingTrips.value = true
      try {
        repository.fetchTripsFromFirestore()
      } catch (e: Exception) {
        // Log or show error if needed
      } finally {
        _isSyncingTrips.value = false
      }
    }
  }

  fun navigateTo(screen: AppScreen) {
    _currentScreen.value = screen
  }

  fun openTrip(tripId: String, tab: TripTab = TripTab.OVERVIEW) {
    _selectedTripId.value = tripId
    _selectedTripTab.value = tab
    _currentScreen.value = AppScreen.TRIP_DASHBOARD
    refreshSettlement()
  }

  fun setTripTab(tab: TripTab) {
    _selectedTripTab.value = tab
    if (tab == TripTab.SETTLEMENT) {
      refreshSettlement()
    }
  }

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  fun showToast(message: String) {
    _snackbarMessage.value = message
  }

  fun refreshSettlement() {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        _settlementResult.value = repository.computeTripSettlement(tripId)
      } catch (_: Exception) {}
    }
  }

  fun createTrip(
    name: String,
    description: String,
    startDate: String = "",
    endDate: String = "",
    budgetPaise: Long = 0L,
    currencyCode: String = "INR",
    initialMemberIds: List<String> = emptyList(),
    onSuccess: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val tripId = repository.createTrip(
          name = name,
          description = description,
          startDate = startDate,
          endDate = endDate,
          budgetPaise = budgetPaise,
          currencyCode = currencyCode,
          initialMemberIds = initialMemberIds
        )
        showToast("Trip '$name' initialized successfully!")
        openTrip(tripId)
        onSuccess(tripId)
      } catch (e: Exception) {
        showToast("Error creating trip: ${e.localizedMessage}")
      }
    }
  }

  suspend fun initializeTrip(
    title: String,
    description: String,
    currency: String,
    startDate: String = "",
    endDate: String = "",
    budgetMinor: Long = 0L,
    memberIds: List<String> = emptyList()
  ): Result<String> {
    return try {
      val tripId = repository.createTrip(
        name = title.trim(),
        description = description.trim(),
        startDate = startDate,
        endDate = endDate,
        budgetPaise = budgetMinor,
        currencyCode = currency.trim().uppercase(),
        initialMemberIds = memberIds
      )
      openTrip(tripId)
      showToast("Trip '${title.trim()}' saved to Firestore!")
      Result.success(tripId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun joinTrip(codeOrId: String, onSuccess: (String) -> Unit) {
    viewModelScope.launch {
      try {
        val result = repository.joinTrip(codeOrId)
        result.onSuccess { tripId ->
          showToast("Joined trip successfully!")
          openTrip(tripId)
          onSuccess(tripId)
        }.onFailure { err ->
          showToast(err.localizedMessage ?: "Failed to join trip")
        }
      } catch (e: Exception) {
        showToast("Failed to join trip: ${e.localizedMessage}")
      }
    }
  }

  fun seedSampleTrips() {
    viewModelScope.launch {
      try {
        repository.createTrip(
          name = "Manali Snow Expedition",
          description = "Solang valley, river rafting & scenic mountain cottage retreat",
          startDate = "15 Oct 2026",
          endDate = "22 Oct 2026",
          budgetPaise = 4500000L,
          initialMemberIds = listOf("usr_aarav_101", "usr_priya_202", "usr_vikram_303")
        )
        repository.createTrip(
          name = "Goa Coastal Road Trip",
          description = "Sunset drives, beach shacks, water sports and seafood dinners",
          startDate = "28 Oct 2026",
          endDate = "04 Nov 2026",
          budgetPaise = 6000000L,
          initialMemberIds = listOf("usr_aarav_101", "usr_priya_202")
        )
        repository.createTrip(
          name = "Rajasthan Heritage & Desert",
          description = "Royal forts, dunes camel ride & Rajasthani folk music under stars",
          startDate = "12 Nov 2026",
          endDate = "18 Nov 2026",
          budgetPaise = 3500000L,
          initialMemberIds = listOf("usr_vikram_303")
        )
        showToast("Created sample travel itineraries!")
      } catch (e: Exception) {
        showToast("Error creating sample trips: ${e.localizedMessage}")
      }
    }
  }

  fun recordContribution(memberUserId: String, amountPaise: Long, note: String) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        repository.recordContribution(tripId, memberUserId, amountPaise, note)
        showToast("Contribution recorded!")
        refreshSettlement()
      } catch (e: Exception) {
        showToast("Failed to record contribution: ${e.localizedMessage}")
      }
    }
  }

  fun createExpense(
    title: String,
    amountPaise: Long,
    category: String,
    location: String,
    description: String,
    payers: Map<String, Long>,
    participants: Map<String, Long>,
    receiptNote: String?,
    onSuccess: () -> Unit
  ) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        val result = repository.createExpense(
          tripId = tripId,
          title = title,
          amountPaise = amountPaise,
          category = category,
          location = location,
          description = description,
          payers = payers,
          participants = participants,
          receiptNote = receiptNote
        )
        result.onSuccess {
          showToast("Expense logged successfully!")
          refreshSettlement()
          onSuccess()
        }.onFailure { err ->
          showToast(err.localizedMessage ?: "Expense validation failed")
        }
      } catch (e: Exception) {
        showToast("Failed to log expense: ${e.localizedMessage}")
      }
    }
  }

  fun approveExpense(expenseId: String) {
    viewModelScope.launch {
      try {
        repository.approveExpense(expenseId)
        showToast("Expense approved!")
        refreshSettlement()
      } catch (e: Exception) {
        showToast("Failed to approve expense: ${e.localizedMessage}")
      }
    }
  }

  fun rejectExpense(expenseId: String, reason: String) {
    viewModelScope.launch {
      try {
        repository.rejectExpense(expenseId, reason)
        showToast("Expense rejected")
        refreshSettlement()
      } catch (e: Exception) {
        showToast("Failed to reject expense: ${e.localizedMessage}")
      }
    }
  }

  fun deleteExpense(expenseId: String) {
    viewModelScope.launch {
      try {
        repository.deleteExpense(expenseId)
        showToast("Expense deleted")
        refreshSettlement()
      } catch (e: Exception) {
        showToast("Failed to delete expense: ${e.localizedMessage}")
      }
    }
  }

  fun recordSettlementPayment(fromUserId: String, toUserId: String, amountPaise: Long, note: String) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        repository.recordSettlementPaid(tripId, fromUserId, toUserId, amountPaise, note)
        showToast("Settlement payment recorded!")
        refreshSettlement()
      } catch (e: Exception) {
        showToast("Failed to record settlement: ${e.localizedMessage}")
      }
    }
  }

  suspend fun lookupUserByEmail(email: String): Result<UserProfileEntity?> {
    return repository.findUserByEmail(email)
  }

  fun addMemberByEmail(
    email: String,
    role: String = "MEMBER",
    onSuccess: (UserProfileEntity) -> Unit,
    onError: (String) -> Unit
  ) {
    val tripId = _selectedTripId.value
    if (tripId == null) {
      onError("No active trip selected")
      return
    }

    viewModelScope.launch {
      val lookupResult = repository.findUserByEmail(email)
      lookupResult.onFailure { err ->
        val msg = err.localizedMessage ?: "Invalid email address"
        onError(msg)
        showToast(msg)
      }.onSuccess { profile ->
        if (profile == null) {
          val msg = "No registered user found with email '$email'"
          onError(msg)
          showToast(msg)
        } else {
          val addResult = repository.addMemberToTrip(tripId, profile.userId, role)
          addResult.onSuccess {
            showToast("${profile.displayName} added to trip!")
            refreshSettlement()
            onSuccess(profile)
          }.onFailure { err ->
            val msg = err.localizedMessage ?: "Failed to add member"
            onError(msg)
            showToast(msg)
          }
        }
      }
    }
  }

  fun addMember(userId: String, role: String = "MEMBER") {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        val result = repository.addMemberToTrip(tripId, userId, role)
        result.onSuccess {
          showToast("Member added to trip!")
          refreshSettlement()
        }.onFailure { err ->
          showToast(err.localizedMessage ?: "Failed to add member")
        }
      } catch (e: Exception) {
        showToast("Failed to add member: ${e.localizedMessage}")
      }
    }
  }

  fun removeMember(userId: String) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        repository.removeMemberFromTrip(tripId, userId)
        showToast("Member removed from trip")
        refreshSettlement()
      } catch (e: Exception) {
        showToast("Failed to remove member: ${e.localizedMessage}")
      }
    }
  }

  fun updateMemberRole(userId: String, newRole: String) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        repository.updateMemberRole(tripId, userId, newRole)
        showToast("Updated role to $newRole")
      } catch (e: Exception) {
        showToast("Failed to update role: ${e.localizedMessage}")
      }
    }
  }

  fun updateBudget(newBudgetPaise: Long, recalculateTargets: Boolean) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        repository.updateTripBudget(tripId, newBudgetPaise, recalculateTargets)
        showToast("Trip budget updated!")
      } catch (e: Exception) {
        showToast("Failed to update budget: ${e.localizedMessage}")
      }
    }
  }

  fun updateTripStatus(status: String) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      try {
        repository.updateTripStatus(tripId, status)
        showToast("Trip marked as $status")
      } catch (e: Exception) {
        showToast("Failed to update status: ${e.localizedMessage}")
      }
    }
  }

  fun updateProfile(displayName: String, username: String, avatarKey: String) {
    viewModelScope.launch {
      try {
        repository.updateProfile(displayName, username, avatarKey)
        showToast("Profile updated successfully")
      } catch (e: Exception) {
        showToast("Failed to update profile: ${e.localizedMessage}")
      }
    }
  }

  fun markNotificationRead(id: String) {
    viewModelScope.launch {
      repository.markNotificationRead(id)
    }
  }

  fun markAllNotificationsRead() {
    viewModelScope.launch {
      repository.markAllNotificationsRead()
      showToast("All notifications marked as read")
    }
  }

  fun getExportSummary(onResult: (String) -> Unit) {
    val tripId = _selectedTripId.value ?: return
    viewModelScope.launch {
      val text = repository.generateTripExportSummary(tripId)
      onResult(text)
    }
  }
}

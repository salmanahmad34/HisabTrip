package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.HisabTripDatabase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HisabTripRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private lateinit var database: HisabTripDatabase
  private lateinit var repository: HisabTripRepository

  @Before
  fun setUpRepository() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, HisabTripDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = HisabTripRepository(database.hisabTripDao(), firestore)
  }

  @After
  fun tearDownDatabase() {
    database.close()
  }

  @Test
  fun createTrip_validPayload_createsDocumentAndReturnsId() = runBlocking {
    val uid = signInTestUser("alice@test.com")
    val tripId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTrip(
        name = "Himalaya Trek",
        description = "Group mountain expedition",
        startDate = "2026-10-15",
        endDate = "2026-10-22",
        budgetPaise = 5000000L
      )
    }

    assertTrue(tripId.isNotEmpty())
    val firestoreTrip = repository.getFirestoreTrip(tripId)
    assertNotNull(firestoreTrip)
    assertEquals("Himalaya Trek", firestoreTrip?.name)
    assertEquals(uid, firestoreTrip?.createdBy)
  }

  @Test
  fun getTripById_unauthorizedUser_failsWithPermissionDenied() = runBlocking {
    signInTestUser("alice@test.com")
    val tripId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTrip(
        name = "Alice Secret Trip",
        description = "Only Alice",
        startDate = "2026-10-15",
        endDate = "2026-10-20",
        budgetPaise = 2000000L
      )
    }

    // Switch to Bob who is not a member of Alice's trip
    signInTestUser("bob@test.com")
    try {
      firestore.collection("trips").document(tripId).get().await()
      fail("Expected PERMISSION_DENIED when Bob accesses Alice's trip")
    } catch (e: Exception) {
      val isDenied = e is FirebaseFirestoreException &&
        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
      assertTrue("Expected permission denied, got: $e", isDenied)
    }
  }

  @Test
  fun createExpense_tripMember_addsExpenseSuccessfully() = runBlocking {
    val aliceUid = signInTestUser("alice@test.com")
    val tripId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTrip(
        name = "Goa Beach Trip",
        description = "Sun and sand",
        startDate = "2026-11-01",
        endDate = "2026-11-05",
        budgetPaise = 3000000L
      )
    }

    val expenseResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createExpense(
        tripId = tripId,
        title = "Seafood Dinner",
        amountPaise = 150000L,
        category = "Food & Dining",
        location = "Calangute",
        description = "Team dinner",
        payers = mapOf(aliceUid to 150000L),
        participants = mapOf(aliceUid to 150000L),
        receiptNote = "Receipt verified"
      )
    }

    assertTrue(expenseResult.isSuccess)
    val expenseId = expenseResult.getOrThrow()
    val firestoreExpense = repository.getFirestoreExpense(tripId, expenseId)
    assertNotNull(firestoreExpense)
    assertEquals("Seafood Dinner", firestoreExpense?.title)
    assertEquals(150000L, firestoreExpense?.amountMinor)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
  }
}

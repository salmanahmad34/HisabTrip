package com.example

import com.example.data.firebase.FirestoreContribution
import com.example.data.firebase.FirestoreExpense
import com.example.data.firebase.FirestoreSettlement
import com.example.data.firebase.FirestoreTripMember
import com.example.data.firebase.FirestoreUserProfile
import com.example.domain.money.MoneyUtils
import com.example.domain.settlement.MemberInfo
import com.example.domain.settlement.SettledPayment
import com.example.domain.settlement.SettlementCalculator
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpenseParticipantEntity
import com.example.data.local.ExpensePayerEntity
import com.example.data.local.TripEntity
import com.example.data.local.TripMemberEntity
import com.example.data.local.UserProfileEntity
import com.example.domain.analytics.ExpenseAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

  @Test
  fun testMoneyFormattingAndParsing() {
    // 500.50 rupees = 50050 paise
    val paise = MoneyUtils.parseToPaise("500.50")
    assertEquals(50050L, paise)

    val formatted = MoneyUtils.formatPaise(50050L)
    assertTrue(formatted.contains("500.50"))

    // Negative paise formatting
    val negFormatted = MoneyUtils.formatPaise(-25000L)
    assertTrue(negFormatted.startsWith("-"))
    assertTrue(negFormatted.contains("250"))

    // Comma-separated Indian notation input: "₹1,25,000.75"
    val parsedLarge = MoneyUtils.parseToPaise("₹1,25,000.75")
    assertEquals(12500075L, parsedLarge)

    // Invalid inputs
    assertNull(MoneyUtils.parseToPaise("abc"))
    assertNull(MoneyUtils.parseToPaise("-50"))
  }

  @Test
  fun testEqualSplitWithRemainderConservation() {
    // ₹100.00 (10000 paise) split 3 ways: 10000 / 3 = 3333 paise, remainder 1 paise
    val participants = listOf("user_1", "user_2", "user_3")
    val shares = MoneyUtils.calculateEqualSplit(10000L, participants)

    assertEquals(3, shares.size)
    assertEquals(3334L, shares["user_1"]) // received remainder paisa
    assertEquals(3333L, shares["user_2"])
    assertEquals(3333L, shares["user_3"])

    // Mathematical identity: sum of shares MUST strictly equal total
    val sum = shares.values.sum()
    assertEquals(10000L, sum)
  }

  @Test
  fun testEqualSplitSevenWaysRemainderConservation() {
    // ₹1.00 (100 paise) split 7 ways: 100 / 7 = 14 paise, remainder 2 paise
    val participants = (1..7).map { "user_$it" }
    val shares = MoneyUtils.calculateEqualSplit(100L, participants)

    assertEquals(7, shares.size)
    assertEquals(15L, shares["user_1"]) // 14 + 1
    assertEquals(15L, shares["user_2"]) // 14 + 1
    assertEquals(14L, shares["user_3"])
    assertEquals(14L, shares["user_4"])
    assertEquals(14L, shares["user_5"])
    assertEquals(14L, shares["user_6"])
    assertEquals(14L, shares["user_7"])

    val sum = shares.values.sum()
    assertEquals(100L, sum)
  }

  @Test
  fun testPercentageSplitConservation() {
    val totalPaise = 150000L // ₹1,500.00
    val percentages = mapOf(
      "user_1" to 50.0,
      "user_2" to 30.0,
      "user_3" to 20.0
    )
    val shares = MoneyUtils.calculatePercentageSplit(totalPaise, percentages)
    val sum = shares.values.sum()
    assertEquals(totalPaise, sum)
    assertEquals(75000L, shares["user_1"])
    assertEquals(45000L, shares["user_2"])
    assertEquals(30000L, shares["user_3"])
  }

  @Test
  fun testSettlementTwoMembers() {
    // Member A pays ₹1,000 (100000 paise) for A and B. Share is ₹500 each.
    val members = listOf(
      MemberInfo("user_a", "Alice", "compass"),
      MemberInfo("user_b", "Bob", "mountain")
    )
    val expensesPaid = mapOf("user_a" to 100000L, "user_b" to 0L)
    val expenseShares = mapOf("user_a" to 50000L, "user_b" to 50000L)

    val result = SettlementCalculator.calculate(
      members = members,
      expensesPaid = expensesPaid,
      expenseShares = expenseShares
    )

    assertEquals(1, result.transfers.size)
    val t = result.transfers[0]
    assertEquals("user_b", t.fromUserId)
    assertEquals("user_a", t.toUserId)
    assertEquals(50000L, t.amountPaise)
    assertTrue(result.isReconciled)
  }

  @Test
  fun testSettlementThreeMembersMultiplePayers() {
    // Alice pays ₹3,000 (300000 paise) for Alice, Bob, Charlie (100000 paise each)
    // Bob pays ₹600 (60000 paise) for Alice, Bob, Charlie (20000 paise each)
    // Charlie pays ₹0.
    // Net:
    // Alice: paid 300000, share 120000 => net +180000 (Creditor)
    // Bob: paid 60000, share 120000 => net -60000 (Debtor)
    // Charlie: paid 0, share 120000 => net -120000 (Debtor)
    val members = listOf(
      MemberInfo("alice", "Alice", "compass"),
      MemberInfo("bob", "Bob", "mountain"),
      MemberInfo("charlie", "Charlie", "camera")
    )
    val expensesPaid = mapOf("alice" to 300000L, "bob" to 60000L, "charlie" to 0L)
    val expenseShares = mapOf("alice" to 120000L, "bob" to 120000L, "charlie" to 120000L)

    val result = SettlementCalculator.calculate(
      members = members,
      expensesPaid = expensesPaid,
      expenseShares = expenseShares
    )

    // Greedy transfers should resolve Charlie -> Alice (120000) and Bob -> Alice (60000)
    assertEquals(2, result.transfers.size)
    val sumTransfers = result.transfers.sumOf { it.amountPaise }
    assertEquals(180000L, sumTransfers)
    assertTrue(result.isReconciled)
  }

  @Test
  fun testSettlementFourMembersAllZeroBalances() {
    // Everyone paid exactly their own share
    val members = listOf(
      MemberInfo("u1", "U1", "compass"),
      MemberInfo("u2", "U2", "mountain"),
      MemberInfo("u3", "U3", "tent"),
      MemberInfo("u4", "U4", "backpack")
    )
    val expensesPaid = mapOf("u1" to 25000L, "u2" to 25000L, "u3" to 25000L, "u4" to 25000L)
    val expenseShares = mapOf("u1" to 25000L, "u2" to 25000L, "u3" to 25000L, "u4" to 25000L)

    val result = SettlementCalculator.calculate(
      members = members,
      expensesPaid = expensesPaid,
      expenseShares = expenseShares
    )

    assertEquals(0, result.transfers.size)
    assertEquals(0L, result.totalSettlementAmountPaise)
    assertTrue(result.isReconciled)
  }

  @Test
  fun testSettlementMathematicalConservationIdentity() {
    // 5 members with complex expenses and upfront wallet contributions
    val members = listOf(
      MemberInfo("u1", "User 1", "compass"),
      MemberInfo("u2", "User 2", "mountain"),
      MemberInfo("u3", "User 3", "camera"),
      MemberInfo("u4", "User 4", "tent"),
      MemberInfo("u5", "User 5", "luggage")
    )
    val expensesPaid = mapOf("u1" to 500000L, "u2" to 200000L, "u3" to 0L, "u4" to 100000L, "u5" to 0L)
    val expenseShares = mapOf("u1" to 160000L, "u2" to 160000L, "u3" to 160000L, "u4" to 160000L, "u5" to 160000L)

    val result = SettlementCalculator.calculate(
      members = members,
      expensesPaid = expensesPaid,
      expenseShares = expenseShares
    )

    // The total sum of all transfers must equal sum of positive balances
    val positiveNetSum = result.memberBalances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise }
    val transferSum = result.transfers.sumOf { it.amountPaise }

    assertEquals(positiveNetSum, transferSum)
    assertTrue(result.isReconciled)
  }

  @Test
  fun testDateRangeFormatting() {
    // Same month and year: "15 Oct 2026" and "22 Oct 2026" -> "Oct 15 – 22, 2026"
    val range1 = com.example.domain.util.DateUtils.formatDateRange("15 Oct 2026", "22 Oct 2026")
    assertTrue("Expected Oct 15 – 22, 2026 but got $range1", range1.contains("Oct") && range1.contains("15") && range1.contains("22"))

    // Different months, same year
    val range2 = com.example.domain.util.DateUtils.formatDateRange("28 Oct 2026", "04 Nov 2026")
    assertTrue(range2.contains("Oct") && range2.contains("Nov"))

    // Single date
    val single = com.example.domain.util.DateUtils.formatDateRange("15 Oct 2026", "")
    assertTrue(single.contains("Oct 15, 2026"))

    // Duration calculation
    val days = com.example.domain.util.DateUtils.calculateDurationDays("15 Oct 2026", "22 Oct 2026")
    assertEquals(8, days)

    val badge = com.example.domain.util.DateUtils.formatDurationBadge("15 Oct 2026", "22 Oct 2026")
    assertTrue(badge?.contains("8 Days") == true)
  }

  @Test
  fun testSettlementWithSettledPayments() {
    // Alice paid ₹2,000 for Alice and Bob (₹1,000 each).
    // Bob initially owes ₹1,000 to Alice.
    // Bob makes a partial payment of ₹400 to Alice.
    val members = listOf(
      MemberInfo("alice", "Alice", "compass"),
      MemberInfo("bob", "Bob", "mountain")
    )
    val expensesPaid = mapOf("alice" to 200000L, "bob" to 0L)
    val expenseShares = mapOf("alice" to 100000L, "bob" to 100000L)
    val settledPayments = listOf(
      SettledPayment(fromUserId = "bob", toUserId = "alice", amountPaise = 40000L)
    )

    val result = SettlementCalculator.calculate(
      members = members,
      expensesPaid = expensesPaid,
      expenseShares = expenseShares,
      settledPayments = settledPayments
    )

    // Outstanding debt should now be ₹600 (60000 paise)
    assertEquals(1, result.transfers.size)
    val transfer = result.transfers[0]
    assertEquals("bob", transfer.fromUserId)
    assertEquals("alice", transfer.toUserId)
    assertEquals(60000L, transfer.amountPaise)
    assertTrue(result.isReconciled)
  }

  @Test
  fun testSettlementCalculationFromFirestoreData() {
    // 3 trip members
    val tripMembers = listOf(
      FirestoreTripMember(id = "m1", tripId = "trip_test", userId = "u1", role = "CREATOR"),
      FirestoreTripMember(id = "m2", tripId = "trip_test", userId = "u2", role = "MEMBER"),
      FirestoreTripMember(id = "m3", tripId = "trip_test", userId = "u3", role = "MEMBER")
    )
    val userProfiles = mapOf(
      "u1" to FirestoreUserProfile(userId = "u1", displayName = "User 1", avatarKey = "compass"),
      "u2" to FirestoreUserProfile(userId = "u2", displayName = "User 2", avatarKey = "mountain"),
      "u3" to FirestoreUserProfile(userId = "u3", displayName = "User 3", avatarKey = "tent")
    )

    // Expense 1: ₹3,000 paid by u1, split equally among u1, u2, u3 (₹1,000 each)
    val exp1 = FirestoreExpense(
      id = "exp1",
      tripId = "trip_test",
      title = "Dinner",
      amountMinor = 300000L,
      status = "APPROVED",
      createdBy = "u1",
      payers = mapOf("u1" to 300000L),
      participants = mapOf("u1" to 100000L, "u2" to 100000L, "u3" to 100000L)
    )

    // Expense 2: ₹1,500 paid by u2 for u2 and u3 only (₹750 each)
    val exp2 = FirestoreExpense(
      id = "exp2",
      tripId = "trip_test",
      title = "Cab",
      amountMinor = 150000L,
      status = "APPROVED",
      createdBy = "u2",
      payers = mapOf("u2" to 150000L),
      participants = mapOf("u2" to 75000L, "u3" to 75000L)
    )

    val result = SettlementCalculator.calculateFromFirestore(
      tripMembers = tripMembers,
      userProfiles = userProfiles,
      expenses = listOf(exp1, exp2)
    )

    // Net check:
    // u1: paid 300000, share 100000 => net +200000
    // u2: paid 150000, share 175000 => net -25000
    // u3: paid 0, share 175000 => net -175000
    // Sum of positive balances: 200000.
    // Sum of negative balances: -200000.
    // Greedy transfers:
    // u3 pays u1: 175000
    // u2 pays u1: 25000
    assertEquals(2, result.transfers.size)
    val totalToSettle = result.transfers.sumOf { it.amountPaise }
    assertEquals(200000L, totalToSettle)
    assertTrue(result.isReconciled)

    val u3Transfer = result.transfers.find { it.fromUserId == "u3" }
    assertEquals("u1", u3Transfer?.toUserId)
    assertEquals(175000L, u3Transfer?.amountPaise)

    val u2Transfer = result.transfers.find { it.fromUserId == "u2" }
    assertEquals("u1", u2Transfer?.toUserId)
    assertEquals(25000L, u2Transfer?.amountPaise)
  }

  @Test
  fun testExpenseAnalyticsCategoryBreakdown() {
    val trip = TripEntity(id = "trip1", name = "Goa Trip", currencyCode = "INR", createdBy = "u1")
    val members = listOf(
      TripMemberEntity(tripId = "trip1", userId = "u1", role = "CREATOR"),
      TripMemberEntity(tripId = "trip1", userId = "u2", role = "MEMBER")
    )
    val profiles = mapOf(
      "u1" to UserProfileEntity(userId = "u1", email = "u1@test.com", displayName = "Alice", username = "alice", avatarKey = "compass"),
      "u2" to UserProfileEntity(userId = "u2", email = "u2@test.com", displayName = "Bob", username = "bob", avatarKey = "mountain")
    )

    // Expense 1: Food ₹3,000 (300000 paise)
    val exp1 = ExpenseEntity(
      id = "e1",
      tripId = "trip1",
      title = "Dinner",
      amountMinor = 300000L,
      category = "Food",
      status = "APPROVED",
      createdBy = "u1"
    )
    // Expense 2: Travel ₹1,000 (100000 paise)
    val exp2 = ExpenseEntity(
      id = "e2",
      tripId = "trip1",
      title = "Taxi",
      amountMinor = 100000L,
      category = "Travel",
      status = "APPROVED",
      createdBy = "u2"
    )
    // Expense 3: Food ₹1,000 (100000 paise)
    val exp3 = ExpenseEntity(
      id = "e3",
      tripId = "trip1",
      title = "Lunch",
      amountMinor = 100000L,
      category = "Food",
      status = "APPROVED",
      createdBy = "u1"
    )
    // Expense 4: Pending expense ₹2,000 (should be excluded by default)
    val exp4 = ExpenseEntity(
      id = "e4",
      tripId = "trip1",
      title = "Club",
      amountMinor = 200000L,
      category = "Activity",
      status = "PENDING",
      createdBy = "u1"
    )

    val expenses = listOf(exp1, exp2, exp3, exp4)
    val payers = listOf(
      ExpensePayerEntity(expenseId = "e1", userId = "u1", amountMinor = 300000L),
      ExpensePayerEntity(expenseId = "e2", userId = "u2", amountMinor = 100000L),
      ExpensePayerEntity(expenseId = "e3", userId = "u1", amountMinor = 100000L)
    )
    val participants = listOf(
      ExpenseParticipantEntity(expenseId = "e1", userId = "u1", shareMinor = 150000L),
      ExpenseParticipantEntity(expenseId = "e1", userId = "u2", shareMinor = 150000L),
      ExpenseParticipantEntity(expenseId = "e2", userId = "u1", shareMinor = 50000L),
      ExpenseParticipantEntity(expenseId = "e2", userId = "u2", shareMinor = 50000L),
      ExpenseParticipantEntity(expenseId = "e3", userId = "u1", shareMinor = 50000L),
      ExpenseParticipantEntity(expenseId = "e3", userId = "u2", shareMinor = 50000L)
    )

    val analytics = ExpenseAnalyticsCalculator.computeAnalytics(
      trip = trip,
      expenses = expenses,
      payers = payers,
      participants = participants,
      members = members,
      profiles = profiles,
      includePending = false
    )

    // Total approved spend: 300000 + 100000 + 100000 = 500000 paise (₹5,000)
    assertEquals(500000L, analytics.totalApprovedSpendMinor)
    assertEquals(500000L, analytics.totalAllSpendMinor)
    assertEquals(3, analytics.approvedExpenseCount)
    assertEquals(1, analytics.pendingExpenseCount)

    // Categories: Food (400000L = 80.0%), Travel (100000L = 20.0%)
    assertEquals(2, analytics.categories.size)
    val foodCat = analytics.categories.find { it.category == "Food" }!!
    assertEquals(400000L, foodCat.totalAmountMinor)
    assertEquals(80.0, foodCat.percentage, 0.1)
    assertEquals(2, foodCat.count)

    val travelCat = analytics.categories.find { it.category == "Travel" }!!
    assertEquals(100000L, travelCat.totalAmountMinor)
    assertEquals(20.0, travelCat.percentage, 0.1)
    assertEquals(1, travelCat.count)

    // Top Category
    assertEquals("Food", analytics.topCategory?.category)
  }

  @Test
  fun testExpenseAnalyticsMemberPaidAndShareBreakdown() {
    val trip = TripEntity(id = "trip1", name = "Mountains", currencyCode = "INR", createdBy = "u1")
    val members = listOf(
      TripMemberEntity(tripId = "trip1", userId = "u1", role = "CREATOR"),
      TripMemberEntity(tripId = "trip1", userId = "u2", role = "MEMBER")
    )
    val profiles = mapOf(
      "u1" to UserProfileEntity(userId = "u1", email = "u1@test.com", displayName = "Alice", username = "alice", avatarKey = "compass"),
      "u2" to UserProfileEntity(userId = "u2", email = "u2@test.com", displayName = "Bob", username = "bob", avatarKey = "mountain")
    )

    val exp = ExpenseEntity(id = "e1", tripId = "trip1", title = "Villa", amountMinor = 600000L, category = "Stay", status = "APPROVED", createdBy = "u1")
    // Alice paid ₹4,000 (400000), Bob paid ₹2,000 (200000)
    val payers = listOf(
      ExpensePayerEntity(expenseId = "e1", userId = "u1", amountMinor = 400000L),
      ExpensePayerEntity(expenseId = "e1", userId = "u2", amountMinor = 200000L)
    )
    // Alice consumed ₹3,000 (300000), Bob consumed ₹3,000 (300000)
    val participants = listOf(
      ExpenseParticipantEntity(expenseId = "e1", userId = "u1", shareMinor = 300000L),
      ExpenseParticipantEntity(expenseId = "e1", userId = "u2", shareMinor = 300000L)
    )

    val analytics = ExpenseAnalyticsCalculator.computeAnalytics(
      trip = trip,
      expenses = listOf(exp),
      payers = payers,
      participants = participants,
      members = members,
      profiles = profiles
    )

    // Average per member: 600000 / 2 = 300000 paise
    assertEquals(300000L, analytics.averagePerMemberMinor)

    // Paid breakdown:
    // Alice paid 400000 (66.7%), Bob paid 200000 (33.3%)
    val alicePaid = analytics.membersPaid.find { it.userId == "u1" }!!
    assertEquals(400000L, alicePaid.totalAmountMinor)
    assertEquals(66.7, alicePaid.percentage, 0.1)

    val bobPaid = analytics.membersPaid.find { it.userId == "u2" }!!
    assertEquals(200000L, bobPaid.totalAmountMinor)
    assertEquals(33.3, bobPaid.percentage, 0.1)

    // Top spender is Alice
    assertEquals("u1", analytics.topSpender?.userId)

    // Share breakdown:
    // Both Alice and Bob consumed 50.0%
    val aliceShare = analytics.membersShare.find { it.userId == "u1" }!!
    assertEquals(300000L, aliceShare.totalAmountMinor)
    assertEquals(50.0, aliceShare.percentage, 0.1)

    val bobShare = analytics.membersShare.find { it.userId == "u2" }!!
    assertEquals(300000L, bobShare.totalAmountMinor)
    assertEquals(50.0, bobShare.percentage, 0.1)
  }

  @Test
  fun testExpenseAnalyticsEmptyExpenses() {
    val trip = TripEntity(id = "trip1", name = "Empty Trip", currencyCode = "INR", createdBy = "u1")
    val members = listOf(
      TripMemberEntity(tripId = "trip1", userId = "u1", role = "CREATOR")
    )
    val analytics = ExpenseAnalyticsCalculator.computeAnalytics(
      trip = trip,
      expenses = emptyList(),
      payers = emptyList(),
      participants = emptyList(),
      members = members,
      profiles = emptyMap()
    )

    assertEquals(0L, analytics.totalApprovedSpendMinor)
    assertEquals(0L, analytics.totalAllSpendMinor)
    assertEquals(0, analytics.approvedExpenseCount)
    assertTrue(analytics.categories.isEmpty())
    assertNull(analytics.topCategory)
    assertEquals(0L, analytics.averagePerMemberMinor)
  }
}

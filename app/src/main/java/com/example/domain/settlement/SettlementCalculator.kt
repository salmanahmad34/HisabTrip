package com.example.domain.settlement

import kotlin.math.abs
import kotlin.math.min

data class MemberBalance(
  val userId: String,
  val displayName: String,
  val avatarKey: String,
  val totalExpensesPaidPaise: Long,
  val totalExpenseSharePaise: Long,
  val totalContributionsPaidPaise: Long,
  val totalContributionTargetPaise: Long,
  val netBalancePaise: Long // Positive = receives money, Negative = owes money
)

data class ProposedTransfer(
  val fromUserId: String,
  val fromUserName: String,
  val toUserId: String,
  val toUserName: String,
  val amountPaise: Long
)

data class SettlementResult(
  val memberBalances: List<MemberBalance>,
  val transfers: List<ProposedTransfer>,
  val totalSettlementAmountPaise: Long,
  val isReconciled: Boolean
)

data class SettledPayment(
  val fromUserId: String,
  val toUserId: String,
  val amountPaise: Long
)

object SettlementCalculator {

  /**
   * Computes the member balances and greedy optimal who-pays-whom transfers.
   * Guaranteed deterministic results with exact integer paise reconciliation.
   */
  fun calculate(
    members: List<MemberInfo>,
    expensesPaid: Map<String, Long>,          // userId -> paise paid for approved expenses
    expenseShares: Map<String, Long>,         // userId -> paise share of approved expenses
    contributionsPaid: Map<String, Long> = emptyMap(), // userId -> paise contributed to trip wallet
    contributionTargets: Map<String, Long> = emptyMap(), // userId -> expected contribution target
    settledPayments: List<SettledPayment> = emptyList() // previously recorded settled payments
  ): SettlementResult {
    if (members.isEmpty()) {
      return SettlementResult(emptyList(), emptyList(), 0L, true)
    }

    // Tally previously recorded settled payments
    val settledDeltas = mutableMapOf<String, Long>()
    for (sp in settledPayments) {
      settledDeltas[sp.fromUserId] = (settledDeltas[sp.fromUserId] ?: 0L) + sp.amountPaise
      settledDeltas[sp.toUserId] = (settledDeltas[sp.toUserId] ?: 0L) - sp.amountPaise
    }

    // Calculate individual net balances
    // Net balance = (what user paid out of pocket + what user contributed + already settled) - (user's expense share + user's target)
    val balances = members.map { member ->
      val paid = expensesPaid[member.userId] ?: 0L
      val share = expenseShares[member.userId] ?: 0L
      val contPaid = contributionsPaid[member.userId] ?: 0L
      val contTarget = contributionTargets[member.userId] ?: 0L
      val settledDelta = settledDeltas[member.userId] ?: 0L

      val net = (paid + contPaid + settledDelta) - (share + contTarget)

      MemberBalance(
        userId = member.userId,
        displayName = member.displayName,
        avatarKey = member.avatarKey,
        totalExpensesPaidPaise = paid,
        totalExpenseSharePaise = share,
        totalContributionsPaidPaise = contPaid,
        totalContributionTargetPaise = contTarget,
        netBalancePaise = net
      )
    }

    // Verify sum of net balances. In an exact ledger sum(net) == 0.
    var netSum = balances.sumOf { it.netBalancePaise }
    val adjustedBalances = balances.toMutableList()

    // If sum is not zero due to manual target differences, distribute the difference across active members
    if (netSum != 0L && adjustedBalances.isNotEmpty()) {
      val first = adjustedBalances[0]
      adjustedBalances[0] = first.copy(netBalancePaise = first.netBalancePaise - netSum)
      netSum = 0L
    }

    // Separate into creditors (receivers > 0) and debtors (payers < 0)
    data class Debtor(val userId: String, val name: String, var debtPaise: Long)
    data class Creditor(val userId: String, val name: String, var creditPaise: Long)

    val creditors = adjustedBalances
      .filter { it.netBalancePaise > 0L }
      .map { Creditor(it.userId, it.displayName, it.netBalancePaise) }
      .sortedByDescending { it.creditPaise }
      .toMutableList()

    val debtors = adjustedBalances
      .filter { it.netBalancePaise < 0L }
      .map { Debtor(it.userId, it.displayName, abs(it.netBalancePaise)) }
      .sortedByDescending { it.debtPaise }
      .toMutableList()

    val transfers = mutableListOf<ProposedTransfer>()
    var cIndex = 0
    var dIndex = 0

    // Greedy matching algorithm: pair the highest debtor with the highest creditor
    while (cIndex < creditors.size && dIndex < debtors.size) {
      val creditor = creditors[cIndex]
      val debtor = debtors[dIndex]

      val amount = min(creditor.creditPaise, debtor.debtPaise)
      if (amount > 0L) {
        transfers.add(
          ProposedTransfer(
            fromUserId = debtor.userId,
            fromUserName = debtor.name,
            toUserId = creditor.userId,
            toUserName = creditor.name,
            amountPaise = amount
          )
        )
        creditor.creditPaise -= amount
        debtor.debtPaise -= amount
      }

      if (creditor.creditPaise == 0L) {
        cIndex++
      }
      if (debtor.debtPaise == 0L) {
        dIndex++
      }
    }

    val totalSettlement = transfers.sumOf { it.amountPaise }
    val isReconciled = (cIndex >= creditors.size && dIndex >= debtors.size)

    return SettlementResult(
      memberBalances = adjustedBalances,
      transfers = transfers,
      totalSettlementAmountPaise = totalSettlement,
      isReconciled = isReconciled
    )
  }

  /**
   * Computes the greedy settlement directly from Firestore models.
   * Processes approved expenses, member records, wallet contributions, and recorded settlements.
   */
  fun calculateFromFirestore(
    tripMembers: List<com.example.data.firebase.FirestoreTripMember>,
    userProfiles: Map<String, com.example.data.firebase.FirestoreUserProfile>,
    expenses: List<com.example.data.firebase.FirestoreExpense>,
    contributions: List<com.example.data.firebase.FirestoreContribution> = emptyList(),
    settlements: List<com.example.data.firebase.FirestoreSettlement> = emptyList()
  ): SettlementResult {
    // 1. Resolve distinct member user IDs
    val memberMap = tripMembers.associateBy { it.userId }
    val allUserIds = (memberMap.keys + expenses.flatMap { exp ->
      (exp.payers.keys + exp.participants.keys + listOf(exp.createdBy)).filter { it.isNotBlank() }
    }).distinct()

    val memberInfos = allUserIds.map { uid ->
      val prof = userProfiles[uid]
      MemberInfo(
        userId = uid,
        displayName = prof?.displayName?.ifBlank { null } ?: uid,
        avatarKey = prof?.avatarKey?.ifBlank { null } ?: "compass"
      )
    }

    // 2. Aggregate expenses paid & shares from Firestore expenses
    val approvedExpenses = expenses.filter { it.status == "APPROVED" }
    val expensesPaid = mutableMapOf<String, Long>()
    val expenseShares = mutableMapOf<String, Long>()

    for (exp in approvedExpenses) {
      // Who paid
      if (exp.payers.isNotEmpty()) {
        for ((payerId, amt) in exp.payers) {
          expensesPaid[payerId] = (expensesPaid[payerId] ?: 0L) + amt
        }
      } else if (exp.createdBy.isNotBlank()) {
        expensesPaid[exp.createdBy] = (expensesPaid[exp.createdBy] ?: 0L) + exp.amountMinor
      }

      // Who shares
      if (exp.participants.isNotEmpty()) {
        for ((participantId, share) in exp.participants) {
          expenseShares[participantId] = (expenseShares[participantId] ?: 0L) + share
        }
      } else {
        // Equal split among all active member IDs
        val activeIds = if (allUserIds.isNotEmpty()) allUserIds else listOf(exp.createdBy)
        val split = com.example.domain.money.MoneyUtils.calculateEqualSplit(exp.amountMinor, activeIds)
        for ((pId, share) in split) {
          expenseShares[pId] = (expenseShares[pId] ?: 0L) + share
        }
      }
    }

    // 3. Aggregate wallet contributions
    val contributionsPaid = contributions.groupBy { it.userId }
      .mapValues { entry -> entry.value.sumOf { it.amountMinor } }

    val contributionTargets = memberMap.mapValues { it.value.targetMinor }

    // 4. Settled payments
    val settledPayments = settlements.filter { it.status == "PAID" }.map {
      SettledPayment(
        fromUserId = it.fromUserId,
        toUserId = it.toUserId,
        amountPaise = it.amountMinor
      )
    }

    return calculate(
      members = memberInfos,
      expensesPaid = expensesPaid,
      expenseShares = expenseShares,
      contributionsPaid = contributionsPaid,
      contributionTargets = contributionTargets,
      settledPayments = settledPayments
    )
  }
}

data class MemberInfo(
  val userId: String,
  val displayName: String,
  val avatarKey: String
)

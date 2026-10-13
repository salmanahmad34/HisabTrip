package com.example.domain.money

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

object MoneyUtils {
  private val formatter: DecimalFormat = run {
    val symbols = DecimalFormatSymbols(Locale("en", "IN")).apply {
      currencySymbol = "₹"
    }
    DecimalFormat("₹#,##,##0.00", symbols)
  }

  private val compactFormatter: DecimalFormat = run {
    val symbols = DecimalFormatSymbols(Locale("en", "IN")).apply {
      currencySymbol = "₹"
    }
    DecimalFormat("₹#,##,##0", symbols)
  }

  /**
   * Formats paise (minor unit) into standard Indian Currency representation.
   * e.g., 50050L -> "₹500.50"
   */
  fun formatPaise(paise: Long, showDecimals: Boolean = true): String {
    val sign = if (paise < 0) "-" else ""
    val absPaise = kotlin.math.abs(paise)
    val rupees = absPaise.toDouble() / 100.0
    val formatted = if (showDecimals || absPaise % 100 != 0L) {
      formatter.format(rupees)
    } else {
      compactFormatter.format(rupees)
    }
    return if (paise < 0) "-$formatted" else formatted
  }

  fun getCurrencySymbol(currencyCode: String): String {
    return when (currencyCode.trim().uppercase()) {
      "INR" -> "₹"
      "USD" -> "$"
      "EUR" -> "€"
      "GBP" -> "£"
      "AED" -> "AED "
      "JPY" -> "¥"
      "THB" -> "฿"
      "SGD" -> "S$"
      "CAD" -> "CA$"
      "AUD" -> "A$"
      else -> currencyCode.trim().uppercase() + " "
    }
  }

  fun formatCurrency(amountMinor: Long, currencyCode: String = "INR", showDecimals: Boolean = true): String {
    val code = currencyCode.trim().uppercase().ifBlank { "INR" }
    if (code == "INR") {
      return formatPaise(amountMinor, showDecimals)
    }
    val symbol = getCurrencySymbol(code)
    val sign = if (amountMinor < 0) "-" else ""
    val absAmount = kotlin.math.abs(amountMinor)
    val major = absAmount.toDouble() / 100.0
    val numStr = if (showDecimals || absAmount % 100 != 0L) {
      String.format(Locale.US, "%.2f", major)
    } else {
      String.format(Locale.US, "%.0f", major)
    }
    return "$sign$symbol$numStr"
  }

  /**
   * Parses user input string (e.g. "500", "500.50", "₹1200") into exact paise (Long).
   * Returns null if string is invalid or negative.
   */
  fun parseToPaise(input: String): Long? {
    val cleaned = input.replace("₹", "").replace(",", "").trim()
    if (cleaned.isEmpty()) return null
    return try {
      val parts = cleaned.split(".")
      if (parts.size > 2) return null
      val whole = parts[0].toLongOrNull() ?: return null
      if (whole < 0) return null

      val fraction = if (parts.size == 2) {
        val dec = parts[1]
        when {
          dec.isEmpty() -> 0L
          dec.length == 1 -> (dec + "0").toLong()
          dec.length == 2 -> dec.toLong()
          else -> dec.substring(0, 2).toLong() // truncate to 2 decimal places
        }
      } else {
        0L
      }
      (whole * 100L) + fraction
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Calculates deterministic equal split among participants.
   * Total sum of returned shares is guaranteed to equal totalAmountPaise exactly.
   * Remainder paise are distributed 1 paisa each to the first participants in the list.
   */
  fun calculateEqualSplit(totalAmountPaise: Long, participantIds: List<String>): Map<String, Long> {
    if (participantIds.isEmpty() || totalAmountPaise <= 0L) return emptyMap()
    val count = participantIds.size.toLong()
    val baseShare = totalAmountPaise / count
    val remainder = (totalAmountPaise % count).toInt()

    val result = mutableMapOf<String, Long>()
    for (i in participantIds.indices) {
      val extra = if (i < remainder) 1L else 0L
      result[participantIds[i]] = baseShare + extra
    }
    return result
  }

  /**
   * Calculates percentage split with deterministic remainder reconciliation.
   * Ensures sum of all shares equals totalAmountPaise exactly.
   */
  fun calculatePercentageSplit(
    totalAmountPaise: Long,
    percentages: Map<String, Double>
  ): Map<String, Long> {
    if (percentages.isEmpty() || totalAmountPaise <= 0L) return emptyMap()
    val result = mutableMapOf<String, Long>()
    var sumCalculated = 0L

    val sortedEntries = percentages.entries.sortedByDescending { it.value }
    for (entry in sortedEntries) {
      val share = ((totalAmountPaise * entry.value) / 100.0).roundToLong()
      result[entry.key] = share
      sumCalculated += share
    }

    val difference = totalAmountPaise - sumCalculated
    if (difference != 0L && sortedEntries.isNotEmpty()) {
      // Adjust difference on the first participant
      val firstKey = sortedEntries.first().key
      result[firstKey] = (result[firstKey] ?: 0L) + difference
    }

    return result
  }
}

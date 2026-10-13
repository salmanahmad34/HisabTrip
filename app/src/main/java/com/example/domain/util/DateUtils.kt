package com.example.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

  private val parserFormats = listOf(
    SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
    SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH),
    SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH),
    SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
    SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
    SimpleDateFormat("d/M/yyyy", Locale.ENGLISH),
    SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH),
    SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH),
    SimpleDateFormat("d MMM", Locale.ENGLISH),
    SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
  )

  private val fmtFull = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
  private val fmtMonthDay = SimpleDateFormat("MMM d", Locale.ENGLISH)
  private val fmtDayOnly = SimpleDateFormat("d", Locale.ENGLISH)
  private val fmtMonthYear = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

  fun parseDate(raw: String?): Date? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    for (formatter in parserFormats) {
      try {
        formatter.isLenient = true
        val date = formatter.parse(trimmed)
        if (date != null) return date
      } catch (_: Exception) {
        // Try next format
      }
    }
    return null
  }

  /**
   * Formats start and end dates with smart coalescing:
   * Same Month & Year: "Oct 15 – 22, 2026"
   * Same Year, Different Month: "Oct 28 – Nov 4, 2026"
   * Different Year: "Dec 28, 2026 – Jan 5, 2027"
   * Same Day: "Oct 15, 2026"
   * Single Date: "Oct 15, 2026"
   */
  fun formatDateRange(startDateStr: String, endDateStr: String): String {
    val startClean = startDateStr.trim()
    val endClean = endDateStr.trim()

    if (startClean.isEmpty() && endClean.isEmpty()) return "Dates to be decided"

    val start = parseDate(startClean)
    val end = parseDate(endClean)

    if (start == null && end == null) {
      return if (endClean.isEmpty() || startClean == endClean) startClean else "$startClean – $endClean"
    }

    if (start != null && (end == null || endClean.isEmpty())) {
      return fmtFull.format(start)
    }

    if (start == null && end != null) {
      return "Until " + fmtFull.format(end)
    }

    if (start != null && end != null) {
      val calStart = Calendar.getInstance().apply { time = start }
      val calEnd = Calendar.getInstance().apply { time = end }

      val startYear = calStart.get(Calendar.YEAR)
      val endYear = calEnd.get(Calendar.YEAR)
      val startMonth = calStart.get(Calendar.MONTH)
      val endMonth = calEnd.get(Calendar.MONTH)
      val startDay = calStart.get(Calendar.DAY_OF_MONTH)
      val endDay = calEnd.get(Calendar.DAY_OF_MONTH)

      return when {
        startYear == endYear && startMonth == endMonth && startDay == endDay -> {
          fmtFull.format(start)
        }
        startYear == endYear && startMonth == endMonth -> {
          val monthName = SimpleDateFormat("MMM", Locale.ENGLISH).format(start)
          "$monthName $startDay – $endDay, $startYear"
        }
        startYear == endYear -> {
          val startPart = fmtMonthDay.format(start)
          val endPart = fmtMonthDay.format(end)
          "$startPart – $endPart, $startYear"
        }
        else -> {
          "${fmtFull.format(start)} – ${fmtFull.format(end)}"
        }
      }
    }

    return startClean
  }

  /**
   * Calculates the trip duration in days
   */
  fun calculateDurationDays(startDateStr: String, endDateStr: String): Int? {
    val start = parseDate(startDateStr) ?: return null
    val end = parseDate(endDateStr) ?: return null
    val diffMillis = end.time - start.time
    if (diffMillis < 0) return 1
    val days = TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS) + 1
    return days.toInt()
  }

  /**
   * Returns human-friendly duration badge text (e.g., "7 Days", "Weekend", "Day Trip")
   */
  fun formatDurationBadge(startDateStr: String, endDateStr: String): String? {
    val days = calculateDurationDays(startDateStr, endDateStr) ?: return null
    return when {
      days <= 1 -> "1 Day Trip"
      days in 2..3 -> "$days Days • Weekend"
      else -> "$days Days"
    }
  }

  /**
   * Checks whether the trip is Ongoing, Upcoming, or Concluded based on dates
   */
  fun getTripTimelineStatus(startDateStr: String, endDateStr: String): TimelineStatus {
    val now = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.time

    val start = parseDate(startDateStr)
    val end = parseDate(endDateStr) ?: start

    if (start == null && end == null) return TimelineStatus.PLANNED

    return when {
      end != null && end.before(now) -> TimelineStatus.PAST
      start != null && start.after(now) -> TimelineStatus.UPCOMING
      else -> TimelineStatus.HAPPENING_NOW
    }
  }
}

enum class TimelineStatus {
  HAPPENING_NOW,
  UPCOMING,
  PAST,
  PLANNED
}

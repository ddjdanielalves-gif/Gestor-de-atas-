package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DeadlineUrgency {
    OVERDUE,     // Atrasada / Vencida
    TODAY,       // Vence Hoje
    SOON,        // Vence em breve (próximos 3-7 dias)
    NORMAL,      // No prazo (> 7 dias)
    COMPLETED    // Concluída
}

object DateUtils {

    private val ptBrLocale = Locale("pt", "BR")
    private val standardDateFormat = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
    private val readableDateFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", ptBrLocale)
    private val shortMonthFormat = SimpleDateFormat("dd 'de' MMM", ptBrLocale)

    fun formatMillisToDate(millis: Long): String {
        return standardDateFormat.format(Date(millis))
    }

    fun formatMillisToReadable(millis: Long): String {
        return readableDateFormat.format(Date(millis))
    }

    fun formatMillisToShort(millis: Long): String {
        return shortMonthFormat.format(Date(millis))
    }

    fun getStartOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun addDaysToCurrentTime(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    fun calculateDaysRemaining(dueMillis: Long, currentMillis: Long = System.currentTimeMillis()): Long {
        val startOfToday = getStartOfDay(currentMillis)
        val startOfDue = getStartOfDay(dueMillis)
        val diff = startOfDue - startOfToday
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun getUrgency(dueMillis: Long, isCompleted: Boolean): DeadlineUrgency {
        if (isCompleted) return DeadlineUrgency.COMPLETED
        val days = calculateDaysRemaining(dueMillis)
        return when {
            days < 0 -> DeadlineUrgency.OVERDUE
            days == 0L -> DeadlineUrgency.TODAY
            days in 1..7 -> DeadlineUrgency.SOON
            else -> DeadlineUrgency.NORMAL
        }
    }

    fun getDeadlineBadgeText(dueMillis: Long, isCompleted: Boolean): String {
        if (isCompleted) return "Concluída"
        val days = calculateDaysRemaining(dueMillis)
        return when {
            days < 0 -> "Atrasada há ${-days} ${if (days == -1L) "dia" else "dias"}"
            days == 0L -> "Vence Hoje!"
            days == 1L -> "Vence Amanhã"
            days in 2..7 -> "Vence em $days dias"
            else -> "Prazo: ${formatMillisToDate(dueMillis)} ($days dias)"
        }
    }

    fun parseDateTextToMillis(dateStr: String): Long? {
        val clean = dateStr.trim()
        try {
            // Try dd/MM/yyyy
            if (clean.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}"))) {
                return standardDateFormat.parse(clean)?.time
            }
        } catch (_: Exception) {}

        try {
            // Try Portuguese full date: "26 de Agosto de 2026"
            val regex = Regex("(\\d{1,2})\\s+de\\s+([a-zA-ZçÇ]+)\\s+de\\s+(\\d{4})", RegexOption.IGNORE_CASE)
            val match = regex.find(clean)
            if (match != null) {
                val (day, monthStr, year) = match.destructured
                val month = when (monthStr.lowercase(ptBrLocale)) {
                    "janeiro" -> Calendar.JANUARY
                    "fevereiro" -> Calendar.FEBRUARY
                    "março", "marco" -> Calendar.MARCH
                    "abril" -> Calendar.APRIL
                    "maio" -> Calendar.MAY
                    "junho" -> Calendar.JUNE
                    "julho" -> Calendar.JULY
                    "agosto" -> Calendar.AUGUST
                    "setembro" -> Calendar.SEPTEMBER
                    "outubro" -> Calendar.OCTOBER
                    "novembro" -> Calendar.NOVEMBER
                    "dezembro" -> Calendar.DECEMBER
                    else -> Calendar.JANUARY
                }
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year.toInt())
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day.toInt())
                cal.set(Calendar.HOUR_OF_DAY, 12)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                return cal.timeInMillis
            }
        } catch (_: Exception) {}

        return null
    }
}

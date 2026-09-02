package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DeadlineUrgency {
    OVERDUE,
    TODAY,
    SOON,
    NORMAL,
    COMPLETED
}

object DateUtils {

    private val ptBrLocale =
        Locale("pt", "BR")

    private val standardDateFormat =
        SimpleDateFormat(
            "dd/MM/yyyy",
            ptBrLocale
        ).apply {
            isLenient = false
        }

    private val isoDateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }

    private val readableDateFormat =
        SimpleDateFormat(
            "d 'de' MMMM 'de' yyyy",
            ptBrLocale
        )

    private val shortMonthFormat =
        SimpleDateFormat(
            "dd 'de' MMM",
            ptBrLocale
        )

    fun formatMillisToDate(
        millis: Long
    ): String =
        standardDateFormat.format(
            Date(millis)
        )

    fun formatMillisToReadable(
        millis: Long
    ): String =
        readableDateFormat.format(
            Date(millis)
        )

    fun formatMillisToShort(
        millis: Long
    ): String =
        shortMonthFormat.format(
            Date(millis)
        )

    fun getStartOfDay(
        timeMillis: Long =
            System.currentTimeMillis()
    ): Long {

        val cal =
            Calendar.getInstance().apply {
                timeInMillis =
                    timeMillis
            }

        cal.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        cal.set(
            Calendar.MINUTE,
            0
        )

        cal.set(
            Calendar.SECOND,
            0
        )

        cal.set(
            Calendar.MILLISECOND,
            0
        )

        return cal.timeInMillis
    }

    fun getEndOfDay(
        timeMillis: Long =
            System.currentTimeMillis()
    ): Long {

        val cal =
            Calendar.getInstance().apply {
                timeInMillis =
                    timeMillis
            }

        cal.set(
            Calendar.HOUR_OF_DAY,
            23
        )

        cal.set(
            Calendar.MINUTE,
            59
        )

        cal.set(
            Calendar.SECOND,
            59
        )

        cal.set(
            Calendar.MILLISECOND,
            999
        )

        return cal.timeInMillis
    }

    /**
     * Adiciona dias a uma data específica.
     *
     * NÃO usa a data atual.
     */
    fun addDaysToDate(
        baseMillis: Long,
        days: Int
    ): Long {

        val cal =
            Calendar.getInstance().apply {
                timeInMillis =
                    baseMillis
            }

        cal.add(
            Calendar.DAY_OF_YEAR,
            days
        )

        return getStartOfDay(
            cal.timeInMillis
        )
    }

    /**
     * Mantido para lembretes criados manualmente.
     */
    fun addDaysToCurrentTime(
        days: Int
    ): Long =
        addDaysToDate(
            System.currentTimeMillis(),
            days
        )

    fun calculateDaysRemaining(
        dueMillis: Long,
        currentMillis: Long =
            System.currentTimeMillis()
    ): Long {

        val diff =
            getStartOfDay(dueMillis) -
                getStartOfDay(currentMillis)

        return TimeUnit.MILLISECONDS
            .toDays(diff)
    }

    fun getUrgency(
        dueMillis: Long,
        isCompleted: Boolean
    ): DeadlineUrgency {

        if (isCompleted) {
            return DeadlineUrgency.COMPLETED
        }

        return when (
            val days =
                calculateDaysRemaining(
                    dueMillis
                )
        ) {

            in Long.MIN_VALUE..-1L ->
                DeadlineUrgency.OVERDUE

            0L ->
                DeadlineUrgency.TODAY

            in 1L..7L ->
                DeadlineUrgency.SOON

            else ->
                DeadlineUrgency.NORMAL
        }
    }

    fun getDeadlineBadgeText(
        dueMillis: Long,
        isCompleted: Boolean
    ): String {

        if (isCompleted) {
            return "Concluída"
        }

        val days =
            calculateDaysRemaining(
                dueMillis
            )

        return when {

            days < 0 ->
                "Atrasada há ${-days} ${
                    if (days == -1L)
                        "dia"
                    else
                        "dias"
                }"

            days == 0L ->
                "Vence Hoje!"

            days == 1L ->
                "Vence Amanhã"

            days in 2..7 ->
                "Vence em $days dias"

            else ->
                "Prazo: ${
                    formatMillisToDate(
                        dueMillis
                    )
                } ($days dias)"
        }
    }

    fun parseDateTextToMillis(
        dateStr: String
    ): Long? {

        val original =
            dateStr.trim()

        if (original.isBlank()) {
            return null
        }

        val clean =
            Regex(
                "\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b"
            )
                .find(original)
                ?.value
                ?: Regex(
                    "\\b\\d{4}-\\d{2}-\\d{2}\\b"
                )
                    .find(original)
                    ?.value
                ?: original

        listOf(
            standardDateFormat,
            isoDateFormat
        ).forEach { formatter ->

            runCatching {
                formatter
                    .parse(clean)
                    ?.time
            }
                .getOrNull()
                ?.let {
                    return getStartOfDay(it)
                }
        }

        val match =
            Regex(
                "(\\d{1,2})\\s+de\\s+" +
                    "([a-zA-ZçÇãõáéíóúâêô]+)" +
                    "\\s+de\\s+(\\d{4})",
                RegexOption.IGNORE_CASE
            )
                .find(clean)
                ?: return null

        val month =
            when (
                match.groupValues[2]
                    .lowercase(ptBrLocale)
            ) {

                "janeiro" ->
                    Calendar.JANUARY

                "fevereiro" ->
                    Calendar.FEBRUARY

                "março",
                "marco" ->
                    Calendar.MARCH

                "abril" ->
                    Calendar.APRIL

                "maio" ->
                    Calendar.MAY

                "junho" ->
                    Calendar.JUNE

                "julho" ->
                    Calendar.JULY

                "agosto" ->
                    Calendar.AUGUST

                "setembro" ->
                    Calendar.SEPTEMBER

                "outubro" ->
                    Calendar.OCTOBER

                "novembro" ->
                    Calendar.NOVEMBER

                "dezembro" ->
                    Calendar.DECEMBER

                else ->
                    return null
            }

        val cal =
            Calendar.getInstance().apply {

                clear()

                set(
                    Calendar.YEAR,
                    match.groupValues[3]
                        .toInt()
                )

                set(
                    Calendar.MONTH,
                    month
                )

                set(
                    Calendar.DAY_OF_MONTH,
                    match.groupValues[1]
                        .toInt()
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    12
                )
            }

        return cal.timeInMillis
    }
}

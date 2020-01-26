package work.ckogyo.returnvisitor.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

fun Calendar.toDateText(context: Context): String {

    val format = if (context.resources.configuration.locale == Locale.JAPAN) {
        SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.getDefault())
    } else {
        android.text.format.DateFormat.getMediumDateFormat(context)
    }
    return format.format(this.time)
}

fun Calendar.toTimeText(context: Context, withSecond: Boolean = false): String {

    var layout = "HH:mm"
    if (withSecond) layout += ":ss"

    return SimpleDateFormat(layout, Locale.getDefault()).format(this.time)
}

fun Calendar.toDateTimeText(context: Context, withSecond: Boolean = false): String {
    return "${this.toDateText(context)} ${this.toTimeText(context, withSecond)}"
}

fun Calendar.cloneWith0Time():Calendar {

    val date2 = Calendar.getInstance()
    date2.timeInMillis = 0
    date2.set(Calendar.YEAR, this.get(Calendar.YEAR))
    date2.set(Calendar.MONTH, this.get(Calendar.MONTH))
    date2.set(Calendar.DAY_OF_MONTH, this.get(Calendar.DAY_OF_MONTH))
    return date2
}

fun Calendar.getDaysDiff(other: Calendar): Int {
    val oneDay = 1000 * 60 * 60 * 24
    val day1 = (this.timeInMillis / oneDay).toInt()
    val day2 = (other.timeInMillis / oneDay).toInt()
    return day2 - day1
}

fun Calendar.isSameDate(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR)
            && get(Calendar.MONTH) == other.get(Calendar.MONTH)
            && get(Calendar.DAY_OF_MONTH) == other.get(Calendar.DAY_OF_MONTH)
}

fun Calendar.isDateBefore(other: Calendar, allowSame: Boolean = false): Boolean {

    if (allowSame && isSameDate(other)) {
        return true
    }

    return when {
        get(Calendar.YEAR) < other.get(Calendar.YEAR) -> true
        get(Calendar.YEAR) > other.get(Calendar.YEAR) -> false
        else -> {
            when {
                get(Calendar.MONTH) < other.get(Calendar.MONTH) -> true
                get(Calendar.MONTH) > other.get(Calendar.MONTH) -> false
                else -> {
                    when {
                        get(Calendar.DAY_OF_MONTH) < other.get(Calendar.DAY_OF_MONTH) -> true
                        get(Calendar.DAY_OF_MONTH) > other.get(Calendar.DAY_OF_MONTH) -> false
                        else -> false
                    }
                }
            }
        }
    }
}

fun Calendar.isDateAfter(other: Calendar, allowSame: Boolean = false): Boolean {

    if (allowSame && isSameDate(other)) {
        return true
    }

    return when {
        get(Calendar.YEAR) < other.get(Calendar.YEAR) -> false
        get(Calendar.YEAR) > other.get(Calendar.YEAR) -> true
        else -> {
            when {
                get(Calendar.MONTH) < other.get(Calendar.MONTH) -> false
                get(Calendar.MONTH) > other.get(Calendar.MONTH) -> true
                else -> {
                    when {
                        get(Calendar.DAY_OF_MONTH) < other.get(Calendar.DAY_OF_MONTH) -> false
                        get(Calendar.DAY_OF_MONTH) > other.get(Calendar.DAY_OF_MONTH) -> true
                        else -> false
                    }
                }
            }
        }
    }
}

fun Calendar.isSameTime(other: Calendar): Boolean {
    return get(Calendar.HOUR_OF_DAY) == other.get(Calendar.HOUR_OF_DAY)
            && get(Calendar.MINUTE) == other.get(Calendar.MINUTE)
}

fun Calendar.isTimeBefore(other: Calendar, allowSame: Boolean): Boolean {
    if (allowSame && isSameTime(other)) {
        return true
    }

    return when {
        get(Calendar.HOUR_OF_DAY) < other.get(Calendar.HOUR_OF_DAY) -> true
        get(Calendar.HOUR_OF_DAY) > other.get(Calendar.HOUR_OF_DAY) -> false
        else -> {
            when {
                get(Calendar.MINUTE) < other.get(Calendar.MINUTE) -> true
                get(Calendar.MINUTE) > other.get(Calendar.MINUTE) -> false
                else -> false
            }
        }
    }
}

fun Calendar.isTimeAfter(other: Calendar, allowSame: Boolean): Boolean {
    if (allowSame && isSameTime(other)) {
        return true
    }

    return when {
        get(Calendar.HOUR_OF_DAY) > other.get(Calendar.HOUR_OF_DAY) -> true
        get(Calendar.HOUR_OF_DAY) < other.get(Calendar.HOUR_OF_DAY) -> false
        else -> {
            when {
                get(Calendar.MINUTE) > other.get(Calendar.MINUTE) -> true
                get(Calendar.MINUTE) < other.get(Calendar.MINUTE) -> false
                else -> false
            }
        }
    }
}

fun Calendar.isSameDateTime(other: Calendar): Boolean {
    return isSameDate(other) && isSameTime(other)
}

fun Calendar.isDateTimeBefore(other: Calendar, allowSame: Boolean): Boolean {
    return isDateBefore(other, allowSame) && isTimeBefore(other, allowSame)
}

fun Calendar.isDateTimeAfter(other: Calendar, allowSame: Boolean): Boolean {
    return  isDateAfter(other, allowSame) && isTimeAfter(other, allowSame)
}


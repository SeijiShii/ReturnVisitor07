package work.ckogyo.returnvisitor.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*


fun getDurationString(duration: Long, showSec: Boolean): String {

    var duration2 = duration

    val secMil = 1000
    val minMil = secMil * 60
    val hourMil = minMil * 60

    val hour = duration2.toInt() / hourMil
    duration2 -= hour * hourMil

    val min = duration2.toInt() / minMil
    duration2 -= min * minMil

    val sec = duration2.toInt() / secMil

    return if (showSec) {
        hour.toString() + ":" + String.format("%02d", min) + ":" + String.format("%02d", sec)
    } else {
        hour.toString() + ":" + String.format("%02d", min)
    }
}

fun getTimeText(calendar: Calendar, showSec: Boolean): String {

    var format = SimpleDateFormat("kk:mm", Locale.getDefault())
    if (showSec) format = SimpleDateFormat("kk:mm:ss", Locale.getDefault())
    return format.format(calendar.time)

}

fun getMonthText(month: Calendar, context: Context): String {
    return android.text.format.DateFormat.format("MM", month).toString()
}

fun getDateTimeText(calendar: Calendar, context: Context): String {
    val format = android.text.format.DateFormat.getMediumDateFormat(context)
    val dateString = format.format(calendar.time)

    return dateString + " " + getTimeText(calendar, false)
}

//fun getDaysAgoText(days: Int, context: Context): String {
//    return if (days <= 0) {
//        context.getString(R.string.today)
//    } else if (days == 1) {
//        context.getString(R.string.yesterday)
//    } else {
//        context.getString(R.string.days_ago, days)
//    }
//}

fun cloneDateWith0Time(date: Calendar):Calendar {

    val date2 = Calendar.getInstance()
    date2.timeInMillis = 0
    date2.set(Calendar.YEAR, date.get(Calendar.YEAR))
    date2.set(Calendar.MONTH, date.get(Calendar.MONTH))
    date2.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
    return date2
}

fun areSameDates(date1: Calendar, date2: Calendar): Boolean {
    return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
            && date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH)
            && date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)
}

fun isDateBefore(date1: Calendar, date2: Calendar, allowSame: Boolean = false): Boolean {

    if (allowSame && areSameDates(date1, date2)) {
        return true
    }

    return when {
        date1.get(Calendar.YEAR) < date2.get(Calendar.YEAR) -> true
        date1.get(Calendar.YEAR) > date2.get(Calendar.YEAR) -> false
        else -> {
            when {
                date1.get(Calendar.MONTH) < date2.get(Calendar.MONTH) -> true
                date1.get(Calendar.MONTH) > date2.get(Calendar.MONTH) -> false
                else -> {
                    when {
                        date1.get(Calendar.DAY_OF_MONTH) < date2.get(Calendar.DAY_OF_MONTH) -> true
                        date1.get(Calendar.DAY_OF_MONTH) > date2.get(Calendar.DAY_OF_MONTH) -> false
                        else -> false
                    }
                }
            }
        }
    }
}

fun isDateAfter(date1: Calendar, date2: Calendar, allowSame: Boolean = false): Boolean {

    if (allowSame && areSameDates(date1, date2)) {
        return true
    }

    return when {
        date1.get(Calendar.YEAR) < date2.get(Calendar.YEAR) -> false
        date1.get(Calendar.YEAR) > date2.get(Calendar.YEAR) -> true
        else -> {
            when {
                date1.get(Calendar.MONTH) < date2.get(Calendar.MONTH) -> false
                date1.get(Calendar.MONTH) > date2.get(Calendar.MONTH) -> true
                else -> {
                    when {
                        date1.get(Calendar.DAY_OF_MONTH) < date2.get(Calendar.DAY_OF_MONTH) -> false
                        date1.get(Calendar.DAY_OF_MONTH) > date2.get(Calendar.DAY_OF_MONTH) -> true
                        else -> false
                    }
                }
            }
        }
    }
}
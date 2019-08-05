package work.ckogyo.returnvisitor.utils

import android.content.Context
import work.ckogyo.returnvisitor.R
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
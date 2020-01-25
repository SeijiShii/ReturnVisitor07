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

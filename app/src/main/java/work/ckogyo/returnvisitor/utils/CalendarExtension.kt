package work.ckogyo.returnvisitor.utils

import java.text.SimpleDateFormat
import java.util.*

fun Calendar.toDateText(): String {

    val format = SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.getDefault())
    return format.format(this.time)
}
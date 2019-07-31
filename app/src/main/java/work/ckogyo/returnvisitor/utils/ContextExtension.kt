package work.ckogyo.returnvisitor.utils

import android.content.Context

fun Context.toDP(pixel: Int): Int {
    return (resources.displayMetrics.density * pixel).toInt()
}
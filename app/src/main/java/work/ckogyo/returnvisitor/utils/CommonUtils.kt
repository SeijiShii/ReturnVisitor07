package work.ckogyo.returnvisitor.utils

import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.record_visit_fragment.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import java.util.*
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.minutes


fun hideKeyboard(activity: Activity) {
    val view = activity.findViewById<View>(android.R.id.content)
    if (view != null) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun getTopInParent(view: View, parentClassName: String): Int {

    val parent = view.parent

    parent?: return 0

    var sum = view.top

    if (parent.javaClass.name != parentClassName) {
        sum += getTopInParent(parent as View, parentClassName)
    }
    return sum
}

val buttonResIds = arrayOf(
    R.drawable.gray_circle_button,
    R.drawable.red_circle_button,
    R.drawable.purple_circle_button,
    R.drawable.blue_circle_button,
    R.drawable.green_circle_button,
    R.drawable.gold_circle_button,
    R.drawable.orange_circle_button
)

fun ratingToColorButtonResId(rating: Visit.Rating): Int {
    return buttonResIds[rating.ordinal]
}

suspend fun requestAddressIfNeeded(place: Place, context: Context):String = suspendCoroutine { cont ->

    if(place.address.isNotEmpty()) {
        cont.resume(place.address)
    } else {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addressList = geocoder.getFromLocation(place.latLng.latitude, place.latLng.longitude, 1)
        if (addressList.isNotEmpty()) {
            place.address = addressList[0].getAddressLine(0)
        }

        cont.resume(place.address)
    }
}

fun Long.toDurationText(withSeconds: Boolean = false): String {

    val secUnit = 1000
    val minUnit = secUnit * 60
    val hourUnit = minUnit * 60

    val h = this / hourUnit
    val m = this % hourUnit / minUnit
    val s = this % minUnit / secUnit

    var txt = "$h:${String.format("%02d", m)}"
    if (withSeconds) txt += String.format(":%02d", s)
    return txt
}

fun Long.toMinute(): Long {
    return this / minInMillis
}

fun Long.toMinuteText(context: Context): String {

    val min = toMinute()
    return context.resources.getString(R.string.minute_placeholder, min)
}


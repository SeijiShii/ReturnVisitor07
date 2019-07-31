package work.ckogyo.returnvisitor.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit


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


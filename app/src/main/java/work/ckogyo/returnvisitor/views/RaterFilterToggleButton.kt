package work.ckogyo.returnvisitor.views

import android.content.Context
import androidx.appcompat.widget.AppCompatButton
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.circleSolidResIds

class RaterFilterToggleButton(context: Context, val rating: Visit.Rating): AppCompatButton(context) {

    var isChecked = false
        set(value) {
            field = value
            refreshBG()
        }

    var onChanged: ((Visit.Rating, Boolean) -> Unit)? = null

    init {

        refreshBG()
        setOnClickListener {
            isChecked = !isChecked
            refreshBG()
            onChanged?.invoke(rating, isChecked)
        }
    }

    private fun refreshBG() {

        val bgResId = if (isChecked) circleSolidResIds[rating.ordinal] else circleSolidResIds[0]
        setBackgroundResource(bgResId)
    }



}
package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.visit_cell.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*

class VisitCell(context: Context) :FrameLayout(context) {

    lateinit var visit: Visit

    init {
        View.inflate(context, R.layout.visit_cell, this).also {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.toDP(100))
        }
    }

    fun refresh(visit: Visit) {
        this.visit = visit

        visitText.text = visit.dateTime.toDateTimeText(context, withSecond = false)
        visitColorMark.setImageDrawable(ResourcesCompat.getDrawable(context.resources, ratingToColorButtonResId(visit.rating), null))

        visitDetailText.text = visit.toString(context, withDateTime = false, withLineBreak = false)
    }


}
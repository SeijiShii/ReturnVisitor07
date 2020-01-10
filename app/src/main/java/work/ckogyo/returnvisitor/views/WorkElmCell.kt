package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.work_elm_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.fragments.WorkListElm
import java.text.SimpleDateFormat
import java.util.*

class WorkElmCell(context: Context) : FrameLayout(context) {

    init {
        View.inflate(context, R.layout.work_elm_cell, this)
    }

    var dataElm: WorkListElm? = null
    set(value) {
        field = value

        field ?: return

        categoryText.text = field!!.category.toString()
        dateTimeText.text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(field!!.dateTime.time)
    }


}
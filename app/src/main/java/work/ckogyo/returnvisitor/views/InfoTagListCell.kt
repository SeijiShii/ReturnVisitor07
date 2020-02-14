package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.info_tag_list_cell.view.*
import kotlinx.android.synthetic.main.placement_list_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.InfoTag
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class InfoTagListCell(context: Context) : LinearLayout(context) {

    private lateinit var infoTag: InfoTag

    var onSelected: ((InfoTag) -> Unit)? = null

    init {

        View.inflate(context, R.layout.info_tag_list_cell, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.toDP(50))

        deleteInfoTagButton.setOnClick {

        }

        setOnClick {
            onSelected?.invoke(infoTag)
        }
    }

    fun refresh(tag: InfoTag) {
        this.infoTag = tag
        refreshInfoTagText()
    }

    private fun refreshInfoTagText() {
        infoTagText.text = infoTag.name
    }
}
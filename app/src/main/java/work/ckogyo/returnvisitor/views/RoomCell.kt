package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.room_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.utils.ratingToColorButtonResId

class RoomCell(context: Context, private val room: Place) : LinearLayout(context) {

    init {
        View.inflate(context, R.layout.room_cell, this)

        roomMark.setImageDrawable(ResourcesCompat.getDrawable(context.resources, ratingToColorButtonResId(room.rating), null))
        roomText.text = room.name
    }
}
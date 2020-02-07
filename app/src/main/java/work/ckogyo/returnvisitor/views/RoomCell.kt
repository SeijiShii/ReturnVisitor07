package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.room_cell.view.*
import kotlinx.android.synthetic.main.visit_cell.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.utils.*

class RoomCell(context: Context, private val room: Place) : HeightAnimationView(context) {

    override val collapseHeight: Int
        get() = 0
    override val extractHeight: Int
        get() = context?.toDP(50)?:0
    override val cellId: String
        get() = room.id

    var onClickShowRoom: ((room: Place) -> Unit)? = null
    var onDeleted: ((room: Place) -> Unit)? = null

    init {
        View.inflate(context, R.layout.room_cell, this)

        roomMark.setImageDrawable(ResourcesCompat.getDrawable(context.resources, ratingToColorButtonResId(room.rating), null))
        roomText.text = room.name
        roomMenuButton.setOnClick {
            showMenuPopup()
        }

        setOnClick {
            onClickShowRoom?.invoke(room)
        }
    }

    private fun showMenuPopup() {

        val popup = PopupMenu(context, roomMenuButton)
        popup.menuInflater.inflate(R.menu.room_cell_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.show_room -> {
                    onClickShowRoom?.invoke(room)
                }
                R.id.delete_room -> {
                    confirmDeleteRoom(context, room){
                        GlobalScope.launch {
                            PlaceCollection.instance.deleteAsync(room).await()
                            handler.post {
                                collapseToHeight0()
                                onDeleted?.invoke(room)
                            }
                        }
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }
}
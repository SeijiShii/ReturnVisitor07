package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.room_cell.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.utils.*

class RoomCell(context: Context) : FrameLayout(context) {

    lateinit var room: Place

    var onClickShowRoom: ((room: Place) -> Unit)? = null
    var onDeleteRoomConfirmed: ((room: Place) -> Unit)? = null

    init {
        View.inflate(context, R.layout.room_cell, this).also {
            it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        roomMenuButton.setOnClick {
            showMenuPopup()
        }

        setOnClick {
            onClickShowRoom?.invoke(room)
        }
    }

    fun refresh(room: Place) {

        this.room = room

        roomMark.setImageDrawable(ResourcesCompat.getDrawable(context.resources, ratingToColorButtonResId(this.room.rating), null))
        roomText.text = this.room.name
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
//                                collapseToHeight0()
                                onDeleteRoomConfirmed?.invoke(room)
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
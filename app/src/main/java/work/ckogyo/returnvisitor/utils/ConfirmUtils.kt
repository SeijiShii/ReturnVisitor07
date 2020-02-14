package work.ckogyo.returnvisitor.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.models.Visit

fun confirmDeleteVisit(context: Context, visit: Visit, onConfirmed: (Visit) -> Unit) {

    AlertDialog.Builder(context)
        .setTitle(R.string.delete_visit)
        .setMessage(context.resources.getString(R.string.delete_visit_confirm, visit.dateTime.toDateTimeText(context)))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.delete){ _, _ ->
            onConfirmed(visit)
        }
        .show()
}

fun confirmDeleteRoom(context: Context, room: Place, onConfirmed: (room: Place) -> Unit) {

    AlertDialog.Builder(context)
        .setTitle(R.string.delete_room)
        .setMessage(context.resources.getString(R.string.delete_room_confirm, room.name))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.delete){ _, _ ->
            onConfirmed(room)
        }
        .show()
}


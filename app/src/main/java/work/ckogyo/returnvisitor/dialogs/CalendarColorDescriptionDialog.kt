package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import work.ckogyo.returnvisitor.R

class CalendarColorDescriptionDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val v = View.inflate(context, R.layout.calendar_color_description_dialog, null)

        return AlertDialog.Builder(context)
            .setView(v)
            .setTitle(R.string.meaning_of_colors)
            .setNeutralButton(R.string.close, null)
            .create()
    }

}
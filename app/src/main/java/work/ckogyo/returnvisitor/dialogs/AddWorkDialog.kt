package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.add_work_dialog.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDateText
import work.ckogyo.returnvisitor.utils.toTimeText
import java.util.*

class AddWorkDialog : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var work: Work

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        work = Work()
        work.start = Calendar.getInstance()
        work.end = Calendar.getInstance()
        work.end.add(Calendar.MINUTE, 5)

        val v = View.inflate(context, R.layout.add_work_dialog, null)
        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.add_work)
            .setView(v)
            .setPositiveButton(R.string.add){_, _ -> }
            .setNegativeButton(R.string.cancel){_, _ -> }
            .create()

        v.dateText.text = work.start.toDateText(context!!)
        v.dateText.setOnClick {

        }

        v.startTimeText.text = work.start.toTimeText(context!!)
        v.startTimeText.setOnClick {

        }

        v.endTimeText.text = work.end.toTimeText(context!!)
        v.endTimeText.setOnClick {

        }

        return dialog
    }

}
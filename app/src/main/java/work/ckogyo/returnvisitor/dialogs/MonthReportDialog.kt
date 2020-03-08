package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.month_report_dialog.*
import kotlinx.android.synthetic.main.month_report_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.models.MonthReport
import work.ckogyo.returnvisitor.utils.*
import java.util.*

class MonthReportDialog(private val month: Calendar) : DialogFragment() {

    private var isDialogClosed = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val v = View.inflate(context, R.layout.month_report_dialog, null)
        val handler = Handler()

        GlobalScope.launch {
            val report = FirebaseDB.instance.loadMonthReport(month)

            if (isDialogClosed) return@launch

            handler.post {

                v ?: return@post

                v.placementCountText?.text = report.plcCount.toString()
                v.showVideoCountText?.text = report.showVideoCount.toString()
                v.timeText?.text = (report.duration / hourInMillis).toString()
                v.rvCountText?.text = report.rvCount.toString()
                v.studyCountText?.text = report.studyCount.toString()
                v.carryOverText?.text = report.carryOver.toMinuteText(context!!)

                v.loadingMonthReportOverlay?.fadeVisibility(false)
                v.reportContainer?.fadeVisibility(true)
            }
        }

        return AlertDialog.Builder(context).also {
            val title = context!!.resources.getString(R.string.month_report_title, month.toMonthTitleString(context!!))
            it.setTitle(title)
            it.setView(v)
            it.setPositiveButton(R.string.report_mail){ _, _ ->
                isDialogClosed = true
            }
            it.setNegativeButton(R.string.close){ _, _ ->
                isDialogClosed = true
//                Log.d(debugTag, "MonthReportDialog closed!")
            }
        }.create()
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)

//        Log.d(debugTag, "MonthReportDialog dismissed!")
        isDialogClosed = true
    }

}
package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.month_report_dialog.*
import kotlinx.android.synthetic.main.month_report_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.hourInMillis
import work.ckogyo.returnvisitor.utils.toMinuteText
import work.ckogyo.returnvisitor.utils.toMonthTitleString
import java.util.*

class MonthReportDialog(private val month: Calendar) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val v = View.inflate(context, R.layout.month_report_dialog, null)
        val handler = Handler()

        GlobalScope.launch {
            val report = MonthReportCollection.instance.updateAndLoadByMonth(month)

            handler.post {

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

            }
            it.setNegativeButton(R.string.close, null)
        }.create()
    }

}
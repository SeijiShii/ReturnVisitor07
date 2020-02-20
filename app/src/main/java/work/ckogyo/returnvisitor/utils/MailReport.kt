package work.ckogyo.returnvisitor.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.MonthReport
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.publisherNameKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.returnVisitorPrefsKey
import java.util.*

fun exportToMail(context: Context, report: MonthReport) {
    val prefs: SharedPreferences = context.getSharedPreferences(
        returnVisitorPrefsKey,
        MODE_PRIVATE
    )
    val name = prefs.getString(publisherNameKey, "")
    val subject: String = context.getString(R.string.month_report_title, report.month.toMonthTitleString(context))
    val message = ("$name\n" +
            "${context.getString(R.string.month)}: ${report.month.toMonthTitleString(context)}\n" +
            "${context.getString(R.string.placement)}: ${report.plcCount}\n" +
            "${context.getString(R.string.show_video)}: ${report.showVideoCount}\n" +
            "${context.getString(R.string.time)}: ${report.duration / hourInMillis}\n" +
            "${context.getString(R.string.return_visit)}: ${report.rvCount}\n" +
            "${context.getString(R.string.study)}: ${report.studyCount}\n" +
            "${context.getString(R.string.note)}: ")

    val mailIntent = Intent(Intent.ACTION_SENDTO).also {
        it.data = Uri.parse("mailto:")
        it.putExtra(Intent.EXTRA_SUBJECT, subject)
        it.putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(mailIntent)
}
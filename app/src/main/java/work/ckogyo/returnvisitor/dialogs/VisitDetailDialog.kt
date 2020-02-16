package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.record_visit_fragment.*
import kotlinx.android.synthetic.main.visit_detail_dialog.*
import kotlinx.android.synthetic.main.visit_detail_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.views.SmallTagView

class VisitDetailDialog(private val visit: Visit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val v = View.inflate(context, R.layout.visit_detail_dialog, null)

        val handler = Handler()

        GlobalScope.launch {
            val placeStr = visit.place.toStringAsync().await()

            handler.post {
                v?.placeText?.text = placeStr
            }
        }

        v.addressText.text = visit.place.address
        v.personsText.text = visit.toPersonVisitString(context!!)

        val plcTagViews = ArrayList<SmallTagView>()
        for (plc in visit.placements) {
            val tagView = SmallTagView(context!!, plc.toShortString(context!!)).also {
                it.backgroundResourceId = R.drawable.dark_violet_border_round
            }
            plcTagViews.add(tagView)
        }
        v.placementTagContainer.addTagViews(plcTagViews)

        val infoTagViews = ArrayList<SmallTagView>()
        for (tag in visit.infoTags) {
            val tagView = SmallTagView(context!!, tag.name)
            infoTagViews.add(tagView)
        }
        v.tagContainer.addTagViews(infoTagViews)

        v.noteText.text = visit.description

        return AlertDialog.Builder(context).also {

            it.setView(v)
            it.setNeutralButton(R.string.close, null)

        }.create()
    }
}
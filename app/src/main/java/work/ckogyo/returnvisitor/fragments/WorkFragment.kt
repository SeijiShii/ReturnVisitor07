package work.ckogyo.returnvisitor.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.work_elm_cell.view.*
import kotlinx.android.synthetic.main.work_fragment.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.WorkElement
import work.ckogyo.returnvisitor.utils.areSameDates
import work.ckogyo.returnvisitor.utils.debugTag
import work.ckogyo.returnvisitor.views.VisitCell
import work.ckogyo.returnvisitor.views.WorkElmCell
import java.util.*
import kotlin.collections.ArrayList

class WorkFragment(private val dataElms: ArrayList<WorkElement>,
                   private val dateToShow: Calendar) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.work_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = WorkElmAdapter(context!!, dataElms)

        workListView.adapter = adapter
        view.setOnTouchListener { _, _ -> true }

        val position = adapter.getPositionByDate(dateToShow)
        if (position > 0) {
            workListView.layoutManager!!.scrollToPosition(position)
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Log.d(debugTag, "view.width: ${view.width}, view.height: ${view.height}")
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }



    class WorkElmAdapter(private val context: Context, val dataElms: ArrayList<WorkElement>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return WorkElmViewHolder(WorkElmCell(context))
        }

        override fun getItemCount(): Int {
            return dataElms.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val elm = dataElms[position]
            (holder as WorkElmViewHolder).dataElm = elm
            if (elm.category == WorkElement.Category.Visit) {
                val visitCell = getVisitCell(elm.visit!!)
                (holder.itemView as WorkElmCell).attacheVisitCell(visitCell)
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)

            val elmCell = holder.itemView as WorkElmCell
            elmCell.detachVisitCell()
        }

        private val visitCells = ArrayList<VisitCell>()
        private fun getVisitCell(visit: Visit): VisitCell {

            for (cell in visitCells) {
                if (cell.visit == visit) {
                    return cell
                }
            }

            val cell = VisitCell(context, visit)
            visitCells.add(cell)
            return cell
        }

        fun getPositionByDate(date: Calendar): Int {
            for (i in 0 until dataElms.size) {
                if (areSameDates(date, dataElms[i].dateTime)) {
                    return i
                }
            }
            return -1
        }

    }

    class WorkElmViewHolder(private val elmCell: WorkElmCell): RecyclerView.ViewHolder(elmCell) {

        var dataElm : WorkElement? = null
        set(value) {
            field = value
            elmCell.dataElm = field
        }

    }


}


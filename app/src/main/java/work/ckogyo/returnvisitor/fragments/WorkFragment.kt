package work.ckogyo.returnvisitor.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.work_fragment.*
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.WorkElement
import work.ckogyo.returnvisitor.models.WorkElmList
import work.ckogyo.returnvisitor.utils.areSameDates
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.getPositionInAncestor
import work.ckogyo.returnvisitor.utils.toDateText
import work.ckogyo.returnvisitor.views.VisitCell
import work.ckogyo.returnvisitor.views.WorkElmCell
import java.util.*
import kotlin.collections.ArrayList

class WorkFragment(private val dataElms: ArrayList<WorkElement>,
                   private val dateToShow: Calendar) : Fragment() {

    private val handler = Handler()
    private lateinit var adapter: WorkElmAdapter
    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.work_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WorkElmAdapter(context!!, dataElms)

        workListView.adapter = adapter
        view.setOnTouchListener { _, _ -> true }

        workListView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    Log.d(debugTag, "onScrollStateChanged: SCROLL_STATE_IDLE")
                    GlobalScope.launch {
                        addNeighboringDateElmsIfNeededAsync(dateToShow, true).await()
                        addNeighboringDateElmsIfNeededAsync(dateToShow, false).await()
                    }
                    updateWorkDateText()
                }
            }
        })

        val position = adapter.getPositionByDate(dateToShow)
        if (position > 0) {
            workListView.layoutManager!!.scrollToPosition(position)
        }


        // 全体が表示されたなら前後日のデータを取得して表示する。
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
//                Log.d(debugTag, "view.width: ${view.width}, view.height: ${view.height}")
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                loadingWorkProgress.fadeVisibility(true)

                GlobalScope.launch {
                    addNeighboringDateElmsIfNeededAsync(dateToShow, true).await()
                    addNeighboringDateElmsIfNeededAsync(dateToShow, false).await()
                    handler.post {
                        loadingWorkProgress.fadeVisibility(false)
                    }
                }

                updateWorkDateText()
            }
        })
    }

    private fun addNeighboringDateElmsIfNeededAsync(date: Calendar, previous: Boolean):Deferred<Unit> {
        return GlobalScope.async {

            val neighboringDate = WorkElmList.instance.getNeighboringDateAsync(date, previous).await()
            if (neighboringDate == null || adapter.hasElmsOfDate(neighboringDate)) {
                handler.post {
                    mainActivity?.switchProgressOverlay(false)
                }
                return@async
            }

            val dateElms = WorkElmList.instance.generateListByDateAsync(neighboringDate).await()
            var waitingHandler = true
            handler.post {
                val layoutManager = workListView.layoutManager as LinearLayoutManager
                val currTopPos = layoutManager.findFirstVisibleItemPosition()
                val topView = layoutManager.findViewByPosition(currTopPos)
                val posInFrame = topView?.getPositionInAncestor(workListView)

//                Log.d(debugTag, "currTopPos: $currTopPos")
                val nextTopPos = adapter.addElms(dateElms, currTopPos)

//                Log.d(debugTag, "nextTopPos: $nextTopPos")

                val offsetY = posInFrame?.y ?: 0
                layoutManager.scrollToPositionWithOffset(nextTopPos, offsetY)
                waitingHandler = false
            }

            while (waitingHandler) {
                delay(50)
            }
            Unit
        }
    }

    private fun updateWorkDateText() {
        val layoutManager = workListView.layoutManager as LinearLayoutManager
        val topPos = layoutManager.findFirstCompletelyVisibleItemPosition()
        val topCell = layoutManager.findViewByPosition(topPos) as WorkElmCell
        val date = topCell.dataElm?.dateTime

        workDateText.text = date?.toDateText() ?: ""
    }

    class WorkElmAdapter(private val context: Context,
                         private val dataElms: ArrayList<WorkElement>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {


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

        fun getDateByPosition(pos: Int): Calendar? {
            if (pos <= dataElms.size - 1) {
                return dataElms[pos].dateTime
            }
            return null
        }

        private fun getTimeInMillisByPosition(pos: Int): Long {
            if (pos <= dataElms.size - 1) {
                return dataElms[pos].dateTime.timeInMillis
            }
            return -1
        }

        private fun getPositionByTimeInMillis(timeInMillis: Long): Int {
            for (i in 0 until dataElms.size) {
                if (dataElms[i].dateTime.timeInMillis == timeInMillis) {
                    return i
                }
            }
            return -1
        }

        fun addElms(elms: ArrayList<WorkElement>, currTopPos: Int):Int {

            val currTopTimeInMillis = getTimeInMillisByPosition(currTopPos)
            val merged = WorkElmList.mergeAvoidingDup(dataElms, elms)
            dataElms.clear()
            dataElms.addAll(merged)
            notifyDataSetChanged()

            if (currTopTimeInMillis < 0) return currTopPos

            val updatedPos = getPositionByTimeInMillis(currTopTimeInMillis)
            if (updatedPos < 0) return currTopPos

            return updatedPos
        }

        fun hasElmsOfDate(date: Calendar): Boolean {
            return getPositionByDate(date) >= 0
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


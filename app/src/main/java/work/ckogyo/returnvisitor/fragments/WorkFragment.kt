package work.ckogyo.returnvisitor.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
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
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.VisitCell
import work.ckogyo.returnvisitor.views.WorkElmCell
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

class WorkFragment(initialDate: Calendar) : Fragment(), DatePickerDialog.OnDateSetListener {

    private val handler = Handler()
    private lateinit var adapter: WorkElmAdapter
    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    private var date: Calendar = initialDate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.work_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnTouchListener { _, _ -> true }

        workListView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateDateByWorkList()
                    GlobalScope.launch {
                        addNeighboringDateElmsIfNeeded()
                    }
                }
            }
        })

        workDateText.setOnClick {
            showDatePicker()
        }

        refreshWorkList()
    }

    private fun refreshWorkList() {

//        val start = System.currentTimeMillis()

        switchLoadingWorkOverlay(true)
        GlobalScope.launch {
            val nearestDate = WorkElmList.instance.getNearestDateWithData(date)

            if (nearestDate == null) {
                handler.post {
                    noWorkFrame.fadeVisibility(true)
                    workListView.fadeVisibility(false)
                    loadingWorkOverlay.fadeVisibility(false)
                }
                return@launch
            }

            date = nearestDate

            handler.post {
                updateWorkDateText()
            }

            // 指定した日の最近日を中心に前後2日(最大5日)分のデータを取得する
            val dates = arrayListOf(date)

            for (i in 0 until 2) {
                val previous = WorkElmList.instance.getNeighboringDate(dates[0], true)
                if (previous != null) {
                    dates.add(0, previous)
                }

                val next = WorkElmList.instance.getNeighboringDate(dates[dates.size - 1], false)
                if (next != null) {
                    dates.add(next)
                }
            }

//            for(i in 0 until dates.size) {
//                Log.d(debugTag, "Loaded dates[$i]: ${dates[i].toDateText()}")
//            }
//            Log.d(debugTag, "Loading dates with data, took ${System.currentTimeMillis() - start}ms.")

            val dataElms = WorkElmList.instance.generateListByDateRange(dates[0], dates[dates.size - 1])
            adapter = WorkElmAdapter(context!!, dataElms)

            handler.post {
                workListView.adapter = adapter
                workListView.layoutManager = SmoothScrollingLayoutManager(context!!)

                val position = adapter.getPositionByDate(date)
                if (position > 0) {
                    workListView.layoutManager!!.scrollToPosition(position)
                }

                switchLoadingWorkOverlay(false)

            }
        }
    }

    private fun addNeighboringDateElmsIfNeeded() {

        handler.post {
            loadingWorkProgressSmall.fadeVisibility(true)
        }

        GlobalScope.launch {

            val layoutManager = workListView.layoutManager as LinearLayoutManager

            val topPos = layoutManager.findFirstVisibleItemPosition()
            val topCell = layoutManager.findViewByPosition(topPos) as WorkElmCell
            val diffToFirstDate = topCell.dataElm!!.dateTime.getDaysDiff(adapter.firstDate).absoluteValue

            val lastPos = layoutManager.findLastVisibleItemPosition()
            val lastCell = layoutManager.findViewByPosition(lastPos) as WorkElmCell
            val diffToLastDate = lastCell.dataElm!!.dateTime.getDaysDiff(adapter.lastDate).absoluteValue

            val elmsToAdd = ArrayList<WorkElement>()

            if (diffToFirstDate <= 1 ) {
                val previous = WorkElmList.instance.getNeighboringDate(adapter.firstDate, true)
                if (previous != null && !adapter.hasElmsOfDate(previous)) {
                    val elms = WorkElmList.instance.generateListByDate(previous)
                    elmsToAdd.addAll(elms)
                }
            }

            if (diffToLastDate <= 1) {
                val next = WorkElmList.instance.getNeighboringDate(adapter.lastDate, false)
                if (next != null && !adapter.hasElmsOfDate(next)) {
                    val elms = WorkElmList.instance.generateListByDate(next)
                    elmsToAdd.addAll(elms)
                }
            }

            if (elmsToAdd.isEmpty()) {
                handler.post {
                    loadingWorkProgressSmall.fadeVisibility(false)
                }
                return@launch
            }

            handler.post {
                val layoutManager = workListView.layoutManager as LinearLayoutManager
                val currTopPos = layoutManager.findFirstVisibleItemPosition()
                val topView = layoutManager.findViewByPosition(currTopPos)
                val posInFrame = topView?.getPositionInAncestor(workListView)

//                Log.d(debugTag, "currTopPos: $currTopPos")
                val nextTopPos = adapter.addElms(elmsToAdd, currTopPos)

//                Log.d(debugTag, "nextTopPos: $nextTopPos")

                val offsetY = posInFrame?.y ?: 0
                layoutManager.scrollToPositionWithOffset(nextTopPos, offsetY)
                loadingWorkProgressSmall.fadeVisibility(false)
            }
        }
    }

    private fun updateDateByWorkList() {
        val layoutManager = workListView.layoutManager as LinearLayoutManager
        val topPos = layoutManager.findFirstCompletelyVisibleItemPosition()
        val topCell = layoutManager.findViewByPosition(topPos) as WorkElmCell
        date = topCell.dataElm?.dateTime ?: date
        updateWorkDateText()
    }

    private fun updateWorkDateText() {
        workDateText.text = date.toDateText(context!!)
    }


    private fun showDatePicker() {
        context?:return

        DatePickerDialog(context!!,
            this,
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH),
            date.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val date = Calendar.getInstance()
        date.set(Calendar.YEAR, year)
        date.set(Calendar.MONTH, month)
        date.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        scrollByDateOrRefresh(date)

//        GlobalScope.launch {
//            val dateHasElm = WorkElmList.instance.aDayHasElmAsync(date).await()
//            if (!dateHasElm) {
//                val cp0 = System.currentTimeMillis()
//                val previousDate = WorkElmList.instance.getNeighboringDate(date, true)
//
//                val cp1 = System.currentTimeMillis()
//                Log.d(debugTag, "Loading previousDate took: ${cp1 - cp0}ms.")
//                val nextDate = WorkElmList.instance.getNeighboringDate(date, false)
//
//                val cp2 = System.currentTimeMillis()
//                Log.d(debugTag, "Loading nextDate took: ${cp2 - cp1}ms.")
//
//                when {
//                    previousDate == null && nextDate == null -> {
//                        // Do nothing.
//                    }
//                    previousDate == null -> {
//                        scrollByDate(nextDate!!)
//                    }
//                    nextDate == null -> {
//                        scrollByDate(previousDate)
//                    }
//                    else -> {
//
//                        val prevDiff = date.getDaysDiff(previousDate)
//                        val nextDiff = date.getDaysDiff(nextDate)
//
//                        val dateToScroll = if (prevDiff.absoluteValue < nextDiff.absoluteValue) previousDate else nextDate
//                        scrollByDate(dateToScroll)
//                    }
//                }
//            }
//        }
    }

    private fun scrollByDateOrRefresh(date: Calendar) {
        if (adapter.hasElmsOfDate(date)) {
            this.date = date
            val pos = adapter.getPositionByDate(this.date)
            workListView.smoothScrollToPosition(pos)
            updateWorkDateText()
        } else {
            switchLoadingWorkOverlay(true)
            GlobalScope.launch {
                this@WorkFragment.date = WorkElmList.instance.getNearestDateWithData(date)!!
                if (adapter.hasElmsOfDate(this@WorkFragment.date)) {
                    handler.post {
                        val pos = adapter.getPositionByDate(this@WorkFragment.date)
                        workListView.smoothScrollToPosition(pos)
                        updateWorkDateText()
                        switchLoadingWorkOverlay(false)
                    }
                } else {
                    handler.post {
                        refreshWorkList()
                    }
                }
            }


        }


    }

    private fun switchLoadingWorkOverlay(loading: Boolean) {

        loadingWorkOverlay.fadeVisibility(loading)
        workListView.fadeVisibility(!loading, addTouchBlockerOnFadeIn = false)
        workDateText.isEnabled = !loading
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
                if (date.isSameDate(dataElms[i].dateTime)) {
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

        val firstDate: Calendar
            get() {
                if (dataElms.isEmpty()) {
                    throw IllegalStateException("WorkElmAdapter.firstDate is referred even dataElms is empty!")
                }
                return dataElms[0].dateTime.cloneWith0Time()
            }

        val lastDate: Calendar
            get() {
                if (dataElms.isEmpty()) {
                    throw IllegalStateException("WorkElmAdapter.lastDate is referred even dataElms is empty!")
                }
                return dataElms[dataElms.size - 1].dateTime.cloneWith0Time()
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


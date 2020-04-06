package work.ckogyo.returnvisitor.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.work_fragment.*
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.AddWorkDialog
import work.ckogyo.returnvisitor.dialogs.VisitDetailDialog
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.models.WorkElement
import work.ckogyo.returnvisitor.models.WorkElmList
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.savedInstanceStateKeys.dateInLongKey
import work.ckogyo.returnvisitor.views.VisitCell
import work.ckogyo.returnvisitor.views.WorkElmCell
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class WorkFragment() : Fragment(), DatePickerDialog.OnDateSetListener {

    private var date: Calendar? = null

    constructor(date: Calendar? = null):this() {
        this.date = date
    }

    private var hasAdapterInitialized = false
    constructor(addedWork: Work) :this() {
        this.date = addedWork.start

        GlobalScope.launch {
            while (!hasAdapterInitialized) {
                delay(50)
            }
            handler.post {
                adapter.onWorkTimeChanged(addedWork)
            }
        }
    }

    var onBackToMapFragment: (() -> Unit)? = null

    // WorkFragmentでVisitを編集したときMapFragmentに伝播するメソッドが必要

    private val handler = Handler()
    private lateinit var adapter: WorkElmAdapter
    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    var onVisitEdited: ((Visit, OnFinishEditParam) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.work_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateInLong = savedInstanceState?.getLong(dateInLongKey)
        if (date == null && dateInLong != null) {
            date = Calendar.getInstance()
            date!!.timeInMillis = dateInLong
        }

        date ?: backToMapFragment()

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

        backToMapButton.setOnClick {
            backToMapFragment()
        }

        workFragmentMenuButton.setOnClick {
            showMenuPopup()
        }

        refreshWorkList()
    }

    private fun refreshWorkList() {

        val start = System.currentTimeMillis()

        switchLoadingWorkOverlay(true)
        GlobalScope.launch {

            date ?: return@launch

            val nearestDate = WorkElmList.instance.getNearestDateWithData(date!!)

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

            val dates = arrayListOf(date!!)

//            val start2 = System.currentTimeMillis()
//            Log.d(debugTag, "Started loading dataElms for ${date.toJPDateText()}")
            val dataElms = WorkElmList.instance.generateListByDate(dates[0])

//            Log.d(debugTag, "Loading dataElms for ${date.toJPDateText()}, took ${System.currentTimeMillis() - start2}ms.")

            context?:return@launch

            adapter = WorkElmAdapter(dataElms)

            handler.post {

                workListView ?: return@post
                date ?: return@post

                workListView?.adapter = adapter
                workListView?.layoutManager = SmoothScrollingLayoutManager(context!!)

                val position = adapter.getPositionByDate(date!!)
                if (position > 0) {
                    workListView?.layoutManager!!.scrollToPosition(position)
                }
                switchLoadingWorkOverlay(false)
                hasAdapterInitialized = true
            }

            // 指定した日の最近日を中心に前後2日(最大5日)分のデータを取得する
            for (i in 0 until 2) {
                val previous = WorkElmList.instance.getNeighboringDate(dates[0], true)
                if (previous != null) {
                    handler.post {
                        loadingWorkProgressSmall?.fadeVisibility(true)
                    }
                    dates.add(0, previous)
//                    val start3 = System.currentTimeMillis()
//                    Log.d(debugTag, "Started loading dataElms for ${previous.toJPDateText()}")
                    val elms = WorkElmList.instance.generateListByDate(previous)
//                    Log.d(debugTag, "Loading dataElms for ${previous.toJPDateText()}, took ${System.currentTimeMillis() - start3}ms.")
                    handler.post {
                        adapter.addElms(elms)
                    }
                }

                val next = WorkElmList.instance.getNeighboringDate(dates[dates.size - 1], false)
                if (next != null) {
                    handler.post {
                        loadingWorkProgressSmall?.fadeVisibility(true)
                    }
                    dates.add(next)
//                    val start4 = System.currentTimeMillis()
//                    Log.d(debugTag, "Started loading dataElms for ${next.toJPDateText()}")
                    val elms = WorkElmList.instance.generateListByDate(next)
//                    Log.d(debugTag, "Loading dataElms for ${next.toJPDateText()}, took ${System.currentTimeMillis() - start4}ms.")
                    handler.post {
                        adapter.addElms(elms)
                    }
                }
            }

            handler.post {
                loadingWorkProgressSmall?.fadeVisibility(false)
            }

        }
    }

    private fun addNeighboringDateElmsIfNeeded() {

        handler.post {
            loadingWorkProgressSmall?.fadeVisibility(true, addTouchBlockerOnFadeIn = true)
        }

        GlobalScope.launch {

            val layoutManager = workListView.layoutManager as LinearLayoutManager

            val topPos = layoutManager.findFirstVisibleItemPosition()
            val topCell = layoutManager.findViewByPosition(topPos) as WorkElmCell
            val topDate = topCell.dataElm!!.dateTime
//            Log.d(debugTag, "Top visible cell date: ${topDate.toDateText(context!!)}")
            val diffToFirstDate = topDate.getDaysDiff(adapter.firstDate).absoluteValue

            val bottomPos = layoutManager.findLastVisibleItemPosition()
            val bottomCell = layoutManager.findViewByPosition(bottomPos) as WorkElmCell
            val bottomDate = bottomCell.dataElm!!.dateTime
//            Log.d(debugTag, "Bottom visible cell date: ${bottomDate.toDateText(context!!)}")
            val diffToLastDate = bottomDate.getDaysDiff(adapter.lastDate).absoluteValue

            val elmsToAdd = ArrayList<WorkElement>()

            if (diffToFirstDate <= 1 ) {
                val previous = WorkElmList.instance.getNeighboringDate(adapter.firstDate, true)
//                Log.d(debugTag, "Previous date with data: ${previous?.toDateText(context!!)}")
                if (previous != null && !adapter.hasElmsOfDate(previous)) {
                    val elms = WorkElmList.instance.generateListByDate(previous)
                    elmsToAdd.addAll(elms)
                }
            }

            if (diffToLastDate <= 1) {
                val next = WorkElmList.instance.getNeighboringDate(adapter.lastDate, false)
//                Log.d(debugTag, "Next date with data: ${next?.toDateText(context!!)}")
                if (next != null && !adapter.hasElmsOfDate(next)) {
                    val elms = WorkElmList.instance.generateListByDate(next)
                    elmsToAdd.addAll(elms)
                }
            }

            if (elmsToAdd.isEmpty()) {
                handler.post {
                    loadingWorkProgressSmall?.fadeVisibility(false)
                }
                return@launch
            }

            handler.post {
                workListView ?: return@post
                adapter.addElms(elmsToAdd)
                loadingWorkProgressSmall?.fadeVisibility(false)
            }
        }
    }

    private fun updateDateByWorkList() {
        val layoutManager = workListView.layoutManager as LinearLayoutManager
        val topPos = layoutManager.findFirstCompletelyVisibleItemPosition()
        val topCell = layoutManager.findViewByPosition(topPos) as? WorkElmCell

        topCell ?: return

        date = topCell.dataElm?.dateTime ?: date
        updateWorkDateText()
    }

    private fun updateWorkDateText() {
        date ?: return
        workDateText?.text = date!!.toDateText(context!!)
    }


    private fun showDatePicker() {
        context?:return
        date ?: return

        DatePickerDialog(context!!,
            this,
            date!!.get(Calendar.YEAR),
            date!!.get(Calendar.MONTH),
            date!!.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val date = Calendar.getInstance()
        date.set(Calendar.YEAR, year)
        date.set(Calendar.MONTH, month)
        date.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        scrollByDateOrRefresh(date)

    }

    private fun scrollByDateOrRefresh(date: Calendar) {
        if (adapter.hasElmsOfDate(date)) {
            this.date = date
            val pos = adapter.getPositionByDate(this.date!!)
            workListView.smoothScrollToPosition(pos)
            updateWorkDateText()
        } else {
            switchLoadingWorkOverlay(true)
            GlobalScope.launch {
                this@WorkFragment.date = WorkElmList.instance.getNearestDateWithData(date)!!
                if (adapter.hasElmsOfDate(this@WorkFragment.date!!)) {
                    handler.post {
                        val pos = adapter.getPositionByDate(this@WorkFragment.date!!)
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

    private fun showMenuPopup() {

        val popup = PopupMenu(context, workFragmentMenuButton)
        popup.menuInflater.inflate(R.menu.work_fragment_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.add_work -> {
                    mainActivity ?: return@setOnMenuItemClickListener true

                    AddWorkDialog().apply {
                        onWorkAdded = { work ->
                            val workElms = WorkElement.fromWork(work)
                            var index = -1
                            for (elm in workElms) {
                                index = adapter.insertElm(elm)
                            }

                            if (index >= 1) {
                                this@WorkFragment.workListView.smoothScrollToPosition(index)
                            }
                            adapter.onWorkTimeChanged(work)
                        }
                    }.show(mainActivity!!.supportFragmentManager, AddWorkDialog::class.java.simpleName)
                }
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun backToMapFragment() {

        mainActivity?.supportFragmentManager?.beginTransaction()
            ?.remove(this)
            ?.commit()

        onBackToMapFragment?.invoke()
    }

    private fun onShowInWideMapInVisitDetail(visit: Visit) {
        backToMapFragment()
        mainActivity?.mapFragment?.animateToLatLng(visit.place.latLng)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        date ?: return

        outState.putLong(dateInLongKey, date!!.timeInMillis)
    }

    inner class WorkElmAdapter(private val dataElms: ArrayList<WorkElement>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val mainActivity = context as? MainActivity

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return WorkElmViewHolder(WorkElmCell(context!!).apply {
                onWorkTimeChanged = this@WorkElmAdapter::onWorkTimeChanged
                onDeleteWorkClicked = this@WorkElmAdapter::onDeleteWorkClickedInCell
            })
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

            val cell = VisitCell(context!!).also {
                it.refresh(visit)
                it.setOnClick { cell ->
                    VisitDetailDialog((cell as VisitCell).visit).also { dialog ->
                        dialog.onClickEditVisit = { visit2 ->
                            mainActivity?.showRecordVisitFragmentForEdit(visit2, this::onFinishEditVisit)
                        }
                        dialog.onDeleteVisitConfirmed = this::onVisitDeleted
                        dialog.onClickShowInWideMap = this@WorkFragment::onShowInWideMapInVisitDetail
                    }.show(childFragmentManager, VisitDetailDialog::class.java.simpleName)
                }
            }
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

            if (dataElms.isEmpty()) return -1

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

        fun addElms(elms: ArrayList<WorkElement>) {

            workListView ?: return

            val lManager = workListView.layoutManager as LinearLayoutManager
            val currTopPos = lManager.findFirstVisibleItemPosition()
            val topView = lManager.findViewByPosition(currTopPos)
            val posInFrame = topView?.getPositionInAncestor(workListView)

            val currTopTimeInMillis = getTimeInMillisByPosition(currTopPos)

            for (elm in elms) {
                val merged = WorkElmList.addElmAvoidingDup(dataElms, elm)
                val pos = merged.indexOf(elm)
                if (pos >= 0) {
                    dataElms.clear()
                    dataElms.addAll(merged)
                    notifyItemInserted(pos)
                }
            }

            if (currTopTimeInMillis < 0) return

            val updatedPos = getPositionByTimeInMillis(currTopTimeInMillis)
            if (updatedPos < 0) return

            val offsetY = posInFrame?.y ?: 0
            lManager.scrollToPositionWithOffset(updatedPos, offsetY)

        }

        fun hasElmsOfDate(date: Calendar): Boolean {
            return getPositionByDate(date) >= 0
        }

        fun onWorkTimeChanged(work: Work /*, category: WorkElement.Category, oldTime: Calendar, newTime: Calendar*/) {

            val startCellPos = getPositionByWorkAndCategory(work, WorkElement.Category.WorkStart)
            val startCell = workListView.findViewHolderForLayoutPosition(startCellPos)?.itemView as? WorkElmCell
            startCell?.updateDurationText()

//            Log.d(debugTag, "Work time changed oldTime: ${oldTime.toTimeText(context)}, newTime: ${newTime.toTimeText(context)}")

            // 時間変更後、カブリが発生したら調整する
            val currStartPos = getPositionByWorkAndCategory(work, WorkElement.Category.WorkStart)
            val currEndPos = getPositionByWorkAndCategory(work, WorkElement.Category.WorkEnd)
            dataElms.sortBy { e -> e.dateTime.timeInMillis }

            val nextStartPos = getPositionByDateTime(work.start)
            if (currStartPos != nextStartPos) {
                notifyItemMoved(currStartPos, nextStartPos)
            }

            val nextEndPos = getPositionByDateTime(work.end)
            if (currEndPos != nextEndPos) {
                notifyItemMoved(currEndPos, nextEndPos)
            }

            // 読み込み済みのWorkを抽出する
            val sameDayWorksInElms = ArrayList<Work>()
            for(elm in dataElms) {
                if (elm.work != null
                    && !sameDayWorksInElms.contains(elm.work!!)
                    && elm.work!!.start.isSameDate(work.start)) {
                    sameDayWorksInElms.add(elm.work!!)
                }
            }

            val worksToRemove = ArrayList<Work>()

            loop@for (work2 in sameDayWorksInElms) {
                if (work == work2) continue
                when {
                    work2.start.isTimeBetween(work.start, work.end, true)
                            || work2.end.isTimeBetween(work.start, work.end, true) -> {
                        // 調整後のWorkに他のWork前後どちらかがかぶる場合、比較対象のWorkを削除してしまう。
                        worksToRemove.add(work2)

                        when {
                            work2.start.isTimeBetween(work.start, work.end, true)
                                    && work2.end.isTimeBetween(work.start, work.end, true) -> {
                                // すっぽり収まる時は削除するだけ
                            }
                            work2.start.isTimeBetween(work.start, work.end, true) -> {
                                work.end = work2.end.clone() as Calendar
                                val workEndPos = getPositionByWorkAndCategory(work, WorkElement.Category.WorkEnd)
                                // データの移動があるのでrecyclerView.layoutManager?.findViewByPosition(Int)では正しいポジションがしゅとくできないらしい。
                                val cell = workListView.findViewHolderForAdapterPosition(workEndPos)?.itemView as? WorkElmCell
                                cell?.refresh()
                            }
                            work2.end.isTimeBetween(work.start, work.end, true) -> {
                                work.start = work2.start.clone() as Calendar
                                val workStartPos = getPositionByWorkAndCategory(work, WorkElement.Category.WorkStart)
                                val cell = workListView.findViewHolderForAdapterPosition(workStartPos)?.itemView as? WorkElmCell
                                cell?.refresh()
                            }
                        }
                    }
                    work.start.isTimeBetween(work2.start, work2.end, true)
                            || work.end.isTimeBetween(work2.start, work2.end, true) -> {
                        // 自分自身が他のWorkにすっぽり包まれているなら自分を削除
                        worksToRemove.add(work)
                        break@loop
                    }
                }
            }

            // 後ろから削除
            worksToRemove.sortByDescending { w -> w.start }
            for (work3 in worksToRemove) {
                val workEndPos = getPositionByWorkAndCategory(work3, WorkElement.Category.WorkEnd)
                dataElms.removeAt(workEndPos)
                notifyItemRemoved(workEndPos)

                val workStartPos = getPositionByWorkAndCategory(work3, WorkElement.Category.WorkStart)
                dataElms.removeAt(workStartPos)
                notifyItemRemoved(workStartPos)
            }

            WorkElmList.refreshIsVisitInWork(dataElms)
            for (elm in dataElms) {
                if (elm.category == WorkElement.Category.Visit) {
                    val pos = getPositionByVisit(elm.visit!!)
                    val cell = workListView.findViewHolderForAdapterPosition(pos)?.itemView as? WorkElmCell
                    cell?.refresh()
                }
            }

            mainActivity?.switchProgressOverlay(true, context!!.getString(R.string.saving_changes))
            val handler = Handler()
            GlobalScope.launch {
                FirebaseDB.instance.saveWorkAsync(work)
                for (work4 in worksToRemove) {
                    FirebaseDB.instance.deleteWorkAsync(work4)
                }
                handler.post {
                    mainActivity?.switchProgressOverlay(false)
                }
            }

        }


        /**
         * WorkCell内でWork削除ボタンが押されたときのためのコールバック
         * この関数内でFirebaseDBからの削除も行う
         */
        private fun onDeleteWorkClickedInCell(work: Work) {

            val handler = Handler()
            GlobalScope.launch {

                // 終了ポジション先取得して削除しないと順番が狂うはず
                FirebaseDB.instance.deleteWorkAsync(work)

                val endPos = getPositionByWorkAndCategory(work, WorkElement.Category.WorkEnd)
                val startPos = getPositionByWorkAndCategory(work, WorkElement.Category.WorkStart)

                deleteWorkElms(work)
                WorkElmList.refreshIsVisitInWork(dataElms)

                val dateBorderPos = deleteDateBorderPositionIfADayHasNoElm(work.start)

                handler.post{

                    notifyItemRemoved(endPos)
                    notifyItemRemoved(startPos)
                    if (dateBorderPos >= 0) {
                        notifyItemRemoved(dateBorderPos)
                    }
                }
            }
        }

        private fun onVisitDeleted(visit: Visit) {
            val pos = getPositionByVisit(visit)
            if (pos < 0) return

            dataElms.removeAt(pos)
            notifyItemRemoved(pos)

            val handler = Handler()
            GlobalScope.launch {
                FirebaseDB.instance.deleteVisitAsync(visit).await()
                val dateBorderPos = deleteDateBorderPositionIfADayHasNoElm(visit.dateTime)

                handler.post {
                    if (dateBorderPos >= 0) {
                        notifyItemRemoved(dateBorderPos)
                    }
                }
            }
        }

        private suspend fun deleteDateBorderPositionIfADayHasNoElm(date: Calendar): Int = suspendCoroutine { cont ->
            GlobalScope.launch {
                if (WorkElmList.instance.aDayHasElm(date)) {
                    cont.resume(-1)
                    return@launch
                }

                val dateBorderElm = getDateBorderElmByDate(date)
                if (dateBorderElm == null) {
                    cont.resume(-1)
                    return@launch
                }

                val pos = dataElms.indexOf(dateBorderElm)

                dataElms.remove(dateBorderElm)
                cont.resume(pos)
            }
        }

        private fun getDateBorderElmByDate(date: Calendar): WorkElement? {
            for(elm in dataElms) {
                if (date.isSameDate(elm.dateTime) && elm.category == WorkElement.Category.DateBorder) {
                    return elm
                }
            }
            return null
        }



        private fun deleteWorkElms(work: Work) {
            val tmp = ArrayList<WorkElement>()
            for (elm in dataElms) {
                if (elm.work == null || elm.work!! != work) {
                    tmp.add(elm)
                }
            }
            dataElms.clear()
            dataElms.addAll(tmp)
        }

        /**
         * 現状のdataElms内でのポジションを取得
         */
        private fun getPositionByWorkAndCategory(work: Work, category: WorkElement.Category): Int {
            for (i in 0 until dataElms.size) {
                dataElms[i].work ?: continue
                if (dataElms[i].work == work && dataElms[i].category == category) {
                    return i
                }
            }
            return -1
        }

        private fun getDataElmByWorkAndCategory(work: Work, category: WorkElement.Category):WorkElement? {
            val pos = getPositionByWorkAndCategory(work, category)
            if (pos < 0) return null
            return dataElms[pos]
        }

        /**
         * dataElmsが日時でソートされていることを前提で取得
         */
        private fun getPositionByDateTime(dateTime: Calendar): Int {

            var pos = 0

//            Log.d(debugTag, "dateTime: ${dateTime.toTimeText(context)}, ${dateTime.timeInMillis}")

            while (pos < dataElms.size) {
//                Log.d(debugTag, "dataElms[$pos].dateTime: ${dataElms[pos].dateTime.toTimeText(context)}, ${dataElms[pos].dateTime.timeInMillis}")
                if (dateTime.timeInMillis <= dataElms[pos].dateTime.timeInMillis) {
                    break
                }
                pos++
            }
            return pos
        }

        private fun getPositionByVisit(visit: Visit): Int {
            for (i in 0 until dataElms.size) {
                dataElms[i].visit ?: continue
                if (dataElms[i].visit == visit) {
                    return i
                }
            }
            return -1
        }

        fun insertElm(elm: WorkElement): Int {

            dataElms.add(elm)
            dataElms.sortBy { elm2 -> elm2.dateTime.timeInMillis }
            val index = dataElms.indexOf(elm)

            notifyItemInserted(index)
            return index
        }

        private fun onFinishEditVisit(visit: Visit, mode: EditMode, param: OnFinishEditParam) {

            // 編集後と削除後のWorkFragmentにおけるVisitCellの挙動
            when(param) {
                OnFinishEditParam.Canceled -> {}
                OnFinishEditParam.Done -> {
                    refreshAndMoveVisitCell(visit)

                    GlobalScope.launch {

                        FirebaseDB.instance.saveVisitAsync(visit).await()

                        // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
                        TimeCountIntentService.saveWorkIfActive()
                    }
                }
                OnFinishEditParam.Deleted -> {
                    onVisitDeleted(visit)
                }
            }

            onVisitEdited?.invoke(visit, param)
        }

        private fun refreshAndMoveVisitCell(visit: Visit) {

            val oldPos = getPositionByVisit(visit)
            if (oldPos >= 0) {

                dataElms[oldPos].visit = visit

                val elmCell = workListView.findViewHolderForAdapterPosition(oldPos)?.itemView as? WorkElmCell

                elmCell ?: return

                elmCell.visitCell?.refresh(visit)

                dataElms.sortBy { elm -> elm.dateTime.timeInMillis }
                WorkElmList.refreshIsVisitInWork(dataElms)

                val newPos = getPositionByVisit(visit)
                if (newPos < 0) return

                if (oldPos != newPos) {
                    notifyItemMoved(oldPos, newPos)
                    elmCell.refreshInWorkBorders()
                }

            }
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


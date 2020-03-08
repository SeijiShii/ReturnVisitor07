package work.ckogyo.returnvisitor.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.where_to_go_next_fragment.*
import kotlinx.android.synthetic.main.where_to_go_next_fragment.visitListView
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.VisitDetailDialog
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.VisitFilter
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.sortByDescendingDateTimeKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.sortByDescendingRatingKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.returnVisitorPrefsKey
import work.ckogyo.returnvisitor.views.RaterFilterToggleButton
import work.ckogyo.returnvisitor.views.VisitCell

class WhereToGoNextFragment : Fragment() {

    var onBackToMapFragment: (() -> Unit)? = null
    var onVisitEdited: ((Visit, OnFinishEditParam) -> Unit)? = null

    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    private lateinit var periodTerms: ArrayList<String>

    private lateinit var visitFilter: VisitFilter
    private val visits = ArrayList<Visit>()
    private val visitsToShow = ArrayList<Visit>()

    private var sortByDescendingDateTime = true
    private var sortByDescendingRating = true

//    private var visitsLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.where_to_go_next_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context ?: return

        loadSortConditions()
        visitFilter = VisitFilter.loadFromSharedPreferences(context!!)

        view.setOnTouchListener { _, _ -> true }

        backToMapButton?.setOnClickListener {
            backToMapFragment()
        }

        filterPanelFrame?.extractHeight = context!!.toDP(100)

        filterPanelFrame?.isExtracted = false
        filterPanelFrame?.refreshHeight()

        panelExtractBar?.setOnClick {
            filterPanelFrame?.isExtracted = !filterPanelFrame.isExtracted
            filterPanelFrame?.animateHeight()

            refreshArrowImage()
        }

        whereToGoMenuButton.setOnClick {
            showMenuPopup()
        }

        periodTerms = context!!.resources.getStringArray(R.array.periodUnitArray).toCollection(ArrayList())

        refreshNoVisitsFrame(show = false, animated = false)
        refreshLoadingVisitsOverlay(show = false, animated = false)

        refreshLoadingVisitsOverlay(show = true)
        refreshNoVisitsFrame(show = false)

        loadLatestVisits()

    }

    private var firstChunkLoaded = false
    private var loadingJob: Job? = null
    private var loadingVisitsCanceled = false

    private fun loadLatestVisits() {

        val handler = Handler()

        GlobalScope.launch {
            val loadedVisits = FirebaseDB.instance.loadLatestVisits(sortByDateTimeDescending = sortByDescendingDateTime, sortByRatingDescending = sortByDescendingRating)
            visits.clear()
            visits.addAll(loadedVisits)

            handler.post {
                initFilterPanel()
                refreshVisitList()
                refreshLoadingVisitsOverlay(show = false)
            }
        }
    }

    private fun isVisitContainedInFilterRatings(visit: Visit): Boolean {
        return visitFilter.ratings.contains(visit.rating)
    }

    private fun isVisitInFilterPeriod(visit: Visit): Boolean {
        return visitFilter.periodStartDate.timeInMillis <= visit.dateTime.timeInMillis
                && visit.dateTime.timeInMillis <= visitFilter.periodEndDate.timeInMillis
    }

    private var chunkArrivedAt = -1L


    private fun refreshVisitsToShow() {

        val tmpVisits1 = ArrayList<Visit>()
        for (visit in visits) {
            if (isVisitContainedInFilterRatings(visit)) {
                tmpVisits1.add(visit)
            }
        }

        val tmpVisits2 = ArrayList<Visit>()
        for (visit in tmpVisits1) {
            if (isVisitInFilterPeriod(visit)) {
                tmpVisits2.add(visit)
            }
        }

        visitsToShow.clear()
        visitsToShow.addAll(tmpVisits2)

        refreshSortings()
    }

    private fun refreshSortings() {
        if (sortByDescendingDateTime) {
            visitsToShow.sortByDescending { v -> v.dateTime.timeInMillis }
        } else {
            visitsToShow.sortBy {  v -> v.dateTime.timeInMillis }
        }

        if (sortByDescendingRating) {
            visitsToShow.sortByDescending { v -> v.rating.ordinal }
        } else {
            visitsToShow.sortBy { v -> v.rating.ordinal }
        }
    }

    private fun refreshArrowImage() {
        val arrowResId = if (filterPanelFrame.isExtracted) {
            R.drawable.ic_keyboard_arrow_down_white_24dp
        } else {
            R.drawable.ic_keyboard_arrow_up_white_24dp
        }
        arrowImageView?.setBackgroundResource(arrowResId)
    }

    private fun refreshNoVisitsFrame(show: Boolean, animated: Boolean = true) {

        if (animated) {
            noVisitDataFrame?.fadeVisibility(show)
        } else {
            noVisitDataFrame?.alpha = if (show) 1f else 0f
        }
    }

    private fun refreshLoadingVisitsOverlay(show: Boolean, animated: Boolean = true) {

        if (animated) {
            loadingFilteredVisitsOverlay?.fadeVisibility(show, addTouchBlockerOnFadeIn = true)
        } else {
            loadingFilteredVisitsOverlay?.alpha = if (show) 1f else 0f
            if (show) {
                loadingFilteredVisitsOverlay?.setOnTouchListener { _, _ -> true }
            } else {
                loadingFilteredVisitsOverlay?.setOnTouchListener(null)
            }
        }
    }

    private fun initFilterPanel() {

        initRaterFilters()

        initPeriodStartSpinner()
        initPeriodEndSpinner()
    }

    private fun initRaterFilters() {

        raterFilterContainer?.removeAllViews()

        for (i in 1..6) {
            val filterButton = RaterFilterToggleButton(context!!, Visit.Rating.values()[i]).also {
                it.isChecked = visitFilter.ratings.contains(it.rating)
                it.layoutParams = LinearLayout.LayoutParams(context!!.toDP(40), LinearLayout.LayoutParams.MATCH_PARENT)
                it.onChanged = this::onRatingFilterChanged
            }
            raterFilterContainer?.addView(filterButton)
        }
    }

    private fun onRatingFilterChanged(rating: Visit.Rating, isChecked: Boolean) {

        if (visitFilter.ratings.contains(rating)) {
            if (!isChecked) {
                visitFilter.ratings.remove(rating)
            }
        } else {
            if (isChecked) {
                visitFilter.ratings.add(rating)
            }
        }

        refreshVisitList()

        context ?: return
        visitFilter.save(context!!)
    }

    private fun initPeriodStartSpinner() {
        val startArray = periodTerms.subList(0, periodTerms.size - 1)
        periodStartSpinner?.adapter = ArrayAdapter<String>(context!!, R.layout.simple_text_view, startArray)

        periodStartSpinner?.setSelection(visitFilter.periodStart.ordinal)

        periodStartSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var periodStart = VisitFilter.PeriodTerm.values()[position]
                if (periodStart.ordinal >= visitFilter.periodEnd.ordinal) {
                    periodStart = VisitFilter.PeriodTerm.values()[visitFilter.periodEnd.ordinal - 1]
                    periodStartSpinner?.setSelection(periodStart.ordinal)
                }
                visitFilter.periodStart = periodStart
                refreshVisitList()
                visitFilter.save(context!!)
            }
        }
    }

    private fun initPeriodEndSpinner() {
        val endArray = periodTerms.subList(1, periodTerms.size)
        periodEndSpinner?.adapter = ArrayAdapter<String>(context!!, R.layout.simple_text_view, endArray)

        periodEndSpinner?.setSelection(visitFilter.periodEnd.ordinal - 1)

        periodEndSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var periodEnd = VisitFilter.PeriodTerm.values()[position + 1]
                if (periodEnd.ordinal <= visitFilter.periodStart.ordinal) {
                    periodEnd = VisitFilter.PeriodTerm.values()[visitFilter.periodStart.ordinal + 1]
                    periodEndSpinner?.setSelection(periodEnd.ordinal - 1)
                }
                visitFilter.periodEnd = periodEnd
                refreshVisitList()
                visitFilter.save(context!!)
            }
        }
    }

    private fun backToMapFragment() {

        mainActivity?.supportFragmentManager?.beginTransaction()
            ?.remove(this)
            ?.commit()

        onBackToMapFragment?.invoke()
    }

    private fun refreshVisitList() {

        refreshVisitsToShow()
        visitListView?.adapter = VisitListAdapter()
//        visitListView?.setHasFixedSize(false)
        refreshLoadingVisitsOverlay(show = false)
        refreshNoVisitsFrame(visitsToShow.isEmpty())
    }

    private fun onShowInWideMapInVisitDetail(visit: Visit) {
        backToMapFragment()
        mainActivity?.mapFragment?.animateToLatLng(visit.place.latLng)
    }

    private fun showMenuPopup() {

        val popup = PopupMenu(context, whereToGoMenuButton)
        popup.menuInflater.inflate(R.menu.where_to_go_fragment_menu, popup.menu)

        popup.menu.findItem(R.id.date_time_sort).title =
            if (sortByDescendingDateTime) getString(R.string.sort_by_ascending_date_time)
            else getString(R.string.sort_by_descending_date_time)

        popup.menu.findItem(R.id.rating_sort).title =
            if (sortByDescendingRating) getString(R.string.sort_by_ascending_rating)
            else getString(R.string.sort_by_descending_rating)

        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.date_time_sort -> {
                    sortByDescendingDateTime = !sortByDescendingDateTime
                }
                R.id.rating_sort -> {
                    sortByDescendingRating = !sortByDescendingRating
                }
            }
            refreshVisitList()
            saveSortConditions()
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun loadSortConditions() {
        context ?: return
        val prefs = context!!.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE)
        sortByDescendingDateTime = prefs.getBoolean(sortByDescendingDateTimeKey, true)
        sortByDescendingRating = prefs.getBoolean(sortByDescendingRatingKey, true)
    }

    private fun saveSortConditions() {
        context ?: return
        context!!.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE).edit()
            .putBoolean(sortByDescendingDateTimeKey, sortByDescendingDateTime)
            .putBoolean(sortByDescendingRatingKey, sortByDescendingRating)
            .apply()
    }

    private inner class VisitListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val cell = VisitCell(context!!).also {
                it.setOnClick { cell ->
                    VisitDetailDialog((cell as VisitCell).visit).also { dialog ->
                        dialog.onClickEditVisit = { visit2 ->
                            mainActivity?.showRecordVisitFragmentForEdit(visit2, this::onFinishEditVisit)
                        }
                        dialog.onDeleteVisitConfirmed = this::onVisitDeleted
                        dialog.onClickShowInWideMap = this@WhereToGoNextFragment::onShowInWideMapInVisitDetail
                    }.show(childFragmentManager, VisitDetailDialog::class.java.simpleName)
                }
            }
            return object :RecyclerView.ViewHolder(cell){}
        }

        override fun getItemCount(): Int {
            return visitsToShow.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val visitCell = holder.itemView as VisitCell
            visitCell.refresh(visitsToShow[position])
        }

        private fun onFinishEditVisit(visit: Visit, mode: EditMode, param: OnFinishEditParam) {

            // 編集後と削除後のWorkFragmentにおけるVisitCellの挙動
            when(param) {
                OnFinishEditParam.Canceled -> {}
                OnFinishEditParam.Done -> {

                    val pos = getPositionByVisit(visit)
                    if (pos >= 0) {
                        val cell = visitListView.findViewHolderForAdapterPosition(pos)?.itemView as? VisitCell
                        cell?.refresh(visit)
                    }

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

        private fun getPositionByVisit(visit: Visit): Int {
            for (i in 0 until visitsToShow.size) {
                if (visitsToShow[i] == visit) {
                    return i
                }
            }
            return -1
        }

        private fun onVisitDeleted(visit: Visit) {
            val pos = getPositionByVisit(visit)
            if (pos < 0) return

            val visitToRemove = visitsToShow[pos]
            visitsToShow.remove(visitToRemove)
            visits.remove(visitToRemove)

            notifyItemRemoved(pos)

            GlobalScope.launch {
                FirebaseDB.instance.deleteVisitAsync(visit)
            }
        }
    }
}
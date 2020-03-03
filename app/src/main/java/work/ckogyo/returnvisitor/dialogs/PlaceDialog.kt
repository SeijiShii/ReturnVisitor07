package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.place_dialog.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.VisitCell

class PlaceDialog(private val place: Place) :DialogFrameFragment() {

    private val handler = Handler()

    override fun onOkClick() {}

    var onRefreshPlace: ((Place) -> Unit)? = null

    var onClose: ((Place, OnFinishEditParam) -> Unit)? = null

    var onEditVisitInvoked: ((Visit) -> Unit)? = null
    var onRecordNewVisitInvoked: ((Place) -> Unit)? = null
    var onShowInWideMap: ((Visit) -> Unit)? = null

    private val visitsToPlace = ArrayList<Visit>()

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.place_dialog, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        closeButtonStyle = CloseButtonStyle.CloseOnly
        allowScroll = false
        allowResize = false

        super.onViewCreated(view, savedInstanceState)

        val titleId = when(place.category) {
            Place.Category.Place -> R.string.place
            Place.Category.House -> R.string.house
            Place.Category.HousingComplex -> R.string.housing_complex
            Place.Category.Room -> R.string.room
        }

        setTitle(titleId)
        addressText.text = place.toString(context!!)
        placeMenuButton.setOnClick {
            showMenuPopup()
        }

        recordVisitButton.setOnClickListener {
            close()
            onRecordNewVisitInvoked?.invoke(place)
        }

        recordNotHomeButton.setOnClickListener {
            addNotHomeVisit()
        }

        refreshColorMark()

        refreshVisitList()
    }

    private fun refreshColorMark() {
        colorMark?.setImageDrawable(ResourcesCompat.getDrawable(context!!.resources, ratingToColorButtonResId(place.rating), null))
    }

    private fun refreshVisitList() {

        loadingVisitsOfPlaceOverlay.fadeVisibility(true)
        visitListView.fadeVisibility(false)

        GlobalScope.launch {

            val loadedVisitsToPlace = VisitCollection.instance.loadVisitsOfPlace(place)
            mergeLoadedVisits(loadedVisitsToPlace)

            handler.post {

                visitListView ?: return@post

                visitListView.adapter = VisitListAdapter()
                visitListView.layoutManager?.scrollToPosition(visitsToPlace.size - 1)

                visitListView.fadeVisibility(true)
                loadingVisitsOfPlaceOverlay.fadeVisibility(false)
            }
        }
    }

    // 将来的には遅延ロードを考える
//    private fun refreshVisitList(isFirstLoading: Boolean) {
//
//        loadingVisitsOfPlaceProgress.fadeVisibility(true)
//        visitListView.fadeVisibility(false)
//
//        val manager = visitListView.layoutManager as LinearLayoutManager
//
//        val currTopVisit = if (isFirstLoading) {
//            null
//        } else {
//            val topPos = manager.findFirstVisibleItemPosition()
//            if (topPos >= 0) {
//                visitsToPlace[topPos]
//            } else {
//                null
//            }
//        }
//
//        GlobalScope.launch {
//
//            val loadedVisitsToPlace = VisitCollection.instance.loadVisitsOfPlace(place, limitLatest10 = isFirstLoading)
//            mergeLoadedVisits(loadedVisitsToPlace)
//
//            handler.post {
//
//                visitListView ?: return@post
//
//                visitListView.adapter = VisitListAdapter()
//
//                visitListView.fadeVisibility(true)
//                loadingVisitsOfPlaceProgress.fadeVisibility(false, addTouchBlockerOnFadeIn = true)
//
//                if (isFirstLoading) {
//                    visitListView.scrollToPosition(visitsToPlace.size - 1)
//                    refreshVisitList(isFirstLoading = false)
//                } else {
//
//                    currTopVisit ?: return@post
//
//                    val topPos = getPositionByVisit(currTopVisit)
//                    manager.scrollToPosition(topPos)
//                }
//            }
//        }
//    }

    private fun mergeLoadedVisits(visits: ArrayList<Visit>) {

        for (visit in visits) {
            if (!visitsToPlace.contains(visit)) {
                visitsToPlace.add(visit)
            }
        }
        visitsToPlace.sortBy { visit -> visit.dateTime.timeInMillis }

    }

    private fun showVisitDetailDialog(visit: Visit) {

        VisitDetailDialog(visit).also {
            it.onClickEditVisit = this@PlaceDialog::onClickEditVisitInCell
            it.onDeleteVisitConfirmed = this@PlaceDialog::onDeleteConfirmedInCell
            it.onClickShowInWideMap = this::onClickShowInWideMapInVisitDetailDialog
        }.show(childFragmentManager, VisitDetailDialog::class.java.simpleName)
    }

    private fun onClickShowInWideMapInVisitDetailDialog(visit: Visit) {
        close()
        onShowInWideMap?.invoke(visit)
    }

    private fun onClickEditVisitInCell(visit: Visit) {
        close()
        onEditVisitInvoked?.invoke(visit)
    }

    private fun onDeleteConfirmedInCell(visit: Visit) {

        val pos = getPositionByVisit(visit)
        if (pos < 0) return

        visitsToPlace.remove(visit)
        visitListView.adapter?.notifyItemRemoved(pos)

        val handler = Handler()
        GlobalScope.launch {
            VisitCollection.instance.deleteAsync(visit).await()

            MonthReportCollection.instance.updateByMonthAsync(visit.dateTime)
            place.refreshRatingByVisitsAsync().await()

            handler.post {
                onRefreshPlace?.invoke(place)
                refreshColorMark()
            }

            PlaceCollection.instance.saveAsync(place)
        }
    }

    private fun getPositionByVisit(visit: Visit): Int {

        for (i in 0 until visitsToPlace.size) {
            if (visit == visitsToPlace[i]) {
                return i
            }
        }
        return -1
    }

    private fun showMenuPopup() {

        val popup = PopupMenu(context, placeMenuButton)
        popup.menuInflater.inflate(R.menu.place_dialog_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.delete_place -> confirmDeletePlace()
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun confirmDeletePlace() {

        AlertDialog.Builder(context!!).setTitle(R.string.delete_place)
            .setMessage(R.string.delete_place_confirm)
            .setNegativeButton(R.string.cancel, null).
                setPositiveButton(R.string.delete){_, _ ->
                    onClose?.invoke(place, OnFinishEditParam.Deleted)
                    close()
                }.create().show()
    }

    private fun addNotHomeVisit() {

        // TODO: 留守宅の追加が遅すぎる件

        loadingVisitsOfPlaceOverlay.fadeVisibility(true, addTouchBlockerOnFadeIn = true)
        GlobalScope.launch {
            val nhVisit = VisitCollection.instance.addNotHomeVisitAsync(place)

            // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
            TimeCountIntentService.saveWorkIfActive()

            handler.post {

                visitsToPlace.add(nhVisit)
                visitListView?.adapter?.notifyItemInserted(visitsToPlace.size - 1)
                visitListView?.smoothScrollToPosition(visitsToPlace.size - 1)

                loadingVisitsOfPlaceOverlay.fadeVisibility(false)
            }

            place.refreshRatingByVisitsAsync().await()
            handler.post {
                refreshColorMark()
            }
        }
    }

    private inner class VisitListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(VisitCell(context!!).also {
                it.setOnClick { v ->
                    this@PlaceDialog.showVisitDetailDialog(it.visit)
                }

            }) {}
        }

        override fun getItemCount(): Int {
            return visitsToPlace.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as VisitCell).refresh(visitsToPlace[position])
        }
    }
}
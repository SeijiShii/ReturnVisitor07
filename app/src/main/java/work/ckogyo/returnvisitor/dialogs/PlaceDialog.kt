package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.place_dialog.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.VisitCell

class PlaceDialog(private val place: Place) :DialogFrameFragment() {

//    private val mainActivity: MainActivity?
//    get() = context as? MainActivity

    private val handler = Handler()

//    private val visitsToPlace = ArrayList<Visit>()

    override fun onOkClick() {}

    var onRefreshPlace: ((Place) -> Unit)? = null

    var onClose: ((Place, OnFinishEditParam) -> Unit)? = null

    var onEditVisitInvoked: ((Visit) -> Unit)? = null
    var onRecordNewVisitInvoked: ((Place) -> Unit)? = null

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.place_dialog, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        showCloseButtonOnly = true
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

        initVisitList()
    }

    private fun refreshColorMark() {
        colorMark.setImageDrawable(ResourcesCompat.getDrawable(context!!.resources, ratingToColorButtonResId(place.rating), null))
    }

    private fun initVisitList() {

        loadingVisitsOfPlaceProgress.fadeVisibility(true)

        GlobalScope.launch {

            val visits = VisitCollection.instance.loadVisitsOfPlace(place)

//            visitsToPlace.clear()
//            visitsToPlace.addAll(visits)

            handler.post {

                visitListContent ?: return@post

                visitListContent?.removeAllViews()
                for (visit in visits) {
                    addVisitCell(visit)
                }
                loadingVisitsOfPlaceProgress.fadeVisibility(false, addTouchBlockerOnFadeIn = true)
            }
        }
    }

    private fun addVisitCell(visit: Visit) {
        val visitCell = VisitCell(context!!, visit).also {
            it.onClickEditVisit = this::onClickEditVisitInCell
            it.onDeleteVisitConfirmed = this::onDeleteConfirmedInCell
            it.setOnClick { _ ->
                showVisitDetailDialog(it.visit)
            }
        }
        visitListContent.addView(visitCell)
    }

    private fun showVisitDetailDialog(visit: Visit) {

        VisitDetailDialog(visit).show(childFragmentManager, VisitDetailDialog::class.java.simpleName)
    }

    private fun onClickEditVisitInCell(visit: Visit) {
        close()
        onEditVisitInvoked?.invoke(visit)
    }

    private fun onDeleteConfirmedInCell(visit: Visit) {

        val handler = Handler()
        GlobalScope.launch {
            VisitCollection.instance.deleteAsync(visit).await()

            handler.post {
                onRefreshPlace?.invoke(place)
                refreshColorMark()
            }
        }
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

        GlobalScope.launch {
            val nhVisit = VisitCollection.instance.addNotHomeVisitAsync(place).await()

            // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
            TimeCountIntentService.saveWorkIfActive()

            handler.post {
                refreshColorMark()
                addVisitCell(nhVisit)
            }
        }
    }
}
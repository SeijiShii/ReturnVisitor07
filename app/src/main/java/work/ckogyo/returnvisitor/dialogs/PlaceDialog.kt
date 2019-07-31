package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.place_dialog.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.VisitCell

class PlaceDialog(private val place: Place) :DialogFrameFragment() {

    private val mainActivity: MainActivity?
    get() = context as? MainActivity

    private val visitsToPlace = ArrayList<Visit>()

    override fun onOkClick() {}

    var onRefreshPlace: ((Place) -> Unit)? = null

    var onClose: ((Place, OnFinishEditParam) -> Unit)? = null

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
        }

        setTitle(titleId)
        addressText.text = place.toString(context!!)
        placeMenuButton.setOnClick {
            showMenuPopup()
        }

        refreshColorMark()

        initVisitList()
    }

    private fun refreshColorMark() {
        colorMark.setImageDrawable(ResourcesCompat.getDrawable(context!!.resources, ratingToColorButtonResId(place.rating), null))
    }

    private fun initVisitList() {

        mainActivity?: return

        val handler = Handler()
        FirebaseHelper.loadVisitsOfPlace(mainActivity!!.dbRef.userDocument, place){

            visitsToPlace.clear()
            visitsToPlace.addAll(it)

            handler.post {

                visitListContent.removeAllViews()
                for (visit in it) {
                    val visitCell = VisitCell(context!!, visit)
                    visitCell.onClickEditVisit = this::onClickEditVisitInCell
                    visitCell.onDeleteVisitConfirmed = this::onDeleteConfirmedInCell
                    visitListContent.addView(visitCell)
                }
            }
        }
    }

    private fun onClickEditVisitInCell(visit: Visit) {

    }

    private fun onDeleteConfirmedInCell(visit: Visit) {

        mainActivity?:return

        mainActivity!!.dbRef.userDocument.collection(visitsKey).document(visit.id).delete()
        visitsToPlace.remove(visit)
        place.refreshRatingByVisits(visitsToPlace)

        mainActivity!!.dbRef.userDocument.collection(placesKey).document(place.id).set(place.hashMap)
        onRefreshPlace?.invoke(place)

        refreshColorMark()
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
                    mainActivity?.dbRef?.userDocument?.collection(placesKey)?.document(place.id)?.delete()
                        ?.addOnSuccessListener {
                            onClose?.invoke(place, OnFinishEditParam.Deleted)
                        }
                    close()
                }.create().show()
    }
}
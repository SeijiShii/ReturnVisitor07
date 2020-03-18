package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.visit_detail_dialog.*
import kotlinx.android.synthetic.main.visit_detail_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.SmallTagView

class VisitDetailDialog(private var visit: Visit? = null) : DialogFragment(), OnMapReadyCallback {

    var onClickEditVisit: ((Visit) -> Unit)? = null
    var onDeleteVisitConfirmed: ((Visit) -> Unit)? = null
    var onClickShowInWideMap: ((Visit) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val v = View.inflate(context, R.layout.visit_detail_dialog, null)

        if (visit != null) {
            v.placeText.text = visit!!.place.toString()

            v.addressText.text = visit!!.place.address
            v.personsText.text = visit!!.toPersonVisitString(context!!)

            v.priorityMark.setImageDrawable(ResourcesCompat.getDrawable(context!!.resources, ratingToColorButtonResId(visit!!.rating), null))
            v.priorityText.text = context!!.resources.getStringArray(R.array.raterArray)[visit!!.rating.ordinal]


            val plcTagViews = ArrayList<SmallTagView>()
            for (plc in visit!!.placements) {
                val tagView = SmallTagView(context!!, plc.toShortString(context!!)).also {
                    it.backgroundResourceId = R.drawable.border_round_dark_violet
                }
                plcTagViews.add(tagView)
            }
            v.placementTagContainer.addTagViews(plcTagViews)

            val infoTagViews = ArrayList<SmallTagView>()
            for (tag in visit!!.infoTags) {
                val tagView = SmallTagView(context!!, tag.name)
                infoTagViews.add(tagView)
            }
            v.tagContainer.addTagViews(infoTagViews)

            v.noteText.text = visit!!.description
        }

        v.visitDetailMenuButton.setOnClick {
            showMenuPopup()
        }

        v.mapView.onCreate(savedInstanceState)
        v.mapView.getMapAsync(this)

        v.showInMapButton.setOnClick {
            refreshMapFrame(true)
        }

        v.goBackToDetailButton.setOnClick {
            refreshMapFrame(false)
        }

        v.showInWideMapButton.setOnClick {
            // MapFragmentへ戻る。
            dismiss()

            visit ?: return@setOnClick

            onClickShowInWideMap?.invoke(visit!!)
        }

        return AlertDialog.Builder(context).also {

            it.setView(v)
            it.setNeutralButton(R.string.close, null)

        }.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visit ?: dismiss()
    }

    private var googleMap: GoogleMap? = null
    private var placeMarkers: PlaceMarkers? = null

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0

        googleMap ?: return

        googleMap!!.uiSettings?.isZoomControlsEnabled = true
        googleMap!!.uiSettings?.isZoomGesturesEnabled = true
        googleMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
        googleMap!!.uiSettings?.setAllGesturesEnabled(true)

        placeMarkers = PlaceMarkers(googleMap!!)

        if (visit != null) {
            placeMarkers!!.addMarker(context!!, visit!!.place)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(visit!!.place.latLng, 19f))
        }

        context ?: return
        googleMap?.setPadding(0, 0, 0, context!!.toDP(50))
    }


    private fun showMenuPopup() {

        val popup = PopupMenu(context!!, dialog.visitDetailMenuButton)
        popup.menuInflater.inflate(R.menu.visit_cell_menu, popup.menu)
        popup.setOnMenuItemClickListener {

            visit ?: return@setOnMenuItemClickListener true

            when(it.itemId) {
                R.id.edit_visit -> {
                    onClickEditVisit?.invoke(visit!!)
                }
                R.id.delete_visit -> {
                    confirmDeleteVisit(context!!, visit!!){
                        onDeleteVisitConfirmed?.invoke(visit!!)
                    }
                }
            }
            this.dismiss()
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    override fun onStart() {
        super.onStart()

        dialog?.mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()

        dialog?.mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()

        dialog?.mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        dialog?.mapView?.onDestroy()
    }

    private fun refreshMapFrame(show: Boolean) {

        if (show) {
            dialog ?: return
            context ?: return

            dialog!!.mapFrame?.layoutParams?.height = dialog!!.visitDetailScrollView.height + context!!.toDP(10)
        }

        dialog?.mapFrame?.fadeVisibility(show)
    }
}
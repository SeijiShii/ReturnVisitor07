package work.ckogyo.returnvisitor.fragments

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.map_fragment.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.PlaceDialog
import work.ckogyo.returnvisitor.dialogs.PlacePopup
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import kotlin.concurrent.thread


class MapFragment : Fragment(), OnMapReadyCallback {

    private val mainActivity: MainActivity?
    get() = context as? MainActivity
    lateinit var googleMap: GoogleMap
    private lateinit var placeMarkers: PlaceMarkers

    private val handler = Handler()

    var onSignOutConfirmed: (() -> Unit)? = null

    private val places = ArrayList<Place>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.map_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // https://stackoverflow.com/questions/35496493/getmapasync-in-fragment
        // getMapAsync() in Fragment
        mapView.onCreate(savedInstanceState)

        initLogoButton()
        initDrawerFrame()

    }

    override fun onStart() {
        super.onStart()

        waitForMapReadyAndShowMarkers()
    }

    override fun onMapReady(p0: GoogleMap?) {

        p0?:return
        googleMap = p0
        placeMarkers = PlaceMarkers(googleMap)

        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        mainActivity?.checkPermissionAndEnableMyLocation(googleMap)

        googleMap.setPadding(0, context!!.toDP(50), 0, context!!.toDP(50))

        googleMap.setOnMapLongClickListener {

            val place = Place()
            place.latLng = it
            place.category = Place.Category.House

            googleMap.animateCamera(CameraUpdateFactory.newLatLng(it))
            val marker = placeMarkers.addMarker(place)

            val popup = PlacePopup(context!!, place)
            popup.onCancel = {
                marker?.remove()
            }

            popup.onClickButton = this::onClickButtonInPlacePopup
            mapOuterFrame.addView(popup)
        }

        googleMap.setOnMarkerClickListener {

            val id = it.tag as? String
            if (id != null) {
                val place = getPlaceById(id)
                if (place != null) {
                    val dialog = PlaceDialog(place)
                    dialog.onClose = this::onClosePlaceDialog
                    dialog.onRefreshPlace = this::onRefreshPlaceInPlaceDialog
                    dialog.onEditVisitInvoked = this::onEditVisitInvokedInPlaceDialog
                    dialog.onRecordNewVisitInvoked = this::onRecordNewVisitInvokedInPlaceDialog
                    mainActivity?.showDialog(dialog)
                }
            }
            return@setOnMarkerClickListener true
        }

        loadCameraPosition()

        isMapReady = true

    }

    private var isMapReady = false
    private var markerShown = false

    fun waitForMapReadyAndShowMarkers() {

        // TODO: マーカーの再描画が確実でない。
        if (markerShown) return

        markerShown = true

        thread {
            while (!isMapReady || mainActivity == null || mainActivity!!.currentUser == null) {
                Thread.sleep(30)
            }

            loadPlaces {
                handler.post{
                    showPlaceMarkers()
                }
            }
        }
    }

    private fun onRecordNewVisitInvokedInPlaceDialog(place: Place) {
        mainActivity?.showRecordVisitFragmentForNew(place, this::onFinishEditVisit)
    }

    private fun onEditVisitInvokedInPlaceDialog(visit: Visit) {
        mainActivity?.showRecordVisitFragmentForEdit(visit, this::onFinishEditVisit)
    }

    private fun onRefreshPlaceInPlaceDialog(place: Place) {
        placeMarkers.refreshMarker(place)
    }

    private fun loadPlaces(onLoaded: () -> Unit) {

        val db = FirebaseDB.instance

        places.clear()
        db.loadPlaces {
            places.addAll(it)
            onLoaded()
        }
    }

    private fun onClosePlaceDialog(place: Place, param: OnFinishEditParam) {

        when(param) {
            OnFinishEditParam.Deleted -> {
                places.remove(place)
                placeMarkers.remove(place)
            }
        }

    }

    private fun onClickButtonInPlacePopup(place: Place) {
        mainActivity?.showRecordVisitFragmentForNew(place, this::onFinishEditVisit)
    }

    override fun onResume() {
        super.onResume()

        mapView.onResume()

        mapView.getMapAsync(this)
    }

    override fun onPause() {
        super.onPause()

        saveCameraPosition()
    }

    private fun saveCameraPosition() {
        mainActivity?:return

        val pos = googleMap.cameraPosition?:return

        val editor = mainActivity!!.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE).edit()
        editor.putFloat(zoomLevelKey, pos.zoom)
        editor.putString(latitudeKey, pos.target.latitude.toString())
        editor.putString(longitudeKey, pos.target.longitude.toString())
        editor.apply()
    }

    private fun loadCameraPosition() {

        mainActivity?:return

        val prefs = mainActivity!!.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE)

        val lat = (prefs.getString(latitudeKey, null)?.toDouble()?:0.0)
        val lng = (prefs.getString(longitudeKey, null)?.toDouble()?:0.0)

        val latLng = LatLng(lat, lng)
        val zoom = prefs.getFloat(zoomLevelKey, 3f)

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun showPlaceMarkers() {

        mainActivity?:return

        for (place in places) {
            placeMarkers.addMarker(place)
        }

        markerShown = true
    }

    private fun onFinishEditVisit(visit: Visit, mode: EditMode, param: OnFinishEditParam) {

        val db = FirebaseDB.instance

        when(param) {
            OnFinishEditParam.Canceled -> {
                when(mode) {
                    EditMode.Add -> {
                        placeMarkers.remove(visit.place)
                    }
                }
            }
            OnFinishEditParam.Done -> {

                placeMarkers.remove(visit.place)

                places.add(visit.place)

                val handler = Handler()

                db.loadVisitsOfPlace(visit.place){
                    it.add(visit)
                    visit.place.refreshRatingByVisits(it)
                    db.setPlace(visit.place)
                    handler.post {
                        placeMarkers.addMarker(visit.place)
                    }
                }

                db.setVisit(visit)

                for (person in visit.persons) {
                    db.setPerson(person)
                }
            }
            OnFinishEditParam.Deleted -> {

                mainActivity?:return

                placeMarkers.remove(visit.place)

                val handler = Handler()

                val place = visit.place
                db.deleteVisit(visit)
                db.loadVisitsOfPlace(place){
                    it.remove(visit)
                    place.refreshRatingByVisits(it)
                    db.setPlace(place)

                    handler.post {
                        placeMarkers.refreshMarker(place)
                    }
                }
            }
        }
    }

    private fun getPlaceById(id: String):Place? {
        for (place in places) {
            if (place.id == id) {
                return place
            }
        }
        return null
    }

    private var isDrawerOpen = false

    private val onDrawerSwipeListener = object : OnSwipeListener(){
        override fun onSwipe(v: View?, direction: Int) {

            if (direction and SWIPE_TO_LEFT == SWIPE_TO_LEFT && isDrawerOpen) {
                switchDrawer()
            }
        }
    }

    private fun initDrawerFrame() {

//        drawerFrame.setOnTouchListener { _, e ->
//            return@setOnTouchListener true
//        }

        drawerFrame.setOnTouchListener(onDrawerSwipeListener)

        initDrawerLogoButton()
        refreshDrawer()
        refreshDrawerOverlay()
        refreshSignOutButton()
        initTimeCountButton()
    }

    private fun refreshDrawer() {

        val w = context!!.toDP(200)

        if (drawerFrame.layoutParams == null) {
            drawerFrame.layoutParams = FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        (drawerFrame.layoutParams as FrameLayout.LayoutParams).leftMargin = if (isDrawerOpen) {
            0
        } else {
            -w
        }
    }

    private fun initLogoButton() {
        logoButton.setOnClick {
            switchDrawer()
        }
    }

    private fun initDrawerLogoButton() {
        drawerLogoButton.setOnClick {
            switchDrawer()
        }
    }

    private fun animateDrawer() {

        val target = if (isDrawerOpen) {
            0
        } else {
            -context!!.toDP(200)
        }

        val origin = (drawerFrame.layoutParams as FrameLayout.LayoutParams).leftMargin

        val animator = ValueAnimator.ofInt(origin, target)
        animator.addUpdateListener {
            (drawerFrame.layoutParams as FrameLayout.LayoutParams).leftMargin = it.animatedValue as Int
            drawerFrame.requestLayout()
        }
        animator.duration = 500
        animator.start()
    }

    private fun refreshDrawerOverlay() {

        drawerOverlay.alpha = if (isDrawerOpen) {
            drawerOverlay.setOnTouchListener(drawerOverlayTouchListener)
            1f
        } else {
            drawerOverlay.setOnTouchListener(null)
            0f
        }
    }

    private fun fadeDrawerOverlay() {

        val target = if (isDrawerOpen) {
            drawerOverlay.setOnTouchListener(drawerOverlayTouchListener)
            1f
        } else {
            drawerOverlay.setOnTouchListener(null)
            0f
        }

        val animator = ValueAnimator.ofFloat(drawerOverlay.alpha, target)
        animator.addUpdateListener {
            drawerOverlay.alpha = it.animatedValue as Float
            drawerOverlay.requestLayout()
        }
        animator.duration = 500
        animator.start()

    }

    private fun switchDrawer() {
        isDrawerOpen = !isDrawerOpen
        animateDrawer()
        fadeDrawerOverlay()
    }

    private val drawerOverlayTouchListener = View.OnTouchListener { _, _ ->
        switchDrawer()
        return@OnTouchListener true
    }

    private fun refreshSignOutButton() {

        signOutButton.text = if (mainActivity?.currentUser != null) {
            signOutButton.setOnClickListener {
                switchDrawer()
                confirmLogout()
            }
            context!!.resources.getString(R.string.logout_placeholder, mainActivity!!.currentUser!!.displayName)
        } else {
            signOutButton.setOnClickListener(null)
            context!!.resources.getString(R.string.not_logged_in)
        }
    }

    private fun confirmLogout() {

        AlertDialog.Builder(context!!)
            .setTitle(R.string.logout)
            .setMessage(context!!.resources.getString(R.string.logout_confirm, mainActivity!!.currentUser!!.displayName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout){ _, _ ->
                places.clear()
                placeMarkers.clear()
                markerShown = false
                onSignOutConfirmed?.invoke()
            }.create().show()
    }

    private fun initTimeCountButton(){
        timeCountButton.refreshCellHeight()
    }



}
package work.ckogyo.returnvisitor.fragments

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.map_fragment.*
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.PlaceDialog
import work.ckogyo.returnvisitor.dialogs.PlacePopup
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*


class MapFragment : Fragment(), OnMapReadyCallback {

    private val mainActivity: MainActivity?
    get() = context as? MainActivity
    lateinit var googleMap: GoogleMap
    private lateinit var placeMarkers: PlaceMarkers

    private val handler = Handler()

    var onSignOutConfirmed: (() -> Unit)? = null

//    private val places = ArrayList<Place>()

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

        GlobalScope.launch {
            waitForMapReadyAndShowMarkers()
        }
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
            popup.onClickNotHomeButton = this::onNotHomeRecorded
            mapOuterFrame.addView(popup)
        }

        googleMap.setOnMarkerClickListener {

            val id = it.tag as? String
            if (id != null) {
                GlobalScope.launch {
                    val place = PlaceCollection.instance.loadById(id)
                    if (place != null) {
                        when(place.category) {
                            Place.Category.Place,
                            Place.Category.House -> handler.post {
                                showPlaceDialog(place)
                            }
                            Place.Category.HousingComplex -> mainActivity?.showHousingComplexFragment(place,
                                onOk = this@MapFragment::onOkInHousingComplexFragment,
                                onDeleted = this@MapFragment::onDeletedInHousingComplexFragment,
                                onCancel = {
                                    placeMarkers.refreshMarker(place)
                                })
                        }
                    }
                }
            }
            return@setOnMarkerClickListener true
        }

        loadCameraPosition()

        isMapReady = true

    }

    private fun onOkInHousingComplexFragment(hComplex: Place) {
        handler.post {
            placeMarkers.refreshMarker(hComplex)
        }
    }

    private fun onDeletedInHousingComplexFragment(hComplex: Place) {
        handler.post {
            placeMarkers.remove(hComplex)
        }
    }

    private fun showPlaceDialog(place: Place) {
        val dialog = PlaceDialog(place)
        dialog.onClose = this::onClosePlaceDialog
        dialog.onRefreshPlace = this::onRefreshPlaceInPlaceDialog
        dialog.onEditVisitInvoked = this::onEditVisitInvokedInPlaceDialog
        dialog.onRecordNewVisitInvoked = this::onRecordNewVisitInvokedInPlaceDialog
        mainActivity?.showDialog(dialog)
    }

    private var isMapReady = false
    private var markerShown = false

    suspend fun waitForMapReadyAndShowMarkers() {

        // TODO: マーカーの再描画が確実でない。
        if (markerShown) return

        markerShown = true

        GlobalScope.launch {
            while (!isMapReady || mainActivity == null || mainActivity!!.currentUser == null) {
                delay(30)
            }

//            loadPlaces()
            handler.post {
                showPlaceMarkers()
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

//    private fun loadPlaces() {
//
//        runBlocking {
//            places.clear()
//            places.addAll(PlaceCollection.instance.loadPlacesForMap())
//        }
//    }

    private fun onClosePlaceDialog(place: Place, param: OnFinishEditParam) {

        when(param) {
            OnFinishEditParam.Deleted -> {
                GlobalScope.launch {
                    PlaceCollection.instance.deleteAsync(place).await()
                }
                placeMarkers.remove(place)
            }
        }

    }

    private fun onClickButtonInPlacePopup(place: Place) {

        placeMarkers.refreshMarker(place)

        when(place.category) {
            Place.Category.Place,
            Place.Category.House -> {
                mainActivity?.showRecordVisitFragmentForNew(place, this::onFinishEditVisit)
            }
            Place.Category.HousingComplex -> mainActivity?.showHousingComplexFragment(place,
                onOk = this::onOkInHousingComplexFragment,
                onDeleted = this::onDeletedInHousingComplexFragment,
                onCancel = {
                    // 新規追加した集合住宅ということ
                    placeMarkers.remove(it)
                })
        }

    }

    private fun onNotHomeRecorded(place: Place) {
        // TODO: 留守宅ボタンが押されたとき

        val visit = Visit()
        visit.place = place
        visit.rating = Visit.Rating.NotHome

        GlobalScope.launch {
            VisitCollection.instance.saveVisitAsync(visit).await()
            handler.post {
                placeMarkers.refreshMarker(place)
            }
        }

        // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
        TimeCountIntentService.saveWorkIfActive()
    }

    override fun onResume() {
        super.onResume()

        mapView.onResume()

        mapView.getMapAsync(this)
    }

    override fun onPause() {
        super.onPause()

        saveCameraPosition()
        googleMap.isMyLocationEnabled = false
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

        Log.d(debugTag, "showPlaceMarkers")

        GlobalScope.launch {
            val places = PlaceCollection.instance.loadPlacesForMap()
            handler.post{
                for (place in places) {
                    placeMarkers.addMarker(place)
                }
                markerShown = true
            }
        }
    }

    private fun onFinishEditVisit(visit: Visit, mode: EditMode, param: OnFinishEditParam) {

        when(param) {
            OnFinishEditParam.Canceled -> {
                when(mode) {
                    EditMode.Add -> {
                        placeMarkers.remove(visit.place)
                    }
                }
            }
            OnFinishEditParam.Done -> {

                mainActivity?.switchProgressOverlay(true, getString(R.string.updating))
                GlobalScope.launch {

                    VisitCollection.instance.saveVisitAsync(visit).await()
                    handler.post{
                        placeMarkers.refreshMarker(visit.place)
                        mainActivity?.switchProgressOverlay(false)
                    }

                    // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
                    TimeCountIntentService.saveWorkIfActive()
                }
            }
            OnFinishEditParam.Deleted -> {

                mainActivity?:return

                GlobalScope.launch {
                    VisitCollection.instance.deleteAsync(visit).await()
                    handler.post {
                        placeMarkers.refreshMarker(visit.place)
                    }
                }
            }
        }
    }

//    private fun getPlaceById(id: String):Place? {
//        for (place in places) {
//            if (place.id == id) {
//                return place
//            }
//        }
//        return null
//    }

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
        initWorkButton()
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
                placeMarkers.clear()
                markerShown = false
                onSignOutConfirmed?.invoke()
            }.create().show()
    }

    private fun initTimeCountButton(){
        timeCountButton.refreshCellHeight()
    }

    private fun initWorkButton() {
        workButton.setOnClickListener {
            switchDrawer()
            mainActivity?.showWorkFragment()
        }
    }



}
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
import com.google.android.gms.maps.model.Marker
import kotlinx.android.synthetic.main.map_fragment.*
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.AddWorkDialog
import work.ckogyo.returnvisitor.dialogs.PlaceDialog
import work.ckogyo.returnvisitor.dialogs.PlacePopup
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.latitudeKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.longitudeKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.returnVisitorPrefsKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.zoomLevelKey
import java.util.*


class MapFragment : Fragment(), OnMapReadyCallback {

    private val mainActivity: MainActivity?
    get() = context as? MainActivity
    lateinit var googleMap: GoogleMap
    private lateinit var placeMarkers: PlaceMarkers

    private val handler = Handler()

    var onSignOutConfirmed: (() -> Unit)? = null

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

        initHelpDialogButton()
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

        googleMap.setPadding(context!!.toDP(10), context!!.toDP(70), 0, context!!.toDP(50))

        googleMap.setOnMapLongClickListener(this::onMapLongClick)
        googleMap.setOnMarkerClickListener(this::onMarkerClick)
        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
            override fun onMarkerDragEnd(marker: Marker?) {
                marker ?: return
                val placeId = marker.tag as? String
                placeId ?: return

                val handler = Handler()

                GlobalScope.launch {
                    val place = FirebaseDB.instance.loadPlaceById(placeId)
                    place ?: return@launch

                    handler.post {
                        place.latLng = marker.position
                        context ?: return@post
                        GlobalScope.launch {
                            place.address = ""
                            place.address = requestAddressIfNeeded(place, context!!)
                            FirebaseDB.instance.savePlaceAsync(place)
                        }
                    }
                }
            }

            override fun onMarkerDragStart(p0: Marker?) {}

            override fun onMarkerDrag(p0: Marker?) {}
        })

        loadCameraPosition()

        isMapReady = true
    }

    private fun onMapLongClick(latLng: LatLng) {
        val place = Place().also {
            it.latLng = latLng
            it.category = Place.Category.House
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        val marker = placeMarkers.addMarker(context!!, place)

        showPlacePopup(place, marker)
    }

    private fun onMarkerClick(marker: Marker): Boolean {
        val id = marker.tag as? String
        if (id != null) {
            GlobalScope.launch {
                val place = FirebaseDB.instance.loadPlaceById(id)
                place ?: return@launch

                when(place.category) {
                    Place.Category.Place,
                    Place.Category.House -> handler.post {
                        showPlaceDialog(place)
                    }
                    Place.Category.HousingComplex -> mainActivity?.showHousingComplexFragment(place,
                        onDeleted = this@MapFragment::onDeletedInHousingComplexFragment,
                        onClose = this@MapFragment::onCloseHousingComplexFragment,
                        isNewHC = false)
                }
            }
        }
        return true
    }

    fun enableMyLocation(enabled: Boolean){
        googleMap.isMyLocationEnabled = enabled
    }

    private fun showPlacePopup(place: Place, marker: Marker?) {

        PlacePopup(context!!, place).also {
            it.onCancel = {
                marker?.remove()
            }
            it.onClickButton = this::onClickButtonInPlacePopup
            it.onClickNotHomeButton = this::onNotHomeRecorded
            mapOuterFrame.addView(it)
        }
    }

    private fun onDeletedInHousingComplexFragment(hComplex: Place) {
        handler.post {
            placeMarkers.remove(hComplex)
        }
    }

    private fun onCloseHousingComplexFragment(hComplex: Place, isNewHC: Boolean) {

        GlobalScope.launch {

            val db = FirebaseDB.instance
            if (db.housingComplexHasRooms(hComplex.id)) {
                db.savePlaceAsync(hComplex).await()
                handler.post {
                    placeMarkers.refreshMarker(context!!, hComplex)
                }
            } else {
                if (isNewHC) {
                    handler.post {
                        placeMarkers.remove(hComplex)
                    }
                    db.deletePlaceAsync(hComplex)
                } else {
                    db.savePlaceAsync(hComplex).await()
                    val rating = hComplex.rating
                    handler.post{
                        hComplex.rating = rating
                        placeMarkers.refreshMarker(context!!, hComplex)
                    }
                }
            }
        }
    }

    private fun showPlaceDialog(place: Place) {
        val dialog = PlaceDialog(place).apply {
            onClose = this@MapFragment::onClosePlaceDialog
            onRefreshPlace = this@MapFragment::onRefreshPlaceInPlaceDialog
            onEditVisitInvoked = this@MapFragment::onEditVisitInvokedInPlaceDialog
            onRecordNewVisitInvoked = this@MapFragment::onRecordNewVisitInvokedInPlaceDialog
            onShowInWideMap = {
                animateToLatLng(it.place.latLng)
            }
        }
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

        val handler = Handler()
        GlobalScope.launch {
            val db = FirebaseDB.instance
            db.savePlaceAsync(place).await()

            handler.post{
                placeMarkers.refreshMarker(context!!, place)
            }
        }
    }

    private fun onClosePlaceDialog(place: Place, param: OnFinishEditParam) {

        when(param) {
            OnFinishEditParam.Deleted -> {
                GlobalScope.launch {
                    FirebaseDB.instance.deletePlaceAsync(place)
                }
                placeMarkers.remove(place)
            }
        }

    }

    private fun onClickButtonInPlacePopup(place: Place) {

        placeMarkers.refreshMarker(context!!, place)

        when(place.category) {
            Place.Category.Place,
            Place.Category.House -> {
                mainActivity?.showRecordVisitFragmentForNew(place, this::onFinishEditVisit)
            }
            Place.Category.HousingComplex -> mainActivity?.showHousingComplexFragment(place,
                onDeleted = this::onDeletedInHousingComplexFragment,
                onClose = this::onCloseHousingComplexFragment,
                isNewHC = true)
        }

    }

    private fun onNotHomeRecorded(place: Place) {
        // TODO: 留守宅ボタンが押されたとき

        val visit = Visit()
        visit.place = place
        visit.rating = Visit.Rating.NotHome

        GlobalScope.launch {

            visit.place.address = requestAddressIfNeeded(visit.place, context!!)

            FirebaseDB.instance.saveVisitAsync(visit).await()
            handler.post {
                placeMarkers.refreshMarker(context!!, visit.place)
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

        mainActivity!!.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE).edit()
            .apply {
                putFloat(zoomLevelKey, pos.zoom)
                putString(latitudeKey, pos.target.latitude.toString())
                putString(longitudeKey, pos.target.longitude.toString())
                apply()
            }
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

//        Log.d(debugTag, "showPlaceMarkers")

        GlobalScope.launch {
            val places = FirebaseDB.instance.loadPlacesForMap()
            handler.post{
                for (place in places) {
                    placeMarkers.addMarker(context!!, place)
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

                    FirebaseDB.instance.saveVisitAsync(visit).await()
                    handler.post{
                        placeMarkers.refreshMarker(context!!, visit.place)
                        mainActivity?.switchProgressOverlay(false)
                    }

                    // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
                    TimeCountIntentService.saveWorkIfActive()
                }
            }
            OnFinishEditParam.Deleted -> {

                mainActivity?:return

                GlobalScope.launch {
                    FirebaseDB.instance.deleteVisitAsync(visit).await()
                    handler.post {
                        placeMarkers.refreshMarker(context!!, visit.place)
                    }
                }
            }
        }
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

        drawerFrame.setOnTouchListener(onDrawerSwipeListener)

        initDrawerLogoButton()
        refreshDrawer()
        refreshDrawerOverlay()
        refreshSignOutButton()
        initTimeCountButton()
        initWorkButton()
        initAddWorkButton()
        initShowCalendarButton()
        initMonthReportButton()
        initMailReportButton()
        initWhereToGoNextButton()
        initTermOfUseButton()
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

    fun refreshSignOutButton() {

        signOutButton.text = if (mainActivity?.currentUser != null) {
            signOutButton.setOnClickListener {
                switchDrawer()
                confirmLogout()
            }

            if (mainActivity!!.currentUser!!.isAnonymous) {
                context!!.resources.getString(R.string.logout_from_no_login)
            } else {
                val displayName = mainActivity!!.currentUser!!.displayName
                context!!.resources.getString(R.string.logout_placeholder, displayName)
            }
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
        timeCountButton.refreshHeight()
    }

    private fun initWorkButton() {
        workButton.setOnClickListener {
            switchDrawer()
            mainActivity?.showWorkFragment(Calendar.getInstance())
        }
    }

    private fun initAddWorkButton() {
        addWorkButton.setOnClickListener {

            switchDrawer()

            val fm = mainActivity?.supportFragmentManager
            fm ?: return@setOnClickListener

            AddWorkDialog().apply {
                onWorkAdded = {work ->
                    mainActivity?.showWorkFragment(work.start)
                }
            }.show(fm, AddWorkDialog::class.java.simpleName)
        }
    }

    private fun initShowCalendarButton() {

        showCalendarButton.setOnClickListener {

            switchDrawer()
            mainActivity?.showCalendarPagerFragment(Calendar.getInstance())
        }
    }

    private fun initMonthReportButton() {

        monthReportButton.setOnClickListener {

            switchDrawer()
            mainActivity?.showMonthReportDialog(Calendar.getInstance())
        }
    }

    private fun initMailReportButton() {

        reportMailButton.setOnClickListener {

            switchDrawer()
            mainActivity?.prepareReportMail(Calendar.getInstance())
        }
    }

    private fun initWhereToGoNextButton() {

        whereToGoNextButton.setOnClickListener {

            switchDrawer()
            mainActivity?.showWhereToGoNextFragment()
        }
    }


    private fun initTermOfUseButton() {

        termOfUseButton.setOnClickListener {

            switchDrawer()
            mainActivity?.showTermOfUseDialog()
        }
    }

    private fun initHelpDialogButton() {
        mapHelpButton.setOnClick {
            mainActivity?.showTextPopupDialog(mapHelpButton, R.string.map_help_description)
        }
    }

    fun onFinishEditVisitInFragments(visit: Visit, param: OnFinishEditParam) {

        val db = FirebaseDB.instance
        when(param) {
            OnFinishEditParam.Canceled -> {}
            OnFinishEditParam.Done -> {
                GlobalScope.launch {
                    db.saveVisitAsync(visit).await()
                    // handler.post待ちしている間になぜかRatingが空き家になるが原因究明は断念し、キャッシュして再代入というズルをする。
                    val rating = visit.place.rating
                    handler.post {
                        visit.place.rating = rating
                        Log.d(debugTag, "onFinishEditVisitInFragments, visit.place.rating = rating : ${visit.place.rating}")
                        placeMarkers.refreshMarker(context!!, visit.place)
                    }
                }
            }
            OnFinishEditParam.Deleted -> {
                GlobalScope.launch {
                    db.deleteVisitAsync(visit).await()
                    // handler.post待ちしている間になぜかRatingが空き家になるが原因究明は断念し、キャッシュして再代入というズルをする。
                    val rating = visit.place.rating
                    handler.post {
                        visit.place.rating = rating
                        Log.d(debugTag, "onFinishEditVisitInFragments, visit.place.rating = rating : ${visit.place.rating}")
                        placeMarkers.refreshMarker(context!!, visit.place)
                    }
                }
            }
        }
    }

    fun animateToLatLng(latLng: LatLng) {

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 19f))
    }

}
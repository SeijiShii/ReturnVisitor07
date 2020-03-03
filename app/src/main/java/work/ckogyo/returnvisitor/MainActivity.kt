package work.ckogyo.returnvisitor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.dialogs.*
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.fragments.*
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        var isAppVisible = false
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    lateinit var mapFragment: MapFragment
        private set

    val googleMap: GoogleMap
        get() = mapFragment.googleMap

    private val handler = Handler()

    val currentUser: FirebaseUser?
    get() = auth.currentUser

    val isLoggedIn : Boolean
    get() = auth.currentUser != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        supportActionBar?.hide()

        MobileAds.initialize(this, getString(R.string.admob_id))
        val adRequest = AdRequest.Builder()
            .addTestDevice("B3596FE6869B7E16CC393BDD40A1ED72")
            .build()
        adView.loadAd(adRequest)

        mapFragment = MapFragment()
        mapFragment.onSignOutConfirmed = this::onSignOutConfirmedInMapFragment
        showMapFragment()

        initGoogleSignIn()
        FirebaseDB.initialize(auth)

    }

    private fun initGoogleSignIn() {

        // [START config_signin]
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        // [END config_signin]

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // [END initialize_auth]

    }

    override fun onStart() {
        super.onStart()
        showLoginDialogIfNeeded()
        switchProgressOverlay(false)
        isAppVisible = true
        watchAndAdjustAdView()
        restartTimeCountIfNeeded()
    }

    override fun onStop() {
        super.onStop()

        isAppVisible = false
        isWatchingForAdView = false
    }

    private fun showMapFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, mapFragment, MapFragment::class.java.simpleName)
        transaction.commit()
    }

    private fun onSignOutConfirmedInMapFragment() {
        signOut()
    }

    fun showRecordVisitFragmentForNew(place: Place, onFinishEditVisit: (Visit, EditMode, OnFinishEditParam) -> Unit) {

        // 訪問は初めてだが、過去に訪問したことのある場所であれば過去のデータを元に下準備する
        switchProgressOverlay(true, getString(R.string.preparing_visit))
        val handler = Handler()
        GlobalScope.launch {
            val visit = VisitCollection.instance.prepareNextVisit(place)

            handler.post {
                val rvFragment = RecordVisitFragment().also {
                    it.visit = visit ?: Visit(place)
                    it.onFinishEdit = onFinishEditVisit
                    it.mode = EditMode.Add
                    it.onBackToMapFragment = {
                        mapFragment.enableMyLocation(true)
                    }
                }

                supportFragmentManager.beginTransaction().also {
                    it.addToBackStack(null)
                    it.add(R.id.fragmentContainer, rvFragment, RecordVisitFragment::class.java.simpleName)
                    it.commit()
                }
                switchProgressOverlay(false)
            }
        }

        handler.post {
            hideKeyboard(this)
            mapFragment.enableMyLocation(false)
        }
    }

    fun showRecordVisitFragmentForEdit(visit: Visit, onFinishEditVisit: (Visit, EditMode, OnFinishEditParam) -> Unit) {

        val rvFragment = RecordVisitFragment().also {
            it.visit = visit
            it.onFinishEdit = onFinishEditVisit
            it.mode = EditMode.Edit
            it.onBackToMapFragment = {
                handler.post {
                    hideKeyboard(this)
                    mapFragment.enableMyLocation(true)
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .add(R.id.fragmentContainer, rvFragment, RecordVisitFragment::class.java.simpleName)
            .commit()

        handler.post {
            hideKeyboard(this)
            mapFragment.enableMyLocation(false)
        }
    }

    // 新規集合住宅を追加後、「閉じる」の場合はキャンセルとみなし、部屋が1つもなければ集合住宅を削除する
    fun showHousingComplexFragment(hComplex: Place,
                                   onOk: (hComplex: Place) -> Unit,
                                   onDeleted: (hComplex: Place) -> Unit,
                                   onClose: (hComplex: Place, isNewHC: Boolean) -> Unit,
                                   isNewHC: Boolean) {
        val hcFragment = HousingComplexFragment().also {
            it.hComplex = hComplex
            it.onOk = onOk
            it.onClose = onClose
            it.onDeleted = onDeleted
            it.isNewHC = isNewHC
            it.onBackToMapFragment = {
                handler.post {
                    hideKeyboard(this)
                    mapFragment.enableMyLocation(true)
                }
            }
        }

        supportFragmentManager.beginTransaction().let {
            it.addToBackStack(null)
            it.add(R.id.fragmentContainer, hcFragment, HousingComplexFragment::class.java.simpleName)
            it.commit()
        }

        handler.post {
            hideKeyboard(this)
            mapFragment.enableMyLocation(false)
        }
    }

    fun showWorkFragment(dateToShow: Calendar) {

//        switchProgressOverlay(true, getString(R.string.loading_works))

        val workFragment = WorkFragment(dateToShow).also {
            it.onVisitEdited = mapFragment::onFinishEditVisitInFragments
            it.onBackToMapFragment = {
                handler.post {
                    hideKeyboard(this)
                    mapFragment.enableMyLocation(true)
                }
            }
        }
        supportFragmentManager.beginTransaction().let {
            it.addToBackStack(null)
            it.add(R.id.fragmentContainer, workFragment, WorkFragment::class.java.simpleName)
            it.commit()
        }

        handler.post {
            hideKeyboard(this)
            mapFragment.enableMyLocation(false)
        }
    }

    fun showCalendarPagerFragment(monthToShow: Calendar) {

        val cpFragment = CalendarPagerFragment(monthToShow).also {
            it.onBackToMapFragment = {
                handler.post {
                    hideKeyboard(this)
                    mapFragment.enableMyLocation(true)
                }
            }
        }
        supportFragmentManager.beginTransaction().let {
            it.addToBackStack(null)
            it.add(R.id.fragmentContainer, cpFragment, CalendarPagerFragment::class.java.simpleName)
            it.commit()
        }

        handler.post {
            hideKeyboard(this)
            mapFragment.enableMyLocation(false)
        }
    }

    fun showWhereToGoNextFragment() {
        val goFragment = WhereToGoNextFragment().also {
            it.onVisitEdited = mapFragment::onFinishEditVisitInFragments
            it.onBackToMapFragment = {
                handler.post {
                    hideKeyboard(this)
                    mapFragment.enableMyLocation(true)
                }
            }
        }
        supportFragmentManager.beginTransaction().let {
            it.addToBackStack(null)
            it.add(R.id.fragmentContainer, goFragment, WhereToGoNextFragment::class.java.simpleName)
            it.commit()
        }
        handler.post {
            hideKeyboard(this)
            mapFragment.enableMyLocation(false)
        }
    }


    fun checkPermissionAndEnableMyLocation(googleMap: GoogleMap?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                requestPermissionCode
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestPermissionCode) {
            if (permissions.isNotEmpty()) {
                for (p in permissions) {
                    if (p == Manifest.permission.ACCESS_COARSE_LOCATION || p == Manifest.permission.ACCESS_FINE_LOCATION) {
                        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment::class.java.simpleName) as? MapFragment
                        try {
                            mapFragment?.googleMap?.isMyLocationEnabled = true
                        } catch (e: SecurityException) {

                        }
                    }
                }
            }
        }
    }

    fun showDialog(dialog: DialogFrameFragment) {

        dialog.show(R.id.appFrame, supportFragmentManager, dialog.javaClass.simpleName)
        hideKeyboard(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data?:return

        when(requestCode) {
            googleSingInRequestCode -> {

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)

                    onSignedIn()

                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(debugTag, "Google sign in failed", e)
                    Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show()
                    showLoginDialogIfNeeded()
                    mapFragment.refreshSignOutButton()
                }
            }
        }
    }


    // [START auth_with_google]
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(debugTag, "firebaseAuthWithGoogle:" + acct.id!!)
        // [START_EXCLUDE silent]

        // [END_EXCLUDE]

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(debugTag, "signInWithCredential:success")
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(debugTag, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show()
                }

                // [START_EXCLUDE]

                // [END_EXCLUDE]
            }
    }
    // [END auth_with_google]

    // [START signin]
    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, googleSingInRequestCode)
    }
    // [END signin]

    fun signInAnonymously() {
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.d(debugTag, "signInAnonymously:success")
                onSignedIn()
            }
            .addOnFailureListener {
                mapFragment.refreshSignOutButton()
                showLoginDialogIfNeeded()
            }
    }

    private fun onSignedIn() {

        loginDialog.close()

        GlobalScope.launch {
            mapFragment.waitForMapReadyAndShowMarkers()

            while (auth.currentUser == null) {
                delay(50)
            }

            handler.post {
                mapFragment.refreshSignOutButton()
            }
        }
    }


    private fun signOut() {
        // Firebase sign out
        auth.signOut()

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            showLoginDialogIfNeeded()
        }
    }

    private fun revokeAccess() {
        // Firebase sign out
        auth.signOut()

        // Google revoke access
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {
            showLoginDialogIfNeeded()
        }
    }


    private var isWatchingForAdView = false
    private fun watchAndAdjustAdView() {
        isWatchingForAdView = true
        val handler = Handler()
        var oldShow = false
        GlobalScope.launch {
            while (isWatchingForAdView) {
                delay(50)
//                Log.d(debugTag, "appFrame.height: ${appFrame.height}")
                val show = appFrame.height >= toDP(500)
                if (show != oldShow) {
                    handler.post {
                        oldShow = show
                        adOuterFrame.fadeVisibility(show, addTouchBlockerOnFadeIn = true)
                    }
                }
            }
        }
    }

    fun switchProgressOverlay(show: Boolean, message: String = "") {
        progressOverlay.fadeVisibility(show, addTouchBlockerOnFadeIn = true)
        progressMessage.text = if (show) {
             message
        } else {
            ""
        }
    }

    fun showMonthReportDialog(month: Calendar) {

        MonthReportDialog(month).also {
            it.show(supportFragmentManager, MonthReportDialog::class.java.simpleName)
        }
    }

    fun prepareReportMail(month: Calendar) {

        switchProgressOverlay(true, getString(R.string.preparing_report_mail))

        val handler = Handler()
        GlobalScope.launch {

            Log.d(debugTag, "prepareReportMail: ${month.toMonthTitleString(this@MainActivity)}")
//            MonthReportCollection.instance.updateByMonthAsync(month).await()
            val report = MonthReportCollection.instance.loadByMonth(month)
            handler.post {
                switchProgressOverlay(false)
                exportToMail(this@MainActivity, report)
            }

        }
    }

    private lateinit var loginDialog: LoginDialog
    private fun showLoginDialogIfNeeded() {
        if (auth.currentUser == null) {
            loginDialog = LoginDialog()
            loginDialog.show(R.id.appFrame, supportFragmentManager, LoginDialog::class.java.simpleName)
        }
    }

    private fun restartTimeCountIfNeeded() {

        val prefs = getSharedPreferences(SharedPrefKeys.returnVisitorPrefsKey, Context.MODE_PRIVATE)
        val isCounting = prefs.getBoolean(SharedPrefKeys.isTimeCounting, false)
        if (isCounting) {

            if (TimeCountIntentService.isTimeCounting) {
                return
            }

            val workId = prefs.getString(TimeCountIntentService.timeCountingWorkId, null)

            workId ?: return

            Intent(this, TimeCountIntentService::class.java).also {
                it.action = TimeCountIntentService.restartCountingToService
                it.putExtra(TimeCountIntentService.timeCountingWorkId, workId)
                startService(it)
            }
        }
    }

    fun showTextPopupDialog(anchor: View, textId: Int) {

        TextPopupDialog(anchor, R.id.appFrame).show(supportFragmentManager, textId)
    }

    fun showTermOfUseDialog() {
        WebViewDialog(getString(R.string.term_of_use_url)).show(R.id.appFrame, supportFragmentManager, WebViewDialog::class.java.simpleName)
    }

}
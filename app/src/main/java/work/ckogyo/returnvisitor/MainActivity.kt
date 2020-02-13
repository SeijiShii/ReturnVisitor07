package work.ckogyo.returnvisitor

import android.Manifest
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.dialogs.DialogFrameFragment
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.fragments.HousingComplexFragment
import work.ckogyo.returnvisitor.fragments.MapFragment
import work.ckogyo.returnvisitor.fragments.RecordVisitFragment
import work.ckogyo.returnvisitor.fragments.WorkFragment
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        var isAppVisible = false
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var mapFragment: MapFragment
    private val handler = Handler()

    val currentUser: FirebaseUser?
    get() = auth.currentUser

    val isLoggedIn : Boolean
    get() = auth.currentUser != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        supportActionBar?.hide()

        mapFragment = MapFragment()
        mapFragment.onSignOutConfirmed = this::onSignOutConfirmedInMapFragment
        showMapFragment()

        initGoogleSignIn()
        FirebaseDB.initialize(auth)

    }

    private fun initGoogleSignIn() {

        googleSignInButton.setOnClickListener {
            signIn()
        }

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
        refreshLoginOverlay()
        switchProgressOverlay(false)
        isAppVisible = true
        watchAndAdjustAdView()
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
                }

                supportFragmentManager.beginTransaction().also {
                    it.addToBackStack(null)
                    it.add(R.id.fragmentContainer, rvFragment, RecordVisitFragment::class.java.simpleName)
                    it.commit()
                }
                switchProgressOverlay(false)
            }
        }
        hideKeyboard(this)
    }

    fun showRecordVisitFragmentForEdit(visit: Visit, onFinishEditVisit: (Visit, EditMode, OnFinishEditParam) -> Unit) {
        val transaction = supportFragmentManager.beginTransaction()
        val rvFragment = RecordVisitFragment()
        rvFragment.visit = visit
        rvFragment.onFinishEdit = onFinishEditVisit
        rvFragment.mode = EditMode.Edit
        transaction.addToBackStack(null)
        transaction.add(R.id.fragmentContainer, rvFragment, RecordVisitFragment::class.java.simpleName)
        transaction.commit()
        hideKeyboard(this)
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
        }

        supportFragmentManager.beginTransaction().let {
            it.addToBackStack(null)
            it.add(R.id.fragmentContainer, hcFragment, HousingComplexFragment::class.java.simpleName)
            it.commit()
        }
        hideKeyboard(this)
    }

    fun showWorkFragment(dateToShow: Calendar) {

//        switchProgressOverlay(true, getString(R.string.loading_works))

        val transaction = supportFragmentManager.beginTransaction()
        val workFragment = WorkFragment(dateToShow)
        transaction.addToBackStack(null)
        transaction.add(R.id.fragmentContainer, workFragment, WorkFragment::class.java.simpleName)
        transaction.commit()

//        switchProgressOverlay(false)


//        GlobalScope.launch {
//
//            handler.post{
//
//            }
//
//            val elmList = WorkElmList.instance
//
//            val latestDateElms = elmList.getListOfToday()
//            if (latestDateElms == null) {
//                handler.post {
//                    Toast.makeText(this@MainActivity, R.string.no_data_recoreded, Toast.LENGTH_SHORT).show()
//                }
//            } else {
//
//                val date = WorkElmList.getDate(latestDateElms)!!
////                val previousDateElms = elmList.getListOfNeighboringDate(date, true)
////                val nextDateElms = elmList.getListOfNeighboringDate(date, false)
////
////                var merged = ArrayList<WorkElement>(latestDateElms)
////                if (previousDateElms != null) {
////                    merged = WorkElmList.mergeAvoidingDup(merged, previousDateElms)
////                }
////
////                if (nextDateElms != null) {
////                    merged = WorkElmList.mergeAvoidingDup(merged, nextDateElms)
////                }
//
//                WorkElmList.refreshIsVisitInWork(latestDateElms)
//
//
//            }
//        }
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

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.appFrame, dialog, DialogFrameFragment::class.java.simpleName)
        transaction.addToBackStack(null)
        transaction.commit()

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
                    GlobalScope.launch {
                        mapFragment.waitForMapReadyAndShowMarkers()
                    }
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(debugTag, "Google sign in failed", e)
                    Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show()
//                    refreshGoogleSignInButton(null)
                    refreshLoginOverlay()
                }
            }
        }
    }


    // [START auth_with_google]
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(debugTag, "firebaseAuthWithGoogle:" + acct.id!!)
        // [START_EXCLUDE silent]
//        showProgressDialog()
        // [END_EXCLUDE]

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(debugTag, "signInWithCredential:success")
                    loginOverlay.fadeVisibility(!isLoggedIn, addTouchBlockerOnFadeIn = true)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(debugTag, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show()
                }

                // [START_EXCLUDE]
//                hideProgressDialog()
                // [END_EXCLUDE]
            }
    }
    // [END auth_with_google]

    // [START signin]
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, googleSingInRequestCode)
    }
    // [END signin]


    private fun signOut() {
        // Firebase sign out
        auth.signOut()

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            loginOverlay.fadeVisibility(!isLoggedIn, addTouchBlockerOnFadeIn = true)
        }
    }

    private fun revokeAccess() {
        // Firebase sign out
        auth.signOut()

        // Google revoke access
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {
//            refreshGoogleSignInButton(null)
            loginOverlay.fadeVisibility(!isLoggedIn, addTouchBlockerOnFadeIn = true)
        }
    }

    private fun refreshLoginOverlay() {
        loginOverlay.alpha = if (auth.currentUser == null) {
            loginOverlay.setOnTouchListener { _, _ -> return@setOnTouchListener true }
            loginOverlay.visibility = View.VISIBLE
            1f
        } else {
            loginOverlay.setOnTouchListener(null)
            loginOverlay.visibility = View.GONE
            0f
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

}
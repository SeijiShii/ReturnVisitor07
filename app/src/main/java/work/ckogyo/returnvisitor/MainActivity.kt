package work.ckogyo.returnvisitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.dialogs.DialogFrameFragment
import work.ckogyo.returnvisitor.fragments.MapFragment
import work.ckogyo.returnvisitor.fragments.RecordVisitFragment
import work.ckogyo.returnvisitor.fragments.WorkFragment
import work.ckogyo.returnvisitor.fragments.WorkListElm
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    companion object {
        var isAppVisible = false
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var mapFragment: MapFragment

    val currentUser: FirebaseUser?
    get() = auth.currentUser

    val isLoggedIn : Boolean
    get() = auth.currentUser != null

//    lateinit var db: FirebaseDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        supportActionBar?.hide()

        mapFragment = MapFragment()
        mapFragment.onSignOutConfirmed = this::onSignOutConfirmedInMapFragment
        showMapFragment()

        initGoogleSignIn()
        FirebaseDB.initialize(auth)

//        db = FirebaseDB(auth)

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
        isAppVisible = true
    }

    override fun onStop() {
        super.onStop()

        isAppVisible = false
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
        val transaction = supportFragmentManager.beginTransaction()
        val rvFragment = RecordVisitFragment()
        rvFragment.visit.place = place
        rvFragment.onFinishEdit = onFinishEditVisit
        rvFragment.mode = EditMode.Add
        transaction.addToBackStack(null)
        transaction.add(R.id.fragmentContainer, rvFragment, RecordVisitFragment::class.java.simpleName)
        transaction.commit()
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
    }

    fun showWorkFragment() {
        val db = FirebaseDB.instance
        GlobalScope.launch {
            val startDate = Calendar.getInstance()
            startDate.add(Calendar.DAY_OF_MONTH, -1)

            val endDate = Calendar.getInstance()
            endDate.add(Calendar.DAY_OF_MONTH, 1)

            val works = db.loadWorksByDateRange(startDate, endDate)
//            Log.d(debugTag, works.toString())

            val dataElms = ArrayList<WorkListElm>()
            for (work in works) {
                dataElms.addAll(WorkListElm.fromWork(work))
            }

            dataElms.sortBy { elm -> elm.dateTime.timeInMillis }

            val transaction = supportFragmentManager.beginTransaction()
            val workFragment = WorkFragment(dataElms)
            transaction.addToBackStack(null)
            transaction.add(R.id.fragmentContainer, workFragment, WorkFragment::class.java.simpleName)
            transaction.commit()

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
                    mapFragment.waitForMapReadyAndShowMarkers()
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
                    loginOverlay.fadeVisibility(!isLoggedIn)
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
            loginOverlay.fadeVisibility(!isLoggedIn)
        }
    }

    private fun revokeAccess() {
        // Firebase sign out
        auth.signOut()

        // Google revoke access
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {
//            refreshGoogleSignInButton(null)
            loginOverlay.fadeVisibility(!isLoggedIn)
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

}
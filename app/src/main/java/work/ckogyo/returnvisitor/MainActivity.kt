package work.ckogyo.returnvisitor

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.main_activity.*
import work.ckogyo.returnvisitor.dialogs.DialogFrameFragment
import work.ckogyo.returnvisitor.fragments.MapFragment
import work.ckogyo.returnvisitor.fragments.RecordVisitFragment
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*

class MainActivity : AppCompatActivity() {

    companion object {
        var isAppVisible = false
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var mapFragment: MapFragment

    val currentUser: FirebaseUser?
    get() = auth.currentUser

    lateinit var db: FirebaseDBWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        supportActionBar?.hide()

        mapFragment = MapFragment()
        mapFragment.onSignOutConfirmed = this::onSignOutConfirmedInMapFragment
        showMapFragment()

        initGoogleSignIn()

        db = FirebaseDBWrapper(auth)

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

//        auth.addAuthStateListener {
//            if (it.currentUser != null) {
//                mapFragment.waitForMapReadyAndShowMarkers()
//            } else {
//
//            }
//        }
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

//    private fun refreshGoogleSignInButton(user: FirebaseUser?) {
//        googleSignInButton.text = if (user != null) {
//            getString(R.string.google_sign_out, user.displayName)
//        } else {
//            getString(R.string.google_sign_in)
//        }
//    }

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
//                    val user = auth.currentUser
//                    refreshGoogleSignInButton(user)
                    fadeLoginOverlay()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(debugTag, "signInWithCredential:failure", task.exception)
//                    refreshGoogleSignInButton(null)
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
//            refreshGoogleSignInButton(null)
            fadeLoginOverlay()
        }
    }

    private fun revokeAccess() {
        // Firebase sign out
        auth.signOut()

        // Google revoke access
        googleSignInClient.revokeAccess().addOnCompleteListener(this) {
//            refreshGoogleSignInButton(null)
            fadeLoginOverlay()
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

    private fun fadeLoginOverlay() {

        val loggedIn = auth.currentUser != null

        val target = if (loggedIn) {
            0f
        } else {
            loginOverlay.visibility = View.VISIBLE
            1f
        }

        val animator = ValueAnimator.ofFloat(loginOverlay.alpha, target)
        animator.addUpdateListener {
            loginOverlay.alpha = it.animatedValue as Float
            loginOverlay.requestLayout()
        }

        animator.duration = 500
        animator.addListener(object :Animator.AnimatorListener{
            override fun onAnimationRepeat(p0: Animator?) {}

            override fun onAnimationEnd(p0: Animator?) {
                if (loggedIn) {
                    loginOverlay.visibility = View.GONE
                }
            }

            override fun onAnimationCancel(p0: Animator?) {}

            override fun onAnimationStart(p0: Animator?) {}
        })
        animator.start()

    }

}
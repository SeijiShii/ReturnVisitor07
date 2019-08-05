package work.ckogyo.returnvisitor.services

import android.app.IntentService
import android.app.Service
import android.app.Service.START_NOT_STICKY
import android.content.Intent
import android.os.IBinder
import android.util.Log
import work.ckogyo.returnvisitor.MainActivity


class IntentCatcherDummyService : Service() {

    val DUMMY_SERVICE_LAUNCH_INTENT = IntentCatcherDummyService::class.java.name + "_launch_dummy_service"
    val TAG = IntentCatcherDummyService::class.java.canonicalName!! + "_TAG"

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i(TAG, "Intent Catcher Dummy Service Started!")

        if (!MainActivity.isAppVisible) {
            val maIntent = Intent(this, MainActivity::class.java)
            maIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(maIntent)
        }

        Log.i(TAG, "Intent Catcher Dummy Service Stopped!")
        stopSelf()

        return START_NOT_STICKY
    }














}
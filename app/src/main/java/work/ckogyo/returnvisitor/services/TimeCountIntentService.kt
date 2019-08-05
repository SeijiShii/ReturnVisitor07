package work.ckogyo.returnvisitor.services

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent.ACTION_DELETE
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.getDurationString
import java.util.*


class TimeCountIntentService : IntentService("TimeCountIntentService") {

    private val timeNotifyId = 100

    private var timeCounting: Boolean = false
    private var mWork: Work? = null

    private var broadcastManager: LocalBroadcastManager? = null
    private var receiver: BroadcastReceiver? = null

    val startCountActionToService = TimeCountIntentService::class.java.name + "_start_counting_action_to_service"
    val restartCountActionToService =
        TimeCountIntentService::class.java.name + "_restart_counting_action_to_service"
    val TIME_COUNTING_ACTION_TO_ACTIVITY = TimeCountIntentService::class.java.name + "_time_counting_action_to_activity"
    val STOP_TIME_COUNT_ACTION_TO_ACTIVITY =
        TimeCountIntentService::class.java.name + "_stop_time_count_action_to_activity"
    val START_TIME = TimeCountIntentService::class.java.name + "_start_time"
    val DURATION = TimeCountIntentService::class.java.name + "_duration"
    val COUNTING_WORK_ID = TimeCountIntentService::class.java.name + "_counting_work_id"
    val CHANGE_START_ACTION_TO_SERVICE = TimeCountIntentService::class.java.name + "_change_start_action_to_service"

    init {
        initBroadcasting()
    }

    private fun initBroadcasting() {
        broadcastManager = LocalBroadcastManager.getInstance(this)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == CHANGE_START_ACTION_TO_SERVICE) {

                    val startTime = intent.getLongExtra(START_TIME, mWork!!.start.timeInMillis)
                    mWork!!.start.timeInMillis = startTime

//                    WorkList.getInstance().setOrAdd(mWork)

//                    RVCloudSync.getInstance().requestDataSyncIfLoggedIn(this@TimeCountIntentService)
                }
            }
        }

        broadcastManager!!.registerReceiver(receiver!!, IntentFilter(CHANGE_START_ACTION_TO_SERVICE))
    }

//    fun getWork(): Work? {
//        return mWork
//    }

    override fun onHandleIntent(intent: Intent?) {

        if (intent != null) {

            if (intent.action == startCountActionToService) {
//                mWork = Work(Calendar.getInstance())
//                WorkList.getInstance().setOrAdd(mWork)
//                RVCloudSync.getInstance().requestDataSyncIfLoggedIn(this)
            } else if (intent.action == restartCountActionToService) {
                val workId = intent.getStringExtra(COUNTING_WORK_ID)
//                mWork = WorkList.getInstance().getById(workId)
//                if (mWork == null) {
//                    stopTimeCount()
//                    return
//                }
            }
        }

        timeCounting = true
        var minCounter = 0

        initNotification(mWork!!.duration)

        while (timeCounting) {

            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                //
            }

            if (mWork != null) {
                val timeBroadCastIntent = Intent()
                timeBroadCastIntent.action = TIME_COUNTING_ACTION_TO_ACTIVITY
                timeBroadCastIntent.putExtra(START_TIME, mWork!!.start.timeInMillis)
                timeBroadCastIntent.putExtra(DURATION, mWork!!.duration)
                timeBroadCastIntent.putExtra(COUNTING_WORK_ID, mWork!!.id)
                mWork!!.end = Calendar.getInstance()
                broadcastManager!!.sendBroadcast(timeBroadCastIntent)
                updateNotification(mWork!!.duration)

                // 約1分ごとに保存するようにする
                minCounter++
                if (minCounter > 50) {

                    mWork!!.end = Calendar.getInstance()

//                    WorkList.getInstance().setOrAdd(mWork)
//                    RVCloudSync.getInstance().requestDataSyncIfLoggedIn(this)
                    minCounter = 0
                }
            }
        }

        if (notificationManager != null) {
            notificationManager!!.cancel(timeNotifyId)
        }

        mWork = null

        val stopIntent = Intent(STOP_TIME_COUNT_ACTION_TO_ACTIVITY)
        broadcastManager!!.sendBroadcast(stopIntent)

    }


    fun isTimeCounting(): Boolean {
        return timeCounting
    }

    fun stopTimeCount() {
        timeCounting = false
    }

    private var notificationManager: NotificationManager? = null
    private var mBuilder: NotificationCompat.Builder? = null

    private fun initNotification(duration: Long) {

        val durationText = getString(R.string.duration_placeholder, getDurationString(duration, true))

        mBuilder = NotificationCompat.Builder(this)
            .setSmallIcon(R.mipmap.rv_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(durationText)


        val dummyIntent = Intent(this, IntentCatcherDummyService::class.java)
        val dummyPendingIntent = PendingIntent.getService(this, 0, dummyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder!!.setContentIntent(dummyPendingIntent)

        val deleteIntent = Intent(this, TimeCountIntentService::class.java)
        deleteIntent.action = ACTION_DELETE
        val deletePendingIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder!!.setDeleteIntent(deletePendingIntent)

        // キャンセルできないようにする
        mBuilder!!.setOngoing(true)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager!!.notify(timeNotifyId, mBuilder!!.build())
    }

    private fun updateNotification(duration: Long) {

        val durationText = getString(R.string.duration_placeholder, getDurationString(duration, true))
        mBuilder!!.setContentText(durationText)

        // キャンセルできないようにする
        mBuilder!!.setOngoing(true)

        notificationManager!!.notify(timeNotifyId, mBuilder!!.build())
    }

}
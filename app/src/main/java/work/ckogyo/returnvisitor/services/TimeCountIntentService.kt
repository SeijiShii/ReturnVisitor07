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
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.toDurationText
import java.util.*


class TimeCountIntentService : IntentService("TimeCountIntentService") {

    private val timeNotifyId = 100

    private var broadcastManager: LocalBroadcastManager? = null
    private var receiver: BroadcastReceiver? = null

    companion object {
        val startCountToService = TimeCountIntentService::class.java.name + "_start_counting_to_service"
        val restartCountToService = TimeCountIntentService::class.java.name + "_restart_counting_to_service"
        val timeCountingToActivity = TimeCountIntentService::class.java.name + "_time_counting_to_activity"
        val stopTimeCountingToActivity = TimeCountIntentService::class.java.name + "_stop_time_count_to_activity"
        val startTime = TimeCountIntentService::class.java.name + "_start_time"
        val endTime = TimeCountIntentService::class.java.name + "end_time"
        val duration = TimeCountIntentService::class.java.name + "_duration"
        val countingWorkId = TimeCountIntentService::class.java.name + "_counting_work_id"
        val changeStartTimeToService = TimeCountIntentService::class.java.name + "_change_start_time_to_service"

        var isTimeCounting: Boolean = false

        fun stopTimeCount() {
            isTimeCounting = false
        }

        // companion objectから触れるようにシングルトン的な
        private var work: Work? = null

        fun isWorkTimeCounting(work2: Work): Boolean {
            if (!isTimeCounting || work == null){
                return false
            }
            return work!! == work2
        }

        /**
         * Workの更新は30秒に1回なのでVisitの編集タイミングなどによってはWork外になってしまうので、データの編集タイミングに合わせて呼ばれる
         */
        fun saveWorkIfActive() {

            work ?: return

            GlobalScope.launch {
                WorkCollection.instance.set(work!!)
                MonthReportCollection.instance.updateAndLoadByMonthAsync(work!!.start)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initBroadcasting()
    }

    // NOTE: アプリがキルされたときに計時サービスを止めるようにしたけれど、いかがなものか
    // メモリ不足でアプリが殺されたとき、バックグラウンドで計時していたいけれどそれもキルされてしまうのか。
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cancelNotification()
    }

    private fun initBroadcasting() {
        broadcastManager = LocalBroadcastManager.getInstance(this)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == changeStartTimeToService) {

                    val startTime = intent.getLongExtra(startTime, work!!.start.timeInMillis)
                    work!!.start.timeInMillis = startTime

//                    WorkList.getInstance().setOrAdd(mWork)

//                    RVCloudSync.getInstance().requestDataSyncIfLoggedIn(this@TimeCountIntentService)
                }
            }
        }

        broadcastManager!!.registerReceiver(receiver!!, IntentFilter(changeStartTimeToService))
    }

    override fun onHandleIntent(intent: Intent?) {

        val workColl = WorkCollection.instance

        if (intent != null) {

            if (intent.action == startCountToService) {
                Toast.makeText(this, "Time count started", Toast.LENGTH_SHORT).show()
                work = Work()
                work!!.start = Calendar.getInstance()
                GlobalScope.launch {
                    workColl.set(work!!)
                    MonthReportCollection.instance.updateAndLoadByMonthAsync(work!!.start)
                }
            } else if (intent.action == restartCountToService) {
                val workId = intent.getStringExtra(countingWorkId)
//                mWork = WorkList.getInstance().getById(workId)
//                if (mWork == null) {
//                    stopTimeCount()
//                    return
//                }
            }
        }

        isTimeCounting = true
        var minCounter = 0

        initNotification(work!!.duration)

        while (isTimeCounting) {

            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                //
            }

            if (work != null) {
                val timeBroadCastIntent = Intent(timeCountingToActivity)
                loadOnIntent(timeBroadCastIntent)

                work!!.end = Calendar.getInstance()
                broadcastManager!!.sendBroadcast(timeBroadCastIntent)
                updateNotification(work!!.duration)

                // 約1分ごとに保存するようにする
                minCounter++
                if (minCounter > 50) {

                    work!!.end = Calendar.getInstance()
                    GlobalScope.launch {
                        WorkCollection.instance.set(work!!)
                        MonthReportCollection.instance.updateAndLoadByMonthAsync(work!!.start)
                    }
                    minCounter = 0
                }
            }
        }

        cancelNotification()

        val stopIntent = Intent(stopTimeCountingToActivity)
        loadOnIntent(stopIntent)

        work = null

        broadcastManager!!.sendBroadcast(stopIntent)

    }

    private fun loadOnIntent(intent: Intent) {
        intent.putExtra(startTime, work!!.start.timeInMillis)
        intent.putExtra(endTime, work!!.end.timeInMillis)
        intent.putExtra(duration, work!!.duration)
        intent.putExtra(countingWorkId, work!!.id)
    }

    private fun cancelNotification() {
        if (notificationManager != null) {
            notificationManager!!.cancel(timeNotifyId)
        }
    }



    private var notificationManager: NotificationManager? = null
    private var mBuilder: NotificationCompat.Builder? = null

    private fun initNotification(duration: Long) {

        val durationText = getString(R.string.duration_placeholder, duration.toDurationText(true))

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

        val durationText = getString(R.string.duration_placeholder, duration.toDurationText(true))
        mBuilder!!.setContentText(durationText)

        // キャンセルできないようにする
        mBuilder!!.setOngoing(true)

        notificationManager!!.notify(timeNotifyId, mBuilder!!.build())
    }



}
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.firebasedb.MonthReportCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.SharedPrefKeys
import work.ckogyo.returnvisitor.utils.toDurationText
import java.util.*


class TimeCountIntentService : IntentService("TimeCountIntentService") {

    private val timeNotifyId = 100

    private var broadcastManager: LocalBroadcastManager? = null
    private var receiver: BroadcastReceiver? = null

    companion object {
        val startCountingToService = TimeCountIntentService::class.java.name + "_start_counting_to_service"
        val restartCountingToService = TimeCountIntentService::class.java.name + "_restart_counting_to_service"
        val timeCountingToActivity = TimeCountIntentService::class.java.name + "_time_counting_to_activity"
        val stopTimeCountingToActivity = TimeCountIntentService::class.java.name + "_stop_time_count_to_activity"
        val startTime = TimeCountIntentService::class.java.name + "_start_time"
        val endTime = TimeCountIntentService::class.java.name + "end_time"
        val duration = TimeCountIntentService::class.java.name + "_duration"
        val timeCountingWorkId = TimeCountIntentService::class.java.name + "_time_counting_work_id"
        val changeStartTimeToService = TimeCountIntentService::class.java.name + "_change_start_time_to_service"

        var isTimeCounting: Boolean = false

        fun stopTimeCount(context: Context) {
            isTimeCounting = false
            saveTimeCountingStateToSharedPrefs(context)
        }

        private fun saveTimeCountingStateToSharedPrefs(context: Context) {

            val prefs = context.getSharedPreferences(SharedPrefKeys.returnVisitorPrefsKey, Context.MODE_PRIVATE)

            prefs.edit().apply {
                putBoolean(SharedPrefKeys.isTimeCounting, isTimeCounting)

                if (isTimeCounting) {
                    putString(timeCountingWorkId, work!!.id)
                } else {
                    remove(timeCountingWorkId)
                }
            }.apply()
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

            if (intent.action == startCountingToService) {
                Toast.makeText(this, "Time count started", Toast.LENGTH_SHORT).show()
                work = Work()
                work!!.start = Calendar.getInstance()
                GlobalScope.launch {
                    workColl.set(work!!)
                    MonthReportCollection.instance.updateAndLoadByMonthAsync(work!!.start)
                }
            } else if (intent.action == restartCountingToService) {
                val workId = intent.getStringExtra(timeCountingWorkId)
                runBlocking {
                    work = workColl.loadById(workId)
                    if (work == null) {
                        stopTimeCount(this@TimeCountIntentService)
                        return@runBlocking
                    }
                }
            }
        }

        isTimeCounting = true
        saveTimeCountingStateToSharedPrefs(this)

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
        intent.putExtra(timeCountingWorkId, work!!.id)
    }

    private fun cancelNotification() {
        if (notificationManager != null) {
            notificationManager!!.cancel(timeNotifyId)
        }
    }



    private var notificationManager: NotificationManager? = null
    private var notificationBuilder: NotificationCompat.Builder? = null

    private fun initNotification(duration: Long) {

        val durationText = getString(R.string.duration_placeholder, duration.toDurationText(true))

        notificationBuilder = NotificationCompat.Builder(this)
            .setSmallIcon(R.mipmap.rv_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(durationText)


        val dummyIntent = Intent(this, IntentCatcherDummyService::class.java)
        val dummyPendingIntent = PendingIntent.getService(this, 0, dummyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder!!.setContentIntent(dummyPendingIntent)

        val deleteIntent = Intent(this, TimeCountIntentService::class.java)
        deleteIntent.action = ACTION_DELETE
        val deletePendingIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder!!.setDeleteIntent(deletePendingIntent)

        // キャンセルできないようにする
        notificationBuilder!!.setOngoing(true)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager!!.notify(timeNotifyId, notificationBuilder!!.build())
    }

    private fun updateNotification(duration: Long) {

        val durationText = getString(R.string.duration_placeholder, duration.toDurationText(true))
        notificationBuilder!!.setContentText(durationText)

        // キャンセルできないようにする
        notificationBuilder!!.setOngoing(true)

        notificationManager!!.notify(timeNotifyId, notificationBuilder!!.build())
    }




}
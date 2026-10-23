package com.radyo.turkiyeradyolari

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object StreamHealthMonitor {
    private const val HEALTH_URL =
        "https://raw.githubusercontent.com/temelturkyilmaz1-dev/radyot-streams/main/health.json"
    private const val JOB_ID = 7301
    private const val CHANNEL_ID = "stream_health"
    private const val PREFS = "stream_health_notifications"
    private const val KEY_OFFLINE = "notified_offline"
    private const val KEY_STATE_VERSION = "state_version"
    // v3: Eski katalog URL'siyle kaydedilmiş bildirim durumunu bir kez sıfırla.
    // Böylece güncel GitHub sağlık dosyasındaki mevcut arızalar yeniden bildirilir.
    private const val STATE_VERSION = 3
    private const val NOTIFICATION_OFFLINE = 7302
    private const val NOTIFICATION_RECOVERED = 7303

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    fun schedule(context: Context) {
        // Android 13+ cihazlarda izin verilmeden çalışan ilk Job, arızayı
        // bildirilmiş gibi kaydetmemeli. İzin yoksa Job'u henüz kurma;
        // MainActivity izin verildiğinde yeniden schedule/check çağırır.
        if (!canNotify(context)) return
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (scheduler.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, StreamHealthJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(60L * 60L * 1000L)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }

    fun check(context: Context, finished: () -> Unit) {
        val request = Request.Builder()
            .url(HEALTH_URL)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = finished()

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val raw = if (it.isSuccessful) it.body?.string() else null
                    if (!raw.isNullOrBlank()) process(context.applicationContext, raw)
                }
                finished()
            }
        })
    }

    private fun process(context: Context, raw: String) {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return
        if (root.optInt("schemaVersion", -1) != 1) return
        val stations = root.optJSONObject("stations") ?: return
        val offline = linkedMapOf<String, String>()
        val keys = stations.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val station = stations.optJSONObject(id) ?: continue
            if (station.optString("status") != "offline") continue
            val name = station.optString("name", id)
            // Eski GitHub kataloğunda kalmış ancak güncel uygulamadan
            // çıkarılmış istasyonlar için gereksiz bildirim üretme.
            if (RadioRepository.byName(name) == null) continue
            val firstUrl = station.optJSONArray("urls")?.optJSONObject(0)
            val detail = firstUrl?.optString("error")?.takeIf(String::isNotBlank)
                ?: "Yayın üç kontroldür alınamıyor"
            offline[name] = detail
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Önceki sürüm, izin kapalıyken de arızaları bildirilmiş sayabiliyordu.
        // Durum sürümünü yükselterek o hatalı kayıtları bir kez temizle ve
        // halen kapalı olan istasyonları yeniden değerlendirmeye al.
        val previous = if (prefs.getInt(KEY_STATE_VERSION, 0) == STATE_VERSION) {
            prefs.getStringSet(KEY_OFFLINE, emptySet()).orEmpty()
        } else {
            emptySet()
        }
        val current = offline.keys
        val newlyOffline = current - previous
        val recovered = previous - current

        // Bildirim izni kapalıysa ya da kullanıcı sağlık bildirim kanalını
        // kapattıysa durumu "bildirildi" olarak kaydetme. Böylece izin/kanal
        // yeniden açıldığında mevcut arıza bir sonraki kontrolde gerçekten
        // kullanıcıya ulaşır.
        if (!canNotify(context)) return
        if (newlyOffline.isNotEmpty()) notifyOffline(context, newlyOffline, offline)
        if (recovered.isNotEmpty()) notifyRecovered(context, recovered)
        prefs.edit()
            .putStringSet(KEY_OFFLINE, current.toSet())
            .putInt(KEY_STATE_VERSION, STATE_VERSION)
            .apply()
    }

    private fun notifyOffline(context: Context, names: Set<String>, details: Map<String, String>) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val style = NotificationCompat.InboxStyle()
        names.take(6).forEach { name -> style.addLine("$name — ${details[name]}") }
        if (names.size > 6) style.addLine("+${names.size - 6} kanal daha")
        val notification = baseNotification(context)
            // Android bildirimlerinde tam renkli uygulama logosu küçük simge
            // olarak kullanılamaz. Özellikle yeni Android sürümleri geçersiz
            // simgeyi reddedip uygulama sürecini kapatabilir.
            .setSmallIcon(R.drawable.ic_radio)
            .setContentTitle("RadyoT yayın uyarısı")
            .setContentText(names.joinToString(", "))
            .setStyle(style)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_OFFLINE, notification)
    }

    private fun notifyRecovered(context: Context, names: Set<String>) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val notification = baseNotification(context)
            .setSmallIcon(R.drawable.ic_radio)
            .setContentTitle("Radyo yayını yeniden açıldı")
            .setContentText(names.joinToString(", "))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_RECOVERED, notification)
    }

    private fun baseNotification(context: Context): NotificationCompat.Builder {
        val intent = Intent(context, SplashActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Radyo yayın durumu",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Üç ardışık kontrolde kapanan ve yeniden açılan yayınlar"
            }
        )
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager?.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }
}

class StreamHealthJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        StreamHealthMonitor.check(this) { jobFinished(params, false) }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}

class StreamHealthBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            StreamHealthMonitor.schedule(context)
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                RadioAlarmScheduler.clearStaleActiveState(context)
            }
            RadioAlarmScheduler.reschedule(context)
        }
    }
}

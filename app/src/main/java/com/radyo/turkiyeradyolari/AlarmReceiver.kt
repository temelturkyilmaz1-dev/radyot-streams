package com.radyo.turkiyeradyolari

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RadioAlarmScheduler.markTriggered(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RadyoT:RadioAlarm"
        ).apply {
            setReferenceCounted(false)
            acquire(60_000L)
        }
        ContextCompat.startForegroundService(
            context,
            Intent(context, RadyoTMediaBrowserService::class.java)
                .setAction(RadyoTMediaBrowserService.ACTION_START_ALARM)
                .putExtra(EXTRA_RADIO, intent.getStringExtra(EXTRA_RADIO))
        )
    }

    companion object {
        const val EXTRA_RADIO = "radio_name"
    }
}

package com.radyo.turkiyeradyolari

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import java.util.Calendar

/** Radyo alarmının seçimini, sesini ve tek seferlik çalışma durumunu kalıcı tutar. */
object RadioAlarmScheduler {
    private const val PREFS = "radio_alarm"
    private const val KEY_RADIO = "radio"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_TRIGGER_AT = "trigger_at"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_VOLUME = "volume"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PREVIOUS_VOLUME = "previous_volume"
    private const val REQUEST_ALARM = 1208
    private const val REQUEST_SHOW = 1209

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun selectedRadio(context: Context): String = prefs(context).getString(KEY_RADIO, null)
        ?: AppPreferences.lastRadio(context)
        ?: RadioRepository.radios.firstOrNull()?.name.orEmpty()

    fun setSelectedRadio(context: Context, radioName: String) {
        prefs(context).edit().putString(KEY_RADIO, radioName).apply()
        val triggerAt = triggerAt(context)
        if (isScheduled(context) && triggerAt > System.currentTimeMillis()) {
            setAlarmClock(context, triggerAt, radioName)
        }
    }

    fun volume(context: Context): Int = prefs(context).getInt(KEY_VOLUME, 70).coerceIn(5, 100)

    fun setVolume(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_VOLUME, percent.coerceIn(5, 100)).apply()
    }

    fun isScheduled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)
    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)
    fun triggerAt(context: Context): Long = prefs(context).getLong(KEY_TRIGGER_AT, 0L)
    fun hour(context: Context): Int = prefs(context).getInt(KEY_HOUR, -1)
    fun minute(context: Context): Int = prefs(context).getInt(KEY_MINUTE, -1)

    fun schedule(context: Context, hour: Int, minute: Int, radioName: String): Boolean {
        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        setSelectedRadio(context, radioName)
        val scheduled = setAlarmClock(context, triggerAt, radioName)
        if (scheduled) {
            prefs(context).edit()
                .putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
                .putLong(KEY_TRIGGER_AT, triggerAt)
                .putBoolean(KEY_ENABLED, true)
                .apply()
        }
        return scheduled
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(alarmIntent(context))
        prefs(context).edit().putBoolean(KEY_ENABLED, false).putLong(KEY_TRIGGER_AT, 0L).apply()
    }

    fun reschedule(context: Context) {
        if (!isScheduled(context)) return
        val triggerAt = triggerAt(context)
        val radioName = selectedRadio(context)
        if (triggerAt <= System.currentTimeMillis() || radioName.isBlank()) {
            cancel(context)
            return
        }
        if (!setAlarmClock(context, triggerAt, radioName)) cancel(context)
    }

    fun markTriggered(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, false)
            .putLong(KEY_TRIGGER_AT, 0L)
            .apply()
    }

    fun applyAlarmVolume(context: Context) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previous = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (maximum * volume(context) / 100f).toInt().coerceIn(1, maximum)
        val settings = prefs(context)
        val edit = settings.edit().putBoolean(KEY_ACTIVE, true)
        if (!settings.getBoolean(KEY_ACTIVE, false)) edit.putInt(KEY_PREVIOUS_VOLUME, previous)
        edit.apply()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun clearStaleActiveState(context: Context) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, false).remove(KEY_PREVIOUS_VOLUME).apply()
    }

    fun stopActive(context: Context) {
        val settings = prefs(context)
        if (settings.getBoolean(KEY_ACTIVE, false)) {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val previous = settings.getInt(KEY_PREVIOUS_VOLUME, -1)
            if (previous >= 0) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, previous.coerceIn(0, maximum), 0)
            }
        }
        settings.edit().putBoolean(KEY_ACTIVE, false).remove(KEY_PREVIOUS_VOLUME).apply()
    }

    private fun setAlarmClock(context: Context, triggerAt: Long, radioName: String): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operation = alarmIntent(context, radioName)
        val showAlarm = PendingIntent.getActivity(
            context,
            REQUEST_SHOW,
            Intent(context, TimerSettingsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return runCatching {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showAlarm), operation)
        }.isSuccess
    }

    private fun alarmIntent(context: Context, radioName: String = selectedRadio(context)): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_RADIO, radioName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

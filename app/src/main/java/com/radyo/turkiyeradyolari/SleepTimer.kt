package com.radyo.turkiyeradyolari

import android.os.Handler
import android.os.Looper

object SleepTimer {
    private val handler = Handler(Looper.getMainLooper())
    private var stopTask: Runnable? = null
    var endAtMillis: Long = 0L
        private set

    fun schedule(minutes: Int) {
        cancel()
        if (minutes <= 0) return
        endAtMillis = System.currentTimeMillis() + minutes * 60_000L
        stopTask = Runnable {
            RadioPlayerManager.stop()
            endAtMillis = 0L
            stopTask = null
        }.also { handler.postDelayed(it, minutes * 60_000L) }
    }

    fun cancel() {
        stopTask?.let(handler::removeCallbacks)
        stopTask = null
        endAtMillis = 0L
    }
}

package com.radyo.turkiyeradyolari

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews

class RadyoTWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            PREVIOUS -> playAdjacent(context, -1)
            PLAY_PAUSE -> if (RadioPlayerManager.currentRadio == null) {
                val radio = AppPreferences.lastRadio(context)?.let(RadioRepository::byName) ?: RadioRepository.radios.firstOrNull()
                if (radio != null) RadioPlayerManager.play(context, radio)
            } else RadioPlayerManager.togglePlayPause(context)
            NEXT -> playAdjacent(context, 1)
            FAVORITE -> toggleFavorite(context)
        }
        updateAll(context)
    }

    private fun playAdjacent(context: Context, direction: Int) {
        val radios = RadioRepository.radios
        if (radios.isEmpty()) return
        val current = RadioPlayerManager.currentRadio ?: AppPreferences.lastRadio(context)?.let(RadioRepository::byName)
        val index = radios.indexOfFirst { it.name == current?.name }.let { if (it < 0) 0 else it }
        RadioPlayerManager.play(context, radios[(index + direction + radios.size) % radios.size])
    }

    private fun toggleFavorite(context: Context) {
        val name = (RadioPlayerManager.currentRadio ?: AppPreferences.lastRadio(context)?.let(RadioRepository::byName))?.name ?: return
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val names = prefs.getString("favorite_radios", "").orEmpty().split("|").filter(String::isNotBlank).toMutableSet()
        if (!names.add(name)) names.remove(name)
        prefs.edit().putString("favorite_radios", names.joinToString("|")).apply()
    }

    companion object {
        private const val PREVIOUS = "com.radyo.turkiyeradyolari.widget.PREVIOUS"
        private const val PLAY_PAUSE = "com.radyo.turkiyeradyolari.widget.PLAY_PAUSE"
        private const val NEXT = "com.radyo.turkiyeradyolari.widget.NEXT"
        private const val FAVORITE = "com.radyo.turkiyeradyolari.widget.FAVORITE"

        fun updateAll(context: Context, albumArtwork: Bitmap? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, RadyoTWidgetProvider::class.java)
            val radio = RadioPlayerManager.currentRadio ?: AppPreferences.lastRadio(context)?.let(RadioRepository::byName)
            val views = RemoteViews(context.packageName, R.layout.widget_radyot)
            if (albumArtwork != null && AppPreferences.showAlbumArt(context)) {
                views.setImageViewBitmap(R.id.widgetArtwork, albumArtwork)
            } else {
                views.setImageViewResource(R.id.widgetArtwork, radio?.logoRes ?: R.drawable.radyot_logo)
            }
            views.setTextViewText(R.id.widgetStation, radio?.name ?: context.getString(R.string.app_name))
            val track = listOf(RadioPlayerManager.currentArtist, RadioPlayerManager.currentTitle).filter(String::isNotBlank).joinToString(" — ")
            views.setTextViewText(R.id.widgetTrack, track.ifBlank { context.getString(R.string.live_radio) })
            views.setImageViewResource(R.id.widgetPlayPause, if (RadioPlayerManager.isCurrentlyPlaying()) R.drawable.ic_pause else R.drawable.ic_play)
            val favorite = radio?.name?.let { context.getSharedPreferences("favorites", Context.MODE_PRIVATE).getString("favorite_radios", "").orEmpty().split("|").contains(it) } == true
            views.setImageViewResource(R.id.widgetFavorite, if (favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite)
            views.setOnClickPendingIntent(R.id.widgetRoot, PendingIntent.getActivity(context, 40, Intent(context, SplashActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            bind(views, context, R.id.widgetPrevious, PREVIOUS, 41)
            bind(views, context, R.id.widgetPlayPause, PLAY_PAUSE, 42)
            bind(views, context, R.id.widgetNext, NEXT, 43)
            bind(views, context, R.id.widgetFavorite, FAVORITE, 44)
            manager.updateAppWidget(component, views)
        }

        private fun bind(views: RemoteViews, context: Context, viewId: Int, action: String, code: Int) {
            views.setOnClickPendingIntent(viewId, PendingIntent.getBroadcast(context, code, Intent(context, RadyoTWidgetProvider::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
    }
}

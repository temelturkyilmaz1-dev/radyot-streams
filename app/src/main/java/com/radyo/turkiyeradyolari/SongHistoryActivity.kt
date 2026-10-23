package com.radyo.turkiyeradyolari

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SongHistoryActivity : AppCompatActivity() {
    private lateinit var radioName: String
    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_history)
        radioName = intent.getStringExtra("RADIO_NAME").orEmpty()
        findViewById<TextView>(R.id.txtTitle).text = getString(R.string.station_history_title, radioName)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnClearHistory).setOnClickListener {
            AlertDialog.Builder(this).setMessage(R.string.clear_history_confirm)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.clear) { _, _ -> SongHistoryStore.clear(this, radioName); load() }.show()
        }
        load()
    }
    private fun load() {
        val list = SongHistoryStore.get(this, radioName)
        findViewById<TextView>(R.id.txtEmpty).visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<RecyclerView>(R.id.recyclerHistory).apply { layoutManager = LinearLayoutManager(this@SongHistoryActivity); adapter = SongHistoryAdapter(list) }
    }
}

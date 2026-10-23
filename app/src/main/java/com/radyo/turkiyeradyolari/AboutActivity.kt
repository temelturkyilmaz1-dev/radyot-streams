package com.radyo.turkiyeradyolari

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        val info = packageManager.getPackageInfo(packageName, 0)
        findViewById<TextView>(R.id.txtVersion).text = getString(R.string.version_format, info.versionName ?: "-")
    }
}

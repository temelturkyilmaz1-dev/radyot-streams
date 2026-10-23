package com.radyo.turkiyeradyolari

import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private var animationFinished = false
    private var mainOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imgSplashLogo)
        val glow = findViewById<View>(R.id.viewSplashGlow)
        val interpolator = AccelerateDecelerateInterpolator()

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(700L)
            .setInterpolator(interpolator)
            .withEndAction {
                logo.animate()
                    .scaleX(1.035f)
                    .scaleY(1.035f)
                    .setDuration(220L)
                    .setInterpolator(interpolator)
                    .withEndAction {
                        logo.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(220L)
                            .setInterpolator(interpolator)
                            .withEndAction {
                                animationFinished = true
                                openMainIfReady()
                            }
                            .start()
                    }
                    .start()
            }
            .start()

        glow.animate()
            .alpha(0f)
            .scaleX(1.55f)
            .scaleY(1.55f)
            .setStartDelay(180L)
            .setDuration(960L)
            .setInterpolator(interpolator)
            .start()
    }

    private fun openMainIfReady() {
        if (!animationFinished || mainOpened || isFinishing || isDestroyed) return
        mainOpened = true
        val isTelevision = (resources.configuration.uiMode and
            Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        startActivity(Intent(this, if (isTelevision) TvMainActivity::class.java else MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}

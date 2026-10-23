package com.radyo.turkiyeradyolari

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/** Son Dinlenenler, Favoriler ve Tüm Radyolar arasında yatay kaydırılan ekran. */
class AllRadiosActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_radios)
        applySystemBarInsets()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        pager = findViewById(R.id.radioPager)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                PAGE_RECENT -> RecentStationsFragment()
                PAGE_FAVORITES -> FavoritesFragment()
                else -> AllStationsFragment()
            }
        }
        pager.offscreenPageLimit = 2

        val tabs = findViewById<TabLayout>(R.id.radioTabs)
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = getString(pageTitle(position))
        }.attach()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateTitle(position)
        })
        if (savedInstanceState == null) {
            val initialPage = intent.getIntExtra(EXTRA_PAGE, PAGE_ALL)
                .coerceIn(PAGE_RECENT, PAGE_ALL)
            pager.setCurrentItem(initialPage, false)
            updateTitle(initialPage)
        } else {
            pager.post { updateTitle(pager.currentItem) }
        }
    }

    private fun updateTitle(position: Int) {
        title = getString(pageTitle(position))
    }

    private fun pageTitle(position: Int): Int = when (position) {
        PAGE_RECENT -> R.string.recent_stations
        PAGE_FAVORITES -> R.string.favorites
        else -> R.string.all_radios
    }

    private fun applySystemBarInsets() {
        val root = findViewById<android.view.View>(R.id.allRadiosRoot)
        val bar = findViewById<android.view.View>(R.id.bar)
        val initialTop = bar.paddingTop
        val initialLeft = root.paddingLeft
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            root.updatePadding(
                left = initialLeft + maxOf(bars.left, gestures.left),
                right = initialRight + maxOf(bars.right, gestures.right),
                bottom = initialBottom + maxOf(bars.bottom, gestures.bottom)
            )
            bar.updatePadding(top = initialTop + bars.top)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    companion object {
        const val EXTRA_PAGE = "browse_page"
        const val PAGE_RECENT = 0
        const val PAGE_FAVORITES = 1
        const val PAGE_ALL = 2
    }
}

package com.radyo.turkiyeradyolari

import androidx.annotation.DrawableRes

data class Radio(
    val name: String,
    val url: String,
    @DrawableRes val logoRes: Int,
    val backupUrls: List<String> = emptyList(),
    val website: String = "",
    val content: String = "",
    val location: String = "",
    val frequency: String = "",
    val logoPath: String = ""
) {
    /** Primary stream first, then unique backup streams. */
    val streamUrls: List<String>
        get() = (listOf(url) + backupUrls)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
}

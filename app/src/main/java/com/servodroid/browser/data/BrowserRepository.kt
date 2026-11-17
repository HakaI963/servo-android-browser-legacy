package com.servodroid.browser.data

import android.content.Context
import android.content.SharedPreferences
import com.servodroid.browser.model.Bookmark
import com.servodroid.browser.model.HistoryEntry
import com.servodroid.browser.model.ThemeMode

/**
 * Small, dependency-free persistence layer backed by SharedPreferences. Data is stored as
 * tab-separated records; this keeps the app self-contained without a database dependency.
 */
class BrowserRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("servo_browser", Context.MODE_PRIVATE)

    var history: List<HistoryEntry> = prefs.getList(KEY_HISTORY).mapNotNull { record ->
        val parts = record.split(SEP, limit = 3)
        if (parts.size == 3) {
            HistoryEntry(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
        } else null
    }
        private set

    var bookmarks: List<Bookmark> = prefs.getList(KEY_BOOKMARKS).mapNotNull { record ->
        val parts = record.split(SEP, limit = 2)
        if (parts.size == 2) Bookmark(parts[0], parts[1]) else null
    }
        private set

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, "SYSTEM")!!) }
            .getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var searchEngineName: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "DuckDuckGo") ?: "DuckDuckGo"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    fun addHistory(url: String, title: String) {
        if (url.startsWith("about:", ignoreCase = true)) return
        val entry = HistoryEntry(url, title, System.currentTimeMillis())
        history = listOf(entry) + history.filterNot { it.url == url }
        prefs.putList(KEY_HISTORY, history.map { "${it.url}$SEP${it.title}$SEP${it.timestamp}" })
    }

    fun removeHistory(url: String) {
        history = history.filterNot { it.url == url }
        prefs.putList(KEY_HISTORY, history.map { "${it.url}$SEP${it.title}$SEP${it.timestamp}" })
    }

    fun clearHistory() {
        history = emptyList()
        prefs.putList(KEY_HISTORY, emptyList())
    }

    fun addBookmark(url: String, title: String) {
        if (bookmarks.any { it.url == url }) return
        val bookmark = Bookmark(url, title)
        bookmarks = listOf(bookmark) + bookmarks
        prefs.putList(KEY_BOOKMARKS, bookmarks.map { "${it.url}$SEP${it.title}" })
    }

    fun removeBookmark(url: String) {
        bookmarks = bookmarks.filterNot { it.url == url }
        prefs.putList(KEY_BOOKMARKS, bookmarks.map { "${it.url}$SEP${it.title}" })
    }

    fun isBookmarked(url: String): Boolean = bookmarks.any { it.url == url }

    fun clearAll() {
        clearHistory()
        bookmarks = emptyList()
        prefs.putList(KEY_BOOKMARKS, emptyList())
    }

    private fun SharedPreferences.getList(key: String): List<String> =
        getString(key, "").orEmpty().takeIf { it.isNotEmpty() }?.split(SEP_RECORD) ?: emptyList()

    private fun SharedPreferences.Editor.putList(key: String, values: List<String>) {
        putString(key, values.joinToString(SEP_RECORD))
        apply()
    }

    private companion object {
        const val KEY_HISTORY = "history"
        const val KEY_BOOKMARKS = "bookmarks"
        const val KEY_THEME = "theme"
        const val KEY_SEARCH_ENGINE = "search_engine"
        const val SEP = "\u001f"
        const val SEP_RECORD = "\u001e"
    }
}

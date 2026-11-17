package com.servodroid.browser.model

data class Tab(
    val id: String,
    val url: String,
    val title: String,
    val isPrivate: Boolean = false,
)

data class HistoryEntry(
    val url: String,
    val title: String,
    val timestamp: Long,
)

data class Bookmark(
    val url: String,
    val title: String,
)

data class DownloadItem(
    val fileName: String,
    val url: String,
    val timestamp: Long,
)

/** Destination rendered by the app shell (everything except the live page). */
sealed interface Screen {
    data object Browser : Screen
    data object Tabs : Screen
    data object History : Screen
    data object Bookmarks : Screen
    data object Downloads : Screen
    data object Settings : Screen
    data object About : Screen
}

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class SearchEngine(val name: String, val queryUrl: String) {
    fun searchUrl(query: String): String = queryUrl + android.net.Uri.encode(query)

    companion object {
        val DUCKDUCKGO = SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=")
        val GOOGLE = SearchEngine("Google", "https://www.google.com/search?q=")
        val BING = SearchEngine("Bing", "https://www.bing.com/search?q=")

        val ALL = listOf(DUCKDUCKGO, GOOGLE, BING)
    }
}

/** Turns user input into a URL or a search query. */
fun resolveAddress(input: String, engine: SearchEngine): String {
    val text = input.trim()
    if (text.isEmpty()) return ""
    val looksLikeUrl = text.contains(".") && !text.contains(' ') ||
        text.startsWith("http://", ignoreCase = true) ||
        text.startsWith("https://", ignoreCase = true) ||
        text.startsWith("about:", ignoreCase = true)
    return if (looksLikeUrl) {
        if (text.startsWith("http", ignoreCase = true) || text.startsWith("about:", ignoreCase = true)) {
            text
        } else {
            "https://$text"
        }
    } else {
        engine.searchUrl(text)
    }
}

fun displayHost(url: String): String = runCatching {
    android.net.Uri.parse(url).host ?: url
}.getOrDefault(url)

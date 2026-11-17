package com.servodroid.browser

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.servodroid.browser.data.BrowserRepository
import com.servodroid.browser.engine.BrowserEngine
import com.servodroid.browser.engine.EngineFactory
import com.servodroid.browser.model.Bookmark
import com.servodroid.browser.model.HistoryEntry
import com.servodroid.browser.model.Screen
import com.servodroid.browser.model.SearchEngine
import com.servodroid.browser.model.Tab
import com.servodroid.browser.model.ThemeMode
import com.servodroid.browser.model.displayHost
import com.servodroid.browser.model.resolveAddress
import java.util.UUID

data class BrowserUiState(
    val tabs: List<Tab> = emptyList(),
    val activeTabId: String? = null,
    val screen: Screen = Screen.Browser,
    val addressText: String = "",
    val isEditingAddress: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 100,
    val isLoading: Boolean = false,
    val title: String = "",
    val currentUrl: String = "",
    val menuOpen: Boolean = false,
    val findOpen: Boolean = false,
    val findQuery: String = "",
    val isBookmarked: Boolean = false,
    val desktopSite: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val engineAvailable: Boolean = false,
    val engineName: String = "",
    val history: List<HistoryEntry> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val message: String? = null,
)

class BrowserViewModel(context: Context) : ViewModel() {

    private val repo = BrowserRepository(context)
    val engine: BrowserEngine = EngineFactory.create()

    var uiState by mutableStateOf(
        BrowserUiState(
            themeMode = repo.themeMode,
            searchEngine = SearchEngine.ALL.firstOrNull { it.name == repo.searchEngineName }
                ?: SearchEngine.DUCKDUCKGO,
            engineAvailable = engine.isNative,
            engineName = engine.displayName,
            history = repo.history,
            bookmarks = repo.bookmarks,
        )
    )
        private set

    val activeTab: Tab?
        get() = uiState.tabs.firstOrNull { it.id == uiState.activeTabId }

    init {
        engine.setListener(object : BrowserEngine.Listener {
            override fun onProgress(progress: Int) {
                uiState = uiState.copy(
                    progress = progress,
                    isLoading = progress in 1..99,
                )
            }

            override fun onTitleChanged(title: String) {
                activeTab?.let { tab ->
                    if (title.isNotBlank() && title != tab.title) {
                        updateTab(tab.id) { it.copy(title = title) }
                        repo.addHistory(tab.url, title)
                        refreshData()
                    }
                }
                uiState = uiState.copy(title = title)
            }

            override fun onUrlChanged(url: String) {
                if (url.isBlank()) return
                uiState = uiState.copy(currentUrl = url, addressText = url)
                activeTab?.let { tab ->
                    if (tab.url != url) updateTab(tab.id) { it.copy(url = url) }
                }
            }

            override fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean) {
                uiState = uiState.copy(canGoBack = canGoBack, canGoForward = canGoForward)
            }

            override fun onAlert(message: String) {
                uiState = uiState.copy(message = message)
            }
        })

        if (uiState.tabs.isEmpty()) newTab()
    }

    private fun newTabInstance(url: String = "about:blank", private: Boolean = false): Tab =
        Tab(id = UUID.randomUUID().toString(), url = url, title = "New tab", isPrivate = private)

    private fun updateTab(id: String, transform: (Tab) -> Tab) {
        uiState = uiState.copy(tabs = uiState.tabs.map { if (it.id == id) transform(it) else it })
        syncBookmarkState()
    }

    private fun syncBookmarkState() {
        uiState = uiState.copy(isBookmarked = activeTab?.let { repo.isBookmarked(it.url) } ?: false)
    }

    private fun refreshData() {
        uiState = uiState.copy(history = repo.history, bookmarks = repo.bookmarks)
    }

    fun consumeMessage() {
        uiState = uiState.copy(message = null)
    }

    fun newTab(private: Boolean = false) {
        val tab = newTabInstance(private = private)
        uiState = uiState.copy(
            tabs = uiState.tabs + tab,
            activeTabId = tab.id,
            screen = Screen.Browser,
            addressText = "",
            currentUrl = "",
            title = "New tab",
            isEditingAddress = false,
            canGoBack = false,
            canGoForward = false,
            progress = 100,
        )
        syncBookmarkState()
    }

    fun closeTab(id: String) {
        if (uiState.tabs.size == 1) {
            newTab()
            return
        }
        val remaining = uiState.tabs.filterNot { it.id == id }
        val nextActive = if (uiState.activeTabId == id) remaining.last().id else uiState.activeTabId
        uiState = uiState.copy(tabs = remaining, activeTabId = nextActive)
        activeTab?.let { engine.loadUrl(it.url) }
        syncBookmarkState()
    }

    fun closeAllTabs() {
        val tab = newTabInstance()
        uiState = uiState.copy(
            tabs = listOf(tab),
            activeTabId = tab.id,
            screen = Screen.Browser,
            addressText = "",
            currentUrl = "",
            title = "New tab",
        )
        engine.loadUrl("about:blank")
        syncBookmarkState()
    }

    fun switchTab(id: String) {
        if (id == uiState.activeTabId) {
            uiState = uiState.copy(screen = Screen.Browser)
            return
        }
        val tab = uiState.tabs.firstOrNull { it.id == id } ?: return
        uiState = uiState.copy(
            activeTabId = id,
            screen = Screen.Browser,
            addressText = tab.url,
            currentUrl = tab.url,
            title = tab.title,
            isEditingAddress = false,
        )
        engine.loadUrl(tab.url)
        syncBookmarkState()
    }

    fun navigate(input: String) {
        val url = resolveAddress(input, uiState.searchEngine)
        if (url.isEmpty()) return
        val tabId = uiState.activeTabId ?: run {
            val fresh = newTabInstance()
            uiState = uiState.copy(tabs = uiState.tabs + fresh, activeTabId = fresh.id)
            fresh.id
        }
        val title = displayHost(url)
        updateTab(tabId) { it.copy(url = url, title = title) }
        repo.addHistory(url, title)
        refreshData()
        uiState = uiState.copy(
            screen = Screen.Browser,
            addressText = url,
            currentUrl = url,
            title = title,
            isEditingAddress = false,
            menuOpen = false,
        )
        engine.loadUrl(url)
        syncBookmarkState()
    }

    fun onAddressChange(text: String) {
        uiState = uiState.copy(addressText = text)
    }

    fun startAddressEditing() {
        uiState = uiState.copy(isEditingAddress = true, addressText = uiState.currentUrl)
    }

    fun stopAddressEditing() {
        uiState = uiState.copy(isEditingAddress = false, addressText = uiState.currentUrl)
    }

    fun reload() = engine.reload()
    fun stop() = engine.stop()
    fun goBack() = engine.goBack()
    fun goForward() = engine.goForward()

    fun openScreen(screen: Screen) {
        uiState = uiState.copy(screen = screen, menuOpen = false)
    }

    fun toggleMenu() {
        uiState = uiState.copy(menuOpen = !uiState.menuOpen)
    }

    fun closeMenu() {
        uiState = uiState.copy(menuOpen = false)
    }

    fun toggleFind(open: Boolean) {
        uiState = uiState.copy(findOpen = open, findQuery = "", menuOpen = false)
        if (!open) engine.findInPage("")
    }

    fun onFindQueryChange(query: String) {
        uiState = uiState.copy(findQuery = query)
        engine.findInPage(query)
    }

    fun toggleDesktopSite() {
        val next = !uiState.desktopSite
        uiState = uiState.copy(desktopSite = next, menuOpen = false)
        engine.setDesktopSite(next)
    }

    fun toggleBookmark() {
        val tab = activeTab ?: return
        if (repo.isBookmarked(tab.url)) repo.removeBookmark(tab.url)
        else repo.addBookmark(tab.url, tab.title.ifBlank { displayHost(tab.url) })
        refreshData()
        syncBookmarkState()
    }

    fun removeBookmark(url: String) {
        repo.removeBookmark(url)
        refreshData()
        syncBookmarkState()
    }

    fun removeHistory(url: String) {
        repo.removeHistory(url)
        refreshData()
    }

    fun clearBrowsingData() {
        repo.clearAll()
        refreshData()
        syncBookmarkState()
        uiState = uiState.copy(message = "Browsing data cleared")
    }

    fun setThemeMode(mode: ThemeMode) {
        repo.themeMode = mode
        uiState = uiState.copy(themeMode = mode)
    }

    fun setSearchEngine(name: String) {
        repo.searchEngineName = name
        uiState = uiState.copy(
            searchEngine = SearchEngine.ALL.firstOrNull { it.name == name } ?: SearchEngine.DUCKDUCKGO,
        )
    }

    fun onKeyEvent(keyCode: Int, unicode: Int, isDown: Boolean) {
        engine.onKeyEvent(keyCode, unicode, isDown)
    }

    override fun onCleared() {
        engine.destroy()
        super.onCleared()
    }
}

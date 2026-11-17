package com.servodroid.browser.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.servodroid.browser.BrowserViewModel
import com.servodroid.browser.model.Screen
import com.servodroid.browser.ui.theme.Dimensions

@Composable
fun BrowserApp(vm: BrowserViewModel) {
    val context = LocalContext.current
    val screen = vm.uiState.screen
    val isBrowser = screen == Screen.Browser
    val isNewTab = vm.uiState.currentUrl.isEmpty() ||
        vm.uiState.currentUrl == "about:blank"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isBrowser) {
                TopToolbar(vm)
            }

            Box(modifier = Modifier.weight(1f)) {
                // The engine surface is kept in the composition at all times so the WebView
                // fallback keeps its state while secondary screens are open.
                vm.engine.Content(modifier = Modifier.fillMaxSize())

                if (isBrowser && isNewTab && !vm.uiState.isEditingAddress) {
                    NewTabPage(
                        vm = vm,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    )
                }
            }

            if (isBrowser) {
                if (vm.uiState.findOpen) FindBar(vm)
                BottomToolbar(vm)
            }
        }

        // Search suggestions float beneath the address bar while editing.
        if (isBrowser && vm.uiState.isEditingAddress) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(Dimensions.ToolbarHeight))
                SuggestionsCard(vm)
            }
        }

        if (!isBrowser) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (screen) {
                    Screen.Tabs -> TabsScreen(vm)
                    Screen.History -> HistoryScreen(vm)
                    Screen.Bookmarks -> BookmarksScreen(vm)
                    Screen.Downloads -> DownloadsScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                    Screen.About -> AboutScreen(vm)
                    Screen.Browser -> Unit
                }
            }
        }

        if (vm.uiState.menuOpen) {
            BrowserMenu(
                vm = vm,
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, vm.uiState.currentUrl)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    }
                },
            )
        }
    }
}

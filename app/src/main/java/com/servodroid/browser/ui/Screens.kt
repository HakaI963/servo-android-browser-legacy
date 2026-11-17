@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.servodroid.browser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.servodroid.browser.BrowserViewModel
import com.servodroid.browser.model.Screen
import com.servodroid.browser.model.ThemeMode
import com.servodroid.browser.model.displayHost
import com.servodroid.browser.ui.theme.AppTypography
import com.servodroid.browser.ui.theme.Dimensions
import com.servodroid.browser.ui.theme.Ink
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Rounded

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(Dimensions.ToolbarHeight).padding(horizontal = Dimensions.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChromeIconButton(icon = Rounded.ArrowBack, label = "Back", onClick = onBack)
            Spacer(Modifier.width(Dimensions.SpaceMd))
            Text(text = title, style = AppTypography.Headline, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            actions()
        }
        Hairline()
    }
}

@Composable
fun TabsScreen(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    val normal = state.tabs.filter { !it.isPrivate }
    val private = state.tabs.filter { it.isPrivate }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(Dimensions.ToolbarHeight).padding(horizontal = Dimensions.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Tabs", style = AppTypography.Headline, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(start = Dimensions.SpaceMd))
            TextButton(onClick = { vm.closeAllTabs() }) {
                Text("Close all", style = AppTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ChromeIconButton(icon = Rounded.Check, label = "Done", tint = MaterialTheme.colorScheme.primary) {
                vm.openScreen(Screen.Browser)
            }
        }
        Hairline()

        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Dimensions.SpaceMd,
                    vertical = Dimensions.SpaceMd,
                ),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpaceMd),
            ) {
                if (normal.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text("Open tabs", style = AppTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Dimensions.SpaceXs))
                    }
                    items(normal, key = { it.id }) { tab -> TabCard(vm, tab) }
                }
                if (private.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Dimensions.SpaceMd, bottom = Dimensions.SpaceXs)) {
                            Icon(Rounded.VisibilityOff, contentDescription = null, tint = Ink.PrivateAccent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(Dimensions.SpaceSm))
                            Text("Private", style = AppTypography.Label, color = Ink.PrivateAccent)
                        }
                    }
                    items(private, key = { it.id }) { tab -> TabCard(vm, tab, private = true) }
                }
            }

            androidx.compose.material3.FloatingActionButton(
                onClick = { vm.newTab() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimensions.SpaceLg),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Rounded.Add, contentDescription = "New tab")
            }
        }
    }
}

@Composable
private fun TabCard(vm: BrowserViewModel, tab: com.servodroid.browser.model.Tab, private: Boolean = false) {
    val active = vm.uiState.activeTabId == tab.id
    val accent = if (private) Ink.PrivateAccent else Ink.Accent
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clickable { vm.switchTab(tab.id) }
            .border(
                if (active) BorderStroke(1.dp, accent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                RoundedCornerShape(Dimensions.RadiusCard),
            ),
        shape = RoundedCornerShape(Dimensions.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(Dimensions.SpaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayHost(tab.url).takeIf { tab.url != "about:blank" } ?: "New tab",
                    style = AppTypography.Caption,
                    color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Rounded.Close,
                    contentDescription = "Close tab",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).clickable { vm.closeTab(tab.id) },
                )
            }
            Spacer(Modifier.height(Dimensions.SpaceMd))
            Text(
                text = tab.title.ifBlank { "New tab" },
                style = AppTypography.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun HistoryScreen(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "History",
            onBack = { vm.openScreen(Screen.Browser) },
            actions = {
                if (state.history.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear", style = AppTypography.Label, color = Ink.Danger)
                    }
                }
            },
        )
        if (state.history.isEmpty()) {
            EmptyState(icon = Rounded.History, title = "No history yet", caption = "Pages you visit will show up here")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text("Recent", style = AppTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(Dimensions.SpaceLg))
                }
                items(state.history, key = { it.url + it.timestamp }) { entry ->
                    HistoryRow(
                        title = entry.title.ifBlank { displayHost(entry.url) },
                        subtitle = displayHost(entry.url),
                        onClick = { vm.navigate(entry.url) },
                        onDelete = { vm.removeHistory(entry.url) },
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history", style = AppTypography.Title) },
            text = { Text("This removes all browsing history from this device.", style = AppTypography.Body) },
            confirmButton = { TextButton(onClick = { vm.clearBrowsingData(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun BookmarksScreen(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Bookmarks",
            onBack = { vm.openScreen(Screen.Browser) },
            actions = {
                val tab = vm.activeTab
                if (tab != null && tab.url != "about:blank") {
                    TextButton(onClick = { vm.toggleBookmark() }) {
                        Text(if (state.isBookmarked) "Remove" else "Add current", style = AppTypography.Label, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
        )
        if (state.bookmarks.isEmpty()) {
            EmptyState(icon = Rounded.BookmarkBorder, title = "No bookmarks yet", caption = "Save pages you want to return to")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.bookmarks, key = { it.url }) { bookmark ->
                    HistoryRow(
                        title = bookmark.title.ifBlank { displayHost(bookmark.url) },
                        subtitle = displayHost(bookmark.url),
                        onClick = { vm.navigate(bookmark.url) },
                        onDelete = { vm.removeBookmark(bookmark.url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(title: String, subtitle: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = title.take(1).uppercase(), style = AppTypography.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Dimensions.SpaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = AppTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ChromeIconButton(icon = Rounded.Close, label = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = onDelete)
    }
}

@Composable
fun DownloadsScreen(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Downloads", onBack = { vm.openScreen(Screen.Browser) })
        EmptyState(icon = Rounded.Download, title = "No downloads yet", caption = "Files you download will show up here")
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, caption: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimensions.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(Dimensions.SpaceLg))
        Text(text = title, style = AppTypography.Title, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Dimensions.SpaceXs))
        Text(text = caption, style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun SettingsScreen(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings", onBack = { vm.openScreen(Screen.Browser) })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SettingSection("Appearance") }
            item {
                SettingLabel("Theme")
                ThemeMode.entries.forEach { mode ->
                    SelectableRow(
                        title = when (mode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.LIGHT -> "Light"
                        },
                        selected = state.themeMode == mode,
                    ) { vm.setThemeMode(mode) }
                }
            }
            item { SettingSection("Search engine") }
            item {
                com.servodroid.browser.model.SearchEngine.ALL.forEach { engine ->
                    SelectableRow(title = engine.name, selected = state.searchEngine.name == engine.name) {
                        vm.setSearchEngine(engine.name)
                    }
                }
            }
            item { SettingSection("Privacy") }
            item {
                SettingsRow(title = "Clear browsing data", tint = Ink.Danger) { confirmClear = true }
            }
            item { SettingSection("Engine") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpaceLg, vertical = Dimensions.SpaceSm),
                    shape = RoundedCornerShape(Dimensions.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(Dimensions.SpaceLg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(if (state.engineAvailable) Ink.Accent else MaterialTheme.colorScheme.onSurfaceVariant),
                            )
                            Spacer(Modifier.width(Dimensions.SpaceSm))
                            Text(state.engineName, style = AppTypography.Title, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(Dimensions.SpaceSm))
                        Text(
                            text = if (state.engineAvailable) "libservoshell.so loaded · native renderer ready"
                            else "Engine binary not available. Build libservoshell.so with ./mach build --android.",
                            style = AppTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { SettingSection("About") }
            item { SettingsRow(title = "About Servo Browser", icon = Rounded.Info) { vm.openScreen(Screen.About) } }
            item { Spacer(Modifier.height(Dimensions.SpaceXxl)) }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear browsing data", style = AppTypography.Title) },
            text = { Text("This removes history and bookmarks stored on this device.", style = AppTypography.Body) },
            confirmButton = { TextButton(onClick = { vm.clearBrowsingData(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingSection(title: String) {
    Text(
        text = title,
        style = AppTypography.Label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Dimensions.SpaceLg, top = Dimensions.SpaceLg, bottom = Dimensions.SpaceSm),
    )
}

@Composable
private fun SettingLabel(title: String) {
    Text(
        text = title,
        style = AppTypography.Caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Dimensions.SpaceLg, top = Dimensions.SpaceSm, bottom = Dimensions.SpaceXs),
    )
}

@Composable
private fun SelectableRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(horizontal = Dimensions.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (selected) Icon(Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsRow(title: String, icon: ImageVector? = null, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = Dimensions.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Dimensions.SpaceMd))
        }
        Text(title, style = AppTypography.Body, color = tint, modifier = Modifier.weight(1f))
        Icon(Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun AboutScreen(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "About", onBack = { vm.openScreen(Screen.Browser) })
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimensions.SpaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Ink.Accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Rounded.Settings, contentDescription = "Servo", tint = Ink.Accent, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(Dimensions.SpaceLg))
            Text("Servo Browser", style = AppTypography.Headline, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Dimensions.SpaceXs))
            Text("Version 0.1.0", style = AppTypography.MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimensions.SpaceXl))
            Text(
                text = "An independent Android browser built on the Servo engine. Servo is a lightweight, high-performance web engine created by the open-source community.",
                style = AppTypography.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimensions.SpaceLg))
            Text(
                text = "Servo · MPL-2.0",
                style = AppTypography.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BrowserMenu(vm: BrowserViewModel, onShare: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { vm.closeMenu() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = Dimensions.RadiusSheet, topEnd = Dimensions.RadiusSheet),
    ) {
        Column(modifier = Modifier.padding(bottom = Dimensions.SpaceXl)) {
            MenuItem(Rounded.Add, "New tab") { vm.newTab(); vm.closeMenu() }
            MenuItem(Rounded.VisibilityOff, "New private tab") { vm.newTab(private = true); vm.closeMenu() }
            MenuItem(Rounded.History, "History") { vm.openScreen(Screen.History) }
            MenuItem(Rounded.BookmarkBorder, "Bookmarks") { vm.openScreen(Screen.Bookmarks) }
            MenuItem(Rounded.Download, "Downloads") { vm.openScreen(Screen.Downloads) }
            Hairline()
            MenuItem(Rounded.Search, "Find in page") { vm.toggleFind(true) }
            MenuItem(
                if (vm.uiState.desktopSite) Rounded.Check else Rounded.DesktopWindows,
                if (vm.uiState.desktopSite) "Desktop site on" else "Desktop site",
            ) { vm.toggleDesktopSite() }
            MenuItem(Rounded.Share, "Share") { onShare(); vm.closeMenu() }
            Hairline()
            MenuItem(Rounded.Settings, "Settings") { vm.openScreen(Screen.Settings) }
            MenuItem(Rounded.Info, "About Servo Browser") { vm.openScreen(Screen.About) }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = Dimensions.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Dimensions.SpaceLg))
        Text(label, style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurface)
    }
}

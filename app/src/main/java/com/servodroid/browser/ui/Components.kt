package com.servodroid.browser.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servodroid.browser.BrowserViewModel
import com.servodroid.browser.model.Screen
import com.servodroid.browser.model.displayHost
import com.servodroid.browser.ui.theme.AppTypography
import com.servodroid.browser.ui.theme.Dimensions
import com.servodroid.browser.ui.theme.Ink

@Composable
fun ChromeIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(Dimensions.TouchTarget),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(Dimensions.IconSize),
        )
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    )
}

@Composable
fun TopToolbar(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    val private = vm.activeTab?.isPrivate == true
    val barColor = if (private) Ink.PrivateSurface else MaterialTheme.colorScheme.surface
    val accent = if (private) Ink.PrivateAccent else Ink.Accent

    Box(modifier = modifier.fillMaxWidth().height(Dimensions.ToolbarHeight)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimensions.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabCountButton(count = state.tabs.size, private = private, accent = accent) {
                vm.openScreen(Screen.Tabs)
            }
            Spacer(Modifier.width(Dimensions.SpaceSm))
            AddressBar(vm = vm, accent = accent, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Dimensions.SpaceSm))
            ChromeIconButton(
                icon = Icons.Rounded.MoreVert,
                label = "Menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { vm.toggleMenu() },
            )
        }
        ProgressLine(
            progress = state.progress,
            color = accent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TabCountButton(count: Int, private: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(Dimensions.RadiusControl))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(1.dp, if (private) accent.copy(alpha = 0.7f) else Color.Transparent),
                RoundedCornerShape(Dimensions.RadiusControl),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = AppTypography.Label.copy(fontSize = 13.sp),
            color = if (private) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AddressBar(vm: BrowserViewModel, accent: Color, modifier: Modifier = Modifier) {
    val state = vm.uiState
    if (state.isEditingAddress) {
        EditingAddressBar(vm = vm, accent = accent, modifier = modifier)
    } else {
        DisplayAddressBar(vm = vm, modifier = modifier)
    }
}

@Composable
private fun DisplayAddressBar(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    val isSecure = state.currentUrl.startsWith("https://", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(Dimensions.RadiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(Dimensions.AddressBarHeight)
            .clickable { vm.startAddressEditing() },
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = Dimensions.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSecure) Icons.Rounded.Lock
                else Icons.Rounded.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(Dimensions.SpaceSm))
            Text(
                text = if (state.currentUrl.isEmpty()) "Search or enter address"
                else prettyUrl(state.currentUrl),
                style = AppTypography.Mono,
                color = if (state.currentUrl.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Dimensions.SpaceSm))
            if (state.isLoading) {
                CircularProgressIndicator(
                    progress = { (state.progress / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { vm.reload() },
                )
            }
        }
    }
}

@Composable
private fun EditingAddressBar(vm: BrowserViewModel, accent: Color, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        shape = RoundedCornerShape(Dimensions.RadiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(Dimensions.AddressBarHeight),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = Dimensions.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Dimensions.SpaceSm))
            BasicTextField(
                value = vm.uiState.addressText,
                onValueChange = { vm.onAddressChange(it) },
                singleLine = true,
                textStyle = AppTypography.Mono.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        vm.navigate(vm.uiState.addressText)
                        keyboard?.hide()
                    },
                ),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                decorationBox = { inner ->
                    if (vm.uiState.addressText.isEmpty()) {
                        Text(
                            text = "Search or enter address",
                            style = AppTypography.Mono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
            if (vm.uiState.addressText.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { vm.onAddressChange("") },
                )
            }
        }
    }
}

@Composable
fun ProgressLine(progress: Int, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = if (progress in 1..99) progress / 100f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .graphicsLayer {
                scaleX = animated
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            }
            .background(color),
    )
}

@Composable
fun BottomToolbar(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    val private = vm.activeTab?.isPrivate == true
    val tint = if (private) Ink.PrivateAccent else MaterialTheme.colorScheme.onSurfaceVariant
    val enabledTint = if (private) tint else MaterialTheme.colorScheme.onSurface

    Surface(
        color = if (private) Ink.PrivateSurface else MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth().height(Dimensions.BottomBarHeight),
    ) {
        Column {
            Hairline()
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChromeIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    label = "Back",
                    enabled = state.canGoBack,
                    tint = enabledTint,
                    onClick = { vm.goBack() },
                )
                ChromeIconButton(
                    icon = Icons.Rounded.ArrowForward,
                    label = "Forward",
                    enabled = state.canGoForward,
                    tint = enabledTint,
                    onClick = { vm.goForward() },
                )
                ChromeIconButton(
                    icon = Icons.Rounded.Home,
                    label = "Home",
                    tint = tint,
                    onClick = { vm.navigate("https://servo.org/") },
                )
                ChromeIconButton(
                    icon = if (state.isBookmarked) Icons.Rounded.Bookmark
                    else Icons.Rounded.BookmarkBorder,
                    label = if (state.isBookmarked) "Remove bookmark" else "Bookmark",
                    tint = if (state.isBookmarked) (if (private) tint else Ink.Accent) else tint,
                    onClick = { vm.toggleBookmark() },
                )
                ChromeIconButton(
                    icon = Icons.Rounded.Share,
                    label = "Share",
                    tint = tint,
                    onClick = { /* handled by BrowserApp via share intent hook */ vm.toggleMenu() },
                )
            }
        }
    }
}

@Composable
fun FindBar(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Dimensions.SpaceMd)) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Dimensions.SpaceSm))
            BasicTextField(
                value = vm.uiState.findQuery,
                onValueChange = { vm.onFindQueryChange(it) },
                singleLine = true,
                textStyle = AppTypography.Body.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (vm.uiState.findQuery.isEmpty()) {
                        Text("Find in page", style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                },
            )
            ChromeIconButton(
                icon = Icons.Rounded.Close,
                label = "Close find",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { vm.toggleFind(false) },
            )
        }
    }
}

private fun prettyUrl(url: String): String {
    val host = displayHost(url)
    return if (url == "about:blank") "New tab" else host
}

@Composable
fun NewTabPage(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val state = vm.uiState
    val private = vm.activeTab?.isPrivate == true
    val accent = if (private) Ink.PrivateAccent else Ink.Accent

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))

        Text(
            text = "Servo",
            style = AppTypography.Display.copy(fontSize = 40.sp, lineHeight = 44.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "an independent web engine",
            style = AppTypography.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimensions.SpaceXs),
        )

        Spacer(Modifier.height(Dimensions.SpaceXxl))

        Surface(
            shape = RoundedCornerShape(Dimensions.RadiusPill),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { vm.startAddressEditing() },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Dimensions.SpaceLg),
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(Dimensions.SpaceMd))
                Text(
                    text = "Search the web",
                    style = AppTypography.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Dimensions.SpaceXxl))

        Text(
            text = "Frequently visited",
            style = AppTypography.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(Dimensions.SpaceMd))

        val shortcuts = listOf(
            Triple("Servo", "S", "https://servo.org/"),
            Triple("Wikipedia", "W", "https://en.wikipedia.org/"),
            Triple("MDN", "M", "https://developer.mozilla.org/"),
            Triple("GitHub", "G", "https://github.com/"),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            shortcuts.forEach { (label, letter, url) ->
                Shortcut(label = label, letter = letter, accent = accent) { vm.navigate(url) }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (state.engineAvailable) accent else MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Spacer(Modifier.width(Dimensions.SpaceSm))
            Text(
                text = if (state.engineAvailable) "Servo engine active" else "Using platform renderer",
                style = AppTypography.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Dimensions.SpaceLg))
    }
}

@Composable
private fun Shortcut(label: String, letter: String, accent: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = letter, style = AppTypography.Title, color = accent)
        }
        Spacer(Modifier.height(Dimensions.SpaceSm))
        Text(
            text = label,
            style = AppTypography.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SuggestionsCard(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val query = vm.uiState.addressText.trim()
    if (query.isEmpty()) return
    val matches = vm.uiState.history
        .filter { it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true) }
        .take(4)

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimensions.SpaceSm),
        shape = RoundedCornerShape(Dimensions.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column {
            SuggestionRow(
                icon = Icons.Rounded.Search,
                title = "Search for \"$query\"",
                subtitle = "with ${vm.uiState.searchEngine.name}",
            ) { vm.navigate(query) }
            matches.forEach { entry ->
                SuggestionRow(
                    icon = Icons.Rounded.History,
                    title = entry.title.ifBlank { displayHost(entry.url) },
                    subtitle = displayHost(entry.url),
                ) { vm.navigate(entry.url) }
            }
        }
    }
}

@Composable
private fun SuggestionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(Dimensions.SpaceMd))
        Column {
            Text(text = title, style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = AppTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

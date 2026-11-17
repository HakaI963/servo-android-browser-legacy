package com.servodroid.browser.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Minimal contract the browser UI uses to drive a rendering engine. */
interface BrowserEngine {
    val displayName: String
    val isNative: Boolean

    fun loadUrl(url: String)
    fun reload()
    fun stop()
    fun goBack()
    fun goForward()
    fun setDesktopSite(enabled: Boolean)
    fun findInPage(query: String)
    fun setListener(listener: Listener)
    fun onResume()
    fun onPause()
    fun destroy()

    /** Forward hardware key events to the engine (no-op for engines that do not need it). */
    fun onKeyEvent(keyCode: Int, unicode: Int, isDown: Boolean) {}

    /** Engine-specific rendering surface (Servo surface or platform WebView). */
    @Composable
    fun Content(modifier: Modifier)

    interface Listener {
        fun onPageStarted(url: String) {}
        fun onPageFinished(url: String, title: String) {}
        fun onTitleChanged(title: String) {}
        fun onUrlChanged(url: String) {}
        fun onProgress(progress: Int) {}
        fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean) {}
        fun onAlert(message: String) {}
    }
}

object EngineFactory {
    /** Creates a Servo engine when libservoshell.so is present, otherwise a WebView fallback. */
    fun create(): BrowserEngine =
        if (ServoEngine.isAvailable()) ServoEngine() else SystemEngine()
}

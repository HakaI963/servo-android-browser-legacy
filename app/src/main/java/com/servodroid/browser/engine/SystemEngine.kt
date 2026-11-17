package com.servodroid.browser.engine

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Development fallback used when libservoshell.so is not bundled. It keeps the browser UI fully
 * usable while the Servo engine binary is being built. Production builds replace it with
 * [ServoEngine] automatically through [EngineFactory].
 */
class SystemEngine : BrowserEngine {

    override val displayName: String = "Platform renderer"
    override val isNative: Boolean = false

    private var listener: BrowserEngine.Listener? = null
    private var webView: WebView? = null
    private var pendingUrl: String? = null

    private val client = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { listener?.onPageStarted(it) }
            listener?.onProgress(20)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val safeUrl = url.orEmpty()
            listener?.onProgress(100)
            listener?.onPageFinished(safeUrl, view?.title.orEmpty())
            if (view != null) {
                listener?.onHistoryChanged(view.canGoBack(), view.canGoForward())
            }
        }
    }

    private val chromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            listener?.onProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            title?.let { listener?.onTitleChanged(it) }
        }

        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: android.webkit.JsResult?,
        ): Boolean {
            val ctx = view?.context ?: return false
            AlertDialog.Builder(ctx)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                .setOnCancelListener { result?.cancel() }
                .show()
            return true
        }
    }

    override fun setListener(listener: BrowserEngine.Listener) {
        this.listener = listener
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(view: WebView) {
        view.webViewClient = client
        view.webChromeClient = chromeClient
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView = view
        pendingUrl?.let { view.loadUrl(it); pendingUrl = null }
    }

    override fun loadUrl(url: String) {
        val view = webView
        if (view == null) pendingUrl = url else view.loadUrl(url)
    }

    override fun reload() = webView?.reload() ?: Unit
    override fun stop() = webView?.stopLoading() ?: Unit
    override fun goBack() = webView?.goBack() ?: Unit
    override fun goForward() = webView?.goForward() ?: Unit

    override fun setDesktopSite(enabled: Boolean) {
        webView?.settings?.userAgentString = if (enabled) DESKTOP_UA else null
        webView?.reload()
    }

    override fun findInPage(query: String) {
        webView?.findAll(query)
    }

    override fun onResume() = webView?.resumeTimers() ?: Unit
    override fun onPause() = webView?.pauseTimers() ?: Unit
    override fun destroy() {
        webView?.destroy()
        webView = null
    }

    @Composable
    override fun Content(modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { ctx: Context -> WebView(ctx).also { configure(it) } },
        )
    }

    private companion object {
        const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
    }
}

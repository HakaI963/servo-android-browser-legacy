package com.servodroid.browser.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.servo.servoview.JNIServo
import java.util.concurrent.Executors

/**
 * Drives the native Servo shell. All JNI calls are marshalled onto a single GL thread, matching
 * Servo's requirement that every call originate from the same thread.
 */
class ServoEngine : BrowserEngine {

    override val displayName: String = "Servo"
    override val isNative: Boolean = true

    private val glDispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "servo-gl") }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: BrowserEngine.Listener? = null
    private var jni: JNIServo? = null
    private var initialized = false
    private var alive = true
    private var pendingUrl: String? = null
    private var desktop = false

    private val callbacks = object : JNIServo.Callbacks {
        override fun wakeup() {
            glDispatcher.execute { jni?.performUpdates() }
        }
        override fun onAlert(message: String) = notify { it.onAlert(message) }
        override fun onLoadStarted() {
            notify { it.onProgress(20) }
        }
        override fun onLoadEnded() {
            notify { it.onProgress(100) }
        }
        override fun onTitleChanged(title: String) = notify { it.onTitleChanged(title) }
        override fun onUrlChanged(url: String) = notify { it.onUrlChanged(url) }
        override fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean) =
            notify { it.onHistoryChanged(canGoBack, canGoForward) }
        override fun onImeShow() = Unit
        override fun onImeHide() = Unit
    }

    init {
        startFrameLoop()
    }

    private fun notify(block: (BrowserEngine.Listener) -> Unit) {
        mainHandler.post { listener?.let(block) }
    }

    private fun engine(block: (JNIServo) -> Unit) {
        glDispatcher.execute { jni?.let(block) }
    }

    /** Called once from [Content] when a Context is available. */
    private fun attach(context: Context) {
        if (initialized) return
        initialized = true
        glDispatcher.execute {
            val instance = JNIServo()
            jni = instance
            try {
                instance.init(
                    context.applicationContext,
                    "[]",
                    pendingUrl,
                    "servo",
                    false,
                    callbacks,
                )
                pendingUrl = null
            } catch (e: Throwable) {
                notify { it.onAlert("Servo engine failed to initialize: ${e.message}") }
            }
        }
    }

    private fun startFrameLoop() {
        mainHandler.post {
            val callback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!alive) return
                    engine { it.doFrame() }
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
            Choreographer.getInstance().postFrameCallback(callback)
        }
    }

    override fun setListener(listener: BrowserEngine.Listener) {
        this.listener = listener
    }

    override fun loadUrl(url: String) {
        pendingUrl = url
        engine { it.loadUri(url) }
    }

    override fun reload() = engine { it.reload() }
    override fun stop() = engine { it.stop() }
    override fun goBack() = engine { it.goBack() }
    override fun goForward() = engine { it.goForward() }

    override fun setDesktopSite(enabled: Boolean) {
        desktop = enabled
        // Servo does not expose a UA override in the JNI shell yet; reload to apply preferences.
        engine { it.reload() }
    }

    override fun findInPage(query: String) = Unit

    override fun onKeyEvent(keyCode: Int, unicode: Int, isDown: Boolean) {
        engine { if (isDown) it.keydown(keyCode, unicode) else it.keyup(keyCode, unicode) }
    }

    // Painting is tied to the Surface lifecycle below; there is no separate resume signal here.
    override fun onResume() = Unit
    override fun onPause() = Unit

    override fun destroy() {
        alive = false
        glDispatcher.shutdownNow()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val context = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(Unit) { attach(context) }

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    isFocusable = true
                    isClickable = true
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            val size = Size(width.coerceAtLeast(1), height.coerceAtLeast(1))
                            engine {
                                it.addPlatformWindow(
                                    size,
                                    ctx.resources.displayMetrics.density,
                                    holder.surface,
                                )
                            }
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int,
                        ) {
                            engine { it.resize(Size(width.coerceAtLeast(1), height.coerceAtLeast(1))) }
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            engine { it.pausePainting() }
                        }
                    })

                    setOnTouchListener { _, event ->
                        val x = event.x
                        val y = event.y
                        val id = if (event.pointerCount > 0) event.getPointerId(0) else 0
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> engine { it.touchDown(x, y, id) }
                            MotionEvent.ACTION_UP -> engine { it.touchUp(x, y, id) }
                            MotionEvent.ACTION_MOVE -> engine { it.touchMove(x, y, id) }
                            MotionEvent.ACTION_CANCEL -> engine { it.touchCancel(x, y, id) }
                        }
                        true
                    }
                }
            },
        )
    }

    companion object {
        fun isAvailable(): Boolean = JNIServo.tryLoad()
    }
}

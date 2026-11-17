package org.servo.servoview

import android.content.Context
import android.util.Size
import android.view.Surface

/**
 * JNI bridge to Servo's Android shell (`libservoshell.so`).
 *
 * These declarations intentionally live in the `org.servo.servoview` package and are named
 * `JNIServo`, because upstream Servo exports its native entry points as
 * `Java_org_servo_servoview_JNIServo_*` from `ports/servoshell/egl/android/mod.rs`. Keeping the
 * package and class names identical lets this app load a stock Servo engine binary unchanged.
 *
 * Every call must be made from the same thread; [ServoEngine] marshals calls onto a dedicated
 * GL dispatcher for that reason.
 */
internal class JNIServo {

    external fun init(
        context: Context,
        args: String?,
        url: String?,
        logStr: String?,
        experimentalMode: Boolean,
        callbacks: Callbacks,
    )

    external fun addPlatformWindow(size: Size, density: Float, surface: Surface)
    external fun performUpdates()
    external fun resize(size: Size)
    external fun loadUri(uri: String)
    external fun reload()
    external fun stop()
    external fun goBack()
    external fun goForward()
    external fun keydown(keycode: Int, unicode: Int)
    external fun keyup(keycode: Int, unicode: Int)
    external fun touchDown(x: Float, y: Float, pointerId: Int)
    external fun touchUp(x: Float, y: Float, pointerId: Int)
    external fun touchMove(x: Float, y: Float, pointerId: Int)
    external fun touchCancel(x: Float, y: Float, pointerId: Int)
    external fun pausePainting()
    external fun resumePainting(surface: Surface, size: Size)
    external fun setExperimentalMode(enable: Boolean)
    external fun doFrame()

    /** Callbacks invoked by the native shell on the Servo thread. */
    interface Callbacks {
        fun wakeup()
        fun onAlert(message: String)
        fun onLoadStarted()
        fun onLoadEnded()
        fun onTitleChanged(title: String)
        fun onUrlChanged(url: String)
        fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean)
        fun onImeShow()
        fun onImeHide()
    }

    companion object {
        private var loaded = false

        /** @return true when the native library was found and loaded. */
        @JvmStatic
        fun tryLoad(): Boolean {
            if (loaded) return true
            loaded = try {
                System.loadLibrary("servoshell")
                true
            } catch (_: UnsatisfiedLinkError) {
                false
            }
            return loaded
        }
    }
}

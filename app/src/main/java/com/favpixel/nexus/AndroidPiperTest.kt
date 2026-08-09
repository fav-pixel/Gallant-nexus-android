// git path: app/src/main/java/com/favpixel/nexus/AndroidPiperTest.kt
package com.favpixel.nexus

import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * JS bridge exposed to the WebView purely to test PiperEngine from inside
 * the app — window.AndroidPiperTest.run() from any page. See
 * PiperEngine.kt for exactly what this does and doesn't prove.
 *
 * Registered in MainActivity via:
 *   webView.addJavascriptInterface(AndroidPiperTest(this), "AndroidPiperTest")
 *
 * Reports progress at each stage rather than one final Toast — if
 * something crashes the whole app process (a native-level failure inside
 * ONNX Runtime's C++ layer, which a Kotlin try/catch can't stop or even
 * see), whichever Toast was shown last is the only diagnostic available
 * without a proper logcat reader. Each stage's own try/catch still
 * catches ordinary Kotlin-level exceptions cleanly, same as before.
 *
 * Not meant to be permanent — once a real phonemizer decision is made,
 * this gets replaced by whatever the actual "speak this text" bridge
 * becomes.
 */
class AndroidPiperTest(private val activity: MainActivity) {

    @JavascriptInterface
    fun run() {
        val engine = PiperEngine(activity)

        toast("Piper: preparing model (first run only, copies it to storage)…")
        try {
            engine.loadIfNeeded()
        } catch (e: Exception) {
            toast("Piper: failed loading model — ${e.message}")
            return
        }

        toast("Piper: model loaded, running inference…")
        val pcm = try {
            engine.synthesizeTestTone()
        } catch (e: Exception) {
            toast("Piper: failed during inference — ${e.message}")
            return
        }

        toast("Piper: inference done (${pcm.size} samples), playing…")
        try {
            engine.playPcm(pcm)
        } catch (e: Exception) {
            toast("Piper: failed during playback — ${e.message}")
            return
        }

        toast("Piper: pipeline complete — check for sound")
    }

    private fun toast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}

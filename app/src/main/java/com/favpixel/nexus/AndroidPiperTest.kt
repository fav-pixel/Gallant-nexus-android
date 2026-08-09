// git path: app/src/main/java/com/favpixel/nexus/AndroidPiperTest.kt
package com.favpixel.nexus

import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * JS bridge exposed to the WebView purely to test PiperEngine from inside
 * the app — window.AndroidPiperTest.run() from any page. See
 * PiperEngine.kt for exactly what this does and doesn't prove; this class
 * is just the trigger + a Toast reporting success or failure, same
 * feedback style as AndroidDownloader and AndroidNotifications.
 *
 * Registered in MainActivity via:
 *   webView.addJavascriptInterface(AndroidPiperTest(this), "AndroidPiperTest")
 *
 * Not meant to be permanent — once a real phonemizer decision is made,
 * this gets replaced by whatever the actual "speak this text" bridge
 * becomes.
 */
class AndroidPiperTest(private val activity: MainActivity) {

    @JavascriptInterface
    fun run() {
        try {
            PiperEngine(activity).playTestTone()
            activity.runOnUiThread {
                Toast.makeText(activity, "Piper pipeline ran — check for sound", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Piper test failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

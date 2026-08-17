// git path: app/src/main/java/com/favpixel/nexus/AndroidSpeech.kt
package com.favpixel.nexus

import android.webkit.JavascriptInterface

/**
 * Native speech-to-text bridge. It launches Android's existing recognizer;
 * Nexus does not bundle a voice model or send audio directly to a privileged
 * service. Results are returned through the fixed window.NexusSpeech channel.
 */
class AndroidSpeech(private val activity: MainActivity) {

    @JavascriptInterface
    fun startListening() {
        activity.runOnUiThread { activity.startSpeechRecognition() }
    }
}

// git path: app/src/main/java/com/favpixel/nexus/AndroidNotifications.kt
package com.favpixel.nexus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * JS bridge exposed to the WebView so pages inside Nexus can show a real
 * Android notification — same shape as AndroidDownloader, and the same
 * reason it has to exist: nothing in a WebView can post a system
 * notification on its own, so the page hands the content across a bridge
 * and the host app does it natively.
 *
 * Registered in MainActivity via:
 *   webView.addJavascriptInterface(AndroidNotifications(this), "AndroidNotifications")
 *
 * IMPORTANT — what this is and isn't: this only fires while the WebView
 * page that calls it is actually running (foreground or just-backgrounded,
 * same as any other JS in the page). It cannot fire on its own, on a
 * schedule, or while the app is fully closed — that's a fundamentally
 * different mechanism (Firebase Cloud Messaging, a device push token, and
 * a server deciding when to send) and hasn't been built yet. This bridge
 * is the "can we show a notification at all" foundation piece; Sovereign
 * proactively briefing you or Unchained's daily digest reaching you when
 * the app isn't open both still need that FCM piece on top of this.
 *
 * @JavascriptInterface methods run on a background thread, not the UI
 * thread — notification posting itself is thread-safe, so no
 * runOnUiThread wrapping is needed here (unlike AndroidDownloader, which
 * touches Toast and permission requests that do require the UI thread).
 */
class AndroidNotifications(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "nexus_general"
        private const val CODE_RED_CHANNEL_ID = "nexus_code_red"
    }

    @JavascriptInterface
    fun notify(title: String, body: String) {
        postNotification(CHANNEL_ID, title, body, NotificationCompat.PRIORITY_DEFAULT)
    }

    /**
     * Displays an emergency notification only after a trusted AEGIS page has
     * already completed its server-side administrator check. This method does
     * not authenticate, open CODE RED, or expose incident evidence.
     */
    @JavascriptInterface
    fun notifyCodeRed(title: String, body: String) {
        postNotification(CODE_RED_CHANNEL_ID, title, body, NotificationCompat.PRIORITY_HIGH)
    }

    private fun postNotification(channelId: String, title: String, body: String, priority: Int) {
        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        // Below Android 13 this permission doesn't exist as a runtime
        // grant at all, so checkSelfPermission would incorrectly report
        // it as not granted — only enforce the check on 13+, where it's
        // real and MainActivity actually requests it on launch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted) return

        ensureChannel(channelId)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(priority)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    // Notification channels are required on Android 8+ (API 26) before any
    // notification can show at all — created lazily on first use rather
    // than in an Application subclass, since this app doesn't have one yet
    // and one channel is all that's needed so far.
    private fun ensureChannel(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) != null) return

        val isCodeRed = channelId == CODE_RED_CHANNEL_ID
        val channel = NotificationChannel(
            channelId,
            if (isCodeRed) "Nexus CODE RED" else "Nexus ecosystem",
            if (isCodeRed) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = if (isCodeRed) {
                "Administrator-approved emergency status from AEGIS"
            } else {
                "Messages and updates from the Nexus ecosystem"
            }
        }
        manager.createNotificationChannel(channel)
    }
}

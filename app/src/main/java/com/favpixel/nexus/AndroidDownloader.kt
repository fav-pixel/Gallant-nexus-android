// git path: app/src/main/java/com/favpixel/nexus/AndroidDownloader.kt
package com.favpixel.nexus

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * JS bridge exposed to the WebView so pages inside Nexus — Yelena's Tier 1
 * file cards, the "Save to phone" fallback, the Artifacts dashboard — can
 * hand off a file to be saved natively.
 *
 * This exists because Yelena's downloads all work via
 * URL.createObjectURL(blob) + a fake <a download> click. That's a normal
 * browser trick, but a blob: URL only exists inside the page's own
 * JavaScript memory — a native android.webkit.DownloadListener only ever
 * receives a URL *string*, and there is no real network resource behind a
 * blob: URL for it to fetch. So a DownloadListener alone can never catch
 * these downloads, no matter what storage permission is granted. The page
 * has to push the actual bytes across a bridge instead — which is what
 * this class is for.
 *
 * Registered in MainActivity via:
 *   webView.addJavascriptInterface(AndroidDownloader(this), "AndroidDownloader")
 *
 * Yelena's client code checks `window.AndroidDownloader` before using this
 * — see client/src/lib/saveFile.ts in the Yelena repo — so this only ever
 * activates inside the wrapped app; Yelena keeps behaving like a normal
 * website everywhere else (regular mobile browser, desktop, etc).
 *
 * @JavascriptInterface methods are called on a background thread, not the
 * UI thread — every UI-touching call here (Toast, permission request) is
 * wrapped in runOnUiThread accordingly.
 */
class AndroidDownloader(private val activity: MainActivity) {

    @JavascriptInterface
    fun saveFile(filename: String, base64Content: String) {
        try {
            val bytes = Base64.decode(base64Content, Base64.DEFAULT)
            // Strip any accidental path components — this only ever writes
            // into the public Downloads location, never an arbitrary path.
            val safeName = filename.substringAfterLast('/').ifBlank { "yelena-file" }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(safeName, bytes)
            } else {
                saveLegacy(safeName, bytes)
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Couldn't save $filename: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Android 10+ (API 29+) — scoped storage. Writing into the public
    // Downloads collection through MediaStore needs no runtime permission
    // at all, which is why AndroidManifest only declares
    // WRITE_EXTERNAL_STORAGE with maxSdkVersion="28" — modern Android
    // never even asks for it.
    private fun saveViaMediaStore(filename: String, bytes: ByteArray) {
        val resolver = activity.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("MediaStore refused the file")

        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        activity.runOnUiThread {
            Toast.makeText(activity, "Saved \"$filename\" to Downloads", Toast.LENGTH_SHORT).show()
        }
    }

    // Android 9 and below — pre-scoped-storage, needs the legacy runtime
    // permission. requestStoragePermission (in MainActivity) re-invokes
    // this same save once the user grants it.
    private fun saveLegacy(filename: String, bytes: ByteArray) {
        activity.runOnUiThread {
            activity.withStoragePermission {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)
                FileOutputStream(file).use { it.write(bytes) }
                Toast.makeText(activity, "Saved \"$filename\" to Downloads", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

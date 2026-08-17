// git path: app/src/main/java/com/favpixel/nexus/AndroidCodeRed.kt
package com.favpixel.nexus

import android.webkit.JavascriptInterface

/**
 * Safe Nexus-side CODE RED bridge.
 *
 * This bridge only routes the authenticated user to AEGIS. It does not
 * authenticate the user, inspect admin state, store a passphrase, or carry
 * any Supabase/GitHub credential. The AEGIS server remains the authority and
 * must verify the administrator profile before opening an incident.
 */
class AndroidCodeRed(private val activity: MainActivity) {

    @JavascriptInterface
    fun openAegisConsole(): String {
        activity.runOnUiThread { activity.openAegisCodeRed() }
        return "SERVER_ADMIN_CHECK_REQUIRED"
    }
}

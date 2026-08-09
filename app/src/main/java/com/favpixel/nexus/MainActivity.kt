package com.favpixel.nexus

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * A single-WebView shell around the whole ecosystem. Nexus is the home
 * screen; tapping any card inside it navigates this same WebView to
 * NeuroArchive, Question Studio, or Yelena — not a separate browser tab —
 * and the Android back button walks back through that history the same
 * way browser back would, eventually returning to Nexus before exiting
 * the app.
 */
class MainActivity : AppCompatActivity() {

    // Change this if you ever move Nexus to a different URL.
    private val homeUrl = "https://gallantnexus-chi.vercel.app/"

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View

    // Holds the pending mic permission request from the WebView while we
    // ask the user for the native RECORD_AUDIO permission, and the pending
    // file-chooser callback while the user picks a file (for Yelena's
    // PDF/Image/Other-files attachment buttons — WebView can't show a file
    // picker on its own, the host app has to provide one).
    private var pendingMicRequest: PermissionRequest? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    // Holds the pending action to run once legacy storage permission is
    // granted (only asked for on Android 9 and below — see
    // AndroidDownloader.kt, which is what actually calls
    // withStoragePermission below).
    private var pendingStorageAction: (() -> Unit)? = null

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val action = pendingStorageAction
            pendingStorageAction = null
            if (granted) action?.invoke()
        }

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val request = pendingMicRequest
            pendingMicRequest = null
            if (request == null) return@registerForActivityResult
            if (granted) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
        }

    // Asked once on first launch (Android 13+ only — see manifest comment).
    // Nothing currently depends on the result: this just gets the
    // permission in place ahead of time so future work (a local
    // notification bridge, or eventually FCM push for Sovereign/Unchained
    // proactively reaching out) doesn't also need to solve "how do we ask
    // for this" from scratch.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = pendingFileCallback
            pendingFileCallback = null
            if (callback == null) return@registerForActivityResult
            val data = result.data
            val uris = if (result.resultCode == RESULT_OK && data != null) {
                val clipData = data.clipData
                if (clipData != null) {
                    Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                } else {
                    data.data?.let { arrayOf(it) } ?: arrayOf()
                }
            } else {
                arrayOf()
            }
            callback.onReceiveValue(uris)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        setupWebView()
        setupBackNavigation()
        requestNotificationPermissionIfNeeded()

        if (savedInstanceState == null) {
            webView.loadUrl(homeUrl)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // required — Supabase session persists via localStorage
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }

        // Exposes window.AndroidDownloader to every page this WebView loads
        // (Nexus, NeuroArchive, Question Studio, Yelena — same shared
        // WebView). Only Yelena's client code actually calls it; see
        // AndroidDownloader.kt for why this exists instead of a plain
        // DownloadListener.
        webView.addJavascriptInterface(AndroidDownloader(this), "AndroidDownloader")
        // Exposes window.AndroidNotifications.notify(title, body) — see
        // AndroidNotifications.kt for what this can and can't do yet.
        webView.addJavascriptInterface(AndroidNotifications(this), "AndroidNotifications")

        webView.webViewClient = object : WebViewClient() {
            // Every link — including ones written as target="_blank" — stays
            // inside this same WebView instead of trying to open a new window.
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadingOverlay.animate().cancel()
                loadingOverlay.alpha = 1f
                loadingOverlay.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                loadingOverlay.animate()
                    .alpha(0f)
                    .setDuration(350)
                    .withEndAction { loadingOverlay.visibility = View.GONE }
                    .start()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            // Yelena's Speak mode uses the browser's mic (getUserMedia).
            // The WebView itself can't grant that — it has to ask the host
            // app, which asks Android, which asks the person.
            override fun onPermissionRequest(request: PermissionRequest) {
                val wantsMic = request.resources.any {
                    it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                }
                if (!wantsMic) {
                    request.deny()
                    return
                }

                val alreadyGranted = ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (alreadyGranted) {
                    request.grant(request.resources)
                } else {
                    pendingMicRequest = request
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            // Yelena's PDF/Image/Other-files attachment buttons rely on a
            // plain <input type="file">, which WebView can't handle without
            // the host app stepping in to show a real file picker.
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback

                val intent = fileChooserParams.createIntent()
                try {
                    fileChooserLauncher.launch(intent)
                } catch (e: Exception) {
                    pendingFileCallback = null
                    return false
                }
                return true
            }
        }
    }

    // Called by AndroidDownloader on Android 9 and below only — API 29+
    // writes via MediaStore instead, which needs no permission at all.
    // Already guaranteed to run on the UI thread by the caller.
    fun withStoragePermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            action()
        } else {
            pendingStorageAction = action
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}

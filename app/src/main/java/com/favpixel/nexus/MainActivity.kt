package com.favpixel.nexus

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        setupWebView()
        setupBackNavigation()

        if (savedInstanceState == null) {
            webView.loadUrl(homeUrl)
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
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
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

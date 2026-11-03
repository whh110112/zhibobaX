package com.whh11.zb8lite.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.whh11.zb8lite.util.Injector

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DetailScreen(url: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    var progress by remember { mutableIntStateOf(0) }
    var web by remember { mutableStateOf<WebView?>(null) }

    // 全屏视频播放容器与回调
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // 物理返回键：优先退出全屏视频，其次 webview 历史返回，最后关闭页面
    BackHandler(enabled = true) {
        if (customVideoView != null) {
            customViewCallback?.onCustomViewHidden()
            customVideoView = null
            customViewCallback = null
        } else if (web?.canGoBack() == true) {
            web?.goBack()
        } else {
            onBack()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Zb8Colors.Bg)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Surface)
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (web?.canGoBack() == true) web?.goBack() else onBack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = Zb8Colors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    color = Zb8Colors.TextPrimary,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { web?.reload() }) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "刷新",
                        tint = Zb8Colors.TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(web?.url ?: url))
                    context.startActivity(intent)
                }) {
                    Icon(
                        Icons.Rounded.OpenInBrowser,
                        contentDescription = "浏览器打开",
                        tint = Zb8Colors.TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = Zb8Colors.Blue,
                    trackColor = Zb8Colors.SurfaceHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }

            Box(Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            val bgColor = if (isDark) 0xFF0B0E14.toInt() else 0xFFFFFFFF.toInt()
                            setBackgroundColor(bgColor)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = false
                                displayZoomControls = false
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = userAgentString.replace("; wv", "")
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                                    progress = 1
                                    v?.evaluateJavascript(Injector.script(isDark), null)
                                }

                                override fun onPageCommitVisible(v: WebView?, u: String?) {
                                    v?.evaluateJavascript(Injector.script(isDark), null)
                                }

                                override fun onPageFinished(v: WebView?, u: String?) {
                                    progress = 100
                                    v?.evaluateJavascript(Injector.script(isDark), null)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val target = request?.url?.toString().orEmpty()
                                    if (target.startsWith("http://") || target.startsWith("https://")) {
                                        return false
                                    }
                                    return true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(v: WebView?, newProgress: Int) {
                                    progress = newProgress
                                    if (newProgress in 15..95) {
                                        v?.evaluateJavascript(Injector.script(isDark), null)
                                    }
                                }

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    customVideoView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    customViewCallback?.onCustomViewHidden()
                                    customVideoView = null
                                    customViewCallback = null
                                }
                            }

                            loadUrl(url)
                            web = this
                        }
                    },
                    update = { view ->
                        val bgColor = if (isDark) 0xFF0B0E14.toInt() else 0xFFFFFFFF.toInt()
                        view.setBackgroundColor(bgColor)
                        view.evaluateJavascript(Injector.script(isDark), null)
                    }
                )
            }
        }

        // 全屏视频播放图层（覆盖在最顶层）
        if (customVideoView != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                factory = {
                    FrameLayout(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        addView(
                            customVideoView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            web?.stopLoading()
            web?.destroy()
            web = null
        }
    }
}

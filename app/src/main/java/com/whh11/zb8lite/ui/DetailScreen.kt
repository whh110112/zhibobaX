package com.whh11.zb8lite.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
    var progress by remember { mutableIntStateOf(0) }
    var web by remember { mutableStateOf<WebView?>(null) }

    // 修复返回键逻辑：优先 webview 内部后退，不能后退才关闭页面
    BackHandler(enabled = true) {
        if (web?.canGoBack() == true) {
            web?.goBack()
        } else {
            onBack()
        }
    }

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
                        setBackgroundColor(0xFF0B0E14.toInt())
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = userAgentString.replace("; wv", "")
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                                progress = 1
                                v?.evaluateJavascript(Injector.script(), null)
                            }

                            override fun onPageFinished(v: WebView?, u: String?) {
                                progress = 100
                                // 页面加载完毕执行注入脚本净化浮层与下载引导
                                v?.evaluateJavascript(Injector.script(), null)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val target = request?.url?.toString().orEmpty()
                                if (target.startsWith("http://") || target.startsWith("https://")) {
                                    return false
                                }
                                // 拦截并阻止各类调起外部 App/下载市场弹窗
                                return true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(v: WebView?, newProgress: Int) {
                                progress = newProgress
                                if (newProgress > 50) {
                                    v?.evaluateJavascript(Injector.script(), null)
                                }
                            }
                        }
                        loadUrl(url)
                        web = this
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

package com.whh11.zb8lite.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.zb8lite.data.LocalStore
import com.whh11.zb8lite.data.SavedZb8Article
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8ArticleDetail
import com.whh11.zb8lite.data.Zb8Block
import com.whh11.zb8lite.data.Zb8Comment
import kotlinx.coroutines.launch

/**
 * 直播吧纯原生新闻详情 + 原生「🔥 神评热议」与全量评论楼中楼！
 * 支持战术动图 GIF 原生播放与点击全屏缩放保存。
 */
@Composable
fun Zb8ArticleDetailScreen(
    url: String,
    fallbackTitle: String,
    onBack: () -> Unit,
    onOpenWeb: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gifLoader = rememberZb8GifLoader()

    var article by remember { mutableStateOf<Zb8ArticleDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var hotComments by remember { mutableStateOf<List<Zb8Comment>>(emptyList()) }
    var comments by remember { mutableStateOf<List<Zb8Comment>>(emptyList()) }
    var commentTotal by remember { mutableIntStateOf(0) }
    var commentPage by remember { mutableIntStateOf(0) }
    var hasMoreComments by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }

    var viewerUrl by remember { mutableStateOf<String?>(null) }

    val favorites by LocalStore.favorites.collectAsState()
    val isFav = remember(favorites, url) { favorites.any { it.url == url } }

    suspend fun loadAll() {
        loading = true
        error = null
        runCatching { Zb8Api.articleDetail(url) }
            .onSuccess { d ->
                article = d
                LocalStore.recordHistory(SavedZb8Article(url, d.title.ifBlank { fallbackTitle }))
                if (d.filename.isNotBlank()) {
                    runCatching { Zb8Api.comments(d.filename, page = 0) }
                        .onSuccess { cp ->
                            hotComments = cp.hotComments
                            comments = cp.comments
                            commentTotal = cp.total
                            hasMoreComments = cp.hasMore
                            commentPage = 0
                        }
                }
            }
            .onFailure { error = it.message ?: "文章加载失败" }
        loading = false
    }

    LaunchedEffect(url) { loadAll() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Zb8Colors.Card)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Zb8Colors.TextPrimary)
            }
            Text(
                article?.source?.ifBlank { "直播吧" } ?: "直播吧",
                color = Zb8Colors.TextPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = {
                val saved = LocalStore.toggleFavorite(
                    SavedZb8Article(url, article?.title?.ifBlank { fallbackTitle } ?: fallbackTitle)
                )
                Toast.makeText(context, if (saved) "已收藏到本地" else "已取消收藏", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "收藏",
                    tint = if (isFav) Zb8Colors.Primary else Zb8Colors.TextSecondary
                )
            }
            IconButton(onClick = {
                val t = article?.title?.ifBlank { fallbackTitle } ?: fallbackTitle
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$t\n$url")
                }
                context.startActivity(Intent.createChooser(intent, "分享资讯"))
            }) {
                Icon(Icons.Outlined.Share, contentDescription = "分享", tint = Zb8Colors.TextSecondary)
            }
            IconButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = "浏览器打开", tint = Zb8Colors.TextSecondary)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        when {
            loading -> LoadingBox()
            error != null && article == null -> ErrorRetryBox(error!!, onRetry = { scope.launch { loadAll() } })
            else -> {
                val d = article!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item(key = "hdr") {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(
                                d.title.ifBlank { fallbackTitle },
                                color = Zb8Colors.TextPrimary,
                                fontSize = 20.5.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(d.source, color = Zb8Colors.Primary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                if (d.time.isNotBlank()) {
                                    Spacer(Modifier.width(10.dp))
                                    Text(d.time, color = Zb8Colors.TextTertiary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (d.blocks.isEmpty()) {
                        item(key = "web_fallback") {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Button(
                                    onClick = { onOpenWeb(url, d.title.ifBlank { fallbackTitle }) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Zb8Colors.Primary)
                                ) {
                                    Text("在内置播放器/网页中查看完整内容", color = Color.White)
                                }
                            }
                        }
                    } else {
                        items(d.blocks.size) { idx ->
                            when (val b = d.blocks[idx]) {
                                is Zb8Block.Paragraph -> Text(
                                    b.text,
                                    color = Zb8Colors.TextPrimary,
                                    fontSize = 16.sp,
                                    lineHeight = 27.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                                )
                                is Zb8Block.Image -> AsyncImage(
                                    model = b.url,
                                    imageLoader = gifLoader,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Zb8Colors.SurfaceHigh)
                                        .clickable { viewerUrl = b.url }
                                )
                            }
                        }
                    }

                    item(key = "sep") {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(8.dp).background(Zb8Colors.Bg))
                    }

                    // 🔥 神评热议
                    if (hotComments.isNotEmpty()) {
                        item(key = "hot_hdr") {
                            Zb8CommentHeader("🔥 神评热议", hotComments.size, Zb8Colors.HotOrange)
                        }
                        items(hotComments, key = { "hot_${it.id}" }) { c ->
                            Zb8CommentCard(c)
                            HorizontalDivider(Modifier.padding(start = 54.dp, end = 16.dp), thickness = 0.5.dp, color = Zb8Colors.Divider)
                        }
                    }

                    // 全部评论
                    item(key = "all_hdr") {
                        Zb8CommentHeader("全部评论", commentTotal, Zb8Colors.Primary)
                    }
                    if (comments.isEmpty()) {
                        item(key = "empty_c") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 26.dp), contentAlignment = Alignment.Center) {
                                Text("暂无评论", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(comments, key = { "c_${it.id}" }) { c ->
                            Zb8CommentCard(c)
                            HorizontalDivider(Modifier.padding(start = 54.dp, end = 16.dp), thickness = 0.5.dp, color = Zb8Colors.Divider)
                        }
                        if (hasMoreComments && d.filename.isNotBlank()) {
                            item(key = "more_c") {
                                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    if (loadingMore) {
                                        CircularProgressIndicator(color = Zb8Colors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                                    } else {
                                        Button(
                                            onClick = {
                                                loadingMore = true
                                                scope.launch {
                                                    val nextP = commentPage + 1
                                                    runCatching { Zb8Api.comments(d.filename, page = nextP) }
                                                        .onSuccess { cp ->
                                                            val seen = comments.map { it.id }.toHashSet()
                                                            val added = cp.comments.filter { it.id !in seen }
                                                            comments = comments + added
                                                            commentPage = nextP
                                                            hasMoreComments = added.isNotEmpty() && cp.hasMore
                                                        }
                                                    loadingMore = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Zb8Colors.SurfaceHigh),
                                            shape = RoundedCornerShape(18.dp)
                                        ) {
                                            Text("加载更多评论", color = Zb8Colors.TextPrimary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    viewerUrl?.let { u ->
        Zb8ImageViewerDialog(url = u, onDismiss = { viewerUrl = null })
    }
}

@Composable
private fun Zb8CommentHeader(title: String, count: Int, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(title, color = Zb8Colors.TextPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text("($count)", color = Zb8Colors.TextTertiary, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun Zb8CommentCard(c: Zb8Comment) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!c.avatar.isNullOrBlank()) {
            AsyncImage(
                model = c.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Zb8Colors.SurfaceHigh)
            )
        } else {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Zb8Colors.SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(c.userName.take(1), color = Zb8Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.userName,
                    color = Zb8Colors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    tint = if (c.isHot) Zb8Colors.HotOrange else Zb8Colors.TextTertiary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    c.upCount.toString(),
                    color = if (c.isHot) Zb8Colors.HotOrange else Zb8Colors.TextTertiary,
                    fontSize = 11.5.sp,
                    fontWeight = if (c.isHot) FontWeight.Bold else FontWeight.Normal
                )
                if (c.downCount > 0) {
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Outlined.ThumbDown, contentDescription = null, tint = Zb8Colors.TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(c.downCount.toString(), color = Zb8Colors.TextTertiary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(c.content, color = Zb8Colors.TextPrimary, fontSize = 14.5.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                listOf(c.time, c.device).filter { it.isNotBlank() }.joinToString(" · "),
                color = Zb8Colors.TextTertiary,
                fontSize = 11.sp
            )

            // 楼中楼子回复预览
            if (c.children.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Zb8Colors.SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    c.children.take(3).forEachIndexed { idx, child ->
                        Text(
                            "${child.userName}：${child.content}",
                            color = Zb8Colors.TextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                        if (idx != c.children.take(3).lastIndex) Spacer(Modifier.height(4.dp))
                    }
                    if (c.children.size > 3) {
                        Spacer(Modifier.height(4.dp))
                        Text("共 ${c.children.size} 条楼中楼回复", color = Zb8Colors.Primary, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}

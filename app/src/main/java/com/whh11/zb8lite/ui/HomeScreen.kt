package com.whh11.zb8lite.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.zb8lite.R
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Item

@Composable
fun HomeScreen(onOpen: (url: String, title: String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Zb8Colors.Bg)
    ) {
        BrandHeader(onReload = { reload++ })
        SegmentTabs(
            labels = listOf("资讯", "赛程", "数据", "圈子"),
            selected = tab,
            onSelect = { tab = it }
        )
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> NewsTab(reload = reload, onOpen = onOpen)
                1 -> MatchTab(reload = reload, onOpen = onOpen)
                2 -> DataTab(reload = reload)
                else -> CommunityTab(reload = reload, onOpen = onOpen)
            }
        }
    }
}

@Composable
fun BrandHeader(onReload: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_brand),
            contentDescription = "直播吧",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "直播吧",
            color = Zb8Colors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Zb8Colors.Live.copy(alpha = 0.15f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text("LITE", color = Zb8Colors.Live, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onReload) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "刷新",
                tint = Zb8Colors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SegmentTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp)) {
        labels.forEachIndexed { i, label ->
            val active = i == selected
            Column(
                Modifier
                    .clickable { onSelect(i) }
                    .padding(end = 22.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    label,
                    color = if (active) Zb8Colors.TextPrimary else Zb8Colors.TextTertiary,
                    fontSize = 16.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(if (active) 20.dp else 0.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Zb8Colors.Blue)
                )
            }
        }
    }
}

@Composable
private fun NewsTab(reload: Int, onOpen: (String, String) -> Unit) {
    val channels = Zb8Api.CHANNELS
    var index by rememberSaveable { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(index, reload) {
        loading = true
        error = null
        runCatching { Zb8Api.news(channels[index]) }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: "网络异常" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(channels.map { it.label }) { i, label ->
                val active = i == index
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) Zb8Colors.Blue else Zb8Colors.Surface)
                        .clickable { index = i }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else Zb8Colors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        when {
            loading && items.isEmpty() -> LoadingBlock()
            error != null && items.isEmpty() -> ErrorBlock(error!!)
            items.isEmpty() -> EmptyBlock("这个频道暂时没有内容")
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 26.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { i, item ->
                    NewsRow(item) { onOpen(item.url, item.title) }
                    if (i != items.lastIndex) {
                        Box(
                            Modifier
                                .padding(horizontal = 16.dp)
                                .height(1.dp)
                                .fillMaxWidth()
                                .background(Zb8Colors.Line.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsRow(item: Zb8Item, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = Zb8Colors.TextPrimary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (item.tag.isNotBlank() || item.time.isNotBlank() || item.comments > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.tag.isNotBlank()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Zb8Colors.SurfaceHigh)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                item.tag.uppercase(),
                                color = Zb8Colors.BlueSoft,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    if (item.comments > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                "${item.comments}评",
                                color = Color(0xFFFFB74D),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (item.time.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(item.time, color = Zb8Colors.TextTertiary, fontSize = 11.sp)
                    }
                }
            }
        }
        item.thumb?.let { url ->
            Spacer(Modifier.width(11.dp))
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Zb8Colors.SurfaceHigh)
            )
        }
    }
}

@Composable
fun LoadingBlock() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("加载中…", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
    }
}

@Composable
fun ErrorBlock(message: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("没抓到数据", color = Zb8Colors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Zb8Colors.TextTertiary, fontSize = 12.sp)
    }
}

@Composable
fun EmptyBlock(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Zb8Colors.TextTertiary, fontSize = 13.sp)
    }
}

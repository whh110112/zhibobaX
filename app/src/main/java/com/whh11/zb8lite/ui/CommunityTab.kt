package com.whh11.zb8lite.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Channel
import com.whh11.zb8lite.data.Zb8Item

@Composable
fun CommunityTab(reload: Int, onOpen: (String, String) -> Unit) {
    val topics = Zb8Api.TOPICS
    var topicIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTopic = topics[topicIndex]

    var items by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(topicIndex, reload) {
        loading = true
        error = null
        runCatching {
            // 根据专区类别选择抓取源
            val channelUrl = when (currentTopic.category) {
                "篮球" -> "https://m.zhibo8.com/nba/"
                "足球" -> "https://m.zhibo8.com/zuqiu/"
                else -> "https://m.zhibo8.com/"
            }
            val raw = Zb8Api.news(Zb8Channel(channelUrl, currentTopic.name))
            if (currentTopic.tag.isBlank()) {
                raw
            } else {
                val filtered = raw.filter { it.title.contains(currentTopic.tag) || it.tag.contains(currentTopic.tag) }
                if (filtered.isNotEmpty()) filtered else raw
            }
        }.onSuccess { items = it }
            .onFailure { error = it.message ?: "加载话题失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 专区/圈子选择
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(topics.map { it.name }) { i, name ->
                val active = i == topicIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) Zb8Colors.Blue else Zb8Colors.Surface)
                        .clickable { topicIndex = i }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                ) {
                    Text(
                        name,
                        color = if (active) Color.White else Zb8Colors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        when {
            loading && items.isEmpty() -> LoadingBlock()
            error != null && items.isEmpty() -> ErrorBlock(error!!)
            items.isEmpty() -> EmptyBlock("该专区暂时没有新讨论")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🔥 吧友热议 · ${currentTopic.name}",
                                color = Zb8Colors.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            Text("精选讨论流", color = Zb8Colors.TextTertiary, fontSize = 11.sp)
                        }
                    }
                    itemsIndexed(items) { i, item ->
                        CommunityRow(item) { onOpen(item.url, item.title) }
                        if (i != items.lastIndex) {
                            Box(
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .fillMaxWidth()
                                    .background(Zb8Colors.Line.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityRow(item: Zb8Item, onClick: () -> Unit) {
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
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                if (item.comments > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            "💬 ${item.comments}评",
                            color = Color(0xFFFFB74D),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                if (item.time.isNotBlank()) {
                    Text(item.time, color = Zb8Colors.TextTertiary, fontSize = 11.sp)
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
                    .size(width = 92.dp, height = 62.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Zb8Colors.SurfaceHigh)
            )
        }
    }
}

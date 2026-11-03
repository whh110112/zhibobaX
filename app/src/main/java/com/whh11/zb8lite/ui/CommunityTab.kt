package com.whh11.zb8lite.ui

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("zb8_community", Context.MODE_PRIVATE) }
    
    var favTeam by remember { mutableStateOf(sp.getString("fav_team", "lakers") ?: "lakers") }
    var followedSet by remember { 
        mutableStateOf(sp.getStringSet("followed", setOf("all", "lakers", "warriors", "realm", "china")) ?: setOf("all")) 
    }

    val topics = Zb8Api.TOPICS
    // 优先排在前面的圈子：我的主队 -> 已关注圈子 -> 其他圈子
    val sortedTopics = remember(favTeam, followedSet) {
        topics.sortedWith { a, b ->
            when {
                a.id == favTeam -> -2
                b.id == favTeam -> 2
                followedSet.contains(a.id) && !followedSet.contains(b.id) -> -1
                !followedSet.contains(a.id) && followedSet.contains(b.id) -> 1
                else -> 0
            }
        }
    }

    var topicIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTopic = sortedTopics.getOrElse(topicIndex.coerceIn(0, sortedTopics.lastIndex)) { sortedTopics[0] }

    var items by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTopic.id, reload) {
        loading = true
        error = null
        runCatching {
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
            .onFailure { error = it.message ?: "加载专区讨论失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 专区/圈子选择
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(sortedTopics) { i, topic ->
                val active = topic.id == currentTopic.id
                val isMyFav = topic.id == favTeam
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) Zb8Colors.Blue else Zb8Colors.Surface)
                        .clickable { topicIndex = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMyFav) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = "主队",
                                tint = if (active) Color(0xFFFFD700) else Color(0xFFFFB800),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            topic.name,
                            color = if (active) Color.White else Zb8Colors.TextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 圈子信息条：关注与设为主队操作
        if (currentTopic.id != "all") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Zb8Colors.Surface)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    currentTopic.name,
                    color = Zb8Colors.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                if (currentTopic.id == favTeam) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFB800).copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("★ 我的主队", color = Color(0xFFFFB800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))

                // 设为主队
                val isFav = currentTopic.id == favTeam
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFav) Zb8Colors.SurfaceHigh else Zb8Colors.Blue.copy(alpha = 0.12f))
                        .clickable {
                            val newFav = if (isFav) "" else currentTopic.id
                            favTeam = newFav
                            sp.edit().putString("fav_team", newFav).apply()
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isFav) "取消主队" else "设为主队",
                        color = if (isFav) Zb8Colors.TextSecondary else Zb8Colors.Blue,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.width(8.dp))

                // 关注圈子
                val isFollowed = followedSet.contains(currentTopic.id)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFollowed) Zb8Colors.SurfaceHigh else Zb8Colors.Blue)
                        .clickable {
                            val newSet = followedSet.toMutableSet()
                            if (isFollowed) newSet.remove(currentTopic.id) else newSet.add(currentTopic.id)
                            followedSet = newSet
                            sp.edit().putStringSet("followed", newSet).apply()
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isFollowed) "已关注" else "+ 关注",
                        color = if (isFollowed) Zb8Colors.TextSecondary else Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        when {
            loading && items.isEmpty() -> LoadingBlock()
            error != null && items.isEmpty() -> ErrorBlock(error!!)
            items.isEmpty() -> EmptyBlock("专区暂时没有讨论")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items) { i, itm ->
                        CommunityRow(itm) {
                            val fullUrl = if (itm.url.startsWith("http")) itm.url else "https://m.zhibo8.com" + itm.url
                            onOpen(fullUrl, itm.title)
                        }
                        if (i != items.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 16.dp, end = 16.dp)
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
}

@Composable
private fun CommunityRow(itm: Zb8Item, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                itm.title,
                color = Zb8Colors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (itm.tag.isNotBlank()) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Zb8Colors.Blue.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(itm.tag, color = Zb8Colors.Blue, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(itm.time, color = Zb8Colors.TextTertiary, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                if (itm.comments > 0) {
                    Text(
                        "${itm.comments} 讨论",
                        color = Zb8Colors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        if (itm.thumb != null) {
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = itm.thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Zb8Colors.SurfaceHigh)
            )
        }
    }
}

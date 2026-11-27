package com.whh11.zb8lite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.zb8lite.R
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Item
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Zb8NewsTab(
    scrollToTopTrigger: Int,
    onOpenItem: (Zb8Item) -> Unit,
    onOpenSettings: () -> Unit
) {
    val channels = Zb8Api.CHANNELS
    var channelIdx by rememberSaveable { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val headerVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 120
        }
    }

    suspend fun load(isRefresh: Boolean = false) {
        if (isRefresh) refreshing = true else loading = true
        error = null
        runCatching { Zb8Api.news(channels[channelIdx]) }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: "网络异常" }
        loading = false
        refreshing = false
    }

    LaunchedEffect(channelIdx) { load() }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
            load(isRefresh = true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Zb8Colors.Bg)
    ) {
        AnimatedVisibility(
            visible = headerVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_brand),
                    contentDescription = "直播吧",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(9.dp))
                Text("直播吧资讯", color = Zb8Colors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Person, contentDescription = "我的与设置", tint = Zb8Colors.TextPrimary)
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .then(if (!headerVisible) Modifier.statusBarsPadding() else Modifier)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                itemsIndexed(channels) { idx, ch ->
                    val active = idx == channelIdx
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { channelIdx = idx }
                            .padding(top = 8.dp, bottom = 6.dp)
                    ) {
                        Text(
                            ch.label,
                            color = if (active) Zb8Colors.Primary else Zb8Colors.TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .width(if (active) 18.dp else 0.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Zb8Colors.Primary)
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { load(isRefresh = true) } },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                loading && items.isEmpty() -> LoadingBox()
                error != null && items.isEmpty() -> ErrorRetryBox(error!!, onRetry = { scope.launch { load() } })
                items.isEmpty() -> EmptyBox("该频道暂无内容")
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    itemsIndexed(items, key = { i, it -> "${it.url}_$i" }) { i, item ->
                        Zb8NewsRow(item = item, onClick = { onOpenItem(item) })
                        if (i != items.lastIndex) {
                            HorizontalDivider(
                                Modifier
                                    .background(Zb8Colors.Card)
                                    .padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = Zb8Colors.Divider
                            )
                        }
                    }
                }
            }
        }
    }
}

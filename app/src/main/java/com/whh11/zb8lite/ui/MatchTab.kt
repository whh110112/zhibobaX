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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.zb8lite.R
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Match
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DateOption(val label: String, val subLabel: String, val dateStr: String)
private data class SportOption(val label: String, val key: String)

/**
 * 直播吧招牌高密度时间轴赛程页：
 * - 修复原默认下标为“昨天”的 Bug，精准默认选中“今天”（index = 2）
 * - 当日开启 15 秒轻量实时比分轮询（接入 bifen4m.qiumibao.com）
 * - 还原直播吧紧凑高密度时间轴行，一屏可看 7~8 场比赛
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchTab(
    scrollToTopTrigger: Int,
    onOpenMatch: (Zb8Match) -> Unit,
    onOpenSettings: () -> Unit
) {
    val sportOptions = remember {
        listOf(
            SportOption("全部", "all"),
            SportOption("NBA/篮球", "basketball"),
            SportOption("足球", "football"),
            SportOption("综合", "other")
        )
    }
    var selectedSportIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentSport = sportOptions[selectedSportIndex]

    val dateOptions = remember { generateDateOptions() }
    // offsets = [-2, -1, 0, 1, 2, 3] => index 2 is TODAY!
    var selectedDateIndex by rememberSaveable { mutableIntStateOf(2) }
    val currentDate = dateOptions.getOrNull(selectedDateIndex)?.dateStr ?: Zb8Api.todayString()
    val isToday = selectedDateIndex == 2

    var matches by remember { mutableStateOf<List<Zb8Match>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    suspend fun load(isRefresh: Boolean = false, silent: Boolean = false) {
        if (!silent) {
            if (isRefresh) refreshing = true else loading = true
        }
        error = null
        runCatching { Zb8Api.matches(dateStr = currentDate, sport = currentSport.key) }
            .onSuccess { matches = it }
            .onFailure { if (!silent) error = it.message ?: "网络连接异常" }
        loading = false
        refreshing = false
    }

    LaunchedEffect(currentDate, currentSport.key) { load() }

    // 当日比赛每 15 秒静默刷新实时比分
    LaunchedEffect(isToday, currentSport.key) {
        if (!isToday) return@LaunchedEffect
        while (true) {
            delay(15_000L)
            load(silent = true)
        }
    }

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
        // 顶栏（直播吧标志性蓝圆 8 + LIVE 实时点）
        Column(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
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
                Text("直播吧", color = Zb8Colors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Zb8Colors.LiveGreen)
                )
                Spacer(Modifier.width(4.dp))
                Text("LIVE", color = Zb8Colors.LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Person, contentDescription = "我的与设置", tint = Zb8Colors.TextPrimary)
                }
            }

            // 项目下划线切换
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                sportOptions.forEachIndexed { idx, opt ->
                    val active = idx == selectedSportIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedSportIndex = idx }
                            .padding(top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            opt.label,
                            color = if (active) Zb8Colors.Primary else Zb8Colors.TextSecondary,
                            fontSize = 14.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
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

        // 紧凑日期选择条
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Bg)
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(dateOptions) { idx, opt ->
                val active = idx == selectedDateIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) Zb8Colors.Primary else Zb8Colors.Card)
                        .clickable { selectedDateIndex = idx }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        opt.label,
                        color = if (active) Color.White else Zb8Colors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        opt.subLabel,
                        color = if (active) Color.White.copy(alpha = 0.85f) else Zb8Colors.TextTertiary,
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { load(isRefresh = true) } },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                loading && matches.isEmpty() -> LoadingBox()
                error != null && matches.isEmpty() -> ErrorRetryBox(error!!, onRetry = { scope.launch { load() } })
                matches.isEmpty() -> EmptyBox("当天暂无该分类赛程")
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 18.dp)
                    ) {
                        itemsIndexed(matches, key = { i, m -> "${m.id}_$i" }) { i, match ->
                            Zb8TimelineMatchRow(match = match, onClick = { onOpenMatch(match) })
                            if (i != matches.lastIndex) {
                                HorizontalDivider(
                                    Modifier
                                        .background(Zb8Colors.Card)
                                        .padding(horizontal = 14.dp),
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
}

/**
 * 直播吧标志性高密度时间轴行：
 * 左侧时间/状态 · 中间上下双队对阵与右对齐比分 · 右侧「文字直播/战报」入口标签
 */
@Composable
fun Zb8TimelineMatchRow(match: Zb8Match, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Zb8Colors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧时间与赛事名
            Column(Modifier.width(68.dp)) {
                Text(
                    match.time.ifBlank { "--:--" },
                    color = if (match.isLive) Zb8Colors.LiveGreen else Zb8Colors.TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    match.competition,
                    color = Zb8Colors.TextTertiary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 中间上下双队对阵与比分
            Column(Modifier.weight(1f)) {
                MatchTeamLine(match.logoA, match.teamA, match.scoreA, match.teamAWon, match.isLive)
                Spacer(Modifier.height(5.dp))
                MatchTeamLine(match.logoB, match.teamB, match.scoreB, match.teamBWon, match.isLive)
            }

            Spacer(Modifier.width(10.dp))

            // 右侧状态与直播间入口标签
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(62.dp)
            ) {
                val badgeColor = when {
                    match.isLive -> Zb8Colors.LiveGreen
                    match.isFinished -> Zb8Colors.TextTertiary
                    else -> Zb8Colors.Primary
                }
                val badgeText = when {
                    match.isLive -> match.period.ifBlank { "直播中" }
                    match.isFinished -> "已完赛"
                    else -> "未开赛"
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (match.isLive) "图文直播" else if (match.isFinished) "赛况" else "前瞻",
                    color = Zb8Colors.Primary,
                    fontSize = 10.5.sp
                )
            }
        }

        if (match.keyStat.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "★ ${match.keyStat}",
                color = Zb8Colors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Zb8Colors.SurfaceHigh)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun MatchTeamLine(logo: String?, name: String, score: String, won: Boolean, isLive: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Zb8TeamLogo(logo, 18)
        Spacer(Modifier.width(7.dp))
        Text(
            name,
            color = Zb8Colors.TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = if (won) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            score.ifBlank { "-" },
            color = when {
                isLive -> Zb8Colors.LiveGreen
                won -> Zb8Colors.TextPrimary
                else -> Zb8Colors.TextSecondary
            },
            fontSize = 14.5.sp,
            fontWeight = if (won || isLive) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

private fun generateDateOptions(): List<DateOption> {
    val cal = Calendar.getInstance()
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfMd = SimpleDateFormat("MM/dd", Locale.getDefault())
    val sdfWeek = SimpleDateFormat("E", Locale.CHINESE)
    val offsets = listOf(-2, -1, 0, 1, 2, 3)
    return offsets.map { off ->
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, off)
        val label = when (off) {
            -2 -> "前天"
            -1 -> "昨天"
            0 -> "今天"
            1 -> "明天"
            2 -> "后天"
            else -> sdfWeek.format(c.time)
        }
        DateOption(label, sdfMd.format(c.time), sdfDate.format(c.time))
    }
}

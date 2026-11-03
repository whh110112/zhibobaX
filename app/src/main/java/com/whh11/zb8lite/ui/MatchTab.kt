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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Match
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DateOption(val label: String, val subLabel: String, val dateStr: String)

private data class SportOption(val label: String, val key: String)

@Composable
fun MatchTab(reload: Int, onOpen: (String, String) -> Unit) {
    val sportOptions = remember {
        listOf(
            SportOption("全部", "all"),
            SportOption("⚽ 足球", "football"),
            SportOption("🏀 篮球", "basketball"),
            SportOption("🎮 电竞", "game"),
            SportOption("🏸 体坛综合", "other")
        )
    }
    var selectedSportIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentSport = sportOptions[selectedSportIndex]

    val dateOptions = remember { generateDateOptions() }
    var selectedDateIndex by rememberSaveable { mutableIntStateOf(1) } // 默认“今天”
    val currentDate = dateOptions.getOrNull(selectedDateIndex)?.dateStr ?: Zb8Api.todayString()

    var matches by remember { mutableStateOf<List<Zb8Match>>(emptyList()) }
    var selectedLeagueFilter by remember { mutableStateOf("全部") }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentDate, currentSport.key, reload) {
        loading = true
        error = null
        selectedLeagueFilter = "全部"
        runCatching { Zb8Api.matches(dateStr = currentDate, sport = currentSport.key) }
            .onSuccess { matches = it }
            .onFailure { error = it.message ?: "网络异常" }
        loading = false
    }

    val leaguesInMatches = remember(matches) {
        val set = linkedSetOf("全部")
        matches.forEach { set.add(it.competition) }
        set.toList()
    }

    val displayMatches = remember(matches, selectedLeagueFilter) {
        if (selectedLeagueFilter == "全部") matches
        else matches.filter { it.competition == selectedLeagueFilter }
    }

    Column(Modifier.fillMaxSize()) {
        // 1. 项目大类切换栏（全部、足球、篮球、电竞、综合体坛）
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(sportOptions) { index, opt ->
                val active = index == selectedSportIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) Zb8Colors.Primary else Zb8Colors.Border)
                        .clickable { selectedSportIndex = index }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        opt.label,
                        color = if (active) Color.White else Zb8Colors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 2. 日期选择横向滚动条
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Bg)
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(dateOptions) { index, opt ->
                val active = index == selectedDateIndex
                Column(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Zb8Colors.Primary.copy(alpha = 0.15f) else Zb8Colors.Card)
                        .clickable { selectedDateIndex = index }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        opt.label,
                        color = if (active) Zb8Colors.Primary else Zb8Colors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        opt.subLabel,
                        color = if (active) Zb8Colors.Primary else Zb8Colors.TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // 3. 当前列表中的联赛快捷筛选（动态生成）
        if (leaguesInMatches.size > 2) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(leaguesInMatches) { _, leagueName ->
                    val active = leagueName == selectedLeagueFilter
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) Zb8Colors.Primary else Zb8Colors.Card)
                            .clickable { selectedLeagueFilter = leagueName }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            leagueName,
                            color = if (active) Color.White else Zb8Colors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 4. 详细比赛对阵列表
        Box(Modifier.fillMaxSize()) {
            when {
                loading && matches.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Zb8Colors.Primary, modifier = Modifier.size(32.dp))
                    }
                }
                error != null && matches.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error ?: "网络连接异常", color = Zb8Colors.TextSecondary, fontSize = 14.sp)
                    }
                }
                displayMatches.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("该分类暂无对阵赛程", color = Zb8Colors.TextSecondary, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        itemsIndexed(displayMatches, key = { index, m -> "${m.id}_$index" }) { _, match ->
                            MatchCard(match = match, onClick = {
                                onOpen(match.url, "${match.teamA} vs ${match.teamB}")
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: Zb8Match, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Zb8Colors.Card)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        // 卡片顶部：赛事全称 + 时间 + 状态角标
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                match.competition,
                color = Zb8Colors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                match.time,
                color = Zb8Colors.TextSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.weight(1f))

            // 状态徽章
            val badgeColor = when {
                match.isLive -> Zb8Colors.Live
                match.isFinished -> Zb8Colors.TextSecondary.copy(alpha = 0.6f)
                else -> Zb8Colors.Primary
            }
            val badgeText = when {
                match.isLive -> "● 正在直播"
                match.isFinished -> "已完赛"
                else -> "未开赛"
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 对阵双方与比分
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主队
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.teamA,
                    color = Zb8Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (match.hasScore && match.scoreA.toIntOrNull() ?: 0 > match.scoreB.toIntOrNull() ?: 0) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                TeamLogo(match.logoA)
            }

            // 中间比分
            Box(
                Modifier.width(72.dp),
                contentAlignment = Alignment.Center
            ) {
                if (match.hasScore) {
                    Text(
                        "${match.scoreA} : ${match.scoreB}",
                        color = if (match.isLive) Zb8Colors.Live else Zb8Colors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                } else {
                    Text(
                        "VS",
                        color = Zb8Colors.TextSecondary.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 客队
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogo(match.logoB)
                Spacer(Modifier.width(8.dp))
                Text(
                    match.teamB,
                    color = Zb8Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (match.hasScore && match.scoreB.toIntOrNull() ?: 0 > match.scoreA.toIntOrNull() ?: 0) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 焦点球员与球星战报
        if (match.keyStat.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Zb8Colors.Bg.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⭐ 核心表现: ",
                    color = Zb8Colors.Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    match.keyStat,
                    color = Zb8Colors.TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TeamLogo(url: String?) {
    if (url.isNullOrBlank()) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Zb8Colors.Border)
        )
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun generateDateOptions(): List<DateOption> {
    val cal = Calendar.getInstance()
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfMonthDay = SimpleDateFormat("MM-dd", Locale.getDefault())
    val sdfWeek = SimpleDateFormat("E", Locale.CHINESE)

    val list = ArrayList<DateOption>()
    // 覆盖：前天(-2)、昨天(-1)、今天(0)、明天(+1)、后天(+2)、大后天(+3)
    val offsets = listOf(-2, -1, 0, 1, 2, 3)

    for (off in offsets) {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, off)
        val dStr = sdfDate.format(c.time)
        val mdStr = sdfMonthDay.format(c.time)
        val wStr = sdfWeek.format(c.time)

        val label = when (off) {
            -2 -> "前天"
            -1 -> "昨天"
            0 -> "今天"
            1 -> "明天"
            2 -> "后天"
            else -> wStr
        }
        list.add(DateOption(label, mdStr, dStr))
    }
    return list
}

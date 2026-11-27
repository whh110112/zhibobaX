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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8MatchLiveDetail
import com.whh11.zb8lite.data.Zb8StarStat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 直播吧原生实时比赛直播室：
 * - 单节小分表（Q1~Q4/OT）+ 实时记分牌
 * - 中文解说员逐回合文字直播（每 10 秒自动拉取最新直播流）
 * - 双方球星核心技术统计
 */
@Composable
fun MatchLiveScreen(
    saishiId: String,
    dateStr: String,
    teamA: String,
    teamB: String,
    fallbackUrl: String,
    onBack: () -> Unit,
    onOpenWeb: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<Zb8MatchLiveDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var subTab by remember { mutableIntStateOf(0) } // 0=文字直播, 1=球星数据

    suspend fun load(silent: Boolean = false) {
        if (!silent) loading = true
        runCatching { Zb8Api.matchLiveDetail(saishiId, dateStr) }
            .onSuccess { detail = it }
        loading = false
    }

    LaunchedEffect(saishiId) { load() }

    // 直播中自动每 10 秒刷新文字直播
    LaunchedEffect(saishiId, detail?.state) {
        if (detail?.state == "3") return@LaunchedEffect
        while (true) {
            delay(10_000L)
            load(silent = true)
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
                .background(Zb8Colors.Card)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Zb8Colors.TextPrimary)
            }
            Text(
                "$teamA vs $teamB",
                color = Zb8Colors.TextPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { scope.launch { load() } }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = Zb8Colors.TextSecondary)
            }
            if (fallbackUrl.startsWith("http")) {
                IconButton(onClick = { onOpenWeb(fallbackUrl, "$teamA vs $teamB") }) {
                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = "战报网页", tint = Zb8Colors.TextSecondary)
                }
            }
        }

        // 顶部比分与单节小分表
        val d = detail
        Column(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    teamA,
                    color = Zb8Colors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    Modifier.width(106.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (!d?.scoreA.isNullOrBlank() && !d?.scoreB.isNullOrBlank()) "${d!!.scoreA} : ${d.scoreB}" else "VS",
                        color = if (d?.state == "1") Zb8Colors.LiveGreen else Zb8Colors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        d?.periodCn?.ifBlank { if (d.state == "3") "已完赛" else "直播室" } ?: "加载中",
                        color = if (d?.state == "1") Zb8Colors.LiveGreen else Zb8Colors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    teamB,
                    color = Zb8Colors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (d != null && d.quarterScoresA.isNotEmpty() && d.quarterScoresB.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                QuarterScoreTable(teamA, teamB, d.quarterScoresA, d.quarterScoresB, d.scoreA, d.scoreB)
            }
        }

        // 子 Tab
        Row(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            listOf("文字直播 (${d?.liveTexts?.size ?: 0})", "球星数据").forEachIndexed { idx, label ->
                val active = subTab == idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { subTab = idx }
                        .padding(top = 8.dp, bottom = 6.dp)
                ) {
                    Text(
                        label,
                        color = if (active) Zb8Colors.Primary else Zb8Colors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .width(if (active) 24.dp else 0.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Zb8Colors.Primary)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        when {
            loading && d == null -> LoadingBox()
            subTab == 0 -> {
                val texts = d?.liveTexts.orEmpty()
                if (texts.isEmpty()) {
                    EmptyBox("暂无文字直播播报（可点右上角查看完整战报页）")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(texts, key = { it.sid }) { line ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Zb8Colors.Card)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    line.anchor,
                                    color = Zb8Colors.Primary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    line.text,
                                    color = Zb8Colors.TextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                val statsA = d?.starStatsA.orEmpty()
                val statsB = d?.starStatsB.orEmpty()
                if (statsA.isEmpty() && statsB.isEmpty()) {
                    EmptyBox("暂无本场核心球星统计")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (statsA.isNotEmpty()) {
                            item { StarStatCard(teamA, statsA) }
                        }
                        if (statsB.isNotEmpty()) {
                            item { StarStatCard(teamB, statsB) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarterScoreTable(
    teamA: String,
    teamB: String,
    qA: List<String>,
    qB: List<String>,
    totalA: String,
    totalB: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Zb8Colors.SurfaceHigh)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val cols = maxOf(qA.size, qB.size)
        Row(Modifier.fillMaxWidth()) {
            Text("球队", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.weight(1.4f))
            for (i in 0 until cols) {
                Text(
                    if (i < 4) "Q${i + 1}" else "OT${i - 3}",
                    color = Zb8Colors.TextTertiary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("总分", color = Zb8Colors.TextTertiary, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(5.dp))
        ScoreRowLine(teamA, qA, cols, totalA)
        Spacer(Modifier.height(4.dp))
        ScoreRowLine(teamB, qB, cols, totalB)
    }
}

@Composable
private fun ScoreRowLine(team: String, q: List<String>, cols: Int, total: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(team, color = Zb8Colors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.4f))
        for (i in 0 until cols) {
            Text(q.getOrElse(i) { "-" }, color = Zb8Colors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
        Text(total.ifBlank { "-" }, color = Zb8Colors.Primary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StarStatCard(team: String, list: List<Zb8StarStat>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Zb8Colors.Card)
            .padding(14.dp)
    ) {
        Text("$team 核心表现", color = Zb8Colors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        list.forEachIndexed { idx, s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.name, color = Zb8Colors.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(s.value, color = Zb8Colors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (idx != list.lastIndex) HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
        }
    }
}

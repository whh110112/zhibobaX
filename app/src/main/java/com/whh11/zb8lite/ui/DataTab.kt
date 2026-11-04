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
import com.whh11.zb8lite.data.LeagueType
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Item
import com.whh11.zb8lite.data.Zb8League
import com.whh11.zb8lite.data.Zb8Match
import com.whh11.zb8lite.data.Zb8Player
import com.whh11.zb8lite.data.Zb8Standing

/** 队名模糊匹配：比赛里的队名常是简称，取前两字比对 */
private fun fuzzyName(a: String, b: String): Boolean {
    if (a.isBlank() || b.isBlank()) return false
    if (a.contains(b) || b.contains(a)) return true
    return a.length >= 2 && b.length >= 2 && a.take(2) == b.take(2)
}

/* ---------------- 球员榜 ---------------- */

@Composable
private fun PlayersView(players: List<Zb8Player>, onClick: (Zb8Player) -> Unit) {
    if (players.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无球员榜数据", color = Zb8Colors.TextSecondary, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                Text("球员", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("球队", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(88.dp))
                Text("进球", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
        }
        itemsIndexed(players, key = { i, p -> "${p.rank}_${p.playerName}_$i" }) { _, p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Zb8Colors.Card)
                    .clickable { onClick(p) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    p.rank.toString(),
                    color = if (p.rank <= 3) Zb8Colors.ScoreRed else Zb8Colors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (p.rank <= 3) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(26.dp),
                    textAlign = TextAlign.Center
                )
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (!p.playerLogo.isNullOrBlank()) {
                        AsyncImage(
                            model = p.playerLogo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(13.dp))
                        )
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        p.playerName,
                        color = Zb8Colors.TextPrimary,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    p.teamName,
                    color = Zb8Colors.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(88.dp)
                )
                Text(
                    p.count,
                    color = Zb8Colors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/* ---------------- 球队详情（原生） ---------------- */

@Composable
private fun TeamDetailScreen(
    team: Zb8Standing,
    league: Zb8League,
    onBack: () -> Unit,
    onOpen: (String, String) -> Unit
) {
    var relatedMatches by remember { mutableStateOf<List<Zb8Match>>(emptyList()) }
    var relatedNews by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(team.teamName) {
        loading = true
        runCatching { Zb8Api.matches() }.onSuccess { all ->
            relatedMatches = all
                .filter { fuzzyName(it.teamA, team.teamName) || fuzzyName(it.teamB, team.teamName) }
                .take(8)
        }
        runCatching { Zb8Api.news(Zb8Api.CHANNELS.first()) }.onSuccess { all ->
            relatedNews = all.filter { fuzzyName(it.title, team.teamName) }.take(8)
        }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Zb8Colors.Bg)
    ) {
        DetailTopBar(title = team.teamName, onBack = onBack)

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!team.teamLogo.isNullOrBlank()) {
                        AsyncImage(
                            model = team.teamLogo,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column {
                        Text(
                            team.teamName,
                            color = Zb8Colors.TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "${league.name} · 第 ${team.rank} 名",
                            color = Zb8Colors.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Zb8Colors.Card)
                        .padding(vertical = 13.dp)
                ) {
                    StatCell("场次", team.played.ifBlank { "-" }, Modifier.weight(1f))
                    StatCell("胜", team.won.ifBlank { "-" }, Modifier.weight(1f))
                    StatCell("平", team.drawn.ifBlank { "-" }, Modifier.weight(1f))
                    StatCell("负", team.lost.ifBlank { "-" }, Modifier.weight(1f))
                    StatCell("净胜", team.goalsDiff.ifBlank { "-" }, Modifier.weight(1f))
                    StatCell("积分", team.points.ifBlank { "-" }, Modifier.weight(1f), highlight = true)
                }
            }

            item { SectionTitle("近期比赛") }

            if (relatedMatches.isEmpty()) {
                item { EmptyHint(if (loading) "加载中…" else "近期没有该队的比赛记录") }
            } else {
                items(relatedMatches.size) { i ->
                    val m = relatedMatches[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Zb8Colors.Card)
                            .clickable { if (m.url.isNotBlank()) onOpen(m.url, "${m.teamA} vs ${m.teamB}") }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                m.competition,
                                color = Zb8Colors.TextTertiary,
                                fontSize = 10.5.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${m.teamA}  ${if (m.hasScore) "${m.scoreA} : ${m.scoreB}" else "vs"}  ${m.teamB}",
                                color = Zb8Colors.TextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            if (m.isLive) "进行中" else m.time,
                            color = if (m.isLive) Zb8Colors.Live else Zb8Colors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            item { SectionTitle("相关新闻") }

            if (relatedNews.isEmpty()) {
                item { EmptyHint(if (loading) "加载中…" else "暂无该队相关新闻") }
            } else {
                items(relatedNews.size) { i ->
                    val n = relatedNews[i]
                    Text(
                        n.title,
                        color = Zb8Colors.TextPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(n.url, n.title) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

/* ---------------- 球员详情（原生） ---------------- */

@Composable
private fun PlayerDetailScreen(
    player: Zb8Player,
    league: Zb8League,
    onBack: () -> Unit,
    onOpen: (String, String) -> Unit
) {
    var relatedNews by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val statLabel = if (league.scorersUrl.contains("%E8%BF%9B%E7%90%83")) "进球" else "数据"

    LaunchedEffect(player.playerName) {
        loading = true
        runCatching { Zb8Api.news(Zb8Api.CHANNELS.first()) }.onSuccess { all ->
            relatedNews = all.filter { fuzzyName(it.title, player.playerName) }.take(10)
        }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Zb8Colors.Bg)
    ) {
        DetailTopBar(title = player.playerName, onBack = onBack)

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!player.playerLogo.isNullOrBlank()) {
                        AsyncImage(
                            model = player.playerLogo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(31.dp))
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column {
                        Text(
                            player.playerName,
                            color = Zb8Colors.TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            listOf(player.teamName, league.name).filter { it.isNotBlank() }.joinToString(" · "),
                            color = Zb8Colors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Zb8Colors.Card)
                        .padding(vertical = 13.dp)
                ) {
                    StatCell(statLabel, player.count.ifBlank { "-" }, Modifier.weight(1f), highlight = true)
                    StatCell("榜内排名", "#${player.rank}", Modifier.weight(1f))
                    if (player.subCount.isNotBlank()) {
                        StatCell("点球", player.subCount.removePrefix("点球 "), Modifier.weight(1f))
                    }
                }
            }

            item { SectionTitle("相关新闻") }

            if (relatedNews.isEmpty()) {
                item { EmptyHint(if (loading) "加载中…" else "暂无相关新闻") }
            } else {
                items(relatedNews.size) { i ->
                    val n = relatedNews[i]
                    Text(
                        n.title,
                        color = Zb8Colors.TextPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(n.url, n.title) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

/* ---------------- 详情页通用小组件 ---------------- */

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Zb8Colors.Card)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("←", color = Zb8Colors.TextPrimary, fontSize = 20.sp)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            color = Zb8Colors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = if (highlight) Zb8Colors.Primary else Zb8Colors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        Text(label, color = Zb8Colors.TextTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp)
    ) {
        Text(text, color = Zb8Colors.TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text, color = Zb8Colors.TextTertiary, fontSize = 12.sp)
    }
}

@Composable
fun DataTab(reload: Int, onOpen: (String, String) -> Unit) {
    val categories = listOf("全部", "篮球", "欧洲足球", "亚洲足球", "美洲/非洲", "电竞与综合")
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedCategory = categories[selectedCategoryIndex]

    val allLeagues = Zb8Api.LEAGUES
    val filteredLeagues = remember(selectedCategoryIndex) {
        if (selectedCategory == "全部") allLeagues
        else allLeagues.filter { it.category == selectedCategory }
    }

    var selectedLeagueIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentLeague = filteredLeagues.getOrNull(selectedLeagueIndex) ?: filteredLeagues.firstOrNull() ?: allLeagues.first()

    var standings by remember { mutableStateOf<List<Zb8Standing>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 0=积分榜 1=射手榜
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var scorers by remember { mutableStateOf<List<Zb8Player>>(emptyList()) }
    val hasScorers = currentLeague.scorersUrl.isNotBlank()

    // 原生详情覆盖层
    var selectedTeam by remember { mutableStateOf<Zb8Standing?>(null) }
    var selectedPlayer by remember { mutableStateOf<Zb8Player?>(null) }

    LaunchedEffect(currentLeague, subTab, reload) {
        loading = true
        error = null
        runCatching {
            when {
                subTab == 1 && hasScorers -> scorers = Zb8Api.players(currentLeague.scorersUrl)
                else -> standings = Zb8Api.standings(currentLeague)
            }
        }.onFailure { error = it.message ?: "加载榜单失败" }
        loading = false
    }

    // 选中球队/球员时整页切到详情，避免多层嵌套布局
    selectedTeam?.let { team ->
        TeamDetailScreen(
            team = team,
            league = currentLeague,
            onBack = { selectedTeam = null },
            onOpen = onOpen
        )
        return
    }
    selectedPlayer?.let { player ->
        PlayerDetailScreen(
            player = player,
            league = currentLeague,
            onBack = { selectedPlayer = null },
            onOpen = onOpen
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        // 1. 项目分类选择
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(categories) { index, cat ->
                val active = index == selectedCategoryIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) Zb8Colors.Primary else Zb8Colors.Border)
                        .clickable {
                            selectedCategoryIndex = index
                            selectedLeagueIndex = 0
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        cat,
                        color = if (active) Color.White else Zb8Colors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 2. 二级联赛选择
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Bg)
                .padding(vertical = 5.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filteredLeagues) { index, league ->
                val active = league.id == currentLeague.id
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Zb8Colors.Primary.copy(alpha = 0.15f) else Zb8Colors.Card)
                        .clickable { selectedLeagueIndex = index }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        league.name,
                        color = if (active) Zb8Colors.Primary else Zb8Colors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 3. 积分榜 / 射手榜（只有足球类赛事且配了射手榜地址才显示）
        if (hasScorers && currentLeague.type == LeagueType.FOOTBALL) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("积分榜", "射手榜").forEachIndexed { i, title ->
                    val active = subTab == i
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) Zb8Colors.Primary else Zb8Colors.Card)
                            .clickable { subTab = i }
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            title,
                            color = if (active) Color.White else Zb8Colors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Zb8Colors.Primary, modifier = Modifier.size(32.dp))
                    }
                }
                error != null && standings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error ?: "网络加载失败", color = Zb8Colors.TextSecondary, fontSize = 14.sp)
                    }
                }
                subTab == 1 && hasScorers -> {
                    PlayersView(players = scorers) { selectedPlayer = it }
                }
                standings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无 ${currentLeague.name} 排名数据", color = Zb8Colors.TextSecondary, fontSize = 14.sp)
                    }
                }
                else -> {
                    val isBasketball = currentLeague.type == LeagueType.BASKETBALL
                    val grouped = standings.groupBy { it.groupTitle }

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        grouped.forEach { (groupTitle, list) ->
                            if (groupTitle.isNotBlank()) {
                                item(key = "g_$groupTitle") {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            groupTitle,
                                            color = Zb8Colors.Primary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 表头
                            item(key = "header_$groupTitle") {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Zb8Colors.Card)
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                    Text("球队", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    if (isBasketball) {
                                        Text("胜/负", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                                        Text("胜率", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                                        Text("胜差", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                                    } else {
                                        Text("赛", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                        Text("胜/平/负", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(62.dp), textAlign = TextAlign.Center)
                                        Text("净", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                        Text("分", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            itemsIndexed(list, key = { index, item -> "${groupTitle}_${item.rank}_${item.teamName}_$index" }) { _, row ->
                                val rankColor = when (row.rank) {
                                    1 -> Color(0xFFE53935)
                                    2 -> Color(0xFFFF9800)
                                    3 -> Color(0xFFFBC02D)
                                    in 4..6 -> Zb8Colors.Primary
                                    else -> Zb8Colors.TextSecondary
                                }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 0.5.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Zb8Colors.Card)
                                        .clickable { selectedTeam = row }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        row.rank.toString(),
                                        color = rankColor,
                                        fontSize = 12.sp,
                                        fontWeight = if (row.rank <= 3) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.width(24.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    Row(
                                        Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!row.teamLogo.isNullOrBlank()) {
                                            AsyncImage(
                                                model = row.teamLogo,
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            row.teamName,
                                            color = Zb8Colors.TextPrimary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (isBasketball) {
                                        Text("${row.won}/${row.lost}", color = Zb8Colors.TextPrimary, fontSize = 11.sp, modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                                        Text(row.winRate.ifBlank { row.points }, color = Zb8Colors.TextPrimary, fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                                        Text(row.gamesBehind.ifBlank { "-" }, color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                                    } else {
                                        Text(row.played, color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                        Text("${row.won}/${row.drawn}/${row.lost}", color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(62.dp), textAlign = TextAlign.Center)
                                        Text(row.goalsDiff, color = Zb8Colors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                        Text(row.points, color = Zb8Colors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
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

package com.whh11.zb8lite.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.zb8lite.data.FollowedZb8Team
import com.whh11.zb8lite.data.LeagueType
import com.whh11.zb8lite.data.LocalStore
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Item
import com.whh11.zb8lite.data.Zb8League
import com.whh11.zb8lite.data.Zb8Match
import com.whh11.zb8lite.data.Zb8Player
import com.whh11.zb8lite.data.Zb8Standing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTab(
    onOpenMatch: (Zb8Match) -> Unit,
    onOpenItem: (Zb8Item) -> Unit,
    onOpenSettings: () -> Unit
) {
    val categories = remember { Zb8Api.LEAGUES.map { it.category }.distinct() }
    var selectedCategoryIdx by rememberSaveable { mutableIntStateOf(0) }
    val currentCategory = categories.getOrElse(selectedCategoryIdx) { categories.first() }
    val leaguesInCategory = remember(currentCategory) {
        Zb8Api.LEAGUES.filter { it.category == currentCategory }
    }
    var selectedLeagueId by rememberSaveable { mutableStateOf(Zb8Api.LEAGUES.first().id) }
    val currentLeague = remember(selectedLeagueId, leaguesInCategory) {
        leaguesInCategory.firstOrNull { it.id == selectedLeagueId } ?: leaguesInCategory.first()
    }

    // 0 = 积分榜/排名, 1 = 射手榜/得分榜, 2 = 助攻榜, 3 = 篮板榜
    var subTab by rememberSaveable(currentLeague.id) { mutableIntStateOf(0) }
    var standings by remember { mutableStateOf<List<Zb8Standing>>(emptyList()) }
    var players by remember { mutableStateOf<List<Zb8Player>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var activeTeam by remember { mutableStateOf<Zb8Standing?>(null) }
    var activePlayer by remember { mutableStateOf<Zb8Player?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun load(isRefresh: Boolean = false) {
        if (isRefresh) refreshing = true else loading = true
        error = null
        runCatching {
            when (subTab) {
                1 -> players = Zb8Api.players(currentLeague.scorersUrl)
                2 -> players = Zb8Api.players(currentLeague.assistsUrl)
                3 -> players = Zb8Api.players(currentLeague.reboundsUrl)
                else -> standings = Zb8Api.standings(currentLeague)
            }
        }.onFailure {
            error = it.message ?: "网络异常"
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(currentLeague.id, subTab) {
        load()
    }

    if (activeTeam != null) {
        BackHandler { activeTeam = null }
        TeamDetailOverlay(
            standing = activeTeam!!,
            league = currentLeague,
            onBack = { activeTeam = null },
            onOpenMatch = onOpenMatch,
            onOpenItem = onOpenItem
        )
        return
    }

    if (activePlayer != null) {
        BackHandler { activePlayer = null }
        PlayerDetailOverlay(
            player = activePlayer!!,
            league = currentLeague,
            onBack = { activePlayer = null },
            onOpenMatch = onOpenMatch,
            onOpenItem = onOpenItem
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Zb8Colors.Bg)
    ) {
        // 顶部栏
        Row(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "数据榜单",
                color = Zb8Colors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.Person, contentDescription = "我的与设置", tint = Zb8Colors.TextSecondary, modifier = Modifier.size(21.dp))
            }
        }

        // 一级分类 Tab
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(categories) { idx, cat ->
                val selected = idx == selectedCategoryIdx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            selectedCategoryIdx = idx
                            selectedLeagueId = Zb8Api.LEAGUES.first { it.category == cat }.id
                            subTab = 0
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        cat,
                        color = if (selected) Zb8Colors.Primary else Zb8Colors.TextSecondary,
                        fontSize = 14.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .width(20.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (selected) Zb8Colors.Primary else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        // 二级联赛胶囊行
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(leaguesInCategory, key = { it.id }) { league ->
                val selected = league.id == currentLeague.id
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Zb8Colors.PrimaryContainer else Zb8Colors.SurfaceHigh)
                        .clickable {
                            selectedLeagueId = league.id
                            subTab = 0
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        league.name,
                        color = if (selected) Zb8Colors.Primary else Zb8Colors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // 三级子榜切换（积分榜 / 射手或得分 / 篮板 / 助攻）
        val availableSubTabs = buildList {
            add(0 to if (currentLeague.type == LeagueType.BASKETBALL) "球队排名" else "积分榜")
            if (currentLeague.scorersUrl.isNotBlank()) {
                add(1 to if (currentLeague.type == LeagueType.BASKETBALL) "得分榜" else "射手榜")
            }
            if (currentLeague.reboundsUrl.isNotBlank()) add(3 to "篮板榜")
            if (currentLeague.assistsUrl.isNotBlank()) add(2 to "助攻榜")
        }
        if (availableSubTabs.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSubTabs.forEach { (tabId, label) ->
                    val selected = subTab == tabId
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Zb8Colors.Primary else Color.Transparent)
                            .clickable { subTab = tabId }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            label,
                            color = if (selected) Color.White else Zb8Colors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { load(isRefresh = true) } },
            modifier = Modifier.weight(1f)
        ) {
            when {
                loading -> LoadingBox()
                error != null -> ErrorRetryBox(error ?: "加载失败", onRetry = { scope.launch { load() } })
                subTab == 0 && standings.isEmpty() -> EmptyBox("暂无榜单数据")
                subTab != 0 && players.isEmpty() -> EmptyBox("暂无球员榜数据")
                subTab == 0 -> StandingsList(
                    league = currentLeague,
                    standings = standings,
                    onClickTeam = { activeTeam = it }
                )
                else -> PlayersList(
                    players = players,
                    statLabel = when (subTab) {
                        1 -> if (currentLeague.type == LeagueType.BASKETBALL) "场均得分" else "进球"
                        3 -> "场均篮板"
                        else -> if (currentLeague.type == LeagueType.BASKETBALL) "场均助攻" else "助攻"
                    },
                    onClickPlayer = { activePlayer = it }
                )
            }
        }
    }
}

@Composable
private fun StandingsList(
    league: Zb8League,
    standings: List<Zb8Standing>,
    onClickTeam: (Zb8Standing) -> Unit
) {
    val grouped = remember(standings) { standings.groupBy { it.groupTitle } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        grouped.forEach { (groupTitle, list) ->
            if (groupTitle.isNotBlank()) {
                item(key = "hdr-$groupTitle") {
                    Text(
                        groupTitle,
                        color = Zb8Colors.Primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Zb8Colors.Bg)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            item(key = "th-$groupTitle") {
                if (league.type == LeagueType.BASKETBALL) BasketballTableHeader() else FootballTableHeader()
                HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
            }
            itemsIndexed(list, key = { idx, item -> "$groupTitle-${item.teamName}-${item.rank}-$idx" }) { idx, row ->
                if (league.type == LeagueType.BASKETBALL) {
                    BasketballStandingRow(row, onClick = { onClickTeam(row) })
                } else {
                    FootballStandingRow(row, total = list.size, onClick = { onClickTeam(row) })
                }
                if (idx < list.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                }
            }
        }
    }
}

@Composable
private fun BasketballTableHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Zb8Colors.Card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        Text("球队", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("胜-负", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
        Text("胜率", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        Text("胜差", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
        Text("近况", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun BasketballStandingRow(row: Zb8Standing, onClick: () -> Unit) {
    val zoneColor = when {
        row.rank in 1..6 -> Zb8Colors.LiveGreen
        row.rank in 7..10 -> Zb8Colors.Amber
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Zb8Colors.Card)
            .clickable(onClick = onClick)
            .padding(end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(22.dp)
                .background(zoneColor)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            row.rank.toString(),
            color = if (row.rank <= 6) Zb8Colors.Primary else Zb8Colors.TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.Center
        )
        Zb8TeamLogo(row.teamLogo, 22)
        Spacer(Modifier.width(8.dp))
        Text(
            row.teamName,
            color = Zb8Colors.TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${row.won}-${row.lost}",
            color = Zb8Colors.TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.Center
        )
        Text(row.winRate.ifBlank { "-" }, color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        Text(row.gamesBehind.ifBlank { "-" }, color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
        Text(
            row.streak.ifBlank { "-" },
            color = if (row.streak.contains("胜")) Zb8Colors.LiveGreen else if (row.streak.contains("负")) Zb8Colors.Red else Zb8Colors.TextTertiary,
            fontSize = 11.5.sp,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun FootballTableHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Zb8Colors.Card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        Text("球队", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("赛", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
        Text("胜/平/负", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
        Text("净胜", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Text("积分", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun FootballStandingRow(row: Zb8Standing, total: Int, onClick: () -> Unit) {
    val zoneColor = when {
        row.tag.contains("欧冠") || row.rank in 1..4 -> Zb8Colors.LiveGreen
        row.tag.contains("欧联") || row.rank == 5 -> Zb8Colors.Primary
        row.tag.contains("降级") || (total >= 16 && row.rank > total - 3) -> Zb8Colors.Red
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Zb8Colors.Card)
            .clickable(onClick = onClick)
            .padding(end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(22.dp)
                .background(zoneColor)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            row.rank.toString(),
            color = if (row.rank <= 4) Zb8Colors.Primary else Zb8Colors.TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.Center
        )
        Zb8TeamLogo(row.teamLogo, 22)
        Spacer(Modifier.width(8.dp))
        Text(
            row.teamName,
            color = Zb8Colors.TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(row.played.ifBlank { "-" }, color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
        Text("${row.won}/${row.drawn}/${row.lost}", color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
        Text(row.goalsDiff.ifBlank { "-" }, color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Text(
            row.points.ifBlank { "-" },
            color = Zb8Colors.TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PlayersList(
    players: List<Zb8Player>,
    statLabel: String,
    onClickPlayer: (Zb8Player) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("球员 / 球队", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(statLabel, color = Zb8Colors.TextTertiary, fontSize = 11.sp)
            }
            HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
        }
        itemsIndexed(players, key = { idx, p -> "${p.playerName}-${p.rank}-$idx" }) { idx, p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .clickable { onClickPlayer(p) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    p.rank.toString(),
                    color = if (p.rank <= 3) Zb8Colors.HotOrange else Zb8Colors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
                Zb8TeamLogo(p.playerLogo ?: p.teamLogo, 28)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.playerName, color = Zb8Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (p.teamName.isNotBlank()) {
                        Text(p.teamName, color = Zb8Colors.TextTertiary, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(p.count, color = Zb8Colors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (p.subCount.isNotBlank()) {
                        Text(p.subCount, color = Zb8Colors.TextTertiary, fontSize = 10.5.sp)
                    }
                }
            }
            if (idx < players.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
            }
        }
    }
}

@Composable
private fun TeamDetailOverlay(
    standing: Zb8Standing,
    league: Zb8League,
    onBack: () -> Unit,
    onOpenMatch: (Zb8Match) -> Unit,
    onOpenItem: (Zb8Item) -> Unit
) {
    val followedTeams by LocalStore.followedTeams.collectAsState()
    val isFollowed = remember(followedTeams, standing.teamName) {
        followedTeams.any { it.name == standing.teamName }
    }
    var teamMatches by remember { mutableStateOf<List<Zb8Match>>(emptyList()) }
    var teamNews by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(standing.teamName) {
        loading = true
        val sportKey = if (league.type == LeagueType.BASKETBALL) "basketball" else "football"
        teamMatches = runCatching {
            Zb8Api.teamSeasonMatches(standing.teamName, league = league, sport = sportKey)
        }.getOrDefault(emptyList())
        teamNews = runCatching {
            Zb8Api.searchArchiveNews(standing.teamName, sport = sportKey)
        }.getOrDefault(emptyList())
        loading = false
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
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Zb8Colors.TextPrimary)
            }
            Zb8TeamLogo(standing.teamLogo, 26)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(standing.teamName, color = Zb8Colors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${league.name} · 第 ${standing.rank} 名", color = Zb8Colors.TextSecondary, fontSize = 11.5.sp)
            }
            IconButton(onClick = {
                LocalStore.toggleFollowTeam(
                    FollowedZb8Team(
                        name = standing.teamName,
                        sport = if (league.type == LeagueType.BASKETBALL) "basketball" else "football",
                        logo = standing.teamLogo
                    )
                )
            }) {
                Icon(
                    if (isFollowed) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFollowed) "取消关注" else "关注球队",
                    tint = if (isFollowed) Zb8Colors.Amber else Zb8Colors.TextSecondary
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        if (loading) {
            LoadingBox()
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        "赛季赛程与战绩",
                        color = Zb8Colors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                if (teamMatches.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Zb8Colors.Card)
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            Text("近期暂无 ${standing.teamName} 的赛程记录", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(teamMatches, key = { it.id }) { m ->
                        CompactTeamMatchRow(m, onClick = { onOpenMatch(m) })
                        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                    }
                }

                item {
                    Text(
                        "相关资讯",
                        color = Zb8Colors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                if (teamNews.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Zb8Colors.Card)
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            Text("近期频道中暂无包含「${standing.teamName}」的资讯", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(teamNews, key = { it.url }) { item ->
                        Zb8NewsRow(item, onClick = { onOpenItem(item) })
                        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompactTeamMatchRow(match: Zb8Match, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Zb8Colors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(60.dp)) {
            Text(match.time.ifBlank { "--:--" }, color = Zb8Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                if (match.isLive) match.period.ifBlank { "直播中" } else match.period.ifBlank { match.competition },
                color = if (match.isLive) Zb8Colors.LiveGreen else Zb8Colors.TextTertiary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(Modifier.weight(1f)) {
            Text("${match.teamA}  vs  ${match.teamB}", color = Zb8Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (match.competition.isNotBlank()) {
                Text(match.competition, color = Zb8Colors.TextTertiary, fontSize = 11.5.sp)
            }
        }
        if (match.hasScore) {
            Text(
                "${match.scoreA} : ${match.scoreB}",
                color = if (match.isLive) Zb8Colors.LiveGreen else Zb8Colors.Primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Zb8Colors.Primary.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun PlayerDetailOverlay(
    player: Zb8Player,
    league: Zb8League,
    onBack: () -> Unit,
    onOpenMatch: (Zb8Match) -> Unit,
    onOpenItem: (Zb8Item) -> Unit
) {
    var teamMatches by remember { mutableStateOf<List<Zb8Match>>(emptyList()) }
    var playerNews by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(player.playerName, player.teamName) {
        loading = true
        val sportKey = if (league.type == LeagueType.BASKETBALL) "basketball" else "football"
        playerNews = runCatching {
            Zb8Api.searchArchiveNews(player.playerName, sport = sportKey)
        }.getOrDefault(emptyList())
        if (player.teamName.isNotBlank()) {
            teamMatches = runCatching {
                Zb8Api.teamSeasonMatches(player.teamName, league = league, sport = sportKey)
            }.getOrDefault(emptyList())
        }
        loading = false
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
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Zb8Colors.TextPrimary)
            }
            Zb8TeamLogo(player.playerLogo ?: player.teamLogo, 28)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(player.playerName, color = Zb8Colors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(
                        player.teamName.takeIf { it.isNotBlank() },
                        "第 ${player.rank} 名",
                        player.count.takeIf { it.isNotBlank() }?.let { "数据 $it" }
                    ).joinToString(" · "),
                    color = Zb8Colors.TextSecondary,
                    fontSize = 11.5.sp
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        if (loading) {
            LoadingBox()
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        "相关资讯（近5日全站归档）",
                        color = Zb8Colors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                if (playerNews.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Zb8Colors.Card)
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            Text("近5日暂无直接提及「${player.playerName}」的新闻", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(playerNews, key = { it.url }) { item ->
                        Zb8NewsRow(item, onClick = { onOpenItem(item) })
                        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                    }
                }

                if (player.teamName.isNotBlank()) {
                    item {
                        Text(
                            "所属球队赛程（${player.teamName}）",
                            color = Zb8Colors.TextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    if (teamMatches.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Zb8Colors.Card)
                                    .padding(horizontal = 16.dp, vertical = 18.dp)
                            ) {
                                Text("暂无 ${player.teamName} 赛程记录", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(teamMatches.take(15), key = { it.id }) { m ->
                            CompactTeamMatchRow(m, onClick = { onOpenMatch(m) })
                            HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                        }
                    }
                }
            }
        }
    }
}

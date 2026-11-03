package com.whh11.zb8lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.zb8lite.data.LeagueType
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Standing

@Composable
fun DataTab(reload: Int) {
    val leagues = Zb8Api.LEAGUES
    var leagueIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentLeague = leagues[leagueIndex]

    var standings by remember { mutableStateOf<List<Zb8Standing>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(leagueIndex, reload) {
        loading = true
        error = null
        runCatching { Zb8Api.standings(currentLeague) }
            .onSuccess { standings = it }
            .onFailure { error = it.message ?: "加载榜单失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 联赛选择 Chip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(leagues.map { it.name }) { i, name ->
                val active = i == leagueIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) Zb8Colors.Blue else Zb8Colors.Surface)
                        .clickable { leagueIndex = i }
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
            loading && standings.isEmpty() -> LoadingBlock()
            error != null && standings.isEmpty() -> ErrorBlock(error!!)
            standings.isEmpty() -> EmptyBlock("暂无该联赛榜单数据")
            else -> {
                if (currentLeague.type == LeagueType.BASKETBALL) {
                    BasketballStandingsView(standings)
                } else {
                    FootballStandingsView(standings)
                }
            }
        }
    }
}

@Composable
private fun FootballStandingsView(items: List<Zb8Standing>) {
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        // 固定表头
        Row(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.SurfaceHigh)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
            Text("球队", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(130.dp))
            Text("场次", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
            Text("胜/平/负", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
            Text("净胜", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
            Text("积分", color = Zb8Colors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { i ->
                val item = items[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (i % 2 == 0) Zb8Colors.Bg else Zb8Colors.Surface.copy(alpha = 0.4f))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 排名标号
                    val rankColor = when (item.rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> Zb8Colors.TextTertiary
                    }
                    Text(
                        "${item.rank}",
                        color = rankColor,
                        fontSize = 12.sp,
                        fontWeight = if (item.rank <= 3) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(26.dp),
                        textAlign = TextAlign.Center
                    )

                    // 球队名与队徽
                    Row(
                        Modifier.width(130.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.teamLogo != null) {
                            AsyncImage(
                                model = item.teamLogo,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            item.teamName,
                            color = Zb8Colors.TextPrimary,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(item.played, color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                    Text("${item.won}/${item.drawn}/${item.lost}", color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                    Text(item.goalsDiff, color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                    Text(
                        item.points,
                        color = Zb8Colors.Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun BasketballStandingsView(items: List<Zb8Standing>) {
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.SurfaceHigh)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
            Text("球队", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(115.dp))
            Text("胜/负", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
            Text("胜率", color = Zb8Colors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
            Text("胜差", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
            Text("近况", color = Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            var lastGroup = ""
            items(items.size) { i ->
                val item = items[i]
                if (item.groupTitle.isNotBlank() && item.groupTitle != lastGroup) {
                    lastGroup = item.groupTitle
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Zb8Colors.Surface)
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            item.groupTitle,
                            color = Zb8Colors.BlueSoft,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (i % 2 == 0) Zb8Colors.Bg else Zb8Colors.Surface.copy(alpha = 0.4f))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${item.rank}",
                        color = if (item.rank <= 8) Zb8Colors.TextPrimary else Zb8Colors.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = if (item.rank <= 8) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(26.dp),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        Modifier.width(115.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.teamLogo != null) {
                            AsyncImage(
                                model = item.teamLogo,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            item.teamName,
                            color = Zb8Colors.TextPrimary,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text("${item.won}/${item.lost}", color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    Text(
                        item.winRate,
                        color = Zb8Colors.Blue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(52.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(item.gamesBehind, color = Zb8Colors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                    Text(item.streak, color = if (item.streak.contains("胜")) Color(0xFFE53935) else Zb8Colors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

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

@Composable
fun MatchTab(reload: Int, onOpen: (String, String) -> Unit) {
    val dateOptions = remember { generateDateOptions() }
    var selectedDateIndex by rememberSaveable { mutableIntStateOf(1) } // 默认“今天”

    val currentDate = dateOptions.getOrNull(selectedDateIndex)?.dateStr ?: Zb8Api.todayString()

    var matches by remember { mutableStateOf<List<Zb8Match>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentDate, reload) {
        loading = true
        error = null
        runCatching { Zb8Api.matches(currentDate) }
            .onSuccess { matches = it }
            .onFailure { error = it.message ?: "网络异常" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 日期选择条
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(dateOptions) { i, opt ->
                val active = i == selectedDateIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) Zb8Colors.Blue else Zb8Colors.Surface)
                        .clickable { selectedDateIndex = i }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            opt.label,
                            color = if (active) Color.White else Zb8Colors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            opt.subLabel,
                            color = if (active) Color.White.copy(alpha = 0.8f) else Zb8Colors.TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        when {
            loading && matches.isEmpty() -> LoadingBlock()
            error != null && matches.isEmpty() -> ErrorBlock(error!!)
            matches.isEmpty() -> EmptyBlock("该日期暂时没有赛事记录")
            else -> {
                val live = matches.count { it.isLive }
                val finished = matches.count { it.isFinished }

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
                                dateOptions.getOrNull(selectedDateIndex)?.label ?: "赛事",
                                color = Zb8Colors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.weight(1f))
                            if (live > 0) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Zb8Colors.Live)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("$live 场进行中", color = Zb8Colors.Live, fontSize = 11.sp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("共 ${matches.size} 场 (已完赛 $finished)", color = Zb8Colors.TextTertiary, fontSize = 11.sp)
                        }
                    }
                    items(matches.size, key = { matches[it].id }) { i ->
                        val m = matches[i]
                        MatchCard(m) { onOpen(m.url, "${m.teamA} vs ${m.teamB}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(m: Zb8Match, onClick: () -> Unit) {
    val scoreAInt = m.scoreA.toIntOrNull()
    val scoreBInt = m.scoreB.toIntOrNull()
    val aWon = scoreAInt != null && scoreBInt != null && scoreAInt > scoreBInt
    val bWon = scoreAInt != null && scoreBInt != null && scoreBInt > scoreAInt

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.5.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Zb8Colors.Surface)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            // 顶部信息：时间、联赛、频道、状态
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.time,
                    color = if (m.isLive) Zb8Colors.Live else Zb8Colors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    m.competition.ifBlank { "综合赛事" },
                    color = Zb8Colors.TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.weight(1f))

                if (m.isLive) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Zb8Colors.Live.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text("进行中", color = Zb8Colors.Live, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (m.isFinished) {
                    Text("完赛", color = Zb8Colors.TextTertiary, fontSize = 11.sp)
                } else if (m.state.isNotBlank()) {
                    Text(m.state, color = Zb8Colors.TextTertiary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 队伍与比分行
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team A
                Column(Modifier.weight(1f)) {
                    TeamScoreRow(
                        name = m.teamA,
                        logo = m.logoA,
                        score = m.scoreA,
                        isWinner = aWon,
                        hasScore = m.hasScore
                    )
                    Spacer(Modifier.height(6.dp))
                    TeamScoreRow(
                        name = m.teamB,
                        logo = m.logoB,
                        score = m.scoreB,
                        isWinner = bWon,
                        hasScore = m.hasScore
                    )
                }
            }

            // 焦点球星数据统计
            if (m.keyStat.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Zb8Colors.SurfaceHigh.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        m.keyStat,
                        color = Zb8Colors.BlueSoft,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamScoreRow(
    name: String,
    logo: String?,
    score: String,
    isWinner: Boolean,
    hasScore: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Zb8Colors.SurfaceHigh)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            color = if (isWinner) Zb8Colors.TextPrimary else Zb8Colors.TextSecondary,
            fontSize = 13.5.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (hasScore) {
            Text(
                score,
                color = if (isWinner) Zb8Colors.Blue else Zb8Colors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}

private data class DateOption(
    val label: String,
    val subLabel: String,
    val dateStr: String
)

private fun generateDateOptions(): List<DateOption> {
    val out = ArrayList<DateOption>()
    val cal = Calendar.getInstance()
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val subDf = SimpleDateFormat("MM-dd", Locale.getDefault())

    // 昨天
    cal.add(Calendar.DAY_OF_YEAR, -1)
    out.add(DateOption("昨天", subDf.format(cal.time), df.format(cal.time)))

    // 今天
    cal.add(Calendar.DAY_OF_YEAR, 1)
    out.add(DateOption("今天", subDf.format(cal.time), df.format(cal.time)))

    // 前天
    cal.add(Calendar.DAY_OF_YEAR, -2)
    out.add(DateOption("前天", subDf.format(cal.time), df.format(cal.time)))

    return listOf(out[0], out[1], out[2])
}

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.zb8lite.data.FollowedZb8Team
import com.whh11.zb8lite.data.LocalStore
import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Item
import com.whh11.zb8lite.data.Zb8Match
import kotlinx.coroutines.launch

/**
 * 直播吧「主队」页（取代原失效的假“社区”标签）：
 * 仅展示真实精准匹配的赛程与频道资讯，绝不拿无关新闻充数。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowTab(
    onOpenMatch: (Zb8Match) -> Unit,
    onOpenItem: (Zb8Item) -> Unit,
    onOpenSettings: () -> Unit
) {
    val followedTeams by LocalStore.followedTeams.collectAsState()
    val primaryTeam by LocalStore.primaryTeam.collectAsState()

    var selectedTeamName by remember(primaryTeam, followedTeams) {
        mutableStateOf(
            followedTeams.firstOrNull { it.name == primaryTeam }?.name
                ?: followedTeams.firstOrNull()?.name.orEmpty()
        )
    }
    val selectedTeam = followedTeams.firstOrNull { it.name == selectedTeamName }

    var matches by remember { mutableStateOf<List<Zb8Match>>(emptyList()) }
    var news by remember { mutableStateOf<List<Zb8Item>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    suspend fun load(isRefresh: Boolean = false) {
        val team = selectedTeam ?: run {
            matches = emptyList()
            news = emptyList()
            loading = false
            refreshing = false
            return
        }
        if (isRefresh) refreshing = true else loading = true
        matches = runCatching { Zb8Api.teamSeasonMatches(team.name, sport = team.sport) }.getOrDefault(emptyList())
        news = runCatching { Zb8Api.searchArchiveNews(team.name, sport = team.sport) }.getOrDefault(emptyList())
        loading = false
        refreshing = false
    }

    LaunchedEffect(selectedTeamName) { load() }

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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "我的主队",
                color = Zb8Colors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Add, contentDescription = "添加主队", tint = Zb8Colors.Primary, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.Person, contentDescription = "我的与设置", tint = Zb8Colors.TextSecondary, modifier = Modifier.size(21.dp))
            }
        }

        // 已关注球队横向切换栏
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(followedTeams, key = { it.name }) { team ->
                val active = team.name == selectedTeamName
                val isPrimary = team.name == primaryTeam
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) Zb8Colors.Primary else Zb8Colors.SurfaceHigh)
                        .clickable { selectedTeamName = team.name }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isPrimary) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (active) Color.White else Zb8Colors.Amber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        team.name,
                        color = if (active) Color.White else Zb8Colors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        if (selectedTeam == null) {
            EmptyBox("点击右上角 + 添加你的主队")
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { load(isRefresh = true) } },
                modifier = Modifier.weight(1f)
            ) {
                if (loading) {
                    LoadingBox()
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // 主队操作条
                        item {
                            val isPrimary = selectedTeam.name == primaryTeam
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Zb8Colors.Card)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(selectedTeam.name, color = Zb8Colors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (selectedTeam.sport == "basketball") "篮球主队" else "足球主队",
                                        color = Zb8Colors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Zb8Colors.PrimaryContainer)
                                        .clickable { LocalStore.setPrimaryTeam(selectedTeam.name) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        if (isPrimary) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = Zb8Colors.Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (isPrimary) "默认主队" else "设为主队",
                                        color = Zb8Colors.Primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (followedTeams.size > 1) {
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { LocalStore.toggleFollowTeam(selectedTeam) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "取消关注", tint = Zb8Colors.TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                        }

                        item {
                            Text(
                                "今日赛程",
                                color = Zb8Colors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                        if (matches.isEmpty()) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Zb8Colors.Card)
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                ) {
                                    Text("今日暂无 ${selectedTeam.name} 的比赛", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(matches, key = { it.id }) { m ->
                                CompactTeamMatchRow(m, onClick = { onOpenMatch(m) })
                                HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                            }
                        }

                        item {
                            Text(
                                "相关快讯",
                                color = Zb8Colors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                        if (news.isEmpty()) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Zb8Colors.Card)
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                ) {
                                    Text("当前频道暂无直接提及「${selectedTeam.name}」的快讯", color = Zb8Colors.TextTertiary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(news, key = { it.url }) { item ->
                                Zb8NewsRow(item, onClick = { onOpenItem(item) })
                                HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTeamDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, sport ->
                LocalStore.toggleFollowTeam(FollowedZb8Team(name, sport))
                selectedTeamName = name
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddTeamDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("basketball") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Zb8Colors.Card,
        title = { Text("添加关注主队", color = Zb8Colors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("例如：凯尔特人、利物浦、国际米兰", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("basketball" to "篮球", "football" to "足球").forEach { (k, label) ->
                        val active = sport == k
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) Zb8Colors.Primary else Zb8Colors.SurfaceHigh)
                                .clickable { sport = k }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(label, color = if (active) Color.White else Zb8Colors.TextPrimary, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotBlank(),
                onClick = { onAdd(name.trim(), sport) }
            ) {
                Text("添加", color = Zb8Colors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Zb8Colors.TextSecondary)
            }
        }
    )
}

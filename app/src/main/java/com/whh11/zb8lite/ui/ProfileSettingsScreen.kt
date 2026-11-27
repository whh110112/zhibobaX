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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.zb8lite.data.LocalStore
import com.whh11.zb8lite.data.SavedZb8Article
import com.whh11.zb8lite.data.ThemeMode

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    onOpenArticle: (url: String, title: String) -> Unit
) {
    val themeMode by LocalStore.themeMode.collectAsState()
    val amoled by LocalStore.amoled.collectAsState()
    val fontScale by LocalStore.fontScale.collectAsState()
    val startTab by LocalStore.startTab.collectAsState()
    val favorites by LocalStore.favorites.collectAsState()
    val history by LocalStore.history.collectAsState()

    var sectionTab by remember { mutableIntStateOf(0) } // 0=设置, 1=收藏, 2=足迹

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
            Text("我的与设置", color = Zb8Colors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Zb8Colors.Card)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("偏好设置", "我的收藏 (${favorites.size})", "浏览足迹 (${history.size})").forEachIndexed { idx, label ->
                val active = sectionTab == idx
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Zb8Colors.Primary else Zb8Colors.SurfaceHigh)
                        .clickable { sectionTab = idx }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else Zb8Colors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)

        when (sectionTab) {
            0 -> SettingsList(
                themeMode = themeMode,
                amoled = amoled,
                fontScale = fontScale,
                startTab = startTab
            )
            1 -> SavedArticlesList(
                list = favorites,
                emptyText = "还没有本地收藏的新闻",
                onOpen = onOpenArticle
            )
            else -> SavedArticlesList(
                list = history,
                emptyText = "还没有浏览记录",
                onOpen = onOpenArticle,
                onClear = { LocalStore.clearHistory() }
            )
        }
    }
}

@Composable
private fun SettingsList(
    themeMode: ThemeMode,
    amoled: Boolean,
    fontScale: Float,
    startTab: Int
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            SectionTitle("外观主题")
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .padding(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val active = themeMode == mode
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) Zb8Colors.Primary else Zb8Colors.SurfaceHigh)
                                .clickable { LocalStore.setThemeMode(mode) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                mode.label,
                                color = if (active) Color.White else Zb8Colors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("AMOLED 纯黑模式", color = Zb8Colors.TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        Text("深色模式下使用 #000000 纯黑背景更省电", color = Zb8Colors.TextTertiary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = amoled,
                        onCheckedChange = { LocalStore.setAmoled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Zb8Colors.Primary)
                    )
                }
            }
        }

        item {
            SectionTitle("启动默认首屏")
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(0 to "比赛（直播吧经典）", 1 to "资讯头条").forEach { (tabIdx, label) ->
                    val active = startTab == tabIdx
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) Zb8Colors.Primary else Zb8Colors.SurfaceHigh)
                            .clickable { LocalStore.setStartTab(tabIdx) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (active) Color.White else Zb8Colors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("正文字号")
            val steps = listOf(0.9f to "紧凑", 1.0f to "标准", 1.12f to "大号", 1.24f to "超大")
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                steps.forEach { (scale, label) ->
                    val active = kotlin.math.abs(fontScale - scale) < 0.04f
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) Zb8Colors.Primary else Zb8Colors.SurfaceHigh)
                            .clickable { LocalStore.setFontScale(scale) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (active) Color.White else Zb8Colors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("关于")
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .padding(16.dp)
            ) {
                Text("直播吧 X · v3.0.0", color = Zb8Colors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "纯原生轻量体育客户端：接入实时比分流、原生文字直播室、原生新闻正文与「神评热议」楼中楼、积分榜/射手榜/助攻榜，无广告、免登录。",
                    color = Zb8Colors.TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Zb8Colors.TextSecondary,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun SavedArticlesList(
    list: List<SavedZb8Article>,
    emptyText: String,
    onOpen: (String, String) -> Unit,
    onClear: (() -> Unit)? = null
) {
    if (list.isEmpty()) {
        EmptyBox(emptyText)
        return
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (onClear != null) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "清空足迹",
                        color = Zb8Colors.Red,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onClear)
                    )
                }
            }
        }
        items(list, key = { "${it.url}-${it.savedAt}" }) { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Zb8Colors.Card)
                    .clickable { onOpen(item.url, item.title) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    item.title,
                    color = Zb8Colors.TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.tag.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.tag, color = Zb8Colors.TextTertiary, fontSize = 11.5.sp)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Zb8Colors.Divider)
        }
    }
}

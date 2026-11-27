package com.whh11.zb8lite

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whh11.zb8lite.data.LocalStore
import com.whh11.zb8lite.data.Zb8Match
import com.whh11.zb8lite.ui.DataTab
import com.whh11.zb8lite.ui.DetailScreen
import com.whh11.zb8lite.ui.FollowTab
import com.whh11.zb8lite.ui.LocalIsDark
import com.whh11.zb8lite.ui.MatchLiveScreen
import com.whh11.zb8lite.ui.MatchTab
import com.whh11.zb8lite.ui.ProfileSettingsScreen
import com.whh11.zb8lite.ui.Zb8ArticleDetailScreen
import com.whh11.zb8lite.ui.Zb8BottomBar
import com.whh11.zb8lite.ui.Zb8NewsTab
import com.whh11.zb8lite.ui.Zb8Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LocalStore.init(this)

        setContent {
            Zb8Theme {
                val isDark = LocalIsDark.current
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }
                Zb8App(onFinish = { finish() })
            }
        }
    }
}

@Composable
private fun Zb8App(onFinish: () -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current

    fun openArticle(url: String, title: String) {
        if (url.contains("jijin.htm") || url.contains("video")) {
            nav.navigate("web?url=${Uri.encode(url)}&title=${Uri.encode(title)}")
        } else {
            nav.navigate("article?url=${Uri.encode(url)}&title=${Uri.encode(title)}")
        }
    }

    fun openMatch(match: Zb8Match) {
        nav.navigate(
            "match/${Uri.encode(match.id)}?date=${Uri.encode(match.date)}&teamA=${Uri.encode(match.teamA)}&teamB=${Uri.encode(match.teamB)}&url=${Uri.encode(match.url)}"
        )
    }

    fun openWeb(url: String, title: String) {
        nav.navigate("web?url=${Uri.encode(url)}&title=${Uri.encode(title)}")
    }

    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainTabsShell(
                onOpenMatch = ::openMatch,
                onOpenArticle = ::openArticle,
                onOpenSettings = { nav.navigate("settings") },
                onFinish = onFinish
            )
        }

        composable(
            route = "article?url={url}&title={title}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val url = entry.arguments?.getString("url").orEmpty()
            val title = entry.arguments?.getString("title").orEmpty()
            Zb8ArticleDetailScreen(
                url = url,
                fallbackTitle = title,
                onBack = { nav.popBackStack() },
                onOpenWeb = ::openWeb
            )
        }

        composable(
            route = "match/{id}?date={date}&teamA={teamA}&teamB={teamB}&url={url}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType; defaultValue = "" },
                navArgument("teamA") { type = NavType.StringType; defaultValue = "" },
                navArgument("teamB") { type = NavType.StringType; defaultValue = "" },
                navArgument("url") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val date = entry.arguments?.getString("date").orEmpty()
            val teamA = entry.arguments?.getString("teamA").orEmpty()
            val teamB = entry.arguments?.getString("teamB").orEmpty()
            val url = entry.arguments?.getString("url").orEmpty()
            MatchLiveScreen(
                saishiId = id,
                dateStr = date,
                teamA = teamA,
                teamB = teamB,
                fallbackUrl = url,
                onBack = { nav.popBackStack() },
                onOpenWeb = ::openWeb
            )
        }

        composable("settings") {
            ProfileSettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenArticle = ::openArticle
            )
        }

        composable(
            route = "web?url={url}&title={title}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "网页" }
            )
        ) { entry ->
            val url = entry.arguments?.getString("url").orEmpty()
            val title = entry.arguments?.getString("title").orEmpty().ifBlank { "网页" }
            DetailScreen(url = url, title = title, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainTabsShell(
    onOpenMatch: (Zb8Match) -> Unit,
    onOpenArticle: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(LocalStore.startTab.value) }
    var matchScrollTop by remember { mutableIntStateOf(0) }
    var newsScrollTop by remember { mutableIntStateOf(0) }
    var lastBackAt by remember { mutableLongStateOf(0L) }

    BackHandler {
        val defaultTab = LocalStore.startTab.value
        if (selectedTab != defaultTab) {
            selectedTab = defaultTab
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackAt < 1800L) {
                onFinish()
            } else {
                lastBackAt = now
                Toast.makeText(context, "再按一次退出直播吧 X", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> MatchTab(
                    scrollToTopTrigger = matchScrollTop,
                    onOpenMatch = onOpenMatch,
                    onOpenSettings = onOpenSettings
                )
                1 -> Zb8NewsTab(
                    scrollToTopTrigger = newsScrollTop,
                    onOpenItem = { item -> onOpenArticle(item.url, item.title) },
                    onOpenSettings = onOpenSettings
                )
                2 -> FollowTab(
                    onOpenMatch = onOpenMatch,
                    onOpenItem = { item -> onOpenArticle(item.url, item.title) },
                    onOpenSettings = onOpenSettings
                )
                else -> DataTab(
                    onOpenMatch = onOpenMatch,
                    onOpenItem = { item -> onOpenArticle(item.url, item.title) },
                    onOpenSettings = onOpenSettings
                )
            }
        }
        Zb8BottomBar(
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            onReselect = { idx ->
                when (idx) {
                    0 -> matchScrollTop++
                    1 -> newsScrollTop++
                }
            }
        )
    }
}

package com.whh11.zb8lite.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色白底"),
    DARK("深色护眼")
}

object LocalStore {
    private const val PREF_NAME = "zb8_lite_v3"
    private var sp: SharedPreferences? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _amoled = MutableStateFlow(false)
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    // 0 = 比赛（直播吧默认门户体验）, 1 = 资讯
    private val _startTab = MutableStateFlow(0)
    val startTab: StateFlow<Int> = _startTab.asStateFlow()

    private val _primaryTeam = MutableStateFlow("湖人")
    val primaryTeam: StateFlow<String> = _primaryTeam.asStateFlow()

    private val _followedTeams = MutableStateFlow<List<FollowedZb8Team>>(defaultTeams())
    val followedTeams: StateFlow<List<FollowedZb8Team>> = _followedTeams.asStateFlow()

    private val _favorites = MutableStateFlow<List<SavedZb8Article>>(emptyList())
    val favorites: StateFlow<List<SavedZb8Article>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow<List<SavedZb8Article>>(emptyList())
    val history: StateFlow<List<SavedZb8Article>> = _history.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp = prefs
        _themeMode.value = runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        _amoled.value = prefs.getBoolean("amoled", false)
        _fontScale.value = prefs.getFloat("font_scale", 1.0f).coerceIn(0.85f, 1.3f)
        _startTab.value = prefs.getInt("start_tab", 0).coerceIn(0, 1)
        _primaryTeam.value = prefs.getString("primary_team", "湖人") ?: "湖人"
        _followedTeams.value = loadTeams(prefs.getString("followed_teams", null))
        _favorites.value = loadArticles(prefs.getString("favorites", null))
        _history.value = loadArticles(prefs.getString("history", null))
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sp?.edit()?.putString("theme_mode", mode.name)?.apply()
    }

    fun setAmoled(enabled: Boolean) {
        _amoled.value = enabled
        sp?.edit()?.putBoolean("amoled", enabled)?.apply()
    }

    fun setFontScale(scale: Float) {
        val c = scale.coerceIn(0.85f, 1.3f)
        _fontScale.value = c
        sp?.edit()?.putFloat("font_scale", c)?.apply()
    }

    fun setStartTab(tab: Int) {
        val v = tab.coerceIn(0, 1)
        _startTab.value = v
        sp?.edit()?.putInt("start_tab", v)?.apply()
    }

    fun setPrimaryTeam(name: String) {
        _primaryTeam.value = name
        sp?.edit()?.putString("primary_team", name)?.apply()
    }

    fun toggleFollowTeam(team: FollowedZb8Team) {
        val list = _followedTeams.value.toMutableList()
        val idx = list.indexOfFirst { it.name == team.name }
        if (idx >= 0) {
            list.removeAt(idx)
            if (_primaryTeam.value == team.name) setPrimaryTeam(list.firstOrNull()?.name.orEmpty())
        } else {
            list.add(0, team)
            if (_primaryTeam.value.isBlank()) setPrimaryTeam(team.name)
        }
        _followedTeams.value = list
        saveTeams(list)
    }

    fun toggleFavorite(article: SavedZb8Article): Boolean {
        val list = _favorites.value.toMutableList()
        val idx = list.indexOfFirst { it.url == article.url }
        val saved = if (idx >= 0) {
            list.removeAt(idx)
            false
        } else {
            list.add(0, article.copy(savedAt = System.currentTimeMillis()))
            true
        }
        _favorites.value = list
        saveArticles("favorites", list)
        return saved
    }

    fun recordHistory(article: SavedZb8Article) {
        if (article.url.isBlank()) return
        val list = _history.value.toMutableList()
        list.removeAll { it.url == article.url }
        list.add(0, article.copy(savedAt = System.currentTimeMillis()))
        val trimmed = list.take(100)
        _history.value = trimmed
        saveArticles("history", trimmed)
    }

    fun clearHistory() {
        _history.value = emptyList()
        sp?.edit()?.remove("history")?.apply()
    }

    private fun defaultTeams(): List<FollowedZb8Team> = listOf(
        FollowedZb8Team("湖人", "basketball"),
        FollowedZb8Team("勇士", "basketball"),
        FollowedZb8Team("火箭", "basketball"),
        FollowedZb8Team("皇家马德里", "football"),
        FollowedZb8Team("阿森纳", "football"),
        FollowedZb8Team("巴塞罗那", "football"),
        FollowedZb8Team("曼城", "football")
    )

    private fun saveTeams(list: List<FollowedZb8Team>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("name", t.name)
                put("sport", t.sport)
                put("logo", t.logo ?: "")
            })
        }
        sp?.edit()?.putString("followed_teams", arr.toString())?.apply()
    }

    private fun loadTeams(raw: String?): List<FollowedZb8Team> {
        if (raw.isNullOrBlank()) return defaultTeams()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return defaultTeams()
        val out = ArrayList<FollowedZb8Team>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            out.add(FollowedZb8Team(name, o.optString("sport", "basketball"), o.optString("logo").takeIf { it.isNotBlank() }))
        }
        return out.ifEmpty { defaultTeams() }
    }

    private fun saveArticles(key: String, list: List<SavedZb8Article>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("url", a.url)
                put("title", a.title)
                put("tag", a.tag)
                put("savedAt", a.savedAt)
            })
        }
        sp?.edit()?.putString(key, arr.toString())?.apply()
    }

    private fun loadArticles(raw: String?): List<SavedZb8Article> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = ArrayList<SavedZb8Article>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val url = o.optString("url")
            val title = o.optString("title")
            if (url.isBlank() || title.isBlank()) continue
            out.add(SavedZb8Article(url, title, o.optString("tag"), o.optLong("savedAt", 0L)))
        }
        return out
    }
}

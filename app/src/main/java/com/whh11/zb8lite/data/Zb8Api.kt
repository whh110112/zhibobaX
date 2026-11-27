package com.whh11.zb8lite.data

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 直播吧 v3.0 原生数据引擎：
 * 1. 当日实时比分：接入 bifen4m.qiumibao.com/json/list.htm（彻底取代失效的 li.item 爬取）+ s.qiumibao.com 融合；
 * 2. 原生直播室：bifen4m v2 单节比分/球星数据 + dingshi4m 逐回合中文文字直播流；
 * 3. 原生文章 + 神评/全量评论：Jsoup 提取正文与动图 + cache.zhibo8.cc 静态 CDN 神评与楼中楼回复。
 */
object Zb8Api {

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val CHANNELS = listOf(
        Zb8Channel("https://m.zhibo8.com/", "头条"),
        Zb8Channel("https://m.zhibo8.com/nba/", "NBA"),
        Zb8Channel("https://m.zhibo8.com/zuqiu/", "足球"),
        Zb8Channel("https://m.zhibo8.com/other/", "综合"),
        Zb8Channel("https://m.zhibo8.com/video.htm", "集锦", highlightsOnly = true)
    )

    val LEAGUES = listOf(
        // 篮球
        Zb8League(
            id = "nba", name = "NBA", category = "篮球", type = LeagueType.BASKETBALL,
            url = "https://stats.qiumibao.com/data/json_v2/nba_paihang.htm",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%BE%97%E5%88%86&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=NBA",
            reboundsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%AF%AE%E6%9D%BF&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=NBA",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=NBA"
        ),
        Zb8League(
            id = "cba", name = "CBA", category = "篮球", type = LeagueType.BASKETBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E6%8E%92%E8%A1%8C&tab=%E6%8E%92%E8%A1%8C&league_id=925&league=CBA",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=925&league=CBA"
        ),
        // 欧洲足球（同时配齐积分榜、射手榜、助攻榜、全赛季赛程）
        Zb8League(
            "epl", "英超", "欧洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=24&league=%E8%8B%B1%E8%B6%85"
        ),
        Zb8League(
            "laliga", "西甲", "欧洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=25&league=%E8%A5%BF%E7%94%B2"
        ),
        Zb8League(
            "seriea", "意甲", "欧洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=26&league=%E6%84%8F%E7%94%B2"
        ),
        Zb8League(
            "bundesliga", "德甲", "欧洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=27&league=%E5%BE%B7%E7%94%B2"
        ),
        Zb8League(
            "ligue1", "法甲", "欧洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=28&league=%E6%B3%95%E7%94%B2"
        ),
        Zb8League(
            "ucl", "欧冠", "欧洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B"
        ),
        // 亚洲/美洲
        Zb8League(
            "csl", "中超", "亚洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=1&league=%E4%B8%AD%E8%B6%85"
        ),
        Zb8League(
            "saudi", "沙特联", "亚洲足球", LeagueType.FOOTBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=318&league=%E6%B2%99%E7%89%B9%E8%B6%85",
            scheduleUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%B5%9B%E7%A8%8B&tab=%E8%B5%9B%E7%A8%8B&league_id=318&league=%E6%B2%99%E7%89%B9%E8%B6%85"
        ),
        Zb8League(
            "mls", "美职联", "美洲足球", LeagueType.BASKETBALL,
            url = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=244&league=%E7%BE%8E%E8%81%8C%E8%81%94"
        )
    )

    private fun fetchSync(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://m.zhibo8.com/")
            .build()
        return client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
            res.body?.string().orEmpty()
        }
    }

    /** 1. 资讯流 */
    suspend fun news(channel: Zb8Channel): List<Zb8Item> = withContext(Dispatchers.IO) {
        val html = fetchSync(channel.url)
        parseNews(html, channel.url, channel.highlightsOnly)
    }

    /** 2. 原生文章正文解析（提取正文段落、高清图片/GIF、评论区 filename） */
    suspend fun articleDetail(url: String): Zb8ArticleDetail = withContext(Dispatchers.IO) {
        val html = fetchSync(url)
        parseArticleDetail(url, html)
    }

    internal fun parseArticleDetail(url: String, html: String): Zb8ArticleDetail {
        val doc = Jsoup.parse(html, url)
        val title = doc.selectFirst("h1, .title, .article-title")?.text()?.trim()
            ?.ifBlank { doc.title().substringBefore("-直播吧").trim() }
            ?: doc.title().substringBefore("-直播吧").trim()
        val time = doc.selectFirst(".time, .createtime, .pass_time")?.text()?.trim().orEmpty()
        val source = doc.selectFirst(".from-name, .source")?.text()?.trim().orEmpty().ifBlank { "直播吧" }

        val fnMatch = Regex("""filename["']?\s*[:=]\s*["']([^"']+)["']""").find(html)
        val filename = fnMatch?.groupValues?.getOrNull(1).orEmpty()

        val blocks = ArrayList<Zb8Block>()
        val images = LinkedHashSet<String>()
        val contentEl = doc.selectFirst("#signals, .content, .article-content, article") ?: doc.body()

        for (el in contentEl.select("p, img")) {
            if (el.tagName().equals("img", true)) {
                val src = imgOf(el)
                if (!src.isNullOrBlank() && images.add(src)) {
                    blocks.add(Zb8Block.Image(src))
                }
            } else {
                val text = el.text().trim()
                if (text.isNotBlank() && !text.contains("打开直播吧") && !text.contains("下载APP")) {
                    blocks.add(Zb8Block.Paragraph(text))
                }
            }
        }
        return Zb8ArticleDetail(
            url = url,
            title = title,
            time = time,
            source = source,
            filename = filename,
            blocks = blocks,
            imageUrls = images.toList()
        )
    }

    /** 3. 直播吧原生神评（热评）+ 全量评论分页（含楼中楼 children） */
    suspend fun comments(filename: String, page: Int = 0): Zb8CommentPage = coroutineScope {
        if (filename.isBlank()) return@coroutineScope Zb8CommentPage(emptyList(), emptyList(), 0, false)
        val path = filename.replace("-", "/")
        val ts = System.currentTimeMillis()
        val countDeferred = async(Dispatchers.IO) {
            runCatching { fetchSync("https://cache.zhibo8.cc/json/${path}_count.htm?_t=$ts") }.getOrNull()
        }
        val hotDeferred: Deferred<String?>? = if (page == 0) async(Dispatchers.IO) {
            runCatching { fetchSync("https://cache.zhibo8.cc/json/${path}_hot.htm?_t=$ts") }.getOrNull()
        } else null
        val pageDeferred = async(Dispatchers.IO) {
            runCatching { fetchSync("https://cache.zhibo8.cc/json/${path}_$page.htm?_t=$ts") }.getOrNull()
        }

        val total = countDeferred.await()?.let {
            runCatching { JSONObject(it).optInt("all_num", 0) }.getOrDefault(0)
        } ?: 0
        val hotList = hotDeferred?.await()?.let { parseCommentArray(it, isHot = true) }.orEmpty()
        val normalList = pageDeferred.await()?.let { parseCommentArray(it, isHot = false) }.orEmpty()
        Zb8CommentPage(
            hotComments = hotList,
            comments = normalList,
            total = if (total > 0) total else normalList.size,
            hasMore = normalList.size >= 15
        )
    }

    internal fun parseCommentArray(json: String, isHot: Boolean): List<Zb8Comment> {
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Zb8Comment>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            parseSingleComment(o, isHot)?.let { out.add(it) }
        }
        return out
    }

    private fun parseSingleComment(o: JSONObject, isHot: Boolean): Zb8Comment? {
        val content = o.optString("content").trim()
        if (content.isBlank()) return null
        val childrenArr = o.optJSONArray("children")
        val children = ArrayList<Zb8Comment>()
        if (childrenArr != null) {
            for (i in 0 until childrenArr.length()) {
                val c = childrenArr.optJSONObject(i) ?: continue
                parseSingleComment(c, isHot = false)?.let { children.add(it) }
            }
        }
        return Zb8Comment(
            id = o.optString("id", "$isHot-${content.hashCode()}"),
            userName = o.optString("username").ifBlank { "吧友" },
            avatar = normalizeUrl(o.optString("figureurl")),
            content = content,
            time = o.optString("createtime"),
            upCount = o.optString("up").toIntOrNull() ?: o.optInt("up", 0),
            downCount = o.optString("down").toIntOrNull() ?: o.optInt("down", 0),
            device = o.optString("device"),
            isHot = isHot,
            children = children
        )
    }

    /**
     * 4. 全网实时赛程（修复：当日接入 bifen4m.qiumibao.com/json/list.htm 与 record 精准融合）
     */
    suspend fun matches(dateStr: String = "", sport: String = "all"): List<Zb8Match> = coroutineScope {
        val targetDate = if (dateStr.isNotBlank()) dateStr else todayString()
        val isToday = targetDate == todayString()

        val recordDeferred = async(Dispatchers.IO) {
            runCatching {
                val json = fetchSync("https://s.qiumibao.com/json/record/$targetDate.htm?_t=${System.currentTimeMillis()}")
                parseRecordJson(json, targetDate)
            }.getOrDefault(emptyList())
        }
        val bifenDeferred: Deferred<List<Zb8Match>>? = if (isToday) async(Dispatchers.IO) {
            runCatching {
                val json = fetchSync("https://bifen4m.qiumibao.com/json/list.htm?_t=${System.currentTimeMillis()}")
                parseBifenListJson(json)
            }.getOrDefault(emptyList())
        } else null

        val recordMatches = recordDeferred.await()
        val bifenMatches = bifenDeferred?.await().orEmpty()

        val merged = LinkedHashMap<String, Zb8Match>()
        // 先放入 record（含队徽与球星高光统计）
        for (m in recordMatches) {
            merged[m.id] = m
        }
        // 用 bifen4m 实时比分流覆盖实时状态与比分，并补充未开赛/正在直播的场次
        for (live in bifenMatches) {
            val existing = merged[live.id]
            if (existing != null) {
                merged[live.id] = existing.copy(
                    scoreA = live.scoreA.ifBlank { existing.scoreA },
                    scoreB = live.scoreB.ifBlank { existing.scoreB },
                    state = live.state.ifBlank { existing.state },
                    period = live.period.ifBlank { existing.period }
                )
            } else if (live.date == targetDate || live.date.isBlank()) {
                merged[live.id] = live
            }
        }

        val sorted = merged.values.sortedWith(
            compareByDescending<Zb8Match> { if (it.isLive) 2 else if (!it.isFinished) 1 else 0 }
                .thenBy { it.time }
        )

        if (sport == "all") sorted else sorted.filter { match ->
            when (sport) {
                "football" -> match.type == "football" || match.competition.contains("超") || match.competition.contains("甲") || match.competition.contains("欧冠") || match.competition.contains("足")
                "basketball" -> match.type == "basketball" || match.competition.contains("NBA") || match.competition.contains("CBA") || match.competition.contains("篮")
                else -> match.type != "football" && match.type != "basketball"
            }
        }
    }

    internal fun parseBifenListJson(json: String): List<Zb8Match> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("list") ?: return emptyList()
        val out = ArrayList<Zb8Match>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val teamA = o.optString("home_team").trim()
            val teamB = o.optString("visit_team").trim()
            if (teamA.isBlank() || teamB.isBlank()) continue
            val id = o.optString("id")
            val rawType = o.optString("type").lowercase(Locale.ROOT)
            val type = when {
                rawType.contains("basket") || rawType.contains("nba") -> "basketball"
                rawType.contains("foot") || rawType.contains("zuqiu") -> "football"
                else -> "other"
            }
            val period = o.optString("period_cn")
            val state = o.optString("state")
            out.add(
                Zb8Match(
                    id = id,
                    time = o.optString("time"),
                    date = o.optString("sdate"),
                    competition = o.optString("league_name_cn").ifBlank { if (type == "basketball") "篮球赛事" else if (type == "football") "足球赛事" else "综合赛事" },
                    type = type,
                    teamA = teamA,
                    teamB = teamB,
                    scoreA = o.optString("home_score"),
                    scoreB = o.optString("visit_score"),
                    state = state,
                    period = period,
                    url = absolute(o.optString("url"), "https://m.zhibo8.com") ?: "https://m.zhibo8.com/"
                )
            )
        }
        return out
    }

    /** 5. 原生比赛直播间数据（单节小分 + 球星表现 + 实时中文文字直播流） */
    suspend fun matchLiveDetail(saishiId: String, dateStr: String): Zb8MatchLiveDetail = coroutineScope {
        val date = dateStr.ifBlank { todayString() }
        val scoreDeferred = async(Dispatchers.IO) {
            runCatching { fetchSync("https://bifen4m.qiumibao.com/json/$date/v2/$saishiId.htm?_t=${System.currentTimeMillis()}") }.getOrNull()
        }
        val maxSidDeferred = async(Dispatchers.IO) {
            runCatching { fetchSync("https://dingshi4m.qiumibao.com/livetext/data/cache/livetext/$saishiId/0/max_sid.json?_t=${System.currentTimeMillis()}") }.getOrNull()
        }

        val scoreJson = scoreDeferred.await()
        val maxSid = maxSidDeferred.await()?.let {
            runCatching { JSONObject(it).optInt("max_sid", 0) }.getOrDefault(0)
        } ?: 0

        val liveTexts = ArrayList<Zb8LiveText>()
        if (maxSid > 0) {
            val sids = listOf(maxSid, maxSid - 2, maxSid - 4).filter { it > 0 }.distinct()
            for (sid in sids) {
                val txt = runCatching {
                    fetchSync("https://dingshi4m.qiumibao.com/livetext/data/cache/livetext/$saishiId/0/lit_page_2/$sid.htm")
                }.getOrNull() ?: continue
                val arr = runCatching { JSONArray(txt) }.getOrNull() ?: continue
                for (i in arr.length() - 1 downTo 0) {
                    val o = arr.optJSONObject(i) ?: continue
                    val line = o.optString("live_text").trim()
                    if (line.isBlank()) continue
                    liveTexts.add(
                        Zb8LiveText(
                            sid = o.optString("live_sid", "$sid-$i"),
                            anchor = o.optString("user_chn").ifBlank { "直播员" },
                            text = line
                        )
                    )
                }
            }
        }

        val root = scoreJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val left = root?.optJSONObject("left")
        val right = root?.optJSONObject("right")

        fun parseScores(obj: JSONObject?): List<String> {
            val arr = obj?.optJSONArray("scores") ?: return emptyList()
            val list = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val v = arr.optString(i)
                if (i < 4 || (v.isNotBlank() && v != "0")) list.add(v)
            }
            return list
        }

        fun parseStars(obj: JSONObject?): List<Zb8StarStat> {
            val arr = obj?.optJSONArray("player_data") ?: return emptyList()
            val list = ArrayList<Zb8StarStat>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val n = p.optString("player_name").trim()
                val v = p.optString("value").trim()
                if (n.isNotBlank() && v.isNotBlank()) list.add(Zb8StarStat(n, v))
            }
            return list
        }

        Zb8MatchLiveDetail(
            saishiId = saishiId,
            date = date,
            periodCn = root?.optString("period_cn").orEmpty(),
            state = root?.optString("state").orEmpty(),
            scoreA = left?.optString("score").orEmpty(),
            scoreB = right?.optString("score").orEmpty(),
            quarterScoresA = parseScores(left),
            quarterScoresB = parseScores(right),
            starStatsA = parseStars(left),
            starStatsB = parseStars(right),
            liveTexts = liveTexts.distinctBy { it.sid },
            maxSid = maxSid
        )
    }

    /** 6. 联赛积分榜与排名 */
    suspend fun standings(league: Zb8League): List<Zb8Standing> = withContext(Dispatchers.IO) {
        val jsonStr = fetchSync(league.url)
        val root = JSONObject(jsonStr)
        val out = ArrayList<Zb8Standing>()
        val dataArr = root.optJSONArray("data") ?: return@withContext emptyList()

        if (league.type == LeagueType.BASKETBALL) {
            for (i in 0 until dataArr.length()) {
                val g = dataArr.optJSONObject(i) ?: continue
                val title = g.optString("title").ifBlank { if (i == 0) "西部" else "东部" }
                val listArr = g.optJSONArray("list") ?: continue
                for (j in 0 until listArr.length()) {
                    val o = listArr.optJSONObject(j) ?: continue
                    out.add(
                        Zb8Standing(
                            rank = o.optInt("排名", j + 1),
                            teamName = o.optString("球队").trim(),
                            teamLogo = normalizeUrl(o.optString("球队图标")),
                            won = o.optString("胜"),
                            lost = o.optString("负"),
                            winRate = o.optString("胜率"),
                            streak = o.optString("近况"),
                            gamesBehind = o.optString("胜差"),
                            points = o.optString("积分"),
                            groupTitle = title
                        )
                    )
                }
            }
        } else {
            val first = dataArr.optJSONObject(0)
            if (first != null && first.has("list")) {
                for (i in 0 until dataArr.length()) {
                    val g = dataArr.optJSONObject(i) ?: continue
                    val gTitle = g.optString("title")
                    val lArr = g.optJSONArray("list") ?: continue
                    for (j in 0 until lArr.length()) {
                        val o = lArr.optJSONObject(j) ?: continue
                        out.add(parseFootballRow(o, j + 1, gTitle))
                    }
                }
            } else {
                for (i in 0 until dataArr.length()) {
                    val o = dataArr.optJSONObject(i) ?: continue
                    out.add(parseFootballRow(o, i + 1, ""))
                }
            }
        }
        out
    }

    /** 7. 球员榜（兼容足球扁平数组与 NBA 分组数组：得分/篮板/助攻/射手） */
    suspend fun players(url: String): List<Zb8Player> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        val jsonStr = fetchSync(url)
        val root = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return@withContext emptyList()
        val dataArr = root.optJSONArray("data") ?: return@withContext emptyList()

        val first = dataArr.optJSONObject(0)
        val itemArrays = if (first != null && first.has("list")) {
            (0 until dataArr.length()).mapNotNull { dataArr.optJSONObject(it)?.optJSONArray("list") }
        } else {
            listOf(dataArr)
        }

        val out = ArrayList<Zb8Player>()
        for (arr in itemArrays) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("球员").ifBlank { o.optString("球员名称") }.trim()
                if (name.isBlank()) continue
                val mainVal = o.optString("进球")
                    .ifBlank { o.optString("助攻") }
                    .ifBlank { o.optString("场均") }
                    .ifBlank { o.optString("数值") }
                val sub = when {
                    o.optString("点球").isNotBlank() -> "点球 ${o.optString("点球")}"
                    o.optString("场次").isNotBlank() -> "${o.optString("场次")}场"
                    else -> ""
                }
                out.add(
                    Zb8Player(
                        rank = o.optString("排名").toIntOrNull() ?: (i + 1),
                        playerName = name,
                        playerLogo = normalizeUrl(o.optString("球员图标").ifBlank { o.optString("球员头像") }),
                        teamName = o.optString("球队").ifBlank { o.optString("球队名称") }.trim(),
                        teamLogo = normalizeUrl(o.optString("球队图标")),
                        count = mainVal,
                        subCount = sub,
                        playerId = o.optString("playerId").ifBlank { o.optString("球员id") },
                        teamId = o.optString("teamId").ifBlank { o.optString("球队id") }
                    )
                )
            }
        }
        out
    }

    /** 8. 球队全赛季/近半月赛程战绩（解决点击球队没有赛程的问题） */
    suspend fun teamSeasonMatches(teamName: String, league: Zb8League? = null, sport: String = "all"): List<Zb8Match> = coroutineScope {
        val target = teamName.trim()
        if (target.isBlank()) return@coroutineScope emptyList()

        val schUrl = league?.scheduleUrl.orEmpty()
        if (schUrl.isNotBlank()) {
            val raw = runCatching { fetchSync(schUrl) }.getOrNull()
            val parsed = raw?.let { parseLeagueSeasonSchedule(it, league!!.name, if (league.type == LeagueType.BASKETBALL) "basketball" else "football") }
                .orEmpty()
                .filter { it.teamA == target || it.teamB == target }
            if (parsed.isNotEmpty()) {
                val upcoming = parsed.filter { !it.isFinished }
                val finished = parsed.filter { it.isFinished }.reversed()
                return@coroutineScope upcoming + finished
            }
        }

        // NBA 或无独立联赛赛程表：并发拉取前后共 11 天的官方赛程归档
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val dates = (-6..4).map { offset ->
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.DAY_OF_YEAR, offset)
            fmt.format(c.time)
        }
        val dayLists = dates.map { d ->
            async(Dispatchers.IO) { runCatching { matches(dateStr = d, sport = sport) }.getOrDefault(emptyList()) }
        }.flatMap { it.await() }

        dayLists.filter { it.teamA == target || it.teamB == target }
            .distinctBy { it.id }
            .sortedWith(compareBy<Zb8Match> { it.isFinished }.thenByDescending { it.date }.thenBy { it.time })
    }

    private fun parseLeagueSeasonSchedule(json: String, leagueName: String, sportType: String): List<Zb8Match> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val rounds = root.optJSONArray("data") ?: return emptyList()
        val out = ArrayList<Zb8Match>()
        for (ri in 0 until rounds.length()) {
            val round = rounds.optJSONObject(ri) ?: continue
            val roundTitle = round.optString("title")
            val list = round.optJSONArray("list") ?: continue
            for (i in 0 until list.length()) {
                val o = list.optJSONObject(i) ?: continue
                val tA = o.optString("主队名称").ifBlank { o.optString("主队") }.trim()
                val tB = o.optString("客队名称").ifBlank { o.optString("客队") }.trim()
                if (tA.isBlank() || tB.isBlank()) continue
                val scoreRaw = o.optString("比分").trim()
                val parts = scoreRaw.split("-", ":")
                val sA = if (parts.size == 2 && parts[0].trim().all { it.isDigit() }) parts[0].trim() else ""
                val sB = if (parts.size == 2 && parts[1].trim().all { it.isDigit() }) parts[1].trim() else ""
                val finished = o.optInt("is_finish", 0) == 1 || (sA.isNotBlank() && sB.isNotBlank())
                val date = o.optString("日期")
                val timeStr = o.optString("时间").substringAfter(" ").ifBlank { o.optString("时间") }
                out.add(
                    Zb8Match(
                        id = o.optString("saishi_id", "$leagueName-$ri-$i"),
                        time = timeStr,
                        date = date,
                        competition = if (roundTitle.isNotBlank()) "$leagueName · $roundTitle" else leagueName,
                        type = sportType,
                        teamA = tA,
                        teamB = tB,
                        logoA = normalizeUrl(o.optString("主队图标")),
                        logoB = normalizeUrl(o.optString("客队图标")),
                        scoreA = sA,
                        scoreB = sB,
                        state = if (finished) "3" else "0",
                        period = if (finished) "完赛" else date,
                        url = absolute(o.optString("内页"), "https://m.zhibo8.com") ?: "https://m.zhibo8.com/"
                    )
                )
            }
        }
        return out
    }

    /** 9. 直播吧近 5 日全量新闻 JSON 归档检索（解决首页 20 条匹配不到球队/球员新闻的问题） */
    suspend fun searchArchiveNews(keyword: String, sport: String = "all"): List<Zb8Item> = coroutineScope {
        val kw = keyword.trim()
        if (kw.isBlank()) return@coroutineScope emptyList()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val dates = (0 downTo -4).map { offset ->
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.DAY_OF_YEAR, offset)
            fmt.format(c.time)
        }
        val channels = when (sport) {
            "basketball" -> listOf("nba")
            "football" -> listOf("zuqiu")
            else -> listOf("nba", "zuqiu")
        }
        val allItems = dates.flatMap { d ->
            channels.map { ch ->
                async(Dispatchers.IO) {
                    runCatching {
                        val json = fetchSync("https://news.zhibo8.cc/$ch/json/$d.htm")
                        parseArchiveNewsJson(json)
                    }.getOrDefault(emptyList())
                }
            }
        }.flatMap { it.await() }

        allItems.distinctBy { it.url }
            .filter { it.title.contains(kw, ignoreCase = true) || it.tag.contains(kw, ignoreCase = true) }
            .take(35)
    }

    private fun parseArchiveNewsJson(json: String): List<Zb8Item> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("video_arr") ?: return emptyList()
        val out = ArrayList<Zb8Item>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title").ifBlank { o.optString("indextitle") }.trim()
            val rawUrl = o.optString("url").trim()
            if (title.isBlank() || rawUrl.isBlank()) continue
            val fullUrl = when {
                rawUrl.startsWith("http") -> rawUrl
                rawUrl.startsWith("/news/web") -> "https://m.zhibo8.com$rawUrl"
                rawUrl.startsWith("/") -> "https://m.zhibo8.com/news/web$rawUrl"
                else -> "https://m.zhibo8.com/news/web/$rawUrl"
            }
            out.add(
                Zb8Item(
                    title = title,
                    url = fullUrl,
                    thumb = normalizeUrl(o.optString("thumbnail")),
                    tag = o.optString("lable").ifBlank { o.optString("type") },
                    time = o.optString("createtime").substringAfter(" ").take(5),
                    comments = 0,
                    isHighlight = rawUrl.contains("jijin")
                )
            )
        }
        return out
    }

    private fun parseFootballRow(o: JSONObject, fallbackRank: Int, groupTitle: String): Zb8Standing {
        val rank = o.optString("排名").toIntOrNull() ?: fallbackRank
        val teamName = o.optString("球队名称").ifBlank { o.optString("球队") }.trim()
        return Zb8Standing(
            rank = rank,
            teamName = teamName,
            teamLogo = normalizeUrl(o.optString("球队图标")),
            played = o.optString("场次", o.optString("场数")),
            won = o.optString("胜"),
            drawn = o.optString("平"),
            lost = o.optString("负"),
            goalsDiff = o.optString("净胜球"),
            points = o.optString("积分"),
            tag = o.optString("标签"),
            groupTitle = groupTitle,
            teamId = o.optString("teamId").ifBlank { o.optString("球队id") }
        )
    }

    private fun parseRecordJson(json: String, date: String): List<Zb8Match> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("list") ?: return emptyList()
        val out = ArrayList<Zb8Match>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val left = o.optJSONObject("left_team")
            val right = o.optJSONObject("right_team")
            val leagueObj = o.optJSONObject("league")
            val teamA = (left?.optString("name") ?: o.optString("home_team")).trim()
            val teamB = (right?.optString("name") ?: o.optString("visit_team")).trim()
            if (teamA.isBlank() || teamB.isBlank()) continue

            val keyInfo = o.optJSONObject("match_key_info")
            val lp = keyInfo?.optJSONArray("left")?.optJSONObject(0)
            val rp = keyInfo?.optJSONArray("right")?.optJSONObject(0)
            val keyStat = listOfNotNull(
                lp?.let { "${it.optString("player_name")} ${it.optString("value")}".trim() }?.takeIf { it.isNotBlank() },
                rp?.let { "${it.optString("player_name")} ${it.optString("value")}".trim() }?.takeIf { it.isNotBlank() }
            ).joinToString("  |  ")

            val rawUrl = o.optString("news_url").ifBlank { o.optString("url") }
            val fullUrl = absolute(rawUrl, "https://m.zhibo8.com") ?: "https://m.zhibo8.com/"
            val compName = leagueObj?.optString("name_cn")?.ifBlank { o.optString("rounds") } ?: "赛事"
            val rawType = o.optString("type").lowercase(Locale.ROOT)
            val type = when {
                rawType.contains("basket") -> "basketball"
                rawType.contains("foot") || rawType.contains("zuqiu") -> "football"
                else -> "other"
            }

            out.add(
                Zb8Match(
                    id = o.optString("saishi_id", "$date-$i"),
                    time = o.optString("stime"),
                    date = date,
                    competition = compName,
                    channel = if (o.optString("status") == "finished") "战报" else "",
                    type = type,
                    teamA = teamA,
                    teamB = teamB,
                    logoA = normalizeUrl(left?.optString("logo_url")),
                    logoB = normalizeUrl(right?.optString("logo_url")),
                    scoreA = left?.optString("score").orEmpty(),
                    scoreB = right?.optString("score").orEmpty(),
                    state = if (o.optString("status") == "finished") "3" else "0",
                    period = if (o.optString("status") == "finished") "完赛" else "未开赛",
                    url = fullUrl,
                    keyStat = keyStat
                )
            )
        }
        return out
    }

    private fun parseNews(html: String, baseUrl: String, highlightsOnly: Boolean): List<Zb8Item> {
        val doc = Jsoup.parse(html, baseUrl)
        val nodes = doc.select("ul.news-lists li, ul.lists li, ul.ent li, div.list li.lite")
        val out = LinkedHashMap<String, Zb8Item>()
        for (li in nodes) {
            val cls = li.className()
            if (cls.contains("adv") || cls.contains("advert")) continue
            if (li.selectFirst(".team-box") != null) continue
            val a = li.selectFirst("h2 > a[href]")
                ?: li.selectFirst("h3 > a[href]")
                ?: li.selectFirst("a[href]")
                ?: continue
            val href = a.attr("href").trim()
            if (href.isEmpty() || href.startsWith("javascript") || !href.contains(".htm")) continue
            if (href.contains("svideo.htm")) continue
            if (highlightsOnly && !href.contains("jijin.htm")) continue

            val title = a.text().trim().ifBlank { li.selectFirst(".title")?.text()?.trim().orEmpty() }
            if (title.length < 6) continue
            val full = absolute(href, "https://m.zhibo8.com") ?: continue
            if (out.containsKey(full)) continue

            val thumb = li.selectFirst("img")?.let { imgOf(it) }
            val tag = li.attr("type").ifBlank {
                li.attr("label").split(",").firstOrNull { it.isNotBlank() }.orEmpty()
            }.ifBlank { href.substringAfter("/news/web/").substringBefore("/") }
            val time = li.selectFirst(".pass_time")?.text()?.trim().orEmpty()
            val comments = li.selectFirst(".pinglun")?.text()?.trim()?.toIntOrNull() ?: 0
            out[full] = Zb8Item(title, full, thumb, tag, time, comments, isHighlight = href.contains("jijin.htm"))
        }
        return out.values.toList()
    }

    private fun imgOf(img: Element): String? {
        val src = img.attr("data-original")
            .ifBlank { img.attr("data-src") }
            .ifBlank { img.attr("src") }
            .trim()
        if (src.isEmpty() || src.contains("video_play.png") || src.contains("alarm.png") || src.contains("ic_play")) return null
        return normalizeUrl(src)
    }

    private fun normalizeUrl(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        if (url.isBlank()) return null
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
            else -> url
        }
    }

    private fun absolute(rel: String, base: String): String? {
        if (rel.isBlank()) return null
        if (rel.startsWith("http://") || rel.startsWith("https://")) return rel
        if (rel.startsWith("//")) return "https:$rel"
        if (!rel.startsWith("/")) return "$base/$rel"
        return "${base.trimEnd('/')}$rel"
    }

    fun todayString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

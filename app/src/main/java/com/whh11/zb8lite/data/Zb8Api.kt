package com.whh11.zb8lite.data

import kotlinx.coroutines.Dispatchers
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
 * 直播吧增强数据层：
 * 1. 资讯：从 m.zhibo8.com 抓取各频道与评论数；
 * 2. 赛程：接入官方 s.qiumibao.com/json/record/{date}.htm，具备真比分、球员统计、完赛/直播状态，
 *    并融合 m.zhibo8.com 首页的未开赛/正在进行赛事；
 * 3. 数据：接入 stats.qiumibao.com 官方积分榜/排名（NBA、CBA、英超、西甲、意甲、德甲、法甲、中超、欧冠）；
 * 4. 圈子/社区：按焦点专区与热门讨论聚合高质量讨论流。
 */
object Zb8Api {

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val CHANNELS = listOf(
        Zb8Channel("https://m.zhibo8.com/", "头条"),
        Zb8Channel("https://m.zhibo8.com/zuqiu/", "足球"),
        Zb8Channel("https://m.zhibo8.com/nba/", "篮球"),
        Zb8Channel("https://m.zhibo8.com/other/", "综合"),
        Zb8Channel("https://m.zhibo8.com/video.htm", "集锦", highlightsOnly = true)
    )

    val LEAGUES = listOf(
        Zb8League("nba", "NBA", LeagueType.BASKETBALL, "https://stats.qiumibao.com/data/json_v2/nba_paihang.htm"),
        Zb8League("epl", "英超", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85"),
        Zb8League("laliga", "西甲", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2"),
        Zb8League("seriea", "意甲", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2"),
        Zb8League("bundesliga", "德甲", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2"),
        Zb8League("csl", "中超", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85"),
        Zb8League("ucl", "欧冠", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=10&league=%E6%AC%A7%E5%86%A0"),
        Zb8League("cba", "CBA", LeagueType.BASKETBALL, "https://stats.qiumibao.com/data/json_v2/cba_paihang.htm"),
        Zb8League("ligue1", "法甲", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2")
    )

    val TOPICS = listOf(
        Zb8Topic("all", "全部热门", "热门", ""),
        Zb8Topic("lakers", "湖人专区", "篮球", "湖人"),
        Zb8Topic("warriors", "勇士专区", "篮球", "勇士"),
        Zb8Topic("celtics", "绿军专区", "篮球", "凯尔特人"),
        Zb8Topic("clippers", "快船专区", "篮球", "快船"),
        Zb8Topic("manutd", "曼联专区", "足球", "曼联"),
        Zb8Topic("arsenal", "阿森纳专区", "足球", "阿森纳"),
        Zb8Topic("realm", "皇马专区", "足球", "皇马"),
        Zb8Topic("barca", "巴萨专区", "足球", "巴萨"),
        Zb8Topic("mancity", "曼城专区", "足球", "曼城"),
        Zb8Topic("china", "国足专区", "足球", "中国男足")
    )

    suspend fun news(channel: Zb8Channel): List<Zb8Item> = withContext(Dispatchers.IO) {
        parseNews(fetch(channel.url), channel.url, channel.highlightsOnly)
    }

    /** 获取赛程：传入 yyyy-MM-dd，留空则为当天 */
    suspend fun matches(dateStr: String = ""): List<Zb8Match> = withContext(Dispatchers.IO) {
        val targetDate = if (dateStr.isNotBlank()) dateStr else todayString()
        val isToday = targetDate == todayString()

        val recordMatches = runCatching {
            val json = fetch("https://s.qiumibao.com/json/record/$targetDate.htm?_t=${System.currentTimeMillis()}")
            parseRecordJson(json, targetDate)
        }.getOrDefault(emptyList())

        if (!isToday) {
            return@withContext recordMatches
        }

        // 今天：将 record.htm（含真实比分与完赛数据）与 m.zhibo8.com（未开赛/正在进行的赛事）融合
        val liveUpcoming = runCatching {
            parseLiveMatches(fetch("https://m.zhibo8.com/"))
        }.getOrDefault(emptyList())

        val combined = ArrayList<Zb8Match>()
        // 优先展示正在进行与未开赛的，接着展示已完赛的真实比分
        val liveList = liveUpcoming.filter { it.isLive }
        val upcomingList = liveUpcoming.filter { !it.isLive && !it.isFinished }

        combined.addAll(liveList)
        combined.addAll(recordMatches)
        
        // 补上尚未开赛的预告场次（去重）
        val existingNames = recordMatches.map { "${it.teamA}-${it.teamB}" }.toSet()
        for (up in upcomingList) {
            if ("${up.teamA}-${up.teamB}" !in existingNames) {
                combined.add(up)
            }
        }
        combined
    }

    /** 联赛积分榜/排行 */
    suspend fun standings(league: Zb8League): List<Zb8Standing> = withContext(Dispatchers.IO) {
        val jsonStr = fetch(league.url)
        val root = JSONObject(jsonStr)
        val out = ArrayList<Zb8Standing>()

        if (league.type == LeagueType.BASKETBALL) {
            // NBA / CBA: data 为分组数组 [ { title: "西部", list: [...] }, { title: "东部", list: [...] } ]
            val dataArr = root.optJSONArray("data") ?: return@withContext emptyList()
            for (i in 0 until dataArr.length()) {
                val groupObj = dataArr.optJSONObject(i) ?: continue
                val title = groupObj.optString("title").ifBlank { if (i == 0) "西部" else "东部" }
                val listArr = groupObj.optJSONArray("list") ?: continue
                for (j in 0 until listArr.length()) {
                    val o = listArr.optJSONObject(j) ?: continue
                    out.add(
                        Zb8Standing(
                            rank = o.optInt("排名", j + 1),
                            teamName = o.optString("球队"),
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
            // 足球：data 可能是平铺数组或带 list 的分组
            val dataArr = root.optJSONArray("data")
            if (dataArr != null && dataArr.length() > 0) {
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
        }
        out
    }

    private fun parseFootballRow(o: JSONObject, fallbackRank: Int, groupTitle: String): Zb8Standing {
        val rankStr = o.optString("排名")
        val rank = rankStr.toIntOrNull() ?: fallbackRank
        val teamName = o.optString("球队名称").ifBlank { o.optString("球队") }
        return Zb8Standing(
            rank = rank,
            teamName = teamName.trim(),
            teamLogo = normalizeUrl(o.optString("球队图标")),
            played = o.optString("场次"),
            won = o.optString("胜"),
            drawn = o.optString("平"),
            lost = o.optString("负"),
            goalsDiff = o.optString("净胜球"),
            points = o.optString("积分"),
            tag = o.optString("标签"),
            groupTitle = groupTitle
        )
    }

    private fun parseRecordJson(json: String, date: String): List<Zb8Match> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("list") ?: return emptyList()
        val out = ArrayList<Zb8Match>(arr.length())

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val left = o.optJSONObject("left_team")
            val right = o.optJSONObject("right_team")
            val leagueObj = o.optJSONObject("league")

            val teamA = left?.optString("name") ?: o.optString("home_team")
            val teamB = right?.optString("name") ?: o.optString("visit_team")
            if (teamA.isBlank() || teamB.isBlank()) continue

            val scoreA = left?.optString("score") ?: ""
            val scoreB = right?.optString("score") ?: ""

            val logoA = left?.optString("logo_url")?.let { normalizeUrl(it) }
            val logoB = right?.optString("logo_url")?.let { normalizeUrl(it) }

            // 焦点球星关键表现
            val keyInfo = o.optJSONObject("match_key_info")
            val leftPlayer = keyInfo?.optJSONArray("left")?.optJSONObject(0)
            val rightPlayer = keyInfo?.optJSONArray("right")?.optJSONObject(0)
            val keyStat = buildString {
                if (leftPlayer != null) {
                    append(leftPlayer.optString("player_name"))
                    append(" ")
                    append(leftPlayer.optString("value"))
                }
                if (rightPlayer != null) {
                    if (isNotEmpty()) append(" | ")
                    append(rightPlayer.optString("player_name"))
                    append(" ")
                    append(rightPlayer.optString("value"))
                }
            }

            val rawUrl = o.optString("news_url").ifBlank { o.optString("url") }
            val fullUrl = absolute(rawUrl, "https://m.zhibo8.com") ?: "https://m.zhibo8.com/"

            val compName = leagueObj?.optString("name_cn")?.ifBlank { o.optString("rounds") } ?: "比赛"

            out.add(
                Zb8Match(
                    id = o.optString("saishi_id", "$date-$i"),
                    time = o.optString("stime"),
                    date = date,
                    competition = compName,
                    channel = if (o.optString("status") == "finished") "已完赛" else "",
                    teamA = teamA,
                    teamB = teamB,
                    logoA = logoA,
                    logoB = logoB,
                    scoreA = scoreA,
                    scoreB = scoreB,
                    state = if (o.optString("status") == "finished") "完赛" else "比赛",
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

            val title = a.text().trim().ifBlank {
                li.selectFirst(".title")?.text()?.trim().orEmpty()
            }
            if (title.length < 6) continue

            val full = absolute(href, "https://m.zhibo8.com") ?: continue
            if (out.containsKey(full)) continue

            val thumb = li.selectFirst("img")?.let { imgOf(it) }
            val tag = li.attr("type").ifBlank {
                li.attr("label").split(",").firstOrNull { it.isNotBlank() }.orEmpty()
            }.ifBlank { href.substringAfter("/news/web/").substringBefore("/") }

            val time = li.selectFirst(".pass_time")?.text()?.trim().orEmpty()
            val commentStr = li.selectFirst(".pinglun")?.text()?.trim().orEmpty()
            val comments = commentStr.toIntOrNull() ?: 0

            out[full] = Zb8Item(title, full, thumb, tag, time, comments)
        }
        return out.values.toList()
    }

    private fun parseLiveMatches(html: String): List<Zb8Match> {
        val doc = Jsoup.parse(html, "https://m.zhibo8.com/")
        val out = ArrayList<Zb8Match>()

        for (li in doc.select("li.item")) {
            if (li.selectFirst(".team-box") == null) continue

            val teamA = li.selectFirst(".team.left-team .team-name")?.text()?.trim().orEmpty()
            val teamB = li.selectFirst(".team.right-team .team-name")?.text()?.trim().orEmpty()
            if (teamA.isBlank() || teamB.isBlank()) continue

            val href = li.attr("href").trim()
            val full = absolute(href, "https://m.zhibo8.com").orEmpty()
            val id = li.attr("id").ifBlank { "${teamA}-${teamB}".hashCode().toString() }

            out += Zb8Match(
                id = id,
                time = li.selectFirst(".time")?.text()?.trim().orEmpty(),
                date = todayString(),
                competition = li.selectFirst(".s_name")?.text()?.trim().orEmpty(),
                channel = li.selectFirst(".s_keywords")?.text()?.trim().orEmpty(),
                teamA = teamA,
                teamB = teamB,
                logoA = li.selectFirst(".team.left-team img")?.let { imgOf(it) },
                logoB = li.selectFirst(".team.right-team img")?.let { imgOf(it) },
                state = li.selectFirst(".remind")?.text()?.trim().orEmpty().ifBlank { "未开赛" },
                url = full
            )
        }
        return out
    }

    private fun fetch(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "*/*")
            .build()
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
    }

    private fun imgOf(img: Element): String? {
        val raw = img.attr("data-original").ifBlank { img.attr("data-src") }.ifBlank { img.attr("src") }
        return absolute(raw.trim(), "https://m.zhibo8.com")
    }

    private fun normalizeUrl(raw: String): String? {
        if (raw.isBlank()) return null
        return when {
            raw.startsWith("http://") -> raw.replace("http://", "https://")
            raw.startsWith("https://") -> raw
            raw.startsWith("//") -> "https:$raw"
            else -> "https://duihui.duoduocdn.com/$raw"
        }
    }

    private fun absolute(raw: String, base: String): String? {
        if (raw.isBlank()) return null
        return when {
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("http") -> raw.replace("http://", "https://")
            raw.startsWith("/") -> base + raw
            else -> null
        }
    }

    fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}

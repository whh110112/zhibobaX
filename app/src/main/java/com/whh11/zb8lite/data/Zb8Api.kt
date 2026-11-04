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
 * 直播吧增强数据层（全品类赛程与数据版）：
 * 1. 资讯：从 m.zhibo8.com 抓取各频道与评论数；
 * 2. 赛程：接入官方 s.qiumibao.com/json/record/{date}.htm 与移动端首页，
 *    完整支持全部、足球、篮球、电竞、体坛综合多项目筛选、真实比分与球星统计；
 * 3. 数据：覆盖篮球（NBA/CBA）、欧洲足球、亚洲足球、美洲/非洲、电竞与综合赛事的积分榜与排名；
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

    // 全项目联赛数据配置（支持篮球、欧洲足球、亚洲足球、美洲/非洲、电竞与综合）
    val LEAGUES = listOf(
        // 1. 篮球
        Zb8League("nba", "NBA", "篮球", LeagueType.BASKETBALL, "https://stats.qiumibao.com/data/json_v2/nba_paihang.htm", "NBA 东西部排行榜"),
        Zb8League("cba", "CBA", "篮球", LeagueType.BASKETBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E6%8E%92%E8%A1%8C&tab=%E6%8E%92%E8%A1%8C&league_id=925&league=CBA", "CBA 常规赛排行榜"),

        // 2. 欧洲足球
        Zb8League("epl", "英超", "欧洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85", "", "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85"),
        Zb8League("laliga", "西甲", "欧洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2", "", "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2"),
        Zb8League("seriea", "意甲", "欧洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2", "", "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2"),
        Zb8League("bundesliga", "德甲", "欧洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2", "", "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2"),
        Zb8League("ligue1", "法甲", "欧洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2", "", "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2"),
        Zb8League("ucl", "欧冠", "欧洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B", "", "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B"),
        Zb8League("uel", "欧联杯", "欧洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=14&league=%E6%AC%A7%E8%81%94%E6%9D%AF"),

        // 3. 亚洲足球
        Zb8League("csl", "中超", "亚洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85", "", "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85"),
        Zb8League("acl", "亚冠精英", "亚洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=226&league=%E4%BA%9A%E5%86%A0%E7%B2%BE%E8%8B%B1"),
        Zb8League("saudi", "沙特联", "亚洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=318&league=%E6%B2%99%E7%89%B9%E8%B6%85"),
        Zb8League("cl1", "中甲", "亚洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=129&league=%E4%B8%AD%E7%94%B2"),
        Zb8League("aleague", "澳超", "亚洲足球", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=258&league=%E6%BE%B3A%E8%81%94"),

        // 4. 美洲/非洲
        Zb8League("mls", "美职联", "美洲/非洲", LeagueType.BASKETBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=244&league=%E7%BE%8E%E8%81%8C%E8%81%94", "美职联 东西区积分榜"),
        Zb8League("brasileirao", "巴甲", "美洲/非洲", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=219&league=%E5%B7%B4%E7%94%B2"),
        Zb8League("arg", "阿超", "美洲/非洲", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=213&league=%E9%98%BF%E7%94%B2"),
        Zb8League("afcon", "非洲杯", "美洲/非洲", LeagueType.FOOTBALL, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=385&league=%E9%9D%9E%E6%B4%B2%E6%9D%AF"),

        // 5. 电竞与综合
        Zb8League("s_wc", "S赛 (LOL)", "电竞与综合", LeagueType.GAME, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E6%8E%92%E8%A1%8C&tab=%E6%8E%92%E8%A1%8C&league_id=3534", "英雄联盟全球总决赛"),
        Zb8League("ti_dota", "TI (DOTA2)", "电竞与综合", LeagueType.GAME, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8F%82%E8%B5%9B%E9%98%9F%E4%BC%8D&tab=%E7%90%83%E9%98%9F%E6%A6%9C&league_id=3536", "DOTA2 国际邀请赛"),
        Zb8League("f1", "F1 赛车", "电竞与综合", LeagueType.OTHER, "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BD%A6%E6%89%8B%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=3132&league=F1", "F1 世界一级方程式锦标赛")
    )

    // 社区专区话题
    val TOPICS = listOf(
        Zb8Topic("all", "全部热议", "热点", "", "全站热评最高的热议话题"),
        Zb8Topic("lakers", "湖人专区", "NBA", "湖人", "紫金军团讨论区"),
        Zb8Topic("warriors", "勇士专区", "NBA", "勇士", "金州勇士讨论区"),
        Zb8Topic("celtics", "凯尔特人", "NBA", "凯尔特人", "绿军总冠军讨论区"),
        Zb8Topic("clippers", "快船专区", "NBA", "快船", "洛杉矶快船专区"),
        Zb8Topic("rockets", "火箭专区", "NBA", "火箭", "休斯顿火箭专区"),
        Zb8Topic("manutd", "曼联专区", "足球", "曼联", "红魔曼联讨论区"),
        Zb8Topic("arsenal", "阿森纳", "足球", "阿森纳", "兵工厂枪迷专区"),
        Zb8Topic("realm", "皇马专区", "足球", "皇家马德里", "银河战舰讨论区"),
        Zb8Topic("barca", "巴萨专区", "足球", "巴塞罗那", "红蓝巴萨讨论区"),
        Zb8Topic("mancity", "曼城专区", "足球", "曼城", "蓝月亮讨论区"),
        Zb8Topic("china", "国足专区", "足球", "国家队", "中国男足与中国足球讨论区")
    )

    suspend fun news(channel: Zb8Channel): List<Zb8Item> = withContext(Dispatchers.IO) {
        val html = fetch(channel.url)
        parseNews(html, channel.url, channel.highlightsOnly)
    }

    /** 获取赛程：传入 yyyy-MM-dd 与运动项目（all/football/basketball/game/other） */
    suspend fun matches(dateStr: String = "", sport: String = "all"): List<Zb8Match> = withContext(Dispatchers.IO) {
        val targetDate = if (dateStr.isNotBlank()) dateStr else todayString()
        val isToday = targetDate == todayString()

        val recordMatches = runCatching {
            val json = fetch("https://s.qiumibao.com/json/record/$targetDate.htm?_t=${System.currentTimeMillis()}")
            parseRecordJson(json, targetDate)
        }.getOrDefault(emptyList())

        val combined = ArrayList<Zb8Match>()
        if (!isToday) {
            combined.addAll(recordMatches)
        } else {
            // 今天：将 record.htm（含真实比分与完赛数据）与 m.zhibo8.com（未开赛/正在进行的赛事）融合
            val liveUpcoming = runCatching {
                parseLiveMatches(fetch("https://m.zhibo8.com/"))
            }.getOrDefault(emptyList())

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
        }

        // 项目过滤
        if (sport == "all") {
            combined
        } else {
            combined.filter { match ->
                when (sport) {
                    "football" -> match.type == "football" || match.competition.contains("超") || match.competition.contains("甲") || match.competition.contains("杯") || match.competition.contains("足")
                    "basketball" -> match.type == "basketball" || match.competition.contains("NBA") || match.competition.contains("CBA") || match.competition.contains("NCAA") || match.competition.contains("篮")
                    "game" -> match.type == "game" || match.competition.contains("S赛") || match.competition.contains("LPL") || match.competition.contains("KPL") || match.competition.contains("TI") || match.competition.contains("电竞")
                    else -> match.type == "other" || (!match.competition.contains("超") && !match.competition.contains("NBA") && !match.competition.contains("CBA") && !match.competition.contains("S赛"))
                }
            }
        }
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
            // 足球与综合：data 可能是平铺数组或带 list 的分组
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

    /**
     * 球员榜（射手榜 / 助攻榜）。
     * 数据源同积分榜，字段：排名 / playerId / teamId / 球队 / 球员 / 球员图标 / 球队图标 / 进球|助攻 / 点球
     */
    suspend fun players(url: String): List<Zb8Player> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        val jsonStr = fetch(url)
        val root = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return@withContext emptyList()
        val arr = root.optJSONArray("data") ?: return@withContext emptyList()
        val out = ArrayList<Zb8Player>(arr.length())

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("球员").ifBlank { o.optString("球员名称") }.trim()
            if (name.isBlank()) continue
            out.add(
                Zb8Player(
                    rank = o.optString("排名").toIntOrNull() ?: (i + 1),
                    playerName = name,
                    // 实测字段名是「球员图标」
                    playerLogo = normalizeUrl(
                        o.optString("球员图标").ifBlank { o.optString("球员头像") }
                    ),
                    teamName = o.optString("球队").ifBlank { o.optString("球队名称") }.trim(),
                    teamLogo = normalizeUrl(o.optString("球队图标")),
                    count = o.optString("进球", o.optString("助攻", o.optString("数值"))),
                    subCount = o.optString("点球").takeIf { it.isNotBlank() }?.let { "点球 $it" } ?: "",
                    playerId = o.optString("playerId").ifBlank { o.optString("球员id") },
                    teamId = o.optString("teamId").ifBlank { o.optString("球队id") }
                )
            )
        }
        out
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

            val rawType = o.optString("type").lowercase(Locale.ROOT)
            val type = when {
                rawType.contains("basket") -> "basketball"
                rawType.contains("foot") || rawType.contains("zuqiu") -> "football"
                rawType.contains("game") || rawType.contains("esport") -> "game"
                else -> "other"
            }

            out.add(
                Zb8Match(
                    id = o.optString("saishi_id", "$date-$i"),
                    time = o.optString("stime"),
                    date = date,
                    competition = compName,
                    channel = if (o.optString("status") == "finished") "已完赛" else "",
                    type = type,
                    teamA = teamA,
                    teamB = teamB,
                    logoA = logoA,
                    logoB = logoB,
                    scoreA = scoreA,
                    scoreB = scoreB,
                    state = if (o.optString("status") == "finished") "完赛" else "未开赛",
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

            val rawType = li.attr("type").lowercase(Locale.ROOT)
            val type = when {
                rawType.contains("basket") -> "basketball"
                rawType.contains("foot") || rawType.contains("zuqiu") -> "football"
                rawType.contains("game") -> "game"
                else -> "other"
            }

            out += Zb8Match(
                id = id,
                time = li.selectFirst(".time")?.text()?.trim().orEmpty(),
                competition = li.selectFirst(".s_name")?.text()?.trim().orEmpty().ifBlank { "赛事" },
                channel = li.selectFirst(".s_keywords")?.text()?.trim().orEmpty(),
                type = type,
                teamA = teamA,
                teamB = teamB,
                logoA = li.selectFirst(".team.left-team img")?.let { imgOf(it) },
                logoB = li.selectFirst(".team.right-team img")?.let { imgOf(it) },
                state = if (li.selectFirst(".live-box") != null) "进行中" else "未开赛",
                url = full
            )
        }
        return out
    }

    private fun imgOf(img: Element): String? {
        val src = img.attr("data-original").ifBlank {
            img.attr("data-src").ifBlank { img.attr("src") }
        }.trim()
        if (src.isEmpty() || src.contains("video_play.png") || src.contains("alarm.png")) return null
        return normalizeUrl(src)
    }

    private fun normalizeUrl(url: String): String {
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            else -> url
        }
    }

    private fun absolute(rel: String, base: String): String? {
        if (rel.startsWith("http://") || rel.startsWith("https://")) return rel
        if (rel.startsWith("//")) return "https:$rel"
        if (!rel.startsWith("/")) return "$base/$rel"
        return "${base.trimEnd('/')}$rel"
    }

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        val res = client.newCall(req).execute()
        res.body?.string().orEmpty()
    }

    fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

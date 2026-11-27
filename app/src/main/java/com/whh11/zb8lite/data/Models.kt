package com.whh11.zb8lite.data

data class Zb8Channel(
    val url: String,
    val label: String,
    val highlightsOnly: Boolean = false
)

data class Zb8Item(
    val title: String,
    val url: String,
    val thumb: String?,
    val tag: String,
    val time: String = "",
    val comments: Int = 0,
    val isHighlight: Boolean = false
)

sealed class Zb8Block {
    data class Paragraph(val text: String) : Zb8Block()
    data class Image(val url: String) : Zb8Block()
}

data class Zb8ArticleDetail(
    val url: String,
    val title: String,
    val time: String,
    val source: String,
    val filename: String,
    val blocks: List<Zb8Block>,
    val imageUrls: List<String>
)

data class Zb8Comment(
    val id: String,
    val userName: String,
    val avatar: String?,
    val content: String,
    val time: String,
    val upCount: Int,
    val downCount: Int,
    val device: String = "",
    val isHot: Boolean = false,
    val children: List<Zb8Comment> = emptyList()
)

data class Zb8CommentPage(
    val hotComments: List<Zb8Comment>,
    val comments: List<Zb8Comment>,
    val total: Int,
    val hasMore: Boolean
)

data class Zb8Match(
    val id: String,
    val time: String,
    val date: String = "",
    val competition: String,
    val channel: String = "",
    val type: String = "other",
    val teamA: String,
    val teamB: String,
    val logoA: String? = null,
    val logoB: String? = null,
    val scoreA: String = "",
    val scoreB: String = "",
    val state: String = "",
    val period: String = "",
    val url: String = "",
    val keyStat: String = ""
) {
    val isLive: Boolean
        get() = state == "1" || state.contains("进行") || state.contains("直播") ||
            period.contains("节") || period.contains("半") || period.contains("分")
    val isFinished: Boolean
        get() = state == "3" || state.contains("完") || state.equals("finished", ignoreCase = true) || period.contains("完")
    val hasScore: Boolean get() = scoreA.isNotBlank() && scoreB.isNotBlank()
    val scoreAInt: Int? get() = scoreA.trim().toIntOrNull()
    val scoreBInt: Int? get() = scoreB.trim().toIntOrNull()
    val teamAWon: Boolean get() = hasScore && (scoreAInt ?: -1) > (scoreBInt ?: -1)
    val teamBWon: Boolean get() = hasScore && (scoreBInt ?: -1) > (scoreAInt ?: -1)
}

data class Zb8StarStat(
    val name: String,
    val value: String
)

data class Zb8LiveText(
    val sid: String,
    val anchor: String,
    val text: String
)

data class Zb8MatchLiveDetail(
    val saishiId: String,
    val date: String,
    val periodCn: String,
    val state: String,
    val scoreA: String,
    val scoreB: String,
    val quarterScoresA: List<String>,
    val quarterScoresB: List<String>,
    val starStatsA: List<Zb8StarStat>,
    val starStatsB: List<Zb8StarStat>,
    val liveTexts: List<Zb8LiveText>,
    val maxSid: Int
)

enum class LeagueType {
    FOOTBALL, BASKETBALL, OTHER
}

data class Zb8League(
    val id: String,
    val name: String,
    val category: String,
    val type: LeagueType,
    val url: String,
    val scorersUrl: String = "",
    val assistsUrl: String = "",
    val reboundsUrl: String = "",
    val scheduleUrl: String = ""
)

data class Zb8Standing(
    val rank: Int,
    val teamName: String,
    val teamLogo: String? = null,
    val played: String = "",
    val won: String = "",
    val drawn: String = "",
    val lost: String = "",
    val goalsDiff: String = "",
    val points: String = "",
    val tag: String = "",
    val winRate: String = "",
    val streak: String = "",
    val gamesBehind: String = "",
    val groupTitle: String = "",
    val teamId: String = ""
)

data class Zb8Player(
    val rank: Int,
    val playerName: String,
    val playerLogo: String? = null,
    val teamName: String = "",
    val teamLogo: String? = null,
    val count: String = "",
    val subCount: String = "",
    val playerId: String = "",
    val teamId: String = ""
)

data class FollowedZb8Team(
    val name: String,
    val sport: String, // "basketball" | "football"
    val logo: String? = null
)

data class SavedZb8Article(
    val url: String,
    val title: String,
    val tag: String = "",
    val savedAt: Long = System.currentTimeMillis()
)

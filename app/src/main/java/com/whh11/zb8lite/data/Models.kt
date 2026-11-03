package com.whh11.zb8lite.data

data class Zb8Item(
    val title: String,
    val url: String,
    val thumb: String?,
    val tag: String,
    val time: String = "",
    val comments: Int = 0
)

/** 直播吧赛事条目：支持真实比分、焦点球员数据、进行中/完赛状态 */
data class Zb8Match(
    val id: String,
    val time: String,
    val date: String = "",
    val competition: String,
    val channel: String = "",
    val teamA: String,
    val teamB: String,
    val logoA: String? = null,
    val logoB: String? = null,
    val scoreA: String = "",
    val scoreB: String = "",
    val state: String = "",
    val url: String = "",
    val keyStat: String = ""
) {
    val isLive: Boolean get() = state.contains("进行中") || state.contains("直播中") || state.equals("live", ignoreCase = true)
    val isFinished: Boolean get() = state.contains("完赛") || state.equals("finished", ignoreCase = true)
    val hasScore: Boolean get() = scoreA.isNotBlank() && scoreB.isNotBlank()
    val hasState: Boolean get() = state.isNotBlank()
}

/** 联赛积分榜/排名条目 */
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
    val groupTitle: String = ""
)

enum class LeagueType {
    FOOTBALL, BASKETBALL
}

data class Zb8League(
    val id: String,
    val name: String,
    val type: LeagueType,
    val url: String
)

data class Zb8Channel(
    val url: String,
    val label: String,
    val highlightsOnly: Boolean = false
)

/** 社区圈子/专区定义 */
data class Zb8Topic(
    val id: String,
    val name: String,
    val category: String,
    val tag: String,
    val description: String = ""
)

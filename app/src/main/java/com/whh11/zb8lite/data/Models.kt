package com.whh11.zb8lite.data

data class Zb8Item(
    val title: String,
    val url: String,
    val thumb: String?,
    val tag: String,
    val time: String = "",
    val comments: Int = 0
)

/** 直播吧赛事条目：支持多项目（足球/篮球/电竞/体坛综合）、真实比分、焦点球员数据、进行中/完赛状态 */
data class Zb8Match(
    val id: String,
    val time: String,
    val date: String = "",
    val competition: String,
    val channel: String = "",
    val type: String = "other", // football, basketball, game, other
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
    val groupTitle: String = "",
    /** 球队 id（球迷宝体系），用于球队详情页 */
    val teamId: String = ""
)

/** 球员榜条目（射手榜 / 助攻榜） */
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

enum class LeagueType {
    FOOTBALL, BASKETBALL, GAME, OTHER
}

data class Zb8League(
    val id: String,
    val name: String,
    val category: String,
    val type: LeagueType,
    val url: String,
    val description: String = "",
    /** 射手榜地址，为空表示该赛事没有球员榜 */
    val scorersUrl: String = ""
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

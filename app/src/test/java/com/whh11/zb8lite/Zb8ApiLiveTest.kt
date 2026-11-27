package com.whh11.zb8lite

import com.whh11.zb8lite.data.Zb8Api
import com.whh11.zb8lite.data.Zb8Match
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Zb8ApiLiveTest {

    @Test
    fun numericScoreComparisonHandlesMultiDigitBasketballScores() {
        val m = Zb8Match(
            id = "1", time = "09:00", competition = "NBA",
            teamA = "湖人", teamB = "勇士",
            scoreA = "112", scoreB = "98", state = "3"
        )
        assertTrue(m.teamAWon)
        assertFalse(m.teamBWon)
    }

    @Test
    fun liveMatchesFusionReturnsRealTodaySchedule() = runBlocking {
        val list = Zb8Api.matches()
        assertTrue("Expected real matches from bifen4m/record, got ${list.size}", list.isNotEmpty())
        assertTrue(list.all { it.teamA.isNotBlank() && it.teamB.isNotBlank() })
    }

    @Test
    fun liveNewsArticleAndCommentsParseCorrectly() = runBlocking {
        val news = Zb8Api.news(Zb8Api.CHANNELS[0])
        assertTrue("Expected non-empty headlines", news.isNotEmpty())
        val firstArticle = news.first { !it.isHighlight }
        val detail = Zb8Api.articleDetail(firstArticle.url)
        assertTrue("Article title should not be blank", detail.title.isNotBlank())
        assertTrue("Article should have content blocks", detail.blocks.isNotEmpty())
        if (detail.filename.isNotBlank()) {
            val page = Zb8Api.comments(detail.filename, 0)
            assertTrue("Comment total should be >= 0", page.total >= 0)
        }
    }

    @Test
    fun liveStandingsAndAssistsParseForNbaAndEpl() = runBlocking {
        val nba = Zb8Api.standings(Zb8Api.LEAGUES.first { it.id == "nba" })
        assertTrue("Expected NBA standings", nba.size >= 20)

        val epl = Zb8Api.LEAGUES.first { it.id == "epl" }
        val eplStandings = Zb8Api.standings(epl)
        assertTrue("Expected EPL standings", eplStandings.size >= 18)

        val eplAssists = Zb8Api.players(epl.assistsUrl)
        assertTrue("Expected EPL assists ranking", eplAssists.isNotEmpty())

        val nbaPts = Zb8Api.players(Zb8Api.LEAGUES.first { it.id == "nba" }.scorersUrl)
        assertTrue("Expected NBA scoring leaders", nbaPts.isNotEmpty())
    }

    @Test
    fun liveSeasonScheduleAndArchiveNewsForTeamAndPlayer() = runBlocking {
        val epl = Zb8Api.LEAGUES.first { it.id == "epl" }
        val arsenalMatches = Zb8Api.teamSeasonMatches("阿森纳", league = epl, sport = "football")
        assertTrue("Expected Arsenal full-season schedule", arsenalMatches.size >= 10)

        val lakersNews = Zb8Api.searchArchiveNews("湖人", sport = "basketball")
        assertTrue("Expected archive news for 湖人", lakersNews.isNotEmpty())
    }
}

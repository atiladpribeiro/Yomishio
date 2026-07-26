package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AutomaticTrackingStatusTest {
    @Test
    fun `all trackers define distinct planned and reading statuses`() {
        val statusPairs = listOf(
            MyAnimeList.PLAN_TO_READ to MyAnimeList.READING,
            Anilist.PLANNING to Anilist.READING,
            Kitsu.PLAN_TO_READ to Kitsu.READING,
            Shikimori.PLANNING to Shikimori.READING,
            Bangumi.PLANNING to Bangumi.READING
        )

        statusPairs.forEach { (planned, reading) ->
            assertThat(planned).isNotEqualTo(reading)
        }
    }

    @Test
    fun `new bindings default to each service planned status`() {
        assertThat(MyAnimeList.DEFAULT_STATUS).isEqualTo(MyAnimeList.PLAN_TO_READ)
        assertThat(Anilist.DEFAULT_STATUS).isEqualTo(Anilist.PLANNING)
        assertThat(Kitsu.DEFAULT_STATUS).isEqualTo(Kitsu.PLAN_TO_READ)
        assertThat(Shikimori.DEFAULT_STATUS).isEqualTo(Shikimori.PLANNING)
        assertThat(Bangumi.DEFAULT_STATUS).isEqualTo(Bangumi.PLANNING)
    }

    @Test
    fun `starting first chapter changes a planned title to service reading status`() {
        val track = TrackImpl().apply {
            status = Anilist.PLANNING
            last_chapter_read = 0
        }

        val changed = applyChapterReadTransition(
            track,
            chapterRead = 1,
            readingStatus = Anilist.READING,
            planToReadStatus = Anilist.PLANNING,
            rereadingStatus = Anilist.REPEATING
        )

        assertThat(changed).isTrue()
        assertThat(track.status).isEqualTo(Anilist.READING)
        assertThat(track.last_chapter_read).isEqualTo(1)
    }

    @Test
    fun `first chapter uses reading status even when remote label was not planned`() {
        val track = TrackImpl().apply {
            status = Anilist.PAUSED
            last_chapter_read = 0
        }

        applyChapterReadTransition(
            track,
            chapterRead = 3,
            readingStatus = Anilist.READING,
            planToReadStatus = Anilist.PLANNING,
            rereadingStatus = Anilist.REPEATING
        )

        assertThat(track.status).isEqualTo(Anilist.READING)
        assertThat(track.last_chapter_read).isEqualTo(3)
    }

    @Test
    fun `rereading status is preserved while progress advances`() {
        val track = TrackImpl().apply {
            status = Shikimori.REPEATING
            last_chapter_read = 0
        }

        applyChapterReadTransition(
            track,
            chapterRead = 2,
            readingStatus = Shikimori.READING,
            planToReadStatus = Shikimori.PLANNING,
            rereadingStatus = Shikimori.REPEATING
        )

        assertThat(track.status).isEqualTo(Shikimori.REPEATING)
        assertThat(track.last_chapter_read).isEqualTo(2)
    }
}

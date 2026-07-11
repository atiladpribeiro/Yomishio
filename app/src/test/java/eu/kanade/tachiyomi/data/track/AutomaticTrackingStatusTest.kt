package eu.kanade.tachiyomi.data.track

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
}

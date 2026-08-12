package eu.kanade.tachiyomi.data.track.kitsu

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KitsuApiTest {
    @Test
    fun `new Kitsu tracking defaults to planned`() {
        assertEquals(Kitsu.PLAN_TO_READ, Kitsu.DEFAULT_STATUS)
    }

    @Test
    fun `created library entry IDs larger than Int are preserved`() {
        val response = Json.parseToJsonElement("""{"data":{"id":"4294967296"}}""").jsonObject

        assertEquals(4_294_967_296L, parseKitsuLibraryEntryId(response))
    }

    @Test
    fun `missing library entry ID returns a useful error`() {
        val response = Json.parseToJsonElement("""{"data":{}}""").jsonObject

        val error = runCatching { parseKitsuLibraryEntryId(response) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Kitsu did not return the created library entry ID", error?.message)
    }
}

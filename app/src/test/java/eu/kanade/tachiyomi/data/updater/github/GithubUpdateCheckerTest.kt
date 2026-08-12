package eu.kanade.tachiyomi.data.updater.github

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubUpdateCheckerTest {
    @Test
    fun `older release is not offered as an update`() {
        assertFalse(isNewerVersion(latest = "1.3.1", current = "1.4.0"))
    }

    @Test
    fun `same release is not offered as an update`() {
        assertFalse(isNewerVersion(latest = "1.4.0", current = "1.4.0"))
    }

    @Test
    fun `newer Yomishio release is offered as an update`() {
        assertTrue(isNewerVersion(latest = "1.4.1", current = "1.4.0"))
    }

    @Test
    fun `prefixed release tags are supported`() {
        assertTrue(isNewerVersion(latest = "v2.0.0", current = "1.9.9"))
    }
}

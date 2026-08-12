package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.UpdateChecker
import eu.kanade.tachiyomi.data.updater.UpdateResult

class GithubUpdateChecker : UpdateChecker() {
    private val service: GithubService = GithubService.create()

    override suspend fun checkForUpdate(): UpdateResult {
        val release = service.getLatestVersion()

        val newVersion = release.version
        return if (isNewerVersion(newVersion, BuildConfig.VERSION_NAME)) {
            GithubUpdateResult.NewUpdate(release)
        } else {
            GithubUpdateResult.NoNewUpdate()
        }
    }
}

internal fun isNewerVersion(
    latest: String,
    current: String
): Boolean {
    fun parse(version: String): List<Int>? {
        val parts = version.removePrefix("v").substringBefore('-').split('.')
        return parts.map { it.toIntOrNull() ?: return null }
    }

    val latestParts = parse(latest) ?: return false
    val currentParts = parse(current) ?: return false
    val componentCount = maxOf(latestParts.size, currentParts.size)

    repeat(componentCount) { index ->
        val latestPart = latestParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (latestPart != currentPart) return latestPart > currentPart
    }
    return false
}

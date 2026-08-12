package eu.kanade.tachiyomi.data.updater

import eu.kanade.tachiyomi.data.updater.github.GithubUpdateChecker

abstract class UpdateChecker {
    companion object {
        fun getUpdateChecker(): UpdateChecker {
            // Fork builds must only check Yomishio releases. The inherited development checker
            // points at Tachiyomi infrastructure and can advertise an incompatible APK.
            return GithubUpdateChecker()
        }
    }

    /**
     * Returns observable containing release information
     */
    abstract suspend fun checkForUpdate(): UpdateResult
}

package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import rx.Completable
import rx.Observable
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Int) {
    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    abstract val name: String

    // Application and remote support for reading dates
    open val supportsReadingDates: Boolean = false

    @DrawableRes
    abstract fun getLogo(): Int

    abstract fun getLogoColor(): Int

    abstract fun getStatusList(): List<Int>

    abstract fun getStatus(status: Int): String

    abstract fun getCompletionStatus(): Int

    /** Statuses used for automatic tracking transitions. */
    abstract fun getReadingStatus(): Int

    abstract fun getPlanToReadStatus(): Int

    /** Status that must be preserved while the user is rereading a title. */
    open fun getRereadingStatus(): Int = -1

    /**
     * Applies the same automatic transition used by Mihon when a chapter is read.
     *
     * A newly started title moves to this service's native Reading/Current status even if the
     * remote service uses a different label. Explicit rereading states are never overwritten.
     */
    fun applyChapterRead(track: Track, chapterRead: Int): Boolean {
        return applyChapterReadTransition(
            track = track,
            chapterRead = chapterRead,
            readingStatus = getReadingStatus(),
            planToReadStatus = getPlanToReadStatus(),
            rereadingStatus = getRereadingStatus()
        )
    }

    abstract fun getScoreList(): List<String>

    open fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    abstract fun displayScore(track: Track): String

    abstract fun add(track: Track): Observable<Track>

    abstract fun update(track: Track): Observable<Track>

    abstract fun bind(track: Track): Observable<Track>

    abstract fun search(query: String): Observable<List<TrackSearch>>

    abstract fun refresh(track: Track): Observable<Track>

    abstract fun login(
        username: String,
        password: String
    ): Completable

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    open val isLogged: Boolean
        get() =
            getUsername().isNotEmpty() &&
                getPassword().isNotEmpty()

    fun getUsername() = preferences.trackUsername(this)!!

    fun getPassword() = preferences.trackPassword(this)!!

    fun saveCredentials(
        username: String,
        password: String
    ) {
        preferences.setTrackCredentials(this, username, password)
    }
}

internal fun applyChapterReadTransition(
    track: Track,
    chapterRead: Int,
    readingStatus: Int,
    planToReadStatus: Int,
    rereadingStatus: Int
): Boolean {
    val shouldAdvanceChapter = chapterRead > track.last_chapter_read
    val startedFromBeginning = track.last_chapter_read == 0 && shouldAdvanceChapter
    val shouldStartReading = track.status == planToReadStatus ||
        (startedFromBeginning && track.status != rereadingStatus)

    if (shouldAdvanceChapter) {
        track.last_chapter_read = chapterRead
    }
    if (shouldStartReading && track.status != readingStatus) {
        track.status = readingStatus
    }

    return shouldAdvanceChapter || shouldStartReading
}

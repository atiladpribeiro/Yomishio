package eu.kanade.tachiyomi.data.track.job

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class DelayedTrackingUpdateJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = Injekt.get<DatabaseHelper>()
        val trackManager = Injekt.get<TrackManager>()
        val delayedTrackingStore = Injekt.get<DelayedTrackingStore>()

        val hasFailures = withContext(Dispatchers.IO) {
            var failed = false
            delayedTrackingStore.getItems().forEach { item ->
                val manga = db.getManga(item.mangaId).executeAsBlocking()
                val track = manga?.let { db.getTracks(it).executeAsBlocking() }
                    ?.find { it.id == item.trackId }
                if (track == null) {
                    // The manga or binding was deleted after being queued; discard only that orphan.
                    delayedTrackingStore.removeItem(item.trackId)
                    return@forEach
                }
                track.last_chapter_read = item.lastChapterRead.toInt()

                try {
                    val service = trackManager.getService(track.sync_id)
                    if (service != null && service.isLogged) {
                        service.update(track).toBlocking().first()
                        db.insertTrack(track).executeAsBlocking()
                        track.id?.let(delayedTrackingStore::removeItem)
                    } else {
                        failed = true
                    }
                } catch (e: Exception) {
                    failed = true
                    Timber.e(e)
                }
            }
            failed
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "DelayedTrackingUpdate"

        fun setupTask(context: Context) {
            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request =
                OneTimeWorkRequestBuilder<DelayedTrackingUpdateJob>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

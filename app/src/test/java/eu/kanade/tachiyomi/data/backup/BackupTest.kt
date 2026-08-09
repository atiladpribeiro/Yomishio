package eu.kanade.tachiyomi.data.backup

import android.app.Application
import android.content.Context
import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.CustomRobolectricGradleTestRunner
import eu.kanade.tachiyomi.data.backup.legacy.LegacyBackupManager
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup
import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.eh.EHentaiThrottleManager
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rx.Observable
import rx.observers.TestSubscriber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton

/**
 * Test class for the [LegacyBackupManager].
 * Note that this does not include the backup create/restore services.
 */
@Config(constants = BuildConfig::class, sdk = [Build.VERSION_CODES.LOLLIPOP])
@RunWith(CustomRobolectricGradleTestRunner::class)
class BackupTest {
    // Create manga array
    var mangaEntries = mutableListOf<JsonElement>()

    // Create category array
    var categoryEntries = mutableListOf<JsonElement>()

    lateinit var app: Application
    lateinit var context: Context
    lateinit var source: HttpSource

    lateinit var legacyBackupManager: LegacyBackupManager

    lateinit var db: DatabaseHelper

    @Before
    fun setup() {
        app = RuntimeEnvironment.application
        context = app.applicationContext
        legacyBackupManager = LegacyBackupManager(context)
        db = legacyBackupManager.databaseHelper

        // Mock the source manager
        val module =
            object : InjektModule {
                override fun InjektRegistrar.registerInjectables() {
                    addSingleton(mock(SourceManager::class.java, RETURNS_DEEP_STUBS))
                }
            }
        Injekt.importModule(module)

        source = mock(HttpSource::class.java)
        `when`(legacyBackupManager.sourceManager.get(anyLong())).thenReturn(source)
    }

    /**
     * Test that checks if no crashes when no categories in library.
     */
    @Test
    fun testRestoreEmptyCategory() {
        // Initialize json with version 2
        initializeJsonTest(2)

        // Create backup of empty database
        legacyBackupManager.backupCategories(categoryEntries)

        // Restore Json
        legacyBackupManager.restoreCategories(JsonArray(categoryEntries))

        // Check if empty
        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).isEmpty()
    }

    /**
     * Test to check if single category gets restored
     */
    @Test
    fun testRestoreSingleCategory() {
        // Initialize json with version 2
        initializeJsonTest(2)

        // Create category and add to json
        val category = addSingleCategory("category")

        // Restore Json
        legacyBackupManager.restoreCategories(JsonArray(categoryEntries))

        // Check if successful
        val dbCats = legacyBackupManager.databaseHelper.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(1)
        assertThat(dbCats[0].name).isEqualTo(category.name)
    }

    /**
     * Test to check if multiple categories get restored.
     */
    @Test
    fun testRestoreMultipleCategories() {
        // Initialize json with version 2
        initializeJsonTest(2)

        // Create category and add to json
        val category = addSingleCategory("category")
        val category2 = addSingleCategory("category2")
        val category3 = addSingleCategory("category3")
        val category4 = addSingleCategory("category4")
        val category5 = addSingleCategory("category5")

        // Insert category to test if no duplicates on restore.
        db.insertCategory(category).executeAsBlocking()

        // Restore Json
        legacyBackupManager.restoreCategories(JsonArray(categoryEntries))

        // Check if successful
        val dbCats = legacyBackupManager.databaseHelper.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(5)
        assertThat(dbCats[0].name).isEqualTo(category.name)
        assertThat(dbCats[1].name).isEqualTo(category2.name)
        assertThat(dbCats[2].name).isEqualTo(category3.name)
        assertThat(dbCats[3].name).isEqualTo(category4.name)
        assertThat(dbCats[4].name).isEqualTo(category5.name)
    }

    /**
     * Test if restore of manga is successful
     */
    @Test
    fun testRestoreManga() {
        // Initialize json with version 2
        initializeJsonTest(2)

        // Add manga to database
        val manga = getSingleManga("One Piece")
        manga.viewer = 3
        manga.id = db.insertManga(manga).executeAsBlocking().insertedId()

        var favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(1)
        assertThat(favoriteManga[0].viewer).isEqualTo(3)

        // Update json with all options enabled
        mangaEntries.add(legacyBackupManager.backupMangaObject(manga, 1))

        // Change manga in database to default values
        val dbManga = getSingleManga("One Piece")
        dbManga.id = manga.id
        db.insertManga(dbManga).executeAsBlocking()

        favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(1)
        assertThat(favoriteManga[0].viewer).isEqualTo(0)

        // Restore local manga
        legacyBackupManager.restoreMangaNoFetch(manga, dbManga)

        // Test if restore successful
        favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(1)
        assertThat(favoriteManga[0].viewer).isEqualTo(3)

        // Clear database to test manga fetch
        clearDatabase()

        // Test if successful
        favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(0)

        // Restore Json
        // Round-trip the manga through the legacy backup representation.
        val json = legacyBackupManager.backupMangaObject(manga, 0).jsonObject[Backup.MANGA]!!
        val jsonManga = legacyBackupManager.jsonToManga(json)

        // Restore manga with fetch observable
        val networkManga = getSingleManga("One Piece")
        networkManga.description = "This is a description"
        `when`(source.fetchMangaDetails(jsonManga)).thenReturn(Observable.just(networkManga))

        val obs = legacyBackupManager.restoreMangaFetchObservable(source, jsonManga)
        val testSubscriber = TestSubscriber<Manga>()
        obs.subscribe(testSubscriber)

        testSubscriber.assertNoErrors()

        // Check if restore successful
        val dbCats = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(dbCats).hasSize(1)
        assertThat(dbCats[0].viewer).isEqualTo(3)
        assertThat(dbCats[0].description).isEqualTo("This is a description")
    }

    /**
     * Test if chapter restore is successful
     */
    @Test
    fun testRestoreChapters() {
        // Initialize json with version 2
        initializeJsonTest(2)

        // Insert manga
        val manga = getSingleManga("One Piece")
        manga.id = legacyBackupManager.databaseHelper.insertManga(manga).executeAsBlocking().insertedId()

        // Create restore list
        val chapters = mutableListOf<Chapter>()
        for (i in 1..8) {
            val chapter = getSingleChapter("Chapter $i")
            chapter.read = true
            chapters.add(chapter)
        }

        // Round-trip the chapters through the legacy backup representation.
        val restoredChapters =
            chapters.map {
                legacyBackupManager.jsonToChapter(
                    buildJsonObject {
                        put("u", it.url)
                        put("r", if (it.read) 1 else 0)
                        put("b", if (it.bookmark) 1 else 0)
                        put("l", it.last_page_read)
                    }
                )
            }

        // Fetch chapters from upstream
        // Create list
        val chaptersRemote = mutableListOf<Chapter>()
        (1..10).mapTo(chaptersRemote) { getSingleChapter("Chapter $it") }
        `when`(source.fetchChapterList(manga)).thenReturn(Observable.just(chaptersRemote))

        // Call restoreChapterFetchObservable
        val obs = legacyBackupManager.restoreChapterFetchObservable(source, manga, restoredChapters, EHentaiThrottleManager())
        val testSubscriber = TestSubscriber<Pair<List<Chapter>, List<Chapter>>>()
        obs.subscribe(testSubscriber)

        testSubscriber.assertNoErrors()

        val dbCats = legacyBackupManager.databaseHelper.getChapters(manga).executeAsBlocking()
        assertThat(dbCats).hasSize(10)
        assertThat(dbCats[0].read).isEqualTo(true)
    }

    /**
     * Test to check if history restore works
     */
    @Test
    fun restoreHistoryForManga() {
        // Initialize json with version 2
        initializeJsonTest(2)

        val manga = getSingleManga("One Piece")
        manga.id = legacyBackupManager.databaseHelper.insertManga(manga).executeAsBlocking().insertedId()

        // Create chapter
        val chapter = getSingleChapter("Chapter 1")
        chapter.manga_id = manga.id
        chapter.read = true
        chapter.id = legacyBackupManager.databaseHelper.insertChapter(chapter).executeAsBlocking().insertedId()

        val historyJson = getSingleHistory(chapter)

        val historyList = mutableListOf<DHistory>()
        historyList.add(historyJson)

        val history =
            historyList.map {
                legacyBackupManager.jsonToHistory(
                    buildJsonArray {
                        add(it.url)
                        add(it.lastRead)
                    }
                )
            }

        // Restore categories
        legacyBackupManager.restoreHistoryForManga(history)

        val historyDB = legacyBackupManager.databaseHelper.getHistoryByMangaId(manga.id!!).executeAsBlocking()
        assertThat(historyDB).hasSize(1)
        assertThat(historyDB[0].last_read).isEqualTo(1000)
    }

    /**
     * Test to check if tracking restore works
     */
    @Test
    fun restoreTrackForManga() {
        // Initialize json with version 2
        initializeJsonTest(2)

        // Create mangas
        val manga = getSingleManga("One Piece")
        val manga2 = getSingleManga("Bleach")
        manga.id = legacyBackupManager.databaseHelper.insertManga(manga).executeAsBlocking().insertedId()
        manga2.id = legacyBackupManager.databaseHelper.insertManga(manga2).executeAsBlocking().insertedId()

        // Create track and add it to database
        // This tests duplicate errors.
        val track = getSingleTrack(manga)
        track.last_chapter_read = 5
        legacyBackupManager.databaseHelper.insertTrack(track).executeAsBlocking()
        var trackDB = legacyBackupManager.databaseHelper.getTracks(manga).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(5)
        track.last_chapter_read = 7

        // Create track for different manga to test track not in database
        val track2 = getSingleTrack(manga2)
        track2.last_chapter_read = 10

        // Round-trip and restore a tracking entry already in the database.
        var trackList = listOf(track)
        var trackListRestore = trackList.map(::roundTripTrack)
        legacyBackupManager.restoreTrackForManga(manga, trackListRestore)

        // Assert if restore works.
        trackDB = legacyBackupManager.databaseHelper.getTracks(manga).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(7)

        // Check parser and restore already in database with lower chapter_read
        track.last_chapter_read = 5
        trackList = listOf(track)
        legacyBackupManager.restoreTrackForManga(manga, trackList)

        // Assert if restore works.
        trackDB = legacyBackupManager.databaseHelper.getTracks(manga).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(7)

        // Check parser and restore, track not in database
        trackList = listOf(track2)

        trackListRestore = trackList.map(::roundTripTrack)
        legacyBackupManager.restoreTrackForManga(manga2, trackListRestore)

        // Assert if restore works.
        trackDB = legacyBackupManager.databaseHelper.getTracks(manga2).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(10)
    }

    fun clearJson() {
        mangaEntries = mutableListOf()
        categoryEntries = mutableListOf()
    }

    fun initializeJsonTest(version: Int) {
        clearJson()
        legacyBackupManager.setVersion(version)
    }

    fun addSingleCategory(name: String): Category {
        val category = Category.create(name)
        categoryEntries.add(
            buildJsonArray {
                add(category.name)
                add(category.order)
            }
        )
        return category
    }

    private fun roundTripTrack(track: TrackImpl): TrackImpl {
        return legacyBackupManager.jsonToTrack(
            buildJsonObject {
                put("t", track.title)
                put("s", track.sync_id)
                put("r", track.media_id)
                put("ml", track.library_id)
                put("l", track.last_chapter_read)
                put("u", track.tracking_url)
            }
        )
    }

    fun clearDatabase() {
        db.deleteMangas().executeAsBlocking()
        db.deleteHistory().executeAsBlocking()
    }

    fun getSingleHistory(chapter: Chapter): DHistory {
        return DHistory(chapter.url, 1000)
    }

    private fun getSingleTrack(manga: Manga): TrackImpl {
        val track = TrackImpl()
        track.title = manga.title
        track.manga_id = manga.id!!
        track.sync_id = 1
        return track
    }

    private fun getSingleManga(title: String): MangaImpl {
        val manga = MangaImpl()
        manga.source = 1
        manga.title = title
        manga.url = "/manga/$title"
        manga.favorite = true
        return manga
    }

    private fun getSingleChapter(name: String): ChapterImpl {
        val chapter = ChapterImpl()
        chapter.name = name
        chapter.url = "/read-online/$name-page-1.html"
        return chapter
    }
}

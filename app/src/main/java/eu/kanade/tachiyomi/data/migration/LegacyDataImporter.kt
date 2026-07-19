package eu.kanade.tachiyomi.data.migration

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Xml
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.io.File

/** Imports a one-time TachiyomiAZ migration bundle before any database or service is opened. */
object LegacyDataImporter {
    private const val DEFAULT_PREFERENCES = "default_preferences.xml"
    private const val DATABASE_NAME = "tachiyomi.db"
    private const val IMPORT_MARKER = "legacy_data_imported"

    fun importIfPresent(context: Context) {
        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // Avoid touching emulated storage on every cold start after a successful import.
        // FUSE metadata calls are noticeably slow on e-ink devices such as the Bigme B7.
        if (defaultPreferences.getBoolean(IMPORT_MARKER, false)) return

        val migrationDir = File(
            Environment.getExternalStorageDirectory(),
            "Yomishio/.migration/legacy"
        )
        if (!migrationDir.isDirectory) return

        try {
            importDatabase(context, File(migrationDir, DATABASE_NAME))
            importPreferences(context, File(migrationDir, "shared_prefs"))

            defaultPreferences.edit()
                .putBoolean(IMPORT_MARKER, true)
                .commit()

            check(migrationDir.deleteRecursively()) { "Could not remove legacy migration bundle" }
            migrationDir.parentFile?.delete()
            Timber.i("Imported TachiyomiAZ data into Yomishio")
        } catch (error: Exception) {
            Timber.e(error, "Failed to import TachiyomiAZ data")
        }
    }

    private fun importDatabase(context: Context, source: File) {
        if (!source.isFile) return

        val destination = context.getDatabasePath(DATABASE_NAME)
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "$DATABASE_NAME.importing")
        source.copyTo(temporary, overwrite = true)

        File(destination.path + "-wal").delete()
        File(destination.path + "-shm").delete()
        File(destination.path + "-journal").delete()
        check(!destination.exists() || destination.delete()) { "Could not replace Yomishio database" }
        check(temporary.renameTo(destination)) { "Could not activate imported database" }
    }

    private fun importPreferences(context: Context, directory: File) {
        if (!directory.isDirectory) return

        directory.listFiles { file -> file.isFile && file.extension == "xml" }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val preferences = if (file.name == DEFAULT_PREFERENCES) {
                    PreferenceManager.getDefaultSharedPreferences(context)
                } else {
                    context.getSharedPreferences(file.nameWithoutExtension, Context.MODE_PRIVATE)
                }
                importPreferenceFile(file, preferences)
            }

        val root = File(Environment.getExternalStorageDirectory(), "Yomishio")
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("download_directory", File(root, "downloads").toURI().toString())
            .putString("backup_directory", File(root, "backup").toURI().toString())
            .commit()
    }

    private fun importPreferenceFile(source: File, preferences: SharedPreferences) {
        val editor = preferences.edit()
        source.inputStream().buffered().use { input ->
            val parser = Xml.newPullParser().apply { setInput(input, Charsets.UTF_8.name()) }
            while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != org.xmlpull.v1.XmlPullParser.START_TAG) continue
                val key = parser.getAttributeValue(null, "name") ?: continue
                when (parser.name) {
                    "string" -> editor.putString(key, parser.nextText())
                    "boolean" -> editor.putBoolean(key, parser.getAttributeValue(null, "value").toBoolean())
                    "int" -> editor.putInt(key, parser.getAttributeValue(null, "value").toInt())
                    "long" -> editor.putLong(key, parser.getAttributeValue(null, "value").toLong())
                    "float" -> editor.putFloat(key, parser.getAttributeValue(null, "value").toFloat())
                    "set" -> editor.putStringSet(key, readStringSet(parser))
                }
            }
        }
        check(editor.commit()) { "Could not import ${source.name}" }
    }

    private fun readStringSet(parser: org.xmlpull.v1.XmlPullParser): Set<String> {
        val values = mutableSetOf<String>()
        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.END_TAG && parser.name == "set") break
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "string") {
                values += parser.nextText()
            }
        }
        return values
    }
}

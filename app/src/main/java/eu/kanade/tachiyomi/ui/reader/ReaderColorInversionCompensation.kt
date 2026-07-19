package eu.kanade.tachiyomi.ui.reader

import android.content.SharedPreferences
import android.database.ContentObserver
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import java.util.Collections
import java.util.WeakHashMap

/**
 * Cancels the system color inversion only for manga page views.
 *
 * The system then inverts the rest of the reader UI as requested by the user, while the second
 * inversion applied here restores the original colors of page artwork.
 */
class ReaderColorInversionCompensation(
    private val activity: ReaderActivity,
    private val preferences: PreferencesHelper
) : ContentObserver(Handler(Looper.getMainLooper())), SharedPreferences.OnSharedPreferenceChangeListener {
    private val pageViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

    private var compensationEnabled = false

    init {
        activity.contentResolver.registerContentObserver(INVERSION_SETTING_URI, false, this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        refresh()
    }

    fun registerPageView(view: View) {
        pageViews += view
        applyTo(view, compensationEnabled)
    }

    fun close() {
        activity.contentResolver.unregisterContentObserver(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        pageViews.forEach { applyTo(it, false) }
        pageViews.clear()
    }

    override fun onChange(selfChange: Boolean) {
        refresh()
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        if (key == PreferenceKeys.readerColorInversionCompensation) refresh()
    }

    private fun refresh() {
        val systemInversionEnabled =
            try {
                Settings.Secure.getInt(activity.contentResolver, INVERSION_SETTING, 0) == 1
            } catch (_: SecurityException) {
                false
            }

        val enabled = ReaderColorInversionMode.shouldCompensate(
            preferences.readerColorInversionCompensation().get(),
            systemInversionEnabled
        )
        if (enabled == compensationEnabled) return

        compensationEnabled = enabled
        pageViews.forEach { applyTo(it, enabled) }
    }

    private fun applyTo(
        view: View,
        enabled: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(if (enabled) inversionRenderEffect() else null)
        } else {
            view.setLayerType(
                if (enabled) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_NONE,
                if (enabled) INVERSION_PAINT else null
            )
        }
        view.invalidate()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun inversionRenderEffect(): RenderEffect =
        RenderEffect.createColorFilterEffect(INVERSION_FILTER)

    companion object {
        private const val INVERSION_SETTING = "accessibility_display_inversion_enabled"
        private val INVERSION_SETTING_URI = Settings.Secure.getUriFor(INVERSION_SETTING)

        private val INVERSION_FILTER =
            ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        private val INVERSION_PAINT = Paint().apply { colorFilter = INVERSION_FILTER }
    }
}

object ReaderColorInversionMode {
    const val OFF = 0
    const val AUTOMATIC = 1
    const val ALWAYS = 2

    fun shouldCompensate(
        mode: Int,
        systemInversionEnabled: Boolean
    ): Boolean = mode == ALWAYS || mode == AUTOMATIC && systemInversionEnabled
}

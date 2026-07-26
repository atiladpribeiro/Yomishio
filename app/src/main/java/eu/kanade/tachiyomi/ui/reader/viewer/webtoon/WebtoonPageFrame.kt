package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.graphics.Canvas
import android.widget.FrameLayout

/** Draws one webtoon page through Yomishio's Bigme inversion compensation when needed. */
class WebtoonPageFrame(
    private val viewer: WebtoonViewer
) : FrameLayout(viewer.activity) {
    init {
        viewer.activity.colorInversionCompensation.registerPageView(this)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val checkpoint = viewer.activity.colorInversionCompensation.beginPageDraw(canvas)
        super.dispatchDraw(canvas)
        viewer.activity.colorInversionCompensation.endPageDraw(canvas, checkpoint)
    }
}

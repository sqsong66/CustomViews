package com.sqsong.imagefilter.processor.filters

import android.graphics.Bitmap
import android.graphics.PointF
import com.sqsong.imagefilter.ImageProcessor
import com.sqsong.imagefilter.geometory.curveGenerator
import com.sqsong.imagefilter.processor.SubFilter

class ToneCurveSubFilter(
    override var tag: Any? = null,
    private val rgbKnots: Array<PointF> = arrayOf(PointF(0f, 0f), PointF(255f, 255f)),
    private val redKnots: Array<PointF> = arrayOf(PointF(0f, 0f), PointF(255f, 255f)),
    private val greenKnots: Array<PointF> = arrayOf(PointF(0f, 0f), PointF(255f, 255f)),
    private val blueKnots: Array<PointF> = arrayOf(PointF(0f, 0f), PointF(255f, 255f)),
) : SubFilter {

    private var r: IntArray? = null
    private var g: IntArray? = null
    private var b: IntArray? = null
    private var rgb: IntArray? = null

    override fun process(input: Bitmap): Bitmap {
        val rgbKnots = sortPointsOnXAxis(rgbKnots)
        val redKnots = sortPointsOnXAxis(redKnots)
        val greenKnots = sortPointsOnXAxis(greenKnots)
        val blueKnots = sortPointsOnXAxis(blueKnots)
        if (rgb == null) {
            rgb = curveGenerator(rgbKnots)
        }
        if (r == null) {
            r = curveGenerator(redKnots)
        }
        if (g == null) {
            g = curveGenerator(greenKnots)
        }
        if (b == null) {
            b = curveGenerator(blueKnots)
        }
        return ImageProcessor.applyCurves(rgb, r, g, b, input)
    }

    private fun sortPointsOnXAxis(points: Array<PointF>?): Array<PointF>? {
        if (points == null) {
            return null
        }
        for (s in 1 until points.size - 1) {
            for (k in 0..points.size - 2) {
                if (points[k].x > points[k + 1].x) {
                    val temp: Float = points[k].x
                    points[k].x = points[k + 1].x //swapping values
                    points[k + 1].x = temp
                }
            }
        }
        return points
    }
}
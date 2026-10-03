package com.abinet.gallerylight

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
class ZoomableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    var onScaleChanged: ((Float) -> Unit)? = null

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val start = PointF()
    private var mode = NONE
    private var oldDist = 1f
    private val minScale = 1f
    private val maxScale = 5f

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                var scale = detector.scaleFactor
                val values = FloatArray(9)
                matrix.getValues(values)
                val cur = values[Matrix.MSCALE_X]
                val target = cur * scale
                scale = when {
                    target > maxScale -> maxScale / cur
                    target < minScale -> minScale / cur
                    else -> scale
                }
                matrix.postScale(scale, scale, detector.focusX, detector.focusY)
                imageMatrix = matrix
                notifyScale()
                return true
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val values = FloatArray(9)
                matrix.getValues(values)
                val cur = values[Matrix.MSCALE_X]
                val target = if (cur > 1.5f) 1f else 2.5f
                val s = target / cur
                matrix.postScale(s, s, e.x, e.y)
                imageMatrix = matrix
                notifyScale()
                return true
            }
            override fun onDown(e: MotionEvent): Boolean = true
        })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        parent?.requestDisallowInterceptTouchEvent(currentScale() > 1.05f)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    mode = ZOOM
                }
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG && currentScale() > 1.0f) {
                matrix.set(savedMatrix)
                matrix.postTranslate(event.x - start.x, event.y - start.y)
                imageMatrix = matrix
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                if (abs(event.x - start.x) < 8 && abs(event.y - start.y) < 8) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun resetTransform() {
        matrix.reset()
        imageMatrix = matrix
        notifyScale()
    }

    private fun currentScale(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun notifyScale() {
        onScaleChanged?.invoke(currentScale())
    }

    private fun spacing(ev: MotionEvent): Float {
        val x = ev.getX(0) - ev.getX(1)
        val y = ev.getY(0) - ev.getY(1)
        return sqrt(x * x + y * y)
    }

    private companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
    }
}
package work.ckogyo.returnvisitor.utils

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView

fun View.getViewCapture(): Bitmap {
    this.isDrawingCacheEnabled = true

    // Viewのキャッシュを取得
    val cache = this.drawingCache
    val captured = Bitmap.createBitmap(cache)
    this.isDrawingCacheEnabled = false
    return captured
}

fun View.getRectInScreen(): Rect {

    fun getLeftTop(v: View, leftTopSum: Point) {

        val parentView = v.parent

        if (parentView is ScrollView) {
            leftTopSum.x -= parentView.scrollX
            leftTopSum.y -= parentView.scrollY
        } else {
            leftTopSum.x += v.left
            leftTopSum.y += v.top
        }

        if (parentView is ViewGroup) {
            getLeftTop(parentView, leftTopSum)
        }
    }

    val leftTopSum = Point()
    getLeftTop(this, leftTopSum)

    return Rect(leftTopSum.x, leftTopSum.y, leftTopSum.x + width, leftTopSum.y + height)
}

fun View.setOnClick(onClick: (View) -> Unit) {

    setOnTouchListener { _, e ->

        when(e.action) {
            MotionEvent.ACTION_DOWN -> {
                alpha = 0.3f
            }
            MotionEvent.ACTION_UP -> {
                onClick(this)
                alpha = 1f
            }
            MotionEvent.ACTION_CANCEL -> {
                alpha = 1f
            }
        }
        return@setOnTouchListener true
    }
}
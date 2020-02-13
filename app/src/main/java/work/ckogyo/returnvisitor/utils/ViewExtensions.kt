package work.ckogyo.returnvisitor.utils

import android.animation.Animator
import android.animation.ValueAnimator
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

    val leftTopSum = Point()
    getLeftTop(this, leftTopSum)

    return Rect(leftTopSum.x, leftTopSum.y, leftTopSum.x + width, leftTopSum.y + height)
}


private fun getLeftTop(v: View, leftTopSum: Point, limitAncestor: View? = null) {

    val parentView = v.parent

    if (parentView is ScrollView) {
        leftTopSum.x -= parentView.scrollX
        leftTopSum.y -= parentView.scrollY
    } else {
        leftTopSum.x += v.left
        leftTopSum.y += v.top
    }

    if (limitAncestor != null && parentView == limitAncestor) {
        return
    }

    if (parentView is ViewGroup) {
        getLeftTop(parentView, leftTopSum)
    }
}

fun View.getPositionInAncestor(ancestor: View): Point {

    val leftTopSum = Point()
    getLeftTop(this, leftTopSum, ancestor)
    return leftTopSum
}

fun View.setOnClick(onClick: ((View) -> Unit)?) {

    if (onClick == null) {
        setOnTouchListener(null)
        return
    }

    setOnTouchListener { _, e ->

        when(e.action) {
            MotionEvent.ACTION_DOWN -> {
                alpha = 0.3f
                return@setOnTouchListener true
            }
            MotionEvent.ACTION_UP -> {
                onClick(this)
                alpha = 1f
                return@setOnTouchListener true
            }
            MotionEvent.ACTION_CANCEL -> {
                alpha = 1f
                return@setOnTouchListener true
            }
            else -> {
                return@setOnTouchListener false
            }
        }
    }
}

fun View.fadeVisibility(fadeIn: Boolean,
                        addTouchBlockerOnFadeIn: Boolean = false,
                        onAnimationFinished: ((fadedIn: Boolean) -> Unit)? = null) {

    val target = if (fadeIn) {
        this.visibility = View.VISIBLE
        if (addTouchBlockerOnFadeIn) {
            this.setOnTouchListener { _, _ -> true }
        }
        1f
    } else {
        this.setOnTouchListener(null)
        0f
    }

    val animator = ValueAnimator.ofFloat(this.alpha, target)
    animator.addUpdateListener {
        this.alpha = it.animatedValue as Float
        this.requestLayout()
    }

    animator.duration = 500
    animator.addListener(object : Animator.AnimatorListener{
        override fun onAnimationRepeat(p0: Animator?) {}

        override fun onAnimationEnd(p0: Animator?) {
            if (!fadeIn) {
                this@fadeVisibility.visibility = View.GONE
            }
            onAnimationFinished?.invoke(fadeIn)
        }

        override fun onAnimationCancel(p0: Animator?) {}

        override fun onAnimationStart(p0: Animator?) {}
    })
    animator.start()

}
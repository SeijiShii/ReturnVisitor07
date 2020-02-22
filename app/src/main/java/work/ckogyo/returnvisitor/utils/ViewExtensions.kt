package work.ckogyo.returnvisitor.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
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
        getLeftTop(parentView, leftTopSum, limitAncestor)
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

// すごく手続き型言語っぽくていやだけど。
private data class ViewAndAnimatorCouple(val view: View, val animator: ValueAnimator)
private val viewAndAnimatorList = ArrayList<ViewAndAnimatorCouple>()

private fun getCoupleByView(v: View): ViewAndAnimatorCouple? {
    for(c in viewAndAnimatorList) {
        if (c.view == v) return c
    }
    return null
}


fun View.fadeVisibility(fadeIn: Boolean,
                        addTouchBlockerOnFadeIn: Boolean = false,
                        onAnimationFinished: ((fadedIn: Boolean) -> Unit)? = null) {

    val couple1 = getCoupleByView(this)
    if (couple1 != null) {
        couple1.animator.cancel()
        viewAndAnimatorList.remove(couple1)
    }

    if (fadeIn) {
        if (addTouchBlockerOnFadeIn) {
            this.setOnTouchListener { _, _ -> true }
        }
    } else {
        this.setOnTouchListener(null)
    }

//    if (!withAnimation) {
//        alpha = if (fadeIn) {
//            visibility = View.VISIBLE
//            1f
//        } else {
//            visibility = View.GONE
//            0f
//        }
//        return
//    }

    val target = if (fadeIn) {
        this.visibility = View.VISIBLE
        1f
    } else {

        0f
    }

    if (animation != null) {
        Log.d(debugTag, "animation != null")
    }

    val animator = ValueAnimator.ofFloat(this.alpha, target)
    animator.addUpdateListener {
        this.alpha = it.animatedValue as Float
        this.requestLayout()
    }

    animator.duration = 1000
    animator.addListener(object : Animator.AnimatorListener{
        override fun onAnimationRepeat(p0: Animator?) {}

        override fun onAnimationEnd(p0: Animator?) {
            if (!fadeIn) {
                this@fadeVisibility.visibility = View.GONE
            }
            onAnimationFinished?.invoke(fadeIn)
            val couple3 = getCoupleByView(this@fadeVisibility)
            if (couple3 != null) {
                viewAndAnimatorList.remove(couple3)
            }
        }

        override fun onAnimationCancel(p0: Animator?) {
            val couple3 = getCoupleByView(this@fadeVisibility)
            if (couple3 != null) {
                viewAndAnimatorList.remove(couple3)
            }
        }

        override fun onAnimationStart(p0: Animator?) {}
    })
    animator.start()

    val couple = ViewAndAnimatorCouple(this, animator)
    viewAndAnimatorList.add(couple)
}


package work.ckogyo.returnvisitor.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.abs

abstract class HeightAnimationView(context: Context): FrameLayout(context){

    open fun onUpdateAnimation(animatedHeight: Int){}

    open fun onRefreshHeight(height: Int){}

    abstract val collapseHeight: Int
    abstract val extractHeight: Int
    abstract val cellId: String

    val currentHeight: Int
        get() {
            return if (isExtracted) {
                extractHeight
            } else {
                collapseHeight
            }
        }

    var isExtracted = false

    fun refreshCellHeight(extracted: Boolean?){

        if (extracted != null){
            isExtracted = extracted
        }

        val h = if (isExtracted){
            extractHeight
        } else {
            collapseHeight
        }
        layoutParams = when(layoutParams) {
            is LinearLayout.LayoutParams -> LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, h)
            is LayoutParams -> LayoutParams(LayoutParams.MATCH_PARENT, h)
            else -> ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, h)
        }
        requestLayout()

        onRefreshHeight(h)
    }

    protected fun animateHeight(onHeightAnimationEnd: ((HeightAnimationView) -> Unit)? = null){

        val origin: Int = measuredHeight

        val target = if (isExtracted){
            extractHeight
        } else {
            collapseHeight
        }

        val animator = ValueAnimator.ofInt(origin, target)
        animator.addUpdateListener {
            val animatedHeight = it.animatedValue as Int
            layoutParams.height = animatedHeight
            onUpdateAnimation(animatedHeight)
            requestLayout()
        }

        animator.addListener(object : Animator.AnimatorListener {

            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                onRefreshHeight(target)
                onHeightAnimationEnd?.invoke(this@HeightAnimationView)
            }

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}
        })

        animator.duration = abs(origin - target).toLong() / 10
        animator.start()
    }

}
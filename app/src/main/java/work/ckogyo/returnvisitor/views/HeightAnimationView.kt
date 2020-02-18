package work.ckogyo.returnvisitor.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.abs

abstract class HeightAnimationView: FrameLayout{

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    open fun onUpdateAnimation(animatedHeight: Int){}

    open fun onRefreshHeight(height: Int){}


    var collapseHeight: Int = 0
    var extractHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

//    abstract val cellId: String

    val currentHeight: Int
        get() {
            return if (isExtracted) {
                extractHeight
            } else {
                collapseHeight
            }
        }

    var isExtracted = false

    open fun refreshCellHeight(extracted: Boolean? = null){

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

    fun animateHeight(onHeightAnimationEnd: ((HeightAnimationView) -> Unit)? = null){

        val target = if (isExtracted){
            extractHeight
        } else {
            collapseHeight
        }

        animateHeight(target, onHeightAnimationEnd)
    }

    private fun animateHeight(targetHeight: Int, onHeightAnimationEnd: ((HeightAnimationView) -> Unit)? = null){

        val origin: Int = measuredHeight

        val animator = ValueAnimator.ofInt(origin, targetHeight)
        animator.addUpdateListener {
            val animatedHeight = it.animatedValue as Int
            layoutParams.height = animatedHeight
            onUpdateAnimation(animatedHeight)
            requestLayout()
        }

        animator.addListener(object : Animator.AnimatorListener {

            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                onRefreshHeight(targetHeight)
                onHeightAnimationEnd?.invoke(this@HeightAnimationView)
            }

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}
        })

        animator.duration = abs(origin - targetHeight).toLong() / 10
        animator.start()
    }

    fun collapseToHeight0(onHeightAnimationEnd: ((HeightAnimationView) -> Unit)? = null) {
        animateHeight(0, onHeightAnimationEnd)
    }

}
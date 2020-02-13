package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.utils.debugTag

class TagViewContainer : LinearLayout {

    var onTagViewRemoved: ((TagView) -> Unit)? = null

    private val tagViews = ArrayList<TagView>()
    private val tagWidthList = ArrayList<Int>()

    constructor(context: Context?) : super(context) {initCommon()}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {initCommon()}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {initCommon()}

    private fun initCommon() {
        orientation = VERTICAL
    }

    private fun updateTagViews() {

        for (tagView in tagViews) {
            tagView.removeFromParent()
        }

        removeAllViews()

        val dummyFrame = FrameLayout(context).also {
            it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            it.visibility = View.INVISIBLE
        }
        addView(dummyFrame)

        for (tagView in tagViews) {
            tagView.addToParent(dummyFrame)
        }

        GlobalScope.launch {
            while (dummyFrame.width <= 0) {
                delay(20)
            }

            val containerWidth = width
//            Log.d(debugTag, "width: $width")

            tagWidthList.clear()
            for(tagView in tagViews) {
//                Log.d(debugTag, "tagView.width: ${tagView.width}")
                tagWidthList.add(tagView.viewWidth)
            }

            handler.post {
                dummyFrame.removeAllViews()
                removeView(dummyFrame)

                var widthSum = 0
                var rowFrame = LinearLayout(context)

                for (i in 0 until tagViews.size) {

                    tagViews[i].onRemoved = this@TagViewContainer::removeTagView

                    if (i == 0 || widthSum + tagWidthList[i] > containerWidth) {
                        rowFrame = generateAndAddRowFrame()
                        widthSum = 0
                    }
                    tagViews[i].addToParent(rowFrame)
                    widthSum += tagWidthList[i]
                }
            }
        }

    }

    private fun generateAndAddRowFrame(): LinearLayout {
        return LinearLayout(context).also {
            it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            it.orientation = HORIZONTAL
            addView(it)
        }
    }

    fun <T: TagView>addTagView(tagView: T) {
        tagViews.add(tagView)
        updateTagViews()
    }

    fun <T: TagView>addTagViews(tagViews2: ArrayList<T>, clearBeforeAdd: Boolean = true) {
        if (clearBeforeAdd) {
            tagViews.clear()
            removeAllViews()
        }

        tagViews.addAll(tagViews2)
        updateTagViews()
    }

    private fun removeTagView(tagView: TagView) {
        tagViews.remove(tagView)
        onTagViewRemoved?.invoke(tagView)
        updateTagViews()
    }
}
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

    private val tagViews = ArrayList<View>()
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
            (tagView.parent as? ViewGroup)?.removeView(tagView)
        }

        removeAllViews()

        val dummyFrame = FrameLayout(context).also {
            it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            it.visibility = View.INVISIBLE
        }
        addView(dummyFrame)

        for (tagView in tagViews) {
            dummyFrame.addView(tagView)
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
                tagWidthList.add(tagView.width)
            }

            handler.post {
                dummyFrame.removeAllViews()
                removeView(dummyFrame)

                var widthSum = 0
                var rowFrame = LinearLayout(context)

                for (i in 0 until tagViews.size) {

                    if (i == 0 || widthSum + tagWidthList[i] > containerWidth) {
                        rowFrame = generateAndAddRowFrame()
                        widthSum = 0
                    }
                    rowFrame.addView(tagViews[i])
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

    fun addTagView(tagView: View) {
        tagViews.add(tagView)
        updateTagViews()
    }

    fun removeTagView(tagView: View) {
        tagViews.remove(tagView)
        updateTagViews()
    }
}
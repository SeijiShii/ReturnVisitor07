package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

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

    }

    private fun updateTagViews() {

        for (tagView in tagViews) {
            (tagView.parent as? ViewGroup)?.removeView(tagView)
        }

        removeAllViews()

        val dummyFrame = FrameLayout(context).also {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(dummyFrame)

        for (tagView in tagViews) {
            dummyFrame.addView(tagView)
        }
    }

    fun addTagView(tagView: View) {

        tagViews.add(tagView)
        tagWidthList.add(0)

        updateTagViews()
    }
}
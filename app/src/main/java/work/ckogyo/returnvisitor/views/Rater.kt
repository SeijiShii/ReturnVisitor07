package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class Rater : LinearLayout {

    var onClickButton: ((index:Int) -> Unit)? = null

    constructor(context: Context?) : super(context){initCommon()}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){initCommon()}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){initCommon()}

    private fun initCommon() {
        layoutParams = ViewGroup.LayoutParams(context!!.toDP(280), context!!.toDP(30))
        initButtons()
    }

    private val resIds = arrayOf(
        R.drawable.gray_circle_button,
        R.drawable.red_circle_button,
        R.drawable.purple_circle_button,
        R.drawable.blue_circle_button,
        R.drawable.green_circle_button,
        R.drawable.gold_circle_button,
        R.drawable.orange_circle_button
    )

    private val buttons = ArrayList<ImageView>()

    private var currIndex = 0

    private fun initButtons() {

        context?:return

        for (i in 0 until 7) {

            val button = ImageView(context)
            button.layoutParams = LayoutParams(context!!.toDP(40), context!!.toDP(30))
            button.setPadding(context!!.toDP(10), context!!.toDP(5), context!!.toDP(10), context!!.toDP(5))
            button.tag = i
            button.setOnClick {
                currIndex = button.tag as Int
                refreshButtons()
                onClickButton?.invoke(currIndex)
            }
            buttons.add(button)
            addView(button)
        }
        refreshButtons()
    }

    private fun setButtonImage(button: ImageView, resId: Int) {
        button.setImageDrawable(ResourcesCompat.getDrawable(context!!.resources, resId, null))
    }

    private fun refreshButtons() {
        for (i in 0 until buttons.size) {
            if (currIndex >= i) {
                setButtonImage(buttons[i], resIds[currIndex])
            } else {
                setButtonImage(buttons[i], resIds[0])
            }
        }
    }

    fun refresh(index: Int) {
        currIndex = index
        refreshButtons()
    }

}
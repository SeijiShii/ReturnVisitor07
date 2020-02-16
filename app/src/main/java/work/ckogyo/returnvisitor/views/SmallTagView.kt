package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.LinearLayout
import android.widget.TextView
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.toDP

class SmallTagView(context: Context, text: String) : TextView(context), TagView {

    var backgroundResourceId = R.drawable.green_border_round
        set(value) {
            field = value
            setBackgroundResource(field)
        }

    init {

        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, context.toDP(30)).also {
            val m = context.toDP(3)
            it.setMargins(m, m, m, m)
        }
        gravity = Gravity.CENTER

        val p = context.toDP(10)
        setPadding(p, p, p, p)

        this.text = " $text "
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(R.color.darkGray)
        setBackgroundResource(backgroundResourceId)
    }

    override var onRemoved: ((TagView) -> Unit)?
        get() = {}
        set(value) {}

    override val viewWidth: Int
        get() = width

    override fun removeFromParent() {
        (parent as? ViewGroup)?.removeView(this)
    }

    override fun addToParent(parent: ViewGroup) {
        parent.addView(this)
    }
}
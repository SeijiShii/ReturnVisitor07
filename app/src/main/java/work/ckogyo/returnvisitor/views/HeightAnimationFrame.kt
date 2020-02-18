package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.debugTag

class HeightAnimationFrame : HeightAnimationView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

}
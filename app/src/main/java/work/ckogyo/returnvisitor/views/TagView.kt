package work.ckogyo.returnvisitor.views

import android.view.View
import android.view.ViewGroup

interface TagView {
    var onRemoved: ((TagView) -> Unit)?

    val viewWidth: Int

    fun removeFromParent()

    fun addToParent(parent: ViewGroup)
}
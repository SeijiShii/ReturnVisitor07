package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.view.View
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place

class HousingComplexDialog(private val hComplex: Place) : DialogFrameFragment() {

    var onOk : (() -> Unit)? = null

    override fun onOkClick() {
        onOk?.invoke()
    }

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.housing_complex_dialog, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        allowScroll = true
//        allowResize = false

        super.onViewCreated(view, savedInstanceState)

        setTitle(R.string.housing_complex)
    }
}
package work.ckogyo.returnvisitor.dialogs

import android.app.Activity
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.text_popup_dialog.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.*
import java.lang.Exception

class TextPopupDialog(private val anchor: View, private val frameId: Int) : Fragment() {

    private var isClosedBySelf = false
    private var isAlreadyClosed = false

    private lateinit var frame: ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.text_popup_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        frame = (context as Activity).findViewById(frameId)

        textPopupOverlay.alpha = 0f

        val handler = Handler()
        GlobalScope.launch {
            while (!isVisible) {
                delay(50)
            }

            while (popupDialog.width <= 0 && popupDialog.height <= 0) {
                delay(50)
            }
            Log.d(debugTag, "width: ${popupDialog.width}, height: ${popupDialog.height}")

            val positionInFrame = anchor.getPositionInAncestor(frame)
            Log.d(debugTag, "anchor x: ${positionInFrame.x}, y: ${positionInFrame.y}")

            handler.post {

                decidePosition(positionInFrame)

                textPopupOverlay.fadeVisibility(true)
            }
        }

        textPopupOverlay.setOnTouchListener { _, _ ->
            close()
            true
        }

        closeTextPopupButton.setOnClick {
            close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isClosedBySelf) {
            throw Exception("TextPopupDialog must be closed by TextPopupDialog::close() function!")
        }
    }

    fun show(fm: FragmentManager) {

        isAlreadyClosed = false

        fm.beginTransaction()
            .addToBackStack(null)
            .add(frameId, this)
            .commit()
    }

    fun close() {

        isClosedBySelf = true

        // 間違えて2回呼んでも余計なフラグメントを消さないように
        if (!isAlreadyClosed) {
            isAlreadyClosed = true

            textPopupOverlay.fadeVisibility(false, onAnimationFinished = {
                (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    .remove(this)
                    .commit()
            })
        }
    }

    private fun decidePosition(anchorPos: Point) {

        val margin = context!!.toDP(10)
        val x = when {
            // アンカーの左側にスペースがあれば、アンカー左側に密着
            anchorPos.x >= popupDialog.width + margin -> anchorPos.x - popupDialog.width
            // アンカー右側にスペースがあれば右側に密着
            frame.width - (anchorPos.x + anchor.width) >= popupDialog.width + margin -> anchorPos.x + anchor.width
            // どちらにもスペースがなければフレームの左からマージンを開けて
            else -> margin
        }

        val y = when {
            // アンカーの上側にスペースがあれば
            anchorPos.y >= popupDialog.height + margin -> anchorPos.y - popupDialog.height
            // アンカーの下側にスペースがあれば
            frame.height - (anchorPos.y + anchor.height) >= popupDialog.height + margin -> anchorPos.y + anchor.height
            // どちらにもスペースがない
            else -> margin
        }

        popupDialog.layoutParams = FrameLayout.LayoutParams(popupDialog.width, popupDialog.height).also {
            it.leftMargin = x
            it.topMargin = y
        }

        popupDialog.requestLayout()
    }

}
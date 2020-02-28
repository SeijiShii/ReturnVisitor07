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
import kotlinx.android.synthetic.main.popup_dialog.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.*
import java.lang.Exception

abstract class PopupDialog(private val anchor: View, private val frameId: Int) : Fragment() {

    abstract fun inflateContentView(): View

    var showCloseButton = false

    enum class VerticalAnchorPosition {
        Top,
        Bottom
    }

    enum class HorizontalAnchorPosition {
        Left,
        Right
    }

    var verticalAnchorPosition = VerticalAnchorPosition.Bottom
    var horizontalAnchorPosition = HorizontalAnchorPosition.Left

    private var isClosedBySelf = false
    private var isAlreadyClosed = false

    private lateinit var frame: ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.popup_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        frame = (context as Activity).findViewById(frameId)

        val v = inflateContentView()
        viewFrame.addView(v)

        closeButtonRow.visibility = if (showCloseButton) {
            View.VISIBLE
        } else {
            View.GONE
        }

        popupOverlay.alpha = 0f

        val handler = Handler()
        GlobalScope.launch {
            while (!isVisible) {
                delay(50)
            }

            while (popupOverlay.width <= 0 || popupOverlay.height <= 0) {
                delay(50)
            }

            oldFrameHeight = frame.height

            Log.d(debugTag, "width: ${popupDialog.width}, height: ${popupDialog.height}")

            val positionInFrame = anchor.getPositionInAncestor(frame)
            Log.d(debugTag, "anchor x: ${positionInFrame.x}, y: ${positionInFrame.y}")

            handler.post {

                decidePosition(positionInFrame)

                popupOverlay.fadeVisibility(true)
                watchFrameHeightChange()
            }
        }

        popupOverlay.setOnTouchListener { _, _ ->
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

        showCore(fm)
    }

    private fun showCore(fm: FragmentManager) {
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

            popupOverlay.fadeVisibility(false, onAnimationFinished = {
                (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    .remove(this)
                    .commit()
            })
        }

        hideKeyboard(context as Activity)
    }

    private fun decidePosition(anchorPos: Point) {

        val margin = context!!.toDP(10)
        val x = when (horizontalAnchorPosition) {
            HorizontalAnchorPosition.Left -> {
                when {
                    // アンカーの左側と左揃え
                    frame.width - anchorPos.x >= popupDialog.width + margin -> anchorPos.x
                    // 右側に十分なスペースがなければ画面右端からマージンを開けた分
                    else -> frame.width - (popupDialog.width + margin)
                }
            }
            else -> {
                when {
                    // アンカーの右側と左揃え
                    frame.width - (anchorPos.x + anchor.width) >= popupDialog.width + margin -> anchorPos.x
                    // 右側に十分なスペースがなければ画面右端からマージンを開けた分
                    else -> frame.width - (popupDialog.width + margin)
                }
            }
        }

        val y = when(verticalAnchorPosition) {
            VerticalAnchorPosition.Bottom -> {
                when {
                    // アンカーの下側
                    frame.height - (anchorPos.y + anchor.height) > popupDialog.height + margin -> anchorPos.y + anchor.height
                    // 下側に十分なスペースがなければ画面下端からマージンを開けた分
                    else -> frame.height - (popupDialog.height + margin)
                }
            }
            else -> {
                when {
                    // アンカーの上側
                    frame.height - anchorPos.y > popupDialog.height + margin -> anchorPos.y + anchor.height
                    // 下側に十分なスペースがなければ画面下端からマージンを開けた分
                    else -> frame.height - (popupDialog.height + margin)
                }
            }
        }

        popupDialog.layoutParams = FrameLayout.LayoutParams(popupDialog.width, popupDialog.height).also {
            it.leftMargin = x
            it.topMargin = y
        }

        oldPopupTop = y
        popupDialog.requestLayout()
    }

    private var oldFrameHeight = 0
    private var oldPopupTop = 0
    private fun watchFrameHeightChange() {
        val handler = Handler()
        GlobalScope.launch {
            while (true) {
                delay(50)

                context ?: return@launch

                if (oldFrameHeight != frame.height) {

                    var popupTop = 0
                    val popupHeight = if (popupDialog.height > frame.height) {
                        popupTop = context!!.toDP(10)
                        frame.height - context!!.toDP(20)
                    } else {
                        popupTop = (frame.height - oldFrameHeight) + oldPopupTop
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    }

                    handler.post {
                        context ?: return@post
                        popupDialog ?: return@post

                        (popupDialog.layoutParams as FrameLayout.LayoutParams).topMargin = popupTop
                        oldPopupTop = popupTop

                        popupDialog.layoutParams.height = popupHeight

                        popupDialog.requestLayout()
                        oldFrameHeight = frame.height
                    }
                }
            }
        }
    }
}
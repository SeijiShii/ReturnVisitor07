package work.ckogyo.returnvisitor.dialogs

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.dialog_frame_framgent.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.hideKeyboard
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP
import kotlin.concurrent.thread

abstract class DialogFrameFragment : Fragment() {

    enum class CloseButtonStyle {
        None,
        OKAndCancel,
        CloseOnly
    }

    protected var closeButtonStyle = CloseButtonStyle.OKAndCancel
        set(value) {
            field = value
            refreshCloseButtonStyle()
        }

    protected var allowScroll = true
    protected var allowResize = true

    var isCancelable: Boolean = true
    set(value) {
        field = value

        if (value) {
            overlay.setOnClick {
                close()
            }
        } else {
            overlay.setOnTouchListener { _, _ -> true }
        }
    }

    abstract fun onOkClick()

    open fun onCancelClick(){}

    open fun onCloseClick(){}

    abstract fun inflateContentView(): View

    protected lateinit var contentView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.dialog_frame_framgent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentView = inflateContentView()

        if (allowScroll) {
            dialogContentFrame1.addView(contentView)
            dialogContentFrame2.visibility = View.GONE
            if (allowResize) {
                dialogContentFrame1.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                dialogContentFrame1.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        } else {
            dialogContentFrame2.addView(contentView)
            dialogScrollView.visibility = View.GONE
        }

        isCancelable = true

        okButton.setOnTouchListener(object: OnButtonClickListener(okButton, this) {
            override fun onClick(view: View) {
                hideKeyboard(context as Activity)
                onOkClick()
            }
        })

        cancelButton.setOnTouchListener(object : OnButtonClickListener(cancelButton, this) {
            override fun onClick(view: View) {
                hideKeyboard(context as Activity)
                onCancelClick()
            }
        })

        closeButton.setOnTouchListener(object : OnButtonClickListener(cancelButton, this) {
            override fun onClick(view: View) {
                hideKeyboard(context as Activity)
                onCloseClick()
            }
        })

        refreshCloseButtonStyle()

        // すべてをブロックする無敵のタッチリスナ
        dialogOuterFrame.setOnTouchListener{ _, _ -> true }

        watchOverlaySizeAndResize()

        setTitle(null)

        overlay.alpha = 0f
        overlay.fadeVisibility(true)

    }

    private fun refreshCloseButtonStyle() {

        when(closeButtonStyle) {
            CloseButtonStyle.None -> {
                okButton.visibility = View.GONE
                cancelButton.visibility = View.GONE
                closeButton.visibility = View.GONE
            }
            CloseButtonStyle.OKAndCancel -> {
                okButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                closeButton.visibility = View.GONE
            }
            CloseButtonStyle.CloseOnly -> {
                okButton.visibility = View.GONE
                cancelButton.visibility = View.GONE
                closeButton.visibility = View.VISIBLE
            }
        }
    }


    protected fun setTitle(title: String?) {

        dialogTitleTextView.visibility = if (title == null || title.isEmpty()) {
            View.GONE
        } else {
            dialogTitleTextView.text = title
            View.VISIBLE
        }
    }

    protected fun setTitle(resId: Int) {
        val str = getString(resId)
        setTitle(str)
    }

//    protected fun setView(view: View) {
//        dialogContentFrame.addView(view)
//    }

    fun show(frameId: Int, fm: FragmentManager, tag: String) {

        isAlreadyClosed = false

        fm.beginTransaction()
            .addToBackStack(null)
            .add(frameId, this, tag)
            .commit()
    }


    private var isAlreadyClosed = false
    open fun close() {
        hideKeyboard(context as Activity)

        // 間違えて2回呼んでも余計なフラグメントを消さないように
        if (!isAlreadyClosed) {
            isAlreadyClosed = true

            overlay.fadeVisibility(false, onAnimationFinished = {
                (context as AppCompatActivity).supportFragmentManager.popBackStack()
            })
        }
    }

    private abstract class OnButtonClickListener(val view: View, val dialog: DialogFrameFragment): View.OnTouchListener {

        abstract fun onClick(view: View)

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {

            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.alpha = 0.5f
                }
                MotionEvent.ACTION_UP -> {
                    view.alpha = 1f
                    onClick(view)
                    dialog.close()
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 1f
                }
            }

            return true
        }
    }

    private fun watchOverlaySizeAndResize() {

        if (!allowResize) return

        val heightBias = context!!.toDP(115)
        val handler = Handler()

        var oldHeight = 0

        thread {

            while (true) {
                Thread.sleep(50)

                overlay?:break

                if (oldHeight != overlay.height) {
                    oldHeight = overlay.height

                    handler.post {

                        if (contentOuterFrame.height > contentFrameHeight) {
                            contentOuterFrame.layoutParams.height = contentFrameHeight
                            dialogOuterFrame.layoutParams.height = contentFrameHeight + heightBias
                        } else {
                            contentOuterFrame.layoutParams.height = 0
                            (contentOuterFrame.layoutParams as LinearLayout.LayoutParams).weight = 1f
                            dialogOuterFrame.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                        }

                        if (allowScroll) {
                            if (dialogScrollView.height > contentFrameHeight) {
                                dialogContentFrame1.layoutParams.height = dialogScrollView.height
                            } else {
                                dialogContentFrame1.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }

                        contentOuterFrame.requestLayout()
                    }
                }
            }
        }

    }

    private val contentFrameHeight: Int
    get() {
        return if (dialogContentFrame1.height > dialogContentFrame2.height) {
            dialogContentFrame1.height
        } else {
            dialogContentFrame2.height
        }
    }

    var isOKButtonEnabled: Boolean = true
    set(value) {
        field = value

        okButton.isEnabled = value
        okButton.visibility = if (value) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

}
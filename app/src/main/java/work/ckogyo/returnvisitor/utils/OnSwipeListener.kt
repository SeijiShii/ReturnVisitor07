package work.ckogyo.returnvisitor.utils

import android.view.MotionEvent
import android.view.View

abstract class OnSwipeListener: View.OnTouchListener {

    companion object {
        const val SWIPE_UP = 1
        const val SWIPE_DOWN = 2
        const val SWIPE_TO_LEFT = 4
        const val SWIPE_TO_RIGHT = 8
    }

    abstract fun onSwipe(v: View?, direction: Int)

    private var isSwiping = false
    private var startPoint : MotionEvent.PointerCoords? = null

    override fun onTouch(v: View?, e: MotionEvent?): Boolean {

        when(e?.action) {
            MotionEvent.ACTION_DOWN -> {
                isSwiping = true

                startPoint = MotionEvent.PointerCoords()
                e.getPointerCoords(0, startPoint)
            }
            MotionEvent.ACTION_UP -> {

                if (isSwiping && startPoint != null) {
                    isSwiping = false
                    val endPoint = MotionEvent.PointerCoords()
                    e.getPointerCoords(0, endPoint)

                    val xMove = endPoint.x - startPoint!!.x
                    val yMove = endPoint.y - startPoint!!.y

                    var direction = 0

                    when{
                        yMove < 0 -> direction += SWIPE_UP
                        yMove > 0 -> direction += SWIPE_DOWN
                    }

                    when{
                        xMove > 0 -> direction += SWIPE_TO_RIGHT
                        xMove < 0 -> direction += SWIPE_TO_LEFT
                    }

                    startPoint = null

                    onSwipe(v, direction)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isSwiping = false
                startPoint = null
            }
        }
        return true
    }
}
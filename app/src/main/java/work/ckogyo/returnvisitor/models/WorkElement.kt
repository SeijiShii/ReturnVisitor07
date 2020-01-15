package work.ckogyo.returnvisitor.models

import java.util.*
import kotlin.collections.ArrayList

/**
 * WorkFragmentに表示するためのデータ要素
 */
class WorkElement(val category: Category) {

    companion object {
        fun fromWork(work: Work): ArrayList<WorkElement> {
            val startElm = WorkElement(Category.WorkStart)
            startElm.work = work
            val endElm = WorkElement(Category.WorkEnd)
            endElm.work = work
            return arrayListOf(startElm, endElm)
        }
    }

    enum class Category {
        WorkStart,
        WorkEnd,
        Visit,
        DateBorder
    }

    var work: Work? = null
    var visit: Visit? = null
    private var borderDate: Calendar? = null

    var isVisitInWork = false


    var dateTime: Calendar
        get() {
            return when(category) {
                Category.WorkStart -> work!!.start
                Category.WorkEnd -> work!!.end
                Category.Visit -> visit!!.dateTime
                Category.DateBorder -> borderDate!!
            }
        }

        set(value) {
            when(category) {
                Category.WorkStart -> work!!.start = value
                Category.WorkEnd -> work!!.end = value
                Category.Visit -> visit!!.dateTime = value
                Category.DateBorder -> borderDate = value
            }
        }


}
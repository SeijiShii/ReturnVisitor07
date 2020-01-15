package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.areSameDates
import work.ckogyo.returnvisitor.utils.cloneDateWith0Time
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WorkListElm(val category: Category) {

    companion object {
        private fun fromWork(work: Work): ArrayList<WorkListElm> {
            val startElm = WorkListElm(Category.WorkStart)
            startElm.work = work
            val endElm = WorkListElm(Category.WorkEnd)
            endElm.work = work
            return arrayListOf(startElm, endElm)
        }

        private fun generateList(works: ArrayList<Work>, visits: ArrayList<Visit>): ArrayList<WorkListElm> {

            var elms = ArrayList<WorkListElm>()

            for (work in works) {
                elms.addAll(fromWork(work))
            }

            for (visit in visits) {
                val elm = WorkListElm(Category.Visit)
                elm.visit = visit
                elms.add(elm)
            }

            elms = updateDateBorders(elms)

            elms.sortBy { e -> e.dateTime.timeInMillis }

            return elms
        }

        suspend fun generateListByDate(date: Calendar): ArrayList<WorkListElm> = suspendCoroutine {  cont ->
            GlobalScope.launch {
                val works = WorkCollection.instance.loadWorksByDate(date)
                val visits = VisitCollection.instance.loadVisitsByDate(date)

                cont.resume(generateList(works, visits))
            }
        }

        private fun updateDateBorders(elms: ArrayList<WorkListElm>): ArrayList<WorkListElm> {

            val tmp = ArrayList<WorkListElm>()
            for (elm in elms) {
                if (elm.category != Category.DateBorder) {
                    tmp.add(elm)
                }
            }

            tmp.sortBy { elm -> elm.dateTime.timeInMillis }

            if (tmp.size > 0) {
                val borderElms = ArrayList<WorkListElm>()
                val borderEml1 = WorkListElm(Category.DateBorder)
                borderEml1.dateTime = cloneDateWith0Time(tmp[0].dateTime)
                borderElms.add(borderEml1)

                if (tmp.size > 1) {
                    for (i in 0 until tmp.size - 1) {
                        if (!areSameDates(tmp[i].dateTime, tmp[i + 1].dateTime)) {

                            val borderElm2 = WorkListElm(Category.DateBorder)
                            borderElm2.dateTime = cloneDateWith0Time(tmp[i + 1].dateTime)
                            borderElms.add(borderElm2)
                        }
                    }
                }

                tmp.addAll(borderElms)
                tmp.sortBy { elm -> elm.dateTime.timeInMillis }
            }

            return tmp
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
    var borderDate: Calendar? = null


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
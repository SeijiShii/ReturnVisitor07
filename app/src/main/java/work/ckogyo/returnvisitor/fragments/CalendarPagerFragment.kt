package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.calendar_pager_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.loadMonthList
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList

class CalendarPagerFragment(private var monthToShow: Calendar) : Fragment() {

    private lateinit var adapter: CalendarPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.calendar_pager_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnTouchListener { _, _ -> true }

        val handler = Handler()

        GlobalScope.launch {
            val months = loadMonthList()
            handler.post {

                val fm = (context as? MainActivity)?.supportFragmentManager
                fm ?: return@post

                adapter = CalendarPagerAdapter(fm, months)
                calendarPager.adapter = adapter

                val pos = adapter.getPositionByMonth(monthToShow)
                if (pos >= 0) {
                    calendarPager.currentItem = pos
                }
            }
        }

        calendarPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                calendarMonthText.text = adapter.months[position].toMonthText()
            }
        })
        calendarPager.offscreenPageLimit = 5


    }

    /**
     * @param months 最初のデータがある月から最後のデータがある月までの月初日のカレンダー。その期間の月はデータがなくても含む。
     *
     */
    private inner class CalendarPagerAdapter(fm: FragmentManager, val months: ArrayList<Calendar>): FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return CalendarFragment(months[position])
        }

        override fun getCount(): Int {
            return months.size
        }

        fun getPositionByMonth(month: Calendar): Int {

            for (i in 0 until months.size) {
                if (month.isSameMonth(months[i])){
                    return i
                }
            }
            return -1
        }
    }

}
package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
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

    private val mainActivity: MainActivity?
        get() = context as? MainActivity

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

        backToMapButton.setOnClick {
            backToMapFragment()
        }

        calendarMenuButton.setOnClick {
            showMenuPopup()
        }

        val handler = Handler()

        loadingCalendarOverlay2.fadeVisibility(true, addTouchBlockerOnFadeIn = true)

        GlobalScope.launch {
            val months = loadMonthList()
            handler.post {

                loadingCalendarOverlay2.fadeVisibility(false)

                // Fragment内でViewPagerを使うときはchildFragmentManagerを渡すべし。
                // https://phicdy.hatenablog.com/entry/fragmentmanager_to_fragmentpageradapter_in_fragment
                adapter = CalendarPagerAdapter(childFragmentManager, months)
                calendarPager.adapter = adapter

                val pos = adapter.getPositionByMonth(monthToShow)
                if (pos >= 0) {
                    calendarPager.currentItem = pos
                }

                refreshLeftButton()
                refreshRightButton()
            }
        }

        calendarPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                monthToShow = adapter.months[position]

                refreshMonthText()
                refreshLeftButton()
                refreshRightButton()
            }
        })
        calendarPager.offscreenPageLimit = 10

        leftButton.setOnClick {
            calendarPager.currentItem -= 1
        }

        rightButton.setOnClick {
            calendarPager.currentItem += 1
        }

        refreshMonthText()

    }

    private fun refreshMonthText() {
        calendarMonthText.text = monthToShow.toMonthText()
    }

    private fun refreshLeftButton() {
        leftButton.isEnabled = calendarPager.currentItem > 0
    }

    private fun refreshRightButton() {
        rightButton.isEnabled = calendarPager.currentItem < adapter.months.size - 1
    }

    private fun showMenuPopup() {

        PopupMenu(context, calendarMenuButton).also {
            it.menuInflater.inflate(R.menu.calendar_menu, it.menu)

            it.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.month_summary -> {
                        mainActivity?.showMonthReportDialog(monthToShow)
                    }
                    R.id.report_mail -> {
                        Log.d(debugTag, monthToShow.toMonthTitleString(context!!))
                        mainActivity?.prepareReportMail(monthToShow)
                    }
                    R.id.switch_week_start -> {
                    }
                }
                return@setOnMenuItemClickListener true
            }
            it.show()
        }
    }

    /**
     * @param months 最初のデータがある月から最後のデータがある月までの月初日のカレンダー。その期間の月はデータがなくても含む。
     *
     */
    private inner class CalendarPagerAdapter(fm: FragmentManager, val months: ArrayList<Calendar>): FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return CalendarFragment(months[position]).also {
                it.onTransitToWorkFragment = {
                    val fm = mainActivity?.supportFragmentManager
                    fm?.beginTransaction()?.remove(this@CalendarPagerFragment)?.commit()
                }
            }
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

    private fun backToMapFragment() {

        mainActivity?.supportFragmentManager?.beginTransaction()
            ?.remove(this)
            ?.commit()
    }

}
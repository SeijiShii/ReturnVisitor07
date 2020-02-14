package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.calendar_pager_fragment.*
import work.ckogyo.returnvisitor.R
import java.util.*

class CalendarPagerFragment(private var monthToShow: Calendar) : Fragment() {

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

        val cFragment = CalendarFragment(monthToShow)
        calendarPager.initialize(cFragment)
    }

}
package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.lazy_load_pager.view.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R

class LazyLoadPager : FrameLayout {

    private val fragments = ArrayList<Fragment>()

    var onSwipeToLeftEnd: ((onLeftPageLoaded:(f: Fragment) -> Unit) -> Unit)? = null
    var onSwipeToRightEnd: ((onRightPageLoaded: (f: Fragment) -> Unit) -> Unit)? = null

    constructor(context: Context) : super(context) {initCommon()}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs){initCommon()}

    private fun initCommon() {
        View.inflate(context, R.layout.lazy_load_pager, this)
    }

    fun initialize(firstFragment: Fragment, finished: ((LazyLoadPager) -> Unit)? = null) {

        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener(){
            override fun onPageSelected(position: Int) {
                when(position) {
                    0 -> {
                        onSwipeToLeftEnd?.invoke{
                            addFragment(it, true)
                        }
                    }
                    fragments.size - 1 -> {
                        onSwipeToRightEnd?.invoke{
                            addFragment(it, false)
                        }
                    }
                }
            }
        })

        fragments.clear()
        fragments.add(firstFragment)

        val fm = (context as MainActivity).supportFragmentManager
        pager.adapter = LazyLoadPagerAdapter(fm)

        finished?.invoke(this)
    }

    fun addFragment(f: Fragment, inLeft: Boolean) {

        if (inLeft) {
            fragments.add(0, f)
        } else {
            fragments.add(f)
        }

        (pager.adapter as FragmentPagerAdapter).notifyDataSetChanged()

        if (inLeft) {
            handler.post {
                pager.setCurrentItem(pager.currentItem + 1, false)
            }
        }
    }

    inner class LazyLoadPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }
    }

}
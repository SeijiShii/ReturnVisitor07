package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import work.ckogyo.returnvisitor.MainActivity

class LazyLoadPager : FrameLayout {

    private lateinit var pager: ViewPager
    private val fragments = ArrayList<Fragment>()

    var onSwipeToLeftEnd: ((onLeftPageLoaded:(f: Fragment) -> Unit) -> Unit)? = null
    var onSwipeToRightEnd: ((onRightPageLoaded: (f: Fragment) -> Unit) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun initialize(firstFragment: Fragment) {

        pager = ViewPager(context)
        pager.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(pager)

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
        pager.adapter = LazyLoadPagerAdapter(fm, fragments)
    }

    fun addFragment(f: Fragment, inLeft: Boolean) {

        (pager.adapter as LazyLoadPagerAdapter).addFragment(f, inLeft)
        (pager.adapter as FragmentPagerAdapter).notifyDataSetChanged()

        if (inLeft) {
            handler.post {
                pager.setCurrentItem(pager.currentItem + 1, false)
            }
        }
    }

    class LazyLoadPagerAdapter(fm: FragmentManager,
                               private val fragments: ArrayList<Fragment>): FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        fun addFragment(f: Fragment, inLeft: Boolean) {

            if (inLeft) {
                fragments.add(0, f)
            } else {
                fragments.add(f)
            }
        }
    }

}
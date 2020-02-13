package work.ckogyo.returnvisitor.dialogs

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.placement_dialog.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlacementCollection
import work.ckogyo.returnvisitor.fragments.AddPlacementFragment
import work.ckogyo.returnvisitor.fragments.PlacementListFragment
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.setOnClick

class PlacementDialog :DialogFrameFragment() {

    private val mainActivity
        get() = context as? MainActivity

    enum class DialogState {
        RecentUsed,
        AddNew
    }

    var onAddPlacement: ((Placement) -> Unit)? = null

    override fun onOkClick() {}

    override fun inflateContentView(): View {
        return View.inflate(context,
            R.layout.placement_dialog, null)
    }

    private var barContainerWidth = 0
    private var state =
        DialogState.RecentUsed

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        showCloseButtonOnly = true
        allowScroll = false

        super.onViewCreated(view, savedInstanceState)

        setTitle(R.string.placement)

        barContainer.viewTreeObserver.addOnGlobalLayoutListener {
            barContainer ?: return@addOnGlobalLayoutListener
            barContainerWidth = barContainer.width
            bar.layoutParams.width = barContainerWidth / 2
            bar.requestLayout()
        }

        recentlyUsedButton.setOnClick {
            state =
                DialogState.RecentUsed
            switchBar()
            switchPager()

        }

        addPlacementButton.setOnClick {
            state =
                DialogState.AddNew
            switchBar()
            switchPager()
        }

        mainActivity ?: return

        placementPager.adapter =
            PlacementDialogAdapter(
                childFragmentManager
            )
        switchPager()

        placementPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                state = if (position == 0) DialogState.RecentUsed else DialogState.AddNew
                switchBar()
            }
        })
    }

    private fun switchPager() {
        when(state) {
            DialogState.RecentUsed -> placementPager.setCurrentItem(0, true)
            else -> placementPager.setCurrentItem(1, true)
        }
    }

    private fun switchBar() {

        val origin = (bar.layoutParams as FrameLayout.LayoutParams).leftMargin
        val target = if (state == DialogState.RecentUsed) 0 else barContainerWidth / 2

        val animator = ValueAnimator.ofInt(origin, target)
        animator.addUpdateListener {
            val params = bar.layoutParams as FrameLayout.LayoutParams
            params.leftMargin = it.animatedValue.toString().toInt()
            bar.layoutParams = params
            bar.requestLayout()
        }
        animator.duration = 300
        animator.start()
    }

    private fun onPlacementLoadedInFragment(count: Int) {
        if (count <= 0) {
            state = DialogState.AddNew
            switchBar()
            switchPager()
        }
    }

    private fun onClickOkInAddPlacementFragment(plc: Placement) {
        close()
        onAddPlacement?.invoke(plc)
        GlobalScope.launch {
            PlacementCollection.instance.setAsync(plc)
        }
    }

    inner class PlacementDialogAdapter(fm: FragmentManager): FragmentStatePagerAdapter(fm) {

//        private val placementListFragment = PlacementListFragment()
//        private val addPlacementFragment = AddPlacementFragment()

        private val items = arrayOf(
            PlacementListFragment().also {
                it.onPlacementLoaded = this@PlacementDialog::onPlacementLoadedInFragment
            },
            AddPlacementFragment().also {
                it.onOk = this@PlacementDialog::onClickOkInAddPlacementFragment
            }
        )

        override fun getItem(position: Int): Fragment {
            return items[position]
        }

        override fun getCount(): Int {
            return 2
        }
    }
}
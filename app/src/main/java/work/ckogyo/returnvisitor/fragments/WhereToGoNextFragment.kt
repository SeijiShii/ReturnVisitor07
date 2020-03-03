package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.where_to_go_next_fragment.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.VisitFilter
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class WhereToGoNextFragment : Fragment() {

    var onBackToMapFragment: (() -> Unit)? = null

    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    private lateinit var periodTerms: ArrayList<String>

    private lateinit var visitFilter: VisitFilter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.where_to_go_next_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context ?: return

        visitFilter = VisitFilter.loadFromSharedPreferences(context!!)

        view.setOnTouchListener { _, _ -> true }

        backToMapButton.setOnClickListener {
            backToMapFragment()
        }

        filterPanelFrame.extractHeight = context!!.toDP(100)

        filterPanelFrame.isExtracted = false
        filterPanelFrame.refreshHeight()

        panelExtractBar.setOnClick {
            filterPanelFrame.isExtracted = !filterPanelFrame.isExtracted
            filterPanelFrame.animateHeight()

            refreshArrowImage()
        }

        periodTerms = context!!.resources.getStringArray(R.array.periodUnitArray).toCollection(ArrayList())

        initFilterPanel()

    }

    private fun refreshArrowImage() {
        val arrowResId = if (filterPanelFrame.isExtracted) {
            R.drawable.ic_keyboard_arrow_down_white_24dp
        } else {
            R.drawable.ic_keyboard_arrow_up_white_24dp
        }
        arrowImageView.setBackgroundResource(arrowResId)
    }

    private fun initFilterPanel() {

        redFilterButton.setOnClickListener {

        }

        purpleFilterButton.setOnClickListener {

        }

        blueFilterButton.setOnClickListener {

        }

        greenFilterButton.setOnClickListener {

        }

        goldFilterButton.setOnClickListener {

        }

        orangeFilterButton.setOnClickListener {

        }

        initPeriodStartSpinner()
        initPeriodEndSpinner()
    }

    private fun initPeriodStartSpinner() {
        val startArray = periodTerms.subList(0, periodTerms.size - 1)
        periodStartSpinner.adapter = ArrayAdapter<String>(context!!, R.layout.simple_text_view, startArray)
        periodStartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var periodStart = VisitFilter.PeriodTerm.values()[position]
                if (periodStart.ordinal >= visitFilter.periodEnd.ordinal) {
                    periodStart = VisitFilter.PeriodTerm.values()[visitFilter.periodEnd.ordinal - 1]
                    periodStartSpinner.setSelection(periodStart.ordinal)
                }
                visitFilter.periodStart = periodStart
            }
        }
        periodStartSpinner.setSelection(visitFilter.periodStart.ordinal)

    }

    private fun initPeriodEndSpinner() {
        val endArray = periodTerms.subList(1, periodTerms.size)
        periodEndSpinner.adapter = ArrayAdapter<String>(context!!, R.layout.simple_text_view, endArray)
        periodEndSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var periodEnd = VisitFilter.PeriodTerm.values()[position + 1]
                if (periodEnd.ordinal <= visitFilter.periodStart.ordinal) {
                    periodEnd = VisitFilter.PeriodTerm.values()[visitFilter.periodStart.ordinal + 1]
                    periodEndSpinner.setSelection(periodEnd.ordinal - 1)
                }
                visitFilter.periodEnd = periodEnd
            }
        }
        periodEndSpinner.setSelection(visitFilter.periodEnd.ordinal - 1)
    }

    private fun backToMapFragment() {

        mainActivity?.supportFragmentManager?.beginTransaction()
            ?.remove(this)
            ?.commit()

        onBackToMapFragment?.invoke()
    }
}
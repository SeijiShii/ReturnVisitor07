package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.add_placement_fragment.*
import kotlinx.android.synthetic.main.dialog_frame_framgent.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.DialogFrameFragment
import work.ckogyo.returnvisitor.models.Placement
import java.util.*

class AddPlacementFragment() : Fragment(){

    companion object {
        private const val maxYearCount = 10
        private const val maxNumber = 3
    }

    private var dialog: DialogFrameFragment? = null

    constructor(dialog: DialogFrameFragment): this() {
        this.dialog = dialog
    }

    private var placement = Placement()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_placement_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPlacementCategorySpinner()
        initMagazineTypeSpinner()
        initYearSpinner()
        initNumberSpinner()
        refreshOKButton()
        refreshMagazineRows()
    }

    private fun initPlacementCategorySpinner() {

        context?:return

        val adapter = ArrayAdapter.createFromResource(context!!, R.array.placementCategoryArray, R.layout.simple_text_view)
        adapter.setDropDownViewResource(R.layout.simple_text_view)
        placementCategorySpinner.adapter = adapter
        placementCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                placement.category = Placement.Category.values()[position]
                refreshOKButton()
                refreshMagazineRows()
            }
        }
    }

    private fun initMagazineTypeSpinner() {

        context?:return

        val adapter = ArrayAdapter.createFromResource(context!!, R.array.magazineTypeArray, R.layout.simple_text_view)
        adapter.setDropDownViewResource(R.layout.simple_text_view)
        magazineTypeSpinner.adapter = adapter
        magazineTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                placement.magazineType = Placement.MagazineType.values()[position]
            }
        }
    }

    private lateinit var yearArray: Array<Int>
    private fun initYearSpinner() {

        context?:return

        // 当年から見て1年先までの10年間を選択できるようにする
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        yearArray = Array(maxYearCount){ thisYear - (maxYearCount - 2) + it }
        val adapter = ArrayAdapter(context!!, R.layout.simple_text_view, yearArray)

        placementYearSpinner.adapter = adapter
        placementYearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                placement.year = yearArray[position]
            }
        }
        placementYearSpinner.setSelection(maxYearCount - 2)
    }

    private lateinit var numberArray: Array<Int>
    private fun initNumberSpinner() {

        context?:return

        numberArray = Array(maxNumber){ it + 1}
        val numStrArray = Array(numberArray.size){"No. ${numberArray[it]}"}
        val adapter = ArrayAdapter(context!!, R.layout.simple_text_view, numStrArray)

        placementNumberSpinner.adapter = adapter
        placementNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                placement.number = numberArray[position]
            }
        }
    }

    private fun refreshOKButton() {
        dialog?.okButton?.isEnabled = placement.category != Placement.Category.None
    }

    private fun refreshMagazineRows() {
        val visibility  = if (placement.category == Placement.Category.Magazine) View.VISIBLE else View.GONE
        magazineTypeSpinnerRow.visibility = visibility
        yearNumberRow.visibility = visibility
    }

    fun retrieveCreatedPlacement(): Placement {

        placement.name = placementNameText.text.toString()
        placement.description = placementDescText.text.toString()

        return placement
    }



}
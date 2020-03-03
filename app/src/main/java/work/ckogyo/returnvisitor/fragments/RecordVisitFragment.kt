package work.ckogyo.returnvisitor.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.record_visit_fragment.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.dialogs.PlacementDialog
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.EditPersonDialog
import work.ckogyo.returnvisitor.dialogs.InfoTagPopup
import work.ckogyo.returnvisitor.firebasedb.InfoTagCollection
import work.ckogyo.returnvisitor.models.*
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.InfoTagView
import work.ckogyo.returnvisitor.views.PersonVisitCell
import work.ckogyo.returnvisitor.views.PlacementTagView
import java.util.*
import kotlin.collections.ArrayList

class RecordVisitFragment : Fragment(),
                            DatePickerDialog.OnDateSetListener,
                            TimePickerDialog.OnTimeSetListener {

    var onBackToMapFragment: (() -> Unit)? = null

    private lateinit var infoTagJob: Deferred<ArrayList<InfoTag>>

    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    var onFinishEdit: ((Visit, EditMode, OnFinishEditParam) -> Unit)? = null

    var visit: Visit = Visit()
    set(value) {
        field = value.clone()
    }

    var mode = EditMode.Add

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.record_visit_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deleteButtonRow.visibility = if (mode == EditMode.Add) {
            View.GONE
        } else {
            View.VISIBLE
        }

        addPersonButton.setOnClickListener(this::onClickAddPerson)
        addPlacementButton.setOnClickListener(this::onClickAddPlacement)
        addInfoTagButton.setOnClickListener(this::onClickAddInfoTag)

        cancelButton.setOnClickListener(this::onCancelClicked)
        okButton.setOnClickListener(this::onOkClicked)
        deleteButton.setOnClickListener (this::onDeleteClicked)

        val handler = Handler()
        GlobalScope.launch {
            val address = requestAddressIfNeeded(visit.place, context!!)
            handler.post {
                addressText.setText(address)
            }
        }

        interestRater.refresh(visit.rating.ordinal)
        interestRater.onClickButton = {
            visit.rating = Visit.Rating.values()[it]
            refreshInterestStatementText()
        }
        refreshInterestStatementText()

        placeNameText.setText(visit.place.name)

        initDateTimeTexts()
        refreshDateTimeTexts()

        initPVCells()
        initPlacementTagViewContainer()
        initInfoTagViewContainer()

        descriptionText.setText(visit.description)

        infoTagJob = InfoTagCollection.instance.loadInLatestUseOrderAsync()
    }

    private fun initPVCells() {

        personVisitContainer.removeAllViews()
        for (pv in visit.personVisits) {
            val pvCell = PersonVisitCell(pv, PersonVisitCell.Mode.Show, context!!).also {
                it.onPersonVisitDeleted = this::onDeletePersonVisit
                it.onPersonDataEdited = this::onPersonDataEditedInPVCell
                it.onModeSwitched = this::onPersonVisitCellModeSwitched
            }
            personVisitContainer.addView(pvCell)
        }
        refreshAddPersonButtonEnability()
    }

    private fun onPersonVisitCellModeSwitched(pv: PersonVisit, mode: PersonVisitCell.Mode) {

        if (mode == PersonVisitCell.Mode.Edit) {
            for (i in 0 until personVisitContainer.childCount) {
                val pvCell = personVisitContainer.getChildAt(i) as? PersonVisitCell
                pvCell ?: continue

                if (pvCell.personVisit != pv) {
                    pvCell.forceSwitchMode(PersonVisitCell.Mode.Show)
                }
            }
        }
    }

    private fun onDeletePersonVisit(pv: PersonVisit) {
        visit.personVisits.remove(pv)
        refreshAddPersonButtonEnability()
    }

    private fun onPersonDataEditedInPVCell(person: Person) {
        refreshAddPersonButtonEnability()
    }

    private fun onCancelClicked(v: View){
        onFinishEdit?.invoke(visit, mode, OnFinishEditParam.Canceled)
        backToMapFragment()
        hideKeyboard(mainActivity!!)
    }

    private fun onOkClicked(v: View) {
        visit.description = descriptionText.text.toString()
        visit.place.name = placeNameText.text.toString()

        onFinishEdit?.invoke(visit, mode, OnFinishEditParam.Done)
        backToMapFragment()
        hideKeyboard(mainActivity!!)
    }

    private fun onDeleteClicked(v: View) {
        confirmDeleteVisit(context!!, visit) {
            onFinishEdit?.invoke(visit, mode, OnFinishEditParam.Deleted)
            backToMapFragment()
            hideKeyboard(mainActivity!!)
        }
    }

    private fun backToMapFragment() {

        mainActivity?.supportFragmentManager?.beginTransaction()
            ?.remove(this)
            ?.commit()

        onBackToMapFragment?.invoke()
    }

    private fun onClickAddPerson(v: View) {

        val person = Person()
        val pv = PersonVisit(person)
        // 初めて会えて追加した人は「会えた」
        pv.seen = true
        visit.personVisits.add(pv)

        val pvCell = PersonVisitCell(pv, PersonVisitCell.Mode.Edit, context!!).also {
            it.onPersonVisitDeleted = this::onDeletePersonVisit
            it.onPersonDataEdited = this::onPersonDataEditedInPVCell
        }

        for (i in 0 until personVisitContainer.childCount) {
            val pvCell2 = personVisitContainer.getChildAt(i) as? PersonVisitCell
            pvCell2?.forceSwitchMode(PersonVisitCell.Mode.Show)
        }

        personVisitContainer.addView(pvCell)
        refreshAddPersonButtonEnability()

        val handler = Handler()
        GlobalScope.launch {
            while (pvCell.width <= 0) {
                delay(50)
            }
            val pos = pvCell.getPositionInAncestor(recordVisitScrollView)
            handler.post {
                recordVisitScrollView.smoothScrollTo(0, pos.y - pvCell.height)
            }
        }
    }

    private fun refreshAddPersonButtonEnability() {

        var allPersonsHaveData = true

        addPersonButton.isEnabled = when {
            visit.personVisits.isEmpty() -> true
            else -> {
                for (i in 0 until personVisitContainer.childCount) {
                    val cell = personVisitContainer.getChildAt(i) as? PersonVisitCell
                    if (cell?.personVisit?.person?.hasData == false) {
                        allPersonsHaveData = false
                    }
                }
                // 既に追加されているPersonVisitCell内のPersonにデータが入力されていれば新しいPersonを追加できる。
                allPersonsHaveData
            }
        }

    }

    private fun initDateTimeTexts() {

        dateText.setOnClick {
            showDatePicker()
        }

        timeText.setOnClick {
            showTimePicker()
        }
    }

    private fun showDatePicker() {
        context?:return

        DatePickerDialog(context!!,
            this,
            visit.dateTime.get(Calendar.YEAR),
            visit.dateTime.get(Calendar.MONTH),
            visit.dateTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDateSet(p0: DatePicker?, p1: Int, p2: Int, p3: Int) {
        visit.dateTime.set(Calendar.YEAR, p1)
        visit.dateTime.set(Calendar.MONTH, p2)
        visit.dateTime.set(Calendar.DAY_OF_MONTH, p3)

        refreshDateTimeTexts()
    }

    private fun showTimePicker() {
        context?:return

        TimePickerDialog(context!!,
            this,
            visit.dateTime.get(Calendar.HOUR_OF_DAY),
            visit.dateTime.get(Calendar.MINUTE),
            true).show()
    }

    override fun onTimeSet(p0: TimePicker?, p1: Int, p2: Int) {
        visit.dateTime.set(Calendar.HOUR_OF_DAY, p1)
        visit.dateTime.set(Calendar.MINUTE, p2)

        refreshDateTimeTexts()
    }

    private fun refreshDateTimeTexts() {

        val dateFormat = android.text.format.DateFormat.getDateFormat(context)
        dateText.text = dateFormat.format(visit.dateTime.time)

        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
        timeText.text = timeFormat.format(visit.dateTime.time)
    }

    private fun refreshInterestStatementText() {
        val raterArray = resources.getStringArray(R.array.raterArray)
        interestStatementText.text = raterArray[visit.rating.ordinal]
    }

    private fun onClickAddPlacement(v: View) {
        val plcDialog = PlacementDialog().also {
            it.onAddPlacement = this::onAddPlacementInPlcDialog
            it.onPlacementDeleted = this::onPlacementDeletedInDialog
        }

        mainActivity?.showDialog(plcDialog)
    }

    private fun onPlacementDeletedInDialog(plc: Placement) {

        for (tagView in placementTagViewContainer.tagViews) {
            if ((tagView as PlacementTagView).placement == plc) {
                placementTagViewContainer.removeTagView(tagView)
            }
        }
    }

    private fun onAddPlacementInPlcDialog(plc: Placement) {
        visit.placements.add(plc)
        val plcTagView = PlacementTagView(context!!, plc)
        placementTagViewContainer.also {
            it.onTagViewRemoved = { tagView ->
                visit.placements.remove((tagView as PlacementTagView).placement)
            }
        }.addTagView(plcTagView)
    }

    private fun initPlacementTagViewContainer() {
        val tagViews = ArrayList<PlacementTagView>()
        for (plc in visit.placements) {
            tagViews.add(PlacementTagView(context!!, plc))
        }
        placementTagViewContainer.addTagViews(tagViews)
    }

    private fun onClickAddInfoTag(v: View) {
//        val tagDialog = InfoTagDialog(visit).also {
//            it.onInfoTagSelected = this::onAddTagInInfoTagDialog
//            it.onInfoTagDeleted = this::onInfoTagDeletedInDialog
//        }
//        mainActivity?.showDialog(tagDialog)

        val tagPopup = InfoTagPopup(infoTagViewContainer, R.id.recordVisitFrame, visit, infoTagJob).also {
            it.onInfoTagSelected = this::onAddTagInInfoTagDialog
            it.onInfoTagDeleted = this::onInfoTagDeletedInDialog
        }
        tagPopup.show(childFragmentManager)
    }

    private fun onInfoTagDeletedInDialog(tag: InfoTag) {

        for (tagView in infoTagViewContainer.tagViews) {
            if ((tagView as InfoTagView).tag == tag) {
                infoTagViewContainer.removeTagView(tagView)
            }
        }
    }

    private fun onAddTagInInfoTagDialog(tag: InfoTag) {
        visit.infoTags.add(tag)
        val tagView = InfoTagView(context!!, tag)
        infoTagViewContainer.also {
            it.onTagViewRemoved = { tagView ->
                visit.infoTags.remove((tagView as InfoTagView).tag)
            }
        }.addTagView(tagView)
    }

    private fun initInfoTagViewContainer() {
        val tagViews = ArrayList<InfoTagView>()
        for (tag in visit.infoTags) {
            tagViews.add(InfoTagView(context!!, tag))
        }
        infoTagViewContainer.addTagViews(tagViews)
    }
}
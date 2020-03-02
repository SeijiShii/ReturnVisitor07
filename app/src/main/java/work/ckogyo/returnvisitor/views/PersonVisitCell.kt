package work.ckogyo.returnvisitor.views

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import kotlinx.android.synthetic.main.person_visit_cell.view.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.PersonVisit
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class PersonVisitCell(val personVisit: PersonVisit, var mode: Mode, context: Context) : HeightAnimationView(context) {

    enum class Mode {
        Show,
        Edit
    }

    private val showModeHeight: Int
        get() = context.toDP(82)

    private val editModeHeight: Int
        get() = context.toDP(205)

    private val switchRowHeight: Int
        get() = context.toDP(42)

    private val mainActivity: MainActivity?
    get() = context as? MainActivity

    override fun onUpdateAnimation(animatedHeight: Int) {
        personCellFrame.layoutParams.height = animatedHeight - switchRowHeight
    }

    var onPersonVisitDeleted: ((PersonVisit) -> Unit)? = null
    var onModeSwitched: ((PersonVisit, Mode) -> Unit)? = null
    var onPersonDataEdited: ((Person) -> Unit)? = null

    init {
        View.inflate(context, R.layout.person_visit_cell, this)

        seenSwitch.isChecked = personVisit.seen
        rvSwitch.isChecked = personVisit.isRv
        studySwitch.isChecked = personVisit.isStudy

        seenSwitch.setOnCheckedChangeListener { _, b ->
            personVisit.seen = b
        }

        rvSwitch.setOnCheckedChangeListener { _, b ->
            personVisit.isRv = b
        }

        studySwitch.setOnCheckedChangeListener{ _, b ->
            personVisit.isStudy = b
        }

        editPersonButton.setOnClickListener {
            mode = Mode.Edit
            animateHeight()
            onModeSwitched?.invoke(personVisit, mode)
        }

        fixPersonButton.setOnClickListener {
            refreshPersonTextForShow()
            mode = Mode.Show
            animateHeight()
            onModeSwitched?.invoke(personVisit, mode)
        }

        personVisitMenuButton.setOnClick { showPopupMenu() }

        initNameText()
        initSexRadioGroup()
        initAgeSpinner()
        initDescriptionText()

        refreshMode()

        refreshPersonTextForShow()
        refreshUIsForEdit()
        refreshSwitches()
    }

    private fun refreshMode() {
        personCellForShow.visibility = if (mode == Mode.Edit) {
            View.GONE
        } else {
            View.VISIBLE
        }

        personCellForEdit.visibility = if (mode == Mode.Edit) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun refreshPersonTextForShow() {
        personText.text = personVisit.person.toString(context)
    }

    private fun refreshUIsForEdit() {
        personNameText.setText(personVisit.person.name)
        personDescriptionText.setText(personVisit.person.description)

        maleRadioButton.isSelected = personVisit.person.sex == Person.Sex.Male
        femaleRadioButton.isSelected = personVisit.person.sex == Person.Sex.Female

        ageSpinner.setSelection(personVisit.person.age.ordinal)
    }

    private fun refreshSwitches() {
        seenSwitch.isChecked = personVisit.seen
        rvSwitch.isChecked = personVisit.isRv
        studySwitch.isChecked = personVisit.isStudy
    }

    private fun animateHeight() {

        val targetHeight = if (mode == Mode.Edit) {
            editModeHeight
        } else {
            showModeHeight
        }

        animateHeight(targetHeight){
            personCellForEdit.fadeVisibility(mode == Mode.Edit)
            personCellForShow.fadeVisibility(mode == Mode.Show)
        }
    }

    private var isPopupShowing = false

    private fun showPopupMenu() {

        if (isPopupShowing) return

        isPopupShowing = true

        val popup = PopupMenu(context, personVisitMenuButton)
        popup.menuInflater.inflate(R.menu.person_visit_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.delete_person -> {
                    showDeleteConfirm()
                }
            }
            return@setOnMenuItemClickListener true
        }

        popup.setOnDismissListener {
            isPopupShowing = false
        }
        popup.show()
    }

    private fun showDeleteConfirm() {

        AlertDialog.Builder(context)
            .setTitle(R.string.delete_person)
            .setMessage(context.getString(R.string.delete_person_confirm, personVisit.person.toString(context)))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete){_, _ ->
                onDeletePersonConfirmed()
            }.create().show()
    }

    private fun onDeletePersonConfirmed() {

        collapseToHeight0 {
            (parent as? ViewGroup)?.removeView(this)
        }
        onPersonVisitDeleted?.invoke(personVisit)
    }

    private fun initNameText() {
        personNameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                personVisit.person.name = p0.toString()
                onPersonDataEdited?.invoke(personVisit.person)
            }
        })
    }

    private fun initSexRadioGroup() {
        when(personVisit.person.sex) {
            Person.Sex.Male -> maleRadioButton.isChecked = true
            Person.Sex.Female -> femaleRadioButton.isChecked = true
            else -> {
                maleRadioButton.isChecked = false
                femaleRadioButton.isChecked = false
            }
        }

        maleRadioButton.setOnCheckedChangeListener { _, b ->
            if (b){
                personVisit.person.sex = Person.Sex.Male
                onPersonDataEdited?.invoke(personVisit.person)
            }
        }

        femaleRadioButton.setOnCheckedChangeListener { _, b ->
            if (b) {
                personVisit.person.sex = Person.Sex.Female
                onPersonDataEdited?.invoke(personVisit.person)
            }
        }
    }

    private fun initAgeSpinner() {

        context?:return

        val adapter = ArrayAdapter.createFromResource(context!!, R.array.ageArray, R.layout.simple_text_view)
        adapter.setDropDownViewResource(R.layout.simple_text_view)
        ageSpinner.adapter = adapter

        ageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                personVisit.person.age = Person.Age.values()[p2]
                onPersonDataEdited?.invoke(personVisit.person)
            }
        }
    }

    private fun initDescriptionText() {
        personDescriptionText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                personVisit.person.description = p0.toString()
            }
        })
    }

    fun forceSwitchMode(mode: Mode) {
        this.mode = mode
        refreshPersonTextForShow()
        refreshUIsForEdit()
        animateHeight()
    }
}
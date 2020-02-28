package work.ckogyo.returnvisitor.views

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.PopupMenu
import kotlinx.android.synthetic.main.person_visit_cell.view.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.EditPersonDialog
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.PersonVisit
import work.ckogyo.returnvisitor.utils.EditMode
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class PersonVisitCell(private val personVisit: PersonVisit, context: Context) : HeightAnimationView(context) {

    enum class Mode {
        Show,
        Edit
    }

    var mode = Mode.Edit

    private val mainActivity: MainActivity?
    get() = context as? MainActivity



    var onDeletePersonVisit: ((PersonVisit) -> Unit)? = null

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
        }

        fixPersonButton.setOnClickListener {
            mode = Mode.Show
            animateHeight()
        }

        personVisitMenuButton.setOnClick { showPopupMenu() }

        initNameText()
        initSexRadioGroup()
        initAgeSpinner()
        initDescriptionText()

        refreshMode()
        refreshPersonTextForShow()
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

    private fun animateHeight() {

        val targetHeight = if (mode == Mode.Edit) {
            context.toDP(202)
        } else {
            context.toDP(82)
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
                R.id.edit_person -> {
                    showEditPersonDialog()
                }
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

    private fun showEditPersonDialog() {

        val editPersonDialog = EditPersonDialog()
        editPersonDialog.mode = EditMode.Edit
        editPersonDialog.onOk = this::onOKInEditPersonDialog
        editPersonDialog.person = personVisit.person
        mainActivity?.showDialog(editPersonDialog)
    }

    private fun onOKInEditPersonDialog(person: Person) {
        personVisit.person = person
        personText.text = personVisit.person.toString(context)
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

        (parent as? ViewGroup)?.removeView(this)

        onDeletePersonVisit?.invoke(personVisit)
    }

    private fun initNameText() {
        personNameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                personVisit.person.name = p0.toString()
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
            }
        }

        femaleRadioButton.setOnCheckedChangeListener { _, b ->
            if (b) {
                personVisit.person.sex = Person.Sex.Female
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
}
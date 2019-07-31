package work.ckogyo.returnvisitor.dialogs

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.edit_person_dialog.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.utils.EditMode

class EditPersonDialog :DialogFrameFragment() {

    var onOk: ((person:Person) -> Unit)? = null

    var person: Person = Person()
    set(value) {
        field = value.clone()
    }



    var mode = EditMode.Add

    override fun onOkClick() {
        onOk?.invoke(person)
    }

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.edit_person_dialog, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        allowScroll = true

        super.onViewCreated(view, savedInstanceState)

        initAgeSpinner()

        when(mode) {
            EditMode.Add -> {
                setTitle(R.string.add_person)
            }
            EditMode.Edit -> {
                setTitle(R.string.edit_person)

                personNameText.setText(person.name)
                sexRadioGroup.check(person.sex.ordinal - 1)
                ageSpinner.setSelection(person.age.ordinal - 1)
                personDescriptionText.setText(person.description)

            }
        }

        when(person.sex) {
            Person.Sex.Male -> maleRadioButton.isChecked = true
            Person.Sex.Female -> femaleRadioButton.isChecked = true
            else -> {
                maleRadioButton.isChecked = false
                femaleRadioButton.isChecked = false
            }
        }

        if (person.age != Person.Age.Unknown) {
            ageSpinner.setSelection(person.age.ordinal - 1)
        }

        personNameText.addTextChangedListener(object :TextWatcher{
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                person.name = p0.toString()
                refreshOKButton()
            }
        })

        maleRadioButton.setOnCheckedChangeListener { _, b ->
            if (b){
                person.sex = Person.Sex.Male
                refreshOKButton()
            }
        }

        femaleRadioButton.setOnCheckedChangeListener { _, b ->
            if (b) {
                person.sex = Person.Sex.Female
                refreshOKButton()
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
                person.age = Person.Age.values()[p2 + 1]
                refreshOKButton()
            }
        }
    }

    private fun refreshOKButton() {
        isOKButtonEnabled = (maleRadioButton.isChecked || femaleRadioButton.isChecked)
                && ageSpinner.selectedItemPosition >= 0
    }
}
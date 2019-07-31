package work.ckogyo.returnvisitor.views

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import kotlinx.android.synthetic.main.person_visit_cell.view.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.EditPersonDialog
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.PersonVisit
import work.ckogyo.returnvisitor.utils.EditMode
import work.ckogyo.returnvisitor.utils.setOnClick

class PersonVisitCell(private val personVisit: PersonVisit, context: Context) : FrameLayout(context) {

    private val mainActivity: MainActivity?
    get() = context as? MainActivity

    var onDeletePersonVisit: ((PersonVisit) -> Unit)? = null

    init {
        View.inflate(context, R.layout.person_visit_cell, this)

        personText.text = personVisit.person.toString(context)

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

        personVisitMenuButton.setOnClick { showPopupMenu() }
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
}
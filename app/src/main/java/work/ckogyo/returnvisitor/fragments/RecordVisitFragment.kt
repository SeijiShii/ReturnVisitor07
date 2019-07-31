package work.ckogyo.returnvisitor.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.record_visit_fragment.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.EditPersonDialog
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.PersonVisit
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.EditMode
import work.ckogyo.returnvisitor.utils.OnFinishEditParam
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.views.PersonVisitCell
import java.text.SimpleDateFormat
import java.time.Year
import java.util.*
import kotlin.concurrent.thread

class RecordVisitFragment : Fragment(),
                            DatePickerDialog.OnDateSetListener,
                            TimePickerDialog.OnTimeSetListener {

    private val mainActivity: MainActivity?
    get() = context as? MainActivity

    var onFinishEdit: ((Visit, OnFinishEditParam) -> Unit)? = null

    var visit: Visit = Visit()
    set(value) {
        field = value.clone()
    }

//    var place: Place = Place()
//    set(value) {
//        field = value.clone()
//    }

    var mode = EditMode.Add

    private val interestTextIds = arrayOf(
        R.string.none,
        R.string.negative,
        R.string.for_next_covering,
        R.string.busy,
        R.string.fair,
        R.string.interested,
        R.string.strongly_interested
    )


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.record_visit_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addPersonButton.setOnClickListener {
            val addPersonDialog = EditPersonDialog()
            addPersonDialog.mode = EditMode.Add
            addPersonDialog.onOk = this::onOkInAddPersonDialog
            mainActivity?.showDialog(addPersonDialog)
        }

        deleteButtonRow.visibility = if (mode == EditMode.Add) {
            View.GONE
        } else {
            View.VISIBLE
        }

        cancelButton.setOnClickListener {
            onFinishEdit?.invoke(visit, OnFinishEditParam.Canceled)
            mainActivity?.supportFragmentManager?.popBackStack()
        }

        okButton.setOnClickListener {
            onFinishEdit?.invoke(visit, OnFinishEditParam.Done)
            mainActivity?.supportFragmentManager?.popBackStack()
        }

        deleteButton.setOnClickListener {
            // TODO: deleteButton.setOnClickListener
            // TODO: Delete Confirm
            onFinishEdit?.invoke(visit, OnFinishEditParam.Deleted)
            mainActivity?.supportFragmentManager?.popBackStack()
        }

        requestAddressIfNeeded()
        interestRater.onClickButton = {
            visit.rating = Visit.Rating.values()[it]
            interestStatementText.setText(interestTextIds[it])
        }

        initDateTimeTexts()
        refreshDateTimeTexts()
    }

    private fun onOkInAddPersonDialog(person: Person) {

        val pv = PersonVisit(person)
        visit.personVisits.add(pv)

        val pvCell = PersonVisitCell(pv, context!!)
        personVisitContainer.addView(pvCell)
    }

    private fun requestAddressIfNeeded() {

        val handler = Handler()
        thread {
            if(visit.place.address.isEmpty()) {
                val geocoder = Geocoder(context!!, Locale.getDefault())
                val addressList = geocoder.getFromLocation(visit.place.latLng.latitude, visit.place.latLng.longitude, 1)
                if (addressList.isNotEmpty()) {
                    visit.place.address = addressList[0].getAddressLine(0)
                }
            }
            handler.post {
                addressText.setText(visit.place.address)
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

}
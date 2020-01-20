package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.housing_complex_fragment.*
import kotlinx.android.synthetic.main.place_dialog.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.utils.OnFinishEditParam
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.requestAddressIfNeeded
import work.ckogyo.returnvisitor.utils.setOnClick

class HousingComplexFragment : Fragment() {

    var onOk: ((hComplex: Place) -> Unit)? = null
    var onDeleted: ((hComplex: Place) -> Unit)? = null

    var hComplex = Place()
    set(value) {
        field = value.clone()
    }

    private val mainActivity: MainActivity?
        get() = context as? MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.housing_complex_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val handler = Handler()
        GlobalScope.launch {
            val address = requestAddressIfNeeded(hComplex, context!!)
            handler.post {
                housingComplexAddressText.setText(address)
            }
        }

        okButton.setOnClickListener {
            mainActivity?.supportFragmentManager?.popBackStack()
            GlobalScope.launch {
                PlaceCollection.instance.set(hComplex)
            }
            onOk?.invoke(hComplex)
        }

        cancelButton.setOnClickListener {
            mainActivity?.supportFragmentManager?.popBackStack()
        }

        housingComplexMenuButton.setOnClick {
            showMenuPopup()
        }

        loadingRoomsProgressFrame.fadeVisibility(true)
        noRoomRegisteredFrame.fadeVisibility(false)
        roomListFrame.fadeVisibility(false)
    }

    private fun showMenuPopup() {

        val popup = PopupMenu(context, housingComplexMenuButton)
        popup.menuInflater.inflate(R.menu.housing_complex_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.delete_place -> confirmDeletePlace()
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun confirmDeletePlace() {

        AlertDialog.Builder(context!!).setTitle(R.string.delete_housing_complex)
            .setMessage(R.string.delete_housing_complex_message)
            .setNegativeButton(R.string.cancel, null).
                setPositiveButton(R.string.delete){_, _ ->

                    GlobalScope.launch {
                        PlaceCollection.instance.delete(hComplex)
                        // TODO 部屋データと部屋への訪問データを削除する
                    }
                    onDeleted?.invoke(hComplex)
                    mainActivity?.supportFragmentManager?.popBackStack()
                }.create().show()
    }


}
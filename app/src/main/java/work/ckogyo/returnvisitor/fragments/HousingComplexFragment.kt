package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.housing_complex_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.requestAddressIfNeeded

class HousingComplexFragment : Fragment() {

    var onOk: ((hComplex: Place) -> Unit)? = null

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

        loadingRoomsProgressFrame.fadeVisibility(true)
        noRoomRegisteredFrame.fadeVisibility(false)
        roomListFrame.fadeVisibility(false)
    }

}
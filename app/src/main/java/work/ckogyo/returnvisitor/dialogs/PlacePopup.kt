package work.ckogyo.returnvisitor.dialogs

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.place_popup.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place

class PlacePopup(context: Context, place: Place) : LinearLayout(context) {

    var onClickButton: ((place: Place) -> Unit)? = null
    var onCancel: ((place: Place) -> Unit)? = null

    init {
        View.inflate(context, R.layout.place_popup, this)

        popupOverlay.setOnTouchListener { _, _ ->
            remove()
            onCancel?.invoke(place)
            return@setOnTouchListener true
        }

        popupFrame.setOnTouchListener{_, _ ->
            return@setOnTouchListener true
        }

        recordHouseButton.setOnClickListener {
            place.category = Place.Category.House
            onClickButton?.invoke(place)
            remove()
        }

        recordHousingComplexButton.setOnClickListener {
            place.category = Place.Category.HousingComplex
            onClickButton?.invoke(place)
            remove()
        }

        recordPlaceButton.setOnClickListener {
            place.category = Place.Category.Place
            onClickButton?.invoke(place)
            remove()
        }

        closeButton.setOnClickListener {
            onCancel?.invoke(place)
            remove()
        }

    }

    private fun remove() {
        (parent as? ViewGroup)?.removeView(this)
    }
}
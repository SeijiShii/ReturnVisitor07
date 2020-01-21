package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
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
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.RoomCell

class HousingComplexFragment : Fragment() {

    var onOk: ((hComplex: Place) -> Unit)? = null
    var onDeleted: ((hComplex: Place) -> Unit)? = null

    private val rooms = ArrayList<Place>()
    private var isLoadingRooms = false

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
                PlaceCollection.instance.saveAsync(hComplex).await()
                onOk?.invoke(hComplex)
            }
        }

        cancelButton.setOnClickListener {
            mainActivity?.supportFragmentManager?.popBackStack()
        }

        housingComplexMenuButton.setOnClick {
            showMenuPopup()
        }

        searchOrAddRoomNumText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshAddRoomButton()
            }
        })

        addInputRoomNumButton.setOnClickListener {
            onClickAddNewRoom()
        }

        refreshAddRoomButton()

        loadingRoomsProgressFrame.fadeVisibility(true)
        noRoomRegisteredFrame.alpha = 0f
        roomListFrame.alpha = 0f

        GlobalScope.launch {

            isLoadingRooms = true
            refreshAddRoomButton()

            val loadedRooms = PlaceCollection.instance.loadRoomsByParentId(hComplex.id)
            rooms.clear()
            rooms.addAll(loadedRooms)
            handler.post {
                refreshRoomList()
                isLoadingRooms = false
                refreshAddRoomButton()
                loadingRoomsProgressFrame.fadeVisibility(false)
            }
        }
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
                        PlaceCollection.instance.deleteAsync(hComplex).await()
                        onDeleted?.invoke(hComplex)
                    }
                    mainActivity?.supportFragmentManager?.popBackStack()
                }.create().show()
    }

    /**
     * addInputRoomNumButtonの更新
     * 活性条件
     *  searchOrAddRoomNumTextが空欄ではない
     *  検索に完全一致する部屋がない
     *  読み込み中ではない
     */
    private fun refreshAddRoomButton() {
        // TODO: 条件の追加
        addInputRoomNumButton.isEnabled = searchOrAddRoomNumText.text.isNotEmpty()
                                            && !isLoadingRooms
    }

    private fun onClickAddNewRoom() {

        val room = Place()
        room.category = Place.Category.Room
        room.parentId = hComplex.id
        room.address = hComplex.address
        room.latLng = hComplex.latLng
        room.name = searchOrAddRoomNumText.text.toString()

        mainActivity?.showRecordVisitFragmentForNew(room, this::onFinishEditVisit)
    }

    private fun onFinishEditVisit(visit: Visit, mode: EditMode, param: OnFinishEditParam) {

    }


    private fun refreshRoomList(searchWord: String = "") {
        roomListFrame.removeAllViews()

        val roomsToShow = ArrayList<Place>()
        if (searchWord.isBlank()) {
            roomsToShow.addAll(rooms)
        } else {
            for (room in rooms) {
                if (room.name == searchWord) {
                    roomsToShow.add(room)
                }
            }
        }

        if (rooms.isEmpty()) {
            noRoomRegisteredFrame.fadeVisibility(true)
            roomListFrame.fadeVisibility(false)
        } else {
            for (room in roomsToShow) {
                val cell = RoomCell(context!!, room)
                roomListFrame.addView(cell)
            }
            noRoomRegisteredFrame.fadeVisibility(false)
            roomListFrame.fadeVisibility(true)
        }
    }


}
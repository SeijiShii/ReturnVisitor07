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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.PlaceDialog
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.RoomCell

class HousingComplexFragment : Fragment() {

    var onOk: ((hComplex: Place) -> Unit)? = null
    var onClose: ((hComplex: Place, isNewHC: Boolean) -> Unit)? = null
    var onDeleted: ((hComplex: Place) -> Unit)? = null

    var isNewHC = false

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

        okButton.setOnClickListener(this::onClickOk)
        closeButton.setOnClickListener(this::onClickClose)

        housingComplexMenuButton.setOnClick {
            showMenuPopup()
        }

        searchOrAddRoomNumText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s != null) {
                    refreshRoomList()
                }
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

        housingComplexNameText.setText(hComplex.name)

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

    private fun onClickOk(v: View) {

        mainActivity?.switchProgressOverlay(true)
        hComplex.name = housingComplexNameText.text.toString()
        val handler = Handler()

        mainActivity?.supportFragmentManager?.popBackStack()

        if(mainActivity != null) {
            hideKeyboard(mainActivity!!)
        }

        GlobalScope.launch {
            PlaceCollection.instance.saveAsync(hComplex).await()
            hComplex.refreshRatingByVisitsAsync().await()
            onOk?.invoke(hComplex)
        }
    }

    private fun onClickClose(v: View) {
        mainActivity?.supportFragmentManager?.popBackStack()
        onClose?.invoke(hComplex, isNewHC)
        hideKeyboard(mainActivity!!)
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

    /**
     * 検索に完全一致する部屋が存在すればそれを編集する。
     * なければその名前で新規作成する。
     */
    private fun onClickAddNewRoom() {

        val searchText = searchOrAddRoomNumText.text.toString()
        searchOrAddRoomNumText.setText("")
        var roomForSearch: Place? = null

        var existsCompleteMatch = false
        for (room in rooms) {
            if (room.name == searchText) {
                roomForSearch = room
                existsCompleteMatch = true
            }
        }

        if (existsCompleteMatch) {
            roomForSearch ?: return
            showPlaceDialogForRoom(roomForSearch)
        } else {
            val room = Place()
            room.category = Place.Category.Room
            room.parentId = hComplex.id
            room.address = hComplex.address
            room.latLng = hComplex.latLng
            room.name = searchText
            mainActivity?.showRecordVisitFragmentForNew(room, this::onFinishEditVisit)
        }
    }

    private fun onFinishEditVisit(visit: Visit, mode: EditMode, param: OnFinishEditParam) {

        val handler = Handler()

        when(param) {
            OnFinishEditParam.Canceled -> {}
            OnFinishEditParam.Done -> {

                mainActivity?.switchProgressOverlay(true, getString(R.string.updating))
                GlobalScope.launch {

                    VisitCollection.instance.saveVisitAsync(visit).await()
                    PlaceCollection.instance.saveAsync(visit.place).await()

                    rooms.remove(visit.place)
                    rooms.add(visit.place)
                    rooms.sortBy { r -> r.name }

                    handler.post{
                        refreshRoomList()
                        mainActivity?.switchProgressOverlay(false)
                    }

                    // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
                    TimeCountIntentService.saveWorkIfActive()
                }
            }
            OnFinishEditParam.Deleted -> {

                GlobalScope.launch {
//                    VisitCollection.instance.deleteAsync(visit).await()
//                    handler.post {
//                    }
                }
            }
        }
    }


    private fun refreshRoomList() {

        val searchWord = searchOrAddRoomNumText.text.toString()

        roomListFrame.removeAllViews()

        val roomsToShow = ArrayList<Place>()
        if (searchWord.isBlank()) {
            roomsToShow.addAll(rooms)
        } else {
            for (room in rooms) {
                if (room.name.contains(searchWord)) {
                    roomsToShow.add(room)
                }
            }
        }

        if (rooms.isEmpty()) {
            noRoomRegisteredFrame.fadeVisibility(true)
            roomListFrame.fadeVisibility(false)
        } else {
            for (room in roomsToShow) {
                val cell = RoomCell(context!!, room).apply{
                    onClickShowRoom = this@HousingComplexFragment::showPlaceDialogForRoom
                    onDeleted = {
                        rooms.remove(it)
                        refreshRoomList()
                    }
                }
                roomListFrame.addView(cell)
            }
            noRoomRegisteredFrame.fadeVisibility(false)
            roomListFrame.fadeVisibility(true)
        }
    }

    private fun showPlaceDialogForRoom(room: Place) {
        val dialog = PlaceDialog(room).apply {

        }
        mainActivity?.showDialog(dialog)
    }


}
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
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.housing_complex_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.PlaceDialog
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.views.RoomCell

class HousingComplexFragment : Fragment() {

    var onBackToMapFragment: (() -> Unit)? = null

    var onClose: ((hComplex: Place, isDeleted: Boolean) -> Unit)? = null
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
                housingComplexAddressText?.setText(address)
            }
        }

//        okButton.setOnClickListener(this::onClickOk)
        closeButton.setOnClickListener(this::onClickClose)

        housingComplexMenuButton.setOnClick {
            showMenuPopup()
        }

        searchOrAddRoomNumText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s != null) {
                    refreshRoomListView()
                }
                refreshAddRoomButton()
            }
        })

        addInputRoomNumButton.setOnClickListener {
            onClickAddNewRoom()
        }

        refreshAddRoomButton()

        loadingRoomsProgressFrame.fadeVisibility(true, addTouchBlockerOnFadeIn = true)
        roomListView.alpha = 0f
        searchOrAddRow.isEnabled = false

        housingComplexNameText.setText(hComplex.name)

        GlobalScope.launch {

            isLoadingRooms = true
            refreshAddRoomButton()

            val loadedRooms = FirebaseDB.instance.loadRoomsByParentId(hComplex.id)
            rooms.clear()
            rooms.addAll(loadedRooms)
            handler.post {
                searchOrAddRow.isEnabled = true
                refreshRoomListView()
                isLoadingRooms = false
                refreshAddRoomButton()
                loadingRoomsProgressFrame?.fadeVisibility(false, addTouchBlockerOnFadeIn = true)
            }
        }

        housingComplexAddressRow.extractHeight = context!!.toDP(70)
        housingComplexAddressRow.isExtracted = false
        openAddressRowButton.setOnClick {
            housingComplexAddressRow.isExtracted = !housingComplexAddressRow.isExtracted
            housingComplexAddressRow.animateHeight()
        }
    }

    private fun onClickClose(v: View) {
        hComplex.name = housingComplexNameText.text.toString()

        backToMapFragment()

        if(mainActivity != null) {
            hideKeyboard(mainActivity!!)
        }

        GlobalScope.launch {

            if (isNewHC && rooms.isEmpty()) {
                FirebaseDB.instance.deletePlaceAsync(hComplex).await()
                onClose?.invoke(hComplex, true)
            } else {
                FirebaseDB.instance.savePlaceAsync(hComplex).await()
                onClose?.invoke(hComplex, false)
            }
        }
    }

    private fun showMenuPopup() {

        val popup = PopupMenu(context, housingComplexMenuButton)
        popup.menuInflater.inflate(R.menu.housing_complex_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.delete_place -> confirmDeleteHC()
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun confirmDeleteHC() {

        AlertDialog.Builder(context!!).setTitle(R.string.delete_housing_complex)
            .setMessage(R.string.delete_housing_complex_message)
            .setNegativeButton(R.string.cancel, null).
                setPositiveButton(R.string.delete){_, _ ->

                    GlobalScope.launch {
                        FirebaseDB.instance.deletePlaceAsync(hComplex).await()
                        onDeleted?.invoke(hComplex)
                    }
                    backToMapFragment()
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
            val room = Place().also {
                it.category = Place.Category.Room
                it.parentId = hComplex.id
                it.parentName = hComplex.name
                it.address = hComplex.address
                it.latLng = hComplex.latLng
                it.name = searchText
            }
            mainActivity?.showRecordVisitFragmentForNew(room, this::onFinishEditVisit)
        }
    }

    private fun onFinishEditVisit(visit: Visit, mode: EditMode, param: OnFinishEditParam) {

        val handler = Handler()
        loadingRoomsProgressFrame.fadeVisibility(true)

        when(param) {
            OnFinishEditParam.Canceled -> {
                handler.post {
                    loadingRoomsProgressFrame?.fadeVisibility(false)
                }
            }
            OnFinishEditParam.Done -> {

//                if (mode != EditMode.Add) return

                GlobalScope.launch {

                    FirebaseDB.instance.saveVisitAsync(visit).await()
                    FirebaseDB.instance.savePlaceAsync(hComplex).await()

                    if (rooms.contains(visit.place)) {
                        // HousingComplexDialog -> PlaceDialog -> 訪問を記録　で帰ってきたパターン
                        val pos = getPositionByRoom(visit.place)
                        if (pos >= 0) {
                            handler.post {
                                val cell = roomListView?.findViewHolderForAdapterPosition(pos)?.itemView as? RoomCell
                                cell?.refresh(visit.place)
                                loadingRoomsProgressFrame?.fadeVisibility(false)
                            }
                        }
                    } else {
                        // HousingComplexDialog -> 部屋の追加 -> 訪問を記録 で帰ってきたパターン
                        rooms.add(visit.place)
                        rooms.sortBy { r -> r.name }

                        handler.post {
                            if (roomListView?.adapter == null) {
                                refreshRoomListView()
                            } else {
                                refreshRoomsToShow()
                                val pos = getPositionByRoom(visit.place)

                                if (pos >= 0) {
                                    roomListView?.adapter?.notifyItemInserted(pos)
                                    roomListView?.smoothScrollToPosition(pos)
                                }
                            }
                            loadingRoomsProgressFrame?.fadeVisibility(false)
                        }
                    }

                    // Workは30秒に一度の更新なのでVisitの更新に合わせてWorkも更新しないと、VisitがWork内に収まらないことがある
                    TimeCountIntentService.saveWorkIfActive()
                }
            }
            OnFinishEditParam.Deleted -> {

            }
        }
    }

    private fun refreshRoomListView() {

        refreshRoomsToShow()

        if (roomsToShow.isEmpty()) {
            noRoomFrame?.fadeVisibility(true)
            roomListView?.fadeVisibility(false)
        } else {
            roomListView?.adapter = RoomListAdapter()
            noRoomFrame?.fadeVisibility(false)
            roomListView?.fadeVisibility(true)
        }
    }

    private val roomsToShow = ArrayList<Place>()

    private fun refreshRoomsToShow() {
        val searchWord = searchOrAddRoomNumText?.text.toString()
        roomsToShow.clear()
        if (searchWord.isBlank()) {
            roomsToShow.addAll(rooms)
        } else {
            for (room in rooms) {
                if (room.name.contains(searchWord)) {
                    roomsToShow.add(room)
                }
            }
        }
    }

    private fun showPlaceDialogForRoom(room: Place) {

        mainActivity ?: return

        val dialog = PlaceDialog(room).apply {
            onEditVisitInvoked = {
                mainActivity?.showRecordVisitFragmentForEdit(it, this@HousingComplexFragment::onFinishEditVisit)
            }
            onRecordNewVisitInvoked = {
                mainActivity?.showRecordVisitFragmentForNew(it, this@HousingComplexFragment::onFinishEditVisit)
            }
            onClose = this@HousingComplexFragment::onClosePlaceDialogForRoom
            onRefreshPlace = this@HousingComplexFragment::onRefreshRoomInPlaceDialog
            onShowInWideMap = this@HousingComplexFragment::onShowInWideMapInVisitDetail
        }
        mainActivity!!.showDialog(dialog)
        hideKeyboard(mainActivity!!)
    }

    private fun onShowInWideMapInVisitDetail(visit: Visit) {
        backToMapFragment()
        mainActivity?.mapFragment?.animateToLatLng(visit.place.latLng)
    }

    private fun onRefreshRoomInPlaceDialog(room: Place) {

        loadingRoomsProgressFrame.fadeVisibility(true)
        val handler = Handler()

        GlobalScope.launch {

            FirebaseDB.instance.savePlaceAsync(room).await()

            handler.post {
                val pos = getPositionByRoom(room)

                if (pos >= 0) {
                    val cell = roomListView?.findViewHolderForAdapterPosition(pos)?.itemView as? RoomCell
                    cell?.refresh(room)
                }
                loadingRoomsProgressFrame?.fadeVisibility(false)
            }
        }
    }

    private fun onClosePlaceDialogForRoom(room: Place, param: OnFinishEditParam) {

        // PlaceDialog内でRoomを削除した場合こちらに帰ってくる
        when(param) {
            OnFinishEditParam.Deleted -> {
                val pos = getPositionByRoom(room)

                rooms.remove(room)
                roomsToShow.remove(room)

                if (pos >= 0) {
                    roomListView.adapter?.notifyItemRemoved(pos)
                }

                GlobalScope.launch {
                    FirebaseDB.instance.deletePlaceAsync(room)
                }
            }
        }
    }

    private fun onDeleteRoomConfirmed(room: Place) {

        val pos = getPositionByRoom(room)

        roomsToShow.remove(room)
        rooms.remove(room)

        GlobalScope.launch {
            FirebaseDB.instance.deletePlaceAsync(room)
        }

        if (pos >= 0) {
            roomListView.adapter?.notifyItemRemoved(pos)
        }
    }

    private fun getPositionByRoom(room: Place): Int {

        for (i in 0 until roomsToShow.size) {
            if (room == roomsToShow[i]) {
                return i
            }
        }
        return -1
    }

    private inner class RoomListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object :RecyclerView.ViewHolder(RoomCell(context!!).also {
                it.onClickShowRoom = this@HousingComplexFragment::showPlaceDialogForRoom
                it.onDeleteRoomConfirmed = this@HousingComplexFragment::onDeleteRoomConfirmed
            }){}
        }

        override fun getItemCount(): Int {
            return roomsToShow.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as RoomCell).refresh(roomsToShow[position])
        }
    }

    private fun backToMapFragment() {

        mainActivity?.supportFragmentManager?.beginTransaction()
            ?.remove(this)
            ?.commit()

        onBackToMapFragment?.invoke()
    }


}
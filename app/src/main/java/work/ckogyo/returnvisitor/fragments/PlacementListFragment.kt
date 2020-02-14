package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.placement_list_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlacementCollection
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.views.PlacementListCell
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlacementListFragment : Fragment() {

    var onPlacementLoaded: ((count: Int) -> Unit)? = null
    var onPlacementSelected: ((Placement) -> Unit)? = null
    var onPlacementDeleted: ((Placement) -> Unit)? = null

    private val placements = ArrayList<Placement>()
    private val handler = Handler()
    private var isLoadingPlacements = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.placement_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fadeNoPlacementOverlay()

        GlobalScope.launch {
            loadPlacementList()
        }

        placementSearchText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshSearchedPlacements()
                refreshPlacementList()
            }
        })
    }

    private suspend fun loadPlacementList() = suspendCoroutine<Unit> {  cont ->
        isLoadingPlacements = true

        handler.post {
            fadeLoadingPlacementOverlay()
        }

        placements.clear()
        GlobalScope.launch {
            val plcs = PlacementCollection.instance.loadInLatestUseOrder()
            placements.addAll(plcs)

            isLoadingPlacements = false

            handler.post {
                fadeLoadingPlacementOverlay()
                fadeNoPlacementOverlay()

                refreshSearchedPlacements()
                refreshPlacementList()
                onPlacementLoaded?.invoke(placements.size)
            }

            cont.resume(Unit)
        }
    }

    private fun refreshPlacementList() {
        if (searchedPlacements.size <= 0) {
            placementListView.fadeVisibility(false)
        } else {
            placementListView.adapter = PlacementListAdapter()
            placementListView.fadeVisibility(true)
        }
    }

    // 算出プロパティにするとリスト更新時に無駄に何度も呼び出されるので手動更新とした
    private var searchedPlacements = ArrayList<Placement>()
    private fun refreshSearchedPlacements() {
        val search = placementSearchText.text.toString()
        if (search.isEmpty()) {
            searchedPlacements = ArrayList(placements)
        }

        val searchedList = ArrayList<Placement>()
        for (plc in placements) {
            if (plc.toShortString(context!!).contains(search)) {
                searchedList.add(plc)
            }
        }
        searchedPlacements = searchedList
    }

    private fun fadeLoadingPlacementOverlay() {
        loadingPlacementOverlay.fadeVisibility(isLoadingPlacements, addTouchBlockerOnFadeIn = true)
    }

    private fun fadeNoPlacementOverlay() {
        noPlacementFrame.fadeVisibility(placements.size <= 0)
    }

    /**
     * 配布物作成フラグメントで作成したものがすでに作成済みのものとほぼ同じであれば作成済みのものを使用するために返す
     */
    fun getSameLikePlacement(plc: Placement): Placement? {
        for (plc2 in placements) {
            if (plc2.category == plc.category) {

                // Magazineの時のみ年と号をチェックする
                var sameYearNumber = true
                if (plc2.category == Placement.Category.Magazine) {
                    sameYearNumber = plc2.year == plc.year && plc2.number == plc.number
                }

                if (sameYearNumber && plc2.name == plc.name && plc2.description == plc.description) {
                    return plc2
                }
            }
        }
        return null
    }

    inner class PlacementListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return PlacementListCellViewHolder(PlacementListCell(context!!).also {
                it.onSelected = { plc ->
                    this@PlacementListFragment.onPlacementSelected?.invoke(plc)
                }
                it.onDeleteConfirmed = this::onDeletePlacementConfirmedInCell
            })
        }

        override fun getItemCount(): Int {
            return searchedPlacements.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val cell = (holder.itemView as? PlacementListCell)
            cell?.refresh(searchedPlacements[position])
        }

        private fun onDeletePlacementConfirmedInCell(cell: PlacementListCell) {

            val pos = getPositionByPlacement(cell.placement)

            if (pos < 0) return

            notifyItemRemoved(pos)

            placements.remove(cell.placement)
            refreshSearchedPlacements()

            GlobalScope.launch {
                PlacementCollection.instance.deleteAsync(cell.placement)
            }

            onPlacementDeleted?.invoke(cell.placement)
        }

        private fun getPositionByPlacement(plc: Placement): Int {

            for (i in 0 until searchedPlacements.size) {
                if (plc == searchedPlacements[i]) {
                    return i
                }
            }
            return -1
        }
    }

    inner class PlacementListCellViewHolder(plcCell: PlacementListCell): RecyclerView.ViewHolder(plcCell)

}
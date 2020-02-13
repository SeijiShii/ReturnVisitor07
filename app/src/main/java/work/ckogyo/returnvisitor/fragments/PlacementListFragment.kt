package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.placement_list_fragment.*
import kotlinx.android.synthetic.main.work_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlacementCollection
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.fadeVisibility
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlacementListFragment : Fragment() {

    var onPlacementLoaded: ((count: Int) -> Unit)? = null
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
                refreshPlacementList()
                onPlacementLoaded?.invoke(placements.size)
            }

            cont.resume(Unit)
        }
    }

    private fun refreshPlacementList() {
        // TODO: refreshPlacementList
        if (placements.size <= 0) {
            placementListView.fadeVisibility(false)
            placementListView.removeAllViews()
        } else {
            placementListView.fadeVisibility(true)
        }
    }

    private fun fadeLoadingPlacementOverlay() {
        loadingPlacementOverlay.fadeVisibility(isLoadingPlacements)
    }

    private fun fadeNoPlacementOverlay() {
        noPlacementFrame.fadeVisibility(placements.size <= 0)
    }

}
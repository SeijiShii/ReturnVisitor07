package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.info_tag_popup.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.InfoTagCollection
import work.ckogyo.returnvisitor.models.InfoTag
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.views.InfoTagListCell
import java.util.*
import kotlin.collections.ArrayList

class InfoTagPopup(private val anchor: View,
                   private val frameId: Int,
                   private val visit: Visit,
                   private val infoTagJob: Deferred<ArrayList<InfoTag>>) : PopupDialog(anchor, frameId) {

    private val infoTags = ArrayList<InfoTag>()

    var onInfoTagSelected: ((InfoTag) -> Unit)? = null
    var onInfoTagDeleted: ((InfoTag) -> Unit)? = null

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.info_tag_popup, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (infoTagJob.isActive && !infoTagJob.isCompleted) {
            loadingInfoTagOverlay.alpha = 1f
        } else {
            loadingInfoTagOverlay.alpha = 0f
        }

        createInfoTagButton.setOnClick(this::onClickCreateInfoTagButton)

        infoTagSearchText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshCreateInfoTagButton()
                refreshInfoTagList()
            }
        })
        refreshCreateInfoTagButton()

        val handler = Handler()

        GlobalScope.launch {
            val tags = infoTagJob.await()
            infoTags.clear()
            infoTags.addAll(tags)

            handler.post {
                loadingInfoTagOverlay.fadeVisibility(false)

                refreshInfoTagList()
            }
        }
    }

    private fun refreshInfoTagList() {

        refreshSearchedTags()

        if (searchedTags.size <= 0) {
            infoTagListView.fadeVisibility(false)
        } else {
            infoTagListView.adapter = InfoTagListAdapter()
            infoTagListView.fadeVisibility(true)
        }
    }

    private var searchedTags = ArrayList<InfoTag>()
    private fun refreshSearchedTags() {

        // すでにVisitに追加されているものを除外する
        val filtered = ArrayList<InfoTag>()
        for (tag in infoTags) {
            var alreadyAdded = false
            for (tag2 in visit.infoTags) {
                if (tag == tag2) {
                    alreadyAdded = true
                }
            }

            if (!alreadyAdded) {
                filtered.add(tag)
            }
        }

        val search = infoTagSearchText.text.toString()
        if (search.isEmpty()) {
            searchedTags = filtered
            return
        }

        val filtered2 = ArrayList<InfoTag>()

        for (tag in filtered) {
            if (tag.name.contains(search)) {
                filtered2.add(tag)
            }
        }
        searchedTags = filtered2
    }

    private fun selectTag(tag: InfoTag) {

        tag.lastUsedAt = Calendar.getInstance()

        GlobalScope.launch {
            FirebaseDB.instance.saveInfoTagAsync(tag)
        }

        onInfoTagSelected?.invoke(tag)
        close()
    }

    private fun refreshCreateInfoTagButton() {
        createInfoTagButton.isEnabled = infoTagSearchText.text.isNotEmpty()
    }

    private fun onClickCreateInfoTagButton(v: View) {
        // すでに登録済みのタグならそれを適用する

        val searchText = infoTagSearchText.text.toString()

        var tag: InfoTag? = null
        for (tag2 in infoTags) {
            if (searchText == tag2.name) {
                tag = tag2
            }
        }
        selectTag(tag ?: InfoTag(searchText))
    }

    private inner class InfoTagListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(InfoTagListCell(context!!).also {
                it.onSelected = this@InfoTagPopup::selectTag
                it.onDeleteInfoTagConfirmed = this::deleteInfoTag
            }){}
        }

        override fun getItemCount(): Int {
            return searchedTags.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as? InfoTagListCell)?.refresh(searchedTags[position])
        }

        private fun deleteInfoTag(tag: InfoTag) {

            val pos = getPositionByInfoTag(tag)

            if (pos < 0) return

            notifyItemRemoved(pos)

            infoTags.remove(tag)
            visit.infoTags.remove(tag)

            refreshSearchedTags()

            GlobalScope.launch {
                FirebaseDB.instance.deleteInfoTagAsync(tag)
            }

            onInfoTagDeleted?.invoke(tag)
        }

        private fun getPositionByInfoTag(tag: InfoTag): Int {

            for (i in 0 until searchedTags.size) {
                if (tag == searchedTags[i]) {
                    return i
                }
            }
            return -1
        }
    }

}
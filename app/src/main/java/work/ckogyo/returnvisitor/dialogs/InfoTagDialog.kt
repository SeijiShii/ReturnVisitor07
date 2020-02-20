package work.ckogyo.returnvisitor.dialogs

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.info_tag_dialog.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.InfoTagCollection
import work.ckogyo.returnvisitor.models.InfoTag
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.views.InfoTagListCell
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InfoTagDialog(val visit: Visit) : DialogFrameFragment() {

    private val infoTags = ArrayList<InfoTag>()
    private val handler = Handler()

    private var isLoadingInfoTags = false

    var onInfoTagSelected: ((InfoTag) -> Unit)? = null
    var onInfoTagDeleted: ((InfoTag) -> Unit)? = null

    override fun onOkClick() {}

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.info_tag_dialog, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        allowScroll = false
        super.onViewCreated(view, savedInstanceState)

        closeButtonStyle = CloseButtonStyle.CloseOnly

        setTitle(R.string.info_tag)

        GlobalScope.launch {
            loadInfoTagList()
        }

        createInfoTagButton.setOnClick(this::onClickCreateInfoTagButton)

        infoTagSearchText.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshCreateInfoTagButton()
                refreshInfoTagList()
            }
        })
        refreshCreateInfoTagButton()
    }

    private suspend fun loadInfoTagList() = suspendCoroutine<Unit> { cont ->
        isLoadingInfoTags = true

        handler.post {
            fadeLoadingInfoTagOverlay()
        }

        infoTags.clear()
        GlobalScope.launch {
            val tags = InfoTagCollection.instance.loadInLatestUseOrder()
            infoTags.addAll(tags)

            isLoadingInfoTags = false

            handler.post {
                fadeLoadingInfoTagOverlay()
                fadeNoInfoTagOverlay()
                refreshInfoTagList()
            }

            cont.resume(Unit)
        }
    }

    private fun fadeLoadingInfoTagOverlay() {
        loadingInfoTagOverlay.fadeVisibility(isLoadingInfoTags, addTouchBlockerOnFadeIn = true)
    }

    private fun fadeNoInfoTagOverlay() {
        noInfoTagFrame.fadeVisibility(infoTags.size <= 0)
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

    private fun refreshInfoTagList() {

        refreshSearchedTags()

        if (searchedTags.size <= 0) {
            infoTagListView.fadeVisibility(false)
        } else {
            infoTagListView.adapter = InfoTagListAdapter()
            infoTagListView.fadeVisibility(true)
        }
    }

    private inner class InfoTagListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(InfoTagListCell(context!!).also {
                it.onSelected = this@InfoTagDialog::selectTag
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
                InfoTagCollection.instance.deleteAsync(tag)
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

    private fun selectTag(tag: InfoTag) {

        tag.lastUsedAt = Calendar.getInstance()

        GlobalScope.launch {
            InfoTagCollection.instance.setAsync(tag)
        }

        onInfoTagSelected?.invoke(tag)
        close()
    }
}
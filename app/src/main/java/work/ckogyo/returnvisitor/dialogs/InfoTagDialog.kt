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
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.views.InfoTagListCell
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InfoTagDialog : DialogFrameFragment() {

    private val infoTags = ArrayList<InfoTag>()
    private val handler = Handler()

    private var isLoadingInfoTags = false

    var onInfoTagSelected: ((InfoTag) -> Unit)? = null

    override fun onOkClick() {}

    override fun inflateContentView(): View {
        return View.inflate(context, R.layout.info_tag_dialog, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        allowScroll = false
        super.onViewCreated(view, savedInstanceState)

        showCloseButtonOnly = true

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

    private fun refreshInfoTagList() {
        if (infoTags.size <= 0) {
            infoTagListView.fadeVisibility(false)
        } else {
            infoTagListView.adapter = InfoTagListAdapter()
            infoTagListView.fadeVisibility(true)
        }
    }

    private inner class InfoTagListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(InfoTagListCell(context!!)){}
        }

        override fun getItemCount(): Int {
            return infoTags.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as? InfoTagListCell)?.refresh(infoTags[position])
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
    }
}
package work.ckogyo.returnvisitor.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.work_fragment.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.models.WorkListElm
import work.ckogyo.returnvisitor.views.WorkElmCell
import java.util.*
import kotlin.collections.ArrayList

class WorkFragment(val dataElms: ArrayList<WorkListElm>) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.work_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workListView.adapter = WorkElmAdapter(context!!, dataElms)
    }

    class WorkElmAdapter(private val context: Context, val dataElms: ArrayList<WorkListElm>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return WorkElmViewHolder(WorkElmCell(context))
        }

        override fun getItemCount(): Int {
            return dataElms.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as WorkElmViewHolder).dataElm = dataElms[position]
        }
    }

    class WorkElmViewHolder(private val elmCell: WorkElmCell): RecyclerView.ViewHolder(elmCell) {

        var dataElm : WorkListElm? = null
        set(value) {
            field = value
            elmCell.dataElm = field
        }

    }


}


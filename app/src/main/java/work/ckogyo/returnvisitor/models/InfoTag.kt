package work.ckogyo.returnvisitor.models

import work.ckogyo.returnvisitor.utils.DataModelKeys.lastUsedAtInMillisKey
import java.util.*
import kotlin.collections.HashMap

class InfoTag : BaseDataModel {

    companion object {
        const val idPrefix = "info_tag"
    }

    /**
     * 最後に使用された日時
     * リストに列挙するため
     */
    var lastUsedAt: Calendar = Calendar.getInstance()

    constructor(name: String = ""): super(idPrefix) {
        this.name = name
    }

    override fun clone(): InfoTag {
        return InfoTag(name).also {
            cloneBaseProperties(it)
        }
    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap
            map[lastUsedAtInMillisKey] = lastUsedAt.timeInMillis
            return map
        }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        val lastUsedAtInMillis = map[lastUsedAtInMillisKey].toString().toLong()
        lastUsedAt.timeInMillis = lastUsedAtInMillis
    }
}
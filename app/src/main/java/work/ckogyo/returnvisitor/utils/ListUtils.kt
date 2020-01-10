package work.ckogyo.returnvisitor.utils

import work.ckogyo.returnvisitor.models.BaseDataModel

fun <T: BaseDataModel>filterUndupList(origList: ArrayList<T>): ArrayList<T> {
    val filtered = ArrayList<T>()
    for(elm in origList) {
        if (!filtered.contains(elm)){
            filtered.add(elm)
        }
    }
    return filtered
}
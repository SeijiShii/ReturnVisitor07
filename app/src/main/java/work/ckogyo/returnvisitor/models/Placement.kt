package work.ckogyo.returnvisitor.models

import android.content.Context
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.*
import java.lang.StringBuilder
import java.util.*

class Placement: BaseDataModel {

    companion object {
        const val idPrefix = "placement"
    }

    constructor():super(idPrefix)

    enum class Category {
        None,
        Bible,
        Book,
        Magazine,
        Tract,
        ContactCard,
        WebLink,
        ShowVideo,
    }

    enum class MagazineType {
        None,
        WatchTower,
        Awake,
        StudyWatchTower
    }

    var category: Category = Category.None
    var magazineType = MagazineType.None
    var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    var number: Int = 1

    /**
     * 最後に使用された日時
     * リストに列挙するため
     */
    var lastUsedAt: Calendar = Calendar.getInstance()

    override fun clone(): Placement {
        val cloned = Placement()
        super.cloneBaseProperties(cloned)

        cloned.category = category
        cloned.magazineType = magazineType
        cloned.year = year
        cloned.number = number

        cloned.lastUsedAt = lastUsedAt.clone() as Calendar

        return cloned
    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[categoryKey] = category
            map[magazineTypeKey] = magazineType
            map[yearKey] = year
            map[numberKey] = number
            map[lastUsedAtInMillisKey] = lastUsedAt.timeInMillis

            return map
        }

    override fun initFromHashMap(map: HashMap<String, Any>){
        super.initFromHashMap(map)

        category = Category.valueOf(map[categoryKey].toString())
        magazineType = MagazineType.valueOf(map[magazineTypeKey].toString())
        year = map[yearKey].toString().toInt()
        number = map[numberKey].toString().toInt()

        lastUsedAt = Calendar.getInstance()
        val lastUsedAtInMillis = map[lastUsedAtInMillisKey].toString().toLong()
        lastUsedAt.timeInMillis = lastUsedAtInMillis

    }

    fun toShortString(context: Context):String {

        val buider = StringBuilder(toCategoryText(context))
        if (category == Category.Magazine) {
            buider.append(" ${toMagazineDataText(context)}")
        }

        if (name.isNotEmpty()) {
            buider.append(" $name")
        }
        return buider.toString()
    }

    private fun toCategoryText(context: Context): String {

        val catArray = context.resources.getStringArray(R.array.placementCategoryArray)
        return catArray[category.ordinal]
    }

    private fun toMagazineDataText(context: Context): String {

        val mzTypeArray = context.resources.getStringArray(R.array.magazineTypeArray)
        return "${mzTypeArray[magazineType.ordinal]} $year / No.$number"
    }

}
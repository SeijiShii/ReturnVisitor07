package work.ckogyo.returnvisitor.utils

fun String.wideToNarrow(): String{

    for (map in wideHalfMap) {
        replace(map.key, map.value)
    }
    return this
}

fun String.containsWide(): Boolean {
    for (map in wideHalfMap) {
        if (contains(map.key)) return true
    }
    return false
}

private val wideHalfMap = mapOf(
    "０" to "0",
    "１" to "1",
    "２" to "2",
    "３" to "3",
    "４" to "4",
    "５" to "5",
    "６" to "6",
    "７" to "7",
    "８" to "8",
    "９" to "9"
)
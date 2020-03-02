package work.ckogyo.returnvisitor.utils

import work.ckogyo.returnvisitor.R


const val requestPermissionCode = 6090
const val googleSingInRequestCode = 6100

const val debugTag = "RETURN_VISITOR_DEBUG"

object FirebaseCollectionKeys {
    const val placesKey = "places"
    const val personsKey = "persons"
    const val visitsKey = "visits"
    const val worksKey = "works"
    const val placementsKey = "placements"
    const val infoTagsKey = "info_tags"
    const val monthReportsKey = "month_reports"
    const val placementIdsKey = "placement_ids"
    const val infoTagIdsKey = "info_tag_ids"
    const val dailyReportsKey = "daily_reports"
}

object SharedPrefKeys {
    const val returnVisitorPrefsKey = "return_visitor_prefs"
    const val publisherNameKey = "publisher_name"
    const val zoomLevelKey = "zoom_level"
    const val latitudeKey = "latitude"
    const val longitudeKey = "longitude"
    const val isTimeCounting = "is_time_counting"
    const val weekStartKey = "week_start"
}

object DataModelKeys {
    const val idKey = "id"
    const val categoryKey = "category"

    const val nameKey = "name"
    const val descriptionKey = "description"

    const val addressKey = "address"
    const val placeIdKey = "place_id"
    const val parentIdKey = "parent_id" // RoomからHousingComplexを指すID

    const val sexKey = "sex"
    const val ageKey = "age"

    const val seenKey = "seen"
    const val isRVKey = "is_rv"
    const val isStudyKey = "is_study"

    const val dateTimeMillisKey = "date_time_millis"
    const val ratingKey = "rating"
    const val personIdKey = "person_id"
    const val personVisitsKey = "person_visits"

    const val startKey = "start"
    const val endKey = "end"

    const val yearKey = "year"
    const val monthKey = "month"
    const val numberKey = "number"
    const val magazineTypeKey = "magazine_type"
    const val lastUsedAtInMillisKey = "last_used_at_in_millis"

    const val durationKey = "duration"
    const val rvCountKey = "rv_count"
    const val studyCountKey = "study_count"
    const val uniqueStudyCountKey = "unique_study_count"
    const val plcCountKey = "plc_count"
    const val showVideoCountKey = "show_video_count"
    const val hasWorkKey = "has_work"
    const val hasVisitKey = "has_visit"

    const val dateStringKey = "date_string"

    const val pastCarryOverKey = "past_carry_over"
    const val dayOfMonthKey = "day_of_month"
}



val pinMarkerIds = arrayOf(
    R.mipmap.gray_pin,
    R.mipmap.red_pin,
    R.mipmap.purple_pin,
    R.mipmap.blue_pin,
    R.mipmap.green_pin,
    R.mipmap.yellow_pin,
    R.mipmap.orange_pin
)

val roundMarkerIds = arrayOf(
    R.mipmap.gray_round,
    R.mipmap.red_round,
    R.mipmap.purple_round,
    R.mipmap.blue_round,
    R.mipmap.green_round,
    R.mipmap.yellow_round,
    R.mipmap.orange_round
)

val squareMarkerIds = arrayOf(
    R.mipmap.gray_square,
    R.mipmap.red_square,
    R.mipmap.purple_square,
    R.mipmap.blue_square,
    R.mipmap.green_square,
    R.mipmap.yellow_square,
    R.mipmap.orange_square
)


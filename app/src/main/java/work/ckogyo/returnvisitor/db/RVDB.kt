package work.ckogyo.returnvisitor.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*

class RVDB {

    companion object {

        const val placeTableName = "place_table"
        const val visitTableName = "visit_table"
    }

    fun insertPlace(context: Context, place: Place) {

        val db = RVDBOpenHelper(context).writableDatabase

        val values = ContentValues()
        values.put(idKey, place.id)
        values.put(latitudeKey, place.latLng.latitude)
        values.put(longitudeKey, place.latLng.longitude)
        values.put(jsonKey, place.jsonObject.toString())

        db.insert(placeTableName, null, values)
    }

    fun loadPlaces(context: Context): ArrayList<Place> {

        val db = RVDBOpenHelper(context).readableDatabase

        val cursor = db.query(placeTableName,
            arrayOf(idKey, latitudeKey, longitudeKey, jsonKey),
            null, null, null, null, null)

        val places = ArrayList<Place>()

        cursor.moveToPosition(-1)
        while (cursor.moveToNext()){

            places.add(cursorToPlace(cursor))
        }

        cursor.close()

        return places
    }

    fun loadPlaceById(context: Context, placeId: String): Place? {

        val db = RVDBOpenHelper(context).readableDatabase

        val cursor = db.query(placeTableName,
            arrayOf(idKey, latitudeKey, longitudeKey, jsonKey),
            "id = $placeId", null, null, null, null)

        val places = ArrayList<Place>()

        cursor.moveToPosition(-1)
        while (cursor.moveToNext()){

            places.add(cursorToPlace(cursor))
        }

        cursor.close()

        return if (places.isNotEmpty()) {
            places[0]
        } else {
            null
        }
    }

    fun deletePlace(context: Context, place: Place) {

        val db = RVDBOpenHelper(context).writableDatabase

        val statement = db.compileStatement("DELETE FROM $placeTableName WHERE id = ?")
        statement.bindString(1, place.id)
        statement.execute()
    }

    fun deleteVisitsOfPlace(context: Context, place: Place) {

        val db = RVDBOpenHelper(context).writableDatabase

        val statement = db.compileStatement("DELETE FROM $visitTableName WHERE place_id = ?")
        statement.bindString(1, place.id)
        statement.execute()
    }

    private fun cursorToPlace(cursor: Cursor):Place {
        return Place(JSONObject(cursor.getString(3)))
    }

    fun insertVisit(context: Context, visit: Visit) {

        val db = RVDBOpenHelper(context).writableDatabase

        val values = ContentValues()
        values.put(idKey, visit.id)
//        values.put(placeIdKey, visit.place.id)
        values.put(jsonKey, visit.jsonObject.toString())

        db.insert(placeTableName, null, values)
    }
}
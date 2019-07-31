package work.ckogyo.returnvisitor.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RVDBOpenHelper(context: Context) : SQLiteOpenHelper(context, dbName, null, dbVersion) {

    companion object {
        const val dbVersion = 2
        const val dbName = "return_visitor_db"

        private const val createPlaceTableSql = "CREATE TABLE ${RVDB.placeTableName}(_id INTEGER PRIMARY KEY, id TEXT, latitude TEXT, longitude TEXT, json TEXT)"
        private const val dropPlaceTableSql = "DROP TABLE IF EXISTS ${RVDB.placeTableName}"
        private const val createVisitTableSql = "CREATE TABLE ${RVDB.visitTableName}(_id INTEGER PRIMARY KEY, id TEXT, place_id TEXT, json TEXT)"
        private const val dropVisitTableSql = "DROP TABLE IF EXISTS ${RVDB.visitTableName}"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createPlaceTableSql)
        db?.execSQL(createVisitTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase?, old: Int, new: Int) {
        db?.execSQL(dropPlaceTableSql)
        db?.execSQL(dropVisitTableSql)
        onCreate(db)
    }
}
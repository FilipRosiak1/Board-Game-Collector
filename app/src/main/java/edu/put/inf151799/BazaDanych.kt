package edu.put.inf151799

import android.database.sqlite.SQLiteDatabase
import java.io.File
import android.database.sqlite.SQLiteOpenHelper
import android.content.Context
import android.content.ContentValues

class BazaDanych(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val _context:Context = context

    companion object {
        private val DATABASE_NAME = "GBBdatabase.db"
        private val DATABASE_VERSION = 1
        val TABLE = "appdata151799"
        val COLUMN_ID = "id"
        val COLUMN_TITLE = "tytul_org"
        val COLUMN_THUMBNAIL = "zdjecie_gry"
        val COLUMN_YEAR = "rok_wydania"
        val COLUMN_RANK = "pozycja_w_rankingu"
        val COLUMN_IS_EXP = "czy_dodatek"
        val COLUMN_IMAGES = "zdjecia"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val database = File(_context.getDatabasePath(DATABASE_NAME).toString())
        if(!database.exists()) tworz_tabele()
    }

    fun tworz_tabele() {
        this.writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE")

        this.writableDatabase.execSQL("CREATE TABLE "+TABLE+"("+COLUMN_ID+" INTEGER PRIMARY KEY, "+COLUMN_TITLE+" TEXT, "
                +COLUMN_THUMBNAIL+" TEXT, "+COLUMN_YEAR+" INTEGER, "+COLUMN_RANK+" INTEGER, "+COLUMN_IS_EXP+" INTEGER, "
                +COLUMN_IMAGES+" TEXT DEFAULT ''"+")")
    }

    fun select_all(item: items, order_by: String): ArrayList<Gra> {
        val db = this.writableDatabase
        val dodatek = item.ordinal
        var query: String
        var lista_gier = ArrayList<Gra>()

        if(order_by == "tytul_org" || order_by == "rok_wydania") {
            query = "SELECT * FROM $TABLE WHERE $COLUMN_IS_EXP = $dodatek ORDER BY $order_by"
        } else query = "SELECT * FROM $TABLE WHERE $COLUMN_IS_EXP = $dodatek"

        val cursor = db.rawQuery(query, null)

        if(cursor.moveToFirst()) {
            while(!cursor.isAfterLast()) {
                var gra: Gra
                val id = cursor.getInt(0)
                val tytul = cursor.getString(1)
                val thumbnail = cursor.getString(2)
                val rok = cursor.getInt(3)
                val ranking = cursor.getInt(4)
                val dodatek = cursor.getInt(5)

                gra = Gra(id, tytul, thumbnail, rok, ranking, dodatek)
                lista_gier.add(gra)
                cursor.moveToNext()
            }
            cursor.close()
        }
        return lista_gier
    }

    fun dodaj_gre(gra: Gra) {
        val values = ContentValues()
        values.put(COLUMN_ID, gra.id)
        values.put(COLUMN_TITLE, gra.tytul)
        values.put(COLUMN_THUMBNAIL, gra.thumbnail)
        values.put(COLUMN_YEAR, gra.rok_wydania)
        values.put(COLUMN_RANK, gra.ranking)
        values.put(COLUMN_IS_EXP, gra.czy_dodatek)
        values.put(COLUMN_IMAGES, "")

        val db = this.writableDatabase
        db.insert(TABLE, null, values)
        db.close()
    }

    fun zmien_zdjecia(gra: Gra) {
        val content = ContentValues()
        content.put(COLUMN_IMAGES, gra.zdjecia)
        this.writableDatabase.update(TABLE,content, COLUMN_ID+" =${gra.id}", null)
    }

    fun znajdz_po_ID(id: Int): Gra? {
        val db = this.writableDatabase
        var gra: Gra? = null
        val query = "SELECT * FROM $TABLE WHERE $COLUMN_ID=$id"
        val cursor = db.rawQuery(query,null)


        if(cursor.moveToFirst()) {
            val id = cursor.getInt(0)
            val tytul = cursor.getString(1)
            val thumbnail = cursor.getString(2)
            val rok = cursor.getInt(3)
            val ranking = cursor.getInt(4)
            val dodatek = cursor.getInt(5)
            val zdjecia = cursor.getString(6)

            gra = Gra(id, tytul, thumbnail, rok, ranking, dodatek, zdjecia)
            cursor.close()
        }
        db.close()
        return gra
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }
}
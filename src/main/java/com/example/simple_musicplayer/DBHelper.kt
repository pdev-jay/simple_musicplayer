package com.example.simple_musicplayer

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DBHelper(context: Context): SQLiteOpenHelper(context, "musicDB", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        val createQuery = """
            create table musicTBL(id text primary key, title text, artist text, albumId text, genre text, duration integer, favorite integer)
        """.trimIndent()

        db?.execSQL(createQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersioin: Int, newVersion: Int) {
        val dropQuery = "drop table musicTBL"
        db?.execSQL(dropQuery)
        this.onCreate(db)
    }

    fun insertMusic(music: Music): Boolean{
        var flag = false
        val db = this.writableDatabase

        val insertQuery =
            "insert into musicTBL(id, title, artist, albumId, genre, duration, favorite) values ('${music.id}', '${music.title}', '${music.artist}', '${music.albumId}', '${music.genre}', ${music.duration}, ${music.favorite})"

        try {
            db.execSQL(insertQuery)
            flag = true
        } catch (e: SQLException){
            Log.d("Log_debug", "${e.printStackTrace()}")
        } finally{
            db.close()
        }

        return flag
    }

    fun selectMusicAll(): MutableList<Music>{
        var musicList: MutableList<Music> = mutableListOf<Music>()
        val db = this.readableDatabase

        val selectQuery = """
            select * from musicTBL
        """.trimIndent()

        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val genre = cursor.getString(4)
                    val duration = cursor.getInt(5)
                    val favorite = cursor.getInt(6)
                    musicList?.add(Music(id, title, artist, albumId, genre, duration, favorite))
                }
            }
        } catch (e: Exception){
            Log.d("Log_debug", "${e.printStackTrace()}")
        } finally {
            cursor?.close()
            db.close()
        }
        return musicList
    }

//    fun selectMusic(query: String): MutableList<Music>{
//        var musicList: MutableList<Music> = mutableListOf()
//        val db = this.readableDatabase
//        var cursor: Cursor? = null
//
//        //query와 유사한 문자열이 입력된다면 artist, title에 대한 select 결과 모두 가져옴
//        val selectQuery = """
//            select * from musicTBL where artist like '$query%' or title like '$query%'
//        """.trimIndent()
//
//        try {
//            cursor = db.rawQuery(selectQuery, null)
//            if (cursor.count > 0) {
//                while (cursor.moveToNext()) {
//                    val id = cursor.getString(0)
//                    val title = cursor.getString(1)
//                    val artist = cursor.getString(2)
//                    val albumId = cursor.getString(3)
//                    val genre = cursor.getString(4)
//                    val duration = cursor.getInt(5)
//                    val favorite = cursor.getInt(6)
//                    musicList?.add(Music(id, title, artist, albumId, genre, duration, favorite))
//                }
//            }
//        } catch (e: Exception){
//            Log.d("Log_debug", "${e.printStackTrace()}")
//        } finally {
//            cursor?.close()
//            db.close()
//        }
//
//        return musicList
//    }

    fun selectFilter(filter: String?, type: Type): MutableList<Music> {
        var musicList: MutableList<Music> = mutableListOf<Music>()
        val db = this.readableDatabase

        var selectQuery: String? = null

        //네비게이션 메뉴의 어느 아이템이 선택되었는지에 따른 query 변화
        selectQuery = when(type){
            Type.ALL -> """
                    select * from musicTBL
                """.trimIndent()

            Type.ARTIST -> """
                    select * from musicTBL where artist = '$filter'
                """.trimIndent()

            Type.GENRE -> """
                    select * from musicTBL where genre = '$filter'
                """.trimIndent()

            Type.FAVORITE -> """
                    select * from musicTBL where favorite = 1
                """.trimIndent()
        }

        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val genre = cursor.getString(4)
                    val duration = cursor.getInt(5)
                    val favorite = cursor.getInt(6)
                    musicList?.add(Music(id, title, artist, albumId, genre, duration, favorite))
                }
            }
        } catch (e: Exception){
            Log.d("Log_debug", "${e.printStackTrace()}")
        } finally {
            cursor?.close()
            db.close()
        }

        return musicList
    }

    fun updateFavorite(music: Music): Boolean{
        var flag = false
        val db = this.writableDatabase

        var updateQuery: String = """
            update musicTBL set favorite = ${music.favorite} where id = '${music.id}'
        """

        try {
            db.execSQL(updateQuery)
            flag = true
        } catch (e: SQLException){
            Log.d("Log_debug", "${e.printStackTrace()}")
        } finally {
            db.close()
        }

        return flag
    }
}
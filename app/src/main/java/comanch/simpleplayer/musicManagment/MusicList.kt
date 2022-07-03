package comanch.simpleplayer.musicManagment

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import comanch.simpleplayer.dataBase.MusicTrack

class MusicList(context: Context, path: String) {

    private val list = mutableListOf<MusicTrack>()

    private val _path = path

    private val mediaContentUriE: Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    private val projection = arrayOf(
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.IS_MUSIC,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media._ID,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.RELATIVE_PATH
        else
            MediaStore.Audio.Media.DATA
    )

    private val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        "is_music=? AND relative_path=?"
    else
        "is_music=? AND _data like ? "
    private val selectionArgs = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        arrayOf("1", path)
    else
        arrayOf("1", "%${path}/%")

    private val sortOrder = "album ASC, artist ASC"

    private val cursor =
        mediaContentUriE?.let {
            context.contentResolver?.query(
                it,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
        }

    private fun createList() {

        if (cursor != null && cursor.moveToFirst() && cursor.count > 0) {
            do {
                val musicTrack = MusicTrack()
                musicTrack.artist = cursor.getString(0)
                musicTrack.album = cursor.getString(1)
                musicTrack.title = cursor.getString(2)
                musicTrack.duration = if (cursor.getLong(4) > 0) {
                    "${getMin(cursor.getLong(4))}:${getSec(cursor.getLong(4))}"
                } else {
                    "0"
                }
                musicTrack.musicId = "${cursor.getLong(5)}"
                list.add(musicTrack)
            } while (cursor.moveToNext())
        }
    }

    private fun getMin(l: Long): String{

        val secLong = l / 1000 / 60
        return if("$secLong".length == 1){
            "0$secLong"
        }else{
            "$secLong"
        }
    }

    private fun getSec(l: Long): String{

        val secLong = l / 1000 % 60
        return if("$secLong".length == 1){
            "0$secLong"
        }else{
            "$secLong"
        }
    }

    fun getMusicList(): List<MusicTrack>{
        createList()
        return list
    }
}
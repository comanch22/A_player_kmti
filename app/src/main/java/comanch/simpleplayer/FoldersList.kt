package comanch.simpleplayer

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import comanch.simpleplayer.dataBase.MusicTrack

class FoldersList(context: Context) {

    private val list = mutableListOf<MusicTrack>()

    private val mediaContentUriE: Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    private val projection = arrayOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.RELATIVE_PATH
        else
            MediaStore.Audio.Media.DATA
    )

    private val selection = "is_music=?"
    private val selectionArgs = arrayOf("1")
    private val sortOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        "relative_path ASC"
    else
        "_data ASC"

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

        var path = ""
        Log.e("aboba", "$path")
        if (cursor != null && cursor.moveToFirst() && cursor.count > 0) {
            do {
                if (path == cursor.getString(0)){
                    continue
                }else{
                    Log.e("aboba", "$path")
                    val folder = MusicTrack()
                    path = cursor.getString(0)
                    folder.isFolder = true
                    folder.relativePath = path
                    folder.relativePathShort = cursor.getString(0).dropLast(1).substringAfterLast("/")
                    list.add(folder)
                }
            } while (cursor.moveToNext())
        }
    }

    fun getFolders(): List<MusicTrack>{
        createList()
        return list
    }
}
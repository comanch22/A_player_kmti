package comanch.simpleplayer.dataBase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "musicTrack_data_base")
data class MusicTrack(

    @PrimaryKey(autoGenerate = true)
    var musicTrackId: Long = 0L,

    @ColumnInfo(name = "title")
    var title: String = "",

    @ColumnInfo(name = "playListName")
    var playListName: String = "",

    @ColumnInfo(name = "relativePath")
    var relativePath: String = "",

    @ColumnInfo(name = "relativePathShort")
    var relativePathShort: String = "",

    @ColumnInfo(name = "artist")
    var artist: String = "",

    @ColumnInfo(name = "album")
    var album: String = "",

    @ColumnInfo(name = "duration")
    var duration: String = "",

    @ColumnInfo(name = "musicId")
    var musicId: String = "",

    @ColumnInfo(name = "uri")
    var uriAsString: String = "",

    @ColumnInfo(name = "isFolder")
    var isFolder: Boolean = false,

    @ColumnInfo(name = "active")
    var active: Int = 0,

    @ColumnInfo(name = "position")
    var position: Int = -1,

    @ColumnInfo(name = "isButtonPlayVisible")
    var isButtonPlayVisible: Int = 0
) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is MusicTrack)
            return false
        return isFolder == other.isFolder &&
                relativePath == other.relativePath &&
                musicId == other.musicId &&
                musicTrackId == other.musicTrackId &&
                playListName == other.playListName
    }

    override fun hashCode(): Int {
        var result = musicTrackId.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }

    fun myCopy(item: MusicTrack) {

        this.relativePathShort = item.relativePathShort
        this.relativePath = item.relativePath
        this.isFolder = item.isFolder
        this.artist = item.artist
        this.album = item.album
        this.duration = item.duration
        this.musicId = item.musicId
        this.title = item.title
        this.uriAsString = item.uriAsString
    }
}

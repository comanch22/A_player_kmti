package comanch.simpleplayer.dataBase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playList_data_base")
class PlayList (

    @PrimaryKey(autoGenerate = true)
    var playListId: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "empty",

    @ColumnInfo(name = "active")
    var active: Int = 0,

    @ColumnInfo(name = "position")
    var position: Int = -1
){}
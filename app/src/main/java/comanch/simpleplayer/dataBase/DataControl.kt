package comanch.simpleplayer.dataBase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MusicTrack::class, PlayList::class], version = 3, exportSchema = false)
abstract class DataControl : RoomDatabase() {

    abstract val musicTrackDAO: MusicTrackDAO
    abstract val playListDAO: PlayListDAO

    companion object {

        @Volatile
        private var INSTANCE: DataControl? = null

        fun getInstance(context: Context): DataControl {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            DataControl::class.java,
                            "music_data_base")
                            .fallbackToDestructiveMigration()
                            .build()
                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}
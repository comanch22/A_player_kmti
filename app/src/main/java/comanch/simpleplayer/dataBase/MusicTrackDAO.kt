package comanch.simpleplayer.dataBase

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MusicTrackDAO {

    @Insert
    suspend fun insert(musicTrack: MusicTrack)

    @Update
    suspend fun update(musicTrack: MusicTrack)

    @Delete
    suspend fun delete(musicTrack: MusicTrack)

    @Insert
    suspend fun insertPlayList(playlist: List<MusicTrack>): List<Long>

    @Transaction
    suspend fun listDelByNameInsByList(deleteListName: String, playlist: List<MusicTrack>) {
        deletePlaylist(deleteListName)
        insertPlayList(playlist)
    }

    @Transaction
    suspend fun activeClearSetOne(track: MusicTrack) {
        deletePlaylist("current playlist")
        update(track)
    }

    @Transaction
    suspend fun getPosition(): Long? {
        val firstItemId = getItemASC()?.musicTrackId
        val firstActiveItemId = getFirstActive()?.musicTrackId
        return if (firstItemId != null && firstActiveItemId != null){
            firstActiveItemId - firstItemId
        }else {
            null
        }
    }

    @Query("SELECT * FROM musicTrack_data_base WHERE musicTrackId < :key and playListName = :name ORDER BY musicTrackId DESC LIMIT 1 ")
    suspend fun getPreviousTrackByKey(key: Long, name: String = "current playlist"): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE musicTrackId > :key and playListName = :name ORDER BY musicTrackId ASC LIMIT 1 ")
    suspend fun getNextTrackByKey(key: Long, name: String = "current playlist"): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE musicId = :musicId and playListName = :name LIMIT 1 ")
    suspend fun getTrackByMusicID(musicId: String, name: String = "current playlist"): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE musicTrackId != :key and playListName = :name ORDER BY active DESC, musicTrackId ASC")
    suspend fun sortByActiveAndWithoutItem(key: Long, name: String = "current playlist"): List<MusicTrack>?

    @Query("SELECT * FROM musicTrack_data_base WHERE active == 1 and playListName = :name ORDER BY musicTrackId ASC LIMIT 1 ")
    suspend fun getFirstActive(name: String = "current playlist"): MusicTrack?

    @Query("DELETE FROM musicTrack_data_base WHERE playListName = :name")
    suspend fun deletePlaylist(name: String): Int

    @Query("SELECT * from musicTrack_data_base WHERE musicTrackId = :key")
    suspend fun get(key: Long): MusicTrack?

    @Query("SELECT * from musicTrack_data_base WHERE playListName = :listName")
    suspend fun getPlaylistByName(listName: String): List<MusicTrack>?

    @Query("SELECT * from musicTrack_data_base WHERE active = :active and playListName = :name")
    suspend fun getPlaylistByActive(active: Int, name: String = "current playlist"): List<MusicTrack>?

    @Query("DELETE FROM musicTrack_data_base")
    suspend fun clear()

    @Query("SELECT * FROM musicTrack_data_base WHERE playListName = :name ORDER BY musicTrackId ASC LIMIT 1")
    suspend fun getItemASC(name: String = "current playlist"): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE playListName = :name ORDER BY musicTrackId DESC LIMIT 1")
    suspend fun getItemDESC(name: String = "current playlist"): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE playListName = :name ORDER BY musicTrackId ASC")
    fun getCurrentItems(name: String = "current playlist"): LiveData<List<MusicTrack>>

    @Query("SELECT COUNT(*) FROM musicTrack_data_base")
    suspend fun getCount(): Int?
}
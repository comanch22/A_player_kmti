package comanch.simpleplayer.dataBase

import androidx.lifecycle.LiveData
import androidx.room.*
import comanch.simpleplayer.helpers.StringKey
import java.util.*

@Dao
interface MusicTrackDAO {

    @Insert
    suspend fun insert(musicTrack: MusicTrack)

    @Insert
    suspend fun insertPlayList(playlist: List<MusicTrack>): List<Long>

    @Update
    suspend fun update(musicTrack: MusicTrack): Int

    @Update
    suspend fun updatePlayList(playlist: List<MusicTrack>)

    @Delete
    suspend fun delete(musicTrack: MusicTrack)

    @Transaction
    suspend fun listDelByNameInsByList(deleteListName: String, playlist: List<MusicTrack>) {
        deletePlaylist(deleteListName)
        insertPlayList(playlist)
    }

    @Transaction
    suspend fun activeClearSetOne(track: MusicTrack) {
        deletePlaylist(StringKey.currentList)
        update(track)
    }

    @Transaction
    suspend fun swapItems(firstId: Long, secondId: Long) {
        val currentList = getPlaylistByName(StringKey.currentList)?.toMutableList()
        val firstItem = getItemById(firstId)
        val secondItem = getItemById(secondId)
        if (currentList != null && firstItem != null && secondItem != null) {
            val firstItemIndex = currentList.binarySearch(
                firstItem,
                compareBy { it.musicTrackId })
            val secondItemIndex = currentList.binarySearch(
                secondItem,
                compareBy { it.musicTrackId })
            if (firstItemIndex < 0 || secondItemIndex < 0) {
                return
            }

            val insertList = mutableListOf<MusicTrack>()
            currentList.forEachIndexed lit@{ index, musicTrack ->

                if (index == firstItemIndex) {
                    return@lit
                }

                if (index == secondItemIndex && firstItemIndex > secondItemIndex){
                    val first = MusicTrack()
                    first.myCopy(firstItem)
                    first.active = firstItem.active
                    first.playListName = StringKey.currentList
                    insertList.add(first)
                }

                val item = MusicTrack()
                item.myCopy(musicTrack)
                item.active = musicTrack.active
                item.playListName = StringKey.currentList
                insertList.add(item)

                if (index == secondItemIndex && firstItemIndex < secondItemIndex){
                    val first = MusicTrack()
                    first.myCopy(firstItem)
                    first.active = firstItem.active
                    first.playListName = StringKey.currentList
                    insertList.add(first)
                }
            }
            listDelByNameInsByList(StringKey.currentList, insertList.toList())
        }
    }

    @Transaction
    suspend fun getPosition(): Long? {
        val firstItemId = getItemASC()?.musicTrackId
        val firstActiveItemId = getFirstActive()?.musicTrackId
        return if (firstItemId != null && firstActiveItemId != null) {
            firstActiveItemId - firstItemId
        } else {
            null
        }
    }

    @Query("SELECT * FROM musicTrack_data_base WHERE musicTrackId < :key and playListName = :name ORDER BY musicTrackId DESC LIMIT 1 ")
    suspend fun getPreviousTrackByKey(key: Long, name: String = StringKey.currentList): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE musicTrackId > :key and playListName = :name ORDER BY musicTrackId ASC LIMIT 1 ")
    suspend fun getNextTrackByKey(key: Long, name: String = StringKey.currentList): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE musicId = :musicId and playListName = :name LIMIT 1 ")
    suspend fun getTrackByMusicID(musicId: String, name: String = StringKey.currentList): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE musicTrackId != :key and playListName = :name ORDER BY active DESC, musicTrackId ASC")
    suspend fun sortByActiveAndWithoutItem(
        key: Long,
        name: String = StringKey.currentList
    ): List<MusicTrack>?

    @Query("SELECT * FROM musicTrack_data_base WHERE active == 1 and playListName = :name ORDER BY musicTrackId ASC LIMIT 1 ")
    suspend fun getFirstActive(name: String = StringKey.currentList): MusicTrack?

    @Query("DELETE FROM musicTrack_data_base WHERE playListName = :name")
    suspend fun deletePlaylist(name: String): Int

    @Query("SELECT * from musicTrack_data_base WHERE playListName = :listName ORDER BY musicTrackId ASC")
    suspend fun getPlaylistByName(listName: String): List<MusicTrack>?

    @Query("SELECT * from musicTrack_data_base WHERE active = :active and playListName = :name ORDER BY musicTrackId ASC")
    suspend fun getPlaylistByActive(
        active: Int,
        name: String = StringKey.currentList
    ): List<MusicTrack>?

    @Query("SELECT * from musicTrack_data_base WHERE active = :active and musicId != :musicId and playListName = :name ORDER BY musicTrackId ASC")
    suspend fun getPlaylistByActiveWithOutItem(
        active: Int,
        musicId: String,
        name: String = StringKey.currentList
    ): List<MusicTrack>?

    @Query("SELECT * FROM musicTrack_data_base WHERE playListName = :name ORDER BY musicTrackId ASC LIMIT 1")
    suspend fun getItemASC(name: String = StringKey.currentList): MusicTrack?

    @Query("SELECT * FROM musicTrack_data_base WHERE playListName = :name ORDER BY musicTrackId ASC")
    fun getCurrentItems(name: String = StringKey.currentList): LiveData<List<MusicTrack>>

    @Query("SELECT COUNT(*) FROM musicTrack_data_base WHERE playListName = :name")
    suspend fun getCount(name: String = StringKey.currentList): Int

    @Query("SELECT * FROM musicTrack_data_base WHERE musicTrackId = :key")
    suspend fun getItemById(key: Long): MusicTrack?
}
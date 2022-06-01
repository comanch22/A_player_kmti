package comanch.simpleplayer.dataBase

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PlayListDAO {

    @Insert
    suspend fun insert(playList: PlayList)

    @Update
    suspend fun update(playList: PlayList)

    @Delete
    suspend fun delete(playList: PlayList)

    @Transaction
    suspend fun delByNameAdd(deleteName: String, addItem: PlayList) {
        getByName(deleteName)?.let { delete(it) }
        insert(addItem)
    }

    @Query("SELECT * from playList_data_base WHERE playListId = :key")
    suspend fun get(key: Long): PlayList?

    @Query("SELECT * from playList_data_base WHERE name = :name")
    suspend fun getByName(name: String): PlayList?

    @Query("DELETE FROM playList_data_base")
    suspend fun clear()

    @Query("SELECT * FROM playList_data_base ORDER BY playListId DESC LIMIT 1")
    suspend fun getItem(): PlayList?

    @Query("SELECT * FROM playList_data_base ORDER BY playListId DESC")
    fun getAllItems(): LiveData<List<PlayList>>

    @Query("SELECT * FROM playList_data_base ORDER BY playListId DESC")
    suspend fun getListItems(): List<PlayList>?

    @Query("SELECT COUNT(playListId) FROM playList_data_base")
    suspend fun getCount(): Int?
}
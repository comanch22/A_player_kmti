package comanch.simpleplayer.dataBase

import androidx.lifecycle.LiveData
import androidx.room.*
import comanch.simpleplayer.helpers.StringKey

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

    @Query("SELECT * from playList_data_base WHERE name = :name")
    suspend fun getByName(name: String): PlayList?

    @Query("SELECT * FROM playList_data_base WHERE name != :name ORDER BY playListId DESC")
    fun getAllItems(name: String = StringKey.currentList): LiveData<List<PlayList>>

    @Query("SELECT COUNT(playListId) FROM playList_data_base")
    suspend fun getCount(): Int?
}
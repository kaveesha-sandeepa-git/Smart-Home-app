package com.example.smart_home.database

import androidx.room.*
import com.example.smart_home.models.Iron

@Dao
interface IronDao {

    @Query("SELECT * FROM irons WHERE deviceId = :deviceId")
    fun getIronById(deviceId: String): Iron?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIron(iron: Iron)

    @Update
    fun updateIron(iron: Iron)

    @Query("DELETE FROM irons WHERE deviceId = :deviceId")
    fun deleteIronById(deviceId: String)
}

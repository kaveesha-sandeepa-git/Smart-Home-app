package com.example.smart_home.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.smart_home.models.Floor

@Dao
interface FloorDao {

    @Query("SELECT * FROM floors")
    fun getAllFloors(): LiveData<List<Floor>>

    @Query("SELECT * FROM floors WHERE floorId = :floorId")
    fun getFloorById(floorId: String): LiveData<Floor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFloor(floor: Floor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(floors: List<Floor>)

    @Transaction
    fun replaceAll(floors: List<Floor>) {
        deleteAllFloors()
        insertAll(floors)
    }

    @Update
    fun updateFloor(floor: Floor)

    @Delete
    fun deleteFloor(floor: Floor)

    @Query("DELETE FROM floors")
    fun deleteAllFloors()

    @Query("SELECT COUNT(*) FROM floors")
    fun getFloorsCount(): LiveData<Int>
}

package com.example.smart_home.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smart_home.models.*

@Database(
    entities = [
        Device::class,
        Floor::class,
        DeviceUsageReport::class,
        DeviceUsageSession::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun floorDao(): FloorDao
    abstract fun usageReportDao(): DeviceUsageReportDao
    abstract fun usageSessionDao(): DeviceUsageSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_home_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                
                // Clear tables on version change if needed
                INSTANCE = instance
                instance
            }
        }
    }
}

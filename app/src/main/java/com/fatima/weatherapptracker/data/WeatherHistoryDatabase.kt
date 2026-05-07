package com.fatima.weatherapptracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CityHistoryEntity::class], version = 1, exportSchema = false)
abstract class WeatherHistoryDatabase : RoomDatabase() {
    abstract fun cityHistoryDao(): CityHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherHistoryDatabase? = null

        fun getInstance(context: Context): WeatherHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WeatherHistoryDatabase::class.java,
                    "weather_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

package com.fatima.weatherapptracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CityHistoryDao {
    @Query("SELECT * FROM city_weather_history ORDER BY updatedAtEpochMs DESC")
    suspend fun getAll(): List<CityHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CityHistoryEntity)

    @Query("DELETE FROM city_weather_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM city_weather_history")
    suspend fun clearAll()

    @Query("SELECT id FROM city_weather_history WHERE cityName = :cityName AND countryName = :countryName LIMIT 1")
    suspend fun findId(cityName: String, countryName: String): Long?
}

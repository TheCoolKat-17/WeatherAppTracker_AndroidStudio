package com.fatima.weatherapptracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "city_weather_history",
    indices = [Index(value = ["cityName", "countryName"], unique = true)]
)
data class CityHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cityName: String,
    val countryName: String,
    val temperature: String,
    val degreeType: String,
    val uvIndex: String,
    val windMs: String,
    val humidityPercent: String,
    val updatedAtEpochMs: Long
)

package com.fatima.weatherapptracker

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatima.weatherapptracker.ui.theme.WeatherAppTrackerTheme
import com.fatima.weatherapptracker.data.CityHistoryEntity
import com.fatima.weatherapptracker.data.WeatherHistoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherAppTrackerTheme {
                WeatherTrackerApp(applicationContext)
            }
        }
    }
}

@Composable
private fun WeatherTrackerApp(context: Context) {
    val prefs = remember { CityPreferences(context) }
    val weatherService = remember { OpenMeteoService() }
    val historyDao = remember { WeatherHistoryDatabase.getInstance(context).cityHistoryDao() }
    val cities = remember { mutableStateListOf<TrackedCity>() }
    val weatherByCity = remember { mutableStateMapOf<String, WeatherState>() }
    val historyRows = remember { mutableStateListOf<CityHistoryEntity>() }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var lastRefreshAt by rememberSaveable { mutableStateOf<Long?>(null) }

    fun cityCountry(city: TrackedCity): String = city.countryName ?: "Unknown"

    fun weatherToHistory(city: TrackedCity, weather: WeatherState): CityHistoryEntity {
        val temperatureValue = weather.temperatureC ?: 0.0
        val displayTemp = if (city.useCelsius) temperatureValue else (temperatureValue * 9 / 5) + 32
        val degreeType = if (city.useCelsius) "C" else "F"
        return CityHistoryEntity(
            cityName = city.cityName,
            countryName = cityCountry(city),
            temperature = "%.1f".format(displayTemp),
            degreeType = degreeType,
            uvIndex = weather.uvIndex?.let { "%.1f".format(it) } ?: "Unavailable",
            windMs = weather.windSpeedMs?.let { "%.1f".format(it) } ?: "Unavailable",
            humidityPercent = weather.humidityPercent?.toString() ?: "Unavailable",
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    fun reloadHistory() {
        scope.launch {
            val rows = historyDao.getAll()
            historyRows.clear()
            historyRows.addAll(rows)
        }
    }

    fun refreshCity(city: TrackedCity) {
        scope.launch {
            weatherByCity[city.id] = WeatherState(isLoading = true)
            val result = weatherService.fetchWeather(city.latitude, city.longitude, city.searchName)
            weatherByCity[city.id] = result
            if (result.error == null && result.temperatureC != null) {
                val entry = weatherToHistory(city, result)
                val existingId = historyDao.findId(entry.cityName, entry.countryName)
                historyDao.upsert(entry.copy(id = existingId ?: 0))
                reloadHistory()
            }
        }
    }

    fun refreshAll() {
        lastRefreshAt = System.currentTimeMillis()
        cities.forEach { refreshCity(it) }
    }

    LaunchedEffect(Unit) {
        cities.clear()
        cities.addAll(prefs.getCities())
        reloadHistory()
        refreshAll()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFEAF6FF), Color(0xFFF7FBFF), Color.White)
                    )
                )
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Weather") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Add City") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("My History") })
            }

            when (selectedTab) {
                0 -> WeatherTab(
                    cities = cities,
                    weatherByCity = weatherByCity,
                    lastRefreshAt = lastRefreshAt,
                    onRefreshAll = { refreshAll() },
                    onToggleUnit = { cityId ->
                        val index = cities.indexOfFirst { it.id == cityId }
                        if (index >= 0) {
                            val updated = cities[index].copy(useCelsius = !cities[index].useCelsius)
                            cities[index] = updated
                            prefs.saveCities(cities)
                            val weather = weatherByCity[cityId]
                            if (weather != null && weather.error == null && weather.temperatureC != null) {
                                scope.launch {
                                    val entry = weatherToHistory(updated, weather)
                                    val existingId = historyDao.findId(entry.cityName, entry.countryName)
                                    historyDao.upsert(entry.copy(id = existingId ?: 0))
                                    reloadHistory()
                                }
                            }
                        }
                    },
                    onDeleteCity = { cityId ->
                        val index = cities.indexOfFirst { it.id == cityId }
                        if (index >= 0) {
                            weatherByCity.remove(cityId)
                            cities.removeAt(index)
                            prefs.saveCities(cities)
                        }
                    }
                )

                1 -> AddCityTab(
                    onSearchCities = { query, callback ->
                        scope.launch {
                            val results = weatherService.searchCities(query)
                            if (results.isEmpty()) {
                                callback(emptyList(), "No city matches found.")
                            } else {
                                callback(results, null)
                            }
                        }
                    },
                    onAddCity = { selected ->
                        if (cities.any { it.id == selected.id }) {
                            return@AddCityTab "${selected.fullLabel} is already tracked."
                        }
                        val newCity = TrackedCity(
                            id = selected.id,
                            searchName = selected.name,
                            cityName = selected.name,
                            adminName = selected.admin,
                            countryName = selected.country,
                            latitude = selected.latitude,
                            longitude = selected.longitude
                        )
                        cities.add(newCity)
                        prefs.saveCities(cities)
                        refreshCity(newCity)
                        "${selected.fullLabel} added."
                    },
                    onDone = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )

                else -> HistoryTab(
                    rows = historyRows,
                    onDeleteRow = { id ->
                        scope.launch {
                            historyDao.deleteById(id)
                            reloadHistory()
                        }
                    },
                    onClearAll = {
                        scope.launch {
                            historyDao.clearAll()
                            reloadHistory()
                        }
                    }
                )
            }
        }
        }
    }
}

@Composable
private fun WeatherTab(
    cities: List<TrackedCity>,
    weatherByCity: Map<String, WeatherState>,
    lastRefreshAt: Long?,
    onRefreshAll: () -> Unit,
    onToggleUnit: (String) -> Unit,
    onDeleteCity: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onRefreshAll) {
            Text("Refresh Weather")
        }
        Text(
            text = "Last refresh: ${lastRefreshAt?.let { formatTimestamp(it) } ?: "Not refreshed yet"}",
            color = Color(0xFF355C7D)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cities, key = { it.id }) { city ->
                val weather = weatherByCity[city.id]
                WeatherCard(
                    city = city,
                    weather = weather,
                    onToggleUnit = onToggleUnit,
                    onDeleteCity = onDeleteCity
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCityTab(
    onSearchCities: (String, (List<CitySearchResult>, String?) -> Unit) -> Unit,
    onAddCity: (CitySearchResult) -> String,
    onDone: (String) -> Unit
) {
    var cityInput by rememberSaveable { mutableStateOf("") }
    var info by rememberSaveable { mutableStateOf("Track as many cities as you want.") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    val cityResults = remember { mutableStateListOf<CitySearchResult>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = cityInput,
            onValueChange = { cityInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("City name", color = Color.Black) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color(0xFF2B4D6C),
                unfocusedBorderColor = Color(0xFF5D7A96),
                focusedLabelColor = Color(0xFF1D3348),
                unfocusedLabelColor = Color(0xFF2A4158),
                cursorColor = Color.Black
            )
        )
        Button(onClick = {
            val query = cityInput.trim()
            if (query.isBlank()) {
                info = "Please enter a city name."
                onDone(info)
                return@Button
            }
            isSearching = true
            onSearchCities(query) { results, error ->
                cityResults.clear()
                cityResults.addAll(results)
                info = error ?: "Select the city you want to track."
                isSearching = false
            }
        }) {
            Text(if (isSearching) "Searching..." else "Search Cities")
        }
        Text(info)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxHeight()) {
            items(cityResults, key = { it.id }) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val addMessage = onAddCity(result)
                            info = addMessage
                            onDone(addMessage)
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(result.fullLabel, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Lat: ${"%.4f".format(result.latitude)}, Lon: ${"%.4f".format(result.longitude)}",
                            color = Color(0xFF4F6070)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    rows: List<CityHistoryEntity>,
    onDeleteRow: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Saved weather history", fontWeight = FontWeight.Bold)
            IconButton(onClick = onClearAll) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear history",
                    tint = Color(0xFF7A0000)
                )
            }
        }
        if (rows.isEmpty()) {
            Text("No history yet. Add cities from tab 2 and refresh weather.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.id }) { row ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(row.cityName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    Text(row.countryName, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = { onDeleteRow(row.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete row",
                                        tint = Color(0xFF7A0000)
                                    )
                                }
                            }
                            Text("Temperature: ${row.temperature}°${row.degreeType}")
                            Text("UV Index: ${row.uvIndex}")
                            Text("Wind (m/s): ${row.windMs}")
                            Text("Humidity (%): ${row.humidityPercent}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(
    city: TrackedCity,
    weather: WeatherState?,
    onToggleUnit: (String) -> Unit,
    onDeleteCity: (String) -> Unit
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showTipsDialog by rememberSaveable { mutableStateOf(false) }
    var showPredictionDialog by rememberSaveable { mutableStateOf(false) }
    val gradient = weatherGradient(weather?.weatherCode)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Surface(
            modifier = Modifier.background(brush = gradient),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedWeatherText(
                            text = city.displayName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            isHeader = true
                        )
                        OutlinedWeatherText(
                            text = city.subLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        if (weather != null && !weather.isLoading && weather.error == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedWeatherText(
                                    text = weatherEmoji(weather.weatherCode),
                                    fontSize = 22.sp,
                                    isHeader = true,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                OutlinedWeatherText(
                                    text = weather.weatherType ?: "Unavailable",
                                    fontSize = 22.sp,
                                    isHeader = true,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        IconButton(onClick = { onToggleUnit(city.id) }) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = if (city.useCelsius) "Switch to Fahrenheit" else "Switch to Celsius",
                                tint = Color(0xFF111111)
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete city",
                                tint = Color(0xFF111111)
                            )
                        }
                    }
                }

                when {
                    weather == null || weather.isLoading -> OutlinedWeatherText("Loading weather...", fontWeight = FontWeight.Bold)
                    weather.error != null -> OutlinedWeatherText(weather.error, color = Color(0xFF7A0000), fontWeight = FontWeight.ExtraBold)
                    weather.temperatureC != null -> {
                        val temp = if (city.useCelsius) weather.temperatureC else (weather.temperatureC * 9 / 5) + 32
                        val suffix = if (city.useCelsius) "°C" else "°F"
                        OutlinedWeatherText(
                            text = "Temperature: ${"%.1f".format(temp)} $suffix",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        OutlinedWeatherText(
                            "UV Index: ${weather.uvIndex?.let { "%.1f".format(it) } ?: "Unavailable"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        OutlinedWeatherText(
                            "Wind: ${weather.windSpeedMs?.let { "%.1f".format(it) } ?: "Unavailable"} m/s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        OutlinedWeatherText(
                            "Humidity: ${weather.humidityPercent?.let { "$it%" } ?: "Unavailable"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedWeatherText(
                        "Time in City: ${weather?.cityTime24h ?: "Unavailable"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { showPredictionDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timeline,
                                contentDescription = "Weather prediction",
                                tint = Color(0xFF111111)
                            )
                        }
                        IconButton(
                            onClick = { showTipsDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = "Weather recommendations",
                                tint = Color(0xFF111111)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete city?") },
            text = { Text("Remove ${city.displayName} from tracked cities?") },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteCity(city.id)
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }

    if (showTipsDialog) {
        val recommendations = buildRecommendations(weather)
        AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            title = { Text("Smart recommendations") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recommendations.forEach { tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "[${tip.severity}]",
                                color = severityColor(tip.severity),
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = tip.message)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTipsDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showPredictionDialog) {
        val confidence = weather?.predictionConfidence ?: "Medium"
        AlertDialog(
            onDismissRequest = { showPredictionDialog = false },
            title = { Text("Today/Tonight outlook") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confidence in Prediction: $confidence", fontWeight = FontWeight.Bold)
                    Text(
                        weather?.predictionSummary
                            ?: "Refresh weather to generate a forecast outlook."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPredictionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

data class TrackedCity(
    val id: String,
    val searchName: String,
    val cityName: String,
    val adminName: String? = null,
    val countryName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val useCelsius: Boolean = true
) {
    val displayName: String
        get() = cityName

    val subLabel: String
        get() = listOfNotNull(adminName, countryName).joinToString(", ").ifBlank { "Unknown region" }
}

data class CitySearchResult(
    val id: String,
    val name: String,
    val admin: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double
) {
    val fullLabel: String
        get() = listOfNotNull(name, admin, country).joinToString(", ")
}

data class WeatherState(
    val temperatureC: Double? = null,
    val uvIndex: Double? = null,
    val weatherCode: Int? = null,
    val weatherType: String? = null,
    val windSpeedMs: Double? = null,
    val humidityPercent: Int? = null,
    val isDay: Boolean? = null,
    val cityTime24h: String? = null,
    val predictionSummary: String? = null,
    val predictionConfidence: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

private class CityPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("city_preferences", Context.MODE_PRIVATE)

    fun getCities(): List<TrackedCity> {
        if (prefs.getBoolean(KEY_FIRST_RUN, true)) {
            val defaultCities = listOf(
                TrackedCity("cape-town", "Cape Town", "Cape Town", countryName = "South Africa", latitude = -33.9249, longitude = 18.4241),
                TrackedCity("london", "London", "London", countryName = "United Kingdom", latitude = 51.5072, longitude = -0.1276),
                TrackedCity("new-york", "New York", "New York", countryName = "United States", latitude = 40.7128, longitude = -74.0060),
                TrackedCity("tokyo", "Tokyo", "Tokyo", countryName = "Japan", latitude = 35.6762, longitude = 139.6503),
                TrackedCity("bangkok", "Bangkok", "Bangkok", countryName = "Thailand", latitude = 13.7563, longitude = 100.5018)
            )
            saveCities(defaultCities)
            prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
            return defaultCities
        }

        val jsonText = prefs.getString(KEY_CITY_LIST_JSON, "").orEmpty()
        if (jsonText.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonText)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cityId = obj.getString("id")
                    add(
                        TrackedCity(
                            id = cityId,
                            searchName = obj.getString("searchName"),
                            cityName = obj.getString("cityName"),
                            adminName = obj.optString("adminName", "").ifBlank { null },
                            countryName = obj.optString("countryName", "").ifBlank { null },
                            latitude = obj.getDouble("latitude"),
                            longitude = obj.getDouble("longitude"),
                            useCelsius = obj.optBoolean("useCelsius", true)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCities(cities: List<TrackedCity>) {
        val array = JSONArray()
        cities.forEach { city ->
            val obj = JSONObject()
                .put("id", city.id)
                .put("searchName", city.searchName)
                .put("cityName", city.cityName)
                .put("adminName", city.adminName ?: "")
                .put("countryName", city.countryName ?: "")
                .put("latitude", city.latitude)
                .put("longitude", city.longitude)
                .put("useCelsius", city.useCelsius)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CITY_LIST_JSON, array.toString()).apply()
    }

    companion object {
        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_CITY_LIST_JSON = "tracked_city_list_json"
    }
}

private class OpenMeteoService {
    private val client = OkHttpClient()

    suspend fun fetchWeather(latitude: Double, longitude: Double, cityName: String): WeatherState = withContext(Dispatchers.IO) {
        try {
            val weatherRequest = Request.Builder()
                .url(
                    "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=$latitude&longitude=$longitude&" +
                        "current=temperature_2m,uv_index,weather_code,wind_speed_10m,relative_humidity_2m,is_day&" +
                        "hourly=temperature_2m,weather_code&timezone=auto"
                )
                .build()
            val weatherBody = client.newCall(weatherRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext WeatherState(error = "Weather unavailable for $cityName.")
                }
                response.body?.string().orEmpty()
            }

            val current = JSONObject(weatherBody).optJSONObject("current")
                ?: return@withContext WeatherState(error = "Weather unavailable for $cityName.")
            val hourly = JSONObject(weatherBody).optJSONObject("hourly")

            val code = if (current.has("weather_code")) current.optInt("weather_code") else null
            WeatherState(
                temperatureC = if (current.has("temperature_2m")) current.optDouble("temperature_2m") else null,
                uvIndex = if (current.has("uv_index")) current.optDouble("uv_index") else null,
                weatherCode = code,
                weatherType = code?.let { mapWeatherCode(it) },
                windSpeedMs = if (current.has("wind_speed_10m")) current.optDouble("wind_speed_10m") else null,
                humidityPercent = if (current.has("relative_humidity_2m")) current.optInt("relative_humidity_2m") else null,
                isDay = if (current.has("is_day")) current.optInt("is_day") == 1 else null,
                cityTime24h = currentCityTime(weatherBody),
                predictionSummary = buildPredictionSummary(current, hourly),
                predictionConfidence = buildPredictionConfidence(current, hourly)
            )
        } catch (_: Exception) {
            WeatherState(error = "Failed to load weather for $cityName.")
        }
    }

    suspend fun searchCities(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext emptyList()
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val req = Request.Builder()
                .url("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=20&language=en&format=json")
                .build()
            val responseBody = client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                response.body?.string().orEmpty()
            }
            val json = JSONObject(responseBody)
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val name = item.optString("name").ifBlank { continue }
                    val admin = item.optString("admin1", "").ifBlank { null }
                    val country = item.optString("country", "").ifBlank { null }
                    val lat = item.optDouble("latitude")
                    val lon = item.optDouble("longitude")
                    val id = "$name|${country.orEmpty()}|${admin.orEmpty()}|$lat|$lon"
                    add(
                        CitySearchResult(
                            id = id,
                            name = name,
                            admin = admin,
                            country = country,
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private fun mapWeatherCode(code: Int): String = when (code) {
    0 -> "Sunny"
    1, 2 -> "Partly Cloudy"
    3 -> "Cloudy"
    45, 48 -> "Foggy"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 80, 81, 82 -> "Raining"
    66, 67 -> "Freezing Rain"
    71, 73, 75, 77, 85, 86 -> "Snowing"
    95, 96, 99 -> "Storming"
    else -> "Unknown"
}

private fun weatherGradient(weatherCode: Int?): Brush {
    val palette = when (weatherCode) {
        0 -> listOf(Color(0xFFFFF4BF), Color(0xFFFFE58A))
        1, 2 -> listOf(Color(0xFFEFE7AF), Color(0xFFF8EEB7))
        3, 45, 48 -> listOf(Color(0xFFE2E6EB), Color(0xFFD0D6DE))
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> listOf(Color(0xFFCFE8FF), Color(0xFFAED5FB))
        71, 73, 75, 77, 85, 86 -> listOf(Color(0xFFE9F3FF), Color(0xFFDCE3EC))
        95, 96, 99 -> listOf(Color(0xFF6B7480), Color(0xFF454C55))
        else -> listOf(Color(0xFFE2E6EB), Color(0xFFD0D6DE))
    }
    return Brush.verticalGradient(palette)
}

private fun formatTimestamp(epochMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

@Composable
private fun OutlinedWeatherText(
    text: String,
    color: Color = Color(0xFF111111),
    fontWeight: FontWeight = FontWeight.Bold,
    fontSize: TextUnit = 16.sp,
    isHeader: Boolean = false
) {
    val style = TextStyle(
        color = color,
        fontWeight = if (isHeader) FontWeight.ExtraBold else fontWeight,
        fontSize = fontSize
    )
    Box {
        Text(
            text = text,
            style = style.copy(
                color = Color.White,
                drawStyle = Stroke(width = 3f)
            )
        )
        Text(text = text, style = style)
    }
}

private fun weatherEmoji(weatherCode: Int?): String = when (weatherCode) {
    0 -> "☀"
    1, 2 -> "⛅"
    3, 45, 48 -> "☁"
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧"
    71, 73, 75, 77, 85, 86 -> "❄"
    95, 96, 99 -> "⛈"
    else -> "🌤"
}

private data class Recommendation(
    val severity: String,
    val message: String
)

private fun severityColor(level: String): Color = when (level) {
    "HIGH" -> Color(0xFFB00020)
    "MEDIUM" -> Color(0xFFB26A00)
    else -> Color(0xFF1B7F3B)
}

private fun buildRecommendations(weather: WeatherState?): List<Recommendation> {
    if (weather == null || weather.isLoading || weather.error != null) {
        return listOf(Recommendation("LOW", "Refresh weather for this city to get recommendations."))
    }
    val tips = mutableListOf<Recommendation>()
    val code = weather.weatherCode
    val isDayTime = weather.isDay == true

    if (code in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)) {
        tips += Recommendation("HIGH", "It may rain; carry a rain-jacket or umbrella.")
    }
    if (weather.uvIndex != null && weather.uvIndex > 3.0 && isDayTime) {
        val severity = if (weather.uvIndex >= 6.0) "HIGH" else "MEDIUM"
        tips += Recommendation(severity, "UV is elevated; carry sunblock and apply frequently.")
    } else if (weather.uvIndex != null && weather.uvIndex > 3.0 && !isDayTime) {
        tips += Recommendation("LOW", "UV is elevated, but it is currently nighttime. Prioritize daytime sun protection.")
    }
    if (code in setOf(71, 73, 75, 77, 85, 86)) {
        tips += Recommendation("HIGH", "Snow conditions expected; wear warm inner layers.")
    }
    if (code == 0) {
        tips += Recommendation("MEDIUM", "Sunny conditions; carry a sunhat.")
    }
    if (weather.windSpeedMs != null && weather.windSpeedMs > 10.0) {
        val severity = if (weather.windSpeedMs > 15.0) "HIGH" else "MEDIUM"
        tips += Recommendation(severity, "Strong wind; avoid loose clothing and carry a jacket.")
    }
    if (weather.humidityPercent != null && weather.humidityPercent > 60) {
        tips += Recommendation("MEDIUM", "Humidity is high; remember to stay hydrated.")
    }
    if (tips.isEmpty()) {
        tips += Recommendation("LOW", "Weather is moderate; stay comfortable and hydrated.")
    }
    return tips
}

private fun buildPredictionSummary(current: JSONObject, hourly: JSONObject?): String {
    if (hourly == null) return "Forecast outlook is unavailable right now."
    val currentTime = current.optString("time")
    val isDay = current.optInt("is_day", -1) == 1
    val times = hourly.optJSONArray("time") ?: return "Forecast outlook is unavailable right now."
    val codes = hourly.optJSONArray("weather_code") ?: return "Forecast outlook is unavailable right now."
    val temps = hourly.optJSONArray("temperature_2m")

    val startIndex = (0 until times.length()).firstOrNull { i ->
        val hourlyTime = times.optString(i)
        hourlyTime == currentTime || hourlyTime.startsWith(currentTime.take(13))
    } ?: 0
    val endExclusive = (startIndex + 8).coerceAtMost(times.length())
    if (startIndex >= endExclusive) return "Forecast outlook is unavailable right now."

    val periodLabel = if (isDay) "rest of today" else "rest of tonight"
    var rainHits = 0
    var snowHits = 0
    var stormHits = 0
    var clearHits = 0
    var minTemp = Double.POSITIVE_INFINITY
    var maxTemp = Double.NEGATIVE_INFINITY

    for (i in startIndex until endExclusive) {
        val code = codes.optInt(i, -1)
        when {
            code in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) -> rainHits++
            code in setOf(71, 73, 75, 77, 85, 86) -> snowHits++
            code in setOf(95, 96, 99) -> stormHits++
            code == 0 || code == 1 || code == 2 -> clearHits++
        }
        if (temps != null) {
            val temp = temps.optDouble(i, Double.NaN)
            if (!temp.isNaN()) {
                minTemp = minOf(minTemp, temp)
                maxTemp = maxOf(maxTemp, temp)
            }
        }
    }

    val trend = when {
        stormHits > 0 -> "Possible stormy spells"
        snowHits > 0 -> "Snow is likely"
        rainHits > 0 -> "Showers are likely"
        clearHits >= 4 -> "Mostly clear conditions"
        else -> "Mixed weather conditions"
    }
    val tempPart = if (minTemp.isFinite() && maxTemp.isFinite()) {
        " with temperatures between ${"%.1f".format(minTemp)}°C and ${"%.1f".format(maxTemp)}°C."
    } else {
        "."
    }
    return "For the $periodLabel: $trend$tempPart"
}

private fun buildPredictionConfidence(current: JSONObject, hourly: JSONObject?): String {
    if (hourly == null) return "Medium"
    val currentTime = current.optString("time")
    val times = hourly.optJSONArray("time") ?: return "Medium"
    val codes = hourly.optJSONArray("weather_code") ?: return "Medium"

    val startIndex = (0 until times.length()).firstOrNull { i ->
        val hourlyTime = times.optString(i)
        hourlyTime == currentTime || hourlyTime.startsWith(currentTime.take(13))
    } ?: 0
    val endExclusive = (startIndex + 8).coerceAtMost(times.length())
    if (startIndex >= endExclusive) return "Medium"

    val buckets = mutableMapOf("rain" to 0, "snow" to 0, "storm" to 0, "clear" to 0, "other" to 0)
    for (i in startIndex until endExclusive) {
        val code = codes.optInt(i, -1)
        when {
            code in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) -> buckets["rain"] = buckets.getValue("rain") + 1
            code in setOf(71, 73, 75, 77, 85, 86) -> buckets["snow"] = buckets.getValue("snow") + 1
            code in setOf(95, 96, 99) -> buckets["storm"] = buckets.getValue("storm") + 1
            code == 0 || code == 1 || code == 2 -> buckets["clear"] = buckets.getValue("clear") + 1
            else -> buckets["other"] = buckets.getValue("other") + 1
        }
    }
    val dominant = buckets.values.maxOrNull() ?: return "Medium"
    val ratio = dominant.toDouble() / (endExclusive - startIndex).toDouble()
    return when {
        ratio >= 0.70 -> "High"
        ratio >= 0.45 -> "Medium"
        else -> "Low"
    }
}

private fun currentCityTime(weatherBody: String): String {
    return try {
        val timezone = JSONObject(weatherBody).optString("timezone")
        if (timezone.isBlank()) return "Unavailable"
        val zoneId = ZoneId.of(timezone)
        ZonedDateTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        "Unavailable"
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    WeatherAppTrackerTheme {
        Spacer(modifier = Modifier.height(1.dp))
    }
}
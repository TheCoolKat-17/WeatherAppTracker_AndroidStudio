# WeatherAppTracker 🌦️

WeatherAppTracker is a mobile weather application developed in Android Studio as part of the ISTN3MT assignment 2 focused on cloud computing, mobile technologies, CRUD functionality, and intelligent mobile systems.

The application allows users to search for cities worldwide, save weather locations, monitor live weather conditions, and receive smart contextual recommendations based on environmental conditions.

### Student Name; Fatima-Zahra Amod
### Student Number: 224147XXX

---

# Application Overview

WeatherAppTracker combines real-time weather tracking with intelligent recommendation features to improve user decision-making and overall mobile experience.

The app demonstrates:
- CRUD operations
- Cloud/API integration
- Intelligent recommendation logic
- Mobile-first UI design
- Real-time weather monitoring
- Local database management using Room Database

---

# Assignment Objective

The assignment required the design and development of a polyglot mobile application operating within the cloud computing and/or mobile technology domain.

The application needed to implement the following CRUD functionalities:

| CRUD Operation Implementation |

```
'Create' Save/Add weather locations.
'Read' Display weather information for saved locations and database history.
'Update' Refresh weather data, toggle temperature units, update recommendations, and update database records.
'Delete' Remove saved weather cards/cities and delete database history entries.
```
The application also incorporates emerging technology concepts through intelligent contextual recommendations.

---

# Features

The application contains three main tabs:

1. **Weather**
2. **Add City**
3. **My History**

---

# Tab 1 — Weather

The **Weather** tab displays all saved weather cards.

Each weather card contains:
- City name and country
- Current weather condition
- Temperature
- UV Index
- Wind speed (m/s)
- Humidity (%)
- Local time of the city

---

## Tab 1 — Weather Card Controls

Each weather card includes multiple interactive controls.

### 🔄 Temperature Toggle
Allows users to switch between:
- Celsius (°C)
- Fahrenheit (°F)

### 🗑️ Delete City
Removes the selected weather card from saved locations.

### 📈 Forecast Feature
Displays forecast predictions for:
- Upcoming weather conditions
- Temperature trends throughout the day/night

### 💡 Smart Recommendations
Provides contextual recommendations based on live weather conditions.

Examples include:
- Carry an umbrella during rain
- Apply sunscreen during high UV levels
- Dress warmly during cold temperatures
- Stay hydrated during hot weather
- Exercise caution during strong winds

This feature simulates lightweight Artificial Intelligence through condition-based decision logic.

---

## Tab 1 — Refresh Weather Functionality

The Weather tab also includes:
- A **Refresh Weather** button
- A **Last Refresh Time** indicator

This allows users to update all saved weather cards with the latest live weather information from the weather API.

When the refresh button is clicked:
- All weather cards are updated
- Weather data stored in the Room Database is also updated

---

# Tab 2 — Add City

The **Add City** tab allows users to:
- Search for cities worldwide
- View dynamically matching search results
- Save selected cities to the Weather tab

Once saved:
- The city automatically appears as a weather card in the Weather tab
- The city data is also inserted into the Room Database

---

# Tab 3 — My History

The **My History** tab displays a history of all cities that have been:
- Saved to the Weather tab
- Saved and later deleted from the Weather tab

This tab reads weather information directly from the Room Database.

---

## Tab 3 — Database Information Stored

Each weather history card contains:
- City name
- Country name
- Temperature
- Temperature unit (°C or °F)
- UV Index
- Wind speed (m/s)
- Humidity (%)

The Room Database stores and manages this information locally within the application.

---

## Tab 3 — History Management Features

Users can:
- Delete individual weather history cards
- Delete all weather history cards at once

This demonstrates additional CRUD functionality through local database management.

---

# Intelligent Recommendation System

WeatherAppTracker includes a lightweight intelligent recommendation engine.

The system analyses:
- Temperature
- UV Index
- Humidity
- Wind speed
- Weather conditions

Based on these environmental factors, the application generates contextual recommendations in real-time to assist users with daily planning and safety awareness.

This demonstrates:
- Intelligent mobile interaction
- Decision-based recommendation systems
- Emerging technology integration
- Context-aware computing

---

# Technologies Used

- Android Studio
- Kotlin / Java
- REST API Integration
- Weather API Services
- Room Database
- Cloud-based Weather Data
- Intelligent Recommendation Logic
- Mobile UI Components

---

# Key Functionalities

- CRUD Functionality  
- Real-Time Weather Data  
- Intelligent Weather Recommendations  
- Dynamic City Search  
- Forecast Predictions  
- Temperature Unit Conversion  
- Interactive Weather Cards  
- Cloud/API Integration  
- Room Database Integration  
- Weather History Management  
- Mobile-Optimised User Interface  

---

# Database Functionality

The application uses **Room Database** to:
- Store saved weather information locally
- Maintain weather history records
- Update weather information after refresh actions
- Retrieve weather history cards for display
- Delete single or multiple database entries

This provides persistent local storage functionality within the application.

---

# User Interface

The application uses a modern card-based design to provide:
- Clean mobile interaction
- Easy readability
- Efficient weather monitoring
- Fast access to controls and features

---

# Future Improvements

Potential future enhancements include:
- Push weather notifications
- GPS location detection
- Voice assistant integration
- Severe weather alerts
- Dark mode support
- AI-powered clothing recommendations
- Weather analytics dashboard
- Offline weather caching

---

# Developer

Developed as part of a university mobile application development assignment focused on:
- Cloud Computing
- Mobile Technologies
- CRUD Operations
- Intelligent Mobile Systems
- Emerging Technologies
- User-Centred Mobile Design
---

# API's Used

- Geocoding API
> https://geocoding-api.open-meteo.com/v1/search
>> Used in Tab 2 search to find matching cities (including same-name cities in different countries/regions).

- Forecast API
> https://api.open-meteo.com/v1/forecast
>> Used in Tab 1 weather cards for current weather data and hourly prediction inputs (temperature, UV, weather code, wind, humidity, day/night, hourly forecast fields).

## Android Studio, app uses platform/framework APIs/libraries:

- Room (local DB for history tab)
- SharedPreferences (saved tracked-city state)
- OkHttp (HTTP client for API calls)
- Jetpack Compose / Material3 (UI)

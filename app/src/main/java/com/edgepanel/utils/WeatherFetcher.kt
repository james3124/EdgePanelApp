package com.edgepanel.utils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class WeatherData(
    val temp: String = "--°C",
    val description: String = "Unknown",
    val location: String = "Unknown",
    val humidity: String = "--%",
    val wind: String = "-- km/h"
)

object WeatherFetcher {

    // Replace with your OpenWeatherMap API key
    private const val API_KEY = "b7155ece80003c3737e3e72a4666b68e"
    private val executor = Executors.newSingleThreadExecutor()

    fun fetch(context: Context, callback: (WeatherData) -> Unit) {
        executor.execute {
            try {
                val location = getLastLocation(context)
                if (location != null) {
                    fetchByCoords(location.latitude, location.longitude, callback)
                } else {
                    fetchByCity("London", callback)
                }
            } catch (e: Exception) {
                callback(WeatherData(description = "Could not load weather"))
            }
        }
    }

    private fun getLastLocation(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (_: SecurityException) { null }
    }

    private fun fetchByCoords(lat: Double, lon: Double, callback: (WeatherData) -> Unit) {
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$API_KEY&units=metric"
        fetchFromUrl(url, callback)
    }

    private fun fetchByCity(city: String, callback: (WeatherData) -> Unit) {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$API_KEY&units=metric"
        fetchFromUrl(url, callback)
    }

    private fun fetchFromUrl(urlStr: String, callback: (WeatherData) -> Unit) {
        try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val temp = "${json.getJSONObject("main").getDouble("temp").toInt()}°C"
            val desc = json.getJSONArray("weather").getJSONObject(0).getString("description")
                .replaceFirstChar { it.uppercase() }
            val locationName = json.getString("name")
            val humidity = "${json.getJSONObject("main").getInt("humidity")}%"
            val wind = "${json.getJSONObject("wind").getDouble("speed").toInt()} km/h"

            callback(WeatherData(temp, desc, locationName, humidity, wind))
        } catch (e: Exception) {
            callback(WeatherData(description = "Weather unavailable"))
        }
    }
}

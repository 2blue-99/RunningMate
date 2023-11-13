package com.running.data.remote.dataSource
import com.running.data.remote.model.WeatherData
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface GetWeatherDataSource {
    @GET("1360000/VilageFcstInfoService_2.0/getUltraSrtNcst")
    suspend fun getWeatherData(@QueryMap data: HashMap<String, String>): WeatherData

}
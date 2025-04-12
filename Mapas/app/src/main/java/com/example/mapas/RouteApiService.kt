package com.example.mapas

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface RouteApiService {
    @GET("v2/directions/driving-car")
    suspend fun getRoute(
        @Query("api_key") apiKey: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): RouteResponse

    @GET("geocode/search")
    suspend fun geocodeAddress(
        @Query("api_key") apiKey: String,
        @Query("text") address: String
    ): GeocodeResponse

    companion object {
        const val BASE_URL = "https://api.openrouteservice.org/"
        const val API_KEY = "5b3ce3597851110001cf6248de72e902472a4f07aaa5afa1771672cd" // Tu API key de OpenRouteService

        fun create(): RouteApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(RouteApiService::class.java)
        }
    }
}

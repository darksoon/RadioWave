package de.radiowave.core.network

import de.radiowave.core.network.dto.RadioBrowserCountry
import de.radiowave.core.network.dto.RadioBrowserStation
import de.radiowave.core.network.dto.RadioBrowserTag
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Radio Browser API.
 */
interface RadioBrowserApi {

    @GET("json/stations/bynameexact/{name}")
    suspend fun searchByNameExact(
        @Path("name") name: String,
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<RadioBrowserStation>

    @GET("json/stations/byname/{name}")
    suspend fun searchByName(
        @Path("name") name: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<RadioBrowserStation>

    @GET("json/stations/bytag/{tag}")
    suspend fun searchByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<RadioBrowserStation>

    @GET("json/stations/bycountrycodeexact/{code}")
    suspend fun searchByCountry(
        @Path("code") countryCode: String,
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<RadioBrowserStation>

    @GET("json/stations/topclick/{count}")
    suspend fun getTopStations(
        @Path("count") count: Int = 100,
    ): List<RadioBrowserStation>

    @GET("json/tags")
    suspend fun getTags(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 100,
    ): List<RadioBrowserTag>

    @GET("json/countries")
    suspend fun getCountries(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
    ): List<RadioBrowserCountry>

    @POST("json/url/{stationuuid}")
    suspend fun registerClick(
        @Path("stationuuid") uuid: String,
    )

    @POST("json/vote/station/{stationuuid}")
    suspend fun reportBrokenStream(
        @Path("stationuuid") uuid: String,
    )
}

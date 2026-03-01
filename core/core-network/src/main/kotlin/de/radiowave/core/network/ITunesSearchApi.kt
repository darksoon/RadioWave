package de.radiowave.core.network

import de.radiowave.core.network.dto.ITunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for iTunes Search API.
 */
interface ITunesSearchApi {

    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("limit") limit: Int = 1,
    ): ITunesSearchResponse
}
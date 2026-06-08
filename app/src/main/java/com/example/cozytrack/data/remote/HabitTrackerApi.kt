package com.example.cozytrack.data.remote

import com.example.cozytrack.data.remote.dto.AuthCredentialsDto
import com.example.cozytrack.data.remote.dto.AuthTokenResponseDto
import com.example.cozytrack.data.remote.dto.ClockResponseDto
import com.example.cozytrack.data.remote.dto.CreateHabitRequestDto
import com.example.cozytrack.data.remote.dto.HabitCheckRequestDto
import com.example.cozytrack.data.remote.dto.HabitDto
import com.example.cozytrack.data.remote.dto.HabitEntryDto
import com.example.cozytrack.data.remote.dto.HabitProgressDto
import com.example.cozytrack.data.remote.dto.HabitUpdateRequestDto
import com.example.cozytrack.data.remote.dto.SignUpRequestDto
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface HabitTrackerApi {
    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequestDto): AuthTokenResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthCredentialsDto): AuthTokenResponseDto

    @GET("api/me")
    suspend fun getMe(): JsonElement

    @GET("api/clock")
    suspend fun getClock(): ClockResponseDto

    @POST("api/habits")
    suspend fun createHabit(@Body request: CreateHabitRequestDto): ResponseBody

    @GET("api/users/{userId}/habits")
    suspend fun getHabits(
        @Path("userId") userId: String,
        @Query("includeArchived") includeArchived: Boolean = false
    ): List<HabitDto>

    @GET("api/habits/{habitId}")
    suspend fun getHabit(@Path("habitId") habitId: String): HabitDto

    @PUT("api/habits/{habitId}")
    suspend fun updateHabit(
        @Path("habitId") habitId: String,
        @Body request: HabitUpdateRequestDto
    ): ResponseBody

    @DELETE("api/habits/{habitId}")
    suspend fun deleteHabit(@Path("habitId") habitId: String): Response<Unit>

    @POST("api/habits/check")
    suspend fun checkHabit(@Body request: HabitCheckRequestDto): ResponseBody

    @GET("api/users/{userId}/entries/{checkDate}")
    suspend fun getEntriesForDay(
        @Path("userId") userId: String,
        @Path("checkDate") checkDate: String
    ): List<HabitEntryDto>

    @GET("api/habits/{habitId}/entries")
    suspend fun getHabitEntries(
        @Path("habitId") habitId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): List<HabitEntryDto>

    @GET("api/habits/{habitId}/progress")
    suspend fun getHabitProgress(
        @Path("habitId") habitId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): HabitProgressDto

    @GET("api/thought")
    suspend fun getThought(): JsonElement
}

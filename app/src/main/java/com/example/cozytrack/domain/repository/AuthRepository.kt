package com.example.cozytrack.domain.repository

import com.example.cozytrack.core.network.ApiResult
import com.example.cozytrack.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val accessToken: Flow<String?>
    val userId: Flow<String?>

    suspend fun signUp(email: String, password: String, name: String): ApiResult<AuthSession>
    suspend fun login(email: String, password: String): ApiResult<AuthSession>
    suspend fun getCurrentUserName(): ApiResult<String>
    suspend fun logout()
}

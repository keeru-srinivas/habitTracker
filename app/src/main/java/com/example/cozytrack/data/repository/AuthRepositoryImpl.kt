package com.example.cozytrack.data.repository

import com.example.cozytrack.core.network.ApiResult
import com.example.cozytrack.core.session.SessionManager
import com.example.cozytrack.data.remote.HabitTrackerApi
import com.example.cozytrack.data.remote.dto.AuthCredentialsDto
import com.example.cozytrack.data.remote.dto.AuthTokenResponseDto
import com.example.cozytrack.data.remote.dto.SignUpRequestDto
import com.example.cozytrack.domain.model.AuthSession
import com.example.cozytrack.domain.repository.AuthRepository
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val api: HabitTrackerApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    override val accessToken: Flow<String?> = sessionManager.accessToken
    override val userId: Flow<String?> = sessionManager.userId

    override suspend fun signUp(
        email: String,
        password: String,
        name: String
    ): ApiResult<AuthSession> {
        return safeAuthCall {
            api.signUp(SignUpRequestDto(email = email, password = password, name = name))
        }
    }

    override suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        return safeAuthCall {
            api.login(AuthCredentialsDto(email = email, password = password))
        }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun getCurrentUserName(): ApiResult<String> {
        return try {
            val name = api.getMe().toDisplayName()
            ApiResult.Success(name ?: "friend")
        } catch (error: Exception) {
            ApiResult.Error(error.toAuthMessage())
        }
    }

    private suspend fun safeAuthCall(
        call: suspend () -> AuthTokenResponseDto
    ): ApiResult<AuthSession> {
        return try {
            val session = call().toDomain()
            sessionManager.saveSession(
                accessToken = session.accessToken,
                userId = session.userId
            )
            ApiResult.Success(session)
        } catch (error: Exception) {
            ApiResult.Error(error.toAuthMessage())
        }
    }
}

private fun Exception.toAuthMessage(): String {
    if (this is UnknownHostException) {
        return "Cannot reach the CozyTrack server. Check that your emulator/device has internet access and DNS is working."
    }

    if (this is SocketTimeoutException) {
        return "The CozyTrack server took too long to respond. Please try again."
    }

    if (this is HttpException) {
        val errorBody = response()?.errorBody()?.string().orEmpty()
        if (code() == 401 && errorBody.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "Invalid email or password. If you do not have an account yet, please sign up first."
        }

        return "Authentication failed: HTTP ${code()}"
    }

    return message ?: "Authentication failed"
}

private fun AuthTokenResponseDto.toDomain(): AuthSession {
    return AuthSession(
        accessToken = accessToken,
        expiresIn = expiresIn,
        tokenType = tokenType,
        userId = userId
    )
}

private fun JsonElement.toDisplayName(): String? {
    val primitiveName = (this as? JsonPrimitive)?.contentOrNull
    val json = runCatching { jsonObject }.getOrNull()

    return primitiveName
        ?: json.readFirstString("name", "displayName", "fullName", "username", "email")
}

private fun JsonObject?.readFirstString(vararg keys: String): String? {
    if (this == null) return null
    return keys.firstNotNullOfOrNull { key ->
        this[key]?.jsonPrimitive?.contentOrNull
    }
}

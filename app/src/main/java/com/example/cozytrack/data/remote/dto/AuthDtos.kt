package com.example.cozytrack.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthCredentialsDto(
    val email: String,
    val password: String
)

@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class AuthTokenResponseDto(
    val accessToken: String,
    val expiresIn: String,
    val tokenType: String,
    val userId: String
)

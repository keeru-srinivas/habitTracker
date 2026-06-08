package com.example.cozytrack.domain.model

data class AuthSession(
    val accessToken: String,
    val expiresIn: String,
    val tokenType: String,
    val userId: String
)

package com.example.cozytrack.domain.repository

import com.example.cozytrack.core.network.ApiResult
import com.example.cozytrack.domain.model.Thought

interface ThoughtRepository {
    suspend fun getThought(): ApiResult<Thought>
}

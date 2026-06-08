package com.example.cozytrack.data.repository

import com.example.cozytrack.core.network.ApiResult
import com.example.cozytrack.data.remote.HabitTrackerApi
import com.example.cozytrack.domain.model.Thought
import com.example.cozytrack.domain.repository.ThoughtRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ThoughtRepositoryImpl(
    private val api: HabitTrackerApi
) : ThoughtRepository {
    override suspend fun getThought(): ApiResult<Thought> {
        return try {
            val response = api.getThought()
            ApiResult.Success(response.toThought())
        } catch (error: Exception) {
            ApiResult.Error(error.message ?: "Could not load thought")
        }
    }
}

private fun kotlinx.serialization.json.JsonElement.toThought(): Thought {
    val primitiveText = (this as? JsonPrimitive)?.contentOrNull
    val json = runCatching { jsonObject }.getOrNull()

    return Thought(
        quote = primitiveText
            ?: json.readFirstString("quote", "thought", "text", "q")
            ?: "Keep going. Small steps count.",
        author = json.readFirstString("author", "a")
    )
}

private fun JsonObject?.readFirstString(vararg keys: String): String? {
    if (this == null) return null
    return keys.firstNotNullOfOrNull { key ->
        this[key]?.jsonPrimitive?.contentOrNull
    }
}

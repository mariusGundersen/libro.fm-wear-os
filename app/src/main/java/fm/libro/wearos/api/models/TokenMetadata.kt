package fm.libro.wearos.api.models

import com.google.gson.annotations.SerializedName

data class TokenMetadata(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("created_at") val createdAt: Long,
)

package com.bskai.account

import com.google.gson.annotations.SerializedName

/**
 * AURA 沙盒服务器账号 API 数据模型(对齐策划书 9.1/9.2/9.3)。
 */
data class AuraUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("account_name") val accountName: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("official") val official: Boolean? = null,
)

data class AuraTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("official") val official: Boolean? = null,
    @SerializedName("user") val user: AuraUserDto? = null,
    @SerializedName("created") val created: Boolean? = null,
)

data class OAuthAuthorizeResponse(
    @SerializedName("state") val state: String,
    @SerializedName("authorize_url") val authorizeUrl: String,
    @SerializedName("provider") val provider: String,
)

data class OfficialLoginRequest(
    @SerializedName("id") val id: String,
    @SerializedName("account_name") val accountName: String,
    @SerializedName("password") val password: String,
)

data class AuraErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
)

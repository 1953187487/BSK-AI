package com.lingxi.ai.account

import retrofit2.http.*

/**
 * AURA 沙盒服务器 API 接口定义(对齐策划书 9.1/9.2/9.3)。
 *
 * 客户端侧职责:
 * - 普通账号:调 authorize 拿 state,用 SDK 完成 OAuth,拿到 code 后调 callback
 * - 官方账号:直接调 official/login,三字段(id+account_name+password)
 * - 官方改密:official/change-password(首次登录强制)
 */
interface LingXiApi {

    // ---------------- 普通账号 OAuth ----------------

    @POST("api/auth/oauth/authorize")
    suspend fun oauthAuthorize(@Body body: Map<String, String>): OAuthAuthorizeResponse

    @POST("api/auth/oauth/callback")
    suspend fun oauthCallback(
        @Body body: Map<String, String>,
    ): LingXiTokenResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): LingXiTokenResponse

    @POST("api/auth/logout")
    suspend fun logout(@Body body: Map<String, String>): Map<String, Any>

    @POST("api/auth/bind")
    suspend fun bind(@Body body: Map<String, String>): Map<String, Any>

    @POST("api/auth/unbind")
    suspend fun unbind(@Body body: Map<String, String>): Map<String, Any>

    @DELETE("api/auth/account")
    suspend fun deleteAccount(): Map<String, Any>

    // ---------------- 官方账号(ID + 名称 + 密码)----------------

    @POST("api/auth/official/login")
    suspend fun officialLogin(@Body body: OfficialLoginRequest): LingXiTokenResponse

    @POST("api/auth/official/change-password")
    @Headers("Authorization: Bearer {token}")
    suspend fun officialChangePassword(
        @Path(value = "token", encoded = true) token: String,
        @Body body: Map<String, String>,
    ): Map<String, Any>

    // ---------------- 管理员后台(需 OPERATOR 以上 Token)----------------

    @GET("api/admin/stats")
    @Headers("Authorization: Bearer {token}")
    suspend fun adminStats(@Path(value = "token", encoded = true) token: String): Map<String, Any>

    @GET("api/admin/accounts")
    @Headers("Authorization: Bearer {token}")
    suspend fun adminAccounts(
        @Path(value = "token", encoded = true) token: String,
        @Query("keyword") keyword: String = "",
        @Query("role") role: String = "",
        @Query("status") status: String = "",
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): Map<String, Any>

    @DELETE("api/admin/accounts/{id}/session")
    @Headers("Authorization: Bearer {token}")
    suspend fun adminKick(
        @Path(value = "token", encoded = true) token: String,
        @Path("id") accountId: String,
    ): Map<String, Any>

    @POST("api/admin/accounts/{id}/ban")
    @Headers("Authorization: Bearer {token}")
    suspend fun adminBan(
        @Path(value = "token", encoded = true) token: String,
        @Path("id") accountId: String,
    ): Map<String, Any>

    @POST("api/admin/accounts/{id}/unban")
    @Headers("Authorization: Bearer {token}")
    suspend fun adminUnban(
        @Path(value = "token", encoded = true) token: String,
        @Path("id") accountId: String,
    ): Map<String, Any>

    @POST("api/admin/accounts/{id}/grant-operator")
    @Headers("Authorization: Bearer {token}")
    suspend fun adminGrantOperator(
        @Path(value = "token", encoded = true) token: String,
        @Path("id") accountId: String,
    ): Map<String, Any>

    @POST("api/admin/accounts/{id}/revoke-operator")
    @Headers("Authorization: Bearer {token}")
    suspend fun adminRevokeOperator(
        @Path(value = "token", encoded = true) token: String,
        @Path("id") accountId: String,
    ): Map<String, Any>
}

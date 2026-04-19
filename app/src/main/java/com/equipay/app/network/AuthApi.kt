package com.equipay.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): Response<TokenResponse>

    @POST("auth/resend-code")
    suspend fun resendCode(@Body body: ResendCodeRequest): Response<OkResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @POST("auth/login-pin")
    suspend fun loginWithPin(@Body body: LoginWithPinRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<OkResponse>

    @POST("auth/pin")
    suspend fun setPin(
        @Header("Authorization") bearer: String,
        @Body body: SetPinRequest
    ): Response<OkResponse>

    @GET("auth/me")
    suspend fun me(@Header("Authorization") bearer: String): Response<Map<String, String>>

    @POST("auth/tatra/start")
    suspend fun startTatraConnect(): Response<StartTatraConnectResponse>
}
package com.equipay.app.network

import com.equipay.app.BuildConfig
import com.equipay.app.auth.AccessTokenHolder
import com.equipay.app.auth.TokenStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

    private lateinit var tokenStore: TokenStore
    private lateinit var retrofit: Retrofit

    fun init(store: TokenStore) {
        tokenStore = store

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = Interceptor { chain ->
            val req = chain.request()
            // Если запрос уже несёт Authorization (setPin/me передают вручную) — не трогаем.
            val hasAuth = req.header("Authorization") != null
            val isAuthEndpoint = req.url.encodedPath.contains("/auth/")
            val newReq = if (!hasAuth && !isAuthEndpoint) {
                AccessTokenHolder.bearer()?.let { bearer ->
                    req.newBuilder().addHeader("Authorization", bearer).build()
                } ?: req
            } else req
            chain.proceed(newReq)
        }

        val refreshAuthenticator = Authenticator { _: Route?, response: Response ->
            // Повторяем запрос только 1 раз
            if (responseCount(response) >= 2) return@Authenticator null

            val currentRefresh = tokenStore.getRefresh() ?: return@Authenticator null

            // Синхронный рефреш — OkHttp гарантирует, что Authenticator на этом клиенте
            // serializes access
            synchronized(this) {
                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build()
                val refreshRequest = Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "auth/refresh")
                    .post(
                        okhttp3.RequestBody.create(
                            "application/json".toMediaType(),
                            """{"refreshToken":"$currentRefresh"}"""
                        )
                    )
                    .build()
                val refreshResp = client.newCall(refreshRequest).execute()
                if (!refreshResp.isSuccessful) {
                    tokenStore.clear()
                    AccessTokenHolder.clear()
                    refreshResp.close()
                    return@Authenticator null
                }
                val body = refreshResp.body?.string().orEmpty()
                refreshResp.close()
                val tokens = try {
                    json.decodeFromString(TokenResponse.serializer(), body)
                } catch (e: Exception) {
                    tokenStore.clear()
                    AccessTokenHolder.clear()
                    return@Authenticator null
                }
                tokenStore.saveSession(
                    refreshToken = tokens.refreshToken,
                    email = tokens.email,
                    userId = tokens.userId,
                    hasPin = tokens.hasPin
                )
                AccessTokenHolder.set(tokens.accessToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            }
        }

        val okHttp = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(refreshAuthenticator)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val usersApi: UsersApi by lazy { retrofit.create(UsersApi::class.java) }
    val accountsApi: AccountsApi by lazy { retrofit.create(AccountsApi::class.java) }
    val banksApi: BanksApi by lazy { retrofit.create(BanksApi::class.java) }
    val cardsApi: CardsApi by lazy { retrofit.create(CardsApi::class.java) }
    val transactionsApi: TransactionsApi by lazy { retrofit.create(TransactionsApi::class.java) }
    val aiApi: AiApi by lazy { retrofit.create(AiApi::class.java) }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}

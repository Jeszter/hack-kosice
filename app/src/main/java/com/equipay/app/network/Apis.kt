package com.equipay.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UsersApi {
    @GET("users/me") suspend fun me(): Response<UserDto>
    @PUT("users/me") suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<UserDto>
    @GET("users/search") suspend fun search(@Query("q") query: String): Response<List<UserDto>>
    @GET("users/by-email") suspend fun byEmail(@Query("email") email: String): Response<UserDto>
}

interface AccountsApi {
    @GET("accounts") suspend fun list(): Response<List<AccountDto>>
    @POST("accounts") suspend fun create(@Body body: CreateAccountRequest): Response<AccountDto>
    @GET("accounts/{id}") suspend fun get(@Path("id") id: String): Response<AccountDto>
    @GET("accounts/{id}/linked-balance") suspend fun linkedBalance(@Path("id") id: String): Response<LinkedBalanceSummaryDto>
    @POST("accounts/{id}/members") suspend fun invite(@Path("id") id: String, @Body body: InviteMemberRequest): Response<MemberDto>
    @POST("accounts/{id}/members/request-invite") suspend fun requestInviteCode(
        @Path("id") id: String,
        @Body body: RequestInviteCodeRequest
    ): Response<RequestInviteCodeResponse>
    @POST("accounts/{id}/members/confirm-invite") suspend fun confirmInviteCode(
        @Path("id") id: String,
        @Body body: ConfirmInviteCodeRequest
    ): Response<ConfirmInviteCodeResponse>
    @POST("accounts/{id}/add-funds") suspend fun addFunds(@Path("id") id: String, @Body body: AddFundsRequest): Response<AccountDto>
    @DELETE("accounts/{id}/members/me") suspend fun leaveGroup(@Path("id") id: String): Response<LeaveGroupResponse>
    @DELETE("accounts/{id}/members/{userId}") suspend fun kickMember(@Path("id") id: String, @Path("userId") userId: String): Response<Unit>
    @GET("accounts/{id}/limit") suspend fun getMonthlyLimit(@Path("id") id: String): Response<MonthlyLimitDto>
    @POST("accounts/{id}/limit") suspend fun setMonthlyLimit(@Path("id") id: String, @Body body: SetMonthlyLimitRequest): Response<MonthlyLimitDto>
}

interface BanksApi {
    @GET("banks/available") suspend fun available(): Response<List<AvailableBankDto>>
    @GET("banks/connections") suspend fun connections(): Response<List<BankConnectionDto>>
    @POST("banks/tatra/connect/start") suspend fun startTatraConnect(): Response<StartTatraConnectResponse>
    @DELETE("banks/connections/{id}") suspend fun disconnect(@Path("id") id: String): Response<Unit>
}

interface CardsApi {
    @GET("cards") suspend fun list(): Response<List<VirtualCardDto>>
    @POST("cards") suspend fun create(@Body body: CreateCardRequest): Response<VirtualCardDto>
    @POST("cards/{id}/freeze") suspend fun freeze(@Path("id") id: String, @Body body: FreezeCardRequest): Response<VirtualCardDto>
    @GET("accounts/{id}/cards") suspend fun listForAccount(@Path("id") accountId: String): Response<List<VirtualCardDto>>
}

interface TransactionsApi {
    @GET("transactions") suspend fun list(): Response<List<TransactionDto>>
    @POST("transactions") suspend fun create(@Body body: CreateTransactionRequest): Response<TransactionDto>
    @GET("transactions/{id}") suspend fun get(@Path("id") id: String): Response<TransactionDto>
    @GET("accounts/{id}/transactions") suspend fun listForAccount(@Path("id") accountId: String): Response<List<TransactionDto>>
    @GET("insights") suspend fun insights(): Response<InsightsDto>
}

interface AiApi {
    @POST("ai/voice-parse") suspend fun voiceParse(@Body body: VoiceParseRequest): Response<VoiceParseResponse>
    @POST("ai/receipt-parse") suspend fun receiptParse(@Body body: ReceiptParseRequest): Response<VoiceParseResponse>
    @POST("ai/smart-split") suspend fun smartSplit(@Body body: SmartSplitRequest): Response<SmartSplitResponse>
    @GET("ai/insights-hint") suspend fun insightsHint(): Response<RebalanceSuggestionDto>
    @POST("ai/chat") suspend fun chat(@Body body: AiChatRequest): Response<AiChatResponse>
}
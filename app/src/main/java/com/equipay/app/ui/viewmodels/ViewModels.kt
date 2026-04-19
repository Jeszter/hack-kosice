package com.equipay.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipay.app.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateGroupUiState(
    val name: String = "",
    val memberEmails: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val success: AccountDto? = null
)

class CreateGroupViewModel : ViewModel() {
    private val _state = MutableStateFlow(CreateGroupUiState())
    val state: StateFlow<CreateGroupUiState> = _state.asStateFlow()

    fun setName(v: String) { _state.value = _state.value.copy(name = v) }

    fun addEmail(email: String) {
        val cleaned = email.trim().lowercase()
        if (cleaned.isBlank() || cleaned in _state.value.memberEmails) return
        _state.value = _state.value.copy(memberEmails = _state.value.memberEmails + cleaned)
    }

    fun removeEmail(email: String) {
        _state.value = _state.value.copy(memberEmails = _state.value.memberEmails - email)
    }

    fun submit() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Group name is required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(loading = true, error = null)
            try {
                val resp = ApiClient.accountsApi.create(CreateAccountRequest(s.name, s.memberEmails))
                if (resp.isSuccessful) {
                    val created = resp.body()!!
                    AppState.selectAccount(created.id)
                    AppState.triggerRefresh()
                    _state.value = _state.value.copy(loading = false, success = created)
                } else {
                    _state.value = _state.value.copy(loading = false, error = "Error ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}

data class CardsUiState(
    val loading: Boolean = true,
    val cards: List<VirtualCardDto> = emptyList(),
    val error: String? = null,
    val creating: Boolean = false
)

class CardsViewModel : ViewModel() {
    private val _state = MutableStateFlow(CardsUiState())
    val state: StateFlow<CardsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val resp = ApiClient.cardsApi.list()
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(loading = false, cards = resp.body().orEmpty())
                } else _state.value = _state.value.copy(loading = false, error = "Error ${resp.code()}")
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun createCard() {
        val accountId = AppState.selectedAccountId.value ?: run {
            _state.value = _state.value.copy(error = "Select a group first")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(creating = true, error = null)
            try {
                val resp = ApiClient.cardsApi.create(CreateCardRequest(accountId))
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(creating = false, cards = listOf(resp.body()!!) + _state.value.cards)
                } else _state.value = _state.value.copy(creating = false, error = "Error ${resp.code()}")
            } catch (e: Exception) {
                _state.value = _state.value.copy(creating = false, error = e.message)
            }
        }
    }

    fun freeze(cardId: String, frozen: Boolean) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.cardsApi.freeze(cardId, FreezeCardRequest(frozen))
                if (resp.isSuccessful) {
                    val updated = resp.body()!!
                    _state.value = _state.value.copy(cards = _state.value.cards.map { if (it.id == cardId) updated else it })
                }
            } catch (_: Exception) {}
        }
    }
}

data class BanksUiState(
    val loading: Boolean = true,
    val available: List<AvailableBankDto> = emptyList(),
    val connections: List<BankConnectionDto> = emptyList(),
    val error: String? = null,
    val connectingBankCode: String? = null,
    val launchUrl: String? = null
)

class BanksViewModel : ViewModel() {
    private val _state = MutableStateFlow(BanksUiState())
    val state: StateFlow<BanksUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val av = ApiClient.banksApi.available()
                val co = ApiClient.banksApi.connections()
                _state.value = BanksUiState(
                    loading = false,
                    available = av.body().orEmpty(),
                    connections = co.body().orEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun connect(bankCode: String) {
        if (_state.value.connectingBankCode != null) return

        if (bankCode != "TATRA") {
            _state.value = _state.value.copy(error = "Сейчас подключение через PSD2 доступно только для Tatra banka")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(error = null, connectingBankCode = bankCode)
            try {
                val resp = ApiClient.banksApi.startTatraConnect()
                if (resp.isSuccessful) {
                    val url = resp.body()?.authorizeUrl
                    if (url.isNullOrBlank()) {
                        _state.value = _state.value.copy(
                            connectingBankCode = null,
                            error = "Сервер не вернул ссылку авторизации"
                        )
                    } else {
                        _state.value = _state.value.copy(
                            connectingBankCode = null,
                            launchUrl = url
                        )
                    }
                } else {
                    val serverError = runCatching { resp.errorBody()?.string() }.getOrNull()
                    _state.value = _state.value.copy(
                        connectingBankCode = null,
                        error = "Ошибка ${resp.code()}: ${serverError ?: "Не удалось начать подключение банка"}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(connectingBankCode = null, error = e.message)
            }
        }
    }

    fun disconnect(connectionId: String) {
        viewModelScope.launch {
            try {
                ApiClient.banksApi.disconnect(connectionId)
                _state.value = _state.value.copy(connections = _state.value.connections.filterNot { it.id == connectionId })
            } catch (_: Exception) {}
        }
    }

    fun onLaunchConsumed() {
        _state.value = _state.value.copy(launchUrl = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun isConnected(bankCode: String) = _state.value.connections.any { it.bankCode == bankCode && it.active }
}

data class HistoryUiState(
    val loading: Boolean = true,
    val transactions: List<TransactionDto> = emptyList(),
    val error: String? = null
)

class HistoryViewModel : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val resp = ApiClient.transactionsApi.list()
                if (resp.isSuccessful) {
                    _state.value = HistoryUiState(loading = false, transactions = resp.body().orEmpty())
                } else {
                    _state.value = _state.value.copy(loading = false, error = "Error ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}

data class InsightsUiState(
    val loading: Boolean = true,
    val insights: InsightsDto? = null,
    val hint: String? = null,
    val error: String? = null
)

class InsightsViewModel : ViewModel() {
    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val ins = ApiClient.transactionsApi.insights()
                val hint = try { ApiClient.aiApi.insightsHint().body()?.message } catch (_: Exception) { null }
                _state.value = InsightsUiState(
                    loading = false,
                    insights = ins.body(),
                    hint = hint
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}

data class NewPaymentUiState(
    val amountCents: Long = 4000,
    val merchant: String = "",
    val category: String? = null,
    val splitMode: String = "equal",
    val customSplit: Map<String, Long>? = null,
    val account: AccountDto? = null,
    val submitting: Boolean = false,
    val result: TransactionDto? = null,
    val error: String? = null,
    val smartSuggestion: String? = null
)

class NewPaymentViewModel : ViewModel() {
    private val _state = MutableStateFlow(NewPaymentUiState())
    val state: StateFlow<NewPaymentUiState> = _state.asStateFlow()

    init { loadCurrentAccount() }

    private fun loadCurrentAccount() {
        val id = AppState.selectedAccountId.value ?: return
        viewModelScope.launch {
            try {
                val resp = ApiClient.accountsApi.get(id)
                if (resp.isSuccessful) _state.value = _state.value.copy(account = resp.body())
            } catch (_: Exception) {}
        }
    }

    fun setAmountCents(cents: Long) { _state.value = _state.value.copy(amountCents = cents) }
    fun setMerchant(s: String) { _state.value = _state.value.copy(merchant = s) }
    fun setCategory(s: String?) { _state.value = _state.value.copy(category = s) }

    fun setSplitMode(mode: String) {
        _state.value = _state.value.copy(splitMode = mode, customSplit = null)
        if (mode == "smart") loadSmartSplit()
    }

    private fun loadSmartSplit() {
        val accountId = _state.value.account?.id ?: return
        viewModelScope.launch {
            try {
                val resp = ApiClient.aiApi.smartSplit(SmartSplitRequest(accountId, _state.value.amountCents))
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    val map = body.split.associate { it.userId to it.amountCents }
                    _state.value = _state.value.copy(customSplit = map, smartSuggestion = body.suggestion)
                }
            } catch (_: Exception) {}
        }
    }

    fun submit(onDone: (TransactionDto) -> Unit) {
        val s = _state.value
        val accountId = s.account?.id ?: return
        val merchantName = if (s.merchant.isBlank()) "Purchase" else s.merchant
        viewModelScope.launch {
            _state.value = s.copy(submitting = true, error = null)
            try {
                val resp = ApiClient.transactionsApi.create(
                    CreateTransactionRequest(
                        accountId = accountId,
                        merchant = merchantName,
                        category = s.category,
                        totalCents = s.amountCents,
                        splitMode = s.splitMode,
                        customSplit = s.customSplit
                    )
                )
                if (resp.isSuccessful) {
                    val tx = resp.body()!!
                    AppState.triggerRefresh()
                    _state.value = _state.value.copy(submitting = false, result = tx)
                    onDone(tx)
                } else {
                    _state.value = _state.value.copy(submitting = false, error = "Error ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, error = e.message)
            }
        }
    }
}

data class VoiceUiState(
    val listening: Boolean = false,
    val transcript: String = "",
    val parsed: VoiceParseResponse? = null,
    val error: String? = null,
    val loading: Boolean = false
)

class VoiceViewModel : ViewModel() {
    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    fun setListening(v: Boolean) {
        _state.value = _state.value.copy(listening = v)
    }

    fun setTranscript(s: String) {
        _state.value = _state.value.copy(transcript = s)
    }

    fun setError(message: String?) {
        _state.value = _state.value.copy(
            loading = false,
            error = message
        )
    }

    fun parse(transcript: String) {
        if (transcript.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                transcript = transcript,
                error = null
            )
            try {
                val resp = ApiClient.aiApi.voiceParse(
                    VoiceParseRequest(transcript, AppState.selectedAccountId.value)
                )
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(
                        loading = false,
                        parsed = resp.body()
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Error ${resp.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message
                )
            }
        }
    }

    fun reset() {
        _state.value = VoiceUiState()
    }

    /**
     * Photo mode: отправляет картинку в base64 на /ai/receipt-parse.
     */
    fun parseImage(imageBase64: String, mimeType: String = "image/jpeg") {
        if (imageBase64.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                transcript = "(scanning receipt...)",
                error = null
            )
            try {
                val resp = ApiClient.aiApi.receiptParse(
                    ReceiptParseRequest(
                        imageBase64 = imageBase64,
                        mimeType = mimeType,
                        accountId = AppState.selectedAccountId.value
                    )
                )
                if (resp.isSuccessful) {
                    val parsed = resp.body()
                    _state.value = _state.value.copy(
                        loading = false,
                        parsed = parsed,
                        transcript = parsed?.merchant?.let { "📷 Receipt: $it" } ?: "📷 Receipt scanned"
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Couldn't scan receipt (error ${resp.code()})"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Scan failed"
                )
            }
        }
    }
}
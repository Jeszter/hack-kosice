package com.equipay.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipay.app.network.AccountDto
import com.equipay.app.network.ApiClient
import com.equipay.app.network.LinkedBalanceSummaryDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val accounts: List<AccountDto> = emptyList(),
    val currentAccount: AccountDto? = null,
    val linkedBalanceCents: Long? = null,
    val linkedBalanceLoading: Boolean = false,
    val memberLinkedBalancesCents: Map<String, Long> = emptyMap(),
    val monthlyLimitCents: Long? = null,
    val spentThisMonthCents: Long? = null,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                linkedBalanceCents = null,
                linkedBalanceLoading = false,
                memberLinkedBalancesCents = emptyMap()
            )

            try {
                val resp = ApiClient.accountsApi.list()
                if (resp.isSuccessful) {
                    val list = resp.body().orEmpty()
                    val currentId = AppState.selectedAccountId.value
                    val current = list.firstOrNull { it.id == currentId } ?: list.firstOrNull()

                    if (current != null && AppState.selectedAccountId.value != current.id) {
                        AppState.selectAccount(current.id)
                    }

                    _state.value = HomeUiState(
                        loading = false,
                        accounts = list,
                        currentAccount = current,
                        linkedBalanceCents = null,
                        linkedBalanceLoading = current != null,
                        memberLinkedBalancesCents = emptyMap()
                    )

                    if (current != null) {
                        loadExtras(current.id)
                    }
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        linkedBalanceLoading = false,
                        error = "Load failed: ${resp.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    linkedBalanceLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectAccount(id: String) {
        AppState.selectAccount(id)
        val selected = _state.value.accounts.firstOrNull { it.id == id }

        _state.value = _state.value.copy(
            currentAccount = selected,
            linkedBalanceCents = null,
            linkedBalanceLoading = selected != null,
            memberLinkedBalancesCents = emptyMap(),
            monthlyLimitCents = null,
            spentThisMonthCents = null
        )

        if (selected != null) {
            viewModelScope.launch {
                loadExtras(selected.id)
            }
        }
    }

    private suspend fun loadExtras(accountId: String) {
        coroutineScope {
            val balanceDeferred = async {
                try {
                    val resp = ApiClient.accountsApi.linkedBalance(accountId)
                    if (resp.isSuccessful) resp.body() else null
                } catch (_: Exception) { null }
            }
            val limitDeferred = async {
                try {
                    val resp = ApiClient.accountsApi.getMonthlyLimit(accountId)
                    if (resp.isSuccessful) resp.body() else null
                } catch (_: Exception) { null }
            }

            val balanceSummary = balanceDeferred.await()
            val limit = limitDeferred.await()

            _state.value = _state.value.copy(
                linkedBalanceCents = balanceSummary?.totalBalanceCents,
                linkedBalanceLoading = false,
                memberLinkedBalancesCents = balanceSummary.toMemberBalanceMap(),
                monthlyLimitCents = if ((limit?.limitCents ?: 0L) > 0) limit?.limitCents else null,
                spentThisMonthCents = limit?.spentThisMonthCents
            )
        }
    }

    private fun LinkedBalanceSummaryDto?.toMemberBalanceMap(): Map<String, Long> {
        if (this == null) return emptyMap()
        return sources
            .groupBy { it.userId }
            .mapValues { (_, sources) -> sources.sumOf { it.balanceCents } }
    }
}
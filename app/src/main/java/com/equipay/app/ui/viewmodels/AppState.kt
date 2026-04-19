package com.equipay.app.ui.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Глобальный app-state — хранит выбранный текущий групповой счёт.
 * Синглтон, чтобы все ViewModels видели одно и то же.
 */
object AppState {
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    fun selectAccount(id: String?) {
        _selectedAccountId.value = id
    }

    private val _refreshTick = MutableStateFlow(0)
    val refreshTick: StateFlow<Int> = _refreshTick.asStateFlow()

    fun triggerRefresh() {
        _refreshTick.value += 1
    }
}

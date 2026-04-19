package com.example.myapplication.ui.dashboard

data class DashboardUiState(
    val title: String = "SplitFlow Assistant",
    val subtitle: String = "Powered by ElevenLabs",
    val lastUserMessage: String = "",
    val lastAssistantMessage: String = ""
)

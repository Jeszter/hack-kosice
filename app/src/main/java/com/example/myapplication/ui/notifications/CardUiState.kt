package com.example.myapplication.ui.notifications

data class CardUiState(
    val cardNumberMasked: String = "•••• 0000",
    val cardStatus: String = "Inactive",
    val splitModeText: String = "OFF",
    val participantsText: String = "0 participants"
)

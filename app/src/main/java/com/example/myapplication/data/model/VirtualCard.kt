package com.example.myapplication.data.model

data class VirtualCard(
    val last4: String,
    val active: Boolean,
    val splitModeEnabled: Boolean,
    val participantsCount: Int
)

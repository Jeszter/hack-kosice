package com.example.myapplication.data.model

data class Participant(
    val id: String,
    val name: String,
    val bankName: String,
    val ibanMasked: String,
    val contributionCents: Long,
    val connected: Boolean,
    val owner: Boolean = false
)

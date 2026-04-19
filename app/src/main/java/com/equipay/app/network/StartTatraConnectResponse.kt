package com.equipay.app.network

import kotlinx.serialization.Serializable

@Serializable
data class StartTatraConnectResponse(
    val authorizeUrl: String
)
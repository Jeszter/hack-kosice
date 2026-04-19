package com.equipay.api.tatra

object TatraConfig {
    val baseUrl: String =
        System.getenv("TATRA_BASE_URL") ?: "https://api.tatrabanka.sk"

    val redirectUri: String =
        System.getenv("TATRA_REDIRECT_URI") ?: "http://localhost:8080/auth/tatra/callback"

    val psuIpAddress: String =
        System.getenv("TATRA_PSU_IP_ADDRESS") ?: "127.0.0.1"

    val clientId: String =
        System.getenv("TATRA_CLIENT_ID") ?: ""

    val clientSecret: String =
        System.getenv("TATRA_CLIENT_SECRET") ?: ""
}
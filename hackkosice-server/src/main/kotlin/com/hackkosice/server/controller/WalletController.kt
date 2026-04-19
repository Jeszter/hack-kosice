package com.hackkosice.server.controller

import com.hackkosice.server.dto.AddWalletMemberRequest
import com.hackkosice.server.dto.CreateWalletRequest
import com.hackkosice.server.dto.WalletResponse
import com.hackkosice.server.entity.UserEntity
import com.hackkosice.server.service.WalletService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wallets")
class WalletController(
    private val walletService: WalletService
) {
    @PostMapping
    fun createWallet(
        @Valid @RequestBody request: CreateWalletRequest,
        @AuthenticationPrincipal currentUser: UserEntity
    ): WalletResponse {
        return walletService.createWallet(currentUser, request)
    }

    @GetMapping
    fun getMyWallets(@AuthenticationPrincipal currentUser: UserEntity): List<WalletResponse> {
        return walletService.getMyWallets(currentUser)
    }

    @GetMapping("/{walletId}")
    fun getWallet(
        @PathVariable walletId: Long,
        @AuthenticationPrincipal currentUser: UserEntity
    ): WalletResponse {
        return walletService.getWallet(walletId, currentUser)
    }

    @PostMapping("/{walletId}/members")
    fun addMember(
        @PathVariable walletId: Long,
        @Valid @RequestBody request: AddWalletMemberRequest,
        @AuthenticationPrincipal currentUser: UserEntity
    ): WalletResponse {
        return walletService.addMember(walletId, request, currentUser)
    }
}

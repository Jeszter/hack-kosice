package com.hackkosice.server.controller

import com.hackkosice.server.dto.BankConnectionResponse
import com.hackkosice.server.dto.MockBankLinkRequest
import com.hackkosice.server.entity.UserEntity
import com.hackkosice.server.service.WalletService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bank-connections")
class BankConnectionController(
    private val walletService: WalletService
) {
    @GetMapping
    fun getMyConnections(@AuthenticationPrincipal currentUser: UserEntity): List<BankConnectionResponse> {
        return walletService.getMyBankConnections(currentUser)
    }

    @PostMapping("/mock-link")
    fun mockLink(
        @Valid @RequestBody request: MockBankLinkRequest,
        @AuthenticationPrincipal currentUser: UserEntity
    ): BankConnectionResponse {
        return walletService.mockLinkBankAccount(request, currentUser)
    }
}

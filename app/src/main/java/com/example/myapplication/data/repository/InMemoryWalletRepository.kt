package com.example.myapplication.data.repository

import com.example.myapplication.data.model.AssistantMessage
import com.example.myapplication.data.model.Participant
import com.example.myapplication.data.model.SharedWallet
import com.example.myapplication.data.model.TransactionItem
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.data.model.VirtualCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class InMemoryWalletRepository : WalletRepository {

    private val internalWallet = MutableStateFlow(
        SharedWallet(
            walletId = "wallet_trip_vienna",
            walletName = "Trip to Vienna",
            totalBalanceCents = 124000,
            participants = listOf(
                Participant("p1", "You", "Tatra banka", "SK•••• 4567", 54000, true, true),
                Participant("p2", "Alex", "Tatra banka", "SK•••• 8021", 30000, true),
                Participant("p3", "Maria", "Tatra banka", "SK•••• 1174", 25000, true),
                Participant("p4", "David", "Tatra banka", "SK•••• 9012", 15000, true)
            ),
            recentTransactions = listOf(
                TransactionItem("t1", "Pizza Place", "Dinner split", -4000, TransactionType.DEBIT, "Today"),
                TransactionItem("t2", "Uber", "Ride to hotel", -1850, TransactionType.DEBIT, "Today"),
                TransactionItem("t3", "Top up", "Added by You", 15000, TransactionType.CREDIT, "Yesterday")
            ),
            virtualCard = VirtualCard(
                last4 = "1234",
                active = true,
                splitModeEnabled = true,
                participantsCount = 4
            ),
            messages = listOf(
                AssistantMessage("m1", "Split 40 euros for dinner between us", true),
                AssistantMessage("m2", "Done. 10.00 € assigned to each participant.", false)
            )
        )
    )

    override val wallet: StateFlow<SharedWallet> = internalWallet.asStateFlow()

    override suspend fun addMoney(amountCents: Long) {
        if (amountCents <= 0) return
        val current = internalWallet.value
        internalWallet.value = current.copy(
            totalBalanceCents = current.totalBalanceCents + amountCents,
            recentTransactions = listOf(
                TransactionItem(
                    id = UUID.randomUUID().toString(),
                    title = "Top up",
                    subtitle = "Added to shared wallet",
                    amountCents = amountCents,
                    type = TransactionType.CREDIT,
                    createdAt = "Now"
                )
            ) + current.recentTransactions
        )
    }

    override suspend fun splitExpenseEvenly(title: String, amountCents: Long) {
        if (amountCents <= 0) return
        val current = internalWallet.value
        val participantsCount = current.participants.size.coerceAtLeast(1)
        val perPerson = amountCents / participantsCount
        val updatedParticipants = current.participants.map {
            it.copy(contributionCents = (it.contributionCents - perPerson).coerceAtLeast(0))
        }
        internalWallet.value = current.copy(
            totalBalanceCents = (current.totalBalanceCents - amountCents).coerceAtLeast(0),
            participants = updatedParticipants,
            recentTransactions = listOf(
                TransactionItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    subtitle = "Even split across $participantsCount accounts",
                    amountCents = -amountCents,
                    type = TransactionType.DEBIT,
                    createdAt = "Now"
                )
            ) + current.recentTransactions,
            messages = current.messages + listOf(
                AssistantMessage(UUID.randomUUID().toString(), "Split ${formatAmount(amountCents)} for $title", true),
                AssistantMessage(UUID.randomUUID().toString(), "Done. ${formatAmount(perPerson)} assigned to each participant.", false)
            )
        )
    }

    override suspend fun toggleCardFreeze() {
        val current = internalWallet.value
        internalWallet.value = current.copy(
            virtualCard = current.virtualCard.copy(active = !current.virtualCard.active)
        )
    }

    override suspend fun addParticipant(name: String, bankName: String, ibanMasked: String) {
        val current = internalWallet.value
        val newParticipant = Participant(
            id = UUID.randomUUID().toString(),
            name = name,
            bankName = bankName,
            ibanMasked = ibanMasked,
            contributionCents = 0,
            connected = true
        )
        val participants = current.participants + newParticipant
        internalWallet.value = current.copy(
            participants = participants,
            virtualCard = current.virtualCard.copy(participantsCount = participants.size)
        )
    }

    private fun formatAmount(amountCents: Long): String {
        val abs = kotlin.math.abs(amountCents)
        val euros = abs / 100
        val cents = abs % 100
        return "$euros.${cents.toString().padStart(2, '0')} €"
    }
}

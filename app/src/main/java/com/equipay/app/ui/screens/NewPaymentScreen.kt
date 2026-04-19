package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.network.MemberDto
import com.equipay.app.ui.components.Avatar
import com.equipay.app.ui.components.SectionLabel
import com.equipay.app.ui.theme.AccentOnWhite
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Divider
import com.equipay.app.ui.theme.Red
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import com.equipay.app.ui.viewmodels.NewPaymentViewModel

@Composable
fun NewPaymentScreen(
    amountCents: Long? = null,
    merchant: String? = null,
    splitMode: String? = null,
    category: String? = null,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onSuccess: () -> Unit
) {
    val vm: NewPaymentViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(amountCents, merchant, splitMode, category) {
        amountCents?.let { vm.setAmountCents(it) }
        merchant?.let { vm.setMerchant(it) }
        splitMode?.let { vm.setSplitMode(it) }
        category?.let { vm.setCategory(it) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Text(
                "New Payment", color = TextPrimary, fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            Icon(
                Icons.Default.Close, null,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp).clickable { onClose() }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Amount
        SectionLabel("Amount", modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .padding(18.dp)
        ) {
            Text(
                formatEuroFromCents(state.amountCents),
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Merchant (if not preset)
        if (state.merchant.isBlank()) {
            Spacer(Modifier.height(14.dp))
            SectionLabel("Merchant", modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = state.merchant,
                    onValueChange = vm::setMerchant,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp
                    ),
                    decorationBox = { inner ->
                        if (state.merchant.isBlank()) {
                            Text("e.g. Pizza Place", color = TextMuted, fontSize = 16.sp)
                        }
                        inner()
                    },
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary)
                )
            }
        } else {
            Spacer(Modifier.height(14.dp))
            Text(
                state.merchant,
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Split between
        SectionLabel("Split between", modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .padding(4.dp)
        ) {
            SplitTab("Equal", state.splitMode == "equal", Modifier.weight(1f)) { vm.setSplitMode("equal") }
            SplitTab("Smart", state.splitMode == "smart", Modifier.weight(1f), showAiBadge = true) { vm.setSplitMode("smart") }
        }

        Spacer(Modifier.height(14.dp))

        val account = state.account
        if (account != null) {
            val split = computeSplit(state.amountCents, account.members, state.customSplit)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
            ) {
                account.members.forEachIndexed { i, m ->
                    val share = split[m.userId] ?: 0L
                    MemberRow(m, share)
                    if (i != account.members.lastIndex) {
                        Box(
                            modifier = Modifier.padding(start = 62.dp).fillMaxWidth().height(1.dp)
                                .background(Divider)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Suggestion block
        val suggestion = state.smartSuggestion ?: if (state.splitMode == "equal") "Splitting equally across members." else null
        if (suggestion != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
                    .padding(14.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Gemini suggestion", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(suggestion, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.error!!, color = Red, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }

        Spacer(Modifier.weight(1f))

        // Pay CTA
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (state.submitting || account == null) Surface1 else AccentWhite)
                .clickable(enabled = !state.submitting && account != null) { vm.submit { onSuccess() } }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.submitting) {
                CircularProgressIndicator(color = AccentOnWhite, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    "Pay & Split",
                    color = if (account == null) TextMuted else AccentOnWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun computeSplit(total: Long, members: List<MemberDto>, custom: Map<String, Long>?): Map<String, Long> {
    if (custom != null) return custom
    if (members.isEmpty()) return emptyMap()
    val n = members.size
    val base = total / n
    val remainder = total % n
    return members.mapIndexed { i, m -> m.userId to (base + if (i < remainder) 1 else 0) }.toMap()
}

@Composable
private fun SplitTab(text: String, selected: Boolean, modifier: Modifier = Modifier, showAiBadge: Boolean = false, onClick: () -> Unit) {
    val bg = if (selected) AccentWhite else Color.Transparent
    val fg = if (selected) AccentOnWhite else TextPrimary
    Row(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bg).clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (showAiBadge) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentOnWhite.copy(alpha = if (selected) 1f else 0.5f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("AI", color = if (selected) AccentWhite else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MemberRow(m: MemberDto, share: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(m.displayName ?: m.email, size = 36)
        Spacer(Modifier.width(12.dp))
        Text(
            m.displayName ?: m.email.substringBefore("@"),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(formatEuroFromCents(share), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

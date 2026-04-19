package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.network.AccountDto
import com.equipay.app.network.MemberDto
import com.equipay.app.ui.components.Avatar
import com.equipay.app.ui.components.SectionLabel
import com.equipay.app.ui.components.Sparkline
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Divider
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import com.equipay.app.ui.viewmodels.AppState
import com.equipay.app.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    onManageClick: () -> Unit,
    onSeeAllParticipants: () -> Unit,
    onCreateGroup: () -> Unit,
    onCreateJoinClick: () -> Unit,
    onLogout: () -> Unit,
    onInsightsClick: () -> Unit,
    bottomPadding: PaddingValues
) {
    val vm: HomeViewModel = viewModel()
    val state by vm.state.collectAsState()
    val refreshTick by AppState.refreshTick.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(refreshTick) { vm.load() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding.calculateBottomPadding() + 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = state.currentAccount?.name ?: "EquiPay",
                    color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Group, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Notifications, "Notifications", tint = TextPrimary,
                    modifier = Modifier.size(22.dp))
                Icon(Icons.Default.Logout, "Logout", tint = TextSecondary,
                    modifier = Modifier.size(20.dp).clickable { onLogout() })
            }
        }

        when {
            state.loading -> {
                Spacer(Modifier.height(60.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            }
            state.accounts.isEmpty() -> {
                EmptyGroupsBlock(onCreateGroup)
                Spacer(Modifier.height(24.dp))
                JoinGroupBlock(onCreateJoinClick)
            }
            else -> {
                BalanceAndActions(
                    account = state.currentAccount!!,
                    displayedBalanceCents = state.linkedBalanceCents ?: state.currentAccount!!.balanceCents,
                    balanceLoading = state.linkedBalanceLoading,
                    limitCents = state.monthlyLimitCents,
                    spentCents = state.spentThisMonthCents,
                    onManageClick = onManageClick,
                    onInsightsClick = onInsightsClick,
                    onCreateJoinClick = onCreateJoinClick,
                    onCreateGroup = onCreateGroup,
                    onSwitchAccount = { acc -> vm.selectAccount(acc.id) },
                    allAccounts = state.accounts
                )
                Spacer(Modifier.height(24.dp))
                ParticipantsBlock(
                    members = state.currentAccount!!.members,
                    memberLinkedBalancesCents = state.memberLinkedBalancesCents,
                    balancesLoading = state.linkedBalanceLoading,
                    onSeeAll = onSeeAllParticipants
                )
                Spacer(Modifier.height(24.dp))
                JoinGroupBlock(onCreateJoinClick)
            }
        }
    }
}

@Composable
private fun BalanceAndActions(
    account: AccountDto,
    displayedBalanceCents: Long,
    balanceLoading: Boolean,
    limitCents: Long?,
    spentCents: Long?,
    allAccounts: List<AccountDto>,
    onManageClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onCreateJoinClick: () -> Unit,
    onCreateGroup: () -> Unit,
    onSwitchAccount: (AccountDto) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Surface1, Surface2)))
            .padding(20.dp)
    ) {
        Column {
            Text("TOTAL BALANCE", color = TextMuted, fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formatEuroFromCents(displayedBalanceCents),
                        color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${account.members.size} member${if (account.members.size > 1) "s" else ""} · ${account.currency}",
                            color = TextSecondary, fontSize = 13.sp
                        )
                        if (balanceLoading) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = TextSecondary)
                        }
                    }
                }
                Sparkline(color = TextPrimary, modifier = Modifier.width(80.dp).height(32.dp))
            }

            if (limitCents != null && limitCents > 0) {
                Spacer(Modifier.height(16.dp))
                val spent = spentCents ?: 0L
                val progress = (spent.toFloat() / limitCents.toFloat()).coerceIn(0f, 1f)
                val overBudget = spent > limitCents
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Monthly limit", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = "${formatEuroFromCents(spent)} / ${formatEuroFromCents(limitCents)}",
                        color = if (overBudget) Color(0xFFFF6B6B) else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Surface2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (overBudget) Color(0xFFFF6B6B) else Color(0xFF4CAF50))
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(18.dp))

    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionTile(Icons.Default.Settings, "Manage", Modifier.weight(1f), onManageClick)
        ActionTile(Icons.Default.GroupAdd, "Create/Join", Modifier.weight(1f), onCreateJoinClick)
        ActionTile(Icons.Default.TrendingUp, "Insights", Modifier.weight(1f), onInsightsClick)
    }

    if (allAccounts.size > 1) {
        Spacer(Modifier.height(18.dp))
        SectionLabel("Your groups", modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            allAccounts.forEach { acc ->
                val isSelected = acc.id == account.id
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) TextPrimary else Surface1)
                        .clickable { onSwitchAccount(acc) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(acc.name, color = if (isSelected) Bg else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Surface1)
                    .clickable { onCreateGroup() }.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("+ New", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun JoinGroupBlock(onJoinClick: () -> Unit) {
    var joinCode by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel("Join a group")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Surface1).padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = joinCode,
                onValueChange = { joinCode = it.uppercase().take(20) },
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                decorationBox = { inner ->
                    if (joinCode.isEmpty()) Text("Enter invite code", color = TextMuted, fontSize = 15.sp)
                    inner()
                }
            )
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (joinCode.length >= 4) TextPrimary else Surface2)
                    .clickable(enabled = joinCode.length >= 4) { onJoinClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Join", color = if (joinCode.length >= 4) Bg else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Or tap Create/Join above", color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyGroupsBlock(onCreateGroup: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Surface1), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Group, null, tint = TextPrimary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No groups yet", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a group to start splitting expenses with friends.",
            color = TextSecondary, fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(TextPrimary)
                .clickable { onCreateGroup() }.padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text("Create group", color = Bg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ParticipantsBlock(
    members: List<MemberDto>,
    memberLinkedBalancesCents: Map<String, Long>,
    balancesLoading: Boolean,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel("Participants")
        Text("See all", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.clickable { onSeeAll() })
    }
    Spacer(Modifier.height(12.dp))
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        members.take(4).forEachIndexed { idx, m ->
            val memberBalance = memberLinkedBalancesCents[m.userId] ?: 0L
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(name = m.displayName ?: m.email, size = 40)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        m.displayName ?: m.email.substringBefore("@"),
                        color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                    if (m.role == "owner") Text("Owner", color = TextMuted, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (balancesLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.6.dp,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            formatEuroFromCents(memberBalance),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text("current balance", color = TextMuted, fontSize = 10.sp)
                }
            }
            if (idx != members.take(4).lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(Divider))
            }
        }
        if (members.size > 4) {
            Spacer(Modifier.height(8.dp))
            Text("+${members.size - 4} more · tap See all", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Surface1)
            .clickable { onClick() }.padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(text = label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatEuroFromCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val whole = abs / 100
    val c = abs % 100
    val wholeStr = if (whole >= 1000) {
        val thousands = whole / 1000
        val remainder = whole % 1000
        "%d %03d".format(thousands, remainder)
    } else whole.toString()
    return "$sign$wholeStr,%02d €".format(c)
}

fun formatEuro(value: Double): String = formatEuroFromCents((value * 100).toLong())
package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.equipay.app.network.AccountDto
import com.equipay.app.network.ApiClient
import com.equipay.app.network.MemberDto
import com.equipay.app.ui.components.Avatar
import com.equipay.app.ui.components.SectionLabel
import com.equipay.app.ui.theme.*
import com.equipay.app.ui.viewmodels.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GroupDetailScreen(
    accountId: String,
    onBack: () -> Unit,
    onGroupLeft: () -> Unit,
    onInviteMember: (String) -> Unit
) {
    var account by remember { mutableStateOf<AccountDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showKickDialog by remember { mutableStateOf<MemberDto?>(null) }
    var actionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accountId) {
        loading = true
        try {
            val accResp = withContext(Dispatchers.IO) { ApiClient.accountsApi.get(accountId) }
            val meResp = withContext(Dispatchers.IO) { ApiClient.usersApi.me() }
            if (accResp.isSuccessful) account = accResp.body()
            if (meResp.isSuccessful) currentUserId = meResp.body()?.id
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val isOwner = account?.ownerUserId == currentUserId

    // Leave dialog
    if (showLeaveDialog) {
        ConfirmDialog(
            title = "Leave group?",
            message = "You will lose access to \"${account?.name}\". This cannot be undone.",
            confirmLabel = "Leave",
            confirmColor = Color(0xFFFF6B6B),
            loading = actionLoading,
            error = actionError,
            onConfirm = {
                actionLoading = true
                actionError = null
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { ApiClient.accountsApi.leaveGroup(accountId) }
                        if (resp.isSuccessful) {
                            AppState.selectAccount(null)
                            AppState.triggerRefresh()
                            showLeaveDialog = false
                            onGroupLeft()
                        } else {
                            actionError = "Failed to leave group (${resp.code()})"
                        }
                    } catch (e: Exception) {
                        actionError = e.message ?: "Something went wrong"
                    } finally {
                        actionLoading = false
                    }
                }
            },
            onDismiss = { showLeaveDialog = false; actionError = null }
        )
    }

    // Kick dialog
    showKickDialog?.let { member ->
        ConfirmDialog(
            title = "Remove member?",
            message = "Remove \"${member.displayName ?: member.email}\" from the group?",
            confirmLabel = "Remove",
            confirmColor = Color(0xFFFF6B6B),
            loading = actionLoading,
            error = actionError,
            onConfirm = {
                actionLoading = true
                actionError = null
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { ApiClient.accountsApi.kickMember(accountId, member.userId) }
                        if (resp.isSuccessful) {
                            account = account?.copy(members = account!!.members.filterNot { it.userId == member.userId })
                            showKickDialog = null
                            actionError = null
                            AppState.triggerRefresh()
                        } else {
                            actionError = "Failed to remove member (${resp.code()})"
                        }
                    } catch (e: Exception) {
                        actionError = e.message ?: "Something went wrong"
                    } finally {
                        actionLoading = false
                    }
                }
            },
            onDismiss = { showKickDialog = null; actionError = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() })
            Text(
                account?.name ?: "Group",
                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(24.dp))
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
            return@Column
        }

        if (error != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error!!, color = Red, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            return@Column
        }

        val acc = account ?: return@Column

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Group stats card
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Surface1).padding(16.dp)
            ) {
                Column {
                    Text("GROUP OVERVIEW", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Members", acc.members.size.toString())
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Divider))
                        StatItem("Balance", formatEuroFromCents(acc.balanceCents))
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Divider))
                        StatItem("Currency", acc.currency)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Members section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Members (${acc.members.size})")
            }
            Spacer(Modifier.height(12.dp))

            acc.members.forEach { member ->
                MemberRow(
                    member = member,
                    isOwner = isOwner,
                    isCurrentUser = member.userId == currentUserId,
                    groupOwnerUserId = acc.ownerUserId,
                    onKick = { showKickDialog = member }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Invite new member section
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Surface2, RoundedCornerShape(14.dp))
                    .clickable { onInviteMember(accountId) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Invite member", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Send an email invitation", color = TextSecondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Danger zone
            if (!isOwner) {
                Text("DANGER ZONE", color = Red.copy(alpha = 0.7f), fontSize = 11.sp,
                    letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Red.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { showLeaveDialog = true }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ExitToApp, null, tint = Red, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Leave group", color = Red, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("You will lose access to this group", color = Red.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: MemberDto,
    isOwner: Boolean,
    isCurrentUser: Boolean,
    groupOwnerUserId: String,
    onKick: () -> Unit
) {
    val isGroupOwner = member.userId == groupOwnerUserId
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Surface1).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = member.displayName ?: member.email, size = 44)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        member.displayName ?: member.email.substringBefore("@"),
                        color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Surface2).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("you", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
                Text(
                    if (isGroupOwner) "Owner" else member.email,
                    color = if (isGroupOwner) TextSecondary else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isGroupOwner) FontWeight.Medium else FontWeight.Normal
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatEuroFromCents(member.contributedCents),
                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Text("contributed", color = TextMuted, fontSize = 10.sp)
            }
            // Owner can kick non-owner members (not self)
            if (isOwner && !isGroupOwner && !isCurrentUser) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(Red.copy(alpha = 0.15f))
                        .clickable { onKick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonRemove, null, tint = Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    loading: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (!loading) onDismiss() }) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Surface1).padding(24.dp)
        ) {
            Column {
                Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(message, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Red, fontSize = 13.sp)
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(Surface2).clickable(enabled = !loading) { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(confirmColor.copy(alpha = 0.15f))
                            .border(1.dp, confirmColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !loading) { onConfirm() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading) {
                            CircularProgressIndicator(color = confirmColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text(confirmLabel, color = confirmColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

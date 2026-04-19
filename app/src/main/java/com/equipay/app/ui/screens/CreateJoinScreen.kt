package com.equipay.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.network.ApiClient
import com.equipay.app.network.ConfirmInviteCodeRequest
import com.equipay.app.network.CreateAccountRequest
import com.equipay.app.ui.screens.auth.AuthInputLabel
import com.equipay.app.ui.screens.auth.AuthTextField
import com.equipay.app.ui.theme.*
import com.equipay.app.ui.viewmodels.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Tab { CREATE, JOIN }

@Composable
fun CreateJoinScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onJoined: () -> Unit,
    // Pre-fill: if user received invite, we can pre-fill accountId + email
    prefillAccountId: String? = null,
    prefillEmail: String? = null
) {
    // Auto-switch to JOIN tab if we have prefill data
    var activeTab by remember { mutableStateOf(if (prefillAccountId != null) Tab.JOIN else Tab.CREATE) }

    // CREATE state
    var createName by remember { mutableStateOf("") }
    var createLoading by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    // JOIN state — 3 steps:
    // Step 1: Enter invite code (the short group code)
    // Step 2: Enter your email to receive the verification code
    // Step 3: Enter the verification code from email
    var joinStep by remember { mutableStateOf(1) }
    var joinCode by remember { mutableStateOf("") }         // the invite code for the group
    var joinAccountId by remember { mutableStateOf(prefillAccountId ?: "") }
    var joinEmail by remember { mutableStateOf(prefillEmail ?: "") }
    var joinVerifyCode by remember { mutableStateOf("") }   // verification code from email
    var joinLoading by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }
    var joinInfo by remember { mutableStateOf<String?>(null) }

    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() })
            Text(
                "Groups",
                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(24.dp))
        }

        // Tab switcher
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Tab.entries.forEach { tab ->
                val isActive = activeTab == tab
                val bgColor by animateColorAsState(
                    targetValue = if (isActive) Surface2 else Surface1,
                    animationSpec = tween(200), label = "tabBg"
                )
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { activeTab = tab; joinError = null; createError = null }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (tab == Tab.CREATE) Icons.Default.Add else Icons.Default.Group,
                            contentDescription = null,
                            tint = if (isActive) TextPrimary else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (tab == Tab.CREATE) "Create" else "Join",
                            color = if (isActive) TextPrimary else TextMuted,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Content
        when (activeTab) {
            Tab.CREATE -> CreateTab(
                name = createName,
                onNameChange = { createName = it; createError = null },
                loading = createLoading,
                error = createError,
                onSubmit = {
                    if (createName.isBlank()) { createError = "Group name is required"; return@CreateTab }
                    createLoading = true; createError = null
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val resp = withContext(Dispatchers.IO) {
                                ApiClient.accountsApi.create(CreateAccountRequest(createName.trim()))
                            }
                            if (resp.isSuccessful) {
                                val created = resp.body()!!
                                AppState.selectAccount(created.id)
                                AppState.triggerRefresh()
                                onCreated()
                            } else {
                                createError = "Failed to create group (${resp.code()})"
                            }
                        } catch (e: Exception) {
                            createError = e.message ?: "Something went wrong"
                        } finally {
                            createLoading = false
                        }
                    }
                }
            )

            Tab.JOIN -> JoinTab(
                step = joinStep,
                code = joinCode,
                email = joinEmail,
                verifyCode = joinVerifyCode,
                loading = joinLoading,
                error = joinError,
                info = joinInfo,
                onCodeChange = { joinCode = it.uppercase().take(12); joinError = null },
                onEmailChange = { joinEmail = it; joinError = null },
                onVerifyCodeChange = { joinVerifyCode = it; joinError = null },
                onSubmit = {
                    keyboard?.hide()
                    when (joinStep) {
                        1 -> {
                            // Lookup the group by invite code, send verification email
                            if (joinCode.isBlank()) { joinError = "Enter invite code"; return@JoinTab }
                            if (joinEmail.isBlank()) { joinError = "Enter your email"; return@JoinTab }
                            joinLoading = true; joinError = null; joinInfo = null
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    // requestInviteCode: group owner sent us an invite, now we request a code to our email
                                    // The invite code IS the accountId or a lookup key
                                    // We use it as accountId for now (server resolves it)
                                    joinAccountId = joinCode.trim()
                                    val resp = withContext(Dispatchers.IO) {
                                        ApiClient.accountsApi.requestInviteCode(
                                            joinAccountId,
                                            com.equipay.app.network.RequestInviteCodeRequest(joinEmail.trim())
                                        )
                                    }
                                    if (resp.isSuccessful) {
                                        joinInfo = resp.body()?.message ?: "Verification code sent to ${joinEmail}"
                                        joinStep = 2
                                    } else {
                                        joinError = when (resp.code()) {
                                            404 -> "Group not found. Check the invite code."
                                            else -> "Error (${resp.code()}). Try again."
                                        }
                                    }
                                } catch (e: Exception) {
                                    joinError = e.message ?: "Something went wrong"
                                } finally {
                                    joinLoading = false
                                }
                            }
                        }
                        2 -> {
                            // Confirm the verification code and join
                            if (joinVerifyCode.isBlank()) { joinError = "Enter the code from email"; return@JoinTab }
                            joinLoading = true; joinError = null
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val resp = withContext(Dispatchers.IO) {
                                        ApiClient.accountsApi.confirmInviteCode(
                                            joinAccountId,
                                            ConfirmInviteCodeRequest(
                                                email = joinEmail.trim(),
                                                code = joinVerifyCode.trim()
                                            )
                                        )
                                    }
                                    if (resp.isSuccessful) {
                                        AppState.triggerRefresh()
                                        onJoined()
                                    } else {
                                        joinError = "Invalid code. Check your email and try again."
                                    }
                                } catch (e: Exception) {
                                    joinError = e.message ?: "Something went wrong"
                                } finally {
                                    joinLoading = false
                                }
                            }
                        }
                    }
                },
                onBack = { joinStep = 1; joinVerifyCode = ""; joinError = null; joinInfo = null }
            )
        }
    }
}

@Composable
private fun CreateTab(
    name: String,
    onNameChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text("Create a group", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Give your group a name — you can invite members after creating it.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(24.dp))

            AuthInputLabel("Group name")
            AuthTextField(
                value = name,
                onChange = onNameChange,
                placeholder = "e.g. \"Trip to Vienna\""
            )

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = Red, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Quick ideas
            Text("Ideas", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            listOf("Family", "Friends", "Work team", "Trip 2025").forEach { suggestion ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)).background(Surface1)
                        .clickable { onNameChange(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(suggestion, color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (name.isBlank() || loading) Surface1 else AccentWhite)
                .clickable(enabled = name.isNotBlank() && !loading) { onSubmit() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(color = AccentOnWhite, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    "Create group",
                    color = if (name.isBlank()) TextMuted else AccentOnWhite,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun JoinTab(
    step: Int,
    code: String,
    email: String,
    verifyCode: String,
    loading: Boolean,
    error: String?,
    info: String?,
    onCodeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onVerifyCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Step indicator
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepDot(num = 1, active = step >= 1, done = step > 1)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(if (step > 1) TextSecondary else Surface2))
                StepDot(num = 2, active = step >= 2, done = false)
            }
            Spacer(Modifier.height(20.dp))

            when (step) {
                1 -> {
                    Text("Join a group", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Enter the invite code from the group owner and your email. We'll send you a verification code.",
                        color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(24.dp))

                    AuthInputLabel("Invite code")
                    InviteCodeField(value = code, onChange = onCodeChange)

                    Spacer(Modifier.height(16.dp))
                    AuthInputLabel("Your email")
                    AuthTextField(
                        value = email,
                        onChange = onEmailChange,
                        placeholder = "your@email.com",
                        keyboardType = KeyboardType.Email
                    )
                }
                2 -> {
                    if (step == 2 && info != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Surface1).padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Email, null, tint = TextSecondary, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(info, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    Text("Enter verification code", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "We sent a 6-digit code to $email. Enter it below to join the group.",
                        color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(24.dp))

                    AuthInputLabel("Verification code")
                    AuthTextField(
                        value = verifyCode,
                        onChange = onVerifyCodeChange,
                        placeholder = "123456",
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Didn't receive it?",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onBack() }
                    )
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Red.copy(alpha = 0.1f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = Red, fontSize = 13.sp)
                }
            }
        }

        // Buttons
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            if (step == 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Surface1).clickable(enabled = !loading) { onBack() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("← Back / resend code", color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            val canSubmit = when (step) {
                1 -> code.length >= 4 && email.contains("@")
                2 -> verifyCode.length >= 4
                else -> false
            }
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (!canSubmit || loading) Surface1 else AccentWhite)
                    .clickable(enabled = canSubmit && !loading) { onSubmit() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(color = AccentOnWhite, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        when (step) {
                            1 -> "Send verification code"
                            else -> "Join group"
                        },
                        color = if (!canSubmit) TextMuted else AccentOnWhite,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteCodeField(value: String, onChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Surface1).padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.uppercase().take(20)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = TextPrimary, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp
            ),
            cursorBrush = SolidColor(TextPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { keyboard?.hide() }),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "ABC-12345",
                        color = TextMuted, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun StepDot(num: Int, active: Boolean, done: Boolean) {
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape)
            .background(when { done -> TextSecondary; active -> TextPrimary; else -> Surface2 }),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(Icons.Default.Check, null, tint = Bg, modifier = Modifier.size(14.dp))
        } else {
            Text(num.toString(), color = if (active) Bg else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

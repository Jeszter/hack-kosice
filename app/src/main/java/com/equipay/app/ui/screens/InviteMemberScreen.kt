package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.network.ApiClient
import com.equipay.app.network.ConfirmInviteCodeRequest
import com.equipay.app.network.RequestInviteCodeRequest
import com.equipay.app.ui.screens.auth.AuthInputLabel
import com.equipay.app.ui.screens.auth.AuthTextField
import com.equipay.app.ui.theme.AccentOnWhite
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Red
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import com.equipay.app.ui.viewmodels.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun InviteMemberScreen(
    accountId: String,
    onBack: () -> Unit,
    onInvited: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Text(
                "Add Member",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(24.dp))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .weight(1f)
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                if (step == 1) "Invite member" else "Confirm code",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                if (step == 1)
                    "Enter the member email. We will send a verification code to that address."
                else
                    "Enter the code from the email to add this user to the group.",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(20.dp))

            AuthInputLabel("Email")
            AuthTextField(
                value = email,
                onChange = {
                    email = it
                    error = null
                },
                placeholder = "friend@email.com",
                keyboardType = KeyboardType.Email
            )

            if (step == 2) {
                Spacer(Modifier.height(16.dp))
                AuthInputLabel("Verification code")
                AuthTextField(
                    value = code,
                    onChange = {
                        code = it
                        error = null
                    },
                    placeholder = "123456",
                    keyboardType = KeyboardType.Number
                )
            }

            if (info != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    info!!,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    error!!,
                    color = Red,
                    fontSize = 13.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (
                        loading ||
                        email.isBlank() ||
                        (step == 2 && code.isBlank())
                    ) Surface1 else AccentWhite
                )
                .clickable(
                    enabled = !loading && email.isNotBlank() && (step == 1 || code.isNotBlank())
                ) {
                    loading = true
                    error = null
                    info = null

                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            if (step == 1) {
                                val response = ApiClient.accountsApi.requestInviteCode(
                                    accountId,
                                    RequestInviteCodeRequest(email.trim())
                                )

                                if (response.isSuccessful) {
                                    info = response.body()?.message ?: "Code sent"
                                    step = 2
                                } else {
                                    error = "Failed to send code: ${response.code()}"
                                }
                            } else {
                                val response = ApiClient.accountsApi.confirmInviteCode(
                                    accountId,
                                    ConfirmInviteCodeRequest(
                                        email = email.trim(),
                                        code = code.trim()
                                    )
                                )

                                if (response.isSuccessful) {
                                    AppState.triggerRefresh()
                                    onInvited()
                                } else {
                                    error = "Invalid code or confirmation failed"
                                }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Something went wrong"
                        } finally {
                            loading = false
                        }
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = AccentOnWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    if (step == 1) "Send code" else "Confirm and add",
                    color = if (
                        email.isBlank() ||
                        (step == 2 && code.isBlank())
                    ) TextMuted else AccentOnWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
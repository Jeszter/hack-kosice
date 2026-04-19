package com.equipay.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.EquiPayApp
import com.equipay.app.ui.theme.AccentOnWhite
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Green
import com.equipay.app.ui.theme.Red
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    email: String,
    onBack: () -> Unit,
    onVerified: (hasPin: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val repo = EquiPayApp.instance.authRepo
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var cooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    fun submit(value: String) {
        if (value.length != 6) return
        loading = true
        error = null
        scope.launch {
            repo.verifyEmail(email, value, android.os.Build.ID).fold(
                onSuccess = { tokens ->
                    loading = false
                    onVerified(tokens.hasPin)
                },
                onFailure = {
                    loading = false
                    error = it.message
                    code = ""
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary,
            modifier = Modifier.size(24.dp).clickable { onBack() }
        )

        Spacer(Modifier.height(20.dp))

        Text("Check your email", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We sent a 6-digit code to\n$email",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(36.dp))

        // Hidden TextField controls the input; boxes display it.
        BasicTextField(
            value = code,
            onValueChange = { new ->
                val filtered = new.filter { it.isDigit() }.take(6)
                code = filtered
                if (filtered.length == 6) {
                    keyboard?.hide()
                    submit(filtered)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus(); keyboard?.show() },
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(6) { i ->
                val char = code.getOrNull(i)?.toString() ?: ""
                val isFocused = i == code.length
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface1)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) TextPrimary else Surface2,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (error != null) Text(error!!, color = Red, fontSize = 13.sp)
        if (info != null) Text(info!!, color = Green, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        // Resend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Didn't get it? ", color = TextSecondary, fontSize = 13.sp)
            Text(
                text = if (cooldown > 0) "Resend (${cooldown}s)" else "Resend code",
                color = if (cooldown > 0) TextMuted else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = cooldown == 0) {
                    scope.launch {
                        repo.resendCode(email).fold(
                            onSuccess = {
                                info = "Code sent to $email"
                                error = null
                                cooldown = 30
                            },
                            onFailure = {
                                error = it.message
                                info = null
                            }
                        )
                    }
                }
            )
        }

        Spacer(Modifier.weight(1f))

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentWhite, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

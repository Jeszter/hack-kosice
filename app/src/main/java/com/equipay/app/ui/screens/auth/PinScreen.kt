package com.equipay.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.EquiPayApp
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Red
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Режим работы экрана.
 * - SetUp: юзер создаёт PIN (ввёл → повторил → сохранили на backend)
 * - Login: юзер уже залогинен, вводит существующий PIN для входа
 */
enum class PinMode { SetUp, Login }

@Composable
fun PinScreen(
    mode: PinMode,
    email: String,
    onPinReady: () -> Unit,
    onFallbackToPassword: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val repo = EquiPayApp.instance.authRepo

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val pinLength = 6
    val currentInput = if (isConfirmStage) confirmPin else pin

    val (title, subtitle) = when {
        mode == PinMode.Login -> "Enter your PIN" to "Welcome back, $email"
        isConfirmStage -> "Confirm your PIN" to "Re-enter the same 6 digits"
        else -> "Create a PIN" to "You'll use it to quickly sign in"
    }

    fun attemptLogin(value: String) {
        loading = true
        error = null
        scope.launch {
            repo.loginWithPin(email, value, android.os.Build.ID).fold(
                onSuccess = { loading = false; onPinReady() },
                onFailure = {
                    loading = false
                    error = it.message
                    pin = ""
                }
            )
        }
    }

    fun attemptSetPin() {
        loading = true
        error = null
        scope.launch {
            repo.setPin(pin).fold(
                onSuccess = { loading = false; onPinReady() },
                onFailure = {
                    loading = false
                    error = it.message
                    pin = ""
                    confirmPin = ""
                    isConfirmStage = false
                }
            )
        }
    }

    fun onDigit(d: Int) {
        if (loading) return
        val current = if (isConfirmStage) confirmPin else pin
        if (current.length >= pinLength) return
        val new = current + d.toString()
        if (isConfirmStage) {
            confirmPin = new
            if (new.length == pinLength) {
                if (new == pin) {
                    attemptSetPin()
                } else {
                    error = "PINs don't match. Try again."
                    pin = ""; confirmPin = ""; isConfirmStage = false
                }
            }
        } else {
            pin = new
            if (new.length == pinLength) {
                when (mode) {
                    PinMode.Login -> attemptLogin(new)
                    PinMode.SetUp -> isConfirmStage = true
                }
            }
        }
    }

    fun onBackspace() {
        if (loading) return
        if (isConfirmStage) {
            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
        } else {
            if (pin.isNotEmpty()) pin = pin.dropLast(1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        Text(title, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(40.dp))

        // 6 dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pinLength) { i ->
                val filled = i < currentInput.length
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) TextPrimary else Color.Transparent)
                        .border(1.5.dp, TextPrimary, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (error != null) {
            Text(error!!, color = Red, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentWhite, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        // Keypad
        NumericKeypad(
            onDigit = ::onDigit,
            onBackspace = ::onBackspace,
            showLeftAction = mode == PinMode.Login && onFallbackToPassword != null,
            leftActionLabel = "Use password",
            onLeftAction = { onFallbackToPassword?.invoke() }
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    showLeftAction: Boolean,
    leftActionLabel: String,
    onLeftAction: () -> Unit
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { d -> KeypadDigit(d, onDigit, Modifier.weight(1f)) }
            }
        }
        // last row: left action / 0 / backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showLeftAction) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable { onLeftAction() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(leftActionLabel, color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Spacer(Modifier.weight(1f).height(64.dp))
            }
            KeypadDigit(0, onDigit, Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun KeypadDigit(digit: Int, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1)
            .border(1.dp, Surface2, RoundedCornerShape(20.dp))
            .clickable { onClick(digit) },
        contentAlignment = Alignment.Center
    ) {
        Text(digit.toString(), color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

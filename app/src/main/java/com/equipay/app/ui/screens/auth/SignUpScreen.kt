package com.equipay.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.EquiPayApp
import com.equipay.app.ui.theme.AccentOnWhite
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Red
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onRegistered: (email: String) -> Unit,
    onGoToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repo = EquiPayApp.instance.authRepo

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = email.isNotBlank() && password.length >= 8 && !loading

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

        Text("Create account", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "We'll send a verification code to your email.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        AuthInputLabel("Name (optional)")
        AuthTextField(
            value = displayName,
            onChange = { displayName = it },
            placeholder = "Your name",
            keyboardType = KeyboardType.Text
        )

        Spacer(Modifier.height(14.dp))

        AuthInputLabel("Email")
        AuthTextField(
            value = email,
            onChange = { email = it },
            placeholder = "you@example.com",
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(14.dp))

        AuthInputLabel("Password (min 8 characters)")
        AuthTextField(
            value = password,
            onChange = { password = it },
            placeholder = "••••••••",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextMuted
                    )
                }
            }
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = Red, fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

        // CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (canSubmit) AccentWhite else Surface1)
                .clickable(enabled = canSubmit) {
                    loading = true
                    error = null
                    scope.launch {
                        repo.register(email.trim().lowercase(), password, displayName.ifBlank { null })
                            .fold(
                                onSuccess = {
                                    loading = false
                                    onRegistered(email.trim().lowercase())
                                },
                                onFailure = {
                                    loading = false
                                    error = it.message
                                }
                            )
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
                    "Continue",
                    color = if (canSubmit) AccentOnWhite else TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Already have an account? ", color = TextSecondary, fontSize = 13.sp)
            Text(
                "Log in",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onGoToLogin() }
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun AuthInputLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun AuthTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = TextMuted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface1,
            unfocusedContainerColor = Surface1,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = TextPrimary,
            focusedBorderColor = TextPrimary,
            unfocusedBorderColor = Surface1
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

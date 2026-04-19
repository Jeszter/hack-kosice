package com.equipay.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.ui.theme.AccentOnWhite
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onSignUp: () -> Unit,
    onLogIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // Logo
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Surface1, Surface2))),
            contentAlignment = Alignment.Center
        ) {
            Text("E", color = TextPrimary, fontSize = 54.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(28.dp))

        Text("EquiPay", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Shared expenses.\nZero awkwardness.",
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        // Primary: Sign Up
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AccentWhite)
                .clickable { onSignUp() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Create account", color = AccentOnWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        // Secondary: Log In
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .border(1.dp, Surface2, RoundedCornerShape(14.dp))
                .clickable { onLogIn() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("I already have an account", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "By continuing you agree to PSD2\nopen banking terms.",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))
    }
}

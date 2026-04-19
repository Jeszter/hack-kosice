package com.equipay.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextPrimary

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(Bg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Surface1, Surface2))),
            contentAlignment = Alignment.Center
        ) {
            Text("E", color = TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
    }
}

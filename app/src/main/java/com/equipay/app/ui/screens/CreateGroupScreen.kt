package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.ui.components.Avatar
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
import com.equipay.app.ui.viewmodels.CreateGroupViewModel

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val vm: CreateGroupViewModel = viewModel()
    val state by vm.state.collectAsState()

    var emailInput by remember { mutableStateOf("") }

    LaunchedEffect(state.success) {
        if (state.success != null) onCreated()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Text(
                "New Group",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(24.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp).weight(1f)) {
            Spacer(Modifier.height(12.dp))
            Text("Group name", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            AuthInputLabel("e.g. \"Trip to Vienna\"")
            AuthTextField(value = state.name, onChange = vm::setName, placeholder = "Group name")

            Spacer(Modifier.height(24.dp))
            Text("Invite members", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "They must already have an EquiPay account.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    AuthTextField(
                        value = emailInput,
                        onChange = { emailInput = it },
                        placeholder = "friend@email.com",
                        keyboardType = KeyboardType.Email
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Surface1)
                        .clickable {
                            vm.addEmail(emailInput)
                            emailInput = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // List of invitees
            state.memberEmails.forEach { em ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface1)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .padding(bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(name = em, size = 32)
                    Spacer(Modifier.width(10.dp))
                    Text(em, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp).clickable { vm.removeEmail(em) }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = Red, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (state.name.isBlank() || state.loading) Surface1 else AccentWhite)
                .clickable(enabled = state.name.isNotBlank() && !state.loading) { vm.submit() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.loading) {
                CircularProgressIndicator(color = AccentOnWhite, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    "Create group",
                    color = if (state.name.isBlank()) TextMuted else AccentOnWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

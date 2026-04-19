package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.network.ApiClient
import com.equipay.app.network.MonthlyLimitDto
import com.equipay.app.network.SetMonthlyLimitRequest
import com.equipay.app.ui.theme.*
import com.equipay.app.ui.viewmodels.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GroupManageScreen(
    accountId: String,
    onBack: () -> Unit,
    onConnectBank: () -> Unit
) {
    var limitData by remember { mutableStateOf<MonthlyLimitDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf("") }

    LaunchedEffect(accountId) {
        loading = true
        try {
            val resp = withContext(Dispatchers.IO) { ApiClient.accountsApi.getMonthlyLimit(accountId) }
            if (resp.isSuccessful) {
                val data = resp.body()
                limitData = data
                if (data != null && data.limitCents > 0) {
                    limitInput = (data.limitCents / 100).toString()
                }
            }
        } catch (_: Exception) {
            // Limit may not be set yet
        } finally {
            loading = false
        }
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
                "Manage Group",
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

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ===== SECTION 1: Monthly Limit =====
            SectionHeader(icon = Icons.Default.EuroSymbol, title = "Monthly Spending Limit")
            Spacer(Modifier.height(4.dp))
            Text(
                "Cap how much the group can spend per month. Members see progress on the home screen.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))

            // Current stats
            if (limitData != null && limitData!!.limitCents > 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Surface1).padding(16.dp)
                ) {
                    Column {
                        Text("THIS MONTH", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBlock("Limit", formatEuroFromCents(limitData!!.limitCents))
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Divider))
                            StatBlock("Spent", formatEuroFromCents(limitData!!.spentThisMonthCents))
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Divider))
                            StatBlock(
                                "Remaining",
                                formatEuroFromCents(limitData!!.remainingCents),
                                color = if (limitData!!.remainingCents < 0) Color(0xFFFF6B6B) else Color(0xFF4CAF50)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        val progress = if (limitData!!.limitCents > 0)
                            (limitData!!.spentThisMonthCents.toFloat() / limitData!!.limitCents).coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(3.dp)).background(Surface2)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(progress).height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (progress >= 1f) Color(0xFFFF6B6B) else Color(0xFF4CAF50))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Limit input
            Text("Set limit (€ per month)", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
                    .border(1.dp, if (limitInput.isNotBlank()) TextPrimary.copy(alpha = 0.3f) else Surface2, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.EuroSymbol, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = limitInput,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.length <= 6) { limitInput = v; success = false }
                    },
                    modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(TextPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { inner ->
                        if (limitInput.isEmpty()) Text("e.g. 4000", color = TextMuted, fontSize = 18.sp)
                        inner()
                    }
                )
                if (limitInput.isNotBlank()) Text(",00 €", color = TextSecondary, fontSize = 16.sp)
            }

            Spacer(Modifier.height(10.dp))

            // Quick presets
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1000", "2000", "4000", "10000").forEach { preset ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (limitInput == preset) TextPrimary else Surface2)
                            .clickable { limitInput = preset; success = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("€$preset", color = if (limitInput == preset) Bg else TextSecondary,
                            fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = Red, fontSize = 13.sp)
            }
            if (success) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Limit saved!", color = Color(0xFF4CAF50), fontSize = 13.sp)
                }
            }

            // Save button
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (saving || limitInput.isBlank()) Surface2 else AccentWhite)
                    .clickable(enabled = !saving && limitInput.isNotBlank()) {
                        val cents = (limitInput.toLongOrNull() ?: 0L) * 100
                        if (cents <= 0) { error = "Please enter a valid limit"; return@clickable }
                        saving = true; error = null
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val resp = withContext(Dispatchers.IO) {
                                    ApiClient.accountsApi.setMonthlyLimit(accountId, SetMonthlyLimitRequest(cents))
                                }
                                if (resp.isSuccessful) {
                                    limitData = resp.body()
                                    success = true
                                    AppState.triggerRefresh()
                                } else {
                                    error = "Failed to save (${resp.code()})"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Something went wrong"
                            } finally {
                                saving = false
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (saving) {
                    CircularProgressIndicator(color = AccentOnWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        "Save limit",
                        color = if (limitInput.isBlank()) TextMuted else AccentOnWhite,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ===== SECTION 2: Bank Connection =====
            SectionHeader(icon = Icons.Default.AccountBalance, title = "Bank Account")
            Spacer(Modifier.height(4.dp))
            Text(
                "Connect your bank account to automatically sync your real balance.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Surface1)
                    .clickable { onConnectBank() }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connect bank", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("PSD2 · Tatra banka supported", color = TextMuted, fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                        tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

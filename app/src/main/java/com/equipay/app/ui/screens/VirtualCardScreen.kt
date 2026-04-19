package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.network.VirtualCardDto
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Red
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import com.equipay.app.ui.viewmodels.BanksViewModel
import com.equipay.app.ui.viewmodels.CardsViewModel

@Composable
fun VirtualCardScreen(
    onBack: () -> Unit,
    onOpenSplitMode: () -> Unit,
    onOpenConnectBank: () -> Unit,
    bottomPadding: PaddingValues
) {
    val vm: CardsViewModel = viewModel()
    val state by vm.state.collectAsState()

    val banksVm: BanksViewModel = viewModel()
    val banksState by banksVm.state.collectAsState()

    LaunchedEffect(Unit) {
        banksVm.load()
    }

    fun handleCreateCard() {
        val hasConnectedBank = banksState.connections.any { it.active }
        if (hasConnectedBank) {
            vm.createCard()
        } else {
            onOpenConnectBank()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(bottom = bottomPadding.calculateBottomPadding())
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
                "Virtual Card",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .clickable { handleCreateCard() },
                contentAlignment = Alignment.Center
            ) {
                if (state.creating) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        "Create card",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            state.cards.isEmpty() -> {
                EmptyCardsBlock(
                    onCreate = { handleCreateCard() },
                    loading = state.creating
                )
            }

            else -> {
                val card = state.cards.first()
                CardVisual(card)
                Spacer(Modifier.height(18.dp))
                StatusRow(card, onToggle = { vm.freeze(card.id, !card.frozen) })
                Spacer(Modifier.height(14.dp))
                CardActions(
                    onFreeze = { vm.freeze(card.id, !card.frozen) },
                    onAdd = { handleCreateCard() }
                )
                Spacer(Modifier.height(14.dp))
                SplitModeRow(count = 4, onClick = onOpenSplitMode)
            }
        }

        val uiError = state.error ?: banksState.error
        if (uiError != null) {
            Text(
                uiError,
                color = Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CardVisual(card: VirtualCardDto) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Surface1, Surface2)))
            .padding(20.dp)
    ) {
        Text(
            "SplitFlow",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = (-12).dp)
                .size(width = 36.dp, height = 26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFB8B8B8), Color(0xFF6E6E6E))))
        )

        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "••••  ${card.last4}",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Text(
                text = if (card.frozen) "FROZEN" else if (card.active) "ACTIVE" else "INACTIVE",
                color = if (card.frozen) Red else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StatusRow(card: VirtualCardDto, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "STATUS",
                color = TextMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (card.frozen) "Frozen" else "Active",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (!card.frozen) AccentWhite else Surface2)
                .clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                if (card.frozen) "Unfreeze" else "Freeze",
                color = if (!card.frozen) Bg else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CardActions(onFreeze: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardActionTile(Icons.Default.AcUnit, "Freeze", Modifier.weight(1f), onFreeze)
        CardActionTile(Icons.Default.Description, "Details", Modifier.weight(1f)) {}
        CardActionTile(Icons.Default.Settings, "Settings", Modifier.weight(1f)) {}
    }
}

@Composable
private fun SplitModeRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Group, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("SPLIT MODE: ON", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "$count participants",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyCardsBlock(onCreate: () -> Unit, loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "No virtual card yet",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a card for shared expenses and manage it with your group.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(AccentWhite)
                .clickable { onCreate() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = Bg,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    "Create Card",
                    color = Bg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CardActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
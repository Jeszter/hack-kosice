package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.network.AvailableBankDto
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import com.equipay.app.ui.viewmodels.BanksViewModel

@Composable
fun ConnectBankScreen(onBack: () -> Unit) {
    val vm: BanksViewModel = viewModel()
    val state by vm.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(state.launchUrl) {
        val url = state.launchUrl ?: return@LaunchedEffect
        uriHandler.openUri(url)
        vm.onLaunchConsumed()
    }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.clearError()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.load()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
                Text(
                    text = "Connect Bank",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(24.dp))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Choose your bank",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            if (state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                val banks = state.available

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    banks.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { bank ->
                                val connected = vm.isConnected(bank.code)
                                val connecting = state.connectingBankCode == bank.code

                                BankCard(
                                    bank = bank,
                                    connected = connected,
                                    connecting = connecting,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (connecting || connected) return@BankCard
                                        vm.connect(bank.code)
                                    }
                                )
                            }

                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Your data is secure.", color = TextSecondary, fontSize = 13.sp)
                    Text("We use PSD2 open banking.", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun BankCard(
    bank: AvailableBankDto,
    connected: Boolean,
    connecting: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .then(
                if (connected) {
                    Modifier.border(1.5.dp, AccentWhite, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !connecting && !connected) { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(4.dp))

        Text(
            text = bank.code,
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )

        Text(
            text = bank.name,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (connected) AccentWhite else Surface2)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        connecting -> "Connecting..."
                        connected -> "Connected"
                        else -> "Connect"
                    },
                    color = if (connected) Bg else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (connected) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Bg,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
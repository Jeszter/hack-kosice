package com.equipay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.network.TransactionDto
import com.equipay.app.ui.components.SectionLabel
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import com.equipay.app.ui.viewmodels.AppState
import com.equipay.app.ui.viewmodels.HistoryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun HistoryScreen(onBack: () -> Unit, bottomPadding: PaddingValues) {
    val vm: HistoryViewModel = viewModel()
    val state by vm.state.collectAsState()
    val refreshTick by AppState.refreshTick.collectAsState()

    LaunchedEffect(refreshTick) { vm.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding.calculateBottomPadding() + 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() })
            Text("History", color = TextPrimary, fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Icon(Icons.Default.FilterList, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
        }

        if (state.loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
            return
        }

        // Группировка по дню
        val today = Instant.now().truncatedTo(ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toLocalDate()
        val grouped = state.transactions.groupBy { tx ->
            val d = try {
                Instant.parse(tx.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (_: Exception) { today }
            when (d) {
                today -> "TODAY"
                today.minusDays(1) -> "YESTERDAY"
                else -> d.toString().uppercase()
            }
        }

        if (state.transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No transactions yet", color = TextSecondary, fontSize = 14.sp)
            }
        }

        grouped.forEach { (label, list) ->
            Spacer(Modifier.height(12.dp))
            SectionLabel(label, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                list.forEach { TransactionRow(it) }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionDto) {
    val title = tx.merchant ?: "Payment"
    val split = tx.splitMode
    val subtitle = if (split == "equal" || split == "smart")
        "Split ${tx.splits.size} ways • ${formatEuroFromCents(tx.totalCents / tx.splits.size.coerceAtLeast(1))} each"
    else
        "${tx.initiatorName ?: "You"} paid"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconFor(tx.category), null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
        Text(
            "-${formatEuroFromCents(tx.totalCents)}",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun iconFor(category: String?): ImageVector = when {
    category == null -> Icons.Default.ShoppingBag
    category.contains("food", true) || category.contains("drink", true) -> Icons.Default.Restaurant
    category.contains("transport", true) -> Icons.Default.DirectionsCar
    category.contains("entertain", true) || category.contains("museum", true) -> Icons.Default.Museum
    else -> Icons.Default.ShoppingBag
}

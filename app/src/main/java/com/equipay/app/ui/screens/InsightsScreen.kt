package com.equipay.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equipay.app.network.DailySpendDto
import com.equipay.app.ui.components.SectionLabel
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.Divider
import com.equipay.app.ui.theme.Surface1
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary
import com.equipay.app.ui.theme.TextSecondary
import com.equipay.app.ui.viewmodels.InsightsViewModel

@Composable
fun InsightsScreen(onBack: () -> Unit, bottomPadding: PaddingValues) {
    val vm: InsightsViewModel = viewModel()
    val state by vm.state.collectAsState()

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
            Text("Insights", color = TextPrimary, fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(24.dp))
        }

        if (state.loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
            return
        }

        val data = state.insights

        // Gemini card
        state.hint?.let { hintText ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gemini", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(hintText, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp).size(20.dp))
            }
            Spacer(Modifier.height(22.dp))
        }

        if (data != null) {
            SectionLabel("Spending this week", modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                formatEuroFromCents(data.spendingThisWeekCents),
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                val delta = data.deltaPercent
                val sign = if (delta >= 0) "+" else ""
                Text("${sign}${String.format("%.1f", delta)}% vs last week", color = TextSecondary, fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))

            if (data.weeklySpend.isNotEmpty()) {
                BarChart(
                    data.weeklySpend,
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(180.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            if (data.topCategories.isNotEmpty()) {
                SectionLabel("Top categories", modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface1)
                ) {
                    data.topCategories.forEachIndexed { i, c ->
                        CategoryRow(iconFor(c.category), c.category, c.amountCents)
                        if (i != data.topCategories.lastIndex) {
                            Box(modifier = Modifier.padding(start = 62.dp).fillMaxWidth().height(1.dp).background(Divider))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun iconFor(cat: String?): ImageVector = when {
    cat?.contains("food", true) == true || cat?.contains("drink", true) == true -> Icons.Default.Restaurant
    cat?.contains("transport", true) == true -> Icons.Default.DirectionsCar
    else -> Icons.Default.ShoppingBag
}

@Composable
private fun CategoryRow(icon: ImageVector, name: String, cents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(formatEuroFromCents(cents), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BarChart(data: List<DailySpendDto>, modifier: Modifier = Modifier) {
    val maxCents = (data.maxOfOrNull { it.amountCents } ?: 1L).coerceAtLeast(1L)
    val yLabels = listOf(maxCents, maxCents / 2, 0L)

    Row(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yLabels.forEach {
                Text("${it / 100}", color = TextMuted, fontSize = 11.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val barWidth = size.width / (data.size * 2f)
                val spacing = size.width / data.size
                data.forEachIndexed { index, d ->
                    val h = (d.amountCents.toFloat() / maxCents) * size.height
                    val x = index * spacing + (spacing - barWidth) / 2
                    val y = size.height - h
                    drawRoundRect(
                        color = Color(0xFFE6E6E6),
                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                data.forEach { Text(it.dayShort, color = TextMuted, fontSize = 11.sp) }
            }
        }
    }
}

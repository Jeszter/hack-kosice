package com.equipay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.ui.theme.AccentWhite
import com.equipay.app.ui.theme.Bg
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary

enum class BottomTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Members("Insights", Icons.Default.Group),
    Card("Card", Icons.Default.CreditCard),
    History("History", Icons.Default.List)
}

/**
 * Bottom bar с центральным mic-FAB. FAB виден ВСЕГДА, на всех main-routes.
 * Структура: [Home] [Insights]  [MIC-FAB]  [Card] [History]
 */
@Composable
fun EquiBottomBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    onMicClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 18.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(BottomTab.Home, selected, onSelect, modifier = Modifier.weight(1f))
            TabItem(BottomTab.Members, selected, onSelect, modifier = Modifier.weight(1f))

            // Mic FAB
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AccentWhite)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Split",
                    tint = Bg,
                    modifier = Modifier.size(24.dp)
                )
            }

            TabItem(BottomTab.Card, selected, onSelect, modifier = Modifier.weight(1f))
            TabItem(BottomTab.History, selected, onSelect, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(tab: BottomTab, selected: BottomTab, onSelect: (BottomTab) -> Unit, modifier: Modifier = Modifier) {
    val isSelected = tab == selected
    val color = if (isSelected) TextPrimary else TextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect(tab) }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

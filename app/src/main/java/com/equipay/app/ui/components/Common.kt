package com.equipay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equipay.app.ui.theme.Surface2
import com.equipay.app.ui.theme.Surface3
import com.equipay.app.ui.theme.TextMuted
import com.equipay.app.ui.theme.TextPrimary

/** Круглый аватар-плейсхолдер (градиент + иконка). Напоминает серые аватарки с макетов. */
@Composable
fun Avatar(name: String, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Surface3, Surface2)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Используем инициал — выглядит как на макете (чуть светлее круг, внутри силуэт/буква)
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = name,
            tint = TextMuted,
            modifier = Modifier.size((size * 0.7).dp)
        )
    }
}

/** Маленький заголовок-секция. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TextMuted,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

@Composable
fun Sparkline(color: Color = TextPrimary, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.6f)
            cubicTo(w * 0.2f, h * 0.2f, w * 0.35f, h * 0.9f, w * 0.55f, h * 0.5f)
            cubicTo(w * 0.7f, h * 0.2f, w * 0.85f, h * 0.7f, w, h * 0.3f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
        )
    }
}

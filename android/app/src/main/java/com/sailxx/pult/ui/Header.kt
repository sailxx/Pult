package com.sailxx.pult.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.LocalPultPalette
import com.sailxx.pult.PultDimens

enum class DeckSection(val title: String) {
    DOCK("Док"),
    TOUCHPAD("Тачпад"),
    CLIPBOARD("Буфер"),
    SIGNATURE("Подпись"),
    CAPTURE("Снимок"),
    APPS("Программы")
}

/**
 * Компактный верхний бар Pult высотой 44dp в стиле Windock.
 * Включает статус хоста Windows, сегментированный переключатель режимов,
 * кнопку быстрых настроек и отключение.
 */
@Composable
fun PultHeader(
    hostName: String,
    selectedSection: DeckSection,
    onSectionSelect: (DeckSection) -> Unit,
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPultPalette.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PultDimens.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Левый блок: Имя хоста и индикатор статуса в стиле Liquid Glass Pill
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(PultDimens.shapePill)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), PultDimens.shapePill)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(palette.live, CircleShape)
            )
            Text(
                text = hostName,
                color = palette.textPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }

        // Центральный блок: Сегментированный переключатель разделов
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(PultDimens.shapePill)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), PultDimens.shapePill)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DeckSection.entries.forEach { section ->
                val active = section == selectedSection
                val bgCol by animateColorAsState(
                    targetValue = if (active) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "tabBg"
                )
                val borderCol by animateColorAsState(
                    targetValue = if (active) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "tabBorder"
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(PultDimens.shapePill)
                        .background(bgCol)
                        .then(if (active) Modifier.border(1.dp, borderCol, PultDimens.shapePill) else Modifier)
                        .clickable {
                            if (!active) {
                                onHaptic()
                                onSectionSelect(section)
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = section.title,
                        color = if (active) palette.textPrimary else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Правый блок: Настройки и отключение
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Кнопка настроек
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable {
                        onHaptic()
                        onOpenSettings()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙",
                    color = palette.textSecondary,
                    fontSize = 13.sp
                )
            }

            // Кнопка отключения
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable {
                        onHaptic()
                        onDisconnect()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

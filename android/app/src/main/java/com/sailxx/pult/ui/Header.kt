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
import com.sailxx.pult.PultColors
import com.sailxx.pult.PultDimens

enum class DeckSection(val title: String) {
    APPS("Приложения"),
    MEDIA("Плеер"),
    CURSOR("Курсор")
}

/**
 * Компактный верхний бар высотой 42dp по спецификации Pult.
 * Включает статус хоста, переключатель режимов, быстрый регулятор звука и кнопку отключения.
 */
@Composable
fun PultHeader(
    hostName: String,
    selectedSection: DeckSection,
    onSectionSelect: (DeckSection) -> Unit,
    volume: Int,
    isMuted: Boolean,
    onVolumeStep: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onDisconnect: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PultDimens.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Левый блок: Имя хоста и индикатор статуса
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(PultDimens.shapeInner)
                .background(PultColors.surface)
                .border(1.dp, PultColors.border, PultDimens.shapeInner)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(PultColors.success, CircleShape)
            )
            Text(
                text = hostName,
                color = PultColors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }

        // Центральный блок: Переключатель режимов (Apps / Player / Cursor)
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(PultDimens.shapeInner)
                .background(PultColors.surface)
                .border(1.dp, PultColors.border, PultDimens.shapeInner)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DeckSection.entries.forEach { section ->
                val active = section == selectedSection
                val bgCol by animateColorAsState(
                    targetValue = if (active) PultColors.surfaceLight else Color.Transparent,
                    animationSpec = tween(180),
                    label = "tabBg"
                )
                val borderCol by animateColorAsState(
                    targetValue = if (active) PultColors.border else Color.Transparent,
                    animationSpec = tween(180),
                    label = "tabBorder"
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(PultDimens.shapeBadge)
                        .background(bgCol)
                        .then(if (active) Modifier.border(1.dp, borderCol, PultDimens.shapeBadge) else Modifier)
                        .clickable {
                            if (!active) {
                                onHaptic()
                                onSectionSelect(section)
                            }
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = section.title,
                        color = if (active) PultColors.textPrimary else PultColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Правый блок: Управление громкостью и отключение
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Кнопка -5%
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(PultDimens.shapeBadge)
                    .background(PultColors.surface)
                    .border(1.dp, PultColors.border, PultDimens.shapeBadge)
                    .clickable {
                        onHaptic()
                        onVolumeStep(-5)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = PultColors.textSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Бейдж громкости / Mute
            val badgeBg by animateColorAsState(
                targetValue = if (isMuted) PultColors.danger.copy(alpha = 0.15f) else PultColors.surface,
                animationSpec = tween(180),
                label = "muteBadgeBg"
            )
            val badgeBorder by animateColorAsState(
                targetValue = if (isMuted) PultColors.danger.copy(alpha = 0.5f) else PultColors.border,
                animationSpec = tween(180),
                label = "muteBadgeBorder"
            )

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(PultDimens.shapeBadge)
                    .background(badgeBg)
                    .border(1.dp, badgeBorder, PultDimens.shapeBadge)
                    .clickable {
                        onHaptic()
                        onToggleMute()
                    }
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMuted) "MUTE" else "$volume%",
                    color = if (isMuted) PultColors.danger else PultColors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp
                )
            }

            // Кнопка +5%
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(PultDimens.shapeBadge)
                    .background(PultColors.surface)
                    .border(1.dp, PultColors.border, PultDimens.shapeBadge)
                    .clickable {
                        onHaptic()
                        onVolumeStep(+5)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = PultColors.textSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Кнопка отключения
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(PultDimens.shapeBadge)
                    .background(PultColors.surface)
                    .border(1.dp, PultColors.border, PultDimens.shapeBadge)
                    .clickable {
                        onHaptic()
                        onDisconnect()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = PultColors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

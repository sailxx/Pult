package com.sailxx.pult.ui

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.LocalPultPalette
import com.sailxx.pult.PultDimens
import com.sailxx.pult.PultPalettes
import com.sailxx.pult.PultThemePalette

@Composable
fun SettingsSheet(
    prefs: SharedPreferences,
    currentPalette: PultThemePalette,
    onPaletteSelect: (PultThemePalette) -> Unit,
    dockSlotsCount: Int,
    onDockSlotsSelect: (Int) -> Unit,
    showLabels: Boolean,
    onShowLabelsToggle: (Boolean) -> Unit,
    onClose: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPultPalette.current

    val themes = listOf(
        PultPalettes.Aurora,
        PultPalettes.Dusk,
        PultPalettes.Ember,
        PultPalettes.Midnight,
        PultPalettes.Oled
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        // Окно настроек
        PultCard(
            modifier = Modifier
                .width(520.dp)
                .fillMaxHeight(0.88f)
                .clickable(enabled = false) {}, // Защита от закрытия по клику внутри
            backgroundColor = palette.surface,
            borderColor = palette.border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Заголовок и кнопка закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "НАСТРОЙКИ PULT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Персонализация внешнего вида и сетки дока",
                            fontSize = 11.sp,
                            color = palette.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, CircleShape)
                            .clickable {
                                onHaptic()
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // РАЗДЕЛ: ТЕМА И СТИЛЬ ОФОРМЛЕНИЯ
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ТЕМАТИЧЕСКАЯ ПАЛИТРА",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themes.forEach { theme ->
                                val isSelected = currentPalette.name.equals(theme.name, ignoreCase = true)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(PultDimens.shapeInner)
                                        .background(if (isSelected) palette.surfaceLight else palette.backgroundBase)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) palette.accent else palette.border,
                                            shape = PultDimens.shapeInner
                                        )
                                        .clickable {
                                            onHaptic()
                                            onPaletteSelect(theme)
                                            prefs.edit().putString("theme", theme.name).apply()
                                        }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Кружок цвета темы
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(theme.accent)
                                            .border(1.dp, theme.border, CircleShape)
                                    )
                                    Text(
                                        text = theme.name.replaceFirstChar { it.uppercase() },
                                        color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // РАЗДЕЛ: СЕТКА ИКОНОК ДОКА (4, 6, 8)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "КОЛИЧЕСТВО ИКОНОК В ДОКЕ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(4 to "4 иконки", 6 to "6 иконок", 8 to "8 иконок").forEach { (count, label) ->
                                val isSelected = dockSlotsCount == count
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(PultDimens.shapeInner)
                                        .background(if (isSelected) palette.accent.copy(alpha = 0.15f) else palette.surfaceLight)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) palette.accent else palette.border,
                                            shape = PultDimens.shapeInner
                                        )
                                        .clickable {
                                            onHaptic()
                                            onDockSlotsSelect(count)
                                            prefs.edit().putInt("dock_slots", count).apply()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) palette.accent else palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // РАЗДЕЛ: ПОДПИСИ ИКОНОК
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PultDimens.shapeInner)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, PultDimens.shapeInner)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Подписи под иконками",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Показывать названия приложений под значками в доке",
                                fontSize = 10.sp,
                                color = palette.textSecondary
                            )
                        }

                        Switch(
                            checked = showLabels,
                            onCheckedChange = {
                                onHaptic()
                                onShowLabelsToggle(it)
                                prefs.edit().putBoolean("show_labels", it).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.accent,
                                checkedTrackColor = palette.accent.copy(alpha = 0.4f),
                                uncheckedThumbColor = palette.textSecondary,
                                uncheckedTrackColor = palette.border
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Кнопка Готово
                PultButton(
                    text = "Готово",
                    onClick = {
                        onHaptic()
                        onClose()
                    },
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                    height = 38.dp
                )
            }
        }
    }
}

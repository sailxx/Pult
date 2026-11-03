package com.sailxx.pult.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.Client
import com.sailxx.pult.PultColors
import com.sailxx.pult.PultDimens

/**
 * Раздел 2: Трёхколоночная панель управления аудио и медиа (PLAYER).
 * 1. Аппаратная мастер-громкость Windows Core Audio со слайдером и кнопками ±5%.
 * 2. Тактильный транспорт медиаплеера (Prev, Play/Pause, Next, Stop, Space).
 * 3. Быстрые системные действия Windows (Win+L, Win+D, Alt+Tab, Выключение мониторов).
 */
@Composable
fun MediaDeck(
    client: Client,
    volume: Int,
    isMuted: Boolean,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onKey: (String) -> Unit,
    onSystem: (String) -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // КОЛОНКА 1: Системная громкость Core Audio
        PultCard(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Шапка
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "МАСТЕР-ГРОМКОСТЬ",
                            color = PultColors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(PultDimens.shapeBadge)
                                .background(if (isMuted) PultColors.danger.copy(alpha = 0.15f) else PultColors.surfaceLight)
                                .border(1.dp, if (isMuted) PultColors.danger.copy(alpha = 0.6f) else PultColors.border, PultDimens.shapeBadge)
                                .clickable {
                                    onHaptic()
                                    onToggleMute()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isMuted) "ВЫКЛЮЧЕН" else "MUTE",
                                color = if (isMuted) PultColors.danger else PultColors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Большой процент громкости
                    Text(
                        text = if (isMuted) "MUTE" else "$volume%",
                        color = if (isMuted) PultColors.danger else PultColors.textPrimary,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp
                    )

                    // Слайдер громкости с кнопками точной подстройки
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(PultDimens.shapeInner)
                                .background(PultColors.surfaceLight)
                                .border(1.dp, PultColors.border, PultDimens.shapeInner)
                                .clickable {
                                    onHaptic()
                                    onVolumeChange((volume - 5).coerceAtLeast(0))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−5", color = PultColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        PultVolumeSlider(
                            volume = volume,
                            isMuted = isMuted,
                            onVolumeChange = { onVolumeChange(it) },
                            onVolumeChangeFinished = { onVolumeChange(it) },
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(PultDimens.shapeInner)
                                .background(PultColors.surfaceLight)
                                .border(1.dp, PultColors.border, PultDimens.shapeInner)
                                .clickable {
                                    onHaptic()
                                    onVolumeChange((volume + 5).coerceAtMost(100))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+5", color = PultColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Core Audio COM API • Без задержки",
                        color = PultColors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        )

        // КОЛОНКА 2: Медиаплеер
        PultCard(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "МЕДИАПЛЕЕР",
                        color = PultColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    // Основной кластер: Prev • Play/Pause • Next
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Предыдущий
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(PultColors.surfaceLight)
                                .border(1.dp, PultColors.border, CircleShape)
                                .clickable {
                                    onHaptic()
                                    onKey("prev")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PultIconPrev(Modifier.size(18.dp))
                        }

                        // Play / Pause (Главная кнопка)
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    onHaptic()
                                    onKey("playpause")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PultIconPlayPause(Modifier.size(26.dp), color = Color.Black)
                        }

                        // Следующий
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(PultColors.surfaceLight)
                                .border(1.dp, PultColors.border, CircleShape)
                                .clickable {
                                    onHaptic()
                                    onKey("next")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PultIconNext(Modifier.size(18.dp))
                        }
                    }

                    // Дополнительные кнопки Stop и Space
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(PultDimens.shapeInner)
                                .background(PultColors.surfaceLight)
                                .border(1.dp, PultColors.border, PultDimens.shapeInner)
                                .clickable {
                                    onHaptic()
                                    onKey("stop")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PultIconStop(Modifier.size(12.dp), color = PultColors.textSecondary)
                                Text("STOP", color = PultColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.4f)
                                .height(38.dp)
                                .clip(PultDimens.shapeInner)
                                .background(PultColors.surfaceLight)
                                .border(1.dp, PultColors.border, PultDimens.shapeInner)
                                .clickable {
                                    onHaptic()
                                    onKey("space")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ПРОБЕЛ (SPACE)", color = PultColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Spotify · YouTube · Яндекс Музыка · Браузер",
                        color = PultColors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        )

        // КОЛОНКА 3: Системные действия Windows
        PultCard(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "СИСТЕМА",
                        color = PultColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SystemActionRow(title = "Блокировка", shortcut = "Win + L") {
                            onHaptic()
                            onSystem("lock")
                        }
                        SystemActionRow(title = "Рабочий стол", shortcut = "Win + D") {
                            onHaptic()
                            onSystem("desktop")
                        }
                        SystemActionRow(title = "Переключение", shortcut = "Alt + Tab") {
                            onHaptic()
                            onSystem("alttab")
                        }
                        SystemActionRow(title = "Мониторы", shortcut = "Выкл") {
                            onHaptic()
                            onSystem("monitors_off")
                        }
                    }

                    Text(
                        text = "Быстрые действия Windows",
                        color = PultColors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun SystemActionRow(
    title: String,
    shortcut: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(PultDimens.shapeInner)
            .background(PultColors.surfaceLight)
            .border(1.dp, PultColors.border, PultDimens.shapeInner)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = PultColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            text = shortcut,
            color = PultColors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

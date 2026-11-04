package com.sailxx.pult.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.LocalPultPalette
import com.sailxx.pult.MouseController
import com.sailxx.pult.PultDimens

@Composable
fun TouchpadScreen(
    mouseController: MouseController,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPultPalette.current
    var speed by remember { mutableFloatStateOf(mouseController.speedMultiplier) }
    var isLkmLocked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mouseController.releaseAll()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ОСНОВНАЯ СЕНСОРНАЯ ПАНЕЛЬ (78% ширины)
        PultCard(
            modifier = Modifier
                .weight(0.78f)
                .fillMaxHeight(),
            backgroundColor = palette.surface.copy(alpha = 0.85f),
            borderColor = palette.border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Верхний бар тачпада со слайдером чувствительности
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(PultDimens.shapeBadge)
                                .background(palette.accent)
                        )
                        Text(
                            text = "ТАЧПАД",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Слайдер скорости
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Скорость ${String.format("%.1f", speed)}×",
                            fontSize = 11.sp,
                            color = palette.textTertiary,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = speed,
                            onValueChange = {
                                speed = it
                                mouseController.speedMultiplier = it
                            },
                            valueRange = 0.5f..3.0f,
                            modifier = Modifier.width(130.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accent,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = palette.border
                            )
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Сама сенсорная область
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(PultDimens.shapeCard)
                        .background(palette.backgroundBase.copy(alpha = 0.5f))
                        .border(1.dp, palette.border, PultDimens.shapeCard)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    onHaptic()
                                    mouseController.click("left")
                                },
                                onDoubleTap = {
                                    onHaptic()
                                    mouseController.click("double")
                                },
                                onLongPress = {
                                    onHaptic()
                                    mouseController.click("right")
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                mouseController.onMove(dragAmount.x, dragAmount.y)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Фоновая разметка сетки/центра в стиле Glass
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        drawCircle(
                            color = palette.border.copy(alpha = 0.4f),
                            radius = 28.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = palette.accent.copy(alpha = 0.35f),
                            radius = 3.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Перемещайте курсор пальцем",
                            color = palette.textSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Тап: ЛКМ  •  2× Тап: Двойной клик  •  Удержание: ПКМ",
                            color = palette.textTertiary.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Нижние аппаратные кнопки мыши: ЛКМ (с Drag&Drop режимом), СКМ, ПКМ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ЛКМ / Удержание ЛКМ
                    val lkmBg by animateColorAsState(
                        targetValue = if (isLkmLocked) palette.accent else palette.surfaceLight,
                        label = "lkmBg"
                    )
                    val lkmFg by animateColorAsState(
                        targetValue = if (isLkmLocked) Color.Black else palette.textPrimary,
                        label = "lkmFg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .clip(PultDimens.shapeInner)
                            .background(lkmBg)
                            .border(1.dp, if (isLkmLocked) palette.accent else palette.border, PultDimens.shapeInner)
                            .clickable {
                                onHaptic()
                                if (isLkmLocked) {
                                    isLkmLocked = false
                                    mouseController.up("left")
                                } else {
                                    mouseController.click("left")
                                }
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isLkmLocked) "ЛКМ ЗАЖАТА" else "ЛКМ",
                                color = lkmFg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Переключатель Drag & Drop (Зажать ЛКМ)
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .fillMaxHeight()
                            .clip(PultDimens.shapeInner)
                            .background(if (isLkmLocked) palette.accent.copy(alpha = 0.2f) else palette.surfaceLight)
                            .border(1.dp, if (isLkmLocked) palette.accent else palette.border, PultDimens.shapeInner)
                            .clickable {
                                onHaptic()
                                isLkmLocked = !isLkmLocked
                                if (isLkmLocked) {
                                    mouseController.down("left")
                                } else {
                                    mouseController.up("left")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLkmLocked) "Отпустить" else "Drag & Drop",
                            color = if (isLkmLocked) palette.accent else palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Средняя кнопка мыши (СКМ)
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxHeight()
                            .clip(PultDimens.shapeInner)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, PultDimens.shapeInner)
                            .clickable {
                                onHaptic()
                                mouseController.click("middle")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "СКМ",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Правая кнопка мыши (ПКМ)
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .clip(PultDimens.shapeInner)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, PultDimens.shapeInner)
                            .clickable {
                                onHaptic()
                                mouseController.click("right")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ПКМ",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // БОКОВАЯ ПОЛОСА СКРОЛЛА (22% ширины)
        PultCard(
            modifier = Modifier
                .weight(0.22f)
                .fillMaxHeight(),
            backgroundColor = palette.surface.copy(alpha = 0.85f),
            borderColor = palette.border
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (-dragAmount.y * 14).toInt()
                            if (delta != 0) {
                                mouseController.scroll(delta)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("▲", color = palette.accent, fontSize = 12.sp)
                    Text(
                        text = "КОЛЕСО\nСКРОЛЛА",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    Text("▼", color = palette.accent, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Вверх / Вниз\nдля прокрутки",
                        color = palette.textTertiary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

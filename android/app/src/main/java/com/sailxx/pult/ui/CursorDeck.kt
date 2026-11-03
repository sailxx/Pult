package com.sailxx.pult.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.MouseController
import com.sailxx.pult.PultColors
import com.sailxx.pult.PultDimens

/**
 * Раздел 3: Высокоточная сенсорная панель мыши (CURSOR).
 * 75% ширина — тачпад со сглаженным перемещением 60 FPS, тапом, двойным тапом и ПКМ по удержанию.
 * 25% ширина — плавная полоса прокрутки колесика и аппаратные кнопки ЛКМ (с удержанием для drag&drop) и ПКМ.
 */
@Composable
fun CursorDeck(
    mouseController: MouseController,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ТАЧПАД (75% ширины)
        PultCard(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxHeight()
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
                        val dx = (dragAmount.x * 1.6f).toInt()
                        val dy = (dragAmount.y * 1.6f).toInt()
                        mouseController.onMove(dx, dy)
                    }
                },
            content = {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "СЕНСОРНЫЙ ТАЧПАД",
                        color = PultColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Плавное ведение пальцем для движения курсора",
                        color = PultColors.textSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Тап — ЛКМ  •  2× Тап — Двойной клик  •  Удержание — ПКМ",
                        color = PultColors.textTertiary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )

        // ПРАВАЯ ОБЛАСТЬ (25% ширины): Полоса прокрутки и кнопки ЛКМ/ПКМ
        Column(
            modifier = Modifier
                .weight(0.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Интерактивная полоса скролла
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(PultDimens.shapeCard)
                    .background(PultColors.surface)
                    .border(1.dp, PultColors.border, PultDimens.shapeCard)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Перетаскивание вверх/вниз для вертикального скролла
                            val delta = (-dragAmount.y * 12).toInt()
                            if (delta != 0) {
                                mouseController.scroll(delta)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("▲", color = PultColors.accent, fontSize = 10.sp)
                    Text(
                        text = "СКРОЛЛ",
                        color = PultColors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text("▼", color = PultColors.accent, fontSize = 10.sp)
                }
            }

            // Кнопка ЛКМ (с поддержкой удержания нажатия для Drag & Drop)
            var isLkmDown by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .clip(PultDimens.shapeCard)
                    .background(if (isLkmDown) PultColors.accent.copy(alpha = 0.25f) else PultColors.surfaceLight)
                    .border(
                        1.dp,
                        if (isLkmDown) PultColors.accent else PultColors.border,
                        PultDimens.shapeCard
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onHaptic()
                                isLkmDown = true
                                mouseController.down("left")
                                try {
                                    awaitRelease()
                                } finally {
                                    isLkmDown = false
                                    mouseController.up("left")
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ЛКМ",
                        color = if (isLkmDown) PultColors.accent else PultColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isLkmDown) "УДЕРЖАНИЕ" else "КЛИК / DRAG",
                        color = PultColors.textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Кнопка ПКМ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .clip(PultDimens.shapeCard)
                    .background(PultColors.surfaceLight)
                    .border(1.dp, PultColors.border, PultDimens.shapeCard)
                    .clickable {
                        onHaptic()
                        mouseController.click("right")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ПКМ",
                    color = PultColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

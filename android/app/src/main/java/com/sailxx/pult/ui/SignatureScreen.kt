package com.sailxx.pult.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.Client
import com.sailxx.pult.LocalPultPalette
import com.sailxx.pult.PultDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class SignatureStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun SignatureScreen(
    client: Client,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPultPalette.current
    val scope = rememberCoroutineScope()

    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var currentColor by remember { mutableStateOf(Color.White) }
    var currentWidth by remember { mutableFloatStateOf(4f) }
    var isSubmitting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(800, 400)) }

    val availableColors = listOf(
        Color(0xFFFFFFFF) to "Белый",
        Color(0xFF38BDF8) to "Голубой",
        Color(0xFF22C55E) to "Зеленый",
        Color(0xFFF59E0B) to "Оранжевый",
        Color(0xFF0F172A) to "Темный"
    )

    fun sendToPc(action: String) {
        if (strokes.isEmpty() && currentPoints.isEmpty()) {
            feedbackMessage = "Сначала нарисуйте подпись на холсте"
            return
        }

        scope.launch {
            isSubmitting = true
            feedbackMessage = "Отправка на ПК..."
            try {
                val pngBytes = withContext(Dispatchers.Default) {
                    val w = canvasSize.width.coerceAtLeast(400)
                    val h = canvasSize.height.coerceAtLeast(200)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    // Прозрачный фон для идеальной вставки в документы
                    canvas.drawColor(android.graphics.Color.TRANSPARENT)

                    val paint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }

                    for (stroke in strokes) {
                        if (stroke.points.size < 2) continue
                        paint.color = stroke.color.toArgb()
                        paint.strokeWidth = stroke.strokeWidth * 1.5f

                        val path = android.graphics.Path()
                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        canvas.drawPath(path, paint)
                    }

                    val stream = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.toByteArray()
                }

                val savedPath = client.sendSignature(pngBytes, action)
                feedbackMessage = when (action) {
                    "clipboard" -> "Подпись скопирована в буфер ПК (Ctrl+V)!"
                    "save" -> if (savedPath != null) "Сохранено: Pictures\\Pult\\Signatures" else "Сохранено на ПК!"
                    else -> "Подпись скопирована и сохранена на ПК!"
                }
            } catch (_: Exception) {
                feedbackMessage = "Ошибка отправки на ПК"
            } finally {
                isSubmitting = false
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ХОЛСТ ДЛЯ РИСОВАНИЯ (78% ширины)
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
                // Верхний бар с индикатором и кнопками Undo / Clear
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
                            text = "ХОЛСТ ПОДПИСИ И НАБРОСКОВ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Кнопка Отменить (Undo)
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(PultDimens.shapeBadge)
                                .background(palette.surfaceLight)
                                .border(1.dp, palette.border, PultDimens.shapeBadge)
                                .clickable {
                                    onHaptic()
                                    if (strokes.isNotEmpty()) {
                                        strokes.removeAt(strokes.lastIndex)
                                    }
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Отменить",
                                color = if (strokes.isNotEmpty()) palette.textPrimary else palette.textTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Кнопка Очистить (Clear)
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(PultDimens.shapeBadge)
                                .background(palette.surfaceLight)
                                .border(1.dp, palette.border, PultDimens.shapeBadge)
                                .clickable {
                                    onHaptic()
                                    strokes.clear()
                                    currentPoints = emptyList()
                                    feedbackMessage = null
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Очистить",
                                color = palette.danger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Область рисования с распознаванием штрихов
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(PultDimens.shapeCard)
                        .background(palette.backgroundBase.copy(alpha = 0.6f))
                        .border(1.dp, palette.border, PultDimens.shapeCard)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onHaptic()
                                    currentPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.size >= 2) {
                                        strokes.add(
                                            SignatureStroke(
                                                points = currentPoints,
                                                color = currentColor,
                                                strokeWidth = currentWidth
                                            )
                                        )
                                    }
                                    currentPoints = emptyList()
                                },
                                onDragCancel = {
                                    currentPoints = emptyList()
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        canvasSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())

                        // Линия базовой подписи
                        drawLine(
                            color = palette.border.copy(alpha = 0.4f),
                            start = Offset(40f, size.height * 0.75f),
                            end = Offset(size.width - 40f, size.height * 0.75f),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Отрисовка всех зафиксированных штрихов
                        for (stroke in strokes) {
                            if (stroke.points.size < 2) continue
                            val p = Path()
                            p.moveTo(stroke.points[0].x, stroke.points[0].y)
                            for (i in 1 until stroke.points.size) {
                                p.lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                            drawPath(
                                path = p,
                                color = stroke.color,
                                style = Stroke(
                                    width = stroke.strokeWidth.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Текущий штрих в процессе движения пальца
                        if (currentPoints.size >= 2) {
                            val p = Path()
                            p.moveTo(currentPoints[0].x, currentPoints[0].y)
                            for (i in 1 until currentPoints.size) {
                                p.lineTo(currentPoints[i].x, currentPoints[i].y)
                            }
                            drawPath(
                                path = p,
                                color = currentColor,
                                style = Stroke(
                                    width = currentWidth.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (strokes.isEmpty() && currentPoints.isEmpty()) {
                        Text(
                            text = "Распишитесь пальцем или стилусом здесь",
                            color = palette.textTertiary.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // ПРАВАЯ ПАНЕЛЬ ИНСТРУМЕНТОВ И ДЕЙСТВИЙ (22% ширины)
        PultCard(
            modifier = Modifier
                .weight(0.22f)
                .fillMaxHeight(),
            backgroundColor = palette.surface.copy(alpha = 0.85f),
            borderColor = palette.border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ЦВЕТ ПЕРА",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    // Выбор цвета пера
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableColors.forEach { (c, _) ->
                            val selected = currentColor == c
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) palette.accent else palette.border,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onHaptic()
                                        currentColor = c
                                    }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "ТОЛЩИНА",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    // Выбор толщины линии
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(2f to "Тонко", 4f to "Средне", 8f to "Жирно").forEach { (w, title) ->
                            val selected = currentWidth == w
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clip(PultDimens.shapeBadge)
                                    .background(if (selected) palette.surfaceLight else Color.Transparent)
                                    .border(1.dp, if (selected) palette.accent else palette.border, PultDimens.shapeBadge)
                                    .clickable {
                                        onHaptic()
                                        currentWidth = w
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 10.sp,
                                    color = if (selected) palette.textPrimary else palette.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = feedbackMessage != null) {
                    Text(
                        text = feedbackMessage ?: "",
                        color = palette.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp
                    )
                }

                // Кнопки отправки
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PultButton(
                        text = "Вставить на ПК",
                        onClick = {
                            onHaptic()
                            sendToPc("clipboard")
                        },
                        primary = true,
                        enabled = !isSubmitting && strokes.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        height = 36.dp
                    )

                    PultButton(
                        text = "Сохранить на ПК",
                        onClick = {
                            onHaptic()
                            sendToPc("save")
                        },
                        enabled = !isSubmitting && strokes.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        height = 32.dp
                    )
                }
            }
        }
    }
}

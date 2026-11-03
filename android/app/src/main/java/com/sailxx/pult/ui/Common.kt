package com.sailxx.pult.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.PultColors
import com.sailxx.pult.PultDimens

/**
 * Базовый контейнер с фирменным стилем Pult.
 */
@Composable
fun PultCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PultColors.surface,
    borderColor: Color = PultColors.border,
    shape: RoundedCornerShape = PultDimens.shapeCard,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape),
        content = content
    )
}

/**
 * Тактильная кнопка с точной геометрией и чистыми шрифтами.
 */
@Composable
fun PultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 40.dp
) {
    val bg = when {
        !enabled -> PultColors.surfaceLight.copy(alpha = 0.5f)
        danger -> PultColors.danger
        primary -> PultColors.accent
        else -> PultColors.surfaceLight
    }
    val fg = when {
        !enabled -> PultColors.textSecondary.copy(alpha = 0.4f)
        danger -> Color.White
        primary -> Color(0xFF030712)
        else -> PultColors.textPrimary
    }
    val borderCol = if (!primary && !danger && enabled) PultColors.border else Color.Transparent

    Box(
        modifier = modifier
            .height(height)
            .clip(PultDimens.shapeButton)
            .background(bg)
            .then(if (borderCol != Color.Transparent) Modifier.border(1.dp, borderCol, PultDimens.shapeButton) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Поле ввода с мягким фокусом и чистой типографикой.
 */
@Composable
fun PultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    type: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = PultColors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(PultDimens.shapeInner)
                .background(PultColors.surface)
                .border(1.dp, PultColors.border, PultDimens.shapeInner)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = PultColors.textSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = PultColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(PultColors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = type),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}

/**
 * Премиальный тактильный слайдер громкости Pult.
 * Интерактивная полоса с четкой шкалой и мгновенной реакцией на касание и перетаскивание.
 */
@Composable
fun PultVolumeSlider(
    volume: Int,
    isMuted: Boolean,
    onVolumeChange: (Int) -> Unit,
    onVolumeChangeFinished: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPercent by remember { mutableStateOf(volume.toFloat()) }

    val displayPercent = if (isDragging) dragPercent else volume.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = (displayPercent / 100f).coerceIn(0f, 1f),
        label = "volumeProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PultColors.surfaceLight)
            .border(1.dp, PultColors.border, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val pct = (offset.x / size.width.toFloat() * 100f).toInt().coerceIn(0, 100)
                    onVolumeChange(pct)
                    onVolumeChangeFinished(pct)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragPercent = (offset.x / size.width.toFloat() * 100f).coerceIn(0f, 100f)
                        onVolumeChange(dragPercent.toInt())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragPercent = (change.position.x / size.width.toFloat() * 100f).coerceIn(0f, 100f)
                        onVolumeChange(dragPercent.toInt())
                    },
                    onDragEnd = {
                        isDragging = false
                        onVolumeChangeFinished(dragPercent.toInt())
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        // Заполненная часть слайдера
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fillWidth = size.width * animatedProgress
            val fillColor = if (isMuted) PultColors.danger.copy(alpha = 0.8f) else PultColors.accent
            
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset.Zero,
                    size = Size(fillWidth, size.height),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
            }

            // Тонкий тактильный бегунок
            if (fillWidth > 4.dp.toPx()) {
                val thumbX = (fillWidth - 3.dp.toPx()).coerceAtLeast(0f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(thumbX, 4.dp.toPx()),
                    size = Size(3.dp.toPx(), size.height - 8.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

/**
 * Геометрические векторные иконки в Apple/Claude минимализме.
 */
@Composable
fun PultIconPlayPause(
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Play треугольник
        val p = Path().apply {
            moveTo(w * 0.18f, h * 0.12f)
            lineTo(w * 0.58f, h * 0.50f)
            lineTo(w * 0.18f, h * 0.88f)
            close()
        }
        drawPath(p, color)

        // Pause полоса
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.72f, h * 0.14f),
            size = Size(w * 0.14f, h * 0.72f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

@Composable
fun PultIconPrev(modifier: Modifier = Modifier, color: Color = PultColors.textPrimary) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p1 = Path().apply {
            moveTo(w * 0.46f, h * 0.16f)
            lineTo(w * 0.12f, h * 0.50f)
            lineTo(w * 0.46f, h * 0.84f)
            close()
        }
        val p2 = Path().apply {
            moveTo(w * 0.88f, h * 0.16f)
            lineTo(w * 0.54f, h * 0.50f)
            lineTo(w * 0.88f, h * 0.84f)
            close()
        }
        drawPath(p1, color)
        drawPath(p2, color)
    }
}

@Composable
fun PultIconNext(modifier: Modifier = Modifier, color: Color = PultColors.textPrimary) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p1 = Path().apply {
            moveTo(w * 0.12f, h * 0.16f)
            lineTo(w * 0.46f, h * 0.50f)
            lineTo(w * 0.12f, h * 0.84f)
            close()
        }
        val p2 = Path().apply {
            moveTo(w * 0.54f, h * 0.16f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.54f, h * 0.84f)
            close()
        }
        drawPath(p1, color)
        drawPath(p2, color)
    }
}

@Composable
fun PultIconStop(modifier: Modifier = Modifier, color: Color = PultColors.textPrimary) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.22f),
            size = Size(w * 0.56f, h * 0.56f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

package com.sailxx.pult.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.sailxx.pult.LocalPultPalette
import com.sailxx.pult.PultThemePalette
import java.io.File

/**
 * Премиальный фоновый градиентный холст Pult в стиле Windock.
 * Создаёт мягкое трёхмерное свечение с радиальными сферами (Aurora, Dusk, Ember, Midnight, OLED)
 * либо отображает пользовательское фото с затемняющим матовым слоем для идеальной читаемости интерфейса.
 */
@Composable
fun PultWallpaper(
    modifier: Modifier = Modifier,
    palette: PultThemePalette = LocalPultPalette.current,
    customImagePath: String? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Если выбрано пользовательское изображение с диска
        var customBitmap by remember(customImagePath) {
            mutableStateOf(
                if (!customImagePath.isNullOrBlank()) {
                    try {
                        val file = File(customImagePath)
                        if (file.exists() && file.length() > 0) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                } else null
            )
        }

        if (customBitmap != null) {
            Image(
                bitmap = customBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Затемняющий и рассеивающий scrim для сохранения контраста
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        } else {
            // Плавный переход цветов при смене палитры
            val bgBase by animateColorAsState(palette.backgroundBase, tween(400), label = "bgBase")
            val gradA by animateColorAsState(palette.gradientA, tween(400), label = "gradA")
            val gradB by animateColorAsState(palette.gradientB, tween(400), label = "gradB")
            val gradC by animateColorAsState(palette.gradientC, tween(400), label = "gradC")

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBase)
            ) {
                val w = size.width
                val h = size.height

                // Радиальное свечение Сфера A: верхний левый угол
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(gradA.copy(alpha = 0.50f), gradA.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w * 0.15f, h * 0.10f),
                        radius = (w + h) * 0.45f
                    )
                )

                // Радиальное свечение Сфера B: нижний правый сектор
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(gradB.copy(alpha = 0.45f), gradB.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w * 0.82f, h * 0.88f),
                        radius = (w + h) * 0.42f
                    )
                )

                // Радиальное свечение Сфера C: центр-справа для гармоничной глубины
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(gradC.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(w * 0.65f, h * 0.25f),
                        radius = (w + h) * 0.35f
                    )
                )

                // Тонкий виньеточный градиент по краям для фокуса на доке и контенте
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = (w + h) * 0.55f
                    )
                )
            }
        }
    }
}

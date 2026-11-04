package com.sailxx.pult.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.Client
import com.sailxx.pult.LocalPultPalette
import com.sailxx.pult.PultDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureScreen(
    client: Client,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPultPalette.current
    val scope = rememberCoroutineScope()

    var screenshotBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var captureTimestamp by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun takeScreenshot() {
        scope.launch {
            isCapturing = true
            statusMessage = "Захват экрана компьютера..."
            try {
                val bytes = client.captureScreen()
                if (bytes != null && bytes.isNotEmpty()) {
                    val bmp = withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    screenshotBitmap = bmp
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    captureTimestamp = sdf.format(Date())
                    statusMessage = "Снимок экрана успешно получен"
                } else {
                    statusMessage = "Не удалось получить снимок с ПК"
                }
            } catch (_: Exception) {
                statusMessage = "Ошибка подключения к ПК"
            } finally {
                isCapturing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        takeScreenshot()
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ОБЛАСТЬ ПРОСМОТРА СКРИНШОТА (78% ширины)
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
                // Заголовок и статус
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
                                .background(palette.live)
                        )
                        Text(
                            text = "СНИМОК ЭКРАНА WINDOWS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (captureTimestamp != null) {
                        Text(
                            text = "Время: $captureTimestamp",
                            color = palette.textTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Контейнер отображения снимка
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(PultDimens.shapeCard)
                        .background(palette.backgroundBase.copy(alpha = 0.6f))
                        .border(1.dp, palette.border, PultDimens.shapeCard),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = screenshotBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Desktop Screenshot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(PultDimens.shapeInner)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isCapturing) "Загрузка снимка экрана..." else "Снимок экрана не загружен",
                                color = palette.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Нажмите «Сделать снимок» для обновления",
                                color = palette.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // БОКОВАЯ ПАНЕЛЬ ДЕЙСТВИЙ (22% ширины)
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
                        text = "ДЕЙСТВИЯ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    PultButton(
                        text = if (isCapturing) "Захват..." else "Сделать снимок",
                        onClick = {
                            onHaptic()
                            takeScreenshot()
                        },
                        primary = true,
                        enabled = !isCapturing,
                        modifier = Modifier.fillMaxWidth(),
                        height = 36.dp
                    )

                    Spacer(Modifier.height(4.dp))

                    PultButton(
                        text = "В буфер ПК",
                        onClick = {
                            onHaptic()
                            scope.launch {
                                isCapturing = true
                                val ok = client.copyCaptureToClipboard()
                                statusMessage = if (ok) "Снимок скопирован в буфер ПК!" else "Ошибка копирования"
                                isCapturing = false
                            }
                        },
                        enabled = !isCapturing && screenshotBitmap != null,
                        modifier = Modifier.fillMaxWidth(),
                        height = 34.dp
                    )

                    PultButton(
                        text = "Сохранить на ПК",
                        onClick = {
                            onHaptic()
                            scope.launch {
                                isCapturing = true
                                val path = client.saveCapture(copyToClipboard = false)
                                statusMessage = if (path != null) "Сохранено: Pictures\\Pult" else "Ошибка сохранения"
                                isCapturing = false
                            }
                        },
                        enabled = !isCapturing && screenshotBitmap != null,
                        modifier = Modifier.fillMaxWidth(),
                        height = 34.dp
                    )
                }

                AnimatedVisibility(visible = statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        color = palette.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp
                    )
                }

                // Информационная плашка
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PultDimens.shapeInner)
                        .background(palette.surfaceLight)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Скриншот захватывает активный рабочий стол Windows с нативным разрешением",
                        color = palette.textTertiary,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

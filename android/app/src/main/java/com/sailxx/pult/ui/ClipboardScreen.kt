package com.sailxx.pult.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.Client
import com.sailxx.pult.LocalPultPalette
import com.sailxx.pult.PultColors
import com.sailxx.pult.PultDimens
import kotlinx.coroutines.launch

@Composable
fun ClipboardScreen(
    client: Client,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalPultPalette.current

    var pcClipboardText by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refreshClipboard() {
        scope.launch {
            isLoading = true
            try {
                val text = client.getClipboard()
                pcClipboardText = text
                statusMessage = null
            } catch (_: Exception) {
                statusMessage = "Ошибка подключения к ПК"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshClipboard()
    }

    val androidClipboard = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Левая колонка: Текущее содержимое буфера Windows
        PultCard(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            backgroundColor = palette.surface.copy(alpha = 0.85f),
            borderColor = palette.border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                            text = "БУФЕР ОБМЕНА WINDOWS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Кнопка обновления
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(PultDimens.shapeBadge)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, PultDimens.shapeBadge)
                            .clickable {
                                onHaptic()
                                refreshClipboard()
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLoading) "Загрузка..." else "Обновить",
                            fontSize = 11.sp,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Контент буфера
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(PultDimens.shapeInner)
                        .background(palette.backgroundBase.copy(alpha = 0.6f))
                        .border(1.dp, palette.border, PultDimens.shapeInner)
                        .padding(12.dp)
                ) {
                    if (pcClipboardText.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Буфер обмена ПК пуст",
                                color = palette.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Скопируйте текст на компьютере или отправьте с телефона",
                                color = palette.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = pcClipboardText!!,
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Действия над буфером ПК
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Скопировать в телефон
                    PultButton(
                        text = "В телефон",
                        onClick = {
                            onHaptic()
                            pcClipboardText?.let { text ->
                                val clip = ClipData.newPlainText("Pult", text)
                                androidClipboard.setPrimaryClip(clip)
                                statusMessage = "Скопировано в буфер телефона"
                            }
                        },
                        enabled = !pcClipboardText.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        height = 34.dp
                    )

                    // Очистить буфер ПК
                    PultButton(
                        text = "Очистить ПК",
                        onClick = {
                            onHaptic()
                            scope.launch {
                                try {
                                    client.clearClipboard()
                                    pcClipboardText = ""
                                    statusMessage = "Буфер ПК очищен"
                                } catch (_: Exception) {
                                    statusMessage = "Ошибка очистки"
                                }
                            }
                        },
                        danger = true,
                        enabled = !pcClipboardText.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        height = 34.dp
                    )
                }
            }
        }

        // Правая колонка: Отправка текста на ПК
        PultCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            backgroundColor = palette.surface.copy(alpha = 0.85f),
            borderColor = palette.border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                            text = "ОТПРАВИТЬ НА ПК",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Быстрая вставка из телефона
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(PultDimens.shapeBadge)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, PultDimens.shapeBadge)
                            .clickable {
                                onHaptic()
                                val clip = androidClipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    inputText = clip.getItemAt(0).text?.toString() ?: ""
                                }
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Вставить из телефона",
                            fontSize = 11.sp,
                            color = palette.accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Поле ввода текста
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(PultDimens.shapeInner)
                        .background(palette.backgroundBase.copy(alpha = 0.6f))
                        .border(1.dp, palette.border, PultDimens.shapeInner)
                        .padding(12.dp)
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Введите текст или вставьте ссылку для ПК...",
                            color = palette.textTertiary,
                            fontSize = 12.sp
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AnimatedVisibility(visible = statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        color = palette.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Кнопка отправки в буфер ПК
                PultButton(
                    text = "Скопировать в буфер ПК",
                    onClick = {
                        onHaptic()
                        scope.launch {
                            try {
                                client.setClipboard(inputText)
                                pcClipboardText = inputText
                                statusMessage = "Отправлено в буфер ПК"
                                inputText = ""
                            } catch (_: Exception) {
                                statusMessage = "Ошибка отправки"
                            }
                        }
                    },
                    primary = true,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    height = 36.dp
                )
            }
        }
    }
}

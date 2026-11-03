package com.sailxx.pult.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.*
import kotlinx.coroutines.launch

/**
 * Премиальный горизонтальный экран подключения к Windows PC.
 */
@Composable
fun ConnectScreen(
    initIp: String,
    initPin: String,
    onConnected: (String, String, String) -> Unit,
    onHaptic: () -> Unit
) {
    var ip by remember { mutableStateOf(initIp) }
    var pin by remember { mutableStateOf(initPin) }
    var status by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    val hosts = remember { mutableStateListOf<Host>() }
    val scope = rememberCoroutineScope()

    fun connectTo(targetIp: String, targetPin: String) {
        scope.launch {
            onHaptic()
            isBusy = true
            isError = false
            val clean = cleanIp(targetIp)
            ip = clean
            status = "Подключение к $clean..."

            val host = ping(clean, 4000)
            if (host == null) {
                isError = true
                status = "Компьютер не отвечает ($clean:$HTTP_PORT).\nПроверьте, что Pult запущен на Windows."
            } else {
                try {
                    Client(clean, targetPin.trim()).apps()
                    onConnected(clean, targetPin.trim(), host.name)
                } catch (e: WrongPin) {
                    isError = true
                    status = "Неверный PIN-код. Проверьте код в трее Windows."
                } catch (e: Exception) {
                    isError = true
                    status = "Ошибка связи: ${e.message ?: "сбой сети"}"
                }
            }
            isBusy = false
        }
    }

    fun search() {
        scope.launch {
            onHaptic()
            isBusy = true
            isError = false
            status = "Поиск компьютеров в сети..."
            hosts.clear()
            val found = discover()
            hosts.addAll(found)
            if (found.isEmpty()) {
                isError = true
                status = "Компьютеры не найдены. Введите IP вручную."
            } else {
                status = ""
                if (found.size == 1 && ip.isBlank()) {
                    ip = found[0].ip
                }
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        if (ip.isBlank()) search()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Левая колонка: Форма входа и заголовок
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Бренд
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(PultDimens.shapeCard)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(22.dp)) {
                        val w = size.width
                        val h = size.height
                        val rx = w * 0.16f
                        val body = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = w * 0.28f, top = h * 0.18f,
                                    right = w * 0.72f, bottom = h * 0.90f,
                                    radiusX = rx, radiusY = rx
                                )
                            )
                        }
                        drawPath(body, Color.Black, style = Stroke(width = 3.2f))
                        drawCircle(Color.Black, radius = 2.4f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.52f))
                    }
                }

                Column {
                    Text(
                        text = "Pult",
                        color = PultColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Горизонтальная док-панель Windows",
                        color = PultColors.textSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            // Поля ввода
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PultTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = "IP АДРЕС КОМПЬЮТЕРА",
                    placeholder = "192.168.1.xxx",
                    type = KeyboardType.Uri
                )

                PultTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = "PIN КОД (6 ЦИФР ИЗ ТРЕЯ)",
                    placeholder = "••••••",
                    type = KeyboardType.NumberPassword
                )
            }

            // Кнопки действий
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PultButton(
                    text = if (isBusy) "Подключение..." else "Подключить",
                    primary = true,
                    enabled = !isBusy && ip.isNotBlank() && pin.length == 6,
                    modifier = Modifier.weight(1.3f),
                    onClick = { connectTo(ip, pin) }
                )

                PultButton(
                    text = "Поиск",
                    primary = false,
                    enabled = !isBusy,
                    modifier = Modifier.weight(0.9f),
                    onClick = { search() }
                )
            }
        }

        // Правая колонка: Список найденных ПК и статус
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ДОСТУПНЫЕ КОМПЬЮТЕРЫ",
                color = PultColors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            if (hosts.isEmpty()) {
                PultCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    content = {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (status.isNotEmpty()) status else "Нажмите «Поиск», чтобы найти активный Pult в локальной сети",
                                color = if (isError) PultColors.danger else PultColors.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hosts, key = { it.ip }) { host ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PultDimens.shapeCard)
                                .background(PultColors.surface)
                                .border(1.dp, PultColors.border, PultDimens.shapeCard)
                                .clickable {
                                    ip = host.ip
                                    if (pin.length == 6) {
                                        connectTo(host.ip, pin)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = host.name,
                                    color = PultColors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = host.ip,
                                    color = PultColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(PultColors.success, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

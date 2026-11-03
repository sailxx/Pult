package com.sailxx.pult

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF090B0E)
private val Surface = Color(0xFF14171D)
private val SurfaceLight = Color(0xFF1F242D)
private val Line = Color(0xFF282F3B)
private val Ink = Color(0xFFF3F4F6)
private val Dim = Color(0xFF8C95A3)
private val Accent = Color(0xFFE8B04A)
private val Bad = Color(0xFFE5675A)
private val Good = Color(0xFF4ADE80)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindToWifi()
        val prefs = getSharedPreferences("pult", Context.MODE_PRIVATE)
        setContent { Root(prefs) }
    }
}

/** Привязывает сетевой трафик приложения к Wi-Fi, чтобы он шёл мимо VPN телефона. */
private fun Context.bindToWifi() {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val wifi = cm.allNetworks.firstOrNull {
        val c = cm.getNetworkCapabilities(it)
        c != null && c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) &&
            !c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
    }
    if (wifi != null) cm.bindProcessToNetwork(wifi)
}

@Composable
private fun Root(prefs: android.content.SharedPreferences) {
    var ip by remember { mutableStateOf(prefs.getString("ip", "") ?: "") }
    var pin by remember { mutableStateOf(prefs.getString("pin", "") ?: "") }
    var hostName by remember { mutableStateOf(prefs.getString("host_name", "ПК") ?: "ПК") }
    var connected by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Bg).safeDrawingPadding()) {
        if (connected) {
            Dock(
                client = Client(ip, pin),
                hostName = hostName,
                prefs = prefs,
                onDisconnect = { connected = false }
            )
        } else {
            ConnectScreen(
                initIp = ip,
                initPin = pin,
                onConnected = { newIp, newPin, name ->
                    ip = newIp; pin = newPin; hostName = name
                    prefs.edit()
                        .putString("ip", newIp)
                        .putString("pin", newPin)
                        .putString("host_name", name)
                        .apply()
                    connected = true
                }
            )
        }
    }
}

@Composable
private fun ConnectScreen(initIp: String, initPin: String, onConnected: (String, String, String) -> Unit) {
    var ip by remember { mutableStateOf(initIp) }
    var pin by remember { mutableStateOf(initPin) }
    var status by remember { mutableStateOf("") }
    var statusBad by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val hosts = remember { mutableStateListOf<Host>() }
    val scope = rememberCoroutineScope()

    fun connect() {
        scope.launch {
            busy = true; statusBad = false
            val addr = cleanIp(ip)
            ip = addr
            status = "Подключаюсь к $addr..."
            val host = ping(addr, 4000)
            if (host == null) {
                statusBad = true
                status = "ПК не отвечает на $addr:$HTTP_PORT.\nПричина: $lastPingError\n" +
                    "Проверьте, что Pult запущен на ПК и телефон в одной Wi-Fi сети."
            } else {
                try {
                    Client(addr, pin.trim()).apps()
                    onConnected(addr, pin.trim(), host.name)
                } catch (e: WrongPin) {
                    statusBad = true; status = "Неверный PIN. Посмотрите его в окне «Адрес и PIN» в трее ПК."
                } catch (e: Exception) {
                    statusBad = true; status = "Ошибка: ${e.message}"
                }
            }
            busy = false
        }
    }

    fun search() {
        scope.launch {
            busy = true; statusBad = false; status = "Ищу ПК в локальной сети..."
            hosts.clear()
            val r = discover()
            hosts.addAll(r)
            status = if (r.isEmpty()) "Ничего не найдено. Введите адрес ПК вручную (он указан в окне на ПК)." else ""
            statusBad = r.isEmpty()
            if (r.size == 1 && ip.isBlank()) ip = r[0].ip
            busy = false
        }
    }

    LaunchedEffect(Unit) { if (ip.isBlank()) search() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(24.dp)) { drawRemoteIcon() }
            }
            Column {
                Text("Pult", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Дистанционный пульт для ПК", color = Dim, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(4.dp))
        Field("Адрес ПК (IP-адрес в Wi-Fi сети)", ip, KeyboardType.Uri) { ip = it }
        Field("PIN-код (6 цифр)", pin, KeyboardType.NumberPassword) { pin = it.filter(Char::isDigit).take(6) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn("Подключить", primary = true, enabled = !busy && ip.isNotBlank() && pin.length == 6, modifier = Modifier.weight(1f)) { connect() }
            Btn("Поиск ПК", primary = false, enabled = !busy, modifier = Modifier.weight(1f)) { search() }
        }

        if (status.isNotEmpty()) Text(status, color = if (statusBad) Bad else Dim, fontSize = 13.sp, lineHeight = 18.sp)

        if (hosts.isNotEmpty()) {
            Text("Найденные компьютеры:", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            hosts.forEach { h ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).clickable { ip = h.ip }.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(h.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(h.ip, color = Dim, fontSize = 13.sp)
                    }
                    Box(Modifier.size(8.dp).background(Good, CircleShape))
                }
            }
        }
    }
}

private enum class TabMode(val title: String) {
    APPS("Приложения"),
    MEDIA("Медиа"),
    TRACKPAD("Мышь"),
    SYSTEM("Система"),
    TASKS("Окна")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Dock(
    client: Client,
    hostName: String,
    prefs: android.content.SharedPreferences,
    onDisconnect: () -> Unit
) {
    var mode by remember { mutableStateOf(TabMode.APPS) }
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var tasks by remember { mutableStateOf<List<TaskItem>?>(null) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var favs by remember { mutableStateOf(prefs.getStringSet("favs", emptySet()) ?: emptySet()) }
    var volume by remember { mutableStateOf(50) }
    var isMuted by remember { mutableStateOf(false) }
    val icons = remember { mutableStateOf(mapOf<String, ImageBitmap>()) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
    }

    fun fire(block: suspend () -> Unit) {
        haptic()
        scope.launch {
            try {
                block()
                error = ""
            } catch (e: WrongPin) {
                onDisconnect()
            } catch (e: Exception) {
                error = "Нет связи с ПК"
            }
        }
    }

    // Первоначальная загрузка приложений и статуса звука
    LaunchedEffect(Unit) {
        try {
            val v = client.volume()
            volume = v.volume
            isMuted = v.muted
        } catch (_: Exception) {}

        try {
            apps = client.apps()
        } catch (e: WrongPin) {
            onDisconnect()
        } catch (e: Exception) {
            error = "Нет связи с ПК"
            apps = emptyList()
        }
    }

    // Фоновое периодическое обновление звука раз в 5 сек
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            try {
                val v = client.volume()
                volume = v.volume
                isMuted = v.muted
            } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        
        // 1. Верхняя панель: статус ПК, громкость и кнопка выхода
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(Good, CircleShape))
                Text(hostName, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mute toggle
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (isMuted) Bad.copy(alpha = 0.2f) else SurfaceLight)
                        .clickable {
                            fire {
                                val res = client.toggleMute()
                                isMuted = res.muted
                                volume = res.volume
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isMuted) "🔇" else "🔊", fontSize = 14.sp)
                }

                // Volume label
                Text("$volume%", color = if (isMuted) Dim else Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                // Disconnect
                Text("Выход", color = Dim, fontSize = 13.sp, modifier = Modifier.clickable { onDisconnect() })
            }
        }

        // 2. Интерактивный слайдер громкости
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("—", color = Dim, fontSize = 16.sp, modifier = Modifier.clickable {
                fire {
                    val next = (volume - 5).coerceAtLeast(0)
                    val r = client.setVolume(next)
                    volume = r.volume; isMuted = r.muted
                }
            })
            Slider(
                value = volume.toFloat(),
                onValueChange = { volume = it.toInt() },
                onValueChangeFinished = {
                    fire {
                        val r = client.setVolume(volume)
                        volume = r.volume; isMuted = r.muted
                    }
                },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Line
                )
            )
            Text("+", color = Dim, fontSize = 16.sp, modifier = Modifier.clickable {
                fire {
                    val next = (volume + 5).coerceAtMost(100)
                    val r = client.setVolume(next)
                    volume = r.volume; isMuted = r.muted
                }
            })
        }

        // 3. Переключатель режимов пульта (Tabs)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TabMode.values().forEach { t ->
                val active = t == mode
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                        .background(if (active) Accent else Color.Transparent)
                        .clickable { haptic(); mode = t }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        t.title,
                        color = if (active) Color(0xFF161105) else Dim,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = Bad, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }

        // 4. Контент выбранного режима
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (mode) {
                TabMode.APPS -> AppsTab(
                    apps = apps,
                    query = query,
                    onQueryChange = { query = it },
                    favs = favs,
                    onToggleFav = { id ->
                        favs = if (id in favs) favs - id else favs + id
                        prefs.edit().putStringSet("favs", favs).apply()
                    },
                    icons = icons.value,
                    onLoadIcon = { id ->
                        scope.launch {
                            val bytes = client.icon(id)
                            if (bytes != null && bytes.isNotEmpty()) {
                                val bmp = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
                                if (bmp != null) icons.value = icons.value + (id to bmp)
                            }
                        }
                    },
                    onLaunch = { fire { client.launch(it) } }
                )

                TabMode.MEDIA -> MediaTab(client = client, fire = ::fire)

                TabMode.TRACKPAD -> TrackpadTab(client = client, haptic = ::haptic)

                TabMode.SYSTEM -> SystemTab(client = client, fire = ::fire)

                TabMode.TASKS -> TasksTab(
                    client = client,
                    fire = ::fire,
                    tasks = tasks,
                    onRefresh = {
                        scope.launch {
                            try { tasks = client.tasks() } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

// ----------------- TAB 1: APPS -----------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppsTab(
    apps: List<AppItem>?,
    query: String,
    onQueryChange: (String) -> Unit,
    favs: Set<String>,
    onToggleFav: (String) -> Unit,
    icons: Map<String, ImageBitmap>,
    onLoadIcon: (String) -> Unit,
    onLaunch: (String) -> Unit
) {
    val shown = (apps ?: emptyList())
        .filter { it.name.contains(query, ignoreCase = true) }
        .sortedWith(compareByDescending<AppItem> { it.id in favs }.thenBy { it.name.lowercase() })

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
            cursorBrush = SolidColor(Accent),
            decorationBox = { inner ->
                Box(Modifier.padding(vertical = 2.dp)) {
                    if (query.isEmpty()) Text("Поиск приложения...", color = Dim, fontSize = 15.sp)
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).padding(12.dp)
        )

        if (apps == null) {
            SkeletonGrid()
        } else if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ничего не найдено", color = Dim, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                GridCells.Adaptive(84.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(shown, key = { it.id }) { app ->
                    val isFav = app.id in favs
                    LaunchedEffect(app.id) {
                        if (icons[app.id] == null) onLoadIcon(app.id)
                    }

                    Column(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (isFav) Color(0xFF232832) else Surface)
                            .then(if (isFav) Modifier.border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)) else Modifier)
                            .combinedClickable(
                                onClick = { onLaunch(app.id) },
                                onLongClick = { onToggleFav(app.id) }
                            ).padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val bmp = icons[app.id]
                        if (bmp != null) Image(bmp, null, Modifier.size(42.dp))
                        else Box(Modifier.size(42.dp).background(Line, RoundedCornerShape(6.dp)))

                        Text(
                            app.name,
                            color = Ink,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ----------------- TAB 2: MEDIA -----------------
@Composable
private fun MediaTab(client: Client, fire: (suspend () -> Unit) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Большой круг управления
        Box(
            Modifier.size(220.dp).clip(CircleShape).background(Surface).border(2.dp, Line, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Центральная кнопка Play/Pause
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(Accent)
                    .clickable { fire { client.key("playpause") } },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(30.dp)) {
                    val w = size.width; val h = size.height
                    val p = Path().apply { moveTo(w * 0.2f, 0f); lineTo(w * 0.95f, h / 2f); lineTo(w * 0.2f, h); close() }
                    drawPath(p, Color(0xFF161105))
                }
            }

            // Кнопка Предыдущий трек (Слева)
            Box(
                Modifier.align(Alignment.CenterStart).padding(start = 14.dp).size(48.dp).clip(CircleShape)
                    .clickable { fire { client.key("prev") } },
                contentAlignment = Alignment.Center
            ) {
                Text("⏮", fontSize = 22.sp, color = Ink)
            }

            // Кнопка Следующий трек (Справа)
            Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(48.dp).clip(CircleShape)
                    .clickable { fire { client.key("next") } },
                contentAlignment = Alignment.Center
            ) {
                Text("⏭", fontSize = 22.sp, color = Ink)
            }

            // Громкость + (Сверху)
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 14.dp).size(48.dp).clip(CircleShape)
                    .clickable { fire { client.key("volup") } },
                contentAlignment = Alignment.Center
            ) {
                Text("🔊+", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold)
            }

            // Громкость - (Снизу)
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).size(48.dp).clip(CircleShape)
                    .clickable { fire { client.key("voldown") } },
                contentAlignment = Alignment.Center
            ) {
                Text("🔉-", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold)
            }
        }

        // Дополнительные клавиши
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ControlChip("⏹ Стоп") { fire { client.key("stop") } }
            ControlChip("🔇 Без звука") { fire { client.key("mute") } }
            ControlChip("⎵ Пробел") { fire { client.key("space") } }
        }
    }
}

// ----------------- TAB 3: TRACKPAD -----------------
@Composable
private fun TrackpadTab(client: Client, haptic: () -> Unit) {
    val scope = rememberCoroutineScope()
    var lastSend by remember { mutableStateOf(0L) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Сенсорная панель (Touchpad area)
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Surface).border(1.dp, Line, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            haptic()
                            scope.launch { client.mouseClick("left") }
                        },
                        onDoubleTap = {
                            haptic()
                            scope.launch { client.mouseClick("double") }
                        },
                        onLongPress = {
                            haptic()
                            scope.launch { client.mouseClick("right") }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val now = System.currentTimeMillis()
                        if (now - lastSend > 16) { // ~60fps ограничение отправки по сети
                            lastSend = now
                            val dx = (dragAmount.x * 1.5f).toInt()
                            val dy = (dragAmount.y * 1.5f).toInt()
                            scope.launch { client.mouseMove(dx, dy) }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Сенсорная панель", color = Dim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Перемещение курсора • Тап = ЛКМ • Зажатие = ПКМ", color = Dim.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }

        // Физические кнопки мыши ЛКМ / Скролл / ПКМ
        Row(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1.5f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(SurfaceLight)
                    .clickable {
                        haptic()
                        scope.launch { client.mouseClick("left") }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("ЛКМ", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SurfaceLight)
                        .clickable { scope.launch { client.mouseScroll(120) } },
                    contentAlignment = Alignment.Center
                ) { Text("▲", color = Dim, fontSize = 12.sp) }

                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SurfaceLight)
                        .clickable { scope.launch { client.mouseScroll(-120) } },
                    contentAlignment = Alignment.Center
                ) { Text("▼", color = Dim, fontSize = 12.sp) }
            }

            Box(
                Modifier.weight(1.5f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(SurfaceLight)
                    .clickable {
                        haptic()
                        scope.launch { client.mouseClick("right") }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("ПКМ", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ----------------- TAB 4: SYSTEM -----------------
@Composable
private fun SystemTab(client: Client, fire: (suspend () -> Unit) -> Unit) {
    var confirmDialog by remember { mutableStateOf<String?>(null) }

    if (confirmDialog != null) {
        AlertDialog(
            onDismissRequest = { confirmDialog = null },
            title = { Text(if (confirmDialog == "shutdown") "Выключить ПК?" else "Перезагрузить ПК?") },
            text = { Text("Компьютер завершит работу через 30 секунд. Вы сможете отменить действие кнопкой в приложении.") },
            confirmButton = {
                Text(
                    "Подтвердить",
                    color = Bad,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val action = confirmDialog!!
                        confirmDialog = null
                        fire { client.system(action) }
                    }.padding(8.dp)
                )
            },
            dismissButton = {
                Text("Отмена", color = Dim, modifier = Modifier.clickable { confirmDialog = null }.padding(8.dp))
            }
        )
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Быстрые действия Windows", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SystemCard("🔒 Блокировка", "Win + L", Modifier.weight(1f)) { fire { client.system("lock") } }
            SystemCard("🖥 Рабочий стол", "Win + D", Modifier.weight(1f)) { fire { client.system("desktop") } }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SystemCard("⎇ Переключить окно", "Alt + Tab", Modifier.weight(1f)) { fire { client.system("alttab") } }
            SystemCard("🌙 Выключить экраны", "Энергосбережение", Modifier.weight(1f)) { fire { client.system("monitors_off") } }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SystemCard("💤 Спящий режим", "Sleep", Modifier.weight(1f)) { fire { client.system("sleep") } }
            SystemCard("↩ Отмена выключения", "shutdown /a", Modifier.weight(1f)) { fire { client.system("cancel_shutdown") } }
        }

        Spacer(Modifier.height(8.dp))
        Text("Питание компьютера", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceLight).clickable { confirmDialog = "restart" }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Перезагрузка ПК", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Bad.copy(alpha = 0.2f)).border(1.dp, Bad.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { confirmDialog = "shutdown" }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Выключить ПК", color = Bad, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------- TAB 5: TASKS -----------------
@Composable
private fun TasksTab(
    client: Client,
    fire: (suspend () -> Unit) -> Unit,
    tasks: List<TaskItem>?,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Открытые окна на ПК", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Обновить ↻", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onRefresh() })
        }

        if (tasks == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загружаю список окон...", color = Dim, fontSize = 14.sp)
            }
        } else if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет открытых окон", color = Dim, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tasks, key = { it.id }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f).clickable { fire { client.switchTask(t.id) } }) {
                            Text(t.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.proc, color = Dim, fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceLight).clickable { fire { client.switchTask(t.id) } }.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("Открыть", color = Accent, fontSize = 12.sp) }

                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(Bad.copy(alpha = 0.15f)).clickable {
                                    fire { client.closeTask(t.id); onRefresh() }
                                }.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("✕", color = Bad, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- UI Helpers -----------------
@Composable
private fun SystemCard(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(8.dp)).background(Surface).clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Dim, fontSize = 11.sp)
    }
}

@Composable
private fun ControlChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Surface).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun SkeletonGrid() {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(700); on = !on } }
    val c = if (on) Color(0xFF1E232C) else Surface
    LazyVerticalGrid(GridCells.Adaptive(84.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items((0 until 20).toList()) { Box(Modifier.fillMaxWidth().aspectRatio(1f).background(c, RoundedCornerShape(8.dp))) }
    }
}

@Composable
private fun Field(label: String, value: String, type: KeyboardType, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Dim, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 16.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(8.dp))
                .border(1.dp, Line, RoundedCornerShape(8.dp)).padding(14.dp)
        )
    }
}

@Composable
private fun Btn(text: String, primary: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (primary) Accent else Surface
    val fg = if (primary) Color(0xFF1A1405) else Ink
    Box(
        modifier.background(bg, RoundedCornerShape(8.dp)).then(if (primary) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(8.dp)))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) fg else fg.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun DrawScope.drawRemoteIcon() {
    val w = size.width; val h = size.height
    val rx = w * 0.16f
    val body = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(
            left = w * 0.28f, top = h * 0.20f, right = w * 0.72f, bottom = h * 0.90f,
            radiusX = rx, radiusY = rx
        ))
    }
    drawPath(body, Color(0xFF090B0E), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f))
    drawCircle(Color(0xFF090B0E), radius = 2.5f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.52f))
}

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF000000)
private val Surface = Color(0xFF0C0E14)
private val SurfaceLight = Color(0xFF141722)
private val Line = Color(0xFF1F2432)
private val Ink = Color(0xFFFFFFFF)
private val Dim = Color(0xFF8B949E)
private val Accent = Color(0xFF38BDF8)
private val Bad = Color(0xFFEF4444)
private val Good = Color(0xFF22C55E)

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
            LandscapeDock(
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

// ==========================================
// 1. CONNECT SCREEN (LANDSCAPE-OPTIMIZED)
// ==========================================
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
                status = "ПК не отвечает на $addr:$HTTP_PORT.\nУбедитесь, что Pult запущен на ПК."
            } else {
                try {
                    Client(addr, pin.trim()).apps()
                    onConnected(addr, pin.trim(), host.name)
                } catch (e: WrongPin) {
                    statusBad = true; status = "Неверный PIN. Проверьте окно Pult на ПК."
                } catch (e: Exception) {
                    statusBad = true; status = "Ошибка: ${e.message}"
                }
            }
            busy = false
        }
    }

    fun search() {
        scope.launch {
            busy = true; statusBad = false; status = "Поиск ПК в локальной сети..."
            hosts.clear()
            val r = discover()
            hosts.addAll(r)
            status = if (r.isEmpty()) "ПК не найдены. Введите IP адрес вручную." else ""
            statusBad = r.isEmpty()
            if (r.size == 1 && ip.isBlank()) ip = r[0].ip
            busy = false
        }
    }

    LaunchedEffect(Unit) { if (ip.isBlank()) search() }

    Row(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Левая колонка: Форма входа
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(38.dp).background(Color.White, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(20.dp)) { drawRemoteIcon() }
                }
                Column {
                    Text("Pult", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Деск-док для Windows", color = Dim, fontSize = 12.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("IP адрес компьютера", ip, KeyboardType.Uri) { ip = it }
                Field("PIN код (6 цифр)", pin, KeyboardType.NumberPassword) { pin = it.filter(Char::isDigit).take(6) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Подключить", primary = true, enabled = !busy && ip.isNotBlank() && pin.length == 6, modifier = Modifier.weight(1.2f)) { connect() }
                Btn("Поиск", primary = false, enabled = !busy, modifier = Modifier.weight(0.8f)) { search() }
            }
        }

        // Правая колонка: Найденные ПК и статус
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("НАЙДЕННЫЕ КОМПЬЮТЕРЫ", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

            if (hosts.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (status.isNotEmpty()) status else "Нажмите «Поиск», чтобы найти ПК в Wi-Fi",
                        color = if (statusBad) Bad else Dim,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(hosts) { h ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(8.dp))
                                .clickable { ip = h.ip; connect() }.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(h.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(h.ip, color = Dim, fontSize = 12.sp)
                            }
                            Box(Modifier.size(8.dp).background(Good, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. MAIN HORIZONTAL DOCK INTERFACE
// ==========================================
private enum class DeckSection(val title: String) {
    APPS("Приложения"),
    MEDIA("Плеер"),
    CURSOR("Курсор")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LandscapeDock(
    client: Client,
    hostName: String,
    prefs: android.content.SharedPreferences,
    onDisconnect: () -> Unit
) {
    var section by remember { mutableStateOf(DeckSection.APPS) }
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var query by remember { mutableStateOf("") }
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
            } catch (e: WrongPin) {
                onDisconnect()
            } catch (_: Exception) {}
        }
    }

    // Первоначальная загрузка
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
        } catch (_: Exception) {
            apps = emptyList()
        }
    }

    // Фоновый опрос громкости раз в 4 сек
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            try {
                val v = client.volume()
                volume = v.volume
                isMuted = v.muted
            } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        
        // ----------------- TOP STATUS & SECTION BAR -----------------
        Row(
            Modifier.fillMaxWidth().height(42.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Левый блок: Имя ПК и статус
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(7.dp).background(Good, CircleShape))
                Text(hostName, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Центральный блок: 3 Раздела (Приложения, Плеер, Курсор)
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(8.dp)).padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DeckSection.values().forEach { s ->
                    val active = s == section
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (active) SurfaceLight else Color.Transparent)
                            .then(if (active) Modifier.border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp)) else Modifier)
                            .clickable { haptic(); section = s }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s.title,
                            color = if (active) Ink else Dim,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Правый блок: Быстрый регулятор громкости и выход
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Stepper -
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(6.dp))
                        .clickable {
                            fire {
                                val next = (volume - 5).coerceAtLeast(0)
                                val r = client.setVolume(next)
                                volume = r.volume; isMuted = r.muted
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("−", color = Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

                // Volume % / Mute
                Box(
                    Modifier.height(30.dp).clip(RoundedCornerShape(6.dp)).background(if (isMuted) Bad.copy(alpha = 0.2f) else Surface)
                        .border(1.dp, if (isMuted) Bad.copy(alpha = 0.5f) else Line, RoundedCornerShape(6.dp))
                        .clickable {
                            fire {
                                val r = client.toggleMute()
                                volume = r.volume; isMuted = r.muted
                            }
                        }.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isMuted) "MUTE" else "$volume%", color = if (isMuted) Bad else Accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Stepper +
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(6.dp))
                        .clickable {
                            fire {
                                val next = (volume + 5).coerceAtMost(100)
                                val r = client.setVolume(next)
                                volume = r.volume; isMuted = r.muted
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("+", color = Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

                // Disconnect
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(6.dp))
                        .clickable { onDisconnect() },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Dim, fontSize = 12.sp) }
            }
        }

        // ----------------- SECTION CONTENT -----------------
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                DeckSection.APPS -> FullscreenAppsDeck(
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

                DeckSection.MEDIA -> FullscreenMediaDeck(
                    client = client,
                    volume = volume,
                    isMuted = isMuted,
                    onVolumeChange = { v, m -> volume = v; isMuted = m },
                    fire = ::fire
                )

                DeckSection.CURSOR -> FullscreenCursorDeck(
                    client = client,
                    haptic = ::haptic
                )
            }
        }
    }
}

// ==========================================
// РАЗДЕЛ 1: ПОЛНОЭКРАННАЯ СЕТКА ИКОНОК (APPS)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullscreenAppsDeck(
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
        // Строка поиска и количество приложений
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 13.sp),
                cursorBrush = SolidColor(Accent),
                decorationBox = { inner ->
                    Box(Modifier.padding(vertical = 1.dp)) {
                        if (query.isEmpty()) Text("🔍 Поиск по меню Пуск и играм...", color = Dim, fontSize = 13.sp)
                        inner()
                    }
                },
                modifier = Modifier.width(320.dp).clip(RoundedCornerShape(8.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 7.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (favs.isNotEmpty()) {
                    Text("★ ${favs.size} в избранном", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("${shown.size} приложений", color = Dim, fontSize = 11.sp)
            }
        }

        // Полноэкранная горизонтальная сетка иконок (6–8 колонок)
        if (apps == null) {
            SkeletonGridLandscape()
        } else if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Приложения не найдены", color = Dim, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                GridCells.Adaptive(92.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shown, key = { it.id }) { app ->
                    val isFav = app.id in favs
                    LaunchedEffect(app.id) {
                        if (icons[app.id] == null) onLoadIcon(app.id)
                    }

                    Column(
                        Modifier.height(82.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (isFav) Color(0xFF131722) else Surface)
                            .border(1.dp, if (isFav) Accent.copy(alpha = 0.6f) else Line, RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = { onLaunch(app.id) },
                                onLongClick = { onToggleFav(app.id) }
                            ).padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val bmp = icons[app.id]
                        if (bmp != null) {
                            Image(bmp, null, Modifier.size(38.dp))
                        } else {
                            Box(Modifier.size(38.dp).background(SurfaceLight, RoundedCornerShape(6.dp)))
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            app.name,
                            color = Ink,
                            fontSize = 11.sp,
                            fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// РАЗДЕЛ 2: ПЛЕЕР МЕДИА И ГРОМКОСТЬ (MEDIA)
// ==========================================
@Composable
private fun FullscreenMediaDeck(
    client: Client,
    volume: Int,
    isMuted: Boolean,
    onVolumeChange: (Int, Boolean) -> Unit,
    fire: (suspend () -> Unit) -> Unit
) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // БЛОК 1: Аппаратная громкость (Core Audio)
        Column(
            Modifier.weight(1.1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("МАСТЕР-ГРОМКОСТЬ", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (isMuted) Bad.copy(alpha = 0.2f) else SurfaceLight)
                        .border(1.dp, if (isMuted) Bad else Line, RoundedCornerShape(6.dp))
                        .clickable {
                            fire {
                                val r = client.toggleMute()
                                onVolumeChange(r.volume, r.muted)
                            }
                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (isMuted) "ВЫКЛЮЧЕН" else "MUTE", color = if (isMuted) Bad else Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                if (isMuted) "MUTED" else "$volume%",
                color = if (isMuted) Bad else Ink,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )

            // Слайдер громкости с кнопками точной подстройки
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceLight).border(1.dp, Line, RoundedCornerShape(8.dp))
                        .clickable {
                            fire {
                                val next = (volume - 5).coerceAtLeast(0)
                                val r = client.setVolume(next)
                                onVolumeChange(r.volume, r.muted)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("−5", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt(), isMuted) },
                    onValueChangeFinished = {
                        fire {
                            val r = client.setVolume(volume)
                            onVolumeChange(r.volume, r.muted)
                        }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Line)
                )

                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceLight).border(1.dp, Line, RoundedCornerShape(8.dp))
                        .clickable {
                            fire {
                                val next = (volume + 5).coerceAtMost(100)
                                val r = client.setVolume(next)
                                onVolumeChange(r.volume, r.muted)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("+5", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }

            Text("Прямая COM-синхронизация с Windows Core Audio", color = Dim, fontSize = 10.sp)
        }

        // БЛОК 2: Плеер воспроизведения (Media Player)
        Column(
            Modifier.weight(1.3f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("УПРАВЛЕНИЕ ВОСПРОИЗВЕДЕНИЕМ", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

            // Основной ряд: Prev - Play/Pause - Next
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Prev
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(SurfaceLight).border(1.dp, Line, CircleShape)
                        .clickable { fire { client.key("prev") } },
                    contentAlignment = Alignment.Center
                ) { Text("◀◀", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

                // Play / Pause
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(Ink)
                        .clickable { fire { client.key("playpause") } },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(26.dp)) {
                        val w = size.width; val h = size.height
                        val p = Path().apply { moveTo(w * 0.15f, 0f); lineTo(w * 0.65f, h / 2f); lineTo(w * 0.15f, h); close() }
                        drawPath(p, Color(0xFF000000))
                        drawRoundRect(
                            Color(0xFF000000),
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.08f),
                            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.84f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                    }
                }

                // Next
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(SurfaceLight).border(1.dp, Line, CircleShape)
                        .clickable { fire { client.key("next") } },
                    contentAlignment = Alignment.Center
                ) { Text("▶▶", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }

            // Вспомогательные клавиши
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeckActionButton("STOP", Modifier.weight(1f)) { fire { client.key("stop") } }
                DeckActionButton("ПРОБЕЛ (ПАУЗА)", Modifier.weight(1.4f)) { fire { client.key("space") } }
            }

            Text("Spotify · YouTube · Браузер · Медиаплееры", color = Dim, fontSize = 10.sp)
        }

        // БЛОК 3: Быстрые действия Windows
        Column(
            Modifier.weight(0.9f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("СИСТЕМА", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DeckSystemRow("Блокировка", "Win + L") { fire { client.system("lock") } }
                DeckSystemRow("Рабочий стол", "Win + D") { fire { client.system("desktop") } }
                DeckSystemRow("Окно", "Alt + Tab") { fire { client.system("alttab") } }
                DeckSystemRow("Мониторы", "Выкл") { fire { client.system("monitors_off") } }
            }

            Text("Управление ПК в 1 клик", color = Dim, fontSize = 10.sp)
        }
    }
}

// ==========================================
// РАЗДЕЛ 3: УПРАВЛЕНИЕ КУРСОРОМ (TRACKPAD)
// ==========================================
@Composable
private fun FullscreenCursorDeck(client: Client, haptic: () -> Unit) {
    val scope = rememberCoroutineScope()
    var lastSend by remember { mutableStateOf(0L) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Большое сенсорное поле курсора
        Box(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
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
                        if (now - lastSend > 16) { // 60 FPS
                            lastSend = now
                            val dx = (dragAmount.x * 1.6f).toInt()
                            val dy = (dragAmount.y * 1.6f).toInt()
                            scope.launch { client.mouseMove(dx, dy) }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("СЕНСОРНАЯ ПАНЕЛЬ МЫШИ", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text("Двигайте пальцем для перемещения курсора по экрану", color = Dim, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text("Тап = ЛКМ  •  Двойной тап = Клик  •  Удержание = ПКМ", color = Dim.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }

        // Правая колонка: Вертикальный скролл и аппаратные кнопки мыши
        Column(
            Modifier.width(130.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Кнопка Скролл вверх
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(Surface).border(1.dp, Line, RoundedCornerShape(10.dp))
                    .clickable { haptic(); scope.launch { client.mouseScroll(120) } },
                contentAlignment = Alignment.Center
            ) { Text("▲ СКРОЛЛ", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

            // Кнопка Скролл вниз
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(Surface).border(1.dp, Line, RoundedCornerShape(10.dp))
                    .clickable { haptic(); scope.launch { client.mouseScroll(-120) } },
                contentAlignment = Alignment.Center
            ) { Text("▼ СКРОЛЛ", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

            // Кнопка ЛКМ
            Box(
                Modifier.fillMaxWidth().weight(1.3f).clip(RoundedCornerShape(10.dp))
                    .background(SurfaceLight).border(1.dp, Line, RoundedCornerShape(10.dp))
                    .clickable { haptic(); scope.launch { client.mouseClick("left") } },
                contentAlignment = Alignment.Center
            ) { Text("LKM", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }

            // Кнопка ПКМ
            Box(
                Modifier.fillMaxWidth().weight(1.3f).clip(RoundedCornerShape(10.dp))
                    .background(SurfaceLight).border(1.dp, Line, RoundedCornerShape(10.dp))
                    .clickable { haptic(); scope.launch { client.mouseClick("right") } },
                contentAlignment = Alignment.Center
            ) { Text("PKM", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ==========================================
@Composable
private fun DeckActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceLight).border(1.dp, Line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun DeckSystemRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceLight).border(1.dp, Line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SkeletonGridLandscape() {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(700); on = !on } }
    val c = if (on) Color(0xFF161922) else Surface
    LazyVerticalGrid(
        GridCells.Adaptive(92.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items((0 until 18).toList()) { Box(Modifier.height(82.dp).background(c, RoundedCornerShape(10.dp))) }
    }
}

@Composable
private fun Field(label: String, value: String, type: KeyboardType, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 14.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(8.dp))
                .border(1.dp, Line, RoundedCornerShape(8.dp)).padding(10.dp)
        )
    }
}

@Composable
private fun Btn(text: String, primary: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (primary) Accent else Surface
    val fg = if (primary) Color(0xFF030712) else Ink
    Box(
        modifier.clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (primary) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(8.dp)))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) fg else fg.copy(alpha = 0.4f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    drawPath(body, Color(0xFF000000), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f))
    drawCircle(Color(0xFF000000), radius = 2.5f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.52f))
}

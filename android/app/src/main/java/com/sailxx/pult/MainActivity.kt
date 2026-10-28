package com.sailxx.pult

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF14171C)
private val Surface = Color(0xFF1D2128)
private val Line = Color(0xFF2B313A)
private val Ink = Color(0xFFE9E6DF)
private val Dim = Color(0xFF8B929C)
private val Accent = Color(0xFFE8B04A)
private val Bad = Color(0xFFE5675A)

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
    var connected by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Bg).safeDrawingPadding()) {
        if (connected) {
            Dock(Client(ip, pin), prefs, onDisconnect = { connected = false })
        } else {
            ConnectScreen(ip, pin, onConnected = { newIp, newPin ->
                ip = newIp; pin = newPin
                prefs.edit().putString("ip", newIp).putString("pin", newPin).apply()
                connected = true
            })
        }
    }
}

@Composable
private fun ConnectScreen(initIp: String, initPin: String, onConnected: (String, String) -> Unit) {
    var ip by remember { mutableStateOf(initIp) }
    var pin by remember { mutableStateOf(initPin) }
    var status by remember { mutableStateOf("") }
    var statusBad by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val hosts = remember { mutableStateListOf<Host>() }
    val scope = rememberCoroutineScope()

    fun connect() {
        scope.launch {
            busy = true; statusBad = false; status = "Подключаюсь к $ip"
            val host = ping(ip.trim(), 3000)
            if (host == null) {
                statusBad = true
                status = "ПК не отвечает на $ip:$HTTP_PORT. Проверьте, что Pult запущен и телефон в той же Wi-Fi сети. " +
                    "Если да, на ПК: значок Pult в трее > «Разрешить в брандмауэре»."
            } else {
                try {
                    Client(ip.trim(), pin.trim()).apps()
                    onConnected(ip.trim(), pin.trim())
                } catch (e: WrongPin) {
                    statusBad = true; status = "Неверный PIN. Он показан в окне «Адрес и PIN» в трее ПК."
                } catch (e: Exception) {
                    statusBad = true; status = "Ошибка: ${e.message}"
                }
            }
            busy = false
        }
    }

    fun search() {
        scope.launch {
            busy = true; statusBad = false; status = "Ищу ПК в сети"
            hosts.clear()
            val r = discover()
            hosts.addAll(r)
            status = if (r.isEmpty()) "Ничего не найдено. Введите адрес вручную (он есть в окне «Адрес и PIN» на ПК)." else ""
            statusBad = r.isEmpty()
            if (r.size == 1 && ip.isBlank()) ip = r[0].ip
            busy = false
        }
    }

    LaunchedEffect(Unit) { if (ip.isBlank()) search() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Pult", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("Пульт для вашего ПК по Wi-Fi", color = Dim, fontSize = 14.sp)

        Field("Адрес ПК", ip, KeyboardType.Uri) { ip = it }
        Field("PIN", pin, KeyboardType.NumberPassword) { pin = it.filter(Char::isDigit).take(6) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn("Подключить", primary = true, enabled = !busy && ip.isNotBlank() && pin.length == 6, modifier = Modifier.weight(1f)) { connect() }
            Btn("Найти ПК", primary = false, enabled = !busy, modifier = Modifier.weight(1f)) { search() }
        }

        if (status.isNotEmpty()) Text(status, color = if (statusBad) Bad else Dim, fontSize = 13.sp, lineHeight = 18.sp)

        hosts.forEach { h ->
            Row(
                Modifier.fillMaxWidth().clip8().background(Surface).clickable { ip = h.ip }.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(h.name, color = Ink, fontSize = 15.sp)
                Text(h.ip, color = Dim, fontSize = 14.sp)
            }
        }
    }
}

private fun Modifier.clip8() = this.clip(RoundedCornerShape(6.dp))

@Composable
private fun Field(label: String, value: String, type: KeyboardType, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Dim, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 17.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(6.dp))
                .border(1.dp, Line, RoundedCornerShape(6.dp)).padding(14.dp)
        )
    }
}

@Composable
private fun Btn(text: String, primary: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (primary) Accent else Surface
    val fg = if (primary) Color(0xFF1A1405) else Ink
    Box(
        modifier.background(bg, RoundedCornerShape(6.dp)).then(if (primary) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(6.dp)))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) fg else fg.copy(alpha = 0.4f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Dock(client: Client, prefs: android.content.SharedPreferences, onDisconnect: () -> Unit) {
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var favs by remember { mutableStateOf(prefs.getStringSet("favs", emptySet()) ?: emptySet()) }
    val icons = remember { mutableStateOf(mapOf<String, ImageBitmap>()) }
    val scope = rememberCoroutineScope()

    fun fire(block: suspend () -> Unit) {
        scope.launch {
            try { block(); error = "" } catch (e: WrongPin) { onDisconnect() } catch (e: Exception) { error = "Нет связи с ПК" }
        }
    }

    LaunchedEffect(Unit) {
        try { apps = client.apps() } catch (e: WrongPin) { onDisconnect() } catch (e: Exception) { error = "Нет связи с ПК"; apps = emptyList() }
    }

    val shown = (apps ?: emptyList())
        .filter { it.name.contains(query, ignoreCase = true) }
        .sortedWith(compareByDescending<AppItem> { it.id in favs }.thenBy { it.name.lowercase() })

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Transport(Modifier.weight(1f), ::fire, client)
            Text("Выйти", color = Dim, fontSize = 13.sp, modifier = Modifier.clickable { onDisconnect() }.padding(8.dp))
        }
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 16.sp),
            cursorBrush = SolidColor(Accent),
            decorationBox = { inner ->
                Box { if (query.isEmpty()) Text("Поиск приложения", color = Dim, fontSize = 16.sp); inner() }
            },
            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(6.dp)).padding(14.dp)
        )
        if (error.isNotEmpty()) Text(error, color = Bad, fontSize = 13.sp)

        if (apps == null) {
            SkeletonGrid()
        } else {
            LazyVerticalGrid(GridCells.Adaptive(88.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(shown, key = { it.id }) { app ->
                    LaunchedEffect(app.id) {
                        if (icons.value[app.id] == null) {
                            val bytes = client.icon(app.id)
                            if (bytes != null && bytes.isNotEmpty()) {
                                val bmp = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
                                if (bmp != null) icons.value = icons.value + (app.id to bmp)
                            }
                        }
                    }
                    Column(
                        Modifier.clip8().background(if (app.id in favs) Color(0xFF262B33) else Surface)
                            .combinedClickable(
                                onClick = { fire { client.launch(app.id) } },
                                onLongClick = {
                                    favs = if (app.id in favs) favs - app.id else favs + app.id
                                    prefs.edit().putStringSet("favs", favs).apply()
                                }
                            ).padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val bmp = icons.value[app.id]
                        if (bmp != null) Image(bmp, null, Modifier.size(44.dp))
                        else Box(Modifier.size(44.dp).background(Line, RoundedCornerShape(6.dp)))
                        Text(app.name, color = Ink, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonGrid() {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(700); on = !on } }
    val c = if (on) Color(0xFF242932) else Surface
    LazyVerticalGrid(GridCells.Adaptive(88.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items((0 until 24).toList()) { Box(Modifier.fillMaxWidth().aspectRatio(1f).background(c, RoundedCornerShape(6.dp))) }
    }
}

@Composable
private fun Transport(modifier: Modifier, fire: (suspend () -> Unit) -> Unit, client: Client) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("prev", "playpause", "next", "voldown", "volup", "mute").forEach { k ->
            Box(
                Modifier.size(42.dp).background(Surface, RoundedCornerShape(6.dp)).clickable { fire { client.key(k) } },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(18.dp)) { drawKey(k) }
            }
        }
    }
}

private fun DrawScope.drawKey(k: String) {
    val w = size.width; val h = size.height
    fun tri(x0: Float, x1: Float) = Path().apply {
        moveTo(x0, 0f); lineTo(x1, h / 2); lineTo(x0, h); close()
    }
    when (k) {
        "playpause" -> drawPath(tri(w * 0.15f, w * 0.95f), Ink)
        "next" -> { drawPath(tri(0f, w * 0.7f), Ink); drawRect(Ink, androidx.compose.ui.geometry.Offset(w * 0.8f, 0f), androidx.compose.ui.geometry.Size(w * 0.18f, h)) }
        "prev" -> {
            drawRect(Ink, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Size(w * 0.18f, h))
            drawPath(Path().apply { moveTo(w, 0f); lineTo(w * 0.3f, h / 2); lineTo(w, h); close() }, Ink)
        }
        "volup" -> {
            drawRect(Ink, androidx.compose.ui.geometry.Offset(0f, h * 0.4f), androidx.compose.ui.geometry.Size(w, h * 0.2f))
            drawRect(Ink, androidx.compose.ui.geometry.Offset(w * 0.4f, 0f), androidx.compose.ui.geometry.Size(w * 0.2f, h))
        }
        "voldown" -> drawRect(Ink, androidx.compose.ui.geometry.Offset(0f, h * 0.4f), androidx.compose.ui.geometry.Size(w, h * 0.2f))
        else -> drawRect(Dim, androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
    }
}

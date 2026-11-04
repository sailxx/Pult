package com.sailxx.pult

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sailxx.pult.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindToWifi()

        // Полноэкранный режим без рамок для горизонтальной док-станции
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val prefs = getSharedPreferences("pult", Context.MODE_PRIVATE)
        setContent {
            var themeName by remember { mutableStateOf(prefs.getString("theme", "dark") ?: "dark") }
            val currentPalette = remember(themeName) { PultPalettes.get(themeName) }

            PultTheme(palette = currentPalette) {
                Root(
                    prefs = prefs,
                    currentPalette = currentPalette,
                    onThemeChange = { newPalette ->
                        themeName = newPalette.name
                        prefs.edit().putString("theme", newPalette.name).apply()
                    }
                )
            }
        }
    }
}

/** Привязывает сетевой трафик приложения к Wi-Fi, чтобы он шёл мимо VPN телефона. */
private fun Context.bindToWifi() {
    try {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val wifi = cm.allNetworks.firstOrNull {
            val c = cm.getNetworkCapabilities(it)
            c != null && c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) &&
                !c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
        }
        if (wifi != null) cm.bindProcessToNetwork(wifi)
    } catch (_: Exception) {}
}

@Composable
private fun Root(
    prefs: android.content.SharedPreferences,
    currentPalette: PultThemePalette,
    onThemeChange: (PultThemePalette) -> Unit
) {
    var ip by remember { mutableStateOf(prefs.getString("ip", "") ?: "") }
    var pin by remember { mutableStateOf(prefs.getString("pin", "") ?: "") }
    var hostName by remember { mutableStateOf(prefs.getString("host_name", "Windows PC") ?: "Windows PC") }
    var connected by remember { mutableStateOf(false) }

    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
    }

    Box(Modifier.fillMaxSize()) {
        // Фоновые мягкие радиальные градиенты в стиле Windock
        PultWallpaper(palette = currentPalette)

        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            if (connected) {
                val client = remember(ip, pin) { Client(ip, pin) }
                LandscapeDock(
                    client = client,
                    hostName = hostName,
                    prefs = prefs,
                    currentPalette = currentPalette,
                    onThemeChange = onThemeChange,
                    onDisconnect = { connected = false },
                    onHaptic = ::haptic
                )
            } else {
                ConnectScreen(
                    initIp = ip,
                    initPin = pin,
                    onConnected = { newIp, newPin, name ->
                        ip = newIp
                        pin = newPin
                        hostName = name
                        prefs.edit()
                            .putString("ip", newIp)
                            .putString("pin", newPin)
                            .putString("host_name", name)
                            .apply()
                        connected = true
                    },
                    onHaptic = ::haptic
                )
            }
        }
    }
}

@Composable
private fun LandscapeDock(
    client: Client,
    hostName: String,
    prefs: android.content.SharedPreferences,
    currentPalette: PultThemePalette,
    onThemeChange: (PultThemePalette) -> Unit,
    onDisconnect: () -> Unit,
    onHaptic: () -> Unit
) {
    var section by remember { mutableStateOf(DeckSection.DOCK) }
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var tasks by remember { mutableStateOf<List<TaskItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var favs by remember { mutableStateOf(prefs.getStringSet("favs", emptySet()) ?: emptySet()) }
    var volume by remember { mutableStateOf(50) }
    var isMuted by remember { mutableStateOf(false) }

    var dockSlotsCount by remember { mutableIntStateOf(prefs.getInt("dock_slots", 6)) }
    var showLabels by remember { mutableStateOf(prefs.getBoolean("show_labels", true)) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val mouseController = remember(client) { MouseController(client, scope) }
    val disconnectAndRelease = {
        mouseController.releaseAll()
        onDisconnect()
    }

    fun fire(block: suspend () -> Unit) {
        onHaptic()
        scope.launch {
            try {
                block()
            } catch (e: WrongPin) {
                disconnectAndRelease()
            } catch (_: Exception) {}
        }
    }

    // Первоначальная загрузка громкости, списка приложений и активных окон
    LaunchedEffect(client) {
        try {
            val v = client.volume()
            volume = v.volume
            isMuted = v.muted
        } catch (_: Exception) {}

        try {
            apps = client.apps()
        } catch (e: WrongPin) {
            disconnectAndRelease()
        } catch (_: Exception) {
            apps = emptyList()
        }

        try {
            tasks = client.tasks()
        } catch (_: Exception) {}
    }

    // Фоновое обновление статуса громкости и задач раз в 3 секунды
    LaunchedEffect(client) {
        while (true) {
            delay(3000)
            try {
                val v = client.volume()
                volume = v.volume
                isMuted = v.muted
            } catch (_: Exception) {}

            try {
                tasks = client.tasks()
            } catch (_: Exception) {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Компактный верхний бар
            PultHeader(
                hostName = hostName,
                selectedSection = section,
                onSectionSelect = { section = it },
                onOpenSettings = { isSettingsOpen = true },
                onDisconnect = disconnectAndRelease,
                onHaptic = onHaptic
            )

            // Контент секции с плавной анимацией перехода
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = section,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "dockSectionTransition"
                ) { currentSection ->
                    when (currentSection) {
                        DeckSection.DOCK -> DockScreen(
                            client = client,
                            apps = apps,
                            tasks = tasks,
                            favs = favs,
                            onToggleFav = { id ->
                                favs = if (id in favs) favs - id else favs + id
                                prefs.edit().putStringSet("favs", favs).apply()
                            },
                            onLaunch = { fire { client.launch(it) } },
                            onSwitchTask = { taskId -> fire { client.switchTask(taskId) } },
                            volume = volume,
                            isMuted = isMuted,
                            onVolumeChange = { newVol ->
                                fire {
                                    val r = client.setVolume(newVol)
                                    volume = r.volume
                                    isMuted = r.muted
                                }
                            },
                            onToggleMute = {
                                fire {
                                    val r = client.toggleMute()
                                    volume = r.volume
                                    isMuted = r.muted
                                }
                            },
                            onKey = { keyName -> fire { client.key(keyName) } },
                            onSystem = { action -> fire { client.system(action) } },
                            dockSlotsCount = dockSlotsCount,
                            showLabels = showLabels,
                            onOpenAllApps = { section = DeckSection.APPS },
                            onHaptic = onHaptic
                        )

                        DeckSection.TOUCHPAD -> TouchpadScreen(
                            mouseController = mouseController,
                            onHaptic = onHaptic
                        )

                        DeckSection.CLIPBOARD -> ClipboardScreen(
                            client = client,
                            onHaptic = onHaptic
                        )

                        DeckSection.SIGNATURE -> SignatureScreen(
                            client = client,
                            onHaptic = onHaptic
                        )

                        DeckSection.CAPTURE -> CaptureScreen(
                            client = client,
                            onHaptic = onHaptic
                        )

                        DeckSection.APPS -> AppsDeck(
                            client = client,
                            apps = apps,
                            tasks = tasks,
                            query = query,
                            onQueryChange = { query = it },
                            favs = favs,
                            onToggleFav = { id ->
                                favs = if (id in favs) favs - id else favs + id
                                prefs.edit().putStringSet("favs", favs).apply()
                            },
                            onLaunch = { fire { client.launch(it) } },
                            onSwitchTask = { taskId -> fire { client.switchTask(taskId) } },
                            onHaptic = onHaptic
                        )
                    }
                }
            }
        }

        // Модальное окно настроек внешнего вида
        if (isSettingsOpen) {
            SettingsSheet(
                prefs = prefs,
                currentPalette = currentPalette,
                onPaletteSelect = onThemeChange,
                dockSlotsCount = dockSlotsCount,
                onDockSlotsSelect = { dockSlotsCount = it },
                showLabels = showLabels,
                onShowLabelsToggle = { showLabels = it },
                onClose = { isSettingsOpen = false },
                onHaptic = onHaptic
            )
        }
    }
}

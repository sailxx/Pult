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
import androidx.compose.foundation.background
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
            PultTheme {
                Root(prefs)
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
private fun Root(prefs: android.content.SharedPreferences) {
    var ip by remember { mutableStateOf(prefs.getString("ip", "") ?: "") }
    var pin by remember { mutableStateOf(prefs.getString("pin", "") ?: "") }
    var hostName by remember { mutableStateOf(prefs.getString("host_name", "Windows PC") ?: "Windows PC") }
    var connected by remember { mutableStateOf(false) }

    val view = LocalView.current
    fun haptic() {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(PultColors.background)
            .safeDrawingPadding()
    ) {
        if (connected) {
            val client = remember(ip, pin) { Client(ip, pin) }
            LandscapeDock(
                client = client,
                hostName = hostName,
                prefs = prefs,
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

@Composable
private fun LandscapeDock(
    client: Client,
    hostName: String,
    prefs: android.content.SharedPreferences,
    onDisconnect: () -> Unit,
    onHaptic: () -> Unit
) {
    var section by remember { mutableStateOf(DeckSection.APPS) }
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var tasks by remember { mutableStateOf<List<TaskItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var favs by remember { mutableStateOf(prefs.getStringSet("favs", emptySet()) ?: emptySet()) }
    var volume by remember { mutableStateOf(50) }
    var isMuted by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Компактный 42dp Header
        PultHeader(
            hostName = hostName,
            selectedSection = section,
            onSectionSelect = { section = it },
            volume = volume,
            isMuted = isMuted,
            onVolumeStep = { step ->
                fire {
                    val next = (volume + step).coerceIn(0, 100)
                    val r = client.setVolume(next)
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

                    DeckSection.MEDIA -> MediaDeck(
                        client = client,
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
                        onHaptic = onHaptic
                    )

                    DeckSection.CURSOR -> CursorDeck(
                        mouseController = mouseController,
                        onHaptic = onHaptic
                    )
                }
            }
        }
    }
}

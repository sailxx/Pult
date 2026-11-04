package com.sailxx.pult.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sailxx.pult.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockScreen(
    client: Client,
    apps: List<AppItem>?,
    tasks: List<TaskItem>,
    favs: Set<String>,
    onToggleFav: (String) -> Unit,
    onLaunch: (String) -> Unit,
    onSwitchTask: (Long) -> Unit,
    volume: Int,
    isMuted: Boolean,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onKey: (String) -> Unit,
    onSystem: (String) -> Unit,
    dockSlotsCount: Int,
    showLabels: Boolean,
    onOpenAllApps: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalPultPalette.current
    val icons = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Сопоставление активных процессов
    val runningAppNames = remember(tasks) {
        tasks.map { it.proc.lowercase() }.toSet()
    }

    // Выбираем иконки для Дока: сначала избранные, затем часто используемые/запущенные
    val dockApps = remember(apps, favs, tasks, dockSlotsCount) {
        val all = apps ?: emptyList()
        val favApps = all.filter { it.id in favs }
        val remaining = all.filter { it.id !in favs }
        (favApps + remaining).take(dockSlotsCount)
    }

    // Состояние контекстного меню выбранной иконки
    var selectedItemForMenu by remember { mutableStateOf<AppItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ВЕРХНИЙ БЛОК: Заголовок и кнопка вызова библиотеки программ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ГЛАВНЫЙ ДОК",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary,
                    letterSpacing = 0.5.sp
                )
                if (favs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(PultDimens.shapeBadge)
                            .background(palette.accent.copy(alpha = 0.12f))
                            .border(1.dp, palette.accent.copy(alpha = 0.3f), PultDimens.shapeBadge)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${favs.size} закреплено",
                            color = palette.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Кнопка открытия полной библиотеки
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(PultDimens.shapeBadge)
                    .background(palette.surfaceLight)
                    .border(1.dp, palette.border, PultDimens.shapeBadge)
                    .clickable {
                        onHaptic()
                        onOpenAllApps()
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Все программы (${apps?.size ?: 0})",
                    fontSize = 11.sp,
                    color = palette.accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ЦЕНТРАЛЬНАЯ ОБЛАСТЬ: Док иконок (4, 6 или 8)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (apps == null) {
                // Плейсхолдер загрузки
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(dockSlotsCount) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(PultDimens.shapeCard)
                                .background(palette.surface.copy(alpha = 0.6f))
                                .border(1.dp, palette.border, PultDimens.shapeCard)
                        )
                    }
                }
            } else {
                val columnsCount = when (dockSlotsCount) {
                    4 -> 4
                    6 -> 6
                    8 -> 4
                    else -> 6
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.wrapContentSize(),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(dockApps, key = { _, app -> app.id }) { _, app ->
                        val isFav = app.id in favs
                        val isRunning = runningAppNames.any { app.name.lowercase().contains(it) }

                        // Асинхронная загрузка иконки из кэша
                        LaunchedEffect(app.id) {
                            if (!icons.containsKey(app.id)) {
                                val cached = IconCache.getFromMemory(app.id)
                                if (cached != null) {
                                    icons[app.id] = cached
                                } else {
                                    val loaded = IconCache.getOrFetch(context, client, app.id)
                                    if (loaded != null) {
                                        icons[app.id] = loaded
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Сама стеклянная плитка дока
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(PultDimens.shapeCard)
                                    .background(if (isFav) palette.surfaceLight else palette.surface.copy(alpha = 0.85f))
                                    .border(
                                        width = if (isRunning || isFav) 1.5.dp else 1.dp,
                                        color = when {
                                            isRunning -> palette.live
                                            isFav -> palette.accent.copy(alpha = 0.6f)
                                            else -> palette.border
                                        },
                                        shape = PultDimens.shapeCard
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onHaptic()
                                            val matchTask = tasks.firstOrNull { it.proc.equals(app.name, ignoreCase = true) }
                                            if (matchTask != null) {
                                                onSwitchTask(matchTask.id)
                                            } else {
                                                onLaunch(app.id)
                                            }
                                        },
                                        onLongClick = {
                                            onHaptic()
                                            selectedItemForMenu = app
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val bmp = icons[app.id]
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = app.name,
                                        modifier = Modifier.size(46.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(PultDimens.shapeInner)
                                            .background(palette.surfaceLight)
                                    )
                                }

                                // Индикатор запущенного процесса (светящаяся точка снизу)
                                if (isRunning) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(palette.live)
                                    )
                                }
                            }

                            // Подпись приложения под иконкой
                            if (showLabels) {
                                Text(
                                    text = app.name,
                                    color = palette.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(76.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // НИЖНЯЯ ПАНЕЛЬ: Управление звуком и мультимедиа (Now Playing bar)
        PultCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            backgroundColor = palette.surface.copy(alpha = 0.90f),
            borderColor = palette.border
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая группа: Статус громкости и кнопка Mute
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(PultDimens.shapeBadge)
                            .background(if (isMuted) palette.danger.copy(alpha = 0.2f) else palette.surfaceLight)
                            .border(1.dp, if (isMuted) palette.danger else palette.border, PultDimens.shapeBadge)
                            .clickable {
                                onHaptic()
                                onToggleMute()
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMuted) "MUTE" else "$volume%",
                            color = if (isMuted) palette.danger else palette.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Кнопки быстрой подстройки звука
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(PultDimens.shapeBadge)
                                .background(palette.surfaceLight)
                                .border(1.dp, palette.border, PultDimens.shapeBadge)
                                .clickable {
                                    onHaptic()
                                    onVolumeChange((volume - 5).coerceIn(0, 100))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(PultDimens.shapeBadge)
                                .background(palette.surfaceLight)
                                .border(1.dp, palette.border, PultDimens.shapeBadge)
                                .clickable {
                                    onHaptic()
                                    onVolumeChange((volume + 5).coerceIn(0, 100))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Центральная группа: Медиа-кнопки (Prev, Play/Pause, Next)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Предыдущий трек
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, CircleShape)
                            .clickable {
                                onHaptic()
                                onKey("MEDIA_PREV")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PultIconPrev(Modifier.size(12.dp), color = palette.textPrimary)
                    }

                    // Воспроизведение / Пауза (Space)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.accent)
                            .clickable {
                                onHaptic()
                                onKey("SPACE")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PultIconPlay(Modifier.size(14.dp), color = Color(0xFF030712))
                    }

                    // Следующий трек
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, CircleShape)
                            .clickable {
                                onHaptic()
                                onKey("MEDIA_NEXT")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PultIconNext(Modifier.size(12.dp), color = palette.textPrimary)
                    }
                }

                // Правая группа: Системные действия (Win+D Свернуть все, Lock PC)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(PultDimens.shapeBadge)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, PultDimens.shapeBadge)
                            .clickable {
                                onHaptic()
                                onKey("WIN_D")
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Рабочий стол",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(PultDimens.shapeBadge)
                            .background(palette.surfaceLight)
                            .border(1.dp, palette.border, PultDimens.shapeBadge)
                            .clickable {
                                onHaptic()
                                onSystem("lock")
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Блок ПК",
                            color = palette.danger,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Всплывающий диалог действий над иконкой (при долгом нажатии)
    AnimatedVisibility(
        visible = selectedItemForMenu != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val app = selectedItemForMenu ?: return@AnimatedVisibility
        val isFav = app.id in favs

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { selectedItemForMenu = null },
            contentAlignment = Alignment.Center
        ) {
            PultCard(
                modifier = Modifier
                    .width(320.dp)
                    .clickable(enabled = false) {},
                backgroundColor = palette.surface,
                borderColor = palette.border
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = app.name,
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(2.dp))

                    // Закрепить / Открепить
                    PultButton(
                        text = if (isFav) "Убрать из избранного" else "Закрепить в избранном",
                        onClick = {
                            onHaptic()
                            onToggleFav(app.id)
                            selectedItemForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 36.dp
                    )

                    // Запустить
                    PultButton(
                        text = "Запустить на ПК",
                        onClick = {
                            onHaptic()
                            onLaunch(app.id)
                            selectedItemForMenu = null
                        },
                        primary = true,
                        modifier = Modifier.fillMaxWidth(),
                        height = 36.dp
                    )

                    // Закрыть меню
                    PultButton(
                        text = "Отмена",
                        onClick = {
                            onHaptic()
                            selectedItemForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 32.dp
                    )
                }
            }
        }
    }
}

package com.sailxx.pult.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

/**
 * Раздел 1: Сетка приложений Windows (APPS).
 * Высокая плотность размещения (7 колонок), оригинальные иконки EXE с дисковым кэшированием,
 * мгновенный поиск, избранное и переключение активных окон.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsDeck(
    client: Client,
    apps: List<AppItem>?,
    tasks: List<TaskItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    favs: Set<String>,
    onToggleFav: (String) -> Unit,
    onLaunch: (String) -> Unit,
    onSwitchTask: (Long) -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val icons = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Сопоставление активных процессов с приложениями
    val runningAppNames = remember(tasks) {
        tasks.map { it.proc.lowercase() }.toSet()
    }

    val filteredApps = remember(apps, query, favs) {
        (apps ?: emptyList())
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedWith(
                compareByDescending<AppItem> { it.id in favs }
                    .thenBy { it.name.lowercase() }
            )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Панель поиска и сводка
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PultTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "",
                placeholder = "Поиск программ и игр...",
                modifier = Modifier.width(340.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (favs.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(PultDimens.shapeBadge)
                            .background(PultColors.accent.copy(alpha = 0.1f))
                            .border(1.dp, PultColors.accent.copy(alpha = 0.3f), PultDimens.shapeBadge)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("★", color = PultColors.accent, fontSize = 11.sp)
                        Text(
                            text = "${favs.size} в избранном",
                            color = PultColors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${filteredApps.size} приложений",
                    color = PultColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Адаптивная сетка 7 колонок
        if (apps == null) {
            SkeletonGrid()
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isEmpty()) "Список приложений пуст" else "Ничего не найдено по запросу",
                    color = PultColors.textSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columnCount = if (maxWidth >= 680.dp) 7 else 5

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredApps, key = { it.id }) { app ->
                        val isFav = app.id in favs
                        val isRunning = runningAppNames.any { app.name.lowercase().contains(it) }

                        // Асинхронная подгрузка иконки через дисковый кэш
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
                            modifier = Modifier
                                .height(82.dp)
                                .clip(PultDimens.shapeCard)
                                .background(if (isFav) Color(0xFF111520) else PultColors.surface)
                                .border(
                                    1.dp,
                                    when {
                                        isFav -> PultColors.accent.copy(alpha = 0.5f)
                                        isRunning -> PultColors.success.copy(alpha = 0.4f)
                                        else -> PultColors.border
                                    },
                                    PultDimens.shapeCard
                                )
                                .combinedClickable(
                                    onClick = {
                                        onHaptic()
                                        // Если приложение запущено — переключаемся на его окно
                                        val matchTask = tasks.firstOrNull { it.proc.equals(app.name, ignoreCase = true) }
                                        if (matchTask != null) {
                                            onSwitchTask(matchTask.id)
                                        } else {
                                            onLaunch(app.id)
                                        }
                                    },
                                    onLongClick = {
                                        onHaptic()
                                        onToggleFav(app.id)
                                    }
                                )
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                val bmp = icons[app.id]
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = app.name,
                                        modifier = Modifier.size(38.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(PultDimens.shapeInner)
                                            .background(PultColors.surfaceLight)
                                    )
                                }

                                if (isRunning) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(PultColors.success, CircleShape)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = app.name,
                                color = PultColors.textPrimary,
                                fontSize = 10.sp,
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
}

@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(21) {
            Box(
                modifier = Modifier
                    .height(82.dp)
                    .clip(PultDimens.shapeCard)
                    .background(PultColors.surface)
                    .border(1.dp, PultColors.border, PultDimens.shapeCard)
            )
        }
    }
}

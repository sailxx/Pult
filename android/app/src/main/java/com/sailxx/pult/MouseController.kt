package com.sailxx.pult

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Высокопроизводительный диспетчер относительного перемещения мыши.
 * Аккумулирует смещения touch-событий и отправляет их строго последовательно без гонок запросов,
 * исключая рывки, перегрузку TCP-стека и отставание курсора.
 */
class MouseController(
    private val client: Client,
    private val scope: CoroutineScope
) {
    private val pendingDx = AtomicInteger(0)
    private val pendingDy = AtomicInteger(0)
    private var isSending = false

    @Volatile
    var speedMultiplier: Float = 1.6f

    fun onMove(dx: Int, dy: Int) {
        pendingDx.addAndGet((dx * speedMultiplier).toInt())
        pendingDy.addAndGet((dy * speedMultiplier).toInt())
        scheduleSend()
    }

    fun onMove(dx: Float, dy: Float) {
        pendingDx.addAndGet((dx * speedMultiplier).toInt())
        pendingDy.addAndGet((dy * speedMultiplier).toInt())
        scheduleSend()
    }

    private fun scheduleSend() {
        synchronized(this) {
            if (isSending) return
            isSending = true
        }

        scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    val dx = pendingDx.getAndSet(0)
                    val dy = pendingDy.getAndSet(0)
                    if (dx == 0 && dy == 0) break

                    try {
                        client.mouseMove(dx, dy)
                    } catch (_: Exception) {}

                    // Плавный интервал для частоты до 60 кадров/сек
                    delay(12)
                }
            } finally {
                synchronized(this@MouseController) {
                    isSending = false
                    if (pendingDx.get() != 0 || pendingDy.get() != 0) {
                        scheduleSend()
                    }
                }
            }
        }
    }

    fun click(btn: String = "left") {
        scope.launch(Dispatchers.IO) {
            try { client.mouseClick(btn) } catch (_: Exception) {}
        }
    }

    fun down(btn: String = "left") {
        scope.launch(Dispatchers.IO) {
            try { client.mouseDown(btn) } catch (_: Exception) {}
        }
    }

    fun up(btn: String = "left") {
        scope.launch(Dispatchers.IO) {
            try { client.mouseUp(btn) } catch (_: Exception) {}
        }
    }

    fun releaseAll() {
        pendingDx.set(0)
        pendingDy.set(0)
        scope.launch(Dispatchers.IO) {
            try { client.mouseUp("all") } catch (_: Exception) {}
        }
    }

    fun scroll(delta: Int) {
        scope.launch(Dispatchers.IO) {
            try { client.mouseScroll(delta) } catch (_: Exception) {}
        }
    }
}

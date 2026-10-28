package com.sailxx.pult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

const val HTTP_PORT = 47800
const val UDP_PORT = 47801

data class Host(val ip: String, val name: String)
data class AppItem(val id: String, val name: String)

class WrongPin : Exception()

/** Все запросы идут через [Client]; `ip` и `pin` берутся из настроек. */
class Client(private val ip: String, private val pin: String) {

    private fun open(path: String, method: String, timeoutMs: Int): HttpURLConnection {
        val c = URL("http://$ip:$HTTP_PORT$path").openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.setRequestProperty("X-Pin", pin)
        return c
    }

    private suspend fun call(path: String, method: String = "GET", timeoutMs: Int = 4000): ByteArray =
        withContext(Dispatchers.IO) {
            val c = open(path, method, timeoutMs)
            try {
                if (c.responseCode == 401) throw WrongPin()
                if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
                c.inputStream.use { it.readBytes() }
            } finally {
                c.disconnect()
            }
        }

    suspend fun apps(): List<AppItem> {
        val text = String(call("/apps", timeoutMs = 8000), Charsets.UTF_8)
        // Ответ хоста простой: [{"id":"..","name":".."},...]
        val re = Regex("\"id\":\"([0-9a-f]+)\",\"name\":\"((?:[^\"\\\\]|\\\\.)*)\"")
        return re.findAll(text).map { AppItem(it.groupValues[1], unescape(it.groupValues[2])) }.toList()
    }

    suspend fun icon(id: String): ByteArray? = try { call("/icon?id=$id") } catch (e: WrongPin) { throw e } catch (e: Exception) { null }

    suspend fun launch(id: String) { call("/launch?id=$id", "POST") }

    suspend fun key(name: String) { call("/key?name=${URLEncoder.encode(name, "UTF-8")}", "POST") }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                if (n == 'u' && i + 5 < s.length) {
                    sb.append(s.substring(i + 2, i + 6).toInt(16).toChar()); i += 6; continue
                }
                sb.append(n); i += 2; continue
            }
            sb.append(ch); i++
        }
        return sb.toString()
    }
}

/** Проверка без PIN: отвечает ли по этому адресу хост Pult. */
suspend fun ping(ip: String, timeoutMs: Int = 800): Host? = withContext(Dispatchers.IO) {
    try {
        val c = URL("http://$ip:$HTTP_PORT/ping").openConnection() as HttpURLConnection
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        try {
            if (c.responseCode != 200) return@withContext null
            val body = c.inputStream.use { String(it.readBytes()) }
            if (!body.contains("\"pult\"")) return@withContext null
            val name = Regex("\"name\":\"([^\"]*)\"").find(body)?.groupValues?.get(1) ?: ip
            Host(ip, name)
        } finally {
            c.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}

private data class Local(val ip: String, val prefix: Int, val broadcast: InetAddress?)

private fun localAddresses(): List<Local> =
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.interfaceAddresses }
        .filter { it.address is Inet4Address }
        .map { Local(it.address.hostAddress!!, it.networkPrefixLength.toInt(), it.broadcast) }

/**
 * Два способа сразу: широковещательный UDP (быстро) и перебор адресов своей подсети по /ping
 * (работает, когда роутер режет broadcast между устройствами).
 */
suspend fun discover(): List<Host> = coroutineScope {
    val found = java.util.concurrent.ConcurrentHashMap<String, Host>()
    val locals = localAddresses()

    val udp = async(Dispatchers.IO) {
        try {
            DatagramSocket().use { s ->
                s.broadcast = true
                s.soTimeout = 1500
                val msg = "PULT?".toByteArray()
                val targets = locals.mapNotNull { it.broadcast } + InetAddress.getByName("255.255.255.255")
                for (t in targets) {
                    try { s.send(DatagramPacket(msg, msg.size, t, UDP_PORT)) } catch (_: Exception) {}
                }
                val buf = ByteArray(256)
                val end = System.currentTimeMillis() + 1800
                while (System.currentTimeMillis() < end) {
                    val p = DatagramPacket(buf, buf.size)
                    try { s.receive(p) } catch (_: SocketTimeoutException) { break }
                    val text = String(p.data, 0, p.length)
                    if (text.startsWith("PULT!")) {
                        val ip = p.address.hostAddress!!
                        found[ip] = Host(ip, text.substringAfter('|', ip))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val gate = Semaphore(48)
    val scans = locals.filter { it.prefix in 16..30 }.flatMap { l ->
        val parts = l.ip.split('.').map { it.toInt() }
        (1..254).map { n -> "${parts[0]}.${parts[1]}.${parts[2]}.$n" }.filter { it != l.ip }
    }.distinct().map { ip ->
        async { gate.withPermit { ping(ip, 500)?.let { found[it.ip] = it } } }
    }

    udp.await()
    scans.forEach { it.await() }
    found.values.sortedBy { it.name }
}

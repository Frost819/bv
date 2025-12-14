package dev.aaa1115910.biliapi.websocket

import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.FrameHeader
import dev.aaa1115910.biliapi.http.entity.live.FrameType
import dev.aaa1115910.biliapi.http.entity.live.HostListItem
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.brotliDecompress
import dev.aaa1115910.biliapi.http.util.zlibDecompress
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.wss
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.remaining
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.intOrNull
import dev.aaa1115910.biliapi.http.entity.live.AuthRequest

object LiveDataWebSocket {
    private lateinit var client: HttpClient
    private val logger = KotlinLogging.logger { }

    private val heartbeat: ByteArray = FrameHeader(
        totalLength = 16,
        headerLength = 16,
        version = 1,
        type = FrameType.HeartRequest.code,
        sequence = 1
    ).toBinary().readByteArray()

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(WebSockets)
        }
    }

    suspend fun connectLiveEvent(
        roomId: Int,
        onEvent: (event: LiveEvent) -> Unit
    ): Job {
        val job = client.launch {
            while (isActive) {
                runCatching {
                    val connectInfo = withContext(Dispatchers.IO) {
                        BiliHttpApi.updateWbi()
                        val imgKey = BiliHttpApi.wbiImgKey ?: throw CancellationException()
                        val subKey = BiliHttpApi.wbiSubKey ?: throw CancellationException()
                        val real = BiliLiveHttpApi.getLiveRoomPlayInfo(roomId).data?.roomId
                            ?: throw CancellationException()
                        val danmuInfo = BiliLiveHttpApi.getLiveDanmuInfoWbi(
                            roomId = real,
                            imgKey = imgKey,
                            subKey = subKey
                        ).data ?: throw CancellationException()
                        val auth = AuthRequest(roomId = real, key = danmuInfo.token).toBinary().readByteArray()
                        ConnectInfo(real, auth, danmuInfo.hostList)
                    }

                    var lastError: Throwable? = null
                    for (hostItem in connectInfo.hosts) {
                        lastError = runCatching {
                            client.wss(host = hostItem.host, port = hostItem.wssPort, path = "/sub") {
                                outgoing.send(Frame.Binary(true, connectInfo.authFrame))
                                val heartbeatJob = client.launch {
                                    while (isActive) {
                                        delay(30_000)
                                        outgoing.send(Frame.Binary(true, heartbeat))
                                    }
                                }

                                try {
                                    while (isActive) {
                                        val frame = incoming.receive()
                                        val eventData = frame.data
                                        handleLiveEventData(eventData).forEach(onEvent)
                                    }
                                } finally {
                                    heartbeatJob.cancel()
                                }
                            }
                            throw IllegalStateException("WebSocket closed")
                        }.exceptionOrNull()

                        if (lastError == null) break
                    }

                    throw lastError ?: IllegalStateException("WebSocket connect failed")
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.warn(it) { "Live danmaku websocket failed" }
                    delay(3000)
                }
            }
        }
        job.invokeOnCompletion {
            it?.printStackTrace()
        }
        return job
    }

    private suspend fun handleLiveEventData(data: ByteArray): List<LiveEvent> {
        return withContext(Dispatchers.IO) {
            if (data.size < 16) return@withContext emptyList()
            parsePacketStream(data)
        }
    }

    private fun parsePacketStream(data: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        val bytePack = ByteReadPacket(data)
        while (bytePack.remaining >= 16) {
            val header = readFrameHeader(bytePack) ?: break
            if (header.dataLength < 0 || bytePack.remaining < header.dataLength) break
            val body = bytePack.readByteArray(header.dataLength)
            result += handlePacket(header, body)
        }
        return result
    }

    private fun handlePacket(header: FrameHeader, body: ByteArray): List<LiveEvent> {
        return when (header.type) {
            FrameType.HeartResponse.code -> emptyList()
            FrameType.AuthResponse.code -> {
                val code = runCatching {
                    val json = Json.parseToJsonElement(body.decodeToString()).jsonObject
                    json["code"]?.jsonPrimitive?.intOrNull
                }.getOrNull()
                logger.info { "Live danmaku auth response code=$code" }
                if (code != null && code != 0) throw IllegalStateException("Live danmaku auth failed code=$code")
                emptyList()
            }
            FrameType.Normal.code -> handleCommandPacket(header.version.toInt(), body)
            else -> emptyList()
        }
    }

    private fun handleCommandPacket(version: Int, body: ByteArray): List<LiveEvent> {
        return when (version) {
            0, 1 -> {
                val strData = body.decodeToString()
                handleLiveCMDEventString(strData)?.let(::listOf) ?: emptyList()
            }
            2 -> parsePacketStream(body.zlibDecompress())
            3 -> parsePacketStream(body.brotliDecompress())
            else -> emptyList()
        }
    }

    private fun handleLiveCMDEventString(strData: String): LiveEvent? {
        val dataJson = Json.parseToJsonElement(strData).jsonObject
        val cmd = dataJson["cmd"]!!.jsonPrimitive.content

        if (cmd.startsWith("DANMU_MSG")) {
            runCatching {
                val danmakuContent = dataJson["info"]!!.jsonArray[1].jsonPrimitive.content
                val senderMid = dataJson["info"]!!.jsonArray[2].jsonArray[0].jsonPrimitive.long
                val senderUsername =
                    dataJson["info"]!!.jsonArray[2].jsonArray[1].jsonPrimitive.content
                var medalLevel: Int? = null
                var medalName: String? = null
                runCatching {
                    medalLevel =
                        dataJson["info"]?.jsonArray?.get(3)?.jsonArray?.get(0)?.jsonPrimitive?.int
                    medalName =
                        dataJson["info"]?.jsonArray?.get(3)?.jsonArray?.get(1)?.jsonPrimitive?.content
                }

                return DanmakuEvent(
                    content = danmakuContent,
                    mid = senderMid,
                    username = senderUsername,
                    medalName = medalName,
                    medalLevel = medalLevel
                )
            }.onFailure {
                logger.warn { "Parse danmaku content failed: ${it.message}" }
            }
            return null
        }

        when (cmd) {
            "COMBO_SEND" -> {}
            "DM_INTERACTION" -> {
                runCatching {
                    val data = dataJson["data"]?.jsonObject ?: return null
                    if (data["type"]?.jsonPrimitive?.intOrNull != 102) return null
                    val inner = data["data"]
                    val innerObj = when {
                        inner == null -> return null
                        inner is kotlinx.serialization.json.JsonPrimitive && inner.isString -> {
                            Json.parseToJsonElement(inner.content).jsonObject
                        }
                        else -> inner.jsonObject
                    }
                    val combo = innerObj["combo"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
                    val danmakuContent = combo["content"]?.jsonPrimitive?.content ?: return null
                    return DanmakuEvent(
                        content = danmakuContent,
                        mid = 0,
                        username = ""
                    )
                }
            }
            "COMMON_NOTICE_DANMAKU" -> {
                runCatching {
                    val data = dataJson["data"]?.jsonObject ?: return null
                    val segments = data["content_segments"]?.jsonArray ?: return null
                    val text = segments.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                    if (text.isBlank()) return null
                    return DanmakuEvent(
                        content = text,
                        mid = 0,
                        username = ""
                    )
                }
            }
            "INTERACT_WORD_V2" -> {}
            "LOG_IN_NOTICE" -> {}
            "ENTRY_EFFECT" -> {}
            "GUARD_BUY" -> {}
            "GUARD_HONOR_THOUSAND" -> {}
            "HOT_RANK_CHANGED" -> {}
            "HOT_RANK_CHANGED_V2" -> {}
            "HOT_RANK_SETTLEMENT" -> {}
            "HOT_RANK_SETTLEMENT_V2" -> {}
            "HOT_ROOM_NOTIFY" -> {}
            "INTERACT_WORD" -> {}
            "LIVE" -> {}
            "LIVE_INTERACTIVE_GAME" -> {}
            "LIKE_INFO_V3_CLICK" -> {}
            "LIKE_INFO_V3_UPDATE" -> {}
            "NOTICE_MSG" -> {}
            "ONLINE_RANK_COUNT" -> {}
            "ONLINE_RANK_V2" -> {}
            "ONLINE_RANK_TOP3" -> {}
            "PREPARING" -> {}
            "ROOM_REAL_TIME_MESSAGE_UPDATE" -> {}
            "SEND_GIFT" -> {}
            "STOP_LIVE_ROOM_LIST" -> {}
            "SUPER_CHAT_ENTRANCE" -> {}
            "SUPER_CHAT_MESSAGE" -> {}
            "SUPER_CHAT_MESSAGE_JPN" -> {}
            "SYS_MSG" -> {}
            "USER_TOAST_MSG" -> {}
            "WATCHED_CHANGE" -> {}
            "WIDGET_BANNER" -> {}
            else -> {
                logger.warn { "Unknown live event: $cmd" }
                logger.warn { dataJson }
            }
        }
        return null
    }

    private fun readFrameHeader(packet: ByteReadPacket): FrameHeader? {
        if (packet.remaining < 16) return null
        return FrameHeader(
            totalLength = packet.readInt(),
            headerLength = packet.readShort(),
            version = packet.readShort(),
            type = packet.readInt(),
            sequence = packet.readInt()
        )
    }

    private data class ConnectInfo(
        val roomId: Int,
        val authFrame: ByteArray,
        val hosts: List<HostListItem>
    )
}
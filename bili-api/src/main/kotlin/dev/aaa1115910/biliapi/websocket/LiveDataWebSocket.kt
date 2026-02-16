package dev.aaa1115910.biliapi.websocket

import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.FrameHeader
import dev.aaa1115910.biliapi.http.entity.live.HostListItem
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.entity.live.OnlineRankCountEvent
import dev.aaa1115910.biliapi.http.entity.live.PopularityChangeEvent
import dev.aaa1115910.biliapi.http.entity.live.readFrameHeader
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.brotliDecompress
import dev.aaa1115910.biliapi.http.util.zlibDecompress
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.wss
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.core.writePacket
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

object LiveDataWebSocket {
    private lateinit var client: HttpClient
    private val logger = KotlinLogging.logger { }

    // 预配置的 Json 实例，复用以提高解析效率
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** 最大重连次数 */
    private const val MAX_RECONNECT_ATTEMPTS = 5
    /** 初始重连延迟 (ms) */
    private const val INITIAL_RECONNECT_DELAY = 1000L
    /** 最大重连延迟 (ms) */
    private const val MAX_RECONNECT_DELAY = 30_000L

    private val heartbeat = byteArrayOf(
        0, 0, 0, 0x1f,
        0, 0x10, 0, 0x1,
        0, 0, 0, 0x2,
        0, 0, 0, 0x1,
        0x5b, 0x6f, 0x62, 0x6a,
        0x65, 0x63, 0x74, 0x20,
        0x4f, 0x62, 0x6a, 0x65,
        0x63, 0x74, 0x5d
    )

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(WebSockets)
        }
    }

    /**
     * 连接直播事件 WebSocket（自动获取连接信息）
     * 内部会调用 API 获取 token 和房间号，适用于外部未预取数据的场景
     */
    suspend fun connectLiveEvent(
        roomId: Int,
        uid: Long = 0,
        onEvent: (event: LiveEvent) -> Unit
    ): Job {
        val danmuInfo =
            BiliLiveHttpApi.getLiveDanmuInfo(roomId).data ?: throw CancellationException()
        val realRoomId =
            BiliLiveHttpApi.getLiveRoomPlayInfo(roomId).data?.roomId
                ?: throw CancellationException()

        return connectLiveEvent(
            realRoomId = realRoomId,
            token = danmuInfo.token,
            hostList = danmuInfo.hostList,
            uid = uid,
            onEvent = onEvent
        )
    }

    /**
     * 连接直播事件 WebSocket（使用预取的连接信息，避免重复 API 调用）
     * @param realRoomId 真实房间号
     * @param token 弹幕连接 token
     * @param hostList WebSocket 主机列表，会按顺序尝试连接
     * @param uid 用户 uid，已登录用户传入实际 uid，未登录传 0
     * @param onEvent 事件回调
     */
    suspend fun connectLiveEvent(
        realRoomId: Int,
        token: String,
        hostList: List<HostListItem>,
        uid: Long = 0,
        onEvent: (event: LiveEvent) -> Unit
    ): Job {
        val data = buildJsonObject {
            put("uid", uid)
            put("roomid", realRoomId)
            put("protover", 3)
            put("platform", "web")
            put("type", 2)
            put("key", token)
        }.toString().toByteArray()
        val authPacket = buildPacket {
            val size = 16 + data.size
            writeInt(size) // 封包总大小
            writeShort(0x10) // 头部大小
            writeShort(1) // 协议版本
            writeInt(7) // 类型
            writeInt(1)
            writePacket(ByteReadPacket(data))
        }
        val authBytes = authPacket.readByteArray()

        val job = client.launch {
            var reconnectAttempt = 0
            var reconnectDelay = INITIAL_RECONNECT_DELAY

            while (isActive) {
                var connected = false
                // 按顺序尝试 host 列表中的每个服务器
                for (host in hostList) {
                    if (!isActive) break
                    try {
                        logger.info { "Connecting to WebSocket host: ${host.host}:${host.wssPort} (attempt ${reconnectAttempt + 1})" }
                        client.wss(
                            host = host.host,
                            port = host.wssPort,
                            path = "/sub"
                        ) {
                            // 连接成功，重置重连计数
                            connected = true
                            reconnectAttempt = 0
                            reconnectDelay = INITIAL_RECONNECT_DELAY
                            logger.info { "WebSocket connected to ${host.host}:${host.wssPort}" }

                            outgoing.send(Frame.Binary(true, authBytes))
                            launch {
                                delay(5000)
                                while (isActive) {
                                    outgoing.send(Frame.Binary(true, heartbeat))
                                    delay(30_000)
                                }
                            }

                            // 使用固定数量的消费者协程处理消息，避免每条消息创建新协程
                            val messageChannel = Channel<ByteArray>(Channel.BUFFERED)
                            val workerCount = 2 // 固定使用2个消费者协程
                            val activeWorkers = AtomicInteger(workerCount)

                            // 启动固定数量的消费者协程
                            repeat(workerCount) {
                                launch {
                                    try {
                                        for (eventData in messageChannel) {
                                            handleLiveEventData(eventData).forEach { event ->
                                                onEvent(event)
                                            }
                                        }
                                    } finally {
                                        if (activeWorkers.decrementAndGet() == 0) {
                                            messageChannel.close()
                                        }
                                    }
                                }
                            }

                            // 接收消息并发送到 Channel
                            try {
                                while (isActive) {
                                    val frame = incoming.receive()
                                    val eventData = frame.data
                                    if (!messageChannel.trySend(eventData).isSuccess) {
                                        // Channel 满了，丢弃消息（背压处理）
                                        logger.warn { "Message channel full, dropping message" }
                                    }
                                }
                            } finally {
                                messageChannel.close()
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e // 不拦截取消
                    } catch (e: Exception) {
                        logger.warn { "WebSocket connection to ${host.host} failed: ${e.message}" }
                        // 继续尝试下一个 host
                    }
                    if (connected) break
                }

                // 如果所有 host 都失败了，或者连接断开后需要重连
                if (!isActive) break
                reconnectAttempt++
                if (reconnectAttempt > MAX_RECONNECT_ATTEMPTS) {
                    logger.error { "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached, giving up" }
                    break
                }
                logger.info { "Reconnecting in ${reconnectDelay}ms (attempt $reconnectAttempt/$MAX_RECONNECT_ATTEMPTS)" }
                delay(reconnectDelay)
                reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
            }
        }
        job.invokeOnCompletion {
            logger.info { "LiveDataWebSocket connection closed: ${it?.message ?: "normal"}" }
        }
        return job
    }

    private suspend fun handleLiveEventData(data: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        withContext(Dispatchers.IO) {
            if (data.size <= 16) return@withContext
            val bytePack = ByteReadPacket(data)
            val head = bytePack.readFrameHeader()
            val body = bytePack.readByteArray((head.totalLength - head.headerLength))
            result.addAll(handleLiveEventBody(head, body))
        }
        return result
    }

    private fun formatPopularity(popularity: Int): String {
        return when {
            popularity >= 100_000_000 -> String.format("%.1f亿人气", popularity / 100_000_000.0)
            popularity >= 10_000 -> String.format("%.1f万人气", popularity / 10_000.0)
            else -> "${popularity}人气"
        }
    }

    private fun handleLiveEventBody(head: FrameHeader, data: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        val bytePack = ByteReadPacket(data)
        when (head.type) {
            //心跳包回复（人气值）
            3 -> {
                // 不从心跳解析人气值，使用 POPULARITY_CHANGE CMD 事件更新
                runCatching { bytePack.readInt() }
            }

            //普通包（命令）
            5 -> {
                when (head.version.toInt()) {
                    //0 普通包正文不使用压缩
                    //1 心跳及认证包正文不使用压缩
                    0, 1 -> {
                        val strData = bytePack.readByteArray().decodeToString()
                        handleLiveCMDEventString(strData)?.let { result += it }
                    }

                    //普通包正文使用zlib压缩
                    2 -> {
                        val decompress = bytePack.readByteArray().zlibDecompress()
                        result += handleLiveEventBodyDecompress(decompress)
                    }

                    //普通包正文使用brotli压缩,解压为一个带头部的协议0普通包
                    3 -> {
                        val decompress = bytePack.readByteArray().brotliDecompress()
                        result += handleLiveEventBodyDecompress(decompress)
                    }

                    else -> {
                        logger.warn { "Unknown package version: ${head.version}" }
                        bytePack.readByteArray()
                    }
                }
            }

            //认证包回复
            8 -> {
                bytePack.readByteArray(10)
            }

            else -> {
                logger.warn { "Unknown package type: ${head.type}" }
                bytePack.readByteArray()
            }
        }
        return if (bytePack.remaining > 16) result + handleLiveEventBody(
            bytePack.readFrameHeader(),
            bytePack.readByteArray()
        )
        else result
    }

    private fun handleLiveEventBodyDecompress(data: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        val bytePack = ByteReadPacket(data)
        val header = bytePack.readFrameHeader()
        val body = bytePack.readByteArray(header.dataLength)
        result += handleLiveCMDEvent(header, body)
        return if (bytePack.remaining > 0) result + handleLiveEventBodyDecompress(bytePack.readByteArray()) else result
    }

    private fun handleLiveCMDEvent(head: FrameHeader, data: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        val strData: String
        when (head.version.toInt()) {
            0 -> {
                strData = data.decodeToString()
            }

            2 -> {
                val decompress = data.zlibDecompress()
                val bytePack = ByteReadPacket(decompress)
                val packageHeader = bytePack.readFrameHeader()
                val body =
                    bytePack.readByteArray((packageHeader.totalLength - packageHeader.headerLength))
                if (bytePack.remaining > 16) {
                    result += handleLiveEventBody(
                        bytePack.readFrameHeader(),
                        bytePack.readByteArray()
                    )
                }
                strData = body.decodeToString()
            }

            else -> {
                logger.warn { "Dropped inner packet with unknown version: ${head.version}, dataSize: ${data.size}" }
                return result
            }
        }
        handleLiveCMDEventString(strData)?.let { result += it }
        return result
    }

    private fun handleLiveCMDEventString(strData: String): LiveEvent? {
        val dataJson = json.parseToJsonElement(strData).jsonObject
        val cmd = dataJson["cmd"]!!.jsonPrimitive.content

        when (cmd) {
            "COMBO_SEND" -> {}
            "DANMU_MSG" -> {
                runCatching {
                    val infoArray = dataJson["info"]!!.jsonArray

                    // 弹幕内容 info[1]，需要检查是否为字符串（有些情况下是 JSON 对象）
                    val contentElement = infoArray[1]
                    if (contentElement !is kotlinx.serialization.json.JsonPrimitive) {
                        logger.warn { "DANMU_MSG content is not a string, skipping: $contentElement" }
                        return@runCatching
                    }
                    val danmakuContent = contentElement.jsonPrimitive.content
                    
                    // 弹幕属性 info[0]
                    val attrArray = infoArray[0].jsonArray
                    val mode = attrArray[1].jsonPrimitive.int          // 弹幕模式
                    val fontSize = attrArray[2].jsonPrimitive.int      // 字号
                    val color = attrArray[3].jsonPrimitive.int         // 颜色
                    
                    // 用户信息 info[2]
                    val userArray = infoArray[2].jsonArray
                    val senderMid = userArray[0].jsonPrimitive.long
                    val senderUsername = userArray[1].jsonPrimitive.content
                    
                    // 粉丝勋章 info[3]（可能为空数组）
                    var medalLevel: Int? = null
                    var medalName: String? = null
                    runCatching {
                        val medalArray = infoArray[3].jsonArray
                        if (medalArray.size > 0) {
                            medalLevel = medalArray[0].jsonPrimitive.int
                            medalName = medalArray[1].jsonPrimitive.content
                        }
                    }

                    return DanmakuEvent(
                        content = danmakuContent,
                        mid = senderMid,
                        username = senderUsername,
                        medalName = medalName,
                        medalLevel = medalLevel,
                        mode = mode,
                        fontSize = fontSize,
                        color = color
                    )
                }.onFailure {
                    logger.warn { "Parse danmaku content failed: ${it.message}" }
                }
            }

            "ENTRY_EFFECT" -> {}
            //有人上舰
            "GUARD_BUY" -> {}
            //千舰通知
            "GUARD_HONOR_THOUSAND" -> {
                println(dataJson)
            }

            "HOT_RANK_CHANGED" -> {}
            "HOT_RANK_CHANGED_V2" -> {}
            "HOT_RANK_SETTLEMENT" -> {}
            "HOT_RANK_SETTLEMENT_V2" -> {}
            "HOT_ROOM_NOTIFY" -> {}
            "INTERACT_WORD" -> {}
            "INTERACT_WORD_V2" -> {}
            "LIVE" -> {
                logger.info { "[EVENT-LIVE] $dataJson" }
            }

            "LIVE_INTERACTIVE_GAME" -> {}
            "LIKE_INFO_V3_CLICK" -> {}
            "LIKE_INFO_V3_UPDATE" -> {}
            "LOG_IN_NOTICE" -> {}
            "NOTICE_MSG" -> {}
            "ONLINE_RANK_COUNT" -> {
                runCatching {
                    val data = dataJson["data"]!!.jsonObject
                    val count = data["count"]!!.jsonPrimitive.int
                    return OnlineRankCountEvent(count = count)
                }.onFailure {
                    logger.warn { "Parse ONLINE_RANK_COUNT failed: ${it.message}" }
                }
            }
            "ONLINE_RANK_V2" -> {}
            "ONLINE_RANK_V3" -> {}
            "ONLINE_RANK_TOP3" -> {}
            "POPULAR_RANK_CHANGED" -> {
                logger.info { "[EVENT-POPULAR_RANK_CHANGED] $dataJson" }
            }
            "POPULARITY_CHANGE" -> {
                logger.info { "[EVENT-POPULARITY_CHANGE] $dataJson" }
                runCatching {
                    val data = dataJson["data"]!!.jsonObject
                    val popularity = data["popularity"]!!.jsonPrimitive.int
                    val popularityText = data["popularity_text"]!!.jsonPrimitive.content
                    return PopularityChangeEvent(
                        popularity = popularity,
                        popularityText = popularityText
                    )
                }.onFailure {
                    logger.warn { "Parse POPULARITY_CHANGE failed: ${it.message}" }
                }
            }
            "PREPARING" -> {}
            "ROOM_REAL_TIME_MESSAGE_UPDATE" -> {}
            "SEND_GIFT" -> {}
            "STOP_LIVE_ROOM_LIST" -> {}
            //醒目留言入口提醒（氪金提醒）
            "SUPER_CHAT_ENTRANCE" -> {}
            //醒目留言
            "SUPER_CHAT_MESSAGE" -> {}
            //醒目留言
            "SUPER_CHAT_MESSAGE_JPN" -> {}
            "SYS_MSG" -> {
                println(dataJson)
            }

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
}
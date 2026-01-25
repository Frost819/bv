package dev.aaa1115910.biliapi.websocket

import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.FrameHeader
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.entity.live.readFrameHeader
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    suspend fun connectLiveEvent(
        roomId: Int,
        onEvent: (event: LiveEvent) -> Unit
    ): Job {
        val danmuInfo =
            BiliLiveHttpApi.getLiveDanmuInfo(roomId).data ?: throw CancellationException()
        val realRoomId =
            BiliLiveHttpApi.getLiveRoomPlayInfo(roomId).data?.roomId
                ?: throw CancellationException()
        val hosts = danmuInfo.hostList.last()

        val data = buildJsonObject {
            put("uid", 0)
            put("roomid", realRoomId)
            put("protover", 2)
            put("platform", "web")
            put("type", 2)
            put("key", danmuInfo.token)
        }.toString().toByteArray()
        val b = buildPacket {
            val size = 16 + data.size
            writeInt(size) // 封包总大小
            writeShort(0x10) // 头部大小
            writeShort(1) // 协议版本
            writeInt(7) // 类型
            writeInt(1)
            writePacket(ByteReadPacket(data))
        }

        val job = client.launch {
            client.wss(
                host = hosts.host,
                port = hosts.wssPort,
                path = "/sub"
            ) {
                val byte = b.readByteArray()
                outgoing.send(Frame.Binary(true, byte))
                launch {
                    delay(5000)
                    while (isActive) {
                        //println("send heart")
                        outgoing.send(Frame.Binary(true, heartbeat))
                        delay(30_000)
                    }
                }
                while (isActive) {
                    val frame = incoming.receive()
                    val eventData = frame.data
                    launch {

                        handleLiveEventData(eventData).forEach { event ->
                            onEvent(event)
                        }
                    }
                }
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

    private fun handleLiveEventBody(head: FrameHeader, data: ByteArray): List<LiveEvent> {
        val result = mutableListOf<LiveEvent>()
        val bytePack = ByteReadPacket(data)
        when (head.type) {
            //心跳包回复（人气值）
            3 -> {
                //println("接收心跳，房间人气值: ${bytePack.readInt()}")
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
                        logger.warn { "todo package version: ${head.version}" }
                        bytePack.readByteArray()
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

            else -> return result
        }
        handleLiveCMDEventString(strData)?.let { result += it }
        return result
    }

    private fun handleLiveCMDEventString(strData: String): LiveEvent? {
        val dataJson = Json.parseToJsonElement(strData).jsonObject
        val cmd = dataJson["cmd"]!!.jsonPrimitive.content

        when (cmd) {
            "COMBO_SEND" -> {}
            "DANMU_MSG" -> {
                runCatching {
                    val infoArray = dataJson["info"]!!.jsonArray
                    
                    // 弹幕内容 info[1]
                    val danmakuContent = infoArray[1].jsonPrimitive.content
                    
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
            "LIVE" -> {
                println(dataJson)
            }

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
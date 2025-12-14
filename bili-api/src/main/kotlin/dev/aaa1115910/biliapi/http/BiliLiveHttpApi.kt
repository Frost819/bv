package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.live.LiveAreaResponse
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomListResponse
import dev.aaa1115910.biliapi.http.entity.live.DanmuInfoData
import dev.aaa1115910.biliapi.http.entity.live.HistoryDanmaku
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoData
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoV2Data
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.buildWbiRid
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BiliLiveHttpApi {
    private var endPoint: String = ""
    private lateinit var client: HttpClient
    private val logger = KotlinLogging.logger { }

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(ContentNegotiation) {
                json(Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    host = "api.live.bilibili.com"
                    protocol = URLProtocol.HTTPS
                }
            }
        }
    }

    /**
     * 获取直播分区列表
     */
    suspend fun getAreaList(): LiveAreaResponse =
        client.get("/room/v1/Area/getList").body()

    /**
     * 获取直播间列表
     */
    suspend fun getRoomList(
        parentAreaId: Int,
        areaId: Int,
        page: Int = 1,
        pageSize: Int = 30,
        sortType: String = "online"
    ): LiveRoomListResponse =
        client.get("/room/v1/Area/getRoomList") {
            parameter("parent_area_id", parentAreaId)
            parameter("area_id", areaId)
            parameter("sort_type", sortType)
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()

    /**
     * 获取直播间[roomId]的弹幕连接地址等信息，例如 token
     */
    suspend fun getLiveDanmuInfo(roomId: Int): BiliResponse<DanmuInfoData> =
        client.get("/xlive/web-room/v1/index/getDanmuInfo") {
            parameter("id", roomId)
        }.body()

    suspend fun getLiveDanmuInfoWbi(
        roomId: Int,
        imgKey: String,
        subKey: String
    ): BiliResponse<DanmuInfoData> {
        val wts = (System.currentTimeMillis() / 1000).toInt()
        val wRid = buildWbiRid(
            queryParams = mapOf(
                "id" to roomId.toString(),
                "type" to "0"
            ),
            imgKey = imgKey,
            subKey = subKey,
            wts = wts
        )
        return client.get("/xlive/web-room/v1/index/getDanmuInfo") {
            parameter("id", roomId)
            parameter("type", 0)
            parameter("wts", wts)
            parameter("w_rid", wRid)
        }.body()
    }

    /**
     * 获取直播间[roomId]的信息
     */
    suspend fun getLiveRoomPlayInfo(roomId: Int): BiliResponse<RoomPlayInfoData> =
        client.get("/xlive/web-room/v1/index/getRoomPlayInfo") {
            parameter("room_id", roomId)
        }.body()

    suspend fun getLiveRoomPlayInfoV2(
        roomId: Int,
        qn: Int = 10000,
        platform: String = "web",
        protocol: String = "0,1",
        format: String = "0,1,2",
        codec: String = "0,1",
        dolby: Int = 5,
        panorama: Int = 1,
        sessData: String? = null
    ): BiliResponse<RoomPlayInfoV2Data> =
        client.get("/xlive/web-room/v2/index/getRoomPlayInfo") {
            parameter("room_id", roomId)
            parameter("qn", qn)
            parameter("platform", platform)
            parameter("protocol", protocol)
            parameter("format", format)
            parameter("codec", codec)
            parameter("dolby", dolby)
            parameter("panorama", panorama)
            header("referer", "https://live.bilibili.com")
            sessData?.takeIf { it.isNotBlank() }?.let { header("Cookie", "SESSDATA=$it;") }
        }.body()

    /**
     * 获取直播间[roomId]的历史弹幕
     */
    suspend fun getLiveDanmuHistory(roomId: Int): BiliResponse<HistoryDanmaku> =
        client.get("/xlive/web-room/v1/dM/gethistory") {
            parameter("roomid", roomId)
        }.body()

}
package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.live.LiveAreaResponse
import dev.aaa1115910.biliapi.entity.live.LiveRoomListResponse
import dev.aaa1115910.biliapi.entity.live.LiveRoomPlayInfoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class LiveRepository {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                coerceInputValues = true
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * 获取直播分区列表
     */
    suspend fun getLiveAreaList(): LiveAreaResponse = withContext(Dispatchers.IO) {
        client.get("https://api.live.bilibili.com/room/v1/Area/getList")
            .body()
    }

    /**
     * 获取直播间列表
     * @param parentAreaId 父分区ID
     * @param areaId 分区ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param sortType 排序方式，默认online（按在线人数）
     */
    suspend fun getLiveRoomList(
        parentAreaId: String,
        areaId: String,
        page: Int = 1,
        pageSize: Int = 30,
        sortType: String = "online"
    ): LiveRoomListResponse = withContext(Dispatchers.IO) {
        client.get("https://api.live.bilibili.com/room/v1/Area/getRoomList") {
            parameter("parent_area_id", parentAreaId)
            parameter("area_id", areaId)
            parameter("sort_type", sortType)
            parameter("page", page)
            parameter("page_size", pageSize)
        }.body()
    }

    /**
     * 获取直播间播放信息
     * @param roomId 直播间ID
     */
    suspend fun getLiveRoomPlayInfo(roomId: Int): LiveRoomPlayInfoResponse =
        withContext(Dispatchers.IO) {
            client.get("https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo") {
                parameter("room_id", roomId)
                parameter("qn", 10000)
                parameter("platform", "web")
                parameter("protocol", "0,1")
                parameter("format", "0,1,2")
                parameter("codec", "0,1")
                parameter("dolby", 5)
                parameter("panorama", 1)
            }.body()
        }
}

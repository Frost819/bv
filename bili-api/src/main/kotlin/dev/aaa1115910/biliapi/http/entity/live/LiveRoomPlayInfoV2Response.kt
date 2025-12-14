package dev.aaa1115910.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomPlayInfoV2Data(
    @SerialName("room_id")
    val roomId: Int,
    @SerialName("live_status")
    val liveStatus: Int = 0,
    @SerialName("playurl_info")
    val playUrlInfo: LivePlayUrlInfo? = null
)

@Serializable
data class LivePlayUrlInfo(
    val playurl: LivePlayUrl? = null
)

@Serializable
data class LivePlayUrl(
    val stream: List<LiveStream> = emptyList()
)

@Serializable
data class LiveStream(
    @SerialName("protocol_name")
    val protocolName: String,
    val format: List<LiveFormat> = emptyList()
)

@Serializable
data class LiveFormat(
    @SerialName("format_name")
    val formatName: String,
    val codec: List<LiveCodec> = emptyList()
)

@Serializable
data class LiveCodec(
    @SerialName("codec_name")
    val codecName: String,
    @SerialName("current_qn")
    val currentQn: Int = 0,
    @SerialName("base_url")
    val baseUrl: String,
    @SerialName("url_info")
    val urlInfo: List<LiveUrlInfo> = emptyList()
)

@Serializable
data class LiveUrlInfo(
    val host: String,
    val extra: String = ""
)


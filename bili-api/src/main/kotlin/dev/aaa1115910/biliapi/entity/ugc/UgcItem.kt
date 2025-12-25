package dev.aaa1115910.biliapi.entity.ugc

import dev.aaa1115910.biliapi.http.entity.home.RcmdIndexData
import dev.aaa1115910.biliapi.http.entity.home.RcmdTopData
import dev.aaa1115910.biliapi.http.util.smartDate
import dev.aaa1115910.biliapi.http.util.toSmartDate
import dev.aaa1115910.biliapi.util.convertStringTimeToSeconds

data class UgcItem(
    val aid: Long,
    val bvid: String = "",
    val title: String,
    val cover: String,
    val author: String,
    val authorMid: Long?,
    val play: Int,
    val danmaku: Int,
    val duration: Int,
    val idx: Int = -1,
    val pubTime: String? = null,
) {
    companion object {
        fun fromRcmdItem(rcmdItem: RcmdIndexData.RcmdItem) =
            UgcItem(
                aid = rcmdItem.args.aid ?: 0,
                title = rcmdItem.title!!,
                cover = rcmdItem.cover!!,
                author = rcmdItem.args.upName ?: "",
                authorMid = rcmdItem.args.upId,
                play = with(rcmdItem.coverLeftText1) {
                    runCatching {
                        if (this!!.endsWith("万")) {
                            (this.substring(0, this.length - 1).toDouble() * 10000).toInt()
                        } else {
                            this.toInt()
                        }
                    }.getOrDefault(-1)
                },
                danmaku = with(rcmdItem.coverLeftText2) {
                    if (this == null) return@with -1
                    runCatching {
                        if (this.endsWith("万")) {
                            (this.substring(0, this.length - 1).toDouble() * 10000).toInt()
                        } else {
                            this.toInt()
                        }
                    }.getOrDefault(-1)
                },
                duration = rcmdItem.coverRightText?.convertStringTimeToSeconds() ?: 0,
                idx = rcmdItem.idx
            )

        fun fromRcmdItem(rcmdItem: RcmdTopData.RcmdItem) =
            UgcItem(
                aid = rcmdItem.id,
                bvid = rcmdItem.bvid,
                title = rcmdItem.title,
                cover = rcmdItem.pic,
                author = rcmdItem.owner?.name ?: "",
                authorMid = rcmdItem.owner?.mid,
                play = rcmdItem.stat?.view ?: -1,
                danmaku = rcmdItem.stat?.danmaku ?: -1,
                duration = rcmdItem.duration,
                pubTime = rcmdItem.pubdate.smartDate
            )

        fun fromVideoInfo(videoInfo: dev.aaa1115910.biliapi.http.entity.video.VideoInfo) =
            UgcItem(
                aid = videoInfo.aid,
                title = videoInfo.title,
                duration = videoInfo.duration,
                author = videoInfo.owner.name,
                authorMid = videoInfo.owner.mid,
                cover = videoInfo.pic,
                play = videoInfo.stat.view,
                danmaku = videoInfo.stat.danmaku,
                pubTime = videoInfo.pubdate.smartDate
            )

        fun fromSmallCoverV5(card: bilibili.app.card.v1.SmallCoverV5): UgcItem {
            // 格式："n.n万观看 · n天前"
            val playAndPubTime = card.rightDesc2.split(" · ")
            val play = playAndPubTime.getOrNull(0)?.let { convertPlayStringToInt(it) } ?: -1
            val pubTime = playAndPubTime.getOrNull(1)

            return UgcItem(
                aid = card.base.param.toLong(),
                title = card.base.title,
                duration = convertStringTimeToSeconds(card.coverRightText1),
                author = card.rightDesc1,
                authorMid = card.up.id,
                cover = card.base.cover,
                play = play,
                pubTime = pubTime,
                danmaku = -1,
                idx = card.base.idx.toInt(),
            )
        }

        @Deprecated("User region v2 instead")
        fun fromRegionDynamicListItem(item: dev.aaa1115910.biliapi.http.entity.region.RegionDynamicList.Item) =
            UgcItem(
                aid = item.param.toLong(),
                title = item.title,
                duration = item.duration,
                author = item.name,
                authorMid = null,
                cover = item.cover,
                play = item.play ?: -1,
                danmaku = item.danmaku ?: -1,
                pubTime = item.pubDate.smartDate
            )

        fun fromRegionRcmdArchive(archive: dev.aaa1115910.biliapi.http.entity.region.RegionFeedRcmd.Archive) =
            UgcItem(
                aid = archive.aid,
                title = archive.title,
                duration = archive.duration,
                author = archive.author.name,
                authorMid = archive.author.mid,
                cover = archive.cover,
                play = archive.stat.view,
                danmaku = archive.stat.danmaku,
                pubTime = archive.pubdate.toSmartDate()
            )
    }
}

private fun convertStringTimeToSeconds(time: String): Int {
    val parts = time.split(":")
    val hours = if (parts.size == 3) parts[0].toInt() else 0
    val minutes = parts[parts.size - 2].toInt()
    val seconds = parts[parts.size - 1].toInt()
    return (hours * 3600) + (minutes * 60) + seconds
}

private fun convertPlayStringToInt(text: String): Int {
    if (text.isBlank()) return -1

    val value = text.replace("观看", "").trim()

    return try {
        when {
            value.endsWith("万") -> {
                val num = value.removeSuffix("万").toDouble()
                (num * 10_000).toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }

            value.endsWith("亿") -> {
                val num = value.removeSuffix("亿").toDouble()
                (num * 100_000_000).toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }

            else -> {
                value.toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        }
    } catch (e: Exception) {
        -1
    }
}
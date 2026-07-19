package dev.aaa1115910.bv.player

data class VideoPlayerOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val enableFfmpegAudioRenderer: Boolean,
    val enableSoftwareVideoDecoder: Boolean,
    val enableFrameRateMatching: Boolean = true
)

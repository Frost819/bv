package dev.aaa1115910.biliapi.entity.reply

import bilibili.main.community.reply.v1.Mode

enum class CommentSort {
    Hot,
    HotAndTime,
    Time;

    internal fun toGrpcMode(): Mode = when (this) {
        Hot -> Mode.MAIN_LIST_HOT
        HotAndTime -> Mode.DEFAULT
        Time -> Mode.MAIN_LIST_TIME
    }
}
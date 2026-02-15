package dev.aaa1115910.biliapi.entity.reply

/**
 * gRPC Cursor 分页（reply.proto 版本使用 CursorReq/CursorReply）
 */
data class CommentPage(
    val next: Long = 0L,
    val prev: Long = 0L
)

data class CommentReplyPage(
    val next: Long = 0L,
    val prev: Long = 0L
)
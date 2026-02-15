package dev.aaa1115910.biliapi.repositories

import bilibili.main.community.reply.v1.DetailListScene
import bilibili.main.community.reply.v1.ReplyGrpcKt
import bilibili.main.community.reply.v1.cursorReq
import bilibili.main.community.reply.v1.detailListReq
import bilibili.main.community.reply.v1.mainListReq
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentRepliesData
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.reply.CommentsData
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import org.koin.core.annotation.Single

@Single
class CommentRepository(
    private val channelRepository: ChannelRepository
) {
    private val replyStub
        get() = runCatching {
            ReplyGrpcKt.ReplyCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    /**
     * 只支持“视频评论”（type=1，oid=aid）
     */
    suspend fun getVideoComments(
        aid: Long,
        sort: CommentSort = CommentSort.Hot,
        page: CommentPage = CommentPage()
    ): CommentsData {
        return runCatching {
            val stub = replyStub ?: throw IllegalStateException("Reply stub is not initialized")
            val reply = stub.mainList(
                mainListReq {
                    oid = aid
                    type = 1L
                    cursor = cursorReq {
                        next = page.next
                        prev = page.prev
                        mode = sort.toGrpcMode()
                    }
                }
            )
            CommentsData.fromMainListReply(reply)
        }.onFailure {
            handleGrpcException(it)
        }.getOrThrow()
    }

    /**
     * 只支持“视频评论回复”（type=1，oid=aid，root=rootRpid）
     */
    suspend fun getVideoCommentReplies(
        aid: Long,
        rootRpid: Long,
        sort: CommentSort = CommentSort.Hot,
        page: CommentReplyPage = CommentReplyPage()
    ): CommentRepliesData {
        return runCatching {
            val stub = replyStub ?: throw IllegalStateException("Reply stub is not initialized")
            val reply = stub.detailList(
                detailListReq {
                    oid = aid
                    type = 1L
                    root = rootRpid
                    rpid = 0L
                    cursor = cursorReq {
                        next = page.next
                        prev = page.prev
                        mode = sort.toGrpcMode()
                    }
                    scene = DetailListScene.REPLY
                }
            )
            CommentRepliesData.fromDetailListReply(reply)
        }.onFailure {
            handleGrpcException(it)
        }.getOrThrow()
    }
}
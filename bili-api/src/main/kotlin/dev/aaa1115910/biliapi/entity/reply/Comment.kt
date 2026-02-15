package dev.aaa1115910.biliapi.entity.reply

import bilibili.main.community.reply.v1.ReplyInfo

data class CommentsData(
    val comments: List<Comment> = emptyList(),
    val nextPage: CommentPage = CommentPage(),
    val hasNext: Boolean
) {
    companion object {
        fun fromMainListReply(
            reply: bilibili.main.community.reply.v1.MainListReply
        ): CommentsData {
            // 置顶：优先使用 MainListReply 的专用字段；其次兼容 reply_control 里的置顶标记
            val rawPinned = buildList {
                if (reply.upTop.id != 0L) add(reply.upTop)
                if (reply.adminTop.id != 0L) add(reply.adminTop)
                if (reply.voteTop.id != 0L) add(reply.voteTop)
                addAll(reply.topRepliesList)
            }.distinctBy { it.id }

            val pinnedIds = rawPinned.map { it.id }.toHashSet()

            val pinnedComments = rawPinned.map { Comment.fromReplyInfo(it, forcePinned = true) }
            val normalComments = reply.repliesList
                .asSequence()
                .filter { it.id !in pinnedIds }
                .map { info ->
                    val pinnedByFlag =
                        info.replyControl.isUpTop || info.replyControl.isAdminTop || info.replyControl.isVoteTop
                    Comment.fromReplyInfo(info, forcePinned = pinnedByFlag)
                }
                .toList()

            return CommentsData(
                comments = pinnedComments + normalComments,
                nextPage = CommentPage(
                    next = reply.cursor.next,
                    prev = reply.cursor.prev
                ),
                hasNext = !reply.cursor.isEnd
            )
        }
    }
}

data class CommentRepliesData(
    val rootComment: Comment,
    val replies: List<Comment>,
    val nextPage: CommentReplyPage = CommentReplyPage(),
    val hasNext: Boolean
) {
    companion object {
        fun fromDetailListReply(
            reply: bilibili.main.community.reply.v1.DetailListReply
        ): CommentRepliesData {
            return CommentRepliesData(
                rootComment = Comment.fromReplyInfo(reply.root, forcePinned = false),
                replies = reply.root.repliesList.map { Comment.fromReplyInfo(it, forcePinned = false) },
                nextPage = CommentReplyPage(
                    next = reply.cursor.next,
                    prev = reply.cursor.prev
                ),
                hasNext = !reply.cursor.isEnd
            )
        }
    }
}

data class Comment(
    val rpid: Long,
    val mid: Long,
    val oid: Long,
    val type: Long,
    val parent: Long,
    val message: String,
    val member: Member,
    val timeDesc: String,
    val like: Long,
    val repliesCount: Int,
    val isPinned: Boolean
) {
    companion object {
        fun fromReplyInfo(info: ReplyInfo, forcePinned: Boolean): Comment {
            val pinnedByFlag =
                info.replyControl.isUpTop || info.replyControl.isAdminTop || info.replyControl.isVoteTop
            return Comment(
                rpid = info.id,
                mid = info.mid,
                oid = info.oid,
                type = info.type,
                parent = info.parent,
                message = info.content.message,
                member = Member(
                    mid = info.mid,
                    avatar = info.member.face,
                    name = info.member.name
                ),
                timeDesc = info.replyControl.timeDesc,
                like = info.like,
                repliesCount = info.count.toInt(),
                isPinned = forcePinned || pinnedByFlag
            )
        }
    }

    data class Member(
        val mid: Long,
        val avatar: String,
        val name: String
    )
}
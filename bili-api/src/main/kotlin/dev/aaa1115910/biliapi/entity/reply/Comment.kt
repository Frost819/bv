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
    val messageParts: List<MessagePart> = emptyList(),
    val pictures: List<Picture> = emptyList(),
    val member: Member,
    val timeDesc: String,
    val like: Long,
    val repliesCount: Int,
    val isPinned: Boolean
) {
    companion object {
        // 保守匹配：必须是 " 空格 + @ + 内容 + 空格 "
        private val MentionRegex = Regex(" @(.+?) ")

        fun fromReplyInfo(info: ReplyInfo, forcePinned: Boolean): Comment {
            val pinnedByFlag =
                info.replyControl.isUpTop || info.replyControl.isAdminTop || info.replyControl.isVoteTop

            val emoteMap = info.content.emoteMap.mapValues { (_, emote) ->
                EmoteResource(
                    url = emote.url,
                    text = emote.text
                )
            }
            val messageParts = parseMessageParts(
                message = info.content.message,
                emoteMap = emoteMap
            )
            val pictures = info.content.picturesList.map {
                Picture(
                    imgSrc = it.imgSrc,
                    imgWidth = it.imgWidth,
                    imgHeight = it.imgHeight,
                    imgSize = it.imgSize
                )
            }

            return Comment(
                rpid = info.id,
                mid = info.mid,
                oid = info.oid,
                type = info.type,
                parent = info.parent,
                message = info.content.message,
                messageParts = messageParts,
                pictures = pictures,
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

        private fun parseMessageParts(
            message: String,
            emoteMap: Map<String, EmoteResource>
        ): List<MessagePart> {
            if (message.isEmpty()) return emptyList()
            if (emoteMap.isEmpty()) return splitTextByMention(message)

            val emoteCodes = emoteMap.keys
                .asSequence()
                .filter { it.isNotEmpty() }
                .sortedByDescending { it.length }
                .toList()
            if (emoteCodes.isEmpty()) return splitTextByMention(message)

            val result = mutableListOf<MessagePart>()
            val textBuffer = StringBuilder()
            var cursor = 0

            fun flushTextBuffer() {
                if (textBuffer.isEmpty()) return
                result += splitTextByMention(textBuffer.toString())
                textBuffer.clear()
            }

            while (cursor < message.length) {
                val matchedCode = emoteCodes.firstOrNull { code ->
                    message.startsWith(code, startIndex = cursor)
                }
                if (matchedCode != null) {
                    val emote = emoteMap[matchedCode]
                    if (emote != null && emote.url.isNotBlank()) {
                        flushTextBuffer()
                        result += MessagePart.Emote(
                            code = matchedCode,
                            url = emote.url,
                            alt = emote.text.ifBlank { matchedCode }
                        )
                        cursor += matchedCode.length
                        continue
                    }
                }

                textBuffer.append(message[cursor])
                cursor += 1
            }

            flushTextBuffer()
            return result
        }

        private fun splitTextByMention(text: String): List<MessagePart.Text> {
            if (text.isEmpty()) return emptyList()

            val result = mutableListOf<MessagePart.Text>()
            var cursor = 0
            val matches = MentionRegex.findAll(text)
            for (m in matches) {
                val start = m.range.first
                val end = m.range.last + 1
                if (start > cursor) {
                    result += MessagePart.Text(text.substring(cursor, start))
                }

                val mentionBody = m.groups[1]?.value.orEmpty()
                result += MessagePart.Text(" ")
                result += MessagePart.Text("@$mentionBody", isMention = true)
                result += MessagePart.Text(" ")

                cursor = end
            }

            if (cursor < text.length) {
                result += MessagePart.Text(text.substring(cursor))
            }
            return result
        }
    }

    data class Member(
        val mid: Long,
        val avatar: String,
        val name: String
    )

    data class Picture(
        val imgSrc: String,
        val imgWidth: Double,
        val imgHeight: Double,
        val imgSize: Double
    )

    sealed class MessagePart {
        data class Text(
            val text: String,
            val isMention: Boolean = false
        ) : MessagePart()

        data class Emote(
            val code: String,
            val url: String,
            val alt: String
        ) : MessagePart()
    }

    private data class EmoteResource(
        val url: String,
        val text: String
    )
}
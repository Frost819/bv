package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.EmoteSize
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.focusedBorder

/**
 * 评论列表项组件
 *
 * @param comment 评论数据
 * @param modifier 修饰符
 * @param onClick 点击回调
 */
@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .focusedBorder(MaterialTheme.shapes.small),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
            pressedScale = 1f
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 主评论
            CommentMainContent(comment = comment)

            // 回复数量提示
            if (comment.replies.isNotEmpty()) {
                Text(
                    text = "${comment.repliesCount} 条回复 >>",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * 主评论内容
 */
@Composable
private fun CommentMainContent(
    comment: Comment
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 用户头像
        AsyncImage(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            model = comment.member.avatar,
            contentDescription = null,
        )

        // 评论内容
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 用户名
            Text(
                text = comment.member.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 评论内容（支持表情）
            CommentContent(
                content = comment.content,
                emotes = comment.emotes,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 底部信息：时间和点赞数
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间
                Text(
                    text = comment.timeDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // 点赞数
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        imageVector = androidx.compose.material.icons.Icons.Rounded.ThumbUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatLikeCount(comment.like),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // 回复数（当没有显示子评论时才显示）
                if (comment.replies.isEmpty() && comment.repliesCount > 0) {
                    Text(
                        text = "${comment.repliesCount} 回复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 评论内容组件，支持表情显示（富文本）
 */
@Composable
fun CommentContent(
    content: List<String>,
    emotes: List<Comment.Emote>,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val emoteNameList = emotes.map { it.text }
    val inlineContentMap = emotes.associateWith { emote ->
        InlineTextContent(
            Placeholder(
                width = emote.size.fontSize.sp,
                height = emote.size.fontSize.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                model = emote.url,
                contentDescription = null
            )
        }
    }.mapKeys { it.key.text }

    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            content.forEach { text ->
                if (emoteNameList.contains(text)) {
                    appendInlineContent(text)
                } else {
                    append(text)
                }
            }
        },
        inlineContent = inlineContentMap,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * 格式化点赞数
 */
private fun formatLikeCount(count: Long): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        else -> count.toString()
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CommentItemPreview() {
    BVTheme {
        CommentItem(
            comment = Comment(
                rpid = 123456,
                mid = 789,
                oid = 12345,
                type = 1,
                parent = 0,
                content = listOf("这是一条测试评论", "[2333]", "后面还有内容"),
                member = Comment.Member(
                    mid = 789,
                    avatar = "",
                    name = "测试用户"
                ),
                timeDesc = "2小时前",
                emotes = listOf(
                    Comment.Emote(
                        text = "[2333]",
                        url = "https://i0.hdslb.com/bfs/emote/4352e2396c13e4150786d48e464d517174845b9c.png",
                        size = EmoteSize.Small
                    )
                ),
                pictures = emptyList(),
                replies = emptyList(),
                repliesCount = 5,
                like = 12345L
            )
        )
    }
}

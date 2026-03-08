package dev.aaa1115910.bv.component.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

private val CommentsBg = Color(0xFFFBFBF4)
private val CommentsText = Color(0xFF000000)
private val UserNameColor = Color(0xFFFE7297)
private val MentionColor = Color(0xFF008DC3)

private data class MessageRenderData(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

private enum class Page { Main, Replies }
@Composable
fun VideoCommentsDialog(
    show: Boolean,
    aid: Long,
    onDismissRequest: () -> Unit
) {
    if (!show) return

    val commentRepository: CommentRepository = getKoin().get()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var gatePassed by remember(aid) { mutableStateOf(false) }
    var page by remember { mutableStateOf(Page.Main) }

    // --- 主评论 ---
    val comments = remember { mutableStateListOf<Comment>() }
    var commentPage by remember { mutableStateOf(CommentPage()) }
    var commentsHasNext by remember { mutableStateOf(true) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentsError by remember { mutableStateOf<String?>(null) }
    val commentsListState = rememberLazyListState()

    // 记录“主列表最后一次聚焦的评论 rpid”，用于从回复页返回时恢复焦点
    var lastMainFocusedRpid by remember { mutableStateOf<Long?>(null) }
    // 为每个主评论 item 维护 FocusRequester（rpid -> requester）
    val mainItemFocusRequesters = remember { mutableMapOf<Long, FocusRequester>() }

    // --- 回复页 ---
    var rootComment by remember { mutableStateOf<Comment?>(null) }
    val replies = remember { mutableStateListOf<Comment>() }
    var replyPage by remember { mutableStateOf(CommentReplyPage()) }
    var repliesHasNext by remember { mutableStateOf(true) }
    var repliesLoading by remember { mutableStateOf(false) }
    var repliesError by remember { mutableStateOf<String?>(null) }
    val repliesListState = rememberLazyListState()

    // 回复列表 item 焦点请求器（rpid -> requester）
    val replyItemFocusRequesters = remember { mutableMapOf<Long, FocusRequester>() }

    // 图片全屏预览：仅保存“当前评论图片集”
    var previewPictures by remember { mutableStateOf<List<Comment.Picture>>(emptyList()) }
    var previewIndex by remember { mutableIntStateOf(0) }

    fun openPreview(pictures: List<Comment.Picture>, index: Int) {
        if (pictures.isEmpty()) return
        previewPictures = pictures
        previewIndex = index.coerceIn(0, pictures.lastIndex)
    }

    fun closePreview() {
        previewPictures = emptyList()
        previewIndex = 0
    }

    fun switchPreview(delta: Int) {
        if (previewPictures.isEmpty()) return
        val size = previewPictures.size
        previewIndex = (previewIndex + delta + size) % size
    }

    // 根容器兜底焦点（无数据/异常时）
    val rootFocusRequester = remember { FocusRequester() }

    fun requestMainFocusRestore() {
        scope.launch {
            delay(40)
            val target = lastMainFocusedRpid
            val requester = target?.let { mainItemFocusRequesters[it] }
            if (requester != null) {
                runCatching { requester.requestFocus() }
                    .onFailure { runCatching { rootFocusRequester.requestFocus() } }
            } else {
                // 没记录到就兜底：列表第一个（如果有）
                val first = comments.firstOrNull()?.rpid
                val firstReq = first?.let { mainItemFocusRequesters[it] }
                if (firstReq != null) runCatching { firstReq.requestFocus() }
                else runCatching { rootFocusRequester.requestFocus() }
            }
        }
    }

    fun goBackLayer(): Boolean {
        if (previewPictures.isNotEmpty()) {
            closePreview()
            return true
        }

        return if (page == Page.Replies) {
            page = Page.Main
            rootComment = null

            replies.clear()
            replyPage = CommentReplyPage()
            repliesHasNext = true
            repliesError = null
            replyItemFocusRequesters.clear()

            requestMainFocusRestore()
            true
        } else {
            onDismissRequest()
            true
        }
    }

    fun loadComments(reset: Boolean) {
        scope.launch {
            if (commentsLoading) return@launch
            commentsLoading = true
            commentsError = null
            try {
                val pageToUse = if (reset) CommentPage() else commentPage
                val data = commentRepository.getVideoComments(
                    aid = aid,
                    sort = CommentSort.Hot,
                    page = pageToUse
                )
                if (reset) comments.clear()
                comments.addAll(data.comments)
                commentPage = data.nextPage
                commentsHasNext = data.hasNext
            } catch (e: Throwable) {
                commentsError = e.message ?: "加载失败"
            } finally {
                commentsLoading = false
            }
        }
    }

    fun loadReplies(reset: Boolean) {
        val root = rootComment ?: return
        scope.launch {
            if (repliesLoading) return@launch
            repliesLoading = true
            repliesError = null
            try {
                val pageToUse = if (reset) CommentReplyPage() else replyPage
                val data = commentRepository.getVideoCommentReplies(
                    aid = aid,
                    rootRpid = root.rpid,
                    sort = CommentSort.Hot,
                    page = pageToUse
                )
                if (reset) replies.clear()
                replies.addAll(data.replies)
                replyPage = data.nextPage
                repliesHasNext = data.hasNext
            } catch (e: Throwable) {
                repliesError = e.message ?: "加载失败"
            } finally {
                repliesLoading = false
            }
        }
    }

    // 打开时：先 gate（无评论/网络错误就 toast 并关闭），通过后再初始化 UI 状态
    LaunchedEffect(aid) {
        gatePassed = false

        page = Page.Main
        rootComment = null

        comments.clear()
        commentPage = CommentPage()
        commentsHasNext = true
        commentsLoading = false
        commentsError = null

        replies.clear()
        replyPage = CommentReplyPage()
        repliesHasNext = true
        repliesLoading = false
        repliesError = null
        replyItemFocusRequesters.clear()

        closePreview()

        val firstPage = runCatching {
            commentRepository.getVideoComments(
                aid = aid,
                sort = CommentSort.Hot,
                page = CommentPage()
            )
        }.getOrElse {
            "网络错误，请稍后重试".toast(context)
            onDismissRequest()
            return@LaunchedEffect
        }

        if (firstPage.comments.isEmpty()) {
            "暂无评论".toast(context)
            onDismissRequest()
            return@LaunchedEffect
        }

        comments.addAll(firstPage.comments)
        commentPage = firstPage.nextPage
        commentsHasNext = firstPage.hasNext

        gatePassed = true

        // 兜底给根容器焦点（等列表 item 创建后再把焦点交给 item）
        delay(40)
        runCatching { rootFocusRequester.requestFocus() }
    }

    // 主列表有数据时：如果没有记录过 lastMainFocusedRpid，就默认记录第一条，并尝试聚焦它
    LaunchedEffect(page, comments.size) {
        if (page == Page.Main && comments.isNotEmpty()) {
            if (lastMainFocusedRpid == null) {
                lastMainFocusedRpid = comments.first().rpid
            }
            requestMainFocusRestore()
        }
    }

    // 回复列表有数据后，聚焦第一个回复正文
    LaunchedEffect(page, replies.size) {
        if (page == Page.Replies && replies.isNotEmpty()) {
            scope.launch {
                delay(50)
                val firstRpid = replies.firstOrNull()?.rpid
                val requester = firstRpid?.let { replyItemFocusRequesters[it] }
                if (requester != null) runCatching { requester.requestFocus() }
                else runCatching { rootFocusRequester.requestFocus() }
            }
        }
    }

    // 懒加载：接近底部加载下一页
    val commentsNearBottom by remember {
        derivedStateOf {
            val lastVisible = commentsListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (comments.size - 3).coerceAtLeast(0)
        }
    }
    LaunchedEffect(commentsNearBottom, commentsHasNext, commentsLoading, page) {
        if (page == Page.Main && commentsNearBottom && commentsHasNext && !commentsLoading && comments.isNotEmpty()) {
            loadComments(reset = false)
        }
    }

    val repliesNearBottom by remember {
        derivedStateOf {
            val lastVisible = repliesListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (replies.size - 3).coerceAtLeast(0)
        }
    }
    LaunchedEffect(repliesNearBottom, repliesHasNext, repliesLoading, page) {
        if (page == Page.Replies && repliesNearBottom && repliesHasNext && !repliesLoading && replies.isNotEmpty()) {
            loadReplies(reset = false)
        }
    }

    if (gatePassed) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(rootFocusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Back) {
                            goBackLayer()
                            true
                        } else false
                    },
                shape = RoundedCornerShape(0.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = CommentsBg,
                    contentColor = CommentsText
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (page) {
                        Page.Main -> {
                            if (commentsError != null) {
                                InlineErrorText(text = commentsError ?: "加载失败")
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = commentsListState,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(comments, key = { _, c -> c.rpid }) { index, comment ->
                                    val bodyFr =
                                        mainItemFocusRequesters.getOrPut(comment.rpid) { FocusRequester() }
                                    val prevBodyFr = comments
                                        .getOrNull(index - 1)
                                        ?.let { mainItemFocusRequesters.getOrPut(it.rpid) { FocusRequester() } }
                                    val nextBodyFr = comments
                                        .getOrNull(index + 1)
                                        ?.let { mainItemFocusRequesters.getOrPut(it.rpid) { FocusRequester() } }

                                    LightCommentItem(
                                        modifier = Modifier.onFocusChanged {
                                            if (it.hasFocus) {
                                                lastMainFocusedRpid = comment.rpid
                                            }
                                        },
                                        bodyFocusRequester = bodyFr,
                                        previousBodyFocusRequester = prevBodyFr,
                                        nextBodyFocusRequester = nextBodyFr,
                                        comment = comment,
                                        showRepliesHint = comment.repliesCount > 0,
                                        onClick = {
                                            if (comment.repliesCount <= 0) return@LightCommentItem

                                            // 进入回复页前，明确记录“从哪条评论进入”
                                            lastMainFocusedRpid = comment.rpid

                                            rootComment = comment
                                            page = Page.Replies

                                            replies.clear()
                                            replyPage = CommentReplyPage()
                                            repliesHasNext = true
                                            repliesError = null
                                            replyItemFocusRequesters.clear()
                                            loadReplies(reset = true)
                                        },
                                        onImageClick = { imgIndex ->
                                            openPreview(comment.pictures, imgIndex)
                                        }
                                    )
                                }

                                item {
                                    BottomStateLight(
                                        loading = commentsLoading,
                                        hasNext = commentsHasNext,
                                        empty = comments.isEmpty(),
                                        emptyText = "暂无评论"
                                    )
                                }
                            }
                        }

                        Page.Replies -> {
                            val root = rootComment
                            if (root == null) {
                                Text(
                                    text = "未选择根评论",
                                    color = CommentsText.copy(alpha = 0.70f),
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RootCommentHeader(comment = root)

                                    if (repliesError != null) {
                                        InlineErrorText(text = repliesError ?: "加载失败")
                                    }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state = repliesListState,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        itemsIndexed(
                                            replies,
                                            key = { _, c -> c.rpid }
                                        ) { index, reply ->
                                            val bodyFr =
                                                replyItemFocusRequesters.getOrPut(reply.rpid) { FocusRequester() }
                                            val prevBodyFr = replies
                                                .getOrNull(index - 1)
                                                ?.let { replyItemFocusRequesters.getOrPut(it.rpid) { FocusRequester() } }
                                            val nextBodyFr = replies
                                                .getOrNull(index + 1)
                                                ?.let { replyItemFocusRequesters.getOrPut(it.rpid) { FocusRequester() } }

                                            LightCommentItem(
                                                bodyFocusRequester = bodyFr,
                                                previousBodyFocusRequester = prevBodyFr,
                                                nextBodyFocusRequester = nextBodyFr,
                                                comment = reply,
                                                showRepliesHint = false,
                                                onClick = {},
                                                onImageClick = { imgIndex ->
                                                    openPreview(reply.pictures, imgIndex)
                                                }
                                            )
                                        }

                                        item {
                                            BottomStateLight(
                                                loading = repliesLoading,
                                                hasNext = repliesHasNext,
                                                empty = replies.isEmpty(),
                                                emptyText = "暂无回复"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (previewPictures.isNotEmpty()) {
            CommentImagePreviewDialog(
                pictures = previewPictures,
                currentIndex = previewIndex,
                onDismissRequest = { closePreview() },
                onSwitch = { delta -> switchPreview(delta) }
            )
        }
    }
}

@Composable
private fun RootCommentHeader(comment: Comment) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Black.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.member.name,
                    color = UserNameColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (comment.isPinned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PinnedBadgeLight()
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = comment.timeDesc,
                    color = CommentsText.copy(alpha = 0.70f),
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "赞 ${comment.like}",
                    color = CommentsText.copy(alpha = 0.70f),
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CommentMessageText(comment = comment)

            if (comment.pictures.isNotEmpty()) {
                RootCommentPictures(pictures = comment.pictures)
            }
        }
    }
}

@Composable
private fun LightCommentItem(
    modifier: Modifier = Modifier,
    bodyFocusRequester: FocusRequester,
    previousBodyFocusRequester: FocusRequester? = null,
    nextBodyFocusRequester: FocusRequester? = null,
    comment: Comment,
    showRepliesHint: Boolean,
    onClick: () -> Unit,
    onImageClick: (Int) -> Unit
) {
    val pictures = comment.pictures
    val pictureFocusRequesters = remember(comment.rpid, pictures.size) {
        List(pictures.size) { FocusRequester() }
    }
    var bodyHasFocus by remember(comment.rpid) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(bodyFocusRequester)
                .focusProperties {
                    previousBodyFocusRequester?.let { up = it }
                    if (pictures.isNotEmpty()) {
                        down = pictureFocusRequesters.first()
                    } else {
                        nextBodyFocusRequester?.let { down = it }
                    }
                }
                .onFocusChanged { bodyHasFocus = it.hasFocus }
                .border(
                    width = 3.dp,
                    color = if (bodyHasFocus) UserNameColor else Color.Transparent,
                    shape = MaterialTheme.shapes.medium
                ),
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent
            ),
            scale = ClickableSurfaceDefaults.scale(
                focusedScale = 1f,
                pressedScale = 1f
            ),
            enabled = true,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.06f)),
                    model = comment.member.avatar,
                    contentDescription = null
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = comment.member.name,
                                color = UserNameColor,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (comment.isPinned) {
                                PinnedBadgeLight()
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = comment.timeDesc,
                            color = CommentsText.copy(alpha = 0.70f),
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "赞 ${comment.like}",
                            color = CommentsText.copy(alpha = 0.70f),
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    CommentMessageText(comment = comment)

                    if (showRepliesHint) {
                        Text(
                            text = "${comment.repliesCount} 条回复 >>",
                            color = CommentsText.copy(alpha = 0.85f),
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        if (pictures.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(pictures, key = { index, p -> "${comment.rpid}-${p.imgSrc}-$index" }) { index, picture ->
                    val fr = pictureFocusRequesters[index]
                    val upRequester = if (index == 0) bodyFocusRequester else pictureFocusRequesters[index - 1]
                    val downRequester = if (index == pictures.lastIndex) {
                        nextBodyFocusRequester ?: FocusRequester.Default
                    } else {
                        pictureFocusRequesters[index + 1]
                    }
                    var pictureHasFocus by remember(comment.rpid, index) { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .width(184.dp)
                            .height(112.dp)
                            .focusRequester(fr)
                            .focusProperties {
                                up = upRequester
                                down = downRequester
                            }
                            .onFocusChanged { pictureHasFocus = it.hasFocus }
                            .border(
                                width = if (pictureHasFocus) 3.dp else 0.dp,
                                color = if (pictureHasFocus) UserNameColor else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = CommentsBg,
                            focusedContainerColor = CommentsBg,
                            pressedContainerColor = CommentsBg
                        ),
                        scale = ClickableSurfaceDefaults.scale(
                            focusedScale = 1f,
                            pressedScale = 1f
                        ),
                        enabled = true,
                        onClick = { onImageClick(index) }
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CommentsBg),
                            model = picture.imgSrc,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentMessageText(comment: Comment) {
    val renderData = remember(comment.rpid, comment.messageParts, comment.message) {
        buildMessageRenderData(comment)
    }

    BasicText(
        text = renderData.text,
        inlineContent = renderData.inlineContent,
        style = TextStyle(
            color = CommentsText,
            fontSize = 24.sp,
            lineHeight = 28.sp
        ),
        overflow = TextOverflow.Ellipsis
    )
}

private fun buildMessageRenderData(comment: Comment): MessageRenderData {
    val parts = comment.messageParts.ifEmpty {
        listOf(Comment.MessagePart.Text(comment.message))
    }

    val inlineContent = linkedMapOf<String, InlineTextContent>()
    var emoteIndex = 0
    val text = buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is Comment.MessagePart.Text -> {
                    if (part.isMention) {
                        withStyle(SpanStyle(color = MentionColor, fontWeight = FontWeight.Medium)) {
                            append(part.text)
                        }
                    } else {
                        append(part.text)
                    }
                }

                is Comment.MessagePart.Emote -> {
                    val id = "comment_emote_$emoteIndex"
                    appendInlineContent(id, part.alt.ifBlank { part.code })
                    inlineContent[id] = InlineTextContent(
                        Placeholder(
                            width = 1.1.em,
                            height = 1.1.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                            model = part.url,
                            contentDescription = part.alt,
                            contentScale = ContentScale.Fit
                        )
                    }
                    emoteIndex += 1
                }
            }
        }
    }
    return MessageRenderData(text = text, inlineContent = inlineContent)
}

@Composable
private fun RootCommentPictures(pictures: List<Comment.Picture>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(pictures, key = { index, p -> "root-${p.imgSrc}-$index" }) { _, picture ->
            Box(
                modifier = Modifier
                    .width(184.dp)
                    .height(112.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(CommentsBg)
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CommentsBg),
                    model = picture.imgSrc,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
}

@Composable
private fun CommentImagePreviewDialog(
    pictures: List<Comment.Picture>,
    currentIndex: Int,
    onDismissRequest: () -> Unit,
    onSwitch: (delta: Int) -> Unit
) {
    if (pictures.isEmpty()) return
    val safeIndex = currentIndex.coerceIn(0, pictures.lastIndex)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(pictures, safeIndex) {
        delay(20)
        runCatching { focusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (it.key) {
                        Key.Back -> {
                            onDismissRequest()
                            true
                        }

                        Key.DirectionLeft -> {
                            onSwitch(-1)
                            true
                        }

                        Key.DirectionRight -> {
                            onSwitch(1)
                            true
                        }

                        else -> false
                    }
                },
            shape = RoundedCornerShape(0.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = CommentsBg,
                contentColor = CommentsText
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CommentsBg)
            ) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = pictures[safeIndex].imgSrc,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
}

@Composable
private fun PinnedBadgeLight() {
    Text(
        text = "置顶",
        color = UserNameColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .border(
                width = 1.dp,                    // 边框粗细
                color = UserNameColor,           // 边框颜色与文字一致
                shape = RoundedCornerShape(3.dp) // 图片中的小圆角
            )
            .padding(horizontal = 4.dp, vertical = 1.dp) // 文字与边框之间的间距
    )
}

@Composable
private fun InlineErrorText(text: String) {
    Text(
        text = text,
        color = Color(0xFFB00020),
        fontSize = 20.sp,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
    )
}

@Composable
private fun BottomStateLight(
    loading: Boolean,
    hasNext: Boolean,
    empty: Boolean,
    emptyText: String
) {
    val t = when {
        loading -> "加载中……"
        empty -> emptyText
        hasNext -> "继续下滑加载更多……"
        else -> "没有了"
    }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        text = t,
        color = CommentsText.copy(alpha = 0.60f),
        fontSize = 18.sp
    )
}
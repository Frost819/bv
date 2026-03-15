package dev.aaa1115910.bv.tv.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.focusedBorder
import dev.aaa1115910.bv.util.isDpadDown
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.onBackPressed
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

/**
 * 子评论浮窗组件
 *
 * @param show 是否显示浮窗
 * @param oid 视频 ID
 * @param rootId 根评论 ID
 * @param rootComment 根评论数据
 * @param onHide 关闭回调
 */
@Composable
fun SubCommentPanel(
    show: Boolean,
    oid: Long,
    rootId: Long,
    rootComment: Comment,
    onHide: () -> Unit
) {
    val commentRepository: CommentRepository = getKoin().get()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    val replies = remember { mutableStateListOf<Comment>() }
    var focusedCommentIndex by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(CommentReplyPage()) }
    var hasNext by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 加载子评论
    val loadReplies: (Boolean) -> Unit = { reset ->
        scope.launch {
            if (loading) return@launch
            loading = true
            error = null

            try {
                val page = if (reset) CommentReplyPage() else currentPage
                val data = commentRepository.getCommentReplies(
                    rpid = rootId,
                    type = 1L,
                    commentId = oid,
                    page = page,
                    preferApiType = Prefs.apiType
                )

                if (reset) {
                    replies.clear()
                    replies.addAll(data.replies)
                } else {
                    replies.addAll(data.replies)
                }

                currentPage = data.nextPage
                hasNext = data.hasNext
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
            } finally {
                loading = false
            }
        }
    }

    // 显示时加载第一页
    LaunchedEffect(show, rootId) {
        if (show && replies.isEmpty()) {
            loadReplies(true)
        }
    }

    // 显示后请求焦点
    LaunchedEffect(show, replies.isNotEmpty()) {
        if (show && replies.isNotEmpty()) {
            delay(300)
            focusRequester.requestFocus(scope)
        }
    }

    // 懒加载
    val isAtBottom by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == replies.size - 1
        }
    }
    LaunchedEffect(isAtBottom, hasNext, loading) {
        if (isAtBottom && hasNext && !loading && replies.isNotEmpty()) {
            loadReplies(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onHide),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .widthIn(min = 300.dp, max = 400.dp)
                    .fillMaxWidth(0.3f)
                    .clickable(enabled = true, onClick = {})
                    .onBackPressed { onHide() },
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 根评论（只读显示，右键展开/收起）
                    SubCommentRootItem(comment = rootComment)

                    // 分隔线
                    Divider(
                        color = Color.White.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // 子评论列表
                    if (error != null) {
                        Text(
                            text = error ?: "加载失败",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (replies.isEmpty() && !loading) {
                        Text(
                            text = "暂无回复",
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { event ->
                                    when {
                                        // 左键返回顶部
                                        event.isKeyDown() && event.key == Key.DirectionLeft -> {
                                            scope.launch {
                                                listState.scrollToItem(0)
                                                delay(100)
                                                focusRequester.requestFocus(scope)
                                            }
                                            true
                                        }
                                        // 下键：逐步滚动，仅在完全可见时转移焦点
                                        event.isKeyDown() && event.isDpadDown() -> {
                                            val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.index == focusedCommentIndex }

                                            if (currentItemInfo != null) {
                                                val viewportEnd = listState.layoutInfo.viewportEndOffset
                                                val itemBottom = currentItemInfo.offset + currentItemInfo.size

                                                // 如果评论底部不可见，逐步滚动
                                                if (itemBottom > viewportEnd) {
                                                    scope.launch {
                                                        // 每次滚动约 150dp
                                                        val scrollAmount = with(density) { 150.dp.toPx() }
                                                        listState.animateScrollBy(scrollAmount)
                                                    }
                                                    true // 拦截事件，不允许焦点转移
                                                } else {
                                                    false // 评论已完全可见，允许焦点转移
                                                }
                                            } else {
                                                false
                                            }
                                        }
                                        else -> false
                                    }
                                },
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = replies,
                                key = { index, it -> "$index-reply-${it.rpid}" }
                            ) { index, reply ->
                                SubCommentItem(
                                    comment = reply,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.hasFocus) {
                                                focusedCommentIndex = index
                                            }
                                        }
                                )
                            }

                            if (loading) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        LoadingTip()
                                    }
                                }
                            }

                            if (!hasNext && replies.isNotEmpty()) {
                                item {
                                    Text(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        text = "没有更多了",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // 底部提示
                    Spacer(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

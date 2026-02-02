package dev.aaa1115910.bv.tv.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.Section
import dev.aaa1115910.biliapi.repositories.CommentRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.focusedBorder
import dev.aaa1115910.bv.util.isDpadDown
import dev.aaa1115910.bv.util.isDpadLeft
import dev.aaa1115910.bv.util.isKeyDown
import dev.aaa1115910.bv.util.onBackPressed
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity

/**
 * 评论浮层组件
 *
 * @param show 是否显示浮层
 * @param oid 视频 aid
 * @param onHide 关闭浮层回调
 * @param episodes 正片剧集列表（用于选集切换）
 * @param sections 章节选集列表（用于选集切换）
 * @param initialEpisodeId 初始选中的剧集ID
 * @param onEpisodeChange 剧集切换回调
 */
@Composable
fun CommentPanel(
    show: Boolean,
    oid: Long,
    onHide: () -> Unit,
    episodes: List<Episode> = emptyList(),
    sections: List<Section> = emptyList(),
    initialEpisodeId: Int = -1,
    onEpisodeChange: ((Episode) -> Unit)? = null
) {
    val commentRepository: CommentRepository = getKoin().get()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val sidebarFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    val comments = remember { mutableStateListOf<Comment>() }
    var loading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(CommentPage()) }
    var hasNext by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 子评论浮窗状态
    var showSubCommentPanel by remember { mutableStateOf(false) }
    var selectedRootComment by remember { mutableStateOf<Comment?>(null) }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    var wasSubCommentPanelShown by remember { mutableStateOf(false) }
    var selectedCommentIndex by remember { mutableStateOf(0) }
    var focusedCommentIndex by remember { mutableStateOf(0) }

    // 选集相关状态
    var currentEpisode by remember { mutableStateOf<Episode?>(null) }
    var focusOnSidebar by remember { mutableStateOf(false) }
    var scrollToCurrentEpisode by remember { mutableStateOf(false) }
    var pendingFocusToComments by remember { mutableStateOf(false) }

    // 合并所有剧集（正片 + 章节）
    val allEpisodeItems by remember(episodes, sections) {
        derivedStateOf {
            buildList {
                var idx = 0
                // 添加正片剧集
                if (episodes.isNotEmpty()) {
                    episodes.forEach { ep ->
                        // 过滤掉 aid 为 0 的剧集
                        if (ep.aid > 0) {
                            add(EpisodeItem(ep, "正片", idx++))
                        }
                    }
                }
                // 添加章节剧集
                sections.forEach { section ->
                    section.episodes.forEach { ep ->
                        // 过滤掉 aid 为 0 的剧集
                        if (ep.aid > 0) {
                            add(EpisodeItem(ep, section.title, idx++))
                        }
                    }
                }
            }
        }
    }

    // 是否显示侧边栏（多于1集时显示）
    val showSidebar by remember(allEpisodeItems) {
        derivedStateOf { allEpisodeItems.size > 1 }
    }

    // 初始化当前选中的剧集
    LaunchedEffect(allEpisodeItems, initialEpisodeId) {
        if (currentEpisode == null && allEpisodeItems.isNotEmpty()) {
            currentEpisode = if (initialEpisodeId != -1) {
                allEpisodeItems.find { it.episode.id == initialEpisodeId }?.episode
            } else {
                allEpisodeItems.firstOrNull()?.episode
            }
        }
    }

    // 获取当前要加载评论的 aid
    val currentOid by remember(currentEpisode, oid) {
        derivedStateOf { currentEpisode?.aid ?: oid }
    }

    // 判断是否滚动到底部
    val isAtBottom by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == comments.size - 1
        }
    }

    // 加载评论
    val loadComments: (reset: Boolean) -> Unit = { reset ->
        scope.launch {
            if (loading) return@launch
            loading = true
            error = null

            try {
                val page = if (reset) CommentPage() else currentPage
                val data = commentRepository.getComments(
                    id = currentOid,
                    type = 1L, // 视频评论
                    sort = CommentSort.Hot,
                    page = page,
                    preferApiType = Prefs.apiType
                )

                if (reset) {
                    comments.clear()
                    comments.addAll(data.comments)
                } else {
                    comments.addAll(data.comments)
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

    // 显示时加载评论
    LaunchedEffect(show, currentOid) {
        if (show && comments.isEmpty()) {
            loadComments(true)
        }
        if (!show) {
            hasRequestedFocus = false
            wasSubCommentPanelShown = false
            // 重置边栏焦点状态，确保下次打开时焦点在评论列表
            focusOnSidebar = false
            scrollToCurrentEpisode = false
            pendingFocusToComments = false
        }
    }

    // 显示后请求焦点（初次显示时或子评论浮窗关闭后）
    LaunchedEffect(show, showSubCommentPanel, comments.isNotEmpty(), loading) {
        if (show && !showSubCommentPanel) {
            // 子评论浮窗刚关闭，需要恢复焦点到之前点击的评论
            if (wasSubCommentPanelShown) {
                delay(300) // 等待动画完成
                listState.scrollToItem(selectedCommentIndex)
                delay(100)
                focusRequester.requestFocus(scope)
                wasSubCommentPanelShown = false
            }
            // 切换剧集后评论加载完成，请求焦点到评论列表
            else if (pendingFocusToComments) {
                delay(100) // 等待渲染完成
                if (!loading && comments.isNotEmpty()) {
                    delay(100) // 等待渲染完成
                    focusRequester.requestFocus(scope)
                    pendingFocusToComments = false
                }
            }
            // 初次显示父评论浮窗，请求焦点
            else if (!hasRequestedFocus) {
                delay(200) // 等待请求完成
                if(!loading){
                    delay(200) // 等待动画完成
                    focusRequester.requestFocus(scope)
                    hasRequestedFocus = true
                }
            }
        }
        // 记录子评论浮窗显示状态
        if (showSubCommentPanel) {
            wasSubCommentPanelShown = true
        }
    }

    // 懒加载：滚动到底部时加载更多
    LaunchedEffect(isAtBottom, hasNext, loading) {
        if (isAtBottom && hasNext && !loading && comments.isNotEmpty()) {
            loadComments(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onHide),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show && !showSubCommentPanel,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .widthIn(
                        min = if (showSidebar) 600.dp else 300.dp,
                        max = if (showSidebar) 700.dp else 400.dp
                    )
                    .fillMaxWidth(if (showSidebar) 0.5f else 0.3f)
                    .clickable(enabled = true, onClick = {}) // 阻止点击穿透
                    .onBackPressed {
                        if (focusOnSidebar && showSidebar) {
                            // 在边栏时按返回键关闭评论面板
                            onHide()
                        } else if (showSidebar) {
                            // 在评论列表时按返回键切换到边栏
                            focusOnSidebar = true
                            scrollToCurrentEpisode = true
                            // 焦点请求由 EpisodeSidebar 内部的 LaunchedEffect 处理
                        } else {
                            // 没有边栏时直接关闭
                            onHide()
                        }
                    },
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 左侧边栏 - 仅在有多个剧集时显示
                    if (showSidebar) {
                        EpisodeSidebar(
                            episodes = allEpisodeItems,
                            currentEpisode = currentEpisode,
                            onEpisodeSelected = { episodeItem ->
                                currentEpisode = episodeItem.episode
                                onEpisodeChange?.invoke(episodeItem.episode)
                                comments.clear()
                                loadComments(true)
                                // 切换剧集后将焦点移回评论列表
                                focusOnSidebar = false
                                scrollToCurrentEpisode = false
                                // 标记需要在评论加载完成后请求焦点
                                pendingFocusToComments = true
                            },
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight(),
                            focusRequester = sidebarFocusRequester,
                            onFocusMoved = {
                                // 焦点返回评论列表
                                focusOnSidebar = false
                                scrollToCurrentEpisode = false
                                scope.launch {
                                    focusRequester.requestFocus(scope)
                                }
                            },
                            scrollToCurrent = scrollToCurrentEpisode
                        )
                    }

                    // 右侧评论列表区域
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 标题栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "评论",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                // 操作提示
                                Text(
                                    text = if (showSidebar) "左键返回顶部,返回键切换剧集" else "左键返回顶部",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                // 显示当前剧集
                                if (currentEpisode != null && showSidebar) {
                                    Text(
                                        text = generateEpisodeTitle(currentEpisode, ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                text = if (comments.isNotEmpty()) "${comments.size} 条" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // 评论列表
                        if (error != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .focusable(),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Text(
                                    text = error ?: "加载失败",
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else if (comments.isEmpty() && !loading) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .focusable(),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Text(
                                    text = "暂无评论",
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { event ->
                                        when {
                                            // 左键返回顶部
                                            event.isKeyDown() && event.isDpadLeft() -> {
                                                scope.launch {
                                                    listState.scrollToItem(0)
                                                    // 滚动后重新请求焦点，使焦点移到第一条评论
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
                                items = comments,
                                key = { _, it -> it.rpid }
                            ) { index, comment ->
                                CommentItem(
                                    comment = comment,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.hasFocus) {
                                                focusedCommentIndex = index
                                            }
                                        },
                                    onClick = {
                                        // 只有有子评论时才能点击打开子评论浮窗
                                        if (comment.repliesCount > 0) {
                                            selectedCommentIndex = index
                                            selectedRootComment = comment
                                            showSubCommentPanel = true
                                        }
                                    }
                                )
                            }

                            // 加载状态
                            if (loading) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        LoadingTip()
                                    }
                                }
                            }

                            // 没有更多了
                            if (!hasNext && comments.isNotEmpty()) {
                                item {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        text = "没有更多评论了",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // 底部提示
                    Spacer(modifier = Modifier.height(8.dp))
                } // Column (右侧评论列表区域) 结束
            } // Row 结束
        } // Surface 结束
        } // AnimatedVisibility 结束
    } // Box 结束

    // 子评论浮窗
    if (selectedRootComment != null) {
        SubCommentPanel(
            show = showSubCommentPanel,
            oid = currentOid,
            rootId = selectedRootComment!!.rpid,
            rootComment = selectedRootComment!!,
            onHide = {
                showSubCommentPanel = false
                selectedRootComment = null
            }
        )
    }
}

/**
 * 选集侧边栏项数据类
 */
private data class EpisodeItem(
    val episode: Episode,
    val sectionTitle: String,
    val index: Int
)

/**
 * 生成剧集标题
 */
private fun generateEpisodeTitle(
    episode: Episode?,
    sectionTitle: String
): String {
    if (episode == null) return ""

    return if (episode.longTitle.isNotEmpty()) {
        runCatching {
            "第 ${episode.title.toInt()} 集 "
        }.getOrDefault("") + episode.longTitle
    } else if (sectionTitle == "正片") {
        runCatching {
            "第 ${episode.title.toInt()} 集"
        }.getOrDefault(episode.title)
    } else {
        episode.title
    }
}

/**
 * 选集侧边栏组件
 */
@Composable
private fun EpisodeSidebar(
    episodes: List<EpisodeItem>,
    currentEpisode: Episode?,
    onEpisodeSelected: (EpisodeItem) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    onFocusMoved: () -> Unit = {},
    scrollToCurrent: Boolean = false
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 为每个剧集项创建独立的 FocusRequester
    val itemFocusRequesters = remember(episodes.size) {
        List(episodes.size) { FocusRequester() }
    }

    // 计算在 LazyColumn 中的实际索引（考虑章节标题）
    fun calculateLazyColumnIndex(episodes: List<EpisodeItem>, episodeIndex: Int): Int {
        if (episodeIndex < 0 || episodes.isEmpty()) return 0
        var actualIndex = 0
        var lastSectionTitle = ""
        // 遍历到目标索引之前的所有项
        for (i in 0 until episodeIndex) {
            if (episodes[i].sectionTitle != lastSectionTitle) {
                lastSectionTitle = episodes[i].sectionTitle
                actualIndex++  // 章节标题占一个索引
            }
            actualIndex++  // 剧集项占一个索引
        }
        // 检查目标项本身是否有新章节标题
        if (episodes[episodeIndex].sectionTitle != lastSectionTitle) {
            actualIndex++  // 目标项的章节标题
        }
        return actualIndex  // 返回剧集项的正确索引
    }

    // 初始化时滚动到当前选中的剧集（只滚动，不请求焦点）
    LaunchedEffect(currentEpisode, episodes) {
        val index = episodes.indexOfFirst { it.episode.id == currentEpisode?.id }
        if (index >= 0) {
            val actualIndex = calculateLazyColumnIndex(episodes, index)
            listState.scrollToItem(maxOf(0, actualIndex - 2))
        }
    }

    // 当 scrollToCurrent 变为 true 时，直接请求焦点到当前剧集（位置已由初始化 LaunchedEffect 处理）
    LaunchedEffect(scrollToCurrent) {
        if (scrollToCurrent) {
            delay(50) // 短暂等待确保布局就绪
            val index = episodes.indexOfFirst { it.episode.id == currentEpisode?.id }
            if (index >= 0) {
                // 直接请求焦点到当前选中的剧集项
                itemFocusRequesters.getOrNull(index)?.requestFocus()
            } else {
                // 没有找到当前剧集，焦点到第一个剧集
                itemFocusRequesters.firstOrNull()?.requestFocus()
            }
        }
    }

    LazyColumn(
        modifier = modifier.focusRequester(focusRequester),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // 按章节分组显示
        var lastSectionTitle = ""

        episodes.forEachIndexed { index, item ->
            // 章节标题
            if (item.sectionTitle != lastSectionTitle) {
                lastSectionTitle = item.sectionTitle
                item {
                    Text(
                        text = item.sectionTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 剧集按钮
            item {
                val isSelected = item.episode.id == currentEpisode?.id
                EpisodeSidebarItem(
                    episode = item.episode,
                    sectionTitle = item.sectionTitle,
                    isSelected = isSelected,
                    onClick = { onEpisodeSelected(item) },
                    onBackKeyPressed = onFocusMoved,
                    focusRequester = itemFocusRequesters[index]
                )
            }
        }
    }
}

/**
 * 选集侧边栏单项组件
 */
@Composable
private fun EpisodeSidebarItem(
    episode: Episode,
    sectionTitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onBackKeyPressed: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val borderColor = if (isSelected) Color(0xFFE39B17) else null
    val context = LocalContext.current

    Surface(
        modifier = Modifier.focusRequester(focusRequester),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) {
                Color.White.copy(alpha = 0.15f)
            } else {
                Color.Transparent
            },
            focusedContainerColor = if (isSelected) {
                Color.White.copy(alpha = 0.15f)
            } else {
                Color.Transparent
            }
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f
        ),
        border = ClickableSurfaceDefaults.border(
            border = borderColor?.let {
                Border(border = BorderStroke(width = 2.dp, color = it))
            } ?: Border.None,
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.small
            )
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 剧集封面缩略图
            AsyncImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                model = ImageRequest.Builder(context)
                    .data(episode.cover)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            // 剧集标题
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = generateEpisodeTitle(episode, sectionTitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.longTitle.isNotEmpty()) {
                    Text(
                        text = episode.longTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

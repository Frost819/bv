package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.entity.VideoListItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    active: Boolean = show,
    currentAid: Long,
    currentCid: Long,
    videoList: List<VideoListItem>,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onEnsureUgcPagesLoaded: (aid: Long) -> Unit,
) {
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    var ensureParentVisibleJob by remember { mutableStateOf<Job?>(null) }
    var focusedParentCid by remember { mutableStateOf<Long?>(null) }

    var didInitialPosition by remember { mutableStateOf(false) }
    var pendingFocusCid by remember { mutableStateOf<Long?>(null) }
    var pendingPrefetchAid by remember { mutableStateOf<Long?>(null) }

    // 仅在“打开时”标记一次“待聚焦 cid”：
    // 列表打开后不再因为 currentCid 的变化抢焦点
    LaunchedEffect(show, active) {
        if (show && active) {
            pendingFocusCid = currentCid
        } else {
            pendingFocusCid = null
            if (!show) {
                pendingPrefetchAid = null
                didInitialPosition = false
            }
        }
    }

    // 推导 pinnedParent：
    // 优先用 currentAid 锁定“当前父项”（即使当前 pages 还没加载）
    // 其次再用 cid 在父项/子项里兜底
    val pinnedParent = remember(videoList, currentAid, currentCid) {
        videoList.firstOrNull { it.aid == currentAid }
            ?: videoList.firstOrNull { it.cid == currentCid }
            ?: videoList.firstOrNull { it.ugcPages?.any { p -> p.cid == currentCid } == true }
    }

    /**
     * 打开时：
     * - 预取 pinnedParent 的子项数据（番剧集跳过）
     * - 初次定位到当前播放项
     *
     * 关键修复：两段式定位 + offset
     * 因为你这里父项可能三行、且展开/收起会改变高度；只滚一次很容易“滚到旧布局位置”。
     */
    LaunchedEffect(show, active, videoList.size) {
        if (!show) return@LaunchedEffect
        if (didInitialPosition) return@LaunchedEffect
        if (videoList.isEmpty()) return@LaunchedEffect

        // 兜底：UGC 多P常见场景 videoList 只有 1 个父项，但 ugcPages 尚未加载且 currentCid 是子项
        val probableParent = pinnedParent ?: videoList.singleOrNull()

        if (probableParent != null && probableParent.epid == null) {
            onEnsureUgcPagesLoaded(probableParent.aid)
        }

        val targetIndex = videoList.indexOfFirst { v ->
            v.aid == currentAid ||
                    v.cid == currentCid ||
                    (v.ugcPages?.any { p -> p.cid == currentCid } == true)
        }
        if (targetIndex != -1) {
            // 第一段：瞬移到 item（避免大列表长动画）
            listState.scrollToItem(targetIndex)

            // 等一帧，让首轮布局稳定（特别是三行标题 / animateContentSize 带来的变化）
            kotlinx.coroutines.android.awaitFrame()

            // 第二段：带 offset 再定位一次，让目标尽量不要贴边（尾部尤其需要）
            // offset 取值你可以按实际屏幕再调：-80 / -120 等
            listState.scrollToItem(targetIndex, scrollOffset = -80)
        }

        didInitialPosition = true
    }

    LaunchedEffect(show, active, focusedParentCid, videoList.size) {
        if (!show) return@LaunchedEffect
        val cid = focusedParentCid ?: return@LaunchedEffect

        val index = videoList.indexOfFirst { it.cid == cid }
        if (index < 0) return@LaunchedEffect

        // 等一帧拿稳定的 layoutInfo
        kotlinx.coroutines.android.awaitFrame()
        // 焦点已经变了就别滚旧的
        if (focusedParentCid != cid) return@LaunchedEffect

        val layout = listState.layoutInfo
        val viewportStart = layout.viewportStartOffset
        val viewportEnd = layout.viewportEndOffset

        val info = layout.visibleItemsInfo.firstOrNull { it.index == index }
        val itemStart = info?.offset
        val itemEnd = info?.let { it.offset + it.size }

        // visibleItemsInfo 里“有”不代表在屏幕内，必须用 offset/size 与 viewport 相交判断
        val actuallyVisible =
            (itemStart != null && itemEnd != null && itemEnd > viewportStart && itemStart < viewportEnd)

        if (!actuallyVisible) {
            // 两段式瞬移：先拉进来，再做 offset 微调（尾部更稳）
            listState.scrollToItem(index)
            kotlinx.coroutines.android.awaitFrame()
            if (focusedParentCid != cid) return@LaunchedEffect
            listState.scrollToItem(index, scrollOffset = -80)
        }
    }

    /**
     * 焦点移动时的“延迟预取”：避免焦点快速上下移动导致请求风暴
     */
    LaunchedEffect(show, pendingPrefetchAid) {
        if (!active) return@LaunchedEffect
        if (!show) return@LaunchedEffect
        val aid = pendingPrefetchAid ?: return@LaunchedEffect

        delay(200)

        if (pendingPrefetchAid == aid) {
            onEnsureUgcPagesLoaded(aid)
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Surface(
            modifier = modifier,
            colors = SurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 60.dp)
                ) {
                    items(
                        items = videoList,
                        key = { it.cid }
                    ) { video ->

                        val isParentSelected = video.cid == currentCid
                        val isSeasonEpisode = video.epid != null
                        val enableChildrenUi = !isSeasonEpisode

                        // 三态：null=未加载/失败，empty=已加载但单P，notEmpty=多P
                        val pages = if (enableChildrenUi) video.ugcPages else null
                        val pagesLoaded = pages != null
                        val hasSubPages = pages?.isNotEmpty() == true

                        val isCurrentParent = video.aid == currentAid
                        val isChildSelected = enableChildrenUi && (pages?.any { it.cid == currentCid } == true)
                        val isPinned = pinnedParent?.aid == video.aid || isChildSelected || isParentSelected

                        var expanded by remember(video.cid) { mutableStateOf(false) }
                        var didAutoExpand by remember(video.cid) { mutableStateOf(false) }

                        // pinnedParent：只自动展开一次（用于初次定位/初次进入当前组），之后可以手动收起
                        LaunchedEffect(show, isPinned) {
                            if (!active) return@LaunchedEffect
                            if (!show) return@LaunchedEffect
                            if (isPinned && !didAutoExpand) {
                                expanded = true
                                didAutoExpand = true
                            }
                        }

                        LaunchedEffect(pagesLoaded, hasSubPages) {
                            // 确认单P：不需要展开
                            if (pagesLoaded && !hasSubPages) expanded = false
                        }

                        // 组焦点跟踪（保留你原来的折叠策略）
                        var groupHasFocus by remember(video.cid) { mutableStateOf(false) }
                        var collapseToken by remember(video.cid) { mutableIntStateOf(0) }

                        fun scheduleCollapseIfNeeded() {
                            if (isPinned) return
                            collapseToken++
                        }

                        LaunchedEffect(collapseToken) {
                            if (collapseToken > 0) {
                                kotlinx.coroutines.android.awaitFrame()
                                if (!groupHasFocus && !isPinned) expanded = false
                            }
                        }

                        // 每个父项独立 requester（不要复用）
                        val parentFocusRequester = remember(video.cid) { FocusRequester() }
                        val parentBringIntoViewRequester = remember(video.cid) { androidx.compose.foundation.relocation.BringIntoViewRequester() }

                        Column(
                            modifier = Modifier.animateContentSize()
                        ) {
                            /**
                             * 关键改动：
                             * bringIntoView 不再跟随 onFocusChanged，而是只在“pendingFocusCid 消耗那次”触发。
                             *
                             * 同时做强硬兜底：
                             * 1) 先把对应父项滚到可见位置（按 index）
                             * 2) 再 requestFocus
                             * 3) 再 bringIntoView
                             */
                            LaunchedEffect(show, pendingFocusCid) {
                                if (!active) return@LaunchedEffect
                                if (!show) return@LaunchedEffect

                                val wantCid = pendingFocusCid ?: return@LaunchedEffect

                                // 只有当“当前要聚焦的 cid”确实属于这个父项（父项自身 cid 或者它的子项 cid）才处理
                                val shouldHandleThisGroup =
                                    (video.cid == wantCid) || (enableChildrenUi && (video.ugcPages?.any { it.cid == wantCid } == true))

                                if (!shouldHandleThisGroup) return@LaunchedEffect

                                // 如果想聚焦的是子项，但子项 UI 还没展开/还没加载，这里尽力让它出现
                                val wantChild = enableChildrenUi && (video.ugcPages?.any { it.cid == wantCid } == true)

                                if (wantChild) {
                                    // 子项要存在：先展开
                                    expanded = true
                                }

                                // 先滚父项到可见（即使最终焦点是子项，也先把父项滚进来，保证后续布局/测量稳定）
                                val parentIndex = videoList.indexOfFirst { it.cid == video.cid }
                                if (parentIndex != -1) {
                                    listState.scrollToItem(parentIndex, scrollOffset = -80)
                                    kotlinx.coroutines.android.awaitFrame()
                                }

                                // 如果 pendingFocusCid 指向父项自身：聚焦父项
                                if (video.cid == wantCid) {
                                    parentFocusRequester.requestFocus()
                                    kotlinx.coroutines.android.awaitFrame()
                                    // bring into view 再兜底一次（只在这次消耗触发）
                                    kotlinx.coroutines.coroutineScope {
                                        launch { parentBringIntoViewRequester.bringIntoView() }
                                    }
                                    pendingFocusCid = null
                                    return@LaunchedEffect
                                }

                                // 走到这里表示想聚焦子项
                                // 但注意：如果 ugcPages 还没加载出来（pagesLoaded=false），此时子项节点不存在，无法 requestFocus
                                // 这里策略：触发加载 + 先聚焦父项（保证焦点可见），等加载完成后你 currentCid 仍会对应子项，再由下面子项逻辑消费
                                if (!pagesLoaded) {
                                    onEnsureUgcPagesLoaded(video.aid)

                                    // 先给父项焦点，至少不要“焦点在屏幕外”
                                    parentFocusRequester.requestFocus()
                                    kotlinx.coroutines.android.awaitFrame()
                                    kotlinx.coroutines.coroutineScope {
                                        launch { parentBringIntoViewRequester.bringIntoView() }
                                    }
                                    // 注意：这里不清 pendingFocusCid，让它等 pagesLoaded 后由子项消耗
                                    return@LaunchedEffect
                                }

                                // pagesLoaded=true 才能真正对子项 requestFocus（见下方子项 LaunchedEffect）
                                // 这里不做任何事，让子项那边消费 pendingFocusCid
                            }

                            DenseListItem(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(parentFocusRequester)
                                    .bringIntoViewRequester(parentBringIntoViewRequester)
                                    .onFocusChanged { state ->
                                        if (state.hasFocus) {
                                            groupHasFocus = true
                                            pendingPrefetchAid = if (enableChildrenUi) video.aid else null

                                            // 仅当父项已经在屏幕外时，才瞬移滚动把它带回可视区。
                                            focusedParentCid = video.cid
                                            ensureParentVisibleJob?.cancel()
                                            ensureParentVisibleJob = scope.launch {
                                                if (!show) return@launch

                                                // 等一帧，确保 listState.layoutInfo 是稳定的
                                                kotlinx.coroutines.android.awaitFrame()
                                                if (focusedParentCid != video.cid) return@launch

                                                val index = videoList.indexOfFirst { it.cid == video.cid }
                                                if (index < 0) return@launch

                                                val layout = listState.layoutInfo
                                                val viewportStart = layout.viewportStartOffset
                                                val viewportEnd = layout.viewportEndOffset

                                                val info = layout.visibleItemsInfo.firstOrNull { it.index == index }
                                                val itemStart = info?.offset
                                                val itemEnd = info?.let { it.offset + it.size }

                                                // visibleItemsInfo 里“有”不代表在屏幕内，必须用 offset/size 与 viewport 相交判断
                                                val actuallyVisible =
                                                    (itemStart != null && itemEnd != null && itemEnd > viewportStart && itemStart < viewportEnd)

                                                if (!actuallyVisible) {
                                                    listState.scrollToItem(index, scrollOffset = -80)
                                                    kotlinx.coroutines.android.awaitFrame()

                                                    // 只在 off-screen 时才 bringIntoView，避免日常移动焦点时干扰滚动
                                                    if (focusedParentCid == video.cid) {
                                                        parentBringIntoViewRequester.bringIntoView()
                                                    }
                                                }
                                            }
                                        } else {
                                            groupHasFocus = false
                                            if (pendingPrefetchAid == video.aid) pendingPrefetchAid = null
                                            scheduleCollapseIfNeeded()

                                            if (focusedParentCid == video.cid) focusedParentCid = null
                                        }
                                    },
                                selected = isParentSelected && !isChildSelected,
                                onClick = {
                                    if (!enableChildrenUi) {
                                        if (!isCurrentParent) onPlayNewVideo(video)
                                        return@DenseListItem
                                    }

                                    if (pagesLoaded && !hasSubPages) {
                                        if (!isCurrentParent) onPlayNewVideo(video)
                                        return@DenseListItem
                                    }

                                    if (!pagesLoaded) {
                                        if (!isCurrentParent) {
                                            onPlayNewVideo(video)
                                        } else {
                                            expanded = !expanded
                                            if (expanded) onEnsureUgcPagesLoaded(video.aid)
                                        }
                                        return@DenseListItem
                                    }

                                    expanded = !expanded
                                },
                                headlineContent = {
                                    Text(
                                        text = video.title,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingContent = {
                                    if (hasSubPages) {
                                        Icon(
                                            imageVector = if (expanded)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            )

                            // 子项未加载：仅在“当前父项 + expanded=true”时展示“加载中...”占位（不可聚焦/不可点击）
                            if (expanded && enableChildrenUi && !pagesLoaded && isCurrentParent) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 16.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MenuListItem(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .focusProperties { canFocus = false },
                                        text = "加载中……",
                                        selected = false,
                                        textAlign = TextAlign.Start,
                                        onClick = {}
                                    )
                                }
                            }

                            // 分P子项（仅展开时显示）
                            if (expanded && hasSubPages) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 16.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    video.ugcPages?.forEach { page ->
                                        key(page.cid) {
                                            val isPageSelected = page.cid == currentCid

                                            val childFocusRequester = remember(page.cid) { FocusRequester() }
                                            val childBringIntoViewRequester = remember(page.cid) { androidx.compose.foundation.relocation.BringIntoViewRequester() }

                                            /**
                                             * 子项：只在 pendingFocusCid 指向它时请求焦点 + bringIntoView（并消耗 pendingFocusCid）
                                             * 这样 bringIntoView 只发生一次，不会在上下移动焦点时干扰滚动。
                                             */
                                            LaunchedEffect(show, pendingFocusCid, expanded, pagesLoaded) {
                                                if (!active) return@LaunchedEffect
                                                if (!show) return@LaunchedEffect
                                                if (!expanded) return@LaunchedEffect
                                                if (!pagesLoaded) return@LaunchedEffect

                                                val wantCid = pendingFocusCid ?: return@LaunchedEffect
                                                if (wantCid != page.cid) return@LaunchedEffect

                                                // 强硬顺序：先把父项滚进来（上面父项 effect 已做过；这里再兜底一次也行）
                                                val parentIndex = videoList.indexOfFirst { it.cid == video.cid }
                                                if (parentIndex != -1) {
                                                    listState.scrollToItem(parentIndex, scrollOffset = -80)
                                                    kotlinx.coroutines.android.awaitFrame()
                                                }

                                                // 请求焦点到子项
                                                childFocusRequester.requestFocus()
                                                kotlinx.coroutines.android.awaitFrame()

                                                // bring into view 兜底（仅这一次）
                                                kotlinx.coroutines.coroutineScope {
                                                    launch { childBringIntoViewRequester.bringIntoView() }
                                                }

                                                // 消耗掉 pendingFocusCid，防止后续 currentCid 变化抢焦点/重复滚动
                                                pendingFocusCid = null
                                            }

                                            MenuListItem(
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp)
                                                    .focusRequester(childFocusRequester)
                                                    .bringIntoViewRequester(childBringIntoViewRequester),
                                                text = page.title,
                                                selected = isPageSelected,
                                                textAlign = TextAlign.Start,
                                                onFocus = {
                                                    groupHasFocus = true
                                                }
                                            ) {
                                                if (!isPageSelected) {
                                                    onPlayNewVideo(video.copy(cid = page.cid))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 如果折叠子项，确保焦点回到父项
                            LaunchedEffect(expanded) {
                                if (!expanded && isParentSelected) {
                                    parentFocusRequester.requestFocus()
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

@Composable
fun MenuListItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    textAlign: TextAlign = TextAlign.Center,
    onFocus: () -> Unit = {},
    onClick: () -> Unit
) {
    DenseListItem(
        modifier = modifier
            .onFocusChanged { if (it.hasFocus) onFocus() },
        selected = selected,
        onClick = onClick,
        headlineContent = {
            Text(
                text = text,
                textAlign = textAlign
            )
        }
    )
}
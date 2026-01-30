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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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

@Composable
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    currentAid: Long,
    currentCid: Long,
    videoList: List<VideoListItem>,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onEnsureUgcPagesLoaded: (aid: Long) -> Unit,
) {
    val listState = rememberLazyListState()

    var pendingFocusCid by remember { mutableStateOf<Long?>(null) }

    // 每次打开时标记一次“待聚焦 cid”
    LaunchedEffect(show, currentCid) {
        if (show) pendingFocusCid = currentCid
    }

    // 推导 pinnedParent：
    // 优先用 currentAid 锁定“当前父项”（即使当前 pages 还没加载）
    // 其次再用 cid 在父项/子项里兜底
    val pinnedParent = remember(videoList, currentAid, currentCid) {
        videoList.firstOrNull { it.aid == currentAid }
            ?: videoList.firstOrNull { it.cid == currentCid }
            ?: videoList.firstOrNull { it.ugcPages?.any { p -> p.cid == currentCid } == true }
    }

    // 打开时：预取 pinnedParent 的子项数据（番剧集跳过）
    LaunchedEffect(show, pinnedParent, videoList) {
        if (!show) return@LaunchedEffect

        // 兜底：UGC 多P常见场景 videoList 只有 1 个父项，但 ugcPages 尚未加载且 currentCid 是子项
        val probableParent = pinnedParent ?: videoList.singleOrNull()

        if (probableParent != null && probableParent.epid == null) {
            onEnsureUgcPagesLoaded(probableParent.aid)
        }

        val targetIndex = videoList.indexOfFirst { v ->
            v.aid == currentAid || v.cid == currentCid || (v.ugcPages?.any { p -> p.cid == currentCid } == true)
        }
        if (targetIndex != -1) {
            // 大列表避免长动画：先瞬移，再可选短动画微调
            listState.scrollToItem(targetIndex)
        }
    }

    val parentFocusRequester = remember { FocusRequester() }
    val childFocusRequester = remember { FocusRequester() }

    // 自动定位到当前分P
    /*
    LaunchedEffect(show) {
        if (show) {
            val currentIndex = videoList.indexOfFirst { video ->
                video.cid == currentCid ||
                        video.ugcPages?.any { it.cid == currentCid } == true
            }

            if (currentIndex != -1) {
                listState.animateScrollToItem(currentIndex)

                val isChild = videoList
                    .getOrNull(currentIndex)
                    ?.ugcPages
                    ?.any { it.cid == currentCid } == true

                if (isChild) {
                    childFocusRequester.requestFocus()
                } else {
                    parentFocusRequester.requestFocus()
                }
            }
        }
    }
    */

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
                        /*
                        val hasSubPages = !video.ugcPages.isNullOrEmpty()
                        val isChildSelected = video.ugcPages?.any { it.cid == currentCid } == true

                        var expanded by remember(video.cid) {
                            mutableStateOf(isChildSelected)
                        }

                        // 如果当前正在播放的是子项，则自动展开父项
                        LaunchedEffect(isChildSelected) {
                            if (isChildSelected) expanded = true
                        }
                        */
                        val isSeasonEpisode = video.epid != null
                        val enableChildrenUi = !isSeasonEpisode

                        // 三态：null=未加载/失败，empty=已加载但单P，notEmpty=多P
                        val pages = if (enableChildrenUi) video.ugcPages else null
                        val pagesLoaded = pages != null
                        val hasSubPages = pages?.isNotEmpty() == true

                        val isChildSelected = enableChildrenUi && (pages?.any { it.cid == currentCid } == true)
                        val isPinned = pinnedParent?.aid == video.aid || isChildSelected || isParentSelected

                        var expanded by remember(video.cid) { mutableStateOf(false) }

                        // pinnedParent：永远展开（命中父项或子项都成立）
                        LaunchedEffect(isPinned) {
                            if (isPinned) expanded = true
                        }

                        // 组焦点跟踪：父项或子项任意获得焦点 => 认为组聚焦
                        var groupHasFocus by remember(video.cid) { mutableStateOf(false) }
                        var collapseToken by remember(video.cid) { mutableStateOf(0) }

                        // 负责更新 Token（数字加 1）
                        fun scheduleCollapseIfNeeded() {
                            if (isPinned) return
                            collapseToken++
                        }

                        // 监听 Token 变化并执行逻辑
                        LaunchedEffect(collapseToken) {
                            // 只有当 token 变化且不为 0 时才执行
                            if (collapseToken > 0) {
                                kotlinx.coroutines.android.awaitFrame() // 等待一帧
                                if (!groupHasFocus && !isPinned) expanded = false
                            }
                        }

                        Column(
                            modifier = Modifier.animateContentSize()
                        ) {
                            // 视频父项
                            val parentModifier =
                                if (isParentSelected)
                                    Modifier.focusRequester(parentFocusRequester)
                                else Modifier

                            // 如果当前 cid 匹配父项，且不是因为子项匹配导致的（即纯粹是父项被选中），则请求焦点
                            LaunchedEffect(show, isParentSelected, pendingFocusCid) {
                                if (show && isParentSelected && pendingFocusCid == currentCid && !isChildSelected) {
                                    parentFocusRequester.requestFocus()
                                    pendingFocusCid = null // 消耗掉，防止重复请求
                                }
                            }

                            DenseListItem(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .onFocusChanged { state ->
                                        if (state.hasFocus) {
                                            groupHasFocus = true
                                            if (enableChildrenUi) onEnsureUgcPagesLoaded(video.aid) // 预取但不展开
                                        } else {
                                            groupHasFocus = false
                                            scheduleCollapseIfNeeded()
                                        }
                                    }
                                    .then(parentModifier),
                                selected = isParentSelected && !isChildSelected,
                                /*
                                onClick = {
                                    if (hasSubPages) {
                                        expanded = !expanded
                                    } else if (!isParentSelected) {
                                        onPlayNewVideo(video)
                                    }
                                },
                                 */
                                onClick = {
                                    // 1) UGC 子项未加载：先触发加载，并允许展开（展示“加载中”占位）
                                    // 2) 已加载且多P：切换展开/折叠（pinned 不允许折叠）
                                    // 3) 已加载但单P：走原有播放逻辑
                                    if (enableChildrenUi && !pagesLoaded) {
                                        onEnsureUgcPagesLoaded(video.aid)
                                        // 让用户看到“展开动作”生效；pinned 会被强制展开也没问题
                                        if (!isPinned) expanded = true
                                        return@DenseListItem
                                    }

                                    if (hasSubPages) {
                                        if (!isPinned) expanded = !expanded
                                    } else if (!isParentSelected) {
                                        onPlayNewVideo(video)
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = video.title,
                                        maxLines = 1,
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

                                            val childModifier =
                                                if (isPageSelected)
                                                    Modifier.focusRequester(childFocusRequester)
                                                else Modifier

                                            // 子项请求焦点逻辑
                                            LaunchedEffect(show, isPageSelected, pendingFocusCid) {
                                                if (show && isPageSelected && pendingFocusCid == currentCid) {
                                                    childFocusRequester.requestFocus()
                                                    pendingFocusCid = null
                                                }
                                            }

                                            MenuListItem(
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp)
                                                    .then(childModifier),
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
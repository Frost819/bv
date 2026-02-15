package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Toc
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.ui.state.PlayerChapter
import dev.aaa1115910.bv.ui.state.PlayerUiState

private enum class UpPanelTab(
    val title: String,
    val icon: ImageVector
) {
    Chapter("选择章节", Icons.Rounded.Toc),
    Video("选择视频", Icons.Rounded.VideoLibrary),
    Collection("选择合集", Icons.Rounded.Subscriptions)
}

private enum class UpPanelFocusState { Nav, Content }

@Composable
fun UpPanelController(
    modifier: Modifier = Modifier,
    show: Boolean,
    uiState: PlayerUiState,
    currentTimeMs: Long,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onGoTime: (timeMs: Long) -> Unit,
    onPlay: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onEnsureUgcPagesLoaded: (aid: Long) -> Unit,
) {
    val chaptersEnabled = uiState.availableChapters.isNotEmpty()
    val collectionsEnabled = (uiState.ugcSeason?.sections?.size ?: 0) > 1 // 严格按 sections.size

    var selectedTab by remember { mutableStateOf(UpPanelTab.Video) }
    var focusState by remember { mutableStateOf(UpPanelFocusState.Nav) }

    // 默认焦点落点：导航“选择视频”
    val navVideoItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            selectedTab = UpPanelTab.Video
            focusState = UpPanelFocusState.Nav
            // 等一帧再请求焦点，避免初次 composition 未稳定
            kotlinx.coroutines.android.awaitFrame()
            navVideoItemFocusRequester.requestFocus()
        }
    }

    // 当前 tab 不可用时，强制回到 Video
    LaunchedEffect(show, chaptersEnabled, collectionsEnabled) {
        if (!show) return@LaunchedEffect
        if (selectedTab == UpPanelTab.Chapter && !chaptersEnabled) selectedTab = UpPanelTab.Video
        if (selectedTab == UpPanelTab.Collection && !collectionsEnabled) selectedTab = UpPanelTab.Video
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedVisibility(
            visible = show,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Surface(
                modifier = Modifier.fillMaxHeight(),
                colors = SurfaceDefaults.colors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // 左侧导航
                    // 左侧导航
                    UpPanelNavList(
                        modifier = Modifier
                            .focusGroup()
                            .onFocusChanged { fs ->
                                if (fs.hasFocus) focusState = UpPanelFocusState.Nav
                            }
                            .onPreviewKeyEvent {
                                // 不再在按键时手动切状态，由 onFocusChanged 跟随真实焦点
                                if (it.type == KeyEventType.KeyUp) {
                                    if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) return@onPreviewKeyEvent false
                                    return@onPreviewKeyEvent true
                                }
                                false
                            },
                        selectedTab = selectedTab,
                        isExpanded = focusState == UpPanelFocusState.Nav,
                        chaptersEnabled = chaptersEnabled,
                        collectionsEnabled = collectionsEnabled,
                        navVideoItemFocusRequester = navVideoItemFocusRequester,
                        onSelectedChanged = { selectedTab = it }
                    )

                    // 右侧内容区：按 Left 回到导航
                    Box(
                        modifier = Modifier
                            .focusGroup()
                            .onFocusChanged { fs ->
                                if (fs.hasFocus) focusState = UpPanelFocusState.Content
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val contentActive = (focusState == UpPanelFocusState.Content)

                        when (selectedTab) {
                            UpPanelTab.Video -> {
                                // 不改 VideoListController.kt 的宽度实现；在这里用 modifier 约束整体宽度，避免填满剩余空间
                                VideoListController(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(300.dp),
                                    show = show,
                                    active = contentActive,
                                    currentAid = uiState.aid,
                                    currentCid = uiState.cid,
                                    videoList = uiState.availableVideoList,
                                    onEnsureUgcPagesLoaded = onEnsureUgcPagesLoaded,
                                    onPlayNewVideo = {
                                        onPlayNewVideo(it)
                                        onDismiss()
                                    }
                                )
                            }

                            UpPanelTab.Chapter -> {
                                if (!chaptersEnabled) return@Box

                                val built = buildChapterParents(
                                    chapters = uiState.availableChapters,
                                    currentTimeMs = currentTimeMs
                                )

                                CollectionListController(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .widthIn(min = 300.dp) ,
                                    show = show,
                                    active = contentActive,
                                    parents = built.parents,
                                    selectedParentKey = built.selectedParentKey,
                                    selectedChildKey = null,
                                    pinnedParentKey = built.selectedParentKey,
                                    enterFocusKey = built.selectedParentKey,
                                    autoExpandPinnedParent = false,
                                    onEnsureChildrenLoaded = {},
                                    onParentClick = { parent ->
                                        val chapter = parent.extra as? PlayerChapter ?: return@CollectionListController
                                        onGoTime(chapter.fromSec * 1000L)
                                        if (!isPlaying) onPlay()
                                        onDismiss()
                                    },
                                    onChildClick = { _, _ -> }
                                )
                            }

                            UpPanelTab.Collection -> {
                                if (!collectionsEnabled) return@Box

                                val built = buildCollectionParents(uiState)

                                CollectionListController(
                                    modifier = Modifier.fillMaxHeight(),
                                    show = show,
                                    active = contentActive,
                                    parents = built.parents,
                                    selectedParentKey = built.selectedParentKey,
                                    selectedChildKey = built.selectedChildKey,
                                    pinnedParentKey = built.selectedParentKey,
                                    onEnsureChildrenLoaded = {},
                                    onParentClick = { /* 父项点击展开/折叠由组件内部处理 */ },
                                    onChildClick = { _, child ->
                                        val target = child.extra as? VideoListItem ?: return@CollectionListController
                                        onPlayNewVideo(target)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpPanelNavList(
    modifier: Modifier = Modifier,
    selectedTab: UpPanelTab,
    isExpanded: Boolean,
    chaptersEnabled: Boolean,
    collectionsEnabled: Boolean,
    navVideoItemFocusRequester: FocusRequester,
    onSelectedChanged: (UpPanelTab) -> Unit,
) {
    val listFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        listFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = modifier
            .focusRequester(listFocusRequester)
            .focusRestorer(navVideoItemFocusRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(UpPanelTab.entries) { _, tab ->
            val enabled = when (tab) {
                UpPanelTab.Chapter -> chaptersEnabled
                UpPanelTab.Video -> true
                UpPanelTab.Collection -> collectionsEnabled
            }

            MenuListItem(
                modifier = Modifier
                    .ifElse(tab == UpPanelTab.Video, Modifier.focusRequester(navVideoItemFocusRequester))
                    .focusProperties { canFocus = enabled }
                    .alpha(if (enabled) 1f else 0.45f),
                text = tab.title,
                icon = tab.icon,
                expanded = isExpanded,
                selected = selectedTab == tab,
                onClick = {},
                onFocus = { if (enabled) onSelectedChanged(tab) }
            )
        }
    }
}

private data class BuiltChapterParents(
    val parents: List<CollectionParentItem>,
    val selectedParentKey: Long?
)

private fun buildChapterParents(
    chapters: List<PlayerChapter>,
    currentTimeMs: Long
): BuiltChapterParents {
    val currentSec = (currentTimeMs / 1000L).toInt()
    val selectedIndex = chapters.indexOfFirst { ch ->
        val from = ch.fromSec
        val to = ch.toSec
        if (to > 0) currentSec in from until to else currentSec >= from
    }.takeIf { it >= 0 }

    return BuiltChapterParents(
        parents = chapters.mapIndexed { index, ch ->
            CollectionParentItem(
                key = index.toLong(),
                title = ch.content,
                children = emptyList(),
                extra = ch
            )
        },
        selectedParentKey = selectedIndex?.toLong()
    )
}

private data class BuiltCollectionParents(
    val parents: List<CollectionParentItem>,
    val selectedParentKey: Long?,
    val selectedChildKey: Long?
)

private fun buildCollectionParents(uiState: PlayerUiState): BuiltCollectionParents {
    val sections = uiState.ugcSeason?.sections.orEmpty()
    val currentAid = uiState.aid

    val parents = sections.mapIndexed { sectionIndex, section ->
        CollectionParentItem(
            key = sectionIndex.toLong(),
            title = section.title,
            children = section.episodes.map { ep ->
                val video = VideoListItem(
                    aid = ep.aid,
                    cid = ep.cid,
                    epid = ep.epid,
                    seasonId = uiState.seasonId.takeIf { it != 0 },
                    title = ep.title,
                    ugcPages = null
                )
                CollectionChildItem(
                    key = ep.aid,
                    title = ep.title,
                    extra = video
                )
            }
        )
    }

    val selectedParentIndex = sections.indexOfFirst { s -> s.episodes.any { it.aid == currentAid } }
        .takeIf { it >= 0 }

    return BuiltCollectionParents(
        parents = parents,
        selectedParentKey = selectedParentIndex?.toLong(),
        selectedChildKey = currentAid.takeIf { it != 0L }
    )
}
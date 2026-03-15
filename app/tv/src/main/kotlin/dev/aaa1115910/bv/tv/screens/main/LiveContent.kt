package dev.aaa1115910.bv.tv.screens.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.component.live.LiveRoomCard
import dev.aaa1115910.bv.tv.util.blockDownFocusExitAtGridEnd
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.live.LiveMode
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import dev.aaa1115910.biliapi.entity.live.LiveAreaGroup
import dev.aaa1115910.bv.tv.util.ProvideListBringIntoViewSpec
import dev.aaa1115910.bv.tv.util.rememberTvLazyListFocusRestorer

// 固定 TopNavItem：推荐
private object RecommendNavItem : TopNavItem {
    override fun getDisplayName(context: Context): String = "推荐"
}

// 固定 TopNavItem：关注
private object FollowingNavItem : TopNavItem {
    override fun getDisplayName(context: Context): String = "关注"
}

// 主分区 TopNavItem
private data class ParentAreaNavItem(val group: LiveAreaGroup) : TopNavItem {
    override fun getDisplayName(context: Context): String = group.name
}

// 子分区 TopNavItem
private data class SubAreaNavItem(val area: LiveAreaItem) : TopNavItem {
    override fun getDisplayName(context: Context): String = area.name
}

@Composable
fun LiveContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    liveViewModel: LiveViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("LiveContent")
    val context = LocalContext.current
    
    val gridState = rememberLazyGridState()
    // 使用 MainScreen 传入的 FocusRequester 作为默认入口焦点（从侧边栏按右进入内容区）
    val parentNavFocusRequester = navFocusRequester
    val subNavFocusRequester = remember { FocusRequester() }
    val roomListFocusRestorer = rememberTvLazyListFocusRestorer()
    var focusOnContent by remember { mutableStateOf(false) }
    var parentNavHasFocus by remember { mutableStateOf(false) }
    var subNavHasFocus by remember { mutableStateOf(false) }

    val currentListOnTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }
    }

    // 监听焦点位置，触发分页加载
    val focusedIndex = liveViewModel.lastFocusedRoomIndex
    val totalItems = liveViewModel.roomList.size
    LaunchedEffect(focusedIndex, totalItems, liveViewModel.loading) {
        if ((totalItems < 10 || focusedIndex >= totalItems - 8) && liveViewModel.hasMore && !liveViewModel.loading) {
            logger.info { "Trigger load more, focusedIndex: $focusedIndex, totalItems: $totalItems" }
            liveViewModel.loadMore()
        }
    }

    LaunchedEffect(liveViewModel.roomList, liveViewModel.loading) {
        if (liveViewModel.roomList.isEmpty() && liveViewModel.loading) {
            liveViewModel.lastFocusedRoomIndex = 0
            gridState.scrollToItem(0)
        }
    }

    BackHandler(focusOnContent || subNavHasFocus || parentNavHasFocus) {
        logger.info { "onFocusBackToNav" }
        if (subNavHasFocus) {
            parentNavFocusRequester.requestFocus(scope)
            return@BackHandler
        }
        if (parentNavHasFocus) {
            drawerItemFocusRequesters[DrawerItem.Live]?.requestFocus()
            return@BackHandler
        }
        // 推荐/关注模式没有子分区栏，直接返回主分区栏
        if (liveViewModel.currentMode == LiveMode.AREA) {
            subNavFocusRequester.requestFocus(scope)
        } else {
            parentNavFocusRequester.requestFocus(scope)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            androidx.compose.foundation.layout.Column {
                // 第一行：推荐 + 关注 + 主分区
                val parentNavItems = remember(
                    liveViewModel.parentAreaGroups.size,
                    liveViewModel.isLoggedIn
                ) {
                    buildList<TopNavItem> {
                        add(RecommendNavItem)
                        if (liveViewModel.isLoggedIn) add(FollowingNavItem)
                        addAll(liveViewModel.parentAreaGroups.map { ParentAreaNavItem(it) })
                    }
                }

                val initialSelectedParent = remember(liveViewModel.currentMode, liveViewModel.currentParentGroup) {
                    when (liveViewModel.currentMode) {
                        LiveMode.RECOMMEND -> RecommendNavItem
                        LiveMode.FOLLOWING -> FollowingNavItem
                        LiveMode.AREA -> parentNavItems.firstOrNull {
                            it is ParentAreaNavItem && it.group.id == liveViewModel.currentParentGroup?.id
                        }
                    }
                }

                if (parentNavItems.isNotEmpty()) {
                    TopNav(
                        modifier = Modifier
                            .focusRequester(parentNavFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged { parentNavHasFocus = it.hasFocus },
                        items = parentNavItems,
                        isLargePadding = false,
                        initialSelectedItem = initialSelectedParent,
                        onSelectedChanged = { nav ->
                            liveViewModel.lastFocusedRoomIndex = 0
                            scope.launch { gridState.scrollToItem(0) }
                            when (nav) {
                                is RecommendNavItem -> liveViewModel.switchToRecommend()
                                is FollowingNavItem -> liveViewModel.switchToFollowing()
                                is ParentAreaNavItem -> liveViewModel.switchParentArea(nav.group)
                            }
                        },
                        onClick = { nav ->
                            val isSameSelection = when (nav) {
                                is RecommendNavItem -> liveViewModel.currentMode == LiveMode.RECOMMEND
                                is FollowingNavItem -> liveViewModel.currentMode == LiveMode.FOLLOWING
                                is ParentAreaNavItem -> liveViewModel.currentMode == LiveMode.AREA && nav.group.id == liveViewModel.currentParentGroup?.id
                                else -> false
                            }
                            if (isSameSelection) {
                                liveViewModel.lastFocusedRoomIndex = 0
                                liveViewModel.refresh()
                                scope.launch { gridState.scrollToItem(0) }
                            }
                        },
                        onLeftKeyEvent = {
                            drawerItemFocusRequesters[DrawerItem.Live]?.requestFocus()
                        }
                    )
                }
                
                // 第二行：子分区（仅在分区模式下显示）
                if (liveViewModel.currentMode == LiveMode.AREA && liveViewModel.subAreaList.isNotEmpty()) {
                    // 监听 currentParentGroup 变化以触发子分区列表更新
                    val subNavItems = remember(liveViewModel.currentParentGroup, liveViewModel.subAreaList.size) {
                        liveViewModel.subAreaList.map { SubAreaNavItem(it) }
                    }
                    TopNav(
                        modifier = Modifier
                            .focusRequester(subNavFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged { subNavHasFocus = it.hasFocus },
                        paddingTop = 4.dp,
                        items = subNavItems,
                        isLargePadding = !focusOnContent && currentListOnTop,
                        initialSelectedItem = subNavItems.firstOrNull { it.area.id == liveViewModel.currentSubArea?.id },
                        onSelectedChanged = { nav ->
                            (nav as? SubAreaNavItem)?.let {
                                liveViewModel.lastFocusedRoomIndex = 0
                                liveViewModel.switchSubArea(it.area)
                                scope.launch { gridState.scrollToItem(0) }
                            }
                        },
                        onClick = { nav ->
                            (nav as? SubAreaNavItem)?.let { item ->
                                if (item.area.id == liveViewModel.currentSubArea?.id) {
                                    liveViewModel.lastFocusedRoomIndex = 0
                                    liveViewModel.refresh()
                                    scope.launch { gridState.scrollToItem(0) }
                                }
                            }
                        },
                        onLeftKeyEvent = {
                            parentNavFocusRequester.requestFocus(scope)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
        ) {
            if (liveViewModel.roomList.isEmpty() && liveViewModel.loading) {
                Row(
                    modifier = Modifier.align(Alignment.Center)
                ){
                    LoadingTip()
                }
            } else {
                ProvideListBringIntoViewSpec(topPadding = 12.dp, bottomPadding = 28.dp) {
                    LazyVerticalGrid(
                        modifier = roomListFocusRestorer.containerModifier(
                            Modifier
                                .fillMaxSize()
                                .blockDownFocusExitAtGridEnd(
                                    currentIndex = focusedIndex,
                                    itemCount = totalItems,
                                    columnCount = 4
                                )
                        ),
                        state = gridState,
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        itemsIndexed(
                            items = liveViewModel.roomList,
                            key = { index, room -> "$index-room-${room.roomId}" }
                        ) { index, room ->
                            val entryCardModifier = roomListFocusRestorer.firstItemModifier(index)

                            LiveRoomCard(
                                modifier = entryCardModifier,
                                data = room,
                                onClick = {
                                    // 保存焦点位置
                                    liveViewModel.lastFocusedRoomIndex = index
                                    if (room.liveStatus != 1) {
                                        "${room.uname} 未开播".toast(context)
                                        return@LiveRoomCard
                                    }
                                    // 启动播放器
                                    VideoPlayerV3Activity.actionStartLive(
                                        context = context,
                                        roomId = room.roomId,
                                        title = room.title,
                                        upId = room.uid,
                                        upName = room.uname,
                                        upFace = room.face,
                                        watchedNum = room.watchedShow?.num ?: (room.online / 10)
                                    )
                                },
                                onFocus = {
                                    liveViewModel.lastFocusedRoomIndex = index
                                    logger.debug { "Focus on room ${room.roomId}" }
                                }
                            )
                        }

                        // 加载中提示
                        if (liveViewModel.loading) {
                            item {
                                LoadingTip()
                            }
                        }

                        // 没有更多了
                        if (!liveViewModel.hasMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier.offset(y = (-16).dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "没有更多内容了~",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

package dev.aaa1115910.bv.tv.screens.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.TopNav
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.component.live.LiveRoomCard
import dev.aaa1115910.bv.util.LiveStreamUrlFetcher
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import dev.aaa1115910.biliapi.entity.live.LiveAreaGroup

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
    var focusOnContent by remember { mutableStateOf(false) }
    var parentNavHasFocus by remember { mutableStateOf(false) }
    var subNavHasFocus by remember { mutableStateOf(false) }

    val currentListOnTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }
    }

    // 监听滚动位置，触发分页加载
    LaunchedEffect(gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val totalItems = liveViewModel.roomList.size
        // 滚动到倒数第5个item时加载更多
        if (lastVisibleIndex >= totalItems - 5 && liveViewModel.hasMore && !liveViewModel.loading) {
            logger.info { "Trigger load more, lastVisibleIndex: $lastVisibleIndex, totalItems: $totalItems" }
            liveViewModel.loadMore()
        }
    }

    // 恢复焦点到上次点击的直播间
    LaunchedEffect(liveViewModel.roomList.size) {
        if (liveViewModel.lastFocusedRoomIndex > 0 && 
            liveViewModel.lastFocusedRoomIndex < liveViewModel.roomList.size) {
            gridState.scrollToItem(liveViewModel.lastFocusedRoomIndex)
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
        subNavFocusRequester.requestFocus(scope)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            androidx.compose.foundation.layout.Column {
                // 第一行：主分区
                if (liveViewModel.parentAreaGroups.isNotEmpty()) {
                    val parentNavItems = remember(liveViewModel.parentAreaGroups) {
                        liveViewModel.parentAreaGroups.map { ParentAreaNavItem(it) }
                    }
                    TopNav(
                        modifier = Modifier
                            .focusRequester(parentNavFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged { parentNavHasFocus = it.hasFocus },
                        items = parentNavItems,
                        isLargePadding = false,
                        initialSelectedItem = parentNavItems.firstOrNull { it.group.id == liveViewModel.currentParentGroup?.id },
                        onSelectedChanged = { nav ->
                            (nav as? ParentAreaNavItem)?.let { liveViewModel.switchParentArea(it.group) }
                        },
                        onClick = { nav ->
                            (nav as? ParentAreaNavItem)?.let { item ->
                                if (item.group.id == liveViewModel.currentParentGroup?.id) {
                                    liveViewModel.refresh()
                                }
                            }
                        },
                        onLeftKeyEvent = {
                            drawerItemFocusRequesters[DrawerItem.Live]?.requestFocus()
                        }
                    )
                }
                
                // 第二行：子分区
                if (liveViewModel.subAreaList.isNotEmpty()) {
                    // 监听 currentParentGroup 变化以触发子分区列表更新
                    val subNavItems = remember(liveViewModel.currentParentGroup, liveViewModel.subAreaList.size) {
                        liveViewModel.subAreaList.map { SubAreaNavItem(it) }
                    }
                    TopNav(
                        modifier = Modifier
                            .focusRequester(subNavFocusRequester)
                            .padding(end = 80.dp)
                            .onFocusChanged { subNavHasFocus = it.hasFocus },
                        items = subNavItems,
                        isLargePadding = !focusOnContent && currentListOnTop,
                        initialSelectedItem = subNavItems.firstOrNull { it.area.id == liveViewModel.currentSubArea?.id },
                        onSelectedChanged = { nav ->
                            (nav as? SubAreaNavItem)?.let { liveViewModel.switchSubArea(it.area) }
                        },
                        onClick = { nav ->
                            (nav as? SubAreaNavItem)?.let { item ->
                                if (item.area.id == liveViewModel.currentSubArea?.id) {
                                    liveViewModel.refresh()
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
                LoadingTip()
            } else {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(
                        items = liveViewModel.roomList,
                        key = { _, room -> room.roomId }
                    ) { index, room ->
                    val entryCardModifier = if (index == 0 && liveViewModel.parentAreaGroups.isEmpty()) {
                        Modifier.focusRequester(navFocusRequester)
                    } else {
                        Modifier
                    }

                    LiveRoomCard(
                        modifier = entryCardModifier,
                        data = room,
                        onClick = {
                            // 保存焦点位置
                            liveViewModel.lastFocusedRoomIndex = index
                            // 启动播放器
                            scope.launch(Dispatchers.IO) {
                                val playInfo = LiveStreamUrlFetcher.fetchLiveStreamUrl(room.roomId)
                                if (playInfo != null) {
                                    VideoPlayerV3Activity.actionStartLive(
                                        context = context,
                                        roomId = playInfo.roomId,
                                        streamUrl = playInfo.streamUrl,
                                        title = room.title,
                                        upName = room.uname
                                    )
                                }
                            }
                        },
                        onFocus = {
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
                }
            }
        }
    }
}

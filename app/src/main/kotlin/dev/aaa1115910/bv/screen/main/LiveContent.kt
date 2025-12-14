package dev.aaa1115910.bv.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import dev.aaa1115910.bv.component.live.LiveRoomCard
import dev.aaa1115910.bv.viewmodel.live.LiveViewModel
import org.koin.androidx.compose.koinViewModel

import androidx.compose.ui.focus.focusProperties

@Composable
fun LiveContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    viewModel: LiveViewModel = koinViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val areaGroups = viewModel.areaGroups
    val subAreas = viewModel.currentSubAreas
    val roomList = viewModel.roomList
    
    var selectedParentTabIndex by remember { mutableIntStateOf(0) }
    var selectedSubTabIndex by remember { mutableIntStateOf(0) }
    
    val firstSubTabFocusRequester = remember { FocusRequester() }

    LaunchedEffect(viewModel.currentParentAreaId) {
        val index = areaGroups.indexOfFirst { it.id == viewModel.currentParentAreaId }
        if (index != -1) selectedParentTabIndex = index
    }
    LaunchedEffect(viewModel.currentAreaId) {
        if (subAreas.isNotEmpty()) {
            val index = subAreas.indexOfFirst { (it.id.toIntOrNull() ?: 0) == viewModel.currentAreaId }
            if (index != -1) selectedSubTabIndex = index
            else selectedSubTabIndex = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Parent Area Tabs
        if (areaGroups.isNotEmpty()) {
            TabRow(
                selectedTabIndex = selectedParentTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .focusRestorer(navFocusRequester)
            ) {
                areaGroups.forEachIndexed { index, group ->
                    LiveTab(
                        selected = selectedParentTabIndex == index,
                        title = group.name,
                        onFocus = {
                             viewModel.onParentAreaSelect(group.id)
                        },
                        onClick = { },
                        modifier = (if (index == 0) Modifier.focusRequester(navFocusRequester) else Modifier)
                            .focusProperties { down = firstSubTabFocusRequester }
                    )
                }
            }
        }

        // 2. Sub Area Tabs
        if (subAreas.isNotEmpty()) {
             TabRow(
                selectedTabIndex = selectedSubTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                subAreas.forEachIndexed { index, item ->
                    LiveTab(
                        selected = selectedSubTabIndex == index,
                        title = item.name,
                        onFocus = {
                            viewModel.onAreaSelect(item.id.toIntOrNull() ?: 0)
                        },
                        onClick = { },
                        modifier = if (index == 0) Modifier.focusRequester(firstSubTabFocusRequester) else Modifier
                    )
                }
            }
        }

        // 3. Room List
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(roomList) { room ->
                LiveRoomCard(
                    data = room,
                    onClick = {
                        dev.aaa1115910.bv.activities.video.VideoPlayerV3Activity.actionStartLive(
                            context = context,
                            roomId = room.roomid,
                            title = room.title,
                            authorMid = room.uid,
                            authorName = room.uname
                        )
                    }
                )
            }
            
            item {
                LaunchedEffect(Unit) {
                    if (viewModel.hasMore) {
                        viewModel.fetchRoomList()
                    }
                }
            }
        }
    }
}

@Composable
private fun TabRowScope.LiveTab(
    selected: Boolean,
    title: String,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tab(
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = LocalContentColor.current,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

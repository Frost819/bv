package dev.aaa1115910.bv.player.tv.controller.playermenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerMenuNavItem
import dev.aaa1115910.bv.player.tv.controller.playermenu.component.MenuListItem
import dev.aaa1115910.bv.util.ifElse

@Composable
fun MenuNavList(
    modifier: Modifier = Modifier,
    selectedMenu: VideoPlayerMenuNavItem,
    onSelectedChanged: (VideoPlayerMenuNavItem) -> Unit,
    isFocusing: Boolean
) {
    val context = LocalContext.current
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val focusRequester = remember { FocusRequester() }
    val navItems = remember(videoPlayerConfigData.isLive) {
        VideoPlayerMenuNavItem.entries.toMutableList().apply {
            if (videoPlayerConfigData.isLive) remove(VideoPlayerMenuNavItem.ClosedCaption)
        }
    }

    LazyColumn(
        modifier = modifier
            .focusRestorer(focusRequester),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(navItems) { index, item ->
            MenuListItem(
                modifier = Modifier
                    .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                text = item.getDisplayName(context),
                icon = item.icon,
                expanded = isFocusing,
                selected = selectedMenu == item,
                onClick = {},
                onFocus = { onSelectedChanged(item) },
            )
        }
    }
}
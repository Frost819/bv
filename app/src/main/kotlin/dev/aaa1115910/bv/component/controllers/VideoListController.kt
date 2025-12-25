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
    currentCid: Long,
    videoList: List<VideoListItem>,
    onPlayNewVideo: (VideoListItem) -> Unit,
) {
    val listState = rememberLazyListState()

    val parentFocusRequester = remember { FocusRequester() }
    val childFocusRequester = remember { FocusRequester() }

    // 自动定位到当前分P
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

                        val hasSubPages = !video.ugcPages.isNullOrEmpty()
                        val isParentSelected = video.cid == currentCid
                        val isChildSelected = video.ugcPages?.any { it.cid == currentCid } == true

                        var expanded by remember(video.cid) {
                            mutableStateOf(isChildSelected)
                        }

                        // 如果当前正在播放的是子项，则自动展开父项
                        LaunchedEffect(isChildSelected) {
                            if (isChildSelected) expanded = true
                        }

                        Column(
                            modifier = Modifier.animateContentSize()
                        ) {
                            // 视频父项
                            val parentModifier =
                                if (isParentSelected)
                                    Modifier.focusRequester(parentFocusRequester)
                                else Modifier

                            DenseListItem(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .then(parentModifier),
                                selected = isParentSelected && !isChildSelected,
                                onClick = {
                                    if (hasSubPages) {
                                        expanded = !expanded
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

                                            MenuListItem(
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp)
                                                    .then(childModifier),
                                                text = page.title,
                                                selected = isPageSelected,
                                                textAlign = TextAlign.Start
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
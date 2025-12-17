package dev.aaa1115910.bv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.ui.common.UiEvent
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun ToViewScreen(
    modifier: Modifier = Modifier,
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // 按 playString 分组
    val (unwatched, watched) = toViewViewModel.histories.partition { it.timeString != "已看完" }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    TvLazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 未看完标题
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "未看完",
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        if (unwatched.isNotEmpty()) {
            items(items = unwatched) { item ->
                Box(contentAlignment = Alignment.Center) {
                    SmallVideoCard(
                        data = item,
                        delToView = true,
                        onClick = {
                            VideoInfoActivity.actionStart(
                                context = context,
                                aid = item.avid,
                                proxyArea = ProxyArea.checkProxyArea(item.title)
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.delToView(item.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.actionStart(
                                context = context,
                                fromController = true,
                                aid = item.avid
                            )
                        },
                        onGoToUpPage = item.upMid?.let {
                            { UpInfoActivity.actionStart(context, it, item.upName) }
                        }
                    )
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyTip()
            }
        }

        // 已看完标题
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "已看完",
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        if (watched.isNotEmpty()) {
            items(items = watched) { item ->
                Box(contentAlignment = Alignment.Center) {
                    SmallVideoCard(
                        data = item,
                        delToView = true,
                        onClick = {
                            VideoInfoActivity.actionStart(
                                context = context,
                                aid = item.avid,
                                proxyArea = ProxyArea.checkProxyArea(item.title)
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.delToView(item.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.actionStart(
                                context = context,
                                fromController = true,
                                aid = item.avid
                            )
                        },
                        onGoToUpPage = item.upMid?.let {
                            { UpInfoActivity.actionStart(context, it, item.upName) }
                        }
                    )
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyTip()
            }
        }
    }
}


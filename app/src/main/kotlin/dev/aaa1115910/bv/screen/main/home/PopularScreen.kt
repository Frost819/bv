package dev.aaa1115910.bv.screen.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.LoadingTip
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.common.UiEvent
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun PopularScreen(
    modifier: Modifier = Modifier,
    popularViewModel: PopularViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val onClickVideo: (UgcItem) -> Unit = { ugcItem ->
        VideoInfoActivity.actionStart(context, ugcItem.aid)
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    // 监听可见区最后一个 item 的 index，距离尾部 20 个就翻页
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= popularViewModel.popularVideoList.size - 20
            }
            .collect {
                scope.launch(Dispatchers.IO) {
                    popularViewModel.loadMore()
                }
            }
    }

    TvLazyVerticalGrid(
        modifier = modifier,
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = popularViewModel.popularVideoList,
            key = { index, _ -> index }
        ) { _, item ->
            SmallVideoCard(
                data = remember(item) {         // `VideoCardData` 只在 item 变动时重建
                    VideoCardData(
                        avid = item.aid,
                        title = item.title,
                        cover = item.cover,
                        play = item.play.takeIf { it != -1 },
                        danmaku = item.danmaku.takeIf { it != -1 },
                        upName = item.author,
                        time = item.duration * 1000L,
                        pubTime = item.pubTime
                    )
                },
                onClick = { onClickVideo(item) },
                onAddWatchLater = {
                    toViewViewModel.addToView(item.aid)
                },
                onGoToDetailPage = {
                    VideoInfoActivity.actionStart(
                        context = context,
                        fromController = true,
                        aid = item.aid
                    )
                },
                onGoToUpPage = if (item.authorMid != null && item.authorMid != 0L) {
                    { UpInfoActivity.actionStart(context, item.authorMid!!, item.author) }
                } else null
            )
        }

        if (popularViewModel.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {    // 网格里占整行
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { LoadingTip() }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "没有更多了捏",
                    color = Color.White
                )
            }
        }
    }
}

package dev.aaa1115910.bv.screen.main.ugc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.biliapi.entity.ugc.region.UgcFeedPage
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.LoadingTip
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun UgcRegionScaffold(
    modifier: Modifier = Modifier,
    state: UgcScaffoldState,
    onLoadMore: () -> Unit,
    onAddWatchLater: ((Long) -> Unit),
    onGoToDetailPage: ((Long) -> Unit),
    onGoToUpPage: ((Long, String) -> Unit),
) {
    val gridState = state.lazyGridState
    val context = LocalContext.current


    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= state.ugcItems.size - 1
            }
            .collect {
                onLoadMore()
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
        // 用index的话快速刷新有概率闪退
        items(state.ugcItems) {item ->
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
                onClick = { VideoInfoActivity.actionStart(context, item.aid) },
                onAddWatchLater = { onAddWatchLater(item.aid) },
                onGoToDetailPage = { onGoToDetailPage(item.aid) },
                onGoToUpPage = item.authorMid?.let {
                    { onGoToUpPage(it, item.author) }
                }
            )
        }

        if (state.updating) {
            item(span = { GridItemSpan(maxLineSpan) }) {    // 网格里占整行
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) { LoadingTip() }
            }
        }
    }
}


data class UgcScaffoldState(
    val lazyGridState: LazyGridState,
    val ugcType: UgcTypeV2,
    val ugcItems: MutableList<UgcItem> = mutableStateListOf<UgcItem>(),
    var nextPage: UgcFeedPage = UgcFeedPage(),
    var hasMore: Boolean = true,
    var updating: Boolean = false
)
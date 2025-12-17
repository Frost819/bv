package dev.aaa1115910.bv.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.http.util.smartDate
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.LoadingTip
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.common.UiEvent
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.TagViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.androidx.compose.koinViewModel

@Composable
fun TagScreen(
    modifier: Modifier = Modifier,
    tagViewModel: TagViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    val uiState = tagViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("tagId")) {
            val tagId = intent.getIntExtra("tagId", 0)
            val tagName = intent.getStringExtra("tagName") ?: ""
            tagViewModel.setNameAndId(tagName, tagId)
            tagViewModel.update()
        } else {
            context.finish()
        }
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

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= uiState.value.videoList.size - 20
            }
            .collect {
                tagViewModel.update()
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp, end = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.value.tagName,
                        fontSize = 24.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.load_data_count,
                                uiState.value.videoList.size
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        AnimatedVisibility(visible = uiState.value.noMore) {
                            Text(
                                text = stringResource(R.string.load_data_no_more),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        TvLazyVerticalGrid(
            modifier = Modifier.padding(innerPadding),
            columns = GridCells.Fixed(4),
            state = gridState,
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(
                items = uiState.value.videoList,
                key = { video -> video.aid }
            ) { video ->
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    SmallVideoCard(
                        data = VideoCardData(
                            avid = video.aid,
                            title = video.title,
                            cover = video.pic,
                            upName = video.owner.name,
                            play = video.stat.view,
                            danmaku = video.stat.danmaku,
                            time = video.duration * 1000L,
                            pubTime = video.pubdate.smartDate
                        ),
                        onClick = { VideoInfoActivity.actionStart(context, video.aid) },
                        onAddWatchLater = {
                            toViewViewModel.addToView(video.aid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.actionStart(
                                context = context,
                                fromController = true,
                                aid = video.aid
                            )
                        },
                        onGoToUpPage =
                        { UpInfoActivity.actionStart(context, video.owner.mid, video.owner.name) }
                    )
                }
            }
            if (uiState.value.loading){
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            }
        }
    }
}
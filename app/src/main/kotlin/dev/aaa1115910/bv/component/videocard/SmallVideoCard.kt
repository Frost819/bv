package dev.aaa1115910.bv.component.videocard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.UpIcon
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl


@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    delToView: Boolean = false,
    onClick: () -> Unit,
    onAddWatchLater: (() -> Unit)? = null,
    onGoToDetailPage: (() -> Unit)? = null,
    onGoToUpPage: (() -> Unit)? = null,
) {
    var showActions by remember { mutableStateOf(false) }
    // 解决长按卡片松开会导致一次按钮触发的问题
    var releaseLongPress by remember { mutableStateOf(false) }
    val firstButtonRequester = remember { FocusRequester() }

    // 判断是否有任何操作按钮
    val hasAnyAction = onAddWatchLater != null || onGoToDetailPage != null || onGoToUpPage != null

    LaunchedEffect(showActions) {
        if (showActions && hasAnyAction) {
            firstButtonRequester.requestFocus()
        } else if (!showActions) {
            releaseLongPress = false // 退出操作态时重置
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = { if (!showActions) onClick() },
            onLongClick = {
                if (hasAnyAction) showActions = true

            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .onFocusChanged { focusState ->
                    if (!focusState.hasFocus) showActions = false
                },
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.border),
                    shape = MaterialTheme.shapes.large
                )
            )
        ) {
            if (showActions) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onAddWatchLater?.let {
                        IconButton(
                            onClick = {
                                if (!releaseLongPress) {
                                    releaseLongPress = true
                                    return@IconButton
                                }
                                it()
                            },
                            modifier = Modifier.focusRequester(firstButtonRequester)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (delToView)
                                        R.drawable.remove_from_list
                                    else
                                        R.drawable.add_to_list
                                ),
                                contentDescription = "Add to/Remove from watch later"
                            )
                        }
                    }

                    onGoToDetailPage?.let {
                        IconButton(onClick = { it() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.info_24px),
                                contentDescription = "Video Detail"
                            )
                        }
                    }

                    onGoToUpPage?.let {
                        IconButton(onClick = { it() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.contact_page_24px),
                                contentDescription = "Up Page"
                            )
                        }
                    }
                }
            } else {
                CardCover(
                    cover = data.cover,
                    play = data.playString,
                    danmaku = data.danmakuString,
                    time = data.timeString
                )
            }
        }

        CardInfo(
            modifier = Modifier.fillMaxWidth(),
            title = data.title,
            upName = data.upName,
            pubTime = data.pubTime
        )
    }
}


@Composable
fun CardCover(
    modifier: Modifier = Modifier,
    cover: String,
    play: String,
    danmaku: String,
    time: String
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large),
            model = cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        // 播放数、弹幕数、时间
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (play.isNotBlank()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_count),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = play,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
            }
            if (danmaku.isNotBlank()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_danmaku_count),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = danmaku,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CardInfo(
    modifier: Modifier = Modifier,
    title: String,
    upName: String,
    pubTime: String?
) {
    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            UpIcon()
            Text(
                modifier = Modifier.weight(1f),
                text = upName,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pubTime ?: "",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Preview
@Composable
fun SmallVideoCardWithoutFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        pubTime = "1小时前"
    )
    BVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCard(
                modifier = Modifier.padding(20.dp),
                onClick = {},
                data = data,
            )
        }
    }
}

@Preview
@Composable
fun SmallVideoCardWithFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        pubTime = "1小时前"
    )
    BVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCard(
                modifier = Modifier.padding(20.dp),
                onClick = {},
                data = data,
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
fun SmallVideoCardsPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        //cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        cover = "",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000,
        pubTime = "1小时前"
    )
    BVTheme {
        TvLazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(20) {
                item {
                    SmallVideoCard(
                        onClick = {},
                        data = data
                    )
                }
            }
        }
    }
}
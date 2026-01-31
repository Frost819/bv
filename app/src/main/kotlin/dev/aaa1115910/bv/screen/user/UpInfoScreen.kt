package dev.aaa1115910.bv.screen.user

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.TextFieldDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import dev.aaa1115910.bv.viewmodel.user.UpInfoViewModel
import kotlinx.coroutines.yield
import org.koin.androidx.compose.koinViewModel


@Composable
fun UpSpaceScreen(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    // 保证进入页面焦点在网格第一张卡片
    val firstItemFocusRequester = remember { FocusRequester() }
    var requestedInitialFocus by remember { mutableStateOf(false) }

    var searchCanFocus by remember { mutableStateOf(false) }
    var searchFieldHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("mid")) {
            val mid = intent.getLongExtra("mid", 0)
            val name = intent.getStringExtra("name") ?: ""
            /*
            upInfoViewModel.upMid = mid
            upInfoViewModel.upName = name
            upInfoViewModel.update()
             */
            upInfoViewModel.reset(mid = mid, name = name)
            upInfoViewModel.startAutoLoad()
        } else {
            context.finish()
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> event.message.toast(context)
            }
        }
    }

    /*
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= upInfoViewModel.spaceVideos.size - 20
            }
            .collect {
                upInfoViewModel.update()
            }
    }
     */

    // 首批数据到来后，把焦点落到第一张卡片
    LaunchedEffect(upInfoViewModel.spaceVideos.size) {
        if (!requestedInitialFocus && upInfoViewModel.spaceVideos.isNotEmpty()) {
            requestedInitialFocus = true
            // 等待一帧，保证 item 已经 compose 出来
            yield()
            firstItemFocusRequester.requestFocus()
        }
    }

    val loadedCount = upInfoViewModel.spaceVideos.size
    val totalCount = upInfoViewModel.totalCount
    val topRightText = remember(loadedCount, totalCount, upInfoViewModel.noMore) {
        if (upInfoViewModel.noMore) {
            val total = totalCount ?: loadedCount
            "共${total}条视频"
        } else {
            if (totalCount != null && totalCount > 0) {
                "加载中……\n$loadedCount / $totalCount"
            } else {
                "加载中……\n$loadedCount"
            }
        }
    }

    val visibleVideos by remember {
        derivedStateOf {
            val q = upInfoViewModel.debouncedQuery.trim()
            if (q.isBlank()) {
                upInfoViewModel.spaceVideos
            } else {
                upInfoViewModel.spaceVideos.filter { it.title.contains(q, ignoreCase = true) }
            }
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
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = upInfoViewModel.upName,
                        fontSize = 26.sp
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    TextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties {
                                // 初期禁止输入框获得焦点，避免它先拿焦点弹 IME
                                canFocus = searchCanFocus
                            }
                            .onFocusChanged { searchFieldHasFocus = it.hasFocus }
                            .drawWithContent {
                                // 先让 TextField 自己画完（背景/文本/内部装饰）
                                drawContent()

                                // 再画“底部粗线”，保证不会被盖住
                                val stroke = 3.dp.toPx()
                                val y = size.height - stroke / 2f
                                drawLine(
                                    color = if (searchFieldHasFocus) Color(0xFFFF0000) else Color.White.copy(alpha = 0.55f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = stroke
                                )
                            },
                        value = upInfoViewModel.rawQuery,
                        onValueChange = upInfoViewModel::onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 26.sp, lineHeight = 30.sp),
                        shape = RectangleShape, // 直角（无圆角）
                        colors = TextFieldDefaults.colors(
                            // 关掉默认 indicator，否则会出现“默认细线 + 你画的粗线”叠加
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                upInfoViewModel.onSearchAction()
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    Text(
                        text = topRightText,
                        fontSize = 26.sp,
                        style = androidx.tv.material3.MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp),
                        textAlign = TextAlign.End,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    /*
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.load_data_count,
                                upInfoViewModel.spaceVideos.size
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        AnimatedVisibility(visible = upInfoViewModel.noMore) {
                            Text(
                                text = stringResource(R.string.load_data_no_more),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    */
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            TvLazyVerticalGrid(
                modifier = Modifier.padding(innerPadding),
                columns = GridCells.Fixed(4),
                state = gridState,
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (upInfoViewModel.spaceVideos.isNotEmpty()) {
                    /*
                itemsIndexed(
                    items = upInfoViewModel.spaceVideos,
                    key = { index, _ -> index }
                ) { _, video ->
                    */
                    itemsIndexed(
                        items = visibleVideos,
                        key = { _, video -> video.avid }
                    ) { index, video ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = if (index == 0) {
                                Modifier
                                    .focusRequester(firstItemFocusRequester)
                                    .onFocusChanged { state ->
                                        // 一旦第一张卡确实拿到过焦点，就允许输入框可聚焦
                                        if (state.hasFocus) {
                                            searchCanFocus = true
                                        }
                                    }
                            } else {
                                Modifier
                            },
                        ) {
                            SmallVideoCard(
                                data = video,
                                onClick = {
                                    VideoInfoActivity.actionStart(
                                        context = context,
                                        aid = video.avid,
                                        proxyArea = ProxyArea.checkProxyArea(video.title)
                                    )
                                },
                                onAddWatchLater = {
                                    toViewViewModel.addToView(video.avid)
                                },
                                onGoToDetailPage = {
                                    VideoInfoActivity.actionStart(
                                        context = context,
                                        fromController = true,
                                        aid = video.avid
                                    )
                                },
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
    }
}
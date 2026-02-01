package dev.aaa1115910.bv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private class FolderQueryState {
    var rawQuery by mutableStateOf("")
    var debouncedQuery by mutableStateOf("")
    var debounceJob: Job? = null
}

@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    favoriteViewModel: FavoriteViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val defaultFocusRequester = remember { FocusRequester() }
    var focusOnTabs by remember { mutableStateOf(true) }
    val lazyGridState = rememberLazyGridState()

    // 每个收藏夹自己的搜索状态（切换收藏夹不清空；离开本页面才清空）
    val folderQueryStates = remember { mutableStateMapOf<Long, FolderQueryState>() }

    fun getQueryState(folderId: Long): FolderQueryState {
        return folderQueryStates.getOrPut(folderId) { FolderQueryState() }
    }

    fun clearFolderQuery(folderId: Long) {
        val st = folderQueryStates[folderId] ?: return
        st.debounceJob?.cancel()
        st.debounceJob = null
        st.rawQuery = ""
        st.debouncedQuery = ""
    }

    fun onFolderQueryChange(folderId: Long, newText: String) {
        val st = getQueryState(folderId)
        st.rawQuery = newText
        st.debounceJob?.cancel()
        st.debounceJob = scope.launch {
            delay(900)
            st.debouncedQuery = st.rawQuery
        }
    }

    fun onFolderSearchAction(folderId: Long) {
        val st = getQueryState(folderId)
        st.debounceJob?.cancel()
        st.debounceJob = null
        st.debouncedQuery = st.rawQuery
    }

    // 离开 FavoriteScreen 页面时清空搜索内容
    DisposableEffect(Unit) {
        onDispose {
            folderQueryStates.values.forEach { it.debounceJob?.cancel() }
            folderQueryStates.clear()
            // 离开收藏页时停止自动加载，避免后台继续刷接口
            favoriteViewModel.stopAutoLoad()
        }
    }

    val currentTabIndex by remember {
        derivedStateOf {
            favoriteViewModel.favoriteFolderMetadataList.indexOf(favoriteViewModel.currentFavoriteFolderMetadata)
        }
    }

    val currentFolderId by remember {
        derivedStateOf { favoriteViewModel.currentFavoriteFolderMetadata?.id }
    }

    val visibleFavorites by remember {
        derivedStateOf {
            val folderId = currentFolderId
            if (folderId == null) return@derivedStateOf favoriteViewModel.favorites

            val q = folderQueryStates[folderId]?.debouncedQuery?.trim().orEmpty()
            if (q.isBlank()) {
                favoriteViewModel.favorites
            } else {
                favoriteViewModel.favorites.filter { it.title.contains(q, ignoreCase = true) }
            }
        }
    }

    // 1s gate：必须在“当前 Tab 按钮上停留 >= 1s”才允许开始请求
    var selectedFolderIdForAutoLoad by remember { mutableStateOf<Long?>(null) }

    // 搜索弹窗
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchDialogFolderId by remember { mutableStateOf<Long?>(null) }
    var searchFieldHasFocus by remember { mutableStateOf(false) }

    fun closeSearchDialog(apply: Boolean) {
        val folderId = searchDialogFolderId
        if (apply && folderId != null) {
            // 按返回关闭也要立刻应用搜索（用 debouncedQuery）
            onFolderSearchAction(folderId)
        }
        showSearchDialog = false
        searchDialogFolderId = null
    }

// Dialog 打开时暂停加载；关闭后继续
    LaunchedEffect(showSearchDialog) {
        favoriteViewModel.updateLoadingPaused(showSearchDialog)
        if (!showSearchDialog && favoriteViewModel.allowAutoLoad) {
            favoriteViewModel.startAutoLoad()
        }
    }

    val updateCurrentFavoriteFolder: (folderMetadata: FavoriteFolderMetadata) -> Unit =
        { folderMetadata ->
            // 切换收藏夹：必须立刻停旧的自动加载
            favoriteViewModel.stopAutoLoad()

            favoriteViewModel.currentFavoriteFolderMetadata = folderMetadata
            favoriteViewModel.favorites.clear()
            favoriteViewModel.resetPageNumber()
            favoriteViewModel.updateFolderItems(force = true)
        }

    /*
    LaunchedEffect(lazyGridState) {
        snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { index ->
                index != null && index >= favoriteViewModel.favorites.size - 20
            }
            .collect {
                scope.launch(Dispatchers.IO) {
                    favoriteViewModel.updateFolderItems()
                }
            }
    }
     */
    // 当“Tab 按钮焦点”稳定停留 1s 后，才开启自动加载
    LaunchedEffect(selectedFolderIdForAutoLoad) {
        val id = selectedFolderIdForAutoLoad ?: return@LaunchedEffect
        delay(1000)
        if (selectedFolderIdForAutoLoad == id && favoriteViewModel.currentFavoriteFolderMetadata?.id == id) {
            favoriteViewModel.allowAutoLoad = true
            favoriteViewModel.startAutoLoad()
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    val density = LocalDensity.current
    val tabLabelFontSize = MaterialTheme.typography.labelLarge.fontSize
    val filterIconSizeDp = with(density) {
        // 让 icon 和文字等高
        tabLabelFontSize.toDp()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent {
                // Dialog 打开时，返回键优先关闭 Dialog
                if (showSearchDialog && it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    closeSearchDialog(apply = true)
                    return@onPreviewKeyEvent true
                }
                false
            },
        horizontalAlignment = Alignment.Start

    ) {
        TabRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusRequester(defaultFocusRequester)
                .onFocusChanged { focusOnTabs = it.hasFocus }
                .focusRestorer(focusRequester),
            selectedTabIndex = currentTabIndex,
            separator = { Spacer(modifier = Modifier.width(12.dp)) },
        ) {
            favoriteViewModel.favoriteFolderMetadataList.forEachIndexed { index, folderMetadata ->

                val folderId = folderMetadata.id
                val queryState = folderQueryStates[folderId]
                val isSearching = queryState?.debouncedQuery?.isNotBlank() == true

                // - KeyDown 时用 nativeKeyEvent.isLongPress 判定长按
                // - 并在 KeyUp 吃掉，避免短按 onClick 冲突
                var longPressTriggered by remember(folderId) { mutableStateOf(false) }

                Tab(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester))
                        .onFocusChanged { state ->
                            if (state.hasFocus) {
                                selectedFolderIdForAutoLoad = folderId
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            val isConfirmKey = event.key == Key.DirectionCenter ||
                                    event.key == Key.Enter ||
                                    event.key == Key.Spacebar

                            if (!isConfirmKey) return@onPreviewKeyEvent false

                            if (event.type == KeyEventType.KeyDown) {
                                if (event.nativeKeyEvent.isLongPress) {
                                    if (!longPressTriggered) {
                                        longPressTriggered = true

                                        // debouncedQuery 非空才算“搜索态”
                                        val isSearchingNow = folderQueryStates[folderId]?.debouncedQuery?.isNotBlank() == true
                                        if (isSearchingNow) {
                                            // 已在搜索态：再次长按 => 清空搜索
                                            clearFolderQuery(folderId)
                                        } else {
                                            // 未搜索：长按打开 dialog
                                            showSearchDialog = true
                                            searchDialogFolderId = folderId
                                        }
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent false
                            }

                            if (event.type == KeyEventType.KeyUp && longPressTriggered) {
                                // 吃掉 KeyUp，避免短按 onClick 被触发
                                longPressTriggered = false
                                return@onPreviewKeyEvent true
                            }

                            false
                        },
                    selected = currentTabIndex == index,
                    onFocus = {
                        if (favoriteViewModel.currentFavoriteFolderMetadata != folderMetadata) {
                            updateCurrentFavoriteFolder(folderMetadata)
                        }
                    },
                    onClick = { updateCurrentFavoriteFolder(folderMetadata) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                             .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSearching) {
                                Icon(
                                    modifier = Modifier.size(filterIconSizeDp),
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Text(

                                text = folderMetadata.title,
                                color = LocalContentColor.current,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TvLazyVerticalGrid(
            modifier = modifier,
            state = lazyGridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (visibleFavorites.isNotEmpty()) {
                items(
                    items = visibleFavorites,
                    key = { favorite -> favorite.avid }
                ) { favorite ->
                    Box(contentAlignment = Alignment.Center) {
                        SmallVideoCard(
                            data = favorite,
                            onClick = { VideoInfoActivity.actionStart(context, favorite.avid) },
                            onAddWatchLater = {
                                toViewViewModel.addToView(favorite.avid)
                            },
                            onGoToDetailPage = {
                                VideoInfoActivity.actionStart(
                                    context = context,
                                    fromController = true,
                                    aid = favorite.avid
                                )
                            },
                            onGoToUpPage = favorite.upMid?.let {
                                { UpInfoActivity.actionStart(context, it, favorite.upName) }
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
    // ===== 搜索 Dialog =====
    if (showSearchDialog) {
        val folderId = searchDialogFolderId
        val folderTitle = favoriteViewModel.favoriteFolderMetadataList.firstOrNull { it.id == folderId }?.title.orEmpty()

        if (folderId != null) {
            val st = getQueryState(folderId)

            TvAlertDialog(
                onDismissRequest = {
                    // 点返回视为“完成输入并应用”
                    closeSearchDialog(apply = true)
                },
                // title 传对应收藏夹名字
                title = {
                    Text(text = "在 $folderTitle 中搜索")
                },
                text = {
                    TextField(
                        modifier = Modifier
                            .width(600.dp)
                            .onFocusChanged { searchFieldHasFocus = it.hasFocus }
                            .drawWithContent {
                                drawContent()
                                val stroke = 3.dp.toPx()
                                val y = size.height - stroke / 2f
                                drawLine(
                                    color = if (searchFieldHasFocus) Color(0xFFFF0000) else Color.White.copy(alpha = 0.55f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = stroke
                                )
                            },
                        value = st.rawQuery,
                        onValueChange = { onFolderQueryChange(folderId, it) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 26.sp, lineHeight = 30.sp),
                        shape = RectangleShape,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { onFolderSearchAction(folderId) }
                        )
                    )
                },
                confirmButton = { },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            )
        }
    }

}
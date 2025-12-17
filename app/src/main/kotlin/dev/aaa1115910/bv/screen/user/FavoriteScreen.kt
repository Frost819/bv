package dev.aaa1115910.bv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import dev.aaa1115910.bv.ui.common.UiEvent
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

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

    val currentTabIndex by remember {
        derivedStateOf {
            favoriteViewModel.favoriteFolderMetadataList.indexOf(favoriteViewModel.currentFavoriteFolderMetadata)
        }
    }

    val updateCurrentFavoriteFolder: (folderMetadata: FavoriteFolderMetadata) -> Unit =
        { folderMetadata ->
            favoriteViewModel.currentFavoriteFolderMetadata = folderMetadata
            favoriteViewModel.favorites.clear()
            favoriteViewModel.resetPageNumber()
            favoriteViewModel.updateFolderItems(force = true)
        }

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

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
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
                Tab(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                    selected = currentTabIndex == index,
                    onFocus = {
                        if (favoriteViewModel.currentFavoriteFolderMetadata != folderMetadata) {
                            updateCurrentFavoriteFolder(folderMetadata)
                        }
                    },
                    onClick = { updateCurrentFavoriteFolder(folderMetadata) }
                ) {
                    Box(
                        modifier = Modifier.height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            text = folderMetadata.title,
                            color = LocalContentColor.current,
                            style = MaterialTheme.typography.labelLarge
                        )
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
            if (favoriteViewModel.favorites.isNotEmpty()) {
                items(
                    items = favoriteViewModel.favorites,
                    key = { favorite -> favorite.avid })
                { favorite ->
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
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

}
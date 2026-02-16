package dev.aaa1115910.bv.mobile.screen.home

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.mobile.activities.VideoPlayerActivity
import dev.aaa1115910.bv.mobile.component.preferences.items.listItemPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.screen.home.search.SearchInputContent
import dev.aaa1115910.bv.mobile.screen.home.search.SearchResultContent
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.viewmodel.search.SearchInputViewModel
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    searchInputViewModel: SearchInputViewModel = koinViewModel(),
    searchResultViewModel: SearchResultViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val windowSize = windowSizeClass.widthSizeClass

    val updateKeyword: (String) -> Unit = { newKeyword ->
        if (newKeyword != searchInputViewModel.keyword) {
            searchInputViewModel.keyword = newKeyword
            searchInputViewModel.updateSuggests()
        }
    }

    val onSearch: (String) -> Unit = {
        searchResultViewModel.keyword = it
        searchResultViewModel.update()
        searchInputViewModel.addSearchHistory(it)
    }

    val onOpenUgc: (Long) -> Unit = { aid ->
        VideoPlayerActivity.actionStart(context = context, aid = aid)
    }

    SearchContent(
        modifier = modifier,
        windowSize = windowSize,
        keywordSuggestions = searchInputViewModel.suggests,
        historyKeywords = searchInputViewModel.searchHistories.map { it.keyword },
        matchedHistory = searchInputViewModel.matchedSearchHistories.map { it.keyword },
        updateKeyword = updateKeyword,
        onSearch = onSearch,
        onOpenUgc = onOpenUgc,
        videoSearchResult = searchResultViewModel.videoSearchResult.videos,
        mediaBangumiSearchResult = searchResultViewModel.mediaBangumiSearchResult.mediaBangumis,
        mediaFtSearchResult = searchResultViewModel.mediaFtSearchResult.mediaFts,
        biliUserSearchResult = searchResultViewModel.biliUserSearchResult.biliUsers,
        liveRoomSearchResult = searchResultViewModel.liveRoomSearchResult.liveRooms
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchContent(
    modifier: Modifier = Modifier,
    windowSize: WindowWidthSizeClass,
    keywordSuggestions: List<String> = emptyList(),
    historyKeywords: List<String>,
    matchedHistory: List<String>,
    updateKeyword: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onOpenUgc: (Long) -> Unit = {},
    videoSearchResult: List<SearchTypeResult.Video>,
    mediaBangumiSearchResult: List<SearchTypeResult.Pgc>,
    mediaFtSearchResult: List<SearchTypeResult.Pgc>,
    biliUserSearchResult: List<SearchTypeResult.User>,
    liveRoomSearchResult: List<SearchTypeResult.LiveRoom>
) {
    val scope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val navController = rememberNavController()

    var searchBarExpanded by remember { mutableStateOf(false) }
    var textFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(textFieldState.text, textFieldFocused) {
        println("Text field state: $textFieldState")
        searchBarExpanded = textFieldState.text != "" && textFieldFocused
        updateKeyword(textFieldState.text.toString())
    }

    val onSearchKeyword: (String) -> Unit = {
        onSearch(it)
        if (navController.currentDestination?.route != "searchResult") navController.navigate("searchResult")
        textFieldState.setTextAndPlaceCursorAtEnd(it)
        scope.launch {
            // 等到 searchBar 移动到顶部再收起
            delay(500)
            searchBarState.animateToCollapsed()
        }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            modifier = Modifier.onFocusChanged { textFieldFocused = it.isFocused },
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearchKeyword,
            placeholder = { Text(text = "在此处输入文字") },
        )
    }

    SharedTransitionLayout(
        modifier = modifier
    ) {
        NavHost(
            navController = navController,
            startDestination = "searchInput"
        ) {
            composable("searchInput") {
                SearchInputContent(
                    windowSize = windowSize,
                    keywordSuggestions = keywordSuggestions,
                    keywordHistories = historyKeywords,
                    matchedKeyworkHistories = matchedHistory,
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    searchBarExpanded = searchBarExpanded,
                    onSearchBarExpandedChange = { searchBarExpanded = it },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    inputField = inputField,
                    onSearch = onSearchKeyword
                )
            }
            composable("searchResult") {
                SearchResultContent(
                    modifier = Modifier.fillMaxSize(),
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    keywordSuggestions = keywordSuggestions,
                    historyKeywords = historyKeywords,
                    matchedHistory = matchedHistory,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    inputField = inputField,
                    videoSearchResult = videoSearchResult,
                    mediaBangumiSearchResult = mediaBangumiSearchResult,
                    mediaFtSearchResult = mediaFtSearchResult,
                    biliUserSearchResult = biliUserSearchResult,
                    liveRoomSearchResult = liveRoomSearchResult,
                    onSearch = onSearchKeyword,
                    onOpenUgc = onOpenUgc
                )
            }
        }
    }

    if (windowSize == WindowWidthSizeClass.Compact) {
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = inputField
        ) {
            SearchBarResultContent(
                modifier = Modifier.fillMaxSize(),
                keyword = textFieldState.text.toString(),
                recentHistory = historyKeywords,
                matchedHistory = matchedHistory,
                suggestions = keywordSuggestions,
                onSearch = onSearchKeyword,
                onDeleteHistory = {}
            )
        }
    }
}


@Composable
fun SearchBarResultContent(
    modifier: Modifier = Modifier,
    keyword: String,
    recentHistory: List<String>,
    matchedHistory: List<String>,
    suggestions: List<String>,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    val listItemColors = ListItemDefaults.colors().copy(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(12.dp)
        ) {
            preferenceGroups(
                "历史记录" to {
                    if (keyword.isNotEmpty()) {
                        matchedHistory.take(10).map {
                            listItemPreference(
                                headlineContent = { Text(text = it) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(3.dp),
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "search history icon",
                                        )
                                    }
                                },
                                colors = listItemColors,
                                onClick = { onSearch(it) }
                            )
                        }
                    } else {
                        recentHistory.take(10).map {
                            listItemPreference(
                                headlineContent = { Text(text = it) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(3.dp),
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "search history icon",
                                        )
                                    }
                                },
                                colors = listItemColors,
                                onClick = { onSearch(it) }
                            )
                        }
                    }
                },
                "搜索建议" to {
                    if (keyword.isNotEmpty()) {
                        suggestions.map {
                            listItemPreference(
                                headlineContent = { Text(text = it) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(3.dp),
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "search suggestion icon",
                                        )
                                    }
                                },
                                colors = listItemColors,
                                onClick = { onSearch(it) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
private fun SearchScreenMobilePreview() {
    BVMobileTheme {
        SearchContent(
            windowSize = WindowWidthSizeClass.Compact,
            videoSearchResult = emptyList(),
            mediaBangumiSearchResult = emptyList(),
            mediaFtSearchResult = emptyList(),
            biliUserSearchResult = emptyList(),
            liveRoomSearchResult = emptyList(),
            historyKeywords = emptyList(),
            matchedHistory = emptyList()
        )
    }
}

@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun SearchScreenTablePreview() {
    BVMobileTheme {
        SearchContent(
            windowSize = WindowWidthSizeClass.Expanded,
            videoSearchResult = emptyList(),
            mediaBangumiSearchResult = emptyList(),
            mediaFtSearchResult = emptyList(),
            biliUserSearchResult = emptyList(),
            liveRoomSearchResult = emptyList(),
            historyKeywords = emptyList(),
            matchedHistory = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchBarResultCompatPreview() {
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            onSearch = {},
            placeholder = { Text(text = "在此处输入文字") },
        )
    }

    BVMobileTheme {
        ExpandedFullScreenSearchBar(
            state = rememberSearchBarState(
                initialValue = SearchBarValue.Expanded
            ),
            inputField = inputField
        ) {
            SearchBarResultContent(
                modifier = Modifier.fillMaxSize(),
                keyword = "123",
                recentHistory = listOf("123", "456", "789"),
                matchedHistory = listOf("123", "456", "789"),
                suggestions = listOf("123", "456", "789"),
                onSearch = {},
                onDeleteHistory = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchBarResultDockedPreview() {
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            onSearch = {},
            placeholder = { Text(text = "在此处输入文字") },
        )
    }

    BVMobileTheme {
        DockedSearchBar(
            expanded = true,
            onExpandedChange = {},
            inputField = inputField,
        ) {
            SearchBarResultContent(
                keyword = "123",
                recentHistory = listOf("123", "456", "789"),
                matchedHistory = listOf("123", "456", "789"),
                suggestions = listOf("123", "456", "789"),
                onSearch = {},
                onDeleteHistory = {}
            )
        }
    }
}
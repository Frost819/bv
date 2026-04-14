package dev.aaa1115910.bv.screen.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import dev.aaa1115910.bv.component.PersonalTopNavItem
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.screen.user.FavoriteScreen
import dev.aaa1115910.bv.screen.user.FollowingSeasonScreen
import dev.aaa1115910.bv.screen.user.HistoryScreen
import dev.aaa1115910.bv.screen.user.ToViewScreen
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import dev.aaa1115910.bv.viewmodel.user.FollowingSeasonViewModel
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun PersonalContent(
    navFocusRequester: FocusRequester,
    favouriteViewModel: FavoriteViewModel = koinViewModel(),
    historyViewModel: HistoryViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
    followingSeasonViewModel: FollowingSeasonViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()

    var focusOnContent by remember { mutableStateOf(false) }

    val firstTab = remember { Prefs.firstPersonalTopNavItem }
    var selectedTab by remember { mutableStateOf(firstTab) }

    val getReorderedItems: (PersonalTopNavItem) -> List<PersonalTopNavItem> = { item ->
        val allItems = PersonalTopNavItem.entries
        val startIndex = allItems.indexOf(item)
        if (startIndex == -1) emptyList()
        else allItems.drop(startIndex) + allItems.take(startIndex)
    }
    val reorderedItems = remember {
        getReorderedItems(firstTab)
    }

    fun refreshPageData(nav: PersonalTopNavItem) {
        when (nav) {
            PersonalTopNavItem.ToView -> {
                toViewViewModel.clearData()
                toViewViewModel.update()
            }

            PersonalTopNavItem.History -> {
                historyViewModel.clearData()
                historyViewModel.update()
            }

            PersonalTopNavItem.Favorite -> {
                favouriteViewModel.clearData()
                favouriteViewModel.updateFoldersInfo()
                favouriteViewModel.updateFolderItems(force = true)
            }

            PersonalTopNavItem.FollowingSeason -> {
                followingSeasonViewModel.clearData()
                followingSeasonViewModel.loadMore()
            }
        }
    }

    //启动时刷新数据
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            favouriteViewModel.updateFolderItems()
        }
        scope.launch(Dispatchers.IO) {
            historyViewModel.update()
        }
        scope.launch(Dispatchers.IO) {
            toViewViewModel.update()
        }
        scope.launch(Dispatchers.IO) {
            followingSeasonViewModel.loadMore()
        }
    }


    Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier
                    .focusRequester(navFocusRequester),
                items = reorderedItems,
                isLargePadding = !focusOnContent,
                onSelectedChanged = { nav ->
                    selectedTab = nav as PersonalTopNavItem
                },
                onClick = { nav ->
                    refreshPageData(nav as PersonalTopNavItem)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
                .onKeyEvent {
                    if (it.key == Key.Menu) {
                        if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                        refreshPageData(selectedTab)
                        navFocusRequester.requestFocus()
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                },
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "personal animated content",
                transitionSpec = {
                    val coefficient = 10
                    if (reorderedItems.indexOf(targetState) < reorderedItems.indexOf(initialState)) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                }
            ) { screen ->
                when (screen) {
                    PersonalTopNavItem.ToView -> {
                        ToViewScreen()
                    }

                    PersonalTopNavItem.History -> {
                        HistoryScreen()
                    }

                    PersonalTopNavItem.Favorite -> {
                        FavoriteScreen()
                    }

                    PersonalTopNavItem.FollowingSeason -> {
                        FollowingSeasonScreen()
                    }
                }
            }
        }
    }
}
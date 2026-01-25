package dev.aaa1115910.bv.tv.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.UserPanel
import dev.aaa1115910.bv.tv.activities.settings.SettingsActivity
import dev.aaa1115910.bv.tv.activities.user.FavoriteActivity
import dev.aaa1115910.bv.tv.activities.user.FollowingSeasonActivity
import dev.aaa1115910.bv.tv.activities.user.HistoryActivity
import dev.aaa1115910.bv.tv.activities.user.LoginActivity
import dev.aaa1115910.bv.tv.activities.user.ToViewActivity
import dev.aaa1115910.bv.tv.activities.user.UserInfoActivity
import dev.aaa1115910.bv.tv.screens.main.DrawerContent
import dev.aaa1115910.bv.tv.screens.main.DrawerItem
import dev.aaa1115910.bv.tv.screens.main.HomeContent
import dev.aaa1115910.bv.tv.screens.main.LiveContent
import dev.aaa1115910.bv.tv.screens.main.PgcContent
import dev.aaa1115910.bv.tv.screens.main.UgcContent
import dev.aaa1115910.bv.tv.screens.main.currentSelectedTabs
import dev.aaa1115910.bv.tv.screens.main.drawerItemFocusRequesters
import dev.aaa1115910.bv.tv.screens.search.SearchInputScreen
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val logger = KotlinLogging.logger("MainScreen")
    val scope = rememberCoroutineScope()
    var showUserPanel by remember { mutableStateOf(false) }
    var lastPressBack: Long by remember { mutableLongStateOf(0L) }
    var selectedDrawerItem by remember { mutableStateOf(DrawerItem.Home) }
    var focusedDrawerItem by remember { mutableStateOf(DrawerItem.Home) }

    val mainFocusRequester = remember { FocusRequester() }
    val ugcFocusRequester = remember { FocusRequester() }
    val pgcFocusRequester = remember { FocusRequester() }
    val liveFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    // 时间显示状态
    var currentTime by remember {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        mutableStateOf(dateFormat.format(Date()))
    }

    // 定时更新时间
    LaunchedEffect(Unit) {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            currentTime = dateFormat.format(Date())
        }
    }

    val handleBack = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressBack < 1500) {
            logger.fInfo { "Exiting bug video" }
            currentSelectedTabs[DrawerItem.Home] = Prefs.defaultHomeTab
            (context as Activity).finish()
        } else {
            lastPressBack = currentTime
            R.string.home_press_back_again_to_exit.toast(context)
        }
    }

    val onFocusToContent: () -> Unit = {
        when (focusedDrawerItem) {
            DrawerItem.Home -> mainFocusRequester.requestFocus()
            DrawerItem.UGC -> ugcFocusRequester.requestFocus()
            DrawerItem.PGC -> pgcFocusRequester.requestFocus()
            DrawerItem.Live -> liveFocusRequester.requestFocus()
            DrawerItem.Search -> searchFocusRequester.requestFocus()
            else -> {
                // 搜索+右侧是搜索->用户+用户内容不是放右侧的，右侧还是搜索。
                // 让内容对应的菜单获得焦点
                drawerItemFocusRequesters[selectedDrawerItem]?.requestFocus()
                when (selectedDrawerItem) {
                    DrawerItem.Home -> mainFocusRequester.requestFocus()
                    DrawerItem.UGC -> ugcFocusRequester.requestFocus()
                    DrawerItem.PGC -> pgcFocusRequester.requestFocus()
                    DrawerItem.Live -> liveFocusRequester.requestFocus()
                    DrawerItem.Search -> searchFocusRequester.requestFocus()
                    else -> {}
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            mainFocusRequester.requestFocus()
        }.onFailure {
            logger.fException(it) { "request default focus requester failed" }
        }
    }

    BackHandler {
        handleBack()
    }

    Scaffold(modifier = modifier) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val borderColor = MaterialTheme.colorScheme.surfaceContainerHigh
            val borderWidth = 1.dp
            // Left side - NavigationRail
            NavigationRail(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(71.dp)
                    .padding(end = borderWidth)
                    .drawBehind {
                        val borderWidthPx = borderWidth.toPx()
                        val x = size.width + borderWidthPx

                        drawLine(
                            color = borderColor,
                            start = Offset(x = x, y = 0f),
                            end = Offset(x = x, y = size.height),
                            strokeWidth = borderWidthPx
                        )
                    },
            ) {
                DrawerContent(
                    modifier = Modifier.fillMaxWidth(),
                    isLogin = userViewModel.isLogin,
                    avatar = userViewModel.face,
                    username = userViewModel.username,
                    //avatar = "https://i2.hdslb.com/bfs/face/ef0457addb24141e15dfac6fbf45293ccf1e32ab.jpg",
                    //username = "碧诗",
                    onDrawerItemChanged = { selectedDrawerItem = it },
                    onDrawerItemfocused = {
                        focusedDrawerItem = it
                    },
                    onOpenSettings = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    onShowUserPanel = {
                        // showUserPanel = true
                        context.startActivity(Intent(context, UserInfoActivity::class.java))
                    },
                    onFocusToContent = onFocusToContent,
                    onLogin = {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                )
            }

            // Right side - NavHost content
            AnimatedContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp),
                targetState = selectedDrawerItem,
                label = "main animated content",
                transitionSpec = {
                    val coefficient = 20
                    if (targetState.ordinal < initialState.ordinal) {
                        slideInVertically { -it / coefficient } togetherWith
                                fadeOut(animationSpec = tween(200)) + slideOutVertically { it / coefficient }
                    } else {
                        slideInVertically { it / coefficient } togetherWith
                                fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / coefficient }
                    }
                }
            ) { screen ->
                when (screen) {
                    DrawerItem.Home -> HomeContent(navFocusRequester = mainFocusRequester)
                    DrawerItem.UGC -> UgcContent(navFocusRequester = ugcFocusRequester)
                    DrawerItem.PGC -> PgcContent(navFocusRequester = pgcFocusRequester)
                    DrawerItem.Live -> LiveContent(navFocusRequester = liveFocusRequester)
                    DrawerItem.Search -> SearchInputScreen(defaultFocusRequester = searchFocusRequester)
                    else -> {}
                }
            }

            // 右上角时间显示
            Text(
                text = currentTime,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 0.dp)
                    .offset(y=(-2).dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Box(
            //     modifier = Modifier
            //         .fillMaxSize()
            //         .padding(start = 80.dp) // 为 NavigationRail 留出空间
            // ) {
            //     NavHost(
            //         navController,
            //         startDestination = startDestination.displayName
            //     ) {
            //         DrawerItem.entries.forEach { destination ->
            //             composable(destination.displayName) {
            //                 when (destination.displayName) {
            //                     DrawerItem.Home.displayName -> HomeContent(navFocusRequester = mainFocusRequester)
            //                     DrawerItem.UGC.displayName -> UgcContent(navFocusRequester = ugcFocusRequester)
            //                     DrawerItem.PGC.displayName -> PgcContent(navFocusRequester = pgcFocusRequester)
            //                     DrawerItem.Search.displayName -> SearchInputScreen(defaultFocusRequester = searchFocusRequester)
            //                     else -> {}
            //                 }
            //             }
            //         }
            //     }
            // }
        }
        AnimatedVisibility(
            visible = showUserPanel,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.Center),
                    visible = showUserPanel,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    UserPanel(
                        modifier = Modifier
                            .padding(12.dp),
                        username = userViewModel.username,
                        face = userViewModel.face,
                        onHide = { showUserPanel = false },
                        onGoMy = {
                            context.startActivity(Intent(context, UserInfoActivity::class.java))
                        },
                        onGoHistory = {
                            context.startActivity(Intent(context, HistoryActivity::class.java))
                        },
                        onGoFavorite = {
                            context.startActivity(Intent(context, FavoriteActivity::class.java))
                        },
                        onGoFollowing = {
                            context.startActivity(
                                Intent(
                                    context,
                                    FollowingSeasonActivity::class.java
                                )
                            )
                        },
                        onGoLater = {
                            context.startActivity(Intent(context, ToViewActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

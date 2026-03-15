package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.LauncherActivity
import dev.aaa1115910.bv.entity.InterfaceMode
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.tv.component.PgcTopNavItem
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.UgcTopNavItem
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.tv.component.settings.SettingNumberListItem
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.component.HomeTopNavItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.tv.util.NavItemConfig
import dev.aaa1115910.bv.tv.util.moveNavItemToFirstAndUnhide
import dev.aaa1115910.bv.tv.util.parseNavItemsOrderToConfig
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import kotlin.math.roundToInt

@Composable
fun UISetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showDensityDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var showInterfaceModeDialog by remember { mutableStateOf(false) }
    var showDefaultHomeTabDialog by remember { mutableStateOf(false) }
    var showHomeNavItemsDialog by remember { mutableStateOf(false) }
    var showUgcNavItemsDialog by remember { mutableStateOf(false) }
    var showPgcNavItemsDialog by remember { mutableStateOf(false) }
    val density by Prefs.densityFlow.collectAsState(context.resources.displayMetrics.widthPixels / 960f)
    val themeType by Prefs.themeTypeFlow.collectAsState(Prefs.themeType)
    val interfaceMode = Prefs.interfaceMode
    var defaultHomeTab by remember { mutableStateOf(HomeTopNavItem.entries.getOrElse(Prefs.defaultHomeTab) { HomeTopNavItem.Recommend }) }
    var showUGCVideoInfo by remember { mutableStateOf(Prefs.showUGCVideoInfo) }
    var videoInfoHistoryIncludeFromPlayer by remember { mutableStateOf(Prefs.videoInfoHistoryIncludeFromPlayer) }
    var ugcVideoInfoHistoryCount by remember { mutableIntStateOf(Prefs.ugcVideoInfoHistoryCount) }
    var ugcVideoPlayerHistoryCount by remember { mutableIntStateOf(Prefs.ugcVideoPlayerHistoryCount) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SettingsMenuNavItem.UI.getDisplayName(context),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_interface_mode_title),
                        supportText = stringResource(R.string.settings_ui_interface_mode_text),
                        valueText = interfaceMode.getDisplayName(context),
                        onClick = { showInterfaceModeDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_density_title),
                        supportText = stringResource(R.string.settings_ui_density_text),
                        valueText = density.toString(),
                        onClick = { showDensityDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_theme_type_title),
                        supportText = stringResource(R.string.settings_ui_theme_type_text),
                        valueText = themeType.getDisplayName(context),
                        onClick = { showThemeTypeDialog = true }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_show_ugc_video_info_title),
                        supportText = stringResource(R.string.settings_show_ugc_video_info_text),
                        checked = showUGCVideoInfo,
                        onCheckedChange = {
                            showUGCVideoInfo = it
                            Prefs.showUGCVideoInfo = it
                        }
                    )
                }
                if (showUGCVideoInfo) {
                    item {
                        SettingNumberListItem(
                            title = stringResource(R.string.settings_ui_ugc_video_info_history_count_title),
                            supportText = stringResource(R.string.settings_ui_ugc_video_info_history_count_text),
                            value = ugcVideoInfoHistoryCount.toDouble(),
                            minValue = 1.0,
                            maxValue = 10.0,
                            isInteger = true,
                            step = 1.0,
                            onValueChange = {
                                ugcVideoInfoHistoryCount = it.toInt()
                                Prefs.ugcVideoInfoHistoryCount = it.toInt()
                            }
                        )
                    }
                    item {
                        SettingSwitchListItem(
                            title = stringResource(R.string.settings_ui_video_info_history_include_from_player_title),
                            supportText = stringResource(R.string.settings_ui_video_info_history_include_from_player_text),
                            checked = videoInfoHistoryIncludeFromPlayer,
                            onCheckedChange = {
                                videoInfoHistoryIncludeFromPlayer = it
                                Prefs.videoInfoHistoryIncludeFromPlayer = it
                            }
                        )
                    }
                }
                if (!showUGCVideoInfo) {
                    item {
                        SettingNumberListItem(
                            title = stringResource(R.string.settings_ui_ugc_video_player_history_count_title),
                            supportText = stringResource(R.string.settings_ui_ugc_video_player_history_count_text),
                            value = ugcVideoPlayerHistoryCount.toDouble(),
                            minValue = 1.0,
                            maxValue = 10.0,
                            isInteger = true,
                            step = 1.0,
                            onValueChange = {
                                ugcVideoPlayerHistoryCount = it.toInt()
                                Prefs.ugcVideoPlayerHistoryCount = it.toInt()
                            }
                        )
                    }
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_default_home_tab_title),
                        supportText = stringResource(R.string.settings_ui_default_home_tab_text),
                        valueText = defaultHomeTab.getDisplayName(context),
                        onClick = { showDefaultHomeTabDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_home_nav_items_title),
                        supportText = stringResource(R.string.settings_ui_home_nav_items_text),
                        onClick = { showHomeNavItemsDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_ugc_nav_items_title),
                        supportText = stringResource(R.string.settings_ui_ugc_nav_items_text),
                        onClick = { showUgcNavItemsDialog = true }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_pgc_nav_items_title),
                        supportText = stringResource(R.string.settings_ui_pgc_nav_items_text),
                        onClick = { showPgcNavItemsDialog = true }
                    )
                }
            }
        }
    }

    UIDensityDialog(
        show = showDensityDialog,
        onHideDialog = { showDensityDialog = false },
        density = density,
        onDensityChange = { Prefs.density = it }
    )

    ThemeTypeDialog(
        show = showThemeTypeDialog,
        onHideDialog = { showThemeTypeDialog = false },
        themeType = themeType,
        onThemeTypeChange = { Prefs.themeType = it }
    )

    InterfaceModeDialog(
        show = showInterfaceModeDialog,
        onHideDialog = { showInterfaceModeDialog = false },
        interfaceMode = interfaceMode,
        onInterfaceModeChange = {
            if (it != interfaceMode) {
                Prefs.interfaceMode = it
                LauncherActivity.actionRestart(context)
            }
        }
    )

    DefaultHomeTabDialog(
        show = showDefaultHomeTabDialog,
        onHideDialog = { showDefaultHomeTabDialog = false },
        defaultHomeTab = defaultHomeTab,
        onDefaultHomeTabChange = {
            defaultHomeTab = it
            Prefs.defaultHomeTab = it.ordinal

            // 更新排序配置：将新的默认标签移到第一位，并取消隐藏
            val currentOrder = Prefs.homeNavItemsOrder
            val updatedOrder = moveNavItemToFirstAndUnhide(currentOrder, it.ordinal, HomeTopNavItem.entries.size)
            Prefs.homeNavItemsOrder = updatedOrder
        }
    )

    HomeNavItemsEditDialog(
        show = showHomeNavItemsDialog,
        onHideDialog = { showHomeNavItemsDialog = false },
        initialOrderString = Prefs.homeNavItemsOrder
    )

    UgcNavItemsEditDialog(
        show = showUgcNavItemsDialog,
        onHideDialog = { showUgcNavItemsDialog = false },
        initialOrderString = Prefs.ugcNavItemsOrder
    )

    PgcNavItemsEditDialog(
        show = showPgcNavItemsDialog,
        onHideDialog = { showPgcNavItemsDialog = false },
        initialOrderString = Prefs.pgcNavItemsOrder
    )
}

@Composable
private fun UIDensityDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    density: Float,
    onDensityChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val defaultDensity by remember { mutableFloatStateOf(context.resources.displayMetrics.widthPixels / 960f) }

    LaunchedEffect(show) {
        if (show) focusRequester.requestFocus(scope)
    }

    // 这里得采用固定的 Density，否则会导致更改 Density 时，对话框反复重新加载
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = defaultDensity,
            fontScale = LocalDensity.current.fontScale
        )
    ) {
        if (show) {
            TvAlertDialog(
                modifier = modifier,
                onDismissRequest = { onHideDialog() },
                title = { Text(text = stringResource(R.string.settings_ui_density_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .fillMaxWidth()
                            .onPreviewKeyEvent {
                                if (it.key == Key.DirectionUp || it.key == Key.DirectionDown) {
                                    if (it.type == KeyEventType.KeyDown) {
                                        var newDensity = if (it.key == Key.DirectionUp)
                                            density + 0.1f else density - 0.1f
                                        newDensity = (newDensity * 10).roundToInt() / 10f
                                        if (newDensity < 0.5f) newDensity = 0.5f
                                        if (newDensity > 5f) newDensity = 5f
                                        onDensityChange(newDensity)
                                    }
                                }
                                false
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = null)
                        Text(text = "$density")
                        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun ThemeTypeDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    themeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = stringResource(R.string.settings_ui_theme_type_title)) },
            text = {
                Column {
                    ThemeType.entries.forEach {
                        ListItem(
                            selected = themeType == it,
                            onClick = { onThemeTypeChange(it) },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = themeType == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun InterfaceModeDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    interfaceMode: InterfaceMode,
    onInterfaceModeChange: (InterfaceMode) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = stringResource(R.string.settings_ui_interface_mode_title)) },
            text = {
                Column {
                    InterfaceMode.entries.forEach {
                        ListItem(
                            selected = interfaceMode == it,
                            onClick = { onInterfaceModeChange(it) },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = interfaceMode == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Preview
@Composable
fun UIDensityDialogPreview() {
    val show by remember { mutableStateOf(true) }
    var density by remember { mutableFloatStateOf(1.0f) }

    BVTheme {
        UIDensityDialog(
            show = show,
            onHideDialog = {},
            density = density,
            onDensityChange = { density = it }
        )
    }
}

@Preview
@Composable
private fun ThemeTypeDialogPreview() {
    val show by remember { mutableStateOf(true) }
    val themeType by remember { mutableStateOf(ThemeType.Auto) }

    BVTheme {
        ThemeTypeDialog(
            show = show,
            onHideDialog = {},
            themeType = themeType,
            onThemeTypeChange = {}
        )
    }
}

@Composable
fun DefaultHomeTabDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    defaultHomeTab: HomeTopNavItem,
    onDefaultHomeTabChange: (HomeTopNavItem) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = stringResource(R.string.settings_ui_default_home_tab_title)) },
            text = {
                Column {
                    HomeTopNavItem.entries.forEach {
                        ListItem(
                            selected = defaultHomeTab == it,
                            onClick = { onDefaultHomeTabChange(it) },
                            headlineContent = {
                                Text(text = it.getDisplayName(LocalContext.current))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = defaultHomeTab == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun HomeNavItemsEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // 解析初始配置（按显示顺序）
    val initialConfigs = remember(initialOrderString) {
        parseNavItemsOrderToConfig(initialOrderString)
    }

    // 当前配置状态
    var navConfigs by remember { mutableStateOf(initialConfigs) }

    // 当前选中的索引
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(show) {
        if (show) focusRequester.requestFocus(scope)
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            // 关闭时自动保存
            saveNavConfigs(navConfigs)
            onHideDialog()
        },
        title = { Text(text = stringResource(R.string.settings_ui_home_nav_items_title)) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.DirectionLeft -> {
                                    // 向左移动：与上一个元素交换位置
                                    // 但第一个元素（默认标签）不能向左移动
                                    if (selectedIndex > 0) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex - 1]
                                            this[selectedIndex - 1] = temp
                                        }
                                        selectedIndex--
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    // 向右移动：与下一个元素交换位置
                                    if (selectedIndex < navConfigs.size - 1) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex + 1]
                                            this[selectedIndex + 1] = temp
                                        }
                                        selectedIndex++
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    // 向上选择
                                    if (selectedIndex > 0) selectedIndex--
                                    true
                                }
                                Key.DirectionDown -> {
                                    // 向下选择
                                    if (selectedIndex < navConfigs.size - 1) selectedIndex++
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    // 确认键：切换隐藏状态（除了默认首页标签）
                                    val config = navConfigs[selectedIndex]
                                    val defaultHomeTabOrdinal = Prefs.defaultHomeTab
                                    if (config.ordinal != defaultHomeTabOrdinal) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            this[selectedIndex] = config.copy(hidden = !config.hidden)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        false
                    }
            ) {
                // 提示文字
                Text(
                    text = stringResource(R.string.settings_ui_home_nav_items_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                navConfigs.forEachIndexed { index, config ->
                    val navItem = HomeTopNavItem.entries.getOrNull(config.ordinal)
                    if (navItem != null) {
                        val isSelected = index == selectedIndex
                        val isDefaultHomeTab = config.ordinal == Prefs.defaultHomeTab

                        NavItemEditRow(
                            title = navItem.getDisplayName(LocalContext.current),
                            hidden = config.hidden,
                            selected = isSelected,
                            showDefaultTag = isDefaultHomeTab,
                            onFocus = { selectedIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun UgcNavItemsEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val initialConfigs = remember(initialOrderString) {
        parseNavItemsOrderToConfig(initialOrderString, UgcTopNavItem.entries.size)
    }
    var navConfigs by remember { mutableStateOf(initialConfigs) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex, navConfigs.size) {
        if (navConfigs.isEmpty()) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset
        val viewportSize = viewportEnd - viewportStart
        if (viewportSize <= 0) return@LaunchedEffect

        val visibleItems = layoutInfo.visibleItemsInfo
        val visibleCount = visibleItems.size
        if (visibleCount <= 0) return@LaunchedEffect

        val firstVisible = listState.firstVisibleItemIndex
        val selectedItemInfo = visibleItems.firstOrNull { it.index == selectedIndex }

        // 1) 先保证“选中项完全可见”（尤其是最后一项，避免底部被截断）
        if (selectedItemInfo != null) {
            val itemStart = selectedItemInfo.offset
            val itemEnd = itemStart + selectedItemInfo.size

            if (itemStart < viewportStart) {
                listState.animateScrollToItem(index = selectedIndex, scrollOffset = 0)
                return@LaunchedEffect
            }

            if (itemEnd > viewportEnd) {
                val bottomAlignedOffset = (viewportSize - selectedItemInfo.size).coerceAtLeast(0)
                listState.animateScrollToItem(index = selectedIndex, scrollOffset = bottomAlignedOffset)
                return@LaunchedEffect
            }

            // 2) 完全可见时，再按“到可见列表中间才开始滚动”的规则微调
            val middleIndex = firstVisible + visibleCount / 2
            if (selectedIndex > middleIndex) {
                val maxFirstVisible = (navConfigs.size - visibleCount).coerceAtLeast(0)
                val targetFirstVisible = (selectedIndex - visibleCount / 2)
                    .coerceIn(0, maxFirstVisible)
                if (targetFirstVisible != firstVisible) {
                    listState.animateScrollToItem(index = targetFirstVisible)
                }
            }
            return@LaunchedEffect
        }

        // 3) 选中项不在可见区域：直接滚动到“中间位置”附近
        val maxFirstVisible = (navConfigs.size - visibleCount).coerceAtLeast(0)
        val targetFirstVisible = (selectedIndex - visibleCount / 2)
            .coerceIn(0, maxFirstVisible)
        if (targetFirstVisible != firstVisible) {
            listState.animateScrollToItem(index = targetFirstVisible)
        }
    }

    LaunchedEffect(show) {
        if (show) focusRequester.requestFocus(scope)
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            saveUgcNavConfigs(navConfigs)
            onHideDialog()
        },
        title = { Text(text = stringResource(R.string.settings_ui_ugc_nav_items_title)) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.DirectionLeft -> {
                                    if (selectedIndex > 0) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex - 1]
                                            this[selectedIndex - 1] = temp
                                        }
                                        selectedIndex--
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (selectedIndex < navConfigs.size - 1) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex + 1]
                                            this[selectedIndex + 1] = temp
                                        }
                                        selectedIndex++
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (selectedIndex > 0) selectedIndex--
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (selectedIndex < navConfigs.size - 1) selectedIndex++
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    val config = navConfigs[selectedIndex]
                                    navConfigs = navConfigs.toMutableList().apply {
                                        this[selectedIndex] = config.copy(hidden = !config.hidden)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        false
                    }
            ) {
                Text(
                    text = stringResource(R.string.settings_ui_ugc_nav_items_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = navConfigs,
                        key = { index, config -> "$index-nav-${config.ordinal}" }
                    ) { index, config ->
                        val navItem = UgcTopNavItem.entries.getOrNull(config.ordinal) ?: return@itemsIndexed
                        NavItemEditRow(
                            title = navItem.getDisplayName(LocalContext.current),
                            hidden = config.hidden,
                            selected = index == selectedIndex,
                            onFocus = { selectedIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun PgcNavItemsEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val initialConfigs = remember(initialOrderString) {
        parseNavItemsOrderToConfig(initialOrderString, PgcTopNavItem.entries.size)
    }
    var navConfigs by remember { mutableStateOf(initialConfigs) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(show) {
        if (show) focusRequester.requestFocus(scope)
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            savePgcNavConfigs(navConfigs)
            onHideDialog()
        },
        title = { Text(text = stringResource(R.string.settings_ui_pgc_nav_items_title)) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.DirectionLeft -> {
                                    if (selectedIndex > 0) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex - 1]
                                            this[selectedIndex - 1] = temp
                                        }
                                        selectedIndex--
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (selectedIndex < navConfigs.size - 1) {
                                        navConfigs = navConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex + 1]
                                            this[selectedIndex + 1] = temp
                                        }
                                        selectedIndex++
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (selectedIndex > 0) selectedIndex--
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (selectedIndex < navConfigs.size - 1) selectedIndex++
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    val config = navConfigs[selectedIndex]
                                    navConfigs = navConfigs.toMutableList().apply {
                                        this[selectedIndex] = config.copy(hidden = !config.hidden)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        false
                    }
            ) {
                Text(
                    text = stringResource(R.string.settings_ui_pgc_nav_items_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                navConfigs.forEachIndexed { index, config ->
                    val navItem = PgcTopNavItem.entries.getOrNull(config.ordinal) ?: return@forEachIndexed
                    NavItemEditRow(
                        title = navItem.getDisplayName(LocalContext.current),
                        hidden = config.hidden,
                        selected = index == selectedIndex,
                        onFocus = { selectedIndex = index }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun NavItemEditRow(
    title: String,
    hidden: Boolean,
    selected: Boolean,
    showDefaultTag: Boolean = false,
    onFocus: () -> Unit
) {
    EditableNavRow(
        selected = selected,
        onFocus = onFocus,
        headline = {
            Text(
                text = title,
                textDecoration = if (hidden) TextDecoration.LineThrough else null,
                color = if (selected) {
                    MaterialTheme.colorScheme.background
                } else if (hidden) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showDefaultTag) {
                    Text(
                        text = stringResource(R.string.settings_ui_home_nav_default_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
                    )
                }

                // 隐藏状态标记
                if (hidden) {
                    Text(
                        text = stringResource(R.string.settings_ui_home_nav_hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_ui_home_nav_visible),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun EditableNavRow(
    selected: Boolean,
    onFocus: () -> Unit,
    headline: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    val shape = remember { RoundedCornerShape(12.dp) }
    val bgColor = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.hasFocus) onFocus() }
            .background(color = bgColor, shape = shape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f)) {
            headline()
        }
        trailing()
    }
}

/**
 * 保存导航项配置到 Prefs
 * 默认标签强制不隐藏
 */
private fun saveNavConfigs(navConfigs: List<NavItemConfig>) {
    val defaultTabOrdinal = Prefs.defaultHomeTab
    val finalOrderString = navConfigs.joinToString(",") { config ->
        val shouldHide = if (config.ordinal == defaultTabOrdinal) {
            false  // 默认标签强制不隐藏
        } else {
            config.hidden
        }
        if (shouldHide) "-${config.ordinal}" else "${config.ordinal}"
    }
    Prefs.homeNavItemsOrder = finalOrderString
}

private fun saveUgcNavConfigs(navConfigs: List<NavItemConfig>) {
    val finalOrderString = navConfigs.joinToString(",") { config ->
        if (config.hidden) "-${config.ordinal}" else "${config.ordinal}"
    }
    Prefs.ugcNavItemsOrder = finalOrderString
}

private fun savePgcNavConfigs(navConfigs: List<NavItemConfig>) {
    val finalOrderString = navConfigs.joinToString(",") { config ->
        if (config.hidden) "-${config.ordinal}" else "${config.ordinal}"
    }
    Prefs.pgcNavItemsOrder = finalOrderString
}

package dev.aaa1115910.bv.tv.screens.search

import android.content.Context
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.Partition
import dev.aaa1115910.bv.util.PartitionUtil

@Composable
fun SearchResultVideoFilter(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideFilter: () -> Unit,
    selectedOrder: SearchFilterOrderType,
    selectedDuration: SearchFilterDuration,
    selectedPartition: Partition?,
    selectedChildPartition: Partition?,
    onSelectedOrderChange: (SearchFilterOrderType) -> Unit,
    onSelectedDurationChange: (SearchFilterDuration) -> Unit,
    onSelectedPartitionChange: (Partition?) -> Unit,
    onSelectedChildPartitionChange: (Partition?) -> Unit,
) {
    val context = LocalContext.current
    val partitions = remember { PartitionUtil.partitions }
    val defaultFocusRequester = remember { FocusRequester() }
    val durationFocusRequester = remember { FocusRequester() }
    val partitionFocusRequester = remember { FocusRequester() }
    val partitionChildFocusRequester = remember { FocusRequester() }

    // 用于防止对话框刚打开时误触发点击事件
    var isDialogJustOpened by remember { mutableStateOf(false) }

    val filterRowSpace = 8.dp

    LaunchedEffect(show) {
        if (show) {
            isDialogJustOpened = true
            // 延迟请求焦点，避免打开对话框的按键事件被新获得焦点的组件消费
            delay(100)
            defaultFocusRequester.requestFocus()
            // 等待一段时间后才允许点击事件
            delay(200)
            isDialogJustOpened = false
        }
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier
                .fillMaxWidth(0.8f),
            onDismissRequest = onHideFilter,
            title = { Text(text = stringResource(R.string.filter_dialog_title)) },
            text = {
                Column {
                    LazyRow(
                        modifier = Modifier.onPreviewKeyEvent {
                            if (it.key == Key.DirectionDown) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    durationFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            false
                        },
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                    ) {
                        items(items = SearchFilterOrderType.webFilters) { orderType ->
                            FilterDialogFilterChip(
                                focusRequester = defaultFocusRequester,
                                selected = orderType == selectedOrder,
                                onClick = { onSelectedOrderChange(orderType) },
                                label = { Text(text = orderType.getDisplayName(context)) },
                                enabled = !isDialogJustOpened
                            )
                        }
                    }
                    LazyRow(
                        modifier = Modifier.onPreviewKeyEvent {
                            if (it.key == Key.DirectionDown) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    partitionFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            if (it.key == Key.DirectionUp) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    defaultFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            false
                        },
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                    ) {
                        items(items = SearchFilterDuration.entries) { duration ->
                            FilterDialogFilterChip(
                                focusRequester = durationFocusRequester,
                                selected = duration == selectedDuration,
                                onClick = { onSelectedDurationChange(duration) },
                                label = { Text(text = duration.getDisplayName(context)) },
                                enabled = !isDialogJustOpened
                            )
                        }
                    }
                    LazyRow(
                        modifier = Modifier.onPreviewKeyEvent {
                            if (it.key == Key.DirectionDown) {
                                if (selectedChildPartition == null) return@onPreviewKeyEvent false
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    partitionChildFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            if (it.key == Key.DirectionUp) {
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    durationFocusRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent true
                            }
                            false
                        },
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                    ) {
                        item {
                            FilterDialogFilterChip(
                                focusRequester = partitionFocusRequester,
                                selected = null == selectedPartition,
                                onClick = {
                                    onSelectedPartitionChange(null)
                                    onSelectedChildPartitionChange(null)
                                },
                                label = { Text(text = "全部分区") },
                                enabled = !isDialogJustOpened
                            )
                        }
                        items(items = partitions) { partition ->
                            FilterDialogFilterChip(
                                focusRequester = partitionFocusRequester,
                                selected = partition == selectedPartition,
                                onClick = {
                                    onSelectedPartitionChange(partition)
                                    onSelectedChildPartitionChange(null)
                                },
                                label = { Text(text = partition.strRes) },
                                enabled = !isDialogJustOpened
                            )
                        }
                    }
                    AnimatedVisibility(visible = selectedPartition != null) {
                        LazyRow(
                            modifier = Modifier.onPreviewKeyEvent {
                                if (it.key == Key.DirectionUp) {
                                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                        partitionFocusRequester.requestFocus()
                                        return@onPreviewKeyEvent true
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                false
                            },
                            horizontalArrangement = Arrangement.spacedBy(filterRowSpace)
                        ) {
                            items(items = selectedPartition?.children ?: emptyList()) { partition ->
                                FilterDialogFilterChip(
                                    focusRequester = partitionChildFocusRequester,
                                    selected = partition == selectedChildPartition,
                                    onClick = {
                                        onSelectedChildPartitionChange(
                                            if (partition != selectedChildPartition) partition
                                            else null
                                        )
                                    },
                                    label = { Text(text = partition.strRes) },
                                    enabled = !isDialogJustOpened
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }

    BackHandler(
        enabled = show,
        onBack = onHideFilter
    )
}

@Composable
private fun FilterDialogFilterChip(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    enabled: Boolean = true
) {
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequesterModifier = if (selected)
        modifier.focusRequester(focusRequester)
    else modifier

    FilterChip(
        modifier = focusRequesterModifier.onFocusChanged { hasFocus = it.hasFocus },
        selected = selected,
        onClick = { if (enabled) onClick() },
        label = label,
        border = if (hasFocus) FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.border,
            borderWidth = 2.dp,
            selectedBorderColor = MaterialTheme.colorScheme.border,
            selectedBorderWidth = 2.dp
        )
        else FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected
        )
    )
}

fun SearchFilterOrderType.getDisplayName(context: Context) = when (this) {
    SearchFilterOrderType.ComprehensiveSort -> context.getString(R.string.search_result_filter_order_type_comprehensive_sort)
    SearchFilterOrderType.MostClicks -> context.getString(R.string.search_result_filter_order_type_most_clicks)
    SearchFilterOrderType.LatestPublish -> context.getString(R.string.search_result_filter_order_type_latest_publish)
    SearchFilterOrderType.MostDanmaku -> context.getString(R.string.search_result_filter_order_type_most_danmaku)
    SearchFilterOrderType.MostFavorites -> context.getString(R.string.search_result_filter_order_type_most_favorites)
    SearchFilterOrderType.MostComment -> "最多评论"
    SearchFilterOrderType.MostLikes -> "最多点赞"
}

fun SearchFilterDuration.getDisplayName(context: Context) = when (this) {
    SearchFilterDuration.All -> context.getString(R.string.search_result_filter_duration_all)
    SearchFilterDuration.LessThan10Minutes -> context.getString(R.string.search_result_filter_duration_less_than_10)
    SearchFilterDuration.Between10And30Minutes -> context.getString(R.string.search_result_filter_duration_10_to_30)
    SearchFilterDuration.Between30And60Minutes -> context.getString(R.string.search_result_filter_duration_30_to_60)
    SearchFilterDuration.MoreThan60Minutes -> context.getString(R.string.search_result_filter_duration_more_than_60)
}
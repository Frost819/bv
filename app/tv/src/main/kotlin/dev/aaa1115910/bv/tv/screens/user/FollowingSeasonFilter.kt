package dev.aaa1115910.bv.tv.screens.user

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonStatus
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.ifElse

@Composable
fun FollowingSeasonFilter(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideFilter: () -> Unit,
    selectedType: FollowingSeasonType,
    selectedStatus: FollowingSeasonStatus,
    onSelectedTypeChange: (FollowingSeasonType) -> Unit,
    onSelectedStatusChange: (FollowingSeasonStatus) -> Unit
) {
    val context = LocalContext.current
    val row1FocusRequester = remember { FocusRequester() }
    val row2FocusRequester = remember { FocusRequester() }

    val filterRowSpace = 8.dp

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = onHideFilter,
            title = { Text(text = stringResource(R.string.filter_dialog_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(filterRowSpace)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .focusRestorer(row1FocusRequester),
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace),
                        contentPadding = PaddingValues(horizontal = filterRowSpace)
                    ) {
                        itemsIndexed(
                            items = FollowingSeasonType.entries,
                            key = { index, type -> "$index-type-${type.name}" }
                        ) { _, type ->
                            FilterDialogFilterChip(
                                modifier = Modifier
                                    .ifElse(
                                        type == selectedType,
                                        Modifier.focusRequester(row1FocusRequester)
                                    ),
                                selected = type == selectedType,
                                onClick = { onSelectedTypeChange(type) },
                                label = { Text(text = type.getDisplayName(context)) },
                            )
                        }
                    }
                    LazyRow(
                        modifier = Modifier
                            .focusRestorer(row2FocusRequester),
                        horizontalArrangement = Arrangement.spacedBy(filterRowSpace),
                        contentPadding = PaddingValues(horizontal = filterRowSpace)
                    ) {
                        itemsIndexed(
                            items = FollowingSeasonStatus.entries,
                            key = { index, status -> "$index-status-${status.name}" }
                        ) { _, status ->
                            FilterDialogFilterChip(
                                modifier = Modifier
                                    .ifElse(
                                        status == selectedStatus,
                                        Modifier.focusRequester(row2FocusRequester)
                                    ),
                                selected = status == selectedStatus,
                                onClick = { onSelectedStatusChange(status) },
                                label = { Text(text = status.getDisplayName(context)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    BackHandler(
        enabled = show,
        onBack = onHideFilter
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterDialogFilterChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        content = label,
        leadingIcon = {
            Row {
                AnimatedVisibility(visible = selected) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Rounded.Done,
                        contentDescription = null
                    )
                }
            }
        }
    )
}
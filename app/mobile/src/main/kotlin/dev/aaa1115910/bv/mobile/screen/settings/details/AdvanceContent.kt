package dev.aaa1115910.bv.mobile.screen.settings.details

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.LauncherActivity
import dev.aaa1115910.bv.entity.InterfaceMode
import dev.aaa1115910.bv.mobile.component.preferences.items.radioPreference
import dev.aaa1115910.bv.mobile.component.preferences.preferenceGroups
import dev.aaa1115910.bv.mobile.theme.BVMobileTheme
import dev.aaa1115910.bv.util.PrefKeys
import dev.aaa1115910.bv.util.Prefs

@Composable
fun AdvanceContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interfaceMode = Prefs.interfaceMode
    val interfaceModeTitle = stringResource(R.string.settings_ui_interface_mode_title)
    val apiTitle = stringResource(R.string.settings_item_api)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        preferenceGroups(
            null to {
                radioPreference(
                    title = interfaceModeTitle,
                    value = interfaceMode,
                    values = InterfaceMode.entries.associateWith { it.getDisplayName(context) },
                    onValueChange = {
                        if (it != interfaceMode) {
                            Prefs.interfaceMode = it
                            LauncherActivity.actionRestart(context)
                        }
                    }
                )
                radioPreference(
                    title = apiTitle,
                    prefReq = PrefKeys.prefApiTypeRequest,
                    values = ApiType.entries.associate { it.ordinal to it.name }
                        .toSortedMap { a, b -> a.compareTo(b) }
                )
            }
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdvanceContentPreview() {
    BVMobileTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            AdvanceContent(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
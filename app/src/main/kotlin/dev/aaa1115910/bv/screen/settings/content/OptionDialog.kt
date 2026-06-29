package dev.aaa1115910.bv.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.aaa1115910.bv.component.settings.SettingsMenuSelectItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionDialog(
    modifier: Modifier = Modifier,
    options: Array<T>,
    selectedOption: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    getDisplayName: (T) -> String
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeightDp = with(density) {
        (windowInfo.containerSize.height * 0.6f).toDp()
    }

    BasicAlertDialog(
        modifier = modifier.padding(vertical = 24.dp),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            LazyColumn(
                modifier = Modifier
                    .wrapContentHeight()
                    .heightIn(max = maxHeightDp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options.toList()) { option ->
                    SettingsMenuSelectItem(
                        text = getDisplayName(option),
                        selected = selectedOption == option,
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }

}

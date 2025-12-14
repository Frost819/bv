package dev.aaa1115910.bv.screen.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.rememberDrawerState
import coil.compose.AsyncImage
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isKeyDown

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LeftNaviContent(
    modifier: Modifier = Modifier,
    isLogin: Boolean = false,
    avatar: String = "",
    onLeftNaviItemChanged: (LeftNaviItem) -> Unit,
    onOpenSettings: () -> Unit,
    onShowUserPanel: () -> Unit,
    onFocusToContent: () -> Unit,
    onLogin: () -> Unit
) {
    var selectedItem by remember { mutableStateOf(LeftNaviItem.Home) }

    LaunchedEffect(selectedItem) {
        onLeftNaviItemChanged(selectedItem)
    }

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isDpadRight()) {
                    if (keyEvent.isKeyDown()) {
                        onFocusToContent()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        containerColor = Color.White.copy(alpha = 0.05f),
    ) {
        var userIsFocused by remember { mutableStateOf(false) }
        NavigationRailItem(
            modifier = Modifier.onFocusChanged {
                userIsFocused = it.hasFocus
            },
            onClick = {
                if (isLogin) {
                    onShowUserPanel()
                } else {
                    onLogin()
                }
            },
            selected = userIsFocused,
            icon = {
                if (isLogin) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        colors = SurfaceDefaults.colors(
                            containerColor = Color.Gray
                        )
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds
                        )
                    }
                } else {
                    Icon(
                        imageVector = LeftNaviItem.User.displayIcon,
                        contentDescription = null
                    )
                }
            }
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            listOf(
                LeftNaviItem.Search,
                LeftNaviItem.Personal,
                LeftNaviItem.Home,
                LeftNaviItem.UGC,
                LeftNaviItem.PGC,
                LeftNaviItem.Live,
            ).forEach { item ->
                var isFocused by remember { mutableStateOf(false) }
                val indicatorColor by animateColorAsState(
                    targetValue = if (item == selectedItem) {
                        MaterialTheme.colorScheme.border
                    } else Color.Transparent,
                    label = "selectionIndicatorColor"
                )
                NavigationRailItem(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.hasFocus }
                        .selectionIndicator(
                            animateColorAsState(
                                targetValue = indicatorColor,
                                label = "selectionIndicatorColor"
                            ).value
                        ),
                    onClick = { selectedItem = item },
                    selected = isFocused,
                    icon = {
                        Icon(
                            imageVector = item.displayIcon,
                            contentDescription = null
                        )
                    }
                )
            }
        }
        var settingsIsFocused by remember { mutableStateOf(false) }
        NavigationRailItem(
            modifier = Modifier.onFocusChanged {
                settingsIsFocused = it.hasFocus
            },
            onClick = onOpenSettings,
            selected = settingsIsFocused,
            icon = {
                Icon(
                    imageVector = LeftNaviItem.Settings.displayIcon,
                    contentDescription = null
                )
            }
        )
    }
}

enum class LeftNaviItem(
    val displayIcon: ImageVector
) {
    User(displayIcon = Icons.Default.AccountCircle),
    Search(displayIcon = Icons.Default.Search),
    Personal(displayIcon = Icons.Default.Person),
    Home(displayIcon = Icons.Default.Home),
    UGC(displayIcon = Icons.Default.OndemandVideo),
    PGC(displayIcon = Icons.Default.Movie),
    Live(displayIcon = Icons.Default.LiveTv),
    Settings(displayIcon = Icons.Default.Settings), ;
}

fun Modifier.selectionIndicator(color: Color): Modifier {
    return this.drawBehind {
        val strokeWidth = 4.dp.toPx()
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(width = strokeWidth, height = size.height)
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LeftNaviContentPreview() {
    BVTheme {
        LeftNaviContent(
            onLeftNaviItemChanged = {},
            onOpenSettings = {},
            onShowUserPanel = {},
            onFocusToContent = {},
            onLogin = {}
        )
    }
}
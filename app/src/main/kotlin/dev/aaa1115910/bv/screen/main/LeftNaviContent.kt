package dev.aaa1115910.bv.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
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
    val focusManager = LocalFocusManager.current

    LaunchedEffect(selectedItem) {
        onLeftNaviItemChanged(selectedItem)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(vertical = 12.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isDpadRight() && keyEvent.isKeyDown()) {
                    focusManager.moveFocus(FocusDirection.Right)
                    return@onPreviewKeyEvent true
                }
                false
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // User avatar/icon
        NavIcon(
            icon = if (!isLogin) LeftNaviItem.User.displayIcon else null,
            avatarUrl = if (isLogin) avatar else null,
            isSelected = false,
            onClick = { if (isLogin) onShowUserPanel() else onLogin() }
        )

        // Main nav items
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            listOf(
                LeftNaviItem.Search,
                LeftNaviItem.Personal,
                LeftNaviItem.Home,
                LeftNaviItem.UGC,
                LeftNaviItem.PGC,
            ).forEach { item ->
                NavIcon(
                    icon = item.displayIcon,
                    isSelected = item == selectedItem,
                    onClick = { selectedItem = item }
                )
            }
        }

        // Settings
        NavIcon(
            icon = LeftNaviItem.Settings.displayIcon,
            isSelected = false,
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun NavIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    avatarUrl: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val pink = Color(0xFFFF69B4)

    Box(
        modifier = modifier
            .size(40.dp)
            .onFocusChanged { isFocused = it.hasFocus }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) pink else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .selectionIndicator(if (isSelected) pink else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        } else if (icon != null) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused || isSelected) pink else Color.White.copy(alpha = 0.8f)
            )
        }
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

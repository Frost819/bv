package dev.aaa1115910.bv.screen.settings.content

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.repositories.ChannelRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.settings.SpeedTestActivity
import dev.aaa1115910.bv.component.settings.SettingListItem
import dev.aaa1115910.bv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.entity.CdnOverrideCatalog
import dev.aaa1115910.bv.screen.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import org.koin.compose.getKoin

@Composable
fun NetworkSetting(
    modifier: Modifier = Modifier,
    channelRepository: ChannelRepository = getKoin().get()
) {
    val context = LocalContext.current
    var enableProxy by remember { mutableStateOf(Prefs.enableProxy) }
    var proxyHttpServer by remember { mutableStateOf(Prefs.proxyHttpServer) }
    var proxyGRPCServer by remember { mutableStateOf(Prefs.proxyGRPCServer) }
    var preferOfficialCdn by remember { mutableStateOf(Prefs.preferOfficialCdn) }
    var cdnOverrideHost by remember { mutableStateOf(Prefs.cdnOverrideHost) }
    var customCdnOverrideSelected by remember {
        mutableStateOf(
            Prefs.cdnOverrideHost.isNotBlank() &&
                CdnOverrideCatalog.regionForHost(Prefs.cdnOverrideHost) == CdnOverrideCatalog.customRegion
        )
    }
    var showProxyHttpServerEditDialog by remember { mutableStateOf(false) }
    var showProxyGRPCServerEditDialog by remember { mutableStateOf(false) }
    var showCdnOverrideRegionDialog by remember { mutableStateOf(false) }
    var showCdnOverrideNodeDialog by remember { mutableStateOf(false) }
    var showCdnOverrideCustomDialog by remember { mutableStateOf(false) }
    val cdnOverrideRegion = if (customCdnOverrideSelected) {
        CdnOverrideCatalog.customRegion
    } else {
        CdnOverrideCatalog.regionForHost(cdnOverrideHost)
    }
    val usingCustomCdnOverride = cdnOverrideRegion == CdnOverrideCatalog.customRegion
    val usingPresetCdnOverride = cdnOverrideRegion !in listOf(
        CdnOverrideCatalog.defaultRegion,
        CdnOverrideCatalog.customRegion
    )

    Box(
        modifier = modifier
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = SettingsMenuNavItem.Network.getDisplayName(context),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        SettingSwitchListItem(
                            title = stringResource(R.string.settings_network_enable_proxy_title),
                            supportText = stringResource(R.string.settings_network_enable_proxy_text),
                            checked = Prefs.enableProxy,
                            onCheckedChange = { enable ->
                                enableProxy = enable
                                Prefs.enableProxy = enable
                                if (enable) BVApp.instance?.initProxy()
                            }
                        )
                        AnimatedVisibility(visible = enableProxy) {
                            Column {
                                SettingListItem(
                                    modifier = Modifier.padding(top = 12.dp),
                                    title = stringResource(R.string.settings_network_proxy_http_server_title),
                                    supportText = if (proxyHttpServer.isBlank()) stringResource(R.string.settings_network_proxy_server_content_empty) else proxyHttpServer,
                                    onClick = { showProxyHttpServerEditDialog = true }
                                )
                                SettingListItem(
                                    modifier = Modifier.padding(top = 12.dp),
                                    title = stringResource(R.string.settings_network_proxy_grpc_server_title),
                                    supportText = if (proxyGRPCServer.isBlank()) stringResource(R.string.settings_network_proxy_server_content_empty) else proxyGRPCServer,
                                    onClick = { showProxyGRPCServerEditDialog = true }
                                )
                            }
                        }
                    }
                }

                item {
                    Column {
                        SettingSwitchListItem(
                            title = stringResource(R.string.settings_network_prefer_official_cdn_title),
                            supportText = stringResource(R.string.settings_network_prefer_official_cdn_text),
                            checked = preferOfficialCdn,
                            onCheckedChange = { enable ->
                                preferOfficialCdn = enable
                                Prefs.preferOfficialCdn = enable
                            }
                        )
                        Column {
                            SettingListItem(
                                modifier = Modifier.padding(top = 12.dp),
                                title = stringResource(R.string.settings_network_cdn_override_region_title),
                                supportText = stringResource(R.string.settings_network_cdn_override_text) +
                                    "\n" + stringResource(
                                        R.string.settings_network_cdn_override_current,
                                        cdnOverrideRegion
                                    ),
                                onClick = { showCdnOverrideRegionDialog = true }
                            )
                            AnimatedVisibility(visible = usingPresetCdnOverride) {
                                SettingListItem(
                                    modifier = Modifier.padding(top = 12.dp),
                                    title = stringResource(R.string.settings_network_cdn_override_node_title),
                                    supportText = CdnOverrideCatalog.displayNameForHost(cdnOverrideHost),
                                    onClick = { showCdnOverrideNodeDialog = true }
                                )
                            }
                            AnimatedVisibility(visible = usingCustomCdnOverride) {
                                SettingListItem(
                                    modifier = Modifier.padding(top = 12.dp),
                                    title = stringResource(R.string.settings_network_cdn_override_custom_title),
                                    supportText = if (cdnOverrideHost.isBlank()) {
                                        stringResource(R.string.settings_network_proxy_server_content_empty)
                                    } else {
                                        cdnOverrideHost
                                    },
                                    onClick = { showCdnOverrideCustomDialog = true }
                                )
                            }
                        }
                    }
                }

                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_network_test_title),
                        supportText = stringResource(R.string.settings_network_test_text),
                        onClick = {
                            context.startActivity(Intent(context, SpeedTestActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    ProxyServerEditDialog(
        show = showProxyHttpServerEditDialog,
        onHideDialog = { showProxyHttpServerEditDialog = false },
        title = stringResource(R.string.settings_network_proxy_http_server_title),
        proxyServer = proxyHttpServer,
        onProxyServerChange = {
            proxyHttpServer = it
            Prefs.proxyHttpServer = it
            BiliHttpProxyApi.createClient(it)
        }
    )
    ProxyServerEditDialog(
        show = showProxyGRPCServerEditDialog,
        onHideDialog = { showProxyGRPCServerEditDialog = false },
        title = stringResource(R.string.settings_network_proxy_grpc_server_title),
        proxyServer = proxyGRPCServer,
        onProxyServerChange = {
            proxyGRPCServer = it
            Prefs.proxyGRPCServer = it
            runCatching {
                channelRepository.initProxyChannel(
                    accessKey = Prefs.accessToken,
                    buvid = Prefs.buvid,
                    proxyServer = it
                )
            }
        }
    )

    if (showCdnOverrideRegionDialog) {
        OptionDialog(
            options = CdnOverrideCatalog.regions.toTypedArray(),
            selectedOption = cdnOverrideRegion,
            onDismiss = { showCdnOverrideRegionDialog = false },
            onSelect = { region ->
                when (region) {
                    CdnOverrideCatalog.defaultRegion -> {
                        customCdnOverrideSelected = false
                        cdnOverrideHost = ""
                        Prefs.cdnOverrideHost = ""
                    }

                    CdnOverrideCatalog.customRegion -> {
                        showCdnOverrideCustomDialog = true
                    }

                    else -> {
                        val firstNode = CdnOverrideCatalog.nodesForRegion(region).firstOrNull().orEmpty()
                        customCdnOverrideSelected = false
                        cdnOverrideHost = firstNode
                        Prefs.cdnOverrideHost = firstNode
                    }
                }
                showCdnOverrideRegionDialog = false
            },
            getDisplayName = { it }
        )
    }

    if (showCdnOverrideNodeDialog && CdnOverrideCatalog.nodesForRegion(cdnOverrideRegion).isNotEmpty()) {
        val nodes = CdnOverrideCatalog.nodesForRegion(cdnOverrideRegion)
        OptionDialog(
            options = nodes.toTypedArray(),
            selectedOption = cdnOverrideHost.takeIf { it in nodes } ?: nodes.first(),
            onDismiss = { showCdnOverrideNodeDialog = false },
            onSelect = {
                cdnOverrideHost = it
                Prefs.cdnOverrideHost = it
                showCdnOverrideNodeDialog = false
            },
            getDisplayName = { it }
        )
    }

    CdnOverrideHostEditDialog(
        show = showCdnOverrideCustomDialog,
        onHideDialog = { showCdnOverrideCustomDialog = false },
        cdnHost = if (customCdnOverrideSelected) cdnOverrideHost else "",
        onCdnHostChange = {
            val normalizedHost = CdnOverrideCatalog.normalizeHost(it)
            customCdnOverrideSelected = normalizedHost.isNotBlank()
            cdnOverrideHost = normalizedHost
            Prefs.cdnOverrideHost = normalizedHost
        }
    )
}

@Composable
fun CdnOverrideHostEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    cdnHost: String,
    onCdnHostChange: (String) -> Unit
) {
    var cdnHostString by remember(show) { mutableStateOf(cdnHost) }

    if (show) {
        AlertDialog(
            modifier = modifier,
            title = { Text(text = stringResource(R.string.settings_network_cdn_override_custom_title)) },
            text = {
                OutlinedTextField(
                    value = cdnHostString,
                    onValueChange = { cdnHostString = it },
                    singleLine = true,
                    maxLines = 1,
                    shape = MaterialTheme.shapes.medium,
                    placeholder = { Text(text = stringResource(R.string.settings_network_cdn_override_input_label)) }
                )
            },
            onDismissRequest = onHideDialog,
            confirmButton = {
                Button(onClick = {
                    CdnOverrideCatalog.normalizeHost(cdnHostString)
                        .takeIf { it.isNotBlank() }
                        ?.let(onCdnHostChange)
                    onHideDialog()
                }) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onHideDialog) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
fun ProxyServerEditDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    title: String,
    proxyServer: String,
    onProxyServerChange: (String) -> Unit
) {
    var proxyServerString by remember(show) { mutableStateOf(proxyServer) }

    if (show) {
        AlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = proxyServerString,
                        onValueChange = { proxyServerString = it },
                        singleLine = true,
                        maxLines = 1,
                        shape = MaterialTheme.shapes.medium,
                        placeholder = { Text(text = stringResource(R.string.proxy_server_edit_dialog_input_field_label)) }
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                        Text(
                            text = stringResource(R.string.proxy_server_edit_dialog_warning),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            onDismissRequest = onHideDialog,
            confirmButton = {
                Button(onClick = {
                    onProxyServerChange(
                        proxyServerString
                            .replace("\n", "")
                            .replace("https://", "")
                            .replace("http://", "")
                    )
                    onHideDialog()
                }) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onHideDialog) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }
}

@Preview
@Composable
fun ProxyServerEditDialogPreview() {
    BVTheme {
        ProxyServerEditDialog(
            show = true,
            onHideDialog = {},
            title = "title",
            proxyServer = "",
            onProxyServerChange = {}
        )
    }
}

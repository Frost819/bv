package dev.aaa1115910.bv.entity

object CdnOverrideCatalog {
    const val defaultRegion = "使用默认源"
    const val customRegion = "手动输入"

    val regionNodes = linkedMapOf(
        "上海" to listOf(
            "cn-sh-ct-01-01.bilivideo.com",
            "cn-sh-ct-01-06.bilivideo.com",
            "cn-sh-ct-01-13.bilivideo.com",
            "cn-sh-ct-01-15.bilivideo.com",
            "cn-sh-ct-01-23.bilivideo.com",
            "cn-sh-ct-01-24.bilivideo.com",
            "cn-sh-ct-01-35.bilivideo.com",
            "cn-sh-ct-01-36.bilivideo.com",
            "cn-sh-office-bcache-01.bilivideo.com"
        ),
        "北京" to listOf(
            "cn-bj-cc-03-14.bilivideo.com",
            "cn-bj-cc-03-17.bilivideo.com",
            "cn-bj-fx-01-04.bilivideo.com",
            "cn-bj-fx-01-05.bilivideo.com",
            "cn-bj-se-01-05.bilivideo.com"
        ),
        "南京" to listOf(
            "cn-jsnj-fx-02-05.bilivideo.com",
            "cn-jsnj-fx-02-07.bilivideo.com",
            "cn-jsnj-fx-02-10.bilivideo.com",
            "cn-jsnj-gd-01-02.bilivideo.com"
        ),
        "天津" to listOf(
            "cn-tj-cm-02-01.bilivideo.com",
            "cn-tj-cm-02-02.bilivideo.com",
            "cn-tj-cm-02-04.bilivideo.com",
            "cn-tj-cm-02-05.bilivideo.com",
            "cn-tj-cm-02-06.bilivideo.com",
            "cn-tj-cm-02-07.bilivideo.com",
            "cn-tj-cu-01-02.bilivideo.com",
            "cn-tj-cu-01-03.bilivideo.com",
            "cn-tj-cu-01-04.bilivideo.com",
            "cn-tj-cu-01-05.bilivideo.com",
            "cn-tj-cu-01-06.bilivideo.com",
            "cn-tj-cu-01-07.bilivideo.com",
            "cn-tj-cu-01-09.bilivideo.com",
            "cn-tj-cu-01-10.bilivideo.com",
            "cn-tj-cu-01-11.bilivideo.com",
            "cn-tj-cu-01-12.bilivideo.com",
            "cn-tj-cu-01-13.bilivideo.com"
        ),
        "广州" to listOf(
            "cn-gdgz-cm-01-02.bilivideo.com",
            "cn-gdgz-cm-01-10.bilivideo.com",
            "cn-gdgz-fx-01-01.bilivideo.com",
            "cn-gdgz-fx-01-02.bilivideo.com",
            "cn-gdgz-fx-01-03.bilivideo.com",
            "cn-gdgz-fx-01-04.bilivideo.com",
            "cn-gdgz-fx-01-08.bilivideo.com",
            "cn-gdgz-fx-01-10.bilivideo.com",
            "cn-gdgz-gd-01-01.bilivideo.com"
        ),
        "成都" to listOf(
            "cn-sccd-cm-03-02.bilivideo.com",
            "cn-sccd-cm-03-05.bilivideo.com",
            "cn-sccd-ct-01-02.bilivideo.com",
            "cn-sccd-ct-01-08.bilivideo.com",
            "cn-sccd-ct-01-10.bilivideo.com",
            "cn-sccd-cu-01-02.bilivideo.com",
            "cn-sccd-cu-01-03.bilivideo.com",
            "cn-sccd-fx-01-01.bilivideo.com",
            "cn-sccd-fx-01-06.bilivideo.com"
        ),
        "杭州" to listOf(
            "cn-zjhz-cm-01-01.bilivideo.com",
            "cn-zjhz-cm-01-04.bilivideo.com",
            "cn-zjhz-cm-01-07.bilivideo.com",
            "cn-zjhz-cm-01-12.bilivideo.com",
            "cn-zjhz-cm-01-17.bilivideo.com",
            "cn-zjhz-cu-01-01.bilivideo.com",
            "cn-zjhz-cu-01-02.bilivideo.com",
            "cn-zjhz-cu-01-05.bilivideo.com",
            "cn-zjhz-cu-v-02.bilivideo.com"
        ),
        "武汉" to listOf(
            "cn-hbwh-cm-01-01.bilivideo.com",
            "cn-hbwh-cm-01-02.bilivideo.com",
            "cn-hbwh-cm-01-04.bilivideo.com",
            "cn-hbwh-cm-01-05.bilivideo.com",
            "cn-hbwh-cm-01-06.bilivideo.com",
            "cn-hbwh-cm-01-08.bilivideo.com",
            "cn-hbwh-fx-01-01.bilivideo.com",
            "cn-hbwh-fx-01-02.bilivideo.com"
        ),
        "沈阳" to listOf(
            "cn-lnsy-cm-01-01.bilivideo.com",
            "cn-lnsy-cm-01-03.bilivideo.com",
            "cn-lnsy-cm-01-04.bilivideo.com",
            "cn-lnsy-cm-01-05.bilivideo.com",
            "cn-lnsy-cm-01-06.bilivideo.com",
            "cn-lnsy-cu-01-03.bilivideo.com",
            "cn-lnsy-cu-01-06.bilivideo.com"
        ),
        "深圳" to listOf(
            "upos-sz-302kodo.bilivideo.com",
            "upos-sz-dynqn.bilivideo.com",
            "upos-sz-estgcos.bilivideo.com",
            "upos-sz-estghw.bilivideo.com",
            "upos-sz-mirror08c.bilivideo.com",
            "upos-sz-mirror08h.bilivideo.com",
            "upos-sz-mirroralibstar1.bilivideo.com",
            "upos-sz-mirroraliov.bilivideo.com",
            "upos-sz-mirrorbd.bilivideo.com",
            "upos-sz-mirrorcf1ov.bilivideo.com",
            "upos-sz-mirrorcosbstar.bilivideo.com",
            "upos-sz-mirrorcosdisp.bilivideo.com",
            "upos-sz-mirrorctos.bilivideo.com",
            "upos-sz-mirrorhwdisp.bilivideo.com",
            "upos-sz-originbstar.bilivideo.com",
            "upos-sz-origincosgzhw.bilivideo.com",
            "upos-sz-origincosv.bilivideo.com"
        ),
        "海外" to listOf(
            "upos-hz-mirrorakam.akamaized.net",
            "upos-sz-mirroraliov.bilivideo.com",
            "upos-sz-mirrorcosov.bilivideo.com"
        ),
        "香港" to listOf(
            "cn-hk-eq-01-01.bilivideo.com",
            "cn-hk-eq-01-03.bilivideo.com",
            "cn-hk-eq-01-09.bilivideo.com",
            "cn-hk-eq-01-10.bilivideo.com",
            "cn-hk-eq-01-12.bilivideo.com",
            "cn-hk-eq-01-13.bilivideo.com",
            "cn-hk-eq-01-14.bilivideo.com",
            "cn-hk-eq-bcache-13.bilivideo.com"
        )
    )

    val regions = listOf(defaultRegion) + regionNodes.keys + customRegion

    fun nodesForRegion(region: String): List<String> = regionNodes[region].orEmpty()

    fun regionForHost(host: String): String {
        val normalizedHost = normalizeHost(host)
        if (normalizedHost.isBlank()) return defaultRegion
        return regionNodes.entries.firstOrNull { (_, nodes) -> normalizedHost in nodes }?.key
            ?: customRegion
    }

    fun displayNameForHost(host: String): String {
        val normalizedHost = normalizeHost(host)
        return normalizedHost.ifBlank { defaultRegion }
    }

    fun normalizeHost(rawHost: String): String {
        return rawHost
            .trim()
            .replace("\n", "")
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("//")
            .trim('/')
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore("#")
    }
}

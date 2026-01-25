package dev.aaa1115910.bv.tv.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.aaa1115910.bv.tv.component.HomeTopNavItem
import dev.aaa1115910.bv.tv.component.PgcTopNavItem
import dev.aaa1115910.bv.tv.component.UgcTopNavItem
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 导航项配置数据类
 */
data class NavItemConfig(
    val ordinal: Int,
    val hidden: Boolean
)

private fun <T> parseTopNavItemsOrder(orderString: String, entries: List<T>): List<T> {
    if (orderString.isBlank()) return entries

    return orderString
        .split(",")
        .mapNotNull { part ->
            val ordinal = part.toIntOrNull() ?: return@mapNotNull null
            val isHidden = ordinal < 0
            val actualOrdinal = if (isHidden) -ordinal else ordinal
            actualOrdinal to isHidden
        }
        .filter { !it.second }
        .mapNotNull { (ordinal, _) -> entries.getOrNull(ordinal) }
}

/**
 * 获取根据设置过滤和排序后的首页导航项列表
 */
val homeNavItemsFlow: Flow<List<HomeTopNavItem>>
    get() = Prefs.homeNavItemsOrderFlow.map { orderString ->
        parseHomeNavItemsOrder(orderString)
    }

/**
 * 获取根据设置过滤和排序后的 UGC 顶部导航项列表
 */
val ugcNavItemsFlow: Flow<List<UgcTopNavItem>>
    get() = Prefs.ugcNavItemsOrderFlow.map { orderString ->
        parseUgcTopNavItemsOrder(orderString)
    }

/**
 * 获取根据设置过滤和排序后的 PGC 顶部导航项列表
 */
val pgcNavItemsFlow: Flow<List<PgcTopNavItem>>
    get() = Prefs.pgcNavItemsOrderFlow.map { orderString ->
        parsePgcTopNavItemsOrder(orderString)
    }

/**
 * 解析导航项排序字符串
 * @param orderString 逗号分隔的 ordinal 列表，负数表示隐藏
 * @return 过滤和排序后的导航项列表
 */
fun parseHomeNavItemsOrder(orderString: String): List<HomeTopNavItem> {
    return parseTopNavItemsOrder(orderString, HomeTopNavItem.entries)
}

fun parseUgcTopNavItemsOrder(orderString: String): List<UgcTopNavItem> {
    return parseTopNavItemsOrder(orderString, UgcTopNavItem.entries)
}

fun parsePgcTopNavItemsOrder(orderString: String): List<PgcTopNavItem> {
    return parseTopNavItemsOrder(orderString, PgcTopNavItem.entries)
}

/**
 * 将指定导航项移到第一位并取消隐藏
 * 用于切换默认标签时，将新默认标签移到第一位
 * @param orderString 当前排序配置字符串
 * @param ordinal 要移到第一位的导航项 ordinal
 * @return 更新后的排序配置字符串
 */
fun moveNavItemToFirstAndUnhide(orderString: String, ordinal: Int): String {
    return moveNavItemToFirstAndUnhide(orderString = orderString, ordinal = ordinal, entriesCount = 0)
}

/**
 * 将指定导航项移到第一位并取消隐藏
 * @param orderString 当前排序配置字符串
 * @param ordinal 要移到第一位的导航项 ordinal
 * @param entriesCount 枚举项数量；当 orderString 为空时用于生成默认序列
 */
fun moveNavItemToFirstAndUnhide(orderString: String, ordinal: Int, entriesCount: Int): String {
    val normalizedOrderString = if (orderString.isBlank()) {
        if (entriesCount <= 0) return orderString
        (0 until entriesCount).joinToString(",")
    } else {
        orderString
    }

    val parts = normalizedOrderString.split(",").map { part ->
        val num = part.toIntOrNull() ?: return@map part to false
        val absNum = if (num < 0) -num else num
        absNum to (num < 0)
    }

    // 找到目标项
    val targetItem = parts.find { it.first == ordinal }
    if (targetItem == null) return normalizedOrderString

    // 构建新的顺序：目标项在前，其他项按原顺序在后
    val otherItems = parts.filter { it.first != ordinal }
    val newParts = listOf(
        ordinal.toString()  // 默认标签在第一位，取消隐藏
    ) + otherItems.map { (ord, hidden) ->
        if (hidden) "-$ord" else "$ord"
    }

    return newParts.joinToString(",")
}

/**
 * 解析排序字符串为配置列表
 * @param orderString 逗号分隔的 ordinal 列表，负数表示隐藏
 * @return 导航项配置列表（按显示顺序）
 */
fun parseNavItemsOrderToConfig(orderString: String): List<NavItemConfig> {
    return parseNavItemsOrderToConfig(orderString, HomeTopNavItem.entries.size)
}

/**
 * 解析排序字符串为配置列表（通用版本）
 * @param orderString 逗号分隔的 ordinal 列表，负数表示隐藏
 * @param entriesCount 枚举项数量
 */
fun parseNavItemsOrderToConfig(orderString: String, entriesCount: Int): List<NavItemConfig> {
    if (entriesCount <= 0) return emptyList()

    if (orderString.isBlank()) {
        return (0 until entriesCount).map { NavItemConfig(it, false) }
    }

    return orderString
        .split(",")
        .mapNotNull { part ->
            val ordinal = part.toIntOrNull() ?: return@mapNotNull null
            val isHidden = ordinal < 0
            val actualOrdinal = if (isHidden) -ordinal else ordinal
            if (actualOrdinal !in 0 until entriesCount) return@mapNotNull null
            NavItemConfig(actualOrdinal, isHidden)
        }
}

package dev.aaa1115910.bv.player.entity

/**
 * 播放器控制栏按钮配置
 * @param id 按钮 ID
 * @param hidden 是否被用户隐藏
 * @param isDefaultFocus 是否为默认焦点按钮
 */
data class ControllerButtonConfig(
    val id: String,
    val hidden: Boolean = false,
    val isDefaultFocus: Boolean = false
)

/**
 * 所有控制栏按钮 ID（默认顺序）
 */
val ALL_CONTROLLER_BUTTON_IDS = listOf(
    "nextVideo", "refresh", "speed", "resolution", "upSpace", "rotation",
    "subtitle", "comment", "danmaku", "loop", "playlist", "related", "settings"
)

/**
 * 解析控制栏按钮配置字符串
 *
 * 格式：逗号分隔的按钮 ID，前缀含义：
 * - 无前缀：可见
 * - `-` 前缀：隐藏
 * - `*` 前缀：默认焦点
 * - `*-` 前缀：隐藏且为默认焦点
 *
 * 例如："refresh,speed,-rotation,*danmaku"
 */
fun parseControllerButtonsOrder(orderString: String): List<ControllerButtonConfig> {
    if (orderString.isBlank()) return emptyList()
    return orderString.split(",")
        .mapNotNull { token ->
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val isDefaultFocus = trimmed.startsWith("*")
            val afterStar = if (isDefaultFocus) trimmed.substring(1) else trimmed
            val isHidden = afterStar.startsWith("-")
            val id = if (isHidden) afterStar.substring(1) else afterStar
            if (id.isEmpty() || !ALL_CONTROLLER_BUTTON_IDS.contains(id)) return@mapNotNull null
            ControllerButtonConfig(id, isHidden, isDefaultFocus)
        }
}

/**
 * 将控制栏按钮配置列表序列化为字符串
 */
fun serializeControllerButtonsOrder(configs: List<ControllerButtonConfig>): String {
    return configs
        .filter { ALL_CONTROLLER_BUTTON_IDS.contains(it.id) }
        .joinToString(",") { config ->
            buildString {
                if (config.isDefaultFocus) append("*")
                if (config.hidden) append("-")
                append(config.id)
            }
        }
}

/**
 * 获取用于编辑的完整按钮配置列表
 * 如果存储的配置为空，返回所有按钮的默认配置；
 * 如果有值，解析后补充缺失的按钮（新增按钮）。
 */
fun getControllerButtonConfigsForEditing(orderString: String): List<ControllerButtonConfig> {
    val configs = parseControllerButtonsOrder(orderString)
    if (configs.isEmpty()) {
        return ALL_CONTROLLER_BUTTON_IDS.map { ControllerButtonConfig(it) }
    }
    val existingIds = configs.map { it.id }.toSet()
    val missing = ALL_CONTROLLER_BUTTON_IDS.filter { it !in existingIds }
        .map { ControllerButtonConfig(it) }
    return configs + missing
}

/**
 * 获取按钮的中文显示名称
 */
fun getControllerButtonDisplayName(id: String): String {
    return when (id) {
        "nextVideo" -> "下一个视频"
        "refresh" -> "刷新"
        "speed" -> "播放速度"
        "resolution" -> "画质"
        "upSpace" -> "UP主空间"
        "rotation" -> "画面旋转"
        "subtitle" -> "字幕"
        "comment" -> "评论"
        "danmaku" -> "弹幕"
        "loop" -> "循环播放"
        "playlist" -> "播放列表"
        "related" -> "相关推荐"
        "settings" -> "设置"
        else -> id
    }
}

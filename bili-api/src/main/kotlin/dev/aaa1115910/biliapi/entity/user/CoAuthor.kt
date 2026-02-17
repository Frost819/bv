package dev.aaa1115910.biliapi.entity.user

/**
 * 联合投稿成员（用于 UGC staff）
 *
 * @param mid 成员 mid
 * @param name 昵称
 * @param face 头像
 * @param title 职务/角色（直接显示后端原始字符串，如 UP主/协力/...）
 */
data class CoAuthor(
    val mid: Long,
    val name: String,
    val face: String,
    val title: String
)
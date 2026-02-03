package dev.aaa1115910.bv.block

/**
 * 屏蔽功能作用范围（页面）。
 *
 * 说明：
 * - 默认不勾选，因此默认不生效
 * - SearchResult 只考虑视频结果：SearchVideo
 * - Related 用于“视频详情相关推荐”（VideosRow 数据源）
 */
enum class BlockPage(val code: String, val displayName: String) {
    Recommend("Recommend", "推荐"),
    Popular("Popular", "热门"),
    Dynamics("Dynamics", "动态"),
    History("History", "历史记录"),
    ToView("ToView", "稍后再看"),
    SearchVideo("SearchVideo", "搜索结果(视频)"),
    Favorite("Favorite", "收藏"),
    Related("Related", "相关推荐"),
    Tag("Tag", "标签页"),
}
package com.example.model

data class ItemWithDetails(
    val item: Item,
    val space: Space?,
    val category: Category?,
    val tags: List<Tag> = emptyList()
)

data class SpaceWithParent(
    val space: Space,
    val parentSpace: Space?
)

data class SearchResult(
    val items: List<ItemWithDetails>,
    val spaces: List<Space>
)

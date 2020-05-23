package com.mvvm.model

import com.google.gson.annotations.SerializedName

class RepoSearchResponse(
    @field: SerializedName("total_count")
    val totalCount: Int = 0,
    @field: SerializedName("items")
    val items: List<Repos>
) {
    var nextPage: Int? = null
}
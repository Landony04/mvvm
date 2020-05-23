package com.mvvm.model

import androidx.room.Entity
import androidx.room.TypeConverters
import com.mvvm.db.GitHubTypeConverters

@Entity(primaryKeys = ["query"])
@TypeConverters(GitHubTypeConverters::class)
class RepoSearchResult(
    val query: String,
    val repoIds: List<Int>,
    val totalCount: Int,
    val next: Int?
)
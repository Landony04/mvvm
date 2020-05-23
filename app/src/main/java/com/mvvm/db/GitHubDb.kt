package com.mvvm.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mvvm.model.Contributor
import com.mvvm.model.RepoSearchResult
import com.mvvm.model.Repos
import com.mvvm.model.User

@Database(
    entities = [
        User::class,
        Repos::class,
        Contributor::class,
        RepoSearchResult::class
    ],
    version = 1
)
abstract class GitHubDb : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun repoDao(): RepoDao
}
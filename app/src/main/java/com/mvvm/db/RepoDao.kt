package com.mvvm.db

import android.util.SparseIntArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mvvm.model.Contributor
import com.mvvm.model.RepoSearchResult
import com.mvvm.model.Repos
import java.util.*

@Dao
abstract class RepoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg repo: Repos)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertContributors(vararg contributor: List<Contributor>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertRepos(vararg respos: List<Repos>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun createRepoIfExist(repo: Repos): Long

    @Query("SELECT * FROM repos WHERE owner_login = :ownerLogin AND name = :name")
    abstract fun load(ownerLogin: String, name: String): LiveData<Repos>

    @Query("SELECT login, avatarUrl, repoName, repoOwner, contributions FROM contributor WHERE repoName = :name AND repoOwner = :owner ORDER BY contributions DESC ")
    abstract fun loadContributors(owner: String, name: String): LiveData<List<Contributor>>

    @Query("SELECT * FROM repos WHERE owner_login = :owner ORDER BY stars DESC ")
    abstract fun loadRepositories(owner: String): LiveData<List<Repos>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(result: RepoSearchResult)

    @Query("SELECT * FROM reposearchresult WHERE 'query' = :query")
    abstract fun search(query: String): LiveData<RepoSearchResult>

    fun loadOrdered(reposIds: List<Int>): LiveData<List<Repos>> {
        val order = SparseIntArray()
        reposIds.withIndex().forEach {
            order.put(it.value, it.index)
        }

        return Transformations.map(loadById(reposIds)) { repositories ->
            Collections.sort(repositories) { r1, r2 ->
                val pos1 = order.get(r1._id)
                val pos2 = order.get(r2._id)
                pos1 - pos2
            }
            repositories
        }
    }

    @Query("SELECT * FROM repos WHERE _id in(:repoIds)")
    protected abstract fun loadById(repoIds: List<Int>): LiveData<List<Repos>>

    @Query("SELECT * FROM reposearchresult WHERE 'query' = :query")
    abstract fun findSearchResult(query: String): RepoSearchResult?
}
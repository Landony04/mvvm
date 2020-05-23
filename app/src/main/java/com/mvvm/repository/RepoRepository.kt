package com.mvvm.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.mvvm.AppExecutors
import com.mvvm.api.ApiResponse
import com.mvvm.api.ApiSuccessResponse
import com.mvvm.api.GitHubApi
import com.mvvm.db.GitHubDb
import com.mvvm.db.RepoDao
import com.mvvm.model.Contributor
import com.mvvm.model.RepoSearchResponse
import com.mvvm.model.RepoSearchResult
import com.mvvm.model.Repos
import com.mvvm.utils.AbsentLiveData
import com.mvvm.utils.RateLimiter
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class RepoRepository(
    private val appExecutors: AppExecutors,
    private val gitHubDb: GitHubDb,
    private val repoDao: RepoDao,
    private val gitHubApi: GitHubApi
) {
    private val repoListRateLimiter = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun loadRepos(owner: String): LiveData<Resource<List<Repos>>> {
        return object : NetworkBoundResource<List<Repos>, List<Repos>>(appExecutors) {
            override fun saveCallResult(item: List<Repos>) {
                repoDao.insertRepos(item)
            }

            override fun shouldFetch(data: List<Repos>?): Boolean {
                return data == null || data.isEmpty() || repoListRateLimiter.shouldFetch(owner)
            }

            override fun loadFromDb(): LiveData<List<Repos>> = repoDao.loadRepositories(owner)

            override fun createCall(): LiveData<ApiResponse<List<Repos>>> =
                gitHubApi.getRepos(owner)

            override fun onFetchFailed() {
                super.onFetchFailed()
                repoListRateLimiter.reset(owner)
            }
        }.asLiveData()
    }

    fun loadRepo(owner: String, login: String): LiveData<Resource<Repos>> {
        return object : NetworkBoundResource<Repos, Repos>(appExecutors) {
            override fun saveCallResult(item: Repos) {
                repoDao.insert(item)
            }

            override fun shouldFetch(data: Repos?): Boolean {
                return data == null
            }

            override fun loadFromDb(): LiveData<Repos> =
                repoDao.load(ownerLogin = owner, name = login)

            override fun createCall(): LiveData<ApiResponse<Repos>> =
                gitHubApi.getRepo(owner = owner, name = login)
        }.asLiveData()
    }

    fun loadContributors(owner: String, name: String): LiveData<Resource<List<Contributor>>> {
        return object : NetworkBoundResource<List<Contributor>, List<Contributor>>(appExecutors) {
            override fun saveCallResult(item: List<Contributor>) {
                item.forEach {
                    it.repoName = name
                    it.repoOwner = owner
                }
                gitHubDb.runInTransaction {
                    repoDao.createRepoIfExist(
                        Repos(
                            _id = Repos.UKNOWN_ID,
                            name = name,
                            fullname = "$owner/$name",
                            description = "",
                            owner = Repos.Owner(owner, null),
                            stars = 0
                        )
                    )
                    repoDao.insertContributors(item)
                }
            }

            override fun shouldFetch(data: List<Contributor>?): Boolean {
                return data == null || data.isEmpty()
            }

            override fun loadFromDb(): LiveData<List<Contributor>> =
                repoDao.loadContributors(owner = owner, name = name)

            override fun createCall(): LiveData<ApiResponse<List<Contributor>>> =
                gitHubApi.getContributors(owner = owner, name = name)
        }.asLiveData()
    }

    fun searchNextPage(query: String): LiveData<Resource<Boolean>> {
        val fetchNextSearchPageTask =
            FetchNextSearchPageTask(query = query, gitHubApi = gitHubApi, dbGitHubDb = gitHubDb)
        appExecutors.networkIo().execute(fetchNextSearchPageTask)
        return fetchNextSearchPageTask.liveData
    }

    fun search(query: String): LiveData<Resource<List<Repos>>> {
        return object : NetworkBoundResource<List<Repos>, RepoSearchResponse>(appExecutors) {
            override fun saveCallResult(item: RepoSearchResponse) {
                val repoIds = item.items.map { it._id }
                val repoSearchResult = RepoSearchResult(
                    query = query,
                    repoIds = repoIds,
                    totalCount = item.totalCount,
                    next = item.nextPage
                )
                gitHubDb.beginTransaction()
                try {
                    repoDao.insertRepos(item.items)
                    repoDao.insert(repoSearchResult)
                } finally {
                    gitHubDb.endTransaction()
                }
            }

            override fun shouldFetch(data: List<Repos>?): Boolean = data == null

            override fun loadFromDb(): LiveData<List<Repos>> {
                return Transformations.switchMap(repoDao.search(query = query)) { searchData ->
                    if (searchData == null) {
                        AbsentLiveData.create()
                    } else {
                        repoDao.loadOrdered(reposIds = searchData.repoIds)
                    }
                }
            }

            override fun createCall(): LiveData<ApiResponse<RepoSearchResponse>> =
                gitHubApi.searchRepos(query = query)

            override fun processResponse(response: ApiSuccessResponse<RepoSearchResponse>): RepoSearchResponse {
                val body = response.body
                body.nextPage = response.nextPage
                return body
            }
        }.asLiveData()
    }
}
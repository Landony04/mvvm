package com.mvvm.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mvvm.api.*
import com.mvvm.db.GitHubDb
import com.mvvm.model.RepoSearchResult
import java.io.IOException

class FetchNextSearchPageTask constructor(
    private val query: String,
    private val gitHubApi: GitHubApi,
    private val dbGitHubDb: GitHubDb
): Runnable {

    private val _liveData = MutableLiveData<Resource<Boolean>>()
    val liveData: LiveData<Resource<Boolean>> = _liveData

    override fun run() {
        val current = dbGitHubDb.repoDao().findSearchResult(query)
        if (current == null) {
            _liveData.postValue(null)
            return
        }

        val nextPage = current.next
        if (nextPage == null) {
            _liveData.postValue(Resource.success(false))
            return
        }

        val newValue = try {
            val response = gitHubApi.searchRepositories(query, nextPage).execute()

            when(val apiResponse = ApiResponse.create(response)) {
                is ApiSuccessResponse -> {
                    val ids = arrayListOf<Int>()
                    ids.addAll(current.repoIds)
                    ids.addAll(apiResponse.body.items.map { it._id })
                    val merge = RepoSearchResult(query, ids, apiResponse.body.totalCount, apiResponse.body.nextPage)
                    try {
                        dbGitHubDb.beginTransaction()
                        dbGitHubDb.repoDao().insert(merge)
                        dbGitHubDb.repoDao().insertRepos(apiResponse.body.items)
                        dbGitHubDb.setTransactionSuccessful()
                    } finally {
                        dbGitHubDb.endTransaction()
                    }
                    Resource.success(apiResponse.nextPage != null)
                }
                is ApiEmptyResponse -> {
                    Resource.success(false)
                }
                is ApiErrorResponse -> {
                    Resource.error(apiResponse.errorMessage, true)
                }
            }
        } catch (e: IOException) {
            Resource.error(e.message!!, true)
        }
        _liveData.postValue(newValue)
    }
}
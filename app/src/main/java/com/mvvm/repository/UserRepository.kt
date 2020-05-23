package com.mvvm.repository

import androidx.lifecycle.LiveData
import com.mvvm.AppExecutors
import com.mvvm.api.ApiResponse
import com.mvvm.api.GitHubApi
import com.mvvm.db.UserDao
import com.mvvm.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val appExecutors: AppExecutors,
    private val userDao: UserDao,
    private val gitHubApi: GitHubApi
) {
    fun loadUser(login: String): LiveData<Resource<User>> {
        return object: NetworkBoundResource<User, User>(appExecutors) {
            override fun saveCallResult(item: User) {
                userDao.insert(item)
            }

            override fun shouldFetch(data: User?): Boolean {
                return data == null
            }

            override fun loadFromDb(): LiveData<User> {
                return userDao.findByLogin(login)
            }

            override fun createCall(): LiveData<ApiResponse<User>> {
                return gitHubApi.getUser(login)
            }
        }.asLiveData()
    }
}
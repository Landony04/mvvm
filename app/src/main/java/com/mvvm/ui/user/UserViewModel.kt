package com.mvvm.ui.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.mvvm.model.Repos
import com.mvvm.model.User
import com.mvvm.repository.RepoRepository
import com.mvvm.repository.Resource
import com.mvvm.repository.UserRepository
import com.mvvm.utils.AbsentLiveData
import javax.inject.Inject

class UserViewModel
@Inject constructor(userRepository: UserRepository, repoRepository: RepoRepository) : ViewModel() {
    private val _login = MutableLiveData<String>()
    val login: LiveData<String>
        get() = _login

    val repositories: LiveData<Resource<List<Repos>>> = Transformations
        .switchMap(_login) { login ->
            if (login == null) {
                AbsentLiveData.create()
            } else {
                repoRepository.loadRepos(login)
            }
        }

    val user: LiveData<Resource<User>> = Transformations
        .switchMap(_login) {login ->
            if (login == null) {
                AbsentLiveData.create()
            } else {
                userRepository.loadUser(login)
            }
        }

    fun setLogin(login: String) {
        if (_login.value != login) {
            _login.value = login
        }
    }

    fun retry() {
        _login.value.let {
            _login.value = it
        }
    }
}
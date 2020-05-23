package com.mvvm.utils

import androidx.lifecycle.LiveData

class AbsentLiveData<T: Any?> constructor(): LiveData<T>() {
    init {
        postValue(null)
    }

    companion object {
        fun<T> create(): LiveData<T> {
            return AbsentLiveData()
        }
    }
}
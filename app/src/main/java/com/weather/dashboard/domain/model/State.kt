package com.weather.dashboard.domain.model

sealed class State<out T> {
    data class Success<out T>(val data: T, val apiConstant: String) : State<T>()
    data class Error(val message: String, var code: Int, val apiConstant: String) : State<Nothing>()
    data class Loading(val apiConstant: String = "", val type: LoadingType = LoadingType.LOADER) : State<Nothing>()
    object Idle : State<Nothing>()
}

enum class LoadingType {
    LOADER,
    REFRESH
}


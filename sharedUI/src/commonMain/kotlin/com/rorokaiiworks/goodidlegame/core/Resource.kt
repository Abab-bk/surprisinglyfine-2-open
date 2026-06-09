package com.rorokaiiworks.goodidlegame.core

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(
        val code: Int,
        val message: String
    ) : Resource<Nothing>()
}


sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
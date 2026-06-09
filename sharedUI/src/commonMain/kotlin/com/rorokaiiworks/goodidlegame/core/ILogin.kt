package com.rorokaiiworks.goodidlegame.core

interface ILogin {
    sealed interface LoginResult {
        object Success : LoginResult
        object NotLoggedIn : LoginResult
    }

    suspend fun isLoggedIn(): LoginResult
    suspend fun login(): LoginResult
}
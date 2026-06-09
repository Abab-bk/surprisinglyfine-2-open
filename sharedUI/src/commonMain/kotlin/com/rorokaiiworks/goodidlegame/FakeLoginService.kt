package com.rorokaiiworks.goodidlegame

import com.rorokaiiworks.goodidlegame.core.ILogin

class FakeLoginService : ILogin {
    override suspend fun isLoggedIn(): ILogin.LoginResult {
        return ILogin.LoginResult.NotLoggedIn
    }

    override suspend fun login(): ILogin.LoginResult {
        return ILogin.LoginResult.Success
    }
}
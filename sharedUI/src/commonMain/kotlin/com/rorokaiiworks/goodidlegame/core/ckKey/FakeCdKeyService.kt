package com.rorokaiiworks.goodidlegame.core.ckKey

import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.rewards.Reward
import kotlinx.coroutines.delay

class FakeCdKeyService : ICdKeyService {
    override suspend fun submit(key: String): Resource<List<Reward>> {
        delay(1000)
        return Resource.Error(
            code = 404,
            message = "wrong cd key"
        )
    }
}
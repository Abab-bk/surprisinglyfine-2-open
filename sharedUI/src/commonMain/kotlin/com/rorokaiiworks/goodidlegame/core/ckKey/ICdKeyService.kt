package com.rorokaiiworks.goodidlegame.core.ckKey

import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.rewards.Reward

interface ICdKeyService {
    suspend fun submit(key: String): Resource<List<Reward>>
}
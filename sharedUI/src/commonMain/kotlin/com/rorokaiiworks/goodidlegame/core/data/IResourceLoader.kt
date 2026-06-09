package com.rorokaiiworks.goodidlegame.core.data

import goodidlegame.sharedui.generated.resources.Res

interface IResourceLoader {
    suspend fun load(path: String): ByteArray {
        return Res.readBytes(path)
    }
}
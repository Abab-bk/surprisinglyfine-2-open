package com.rorokaiiworks.goodidlegame

import com.rorokaiiworks.goodidlegame.core.persistent.FileWriter
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.GameSaveVersion
import com.rorokaiiworks.goodidlegame.core.persistent.migrateGameSave
import io.github.xxfast.kstore.DefaultJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class MigratingJsonFileWriter(
    private val file: Path,
    private val json: Json = DefaultJson,
) : FileWriter<GameSave>() {

    private val tempFile: Path = Path("$file.temp")

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun read(): GameSave? {
        return try {
            val source = SystemFileSystem.source(file).buffered()
            val jsonObject = withContext(Dispatchers.IO) {
                GZIPInputStream(source.asInputStream()).use { gzipStream ->
                    json.decodeFromStream(JsonElement.serializer(), gzipStream) as JsonObject
                }
            }

            val version = when (val versionElement = jsonObject["version"]) {
                null, is JsonNull -> GameSaveVersion.V1_LEGACY_COMMUNITY
                is JsonPrimitive if versionElement.isString -> GameSaveVersion.valueOf(versionElement.content)
                else -> throw IllegalArgumentException("Unknown save version format")
            }

            val migratedJson = if (version < GameSaveVersion.V2_COMMUNITY_REWORK) {
                migrateGameSave(version, jsonObject)
            } else {
                jsonObject
            }

            json.decodeFromJsonElement(GameSave.serializer(), migratedJson)

        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun write(save: GameSave?) {
        if (save == null) {
            SystemFileSystem.delete(file, mustExist = false)
            return
        }
        try {
            val sink = SystemFileSystem.sink(tempFile).buffered()
            withContext(Dispatchers.IO) {
                GZIPOutputStream(sink.asOutputStream()).use { gzipStream ->
                    json.encodeToStream(GameSave.serializer(), save, gzipStream)
                }
            }
        } catch (e: Throwable) {
            SystemFileSystem.delete(tempFile, mustExist = false)
            throw e
        }
        SystemFileSystem.atomicMove(source = tempFile, destination = file)
    }
}

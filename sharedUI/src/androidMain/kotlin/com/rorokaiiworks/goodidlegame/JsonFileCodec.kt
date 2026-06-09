package com.rorokaiiworks.goodidlegame

import io.github.xxfast.kstore.Codec
import io.github.xxfast.kstore.DefaultJson
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

inline fun <reified T : @Serializable Any> jsonStoreOf(
    file: Path,
    default: T? = null,
    enableCache: Boolean = true,
    json: Json = DefaultJson,
): KStore<T> = storeOf(
    codec = JsonFileCodec(file = file, json = json),
    default = default,
    enableCache = enableCache,
)

inline fun <reified T : @Serializable Any> JsonFileCodec(
    file: Path,
    tempFile: Path = Path("$file.temp"),
    json: Json = DefaultJson,
): JsonFileCodec<T> = JsonFileCodec(
    file = file,
    tempFile = tempFile,
    json = json,
    serializer = json.serializersModule.serializer(),
)


class JsonFileCodec<T : @Serializable Any>(
    private val file: Path,
    private val tempFile: Path,
    private val json: Json = DefaultJson,
    private val serializer: KSerializer<T>,
) : Codec<T> {
    /**
     * Decodes the file to a value.
     * If the file does not exist, null is returned.
     * @return optional value that is decoded
     */
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun decode(): T? =
        try {
            val source = SystemFileSystem.source(file).buffered()
            withContext(Dispatchers.IO) {
                GZIPInputStream(source.asInputStream()).use { gzipStream ->
                    json.decodeFromStream(serializer, gzipStream)
                }
            }
        } catch (e: Exception) {
            null
        }

    /**
     * Encodes the given value to the file.
     * If the value is null, the file is deleted.
     * If the encoding fails, the temp file is deleted.
     * If the encoding succeeds, the temp file is atomically moved to the target file - completing the transaction.
     * @param value optional value to encode
     */
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun encode(value: T?) {
        if (value == null) {
            SystemFileSystem.delete(file, mustExist = false)
            return
        }
        try {
            val sink = SystemFileSystem.sink(tempFile).buffered()
            withContext(Dispatchers.IO) {
                GZIPOutputStream(sink.asOutputStream()).use { gzipStream ->
                    json.encodeToStream(serializer, value, gzipStream)
                }
            }
        } catch (e: Throwable) {
            SystemFileSystem.delete(tempFile, mustExist = false)
            throw e
        }
        SystemFileSystem.atomicMove(source = tempFile, destination = file)
    }
}
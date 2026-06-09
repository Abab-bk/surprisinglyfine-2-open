package com.rorokaiiworks.goodidlegame

import com.rorokaiiworks.goodidlegame.core.data.DataTable.Companion.yaml
import com.rorokaiiworks.goodidlegame.core.data.Template
import okio.FileSystem
import okio.Path
import okio.Source
import okio.source
import kotlin.system.exitProcess

actual fun getPlatform(): Platform = object : Platform {
    override val name: String = "Android"
}

actual inline fun <reified T : Template> loadTemplates(byteArray: ByteArray): List<T> {
    val templates = yaml.decodeFromSource<List<T>>(byteArray.inputStream().source())
    return templates
}

actual fun byteArrayToOkioSource(byteArray: ByteArray): Source = byteArray.inputStream().source()

actual fun platformListFiles(
    directoryPath: Path,
    extension: String
): List<String> {
    val fileSystem = FileSystem.SYSTEM

    val metadata = fileSystem.metadataOrNull(directoryPath)
    if (metadata == null || !metadata.isDirectory) {
        return emptyList()
    }

    return fileSystem.list(directoryPath)
        .filter { path ->
            val isFile = fileSystem.metadata(path).isRegularFile
            val matchesExtension = path.name.endsWith(".$extension", ignoreCase = true)
            isFile && matchesExtension
        }
        .map { it.name }
}

actual fun exit(
    status: Int,
    info: (String) -> Unit,
    error: (Throwable?, String) -> Unit
) {
    exitProcess(status)
}
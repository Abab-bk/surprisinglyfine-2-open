package com.rorokaiiworks.goodidlegame

import com.codedisaster.steamworks.SteamAPI
import com.rorokaiiworks.goodidlegame.core.data.DataTable.Companion.yaml
import com.rorokaiiworks.goodidlegame.core.data.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.source
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.system.exitProcess


class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual inline fun <reified T : Template> loadTemplates(byteArray: ByteArray): List<T> {
    val templates = yaml.decodeFromSource<List<T>>(ByteArrayInputStream(byteArray).source())
    return templates
}

actual fun exit(status: Int, info: (String) -> Unit, error: (Throwable?, String) -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
        runCatching {
            SteamAPI.shutdown()
            info("Steam API exit")
        }.onFailure { error(it, "Steam shutdown failed") }

        exitProcess(status)
    }
}

fun getAppSaveFolder(): Path {
    val saveDir = kotlin.io.path.Path("saves")

    if (saveDir.notExists()) {
        saveDir.createDirectories()
    }
    return saveDir.toOkioPath()
}

actual fun platformListFiles(
    directoryPath: Path,
    extension: String
): List<String> {
    val dir = File(directoryPath.toString())
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    return dir.listFiles { _, name ->
        name.endsWith(extension)
    }?.map { it.name } ?: emptyList()
}

actual fun byteArrayToOkioSource(byteArray: ByteArray): Source = ByteArrayInputStream(byteArray).source()

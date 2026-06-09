package com.rorokaiiworks.goodidlegame

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.rorokaiiworks.goodidlegame.core.data.Template
import dev.theolm.txtlogwriter.LinePrefix
import dev.theolm.txtlogwriter.TxtLogWriter
import name.kropp.kotlinx.gettext.I18n
import okio.Path.Companion.toPath
import okio.Source
import java.text.MessageFormat
import javax.swing.JOptionPane


interface Platform {
    val name: String
}

object CrashReporter {
    private var isSetup = false

    val logger = Logger(
        config = loggerConfigInit(platformLogWriter(), TxtLogWriter(
            config = TxtLogWriter.Config(
                filePath = "logs/",
                fileName = "good_log_crash.txt",
                linePrefix = LinePrefix.None,
                canWrite = { tag, severity -> true },
            )
        )),
        tag = "CrashReporter"
    )

    fun setup() {
        if (isSetup) return
        isSetup = true
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger.e(throwable) { "Log crash: ${throwable.message}" }

            try {
                if (!java.awt.GraphicsEnvironment.isHeadless()) {
                    val message = """
The program has unexpectedly crashed!
Please send the following files located in the game folder to the Steam Community:
logs/good_log_crash.txt
                """.trimIndent()
                    val runnable = Runnable {
                        val frame = java.awt.Frame().apply {
                            isUndecorated = true
                            isAlwaysOnTop = true
                            setLocationRelativeTo(null)
                            isVisible = true
                        }
                        try {
                            JOptionPane.showMessageDialog(
                                frame,
                                message,
                                "Error",
                                JOptionPane.ERROR_MESSAGE,
                            )
                        } finally {
                            frame.dispose()
                        }
                    }

                    if (java.awt.EventQueue.isDispatchThread()) {
                        runnable.run()
                    } else {
                        java.awt.EventQueue.invokeAndWait(runnable)
                    }
                }
            } catch (e: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}


fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "agoodidlegame.preferences_pb"


fun I18n.tr(text: String, vararg args: Any?): String =
    MessageFormat.format(tr(text), *args)

fun I18n.trc(context: String, text: String, vararg args: Any?): String =
    MessageFormat.format(trc(context, text), *args)


expect fun getPlatform(): Platform

expect inline fun <reified T : Template> loadTemplates(byteArray: ByteArray): List<T>

expect fun exit(status: Int, info: (String) -> Unit, error: (Throwable?, String) -> Unit)


expect fun platformListFiles(directoryPath: okio.Path, extension: String): List<String>


expect fun byteArrayToOkioSource(byteArray: ByteArray): Source

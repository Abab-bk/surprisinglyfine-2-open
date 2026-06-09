package com.rorokaiiworks.goodidlegame

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.rorokaiiworks.goodidlegame.core.FakeAdPlayer
import com.rorokaiiworks.goodidlegame.core.IAdPlayer
import com.rorokaiiworks.goodidlegame.core.ILogin
import com.rorokaiiworks.goodidlegame.core.ISoundPlayer
import com.rorokaiiworks.goodidlegame.core.achievements.IAchievementAdapter
import com.rorokaiiworks.goodidlegame.core.ckKey.FakeCdKeyService
import com.rorokaiiworks.goodidlegame.core.ckKey.ICdKeyService
import com.rorokaiiworks.goodidlegame.core.di.DIQualifiers
import com.rorokaiiworks.goodidlegame.core.leaderBoard.ILeaderboardService
import com.rorokaiiworks.goodidlegame.core.persistent.*
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.ui.ILoginUi
import io.github.xxfast.kstore.DefaultJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import okio.Path.Companion.toOkioPath
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

fun androidModules(activity: Activity, context: Context) = module {
    single<ILogin> { TapTapLoginService(activity) }
    single<ILoginUi> { TapTapLoginUi() }
    single<ICloudProvider> { FakeCloudProvider() }
    single<ICdKeyService> { FakeCdKeyService() }
    single<IAdPlayer> { FakeAdPlayer() }
    single<ISoundPlayer> { AndroidSoundPlayer(context = context) }
    single<ILeaderboardService> { TapTapLeaderBoardService() }
    single<DLCService> { AndroidDLCService() }
    single<ComplianceService> { TapTapComplianceService(activity) }

    single<DataStore<Preferences>> { createAndroidSettingsDataStore(context) }

    single<FileWriter<AppSave>>(named(DIQualifiers.AppSaveFileWriter)) {
        FileWriter.KStoreFileWriter(
            kstore = jsonStoreOf<AppSave>(
                file = Path("${getAppSaveFolder(context)}/app.save"),
                json = DefaultJson
            )
        )
    }

    single<GameFileWriterFactory>(
        named(DIQualifiers.GameSaveFileWriterFactory)
    ) {
        GameFileWriterFactory(
            gameSaveFileWriterFactory = { slotId: String ->
                MigratingJsonFileWriter(
                    file = Path("${getAppSaveFolder(context)}/$slotId.dat"),
                    json = DefaultJson
                )
            }
        )
    }

    single(named(DIQualifiers.AppSaveFolder)) { getAppSaveFolder(context) }
}

private var dataStore: DataStore<Preferences>? = null


private fun getAppSaveFolder(context: Context): okio.Path {
    val saveDir = File(context.filesDir, "saves")

    if (!saveDir.exists()) {
        saveDir.mkdirs()
    }
    return saveDir.toOkioPath()
}

fun createAndroidSettingsDataStore(context: Context): DataStore<Preferences> {
    if (dataStore == null) {
        dataStore = createDataStore(
            producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
        )
    }

    return dataStore!!
}

fun loadAndroidInitialSettings(context: Context): Settings = runBlocking {
    createAndroidSettingsDataStore(context).data.map(SettingsSaver.Companion::toSettings).first()
}
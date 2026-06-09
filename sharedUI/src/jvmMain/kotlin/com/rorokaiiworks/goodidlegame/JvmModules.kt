@file:OptIn(ExperimentalSerializationApi::class)

package com.rorokaiiworks.goodidlegame

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
import com.rorokaiiworks.goodidlegame.core.leaderBoard.FakeLeaderBoardService
import com.rorokaiiworks.goodidlegame.core.leaderBoard.ILeaderboardService
import com.rorokaiiworks.goodidlegame.core.persistent.*
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.ui.FakeLoginUi
import com.rorokaiiworks.goodidlegame.ui.ILoginUi
import io.github.xxfast.kstore.DefaultJson
import kotlinx.io.files.Path
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val jvmSettingsDataStore: DataStore<Preferences> by lazy {
    createDataStore(
        producePath = {
            Path("${getAppSaveFolder()}/$dataStoreFileName").toString()
        }
    )
}

fun loadJvmInitialSettings(): Settings = runBlocking {
    jvmSettingsDataStore.data
        .map(SettingsSaver.Companion::toSettings)
        .first()
}

val jvmSettingsModule = module {
    single<DataStore<Preferences>> { jvmSettingsDataStore }
}

@OptIn(ExperimentalSerializationApi::class)
fun jvmModules(steamAvailable: Boolean) = module {
    single<ILogin> { FakeLoginService() }
    single<ILoginUi> { FakeLoginUi() }
    single<ICloudProvider> { FakeCloudProvider() }
    single<ICdKeyService> { FakeCdKeyService() }
    single<IAdPlayer> { FakeAdPlayer() }
    single<ISoundPlayer> { JvmSoundPlayer() }
    single<DLCService> {
        if (steamAvailable) {
            SteamDLCService(get(), get())
        } else {
            object : DLCService {
                override fun unlocked(dlc: DLC): Boolean = true
                override fun enabled(dlc: DLC): Boolean = true
                override fun goToDlcShop(dlc: DLC): Boolean = false
            }
        }
    }
    single<ILeaderboardService> {
        if (steamAvailable) {
            SteamLeaderboardService(
                userStats = get(),
                steamUser = get(),
                steamFriends = get(),
                steamStatsManager = get()
            )
        } else {
            FakeLeaderBoardService()
        }
    }
    if (steamAvailable) {
        single<IAchievementAdapter> {
            SteamAchievementsAdapter(get(), get())
        }
    }

    single<FileWriter<AppSave>>(named(DIQualifiers.AppSaveFileWriter)) {
        FileWriter.KStoreFileWriter(
            kstore = jsonStoreOf<AppSave>(
                file = Path("${getAppSaveFolder()}/app.save"),
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
                    file = Path("${getAppSaveFolder()}/$slotId.dat"),
                    json = DefaultJson
                )
            }
        )
    }

    single<ComplianceService> { FakeComplianceService() }

    single(named(DIQualifiers.AppSaveFolder)) { getAppSaveFolder() }
}

package com.rorokaiiworks.goodidlegame

import co.touchlab.kermit.Logger
import com.codedisaster.steamworks.SteamApps
import com.codedisaster.steamworks.SteamFriends
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class SteamDLCService(
    private val steamApps: SteamApps,
    private val steamFriends: SteamFriends
) : DLCService, KoinComponent {
    private val launchSettings: LaunchSettings by inject()
    private val logger: Logger by inject { parametersOf("SteamDLCService") }

    override fun unlocked(dlc: DLC): Boolean {
        val appId = dlc.resolveAppId() ?: return false

        if (launchSettings.mockNoDLCUnlocked) return false

        val isUnlocked = steamApps.isSubscribedApp(appId)
//        logger.i { "Steam DLC: ${dlc.name} isUnlocked: $isUnlocked" }

        return isUnlocked
    }

    override fun enabled(dlc: DLC): Boolean {
        val appId = dlc.resolveAppId() ?: return false
        if (launchSettings.mockNoDLCUnlocked) return false

        val isEnabled = steamApps.isDlcInstalled(appId)
//        logger.i { "Steam DLC: ${dlc.name} isEnabled: $isEnabled" }

        return isEnabled
    }

    override fun goToDlcShop(dlc: DLC): Boolean {
        val appId = dlc.resolveAppId() ?: return false
        steamFriends.activateGameOverlayToStore(
            appId,
            SteamFriends.OverlayToStoreFlag.None
        )
        return true
    }

    private fun DLC.resolveAppId(): Int? {
        val appId = when (this) {
            DLC.Societal -> SOCIETAL_DLC_APP_ID
        }

        return appId.takeIf { it > 0 }
    }

    private companion object {
//        private const val SOCIETAL_DLC_APP_ID = 0
        private const val SOCIETAL_DLC_APP_ID = 4422220
    }
}

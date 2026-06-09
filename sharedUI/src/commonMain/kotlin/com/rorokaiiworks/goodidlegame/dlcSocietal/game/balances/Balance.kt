package com.rorokaiiworks.goodidlegame.dlcSocietal.game.balances

import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class Balance : KoinComponent {
    private val cityInventory: CityInventory by inject()
    var lastSettlementMills: Long = 0
        private set

    fun syncLastSettlementMills(mills: Long) {
        lastSettlementMills = mills
    }

    var balance: Long = 0
        private set

    var secondsUntilSettlement: Float = 0F

    fun tick(currentMills: Long, buildings: List<Building>) {
        if (lastSettlementMills == 0L) {
            lastSettlementMills = currentMills
        }

        balance = -buildings.sumOf { it.totalMaintenanceBalance }

        val overdueMills = currentMills - lastSettlementMills
        val settlementCount = (overdueMills / BALANCE_PERIOD_MILLIS).toInt()

        if (settlementCount > 0) {
            val totalChange = balance * settlementCount

            if (totalChange > 0) {
                cityInventory.addIsleBucks(totalChange)
            } else if (totalChange < 0) {
                cityInventory.spendIsleBucks(-totalChange)
            }

            lastSettlementMills += settlementCount * BALANCE_PERIOD_MILLIS
        }

        secondsUntilSettlement = (BALANCE_PERIOD_MILLIS - (currentMills - lastSettlementMills)) / 1000F
    }

    companion object {
        const val BALANCE_PERIOD = 10f
        const val BALANCE_PERIOD_MILLIS = (BALANCE_PERIOD * 1000L).toLong()
    }
}

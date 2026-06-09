package com.rorokaiiworks.goodidlegame.core.starStore

import com.rorokaiiworks.goodidlegame.core.IAdPlayer
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class StarStore : KoinComponent, IPersistable {
    val items: DataTable<StarStoreItem> by inject(named<StarStoreItem>())
    val purchased: MutableList<StarStoreItem> = mutableListOf()

    private val adPlayer: IAdPlayer by inject()
    private val timeProvider: ITimeProvider by inject()
    private val playerInventory: PlayerInventory by inject()


    suspend fun buyItem(item: StarStoreItem): Resource<Unit> {
        if (item.isAdNeeded) {
            val result = adPlayer.playAd()

            if (result is Resource.Success) {
                addPurchased(item)
            }

            return result
        }

        if (playerInventory.stars < item.price) {
            return Resource.Error(
                message = "Not enough stars",
                code = 10001
            )
        }

        playerInventory.spendStars(item.price)
        addPurchased(item)
        return Resource.Success(Unit)
    }

    fun addPurchased(item: StarStoreItem) {
        item.apply()
        purchased.add(item)
    }

    fun addPurchasedAfterLoad(item: StarStoreItem, duration: Float) {
        item.remain = duration
        item.apply()
        purchased.add(item)
    }

    fun tick(delta: Float) {
        val toRemove = mutableListOf<StarStoreItem>()

        for (item in purchased) {
            item.remain -= delta
            if (item.remain <= 0f) {
                item.remove()
                toRemove.add(item)
            }
        }

        purchased.removeAll(toRemove)
    }

    @OptIn(ExperimentalTime::class)
    override fun doSave(gameSave: GameSave, currentMills: Long) {
        val starBuffState = StarBuffState(
            purchased = purchased.map { item ->
                StarStoreItemSaveData(
                    id = item.id,
                    purchasedAt = item.purchasedAt
                )
            }
        )
        gameSave.starBuffState = starBuffState
    }

    @OptIn(ExperimentalTime::class)
    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        gameSave.starBuffState.purchased.forEach { saveData ->
            val duration = saveData.purchasedAt?.let { timeProvider.now() - it } ?: Duration.ZERO
            if (duration <= Duration.ZERO) return@forEach

            addPurchasedAfterLoad(
                item = items.find(saveData.id),
                duration = duration.inWholeSeconds.toFloat() // TODO: FIXME
            )
        }
    }
}
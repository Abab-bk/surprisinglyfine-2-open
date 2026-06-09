package com.rorokaiiworks.goodidlegame.core.starStore

import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@Serializable
@SerialName("starStoreItem")
@OptIn(ExperimentalTime::class)
sealed class StarStoreItem : Template, KoinComponent, ISourceName {
    protected val player: Player by inject()
    protected val timeProvider: ITimeProvider by inject()
    override val sourceName: String
        get() = name

    override val id: String = ""

    val name: String = ""
    val price: Long = 0
    val isAdNeeded: Boolean = false
    val duration: Float = 0f

    @Transient
    var purchasedAt: Instant? = null
        protected set

    @Transient
    var remain: Float = duration

    abstract fun apply()
    abstract fun remove()

    @OptIn(ExperimentalUuidApi::class)
    @SerialName("modifiersStoreItem")
    @Serializable
    class ModifiersStoreItem : StarStoreItem() {
        val modifiers: List<StatModifier> = listOf()

        override fun apply() {
            purchasedAt = timeProvider.now()

            modifiers.forEach { modifier ->
                player.effectManager.addEffect(
                    Effect(
                        source = this,
                        sourceName = this,
                        id = id,
                        modifiers = listOf(modifier),
                    )
                )
            }
        }

        override fun remove() {
            player.effectManager.removeAllEffectsBySource(this)

            purchasedAt = null
            remain = 0f
        }
    }

    @SerialName("itemStoreItem")
    @Serializable
    class ItemStoreItem(
        val itemId: String,
        val itemCount: Long
    ): StarStoreItem(), KoinComponent {
        private val playerInventory: PlayerInventory by inject()
        private val itemService: ItemService by inject()

        override fun apply() {
            purchasedAt = timeProvider.now()
            playerInventory.inventory.addItem(itemService.createItem(itemId, itemCount))
        }

        override fun remove() {
            purchasedAt = null
            remain = 0f
        }
    }
}

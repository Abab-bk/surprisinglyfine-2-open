@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.core.rewards

import androidx.compose.runtime.Composable
import com.rorokaiiworks.goodidlegame.core.GameState
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.tr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed class Reward : KoinComponent {
    protected val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    protected val skillTemplates: DataTable<SkillTemplate> by inject(named<SkillTemplate>())
    protected val playerInventory: PlayerInventory by inject()
    protected val playerSkills: PlayerSkills by inject()
    protected val player: Player by inject()
    protected val gameState: GameState by inject()

    abstract fun getIconName(): String

    @Composable
    abstract fun getDescription(i18n: I18n = koinInject()): String
    abstract fun grant()

    @SerialName("fakeReward")
    @Serializable
    class FakeReward(
        val name: String
    ) : Reward() {
        override fun getIconName(): String = "default"

        @Composable
        override fun getDescription(i18n: I18n): String {
            return name
        }

        override fun grant() {
            // Do nothing
        }
    }

    @SerialName("xpReward")
    @Serializable
    class XpReward(
        val skillId: String,
        val count: Long
    ) : Reward() {
        private val skillTemplate get() = skillTemplates.find(skillId)

        override fun getIconName(): String = skillTemplate.id

        @Composable
        override fun getDescription(i18n: I18n): String {
            return "${i18n.tr(skillTemplate.name)}: $count XP"
        }

        override fun grant() {
            playerSkills.skills[skillTemplate.id]?.addXp(count, player.stats)
        }
    }

    @SerialName("itemReward")
    @Serializable
    class ItemReward(
        val itemId: String,
        val count: Long
    ) : Reward() {
        private val itemTemplate get() = itemTemplates.find(itemId)

        override fun getIconName(): String = itemTemplate.id

        @Composable
        override fun getDescription(i18n: I18n): String {
            return "${i18n.tr(itemTemplate.name)} x $count"
        }

        override fun grant() {
            playerInventory.inventory.addItem(
                Item(
                    template = itemTemplates.find(itemId),
                    count = count
                )
            )
        }
    }

    @SerialName("unlockSkillReward")
    @Serializable
    class UnlockSkillReward(
        val skillId: String
    ) : Reward(), KoinComponent {
        private val skillTemplate get() = skillTemplates.find(skillId)

        override fun getIconName(): String = skillTemplate.id

        @Composable
        override fun getDescription(i18n: I18n): String {
            return i18n.tr("Unlock {0}", i18n.tr(skillTemplate.name))
        }

        override fun grant() {
            gameState.unlockSkill(skillId)
        }
    }
}
package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alorma.compose.settings.ui.SettingsSwitch
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.props.PotionProp
import com.rorokaiiworks.goodidlegame.core.props.PropSlot
import com.rorokaiiworks.goodidlegame.core.props.PropsContainer
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.loadout.PropSlotPanel
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun PropsContainerPanel(
    modifier: Modifier = Modifier,
    propsContainer: PropsContainer,
    autoEquip: Boolean,
    onAutoEquipChange: (Boolean) -> Unit,
    onAutoEquipSlotClicked: (String) -> Unit,
    i18n: I18n = koinInject(),
    itemTemplates: DataTable<ItemTemplate> = koinInject(named<ItemTemplate>()),
    onClick: (PropSlot) -> Unit,
) {
    BaseCard(modifier = modifier) {
        CardTitle(title = i18n.tr("Props"))

        for (itemSlot in propsContainer.propSlots) {
            PropSlotPanel(
                name = i18n.tr(itemSlot.item?.template?.name ?: itemSlot.name),
                iconName = itemSlot.item?.template?.id ?: itemSlot.id,
                onClick = { onClick(itemSlot) }
            ) {
                if (itemSlot.item is PotionProp) {
                    Text("${(itemSlot.item as PotionProp).timeLeft / 1000L}s")
                }
            }
        }

        SettingsSwitch(
            state = autoEquip,
            title = {
                Text(i18n.tr("Auto Equip"))
            },
            onCheckedChange = onAutoEquipChange
        )

        if (autoEquip) {
            val autoEquip = propsContainer.propSlotsAutoEquip

            for (itemSlot in propsContainer.propSlots) {
                PropSlotPanel(
                    name =
                        if (autoEquip[itemSlot.id]?.isNotEmpty() == true)
                            itemTemplates.find(autoEquip[itemSlot.id]!!).name
                        else itemSlot.name,

                    iconName =
                        if (autoEquip[itemSlot.id]?.isNotEmpty() == true)
                            itemTemplates.find(autoEquip[itemSlot.id]!!).id
                        else itemSlot.id,

                    onClick = {
                        onAutoEquipSlotClicked(itemSlot.id)
                    }
                )
            }
        }
    }
}

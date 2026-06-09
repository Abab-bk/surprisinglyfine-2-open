package com.rorokaiiworks.goodidlegame.ui.traits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.traits.*
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.math.abs
import kotlin.time.Duration

data class TraitScreenUiState(
    val selectedTraitId: String? = null
)

class TraitScreenViewModel : ViewModel(), KoinComponent {
    val traitSystem: TraitSystem by inject()
    private val traitTemplates: DataTable<TraitTemplate> by inject(named<TraitTemplate>())
    private val playerSkills: PlayerSkills by inject()
    private val playerInventory: PlayerInventory by inject()
    val timeProvider: ITimeProvider by inject()

    val traits: List<TraitTemplate> get() = traitTemplates.all()

    private val _uiState = MutableStateFlow(TraitScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onSelectTrait(id: String?) {
        _uiState.update { it.copy(selectedTraitId = id) }
    }

    fun totalSkillLevel(): Int = playerSkills.skills.values.sumOf { it.level }

    fun switchStatus(traitId: String): TraitSwitchStatus = traitSystem.getSwitchStatus(traitId)

    fun switchTrait(traitId: String) {
        traitSystem.switchTrait(traitId)
    }

    fun selectPerk(traitId: String, containerIndex: Int, perkId: String) {
        traitSystem.selectPerk(traitId, containerIndex, perkId)
    }

    fun selectedPerkId(traitId: String, containerIndex: Int): String? =
        traitSystem.getSelectedPerkId(traitId, containerIndex)

    fun formaterCount(): Long =
        playerInventory.inventory.findItem(TraitSystem.TRAIT_SWITCH_ITEM_ID)?.count ?: 0
}

@Composable
fun TraitScreen(
    viewModel: TraitScreenViewModel = koinViewModel(),
    i18n: I18n = koinInject()
) {
    var cooldown by remember { mutableStateOf(Duration.ZERO) }

    LaunchedEffect(Unit) {
        viewModel.viewModelScope.launch {
            while (true) {
                cooldown = viewModel.traitSystem.getCooldownRemaining()
                delay(1000)
            }
        }
    }

    val totalSkillLevel = viewModel.totalSkillLevel()
    val currentTraitId = viewModel.traitSystem.currentTraitId
    val formaterCount = viewModel.formaterCount()

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BaseCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(label = i18n.tr("Skills Total Level"), value = "$totalSkillLevel")
                    StatChip(label = i18n.tr("Formater"), value = "$formaterCount")
                    if (currentTraitId == null) {
                        Text(
                            text = i18n.tr("✦ First selection is free"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (cooldown > Duration.ZERO) {
                    Text(
                        text = i18n.tr("Switch cooldown: {0}", Humanizer.duration(cooldown)),
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.selectedTraitId == null) {
                TraitGallery(
                    traits = viewModel.traits,
                    currentTraitId = currentTraitId,
                    onOpenDetail = viewModel::onSelectTrait
                )
            } else {
                val template = viewModel.traits.firstOrNull { it.id == uiState.selectedTraitId } ?: return@Box
                val status = viewModel.switchStatus(template.id)
                TraitDetailPanel(
                    template = template,
                    isSelected = template.id == currentTraitId,
                    totalSkillLevel = totalSkillLevel,
                    hasFormater = formaterCount > 0,
                    selectedPerkProvider = { index -> viewModel.selectedPerkId(template.id, index) },
                    onSelectPerk = { containerIndex, perkId ->
                        viewModel.selectPerk(template.id, containerIndex, perkId)
                    },
                    status = status,
                    onSelect = { viewModel.switchTrait(template.id) },
                    onBack = { viewModel.onSelectTrait(null) },
                    i18n = i18n
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TraitGallery(
    traits: List<TraitTemplate>,
    currentTraitId: String?,
    onOpenDetail: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(400.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(traits, key = { it.id }) { template ->
            TraitPosterCard(
                template = template,
                isSelected = template.id == currentTraitId,
                onOpenDetail = { onOpenDetail(template.id) }
            )
        }
    }
}

@Composable
private fun TraitPosterCard(
    i18n: I18n = koinInject(),
    template: TraitTemplate,
    isSelected: Boolean,
    onOpenDetail: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val reveal by animateFloatAsState(if (isHovered) 1f else 0f)
    val scrimAlpha by animateFloatAsState(if (isHovered) 0f else 0.65f)
    val glowAlpha by animateFloatAsState(if (isHovered) 0.55f else 0.15f)

    val gradient = remember(template.id) { traitGradientFor(template.id) }
    val grayscale = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .background(brush = gradient)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f + glowAlpha),
                shape = RectangleShape
            )
            .hoverable(interactionSource)
            .drawWithContent {
                drawContent()
                drawRect(Color.Black.copy(alpha = scrimAlpha))
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameImage(
                iconName = template.id,
                modifier = Modifier.size(72.dp),
                colorFilter = if (reveal > 0.25f) null else grayscale
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = i18n.tr(template.name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(
                            alpha = if (reveal > 0.25f) 1f else 0.55f
                        )
                    )
                    if (isSelected) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = i18n.tr("● ACTIVE"),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row {
                    Spacer(Modifier.weight(0.6f))

                    Button(
                        modifier = Modifier.weight(0.4f),
                        onClick = onOpenDetail,
                    ) {
                        Text(
                            text = if (isSelected) i18n.tr("Details") else i18n.tr("Inspect"),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun TraitDetailPanel(
    template: TraitTemplate,
    isSelected: Boolean,
    totalSkillLevel: Int,
    hasFormater: Boolean,
    selectedPerkProvider: (Int) -> String?,
    onSelectPerk: (Int, String) -> Unit,
    status: TraitSwitchStatus,
    onSelect: () -> Unit,
    onBack: () -> Unit,
    i18n: I18n
) {
    val scrollState = rememberScrollState()
    val headerGradient = remember(template.id) { traitGradientFor(template.id) }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onBack) {
                Text(i18n.tr("← Back"), style = MaterialTheme.typography.bodyLarge)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(headerGradient)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = i18n.tr(template.name),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isSelected) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = i18n.tr("● ACTIVE TRAIT"),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        ItemModifiersPanel(modifiers = template.baseBonus)
                    }
                    GameImage(iconName = template.id, modifier = Modifier.size(100.dp))
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSelect,
                enabled = status.canSwitch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (status.isCurrent)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = when {
                        status.isCurrent -> i18n.tr("Selected")
                        status.requiresItem -> i18n.tr("Switch  (1× Formater)")
                        else -> i18n.tr("Select")
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            template.perks.forEachIndexed { containerIndex, perkContainer ->
                val unlocked = totalSkillLevel >= perkContainer.unlockByLevel
                val selectedPerkId = selectedPerkProvider(containerIndex)

                PerkSlotPanel(
                    perkContainer = perkContainer,
                    unlocked = unlocked,
                    isTraitSelected = isSelected,
                    hasFormater = hasFormater,
                    selectedPerkId = selectedPerkId,
                    totalSkillLevel = totalSkillLevel,
                    onSelectPerk = { perkId -> onSelectPerk(containerIndex, perkId) },
                    i18n = i18n
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


@Composable
private fun PerkSlotPanel(
    perkContainer: PerkContainer,
    unlocked: Boolean,
    isTraitSelected: Boolean,
    hasFormater: Boolean,
    selectedPerkId: String?,
    totalSkillLevel: Int,
    onSelectPerk: (String) -> Unit,
    i18n: I18n
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (unlocked)
                    MaterialTheme.colorScheme.outlineVariant
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (unlocked) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (unlocked)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (unlocked) "✓" else "⌛",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = i18n.tr("Skills Total Level ") + "${perkContainer.unlockByLevel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (unlocked)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (!unlocked) {
                Text(
                    text = i18n.tr(
                        "Reach total skill level {0} to unlock this perk slot.  (Currently: {1})",
                        perkContainer.unlockByLevel,
                        totalSkillLevel
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (isTraitSelected && unlocked) {
                Text(
                    text = i18n.tr("Changing perk costs 1× Formater"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasFormater)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                perkContainer.perks.forEach { perk ->
                    val isActive = selectedPerkId == perk.id
                    val canActivate = isTraitSelected && unlocked && hasFormater && !isActive

                    PerkChoiceCard(
                        modifier = Modifier.weight(1f),
                        perk = perk,
                        isActive = isActive,
                        canActivate = canActivate,
                        dimmed = !unlocked,
                        onActivate = { onSelectPerk(perk.id) }
                    )
                }
            }
        }
    }
}


@Composable
private fun PerkChoiceCard(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    perk: Perk,
    isActive: Boolean,
    canActivate: Boolean,
    dimmed: Boolean,
    onActivate: () -> Unit
) {
    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        !dimmed -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    val bgColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dimmed) 0.3f else 0.5f)
    }

    Surface(
        modifier = modifier
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isActive) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = i18n.tr("Active"),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = i18n.tr(perk.desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dimmed) 0.4f else 1f)
            )

            if (!isActive) {
                Button(
                    onClick = onActivate,
                    enabled = canActivate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = i18n.tr("Activate"),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

private fun traitGradientFor(id: String): Brush {
    val seed = id.hashCode()
    val palettes = listOf(
        listOf(Color(0xFF1E1D2B), Color(0xFF2F2A40), Color(0xFF6B4E9E)),
        listOf(Color(0xFF1A2430), Color(0xFF24394A), Color(0xFF5E8C91)),
        listOf(Color(0xFF2A1E24), Color(0xFF3F2B33), Color(0xFF9E5A5A)),
        listOf(Color(0xFF1F2A24), Color(0xFF2F3C33), Color(0xFF8DAA8A)),
        listOf(Color(0xFF2A2220), Color(0xFF3B2F2A), Color(0xFFB47C5B)),
    )
    val palette = palettes[abs(seed) % palettes.size]
    return Brush.linearGradient(palette)
}
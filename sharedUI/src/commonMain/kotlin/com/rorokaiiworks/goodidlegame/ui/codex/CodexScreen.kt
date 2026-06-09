@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rorokaiiworks.goodidlegame.ui.codex

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.core.codex.Codex
import com.rorokaiiworks.goodidlegame.core.codex.CodexItemProgress
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.*
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import com.rorokaiiworks.goodidlegame.ui.skills.CategoryBtn
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class CodexCategory(
    val itemTypes: Set<ItemType>,
    val title: String,
) {
    Equipment(ItemType.Equipment, i18nWrapper("Equipment")),
    Farming(ItemType.Farming, i18nWrapper("Farming")),
    Material(setOf(ItemType.Material), i18nWrapper("Material")),
    Misc(setOf(ItemType.Misc), i18nWrapper("Misc")),
    Props(ItemType.Props, i18nWrapper("Props")),
    Relics(setOf(ItemType.Relic), i18nWrapper("Relics"))
}


class CodexScreenViewModel : ViewModel(), KoinComponent {
    val codex: Codex by inject()
    val i18n: I18n by inject()
    var selectedCategory by mutableStateOf(CodexCategory.Equipment)
        private set
    var selectedItem by mutableStateOf<ItemTemplate?>(null)
        private set

    val queryState = TextFieldState()

    fun selectCategory(category: CodexCategory) {
        selectedCategory = category
    }

    fun onItemClick(item: ItemTemplate) {
        selectedItem = item
    }

    fun cancelItemSelection() {
        selectedItem = null
    }
}

@Composable
fun CodexScreen(
    viewModel: CodexScreenViewModel = koinInject()
) {
    val codex = viewModel.codex
    val filteredItems by remember {
        derivedStateOf {
            val query = viewModel.queryState.text
            val category = viewModel.selectedCategory

            codex.itemTemplates.all().filter {
                val matchesQuery = query.isBlank() ||
                        viewModel.i18n.tr(it.name).contains(query, ignoreCase = true)
                val matchesCategory = it.type in category.itemTypes

                it.tier > -2 && matchesQuery && matchesCategory
            }
        }
    }

    Box {
        if (viewModel.selectedItem != null) {
            GameDialog(
                title = viewModel.i18n.tr(viewModel.selectedItem!!.name),
                onDismissRequest = viewModel::cancelItemSelection,
            ) {
                CodexItemDetail(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    item = viewModel.selectedItem!!,
                    viewModel = viewModel
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CodexCategory.entries.forEach { category ->
                    CategoryBtn(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        category = viewModel.i18n.tr(category.title),
                        isSelected = category == viewModel.selectedCategory,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }

            BaseCard(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    DefaultHorizontalDivider()

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        state = viewModel.queryState,
                        label = { Text(viewModel.i18n.tr("Search")) },
                    )

                    DefaultHorizontalDivider()
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        CodexItemView(
                            viewModel = viewModel,
                            item = item,
                            onClick = viewModel::onItemClick
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun CodexItemDetail(
    modifier: Modifier = Modifier,
    item: ItemTemplate,
    viewModel: CodexScreenViewModel
) {
    val progress = viewModel.codex.tryGetProgress(item.id)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CodexItemImage(
            modifier = Modifier.size(80.dp),
            iconName = item.id,
            progress = progress
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = viewModel.i18n.tr("Mastery Progress: {0}/{1}", progress.progress, CodexItemProgress.MAX_PROGRESS))

            LinearProgressIndicator(
                modifier = Modifier.height(16.dp),
                progress = { progress.progress.toFloat() / CodexItemProgress.MAX_PROGRESS }
            )
        }
    }
}


@Composable
private fun CodexItemView(
    viewModel: CodexScreenViewModel,
    item: ItemTemplate,
    onClick: (ItemTemplate) -> Unit,
) {
    val progress = viewModel.codex.tryGetProgress(item.id)

    Surface(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.background
            )
            .height(100.dp)
            .width(80.dp),
        onClick = { onClick(item) },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            CodexItemImage(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .fillMaxSize(),
                iconName = item.id,
                progress = progress
            )

            DefaultHorizontalDivider()

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = Humanizer.abbreviation(progress.progress),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CodexItemImage(
    modifier: Modifier = Modifier,
    iconName: String, progress: CodexItemProgress
) {
    GameImage(
        modifier = modifier,
        iconName = iconName,
        colorFilter = if (progress.progress < CodexItemProgress.MAX_PROGRESS) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else {
            null
        }
    )
}


@Composable
@Preview
private fun CodexScreenPreview() {
    IdleGameTheme {
        CodexScreen()
    }
}
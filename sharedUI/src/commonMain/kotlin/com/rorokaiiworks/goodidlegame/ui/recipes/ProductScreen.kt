@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.window.core.layout.WindowSizeClass
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.recipes.CraftResult
import com.rorokaiiworks.goodidlegame.core.recipes.Recipe
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitleWithCloseBtn
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.uuid.ExperimentalUuidApi

data class ProductScreenUiState(
    val productTemplate: ItemTemplate? = null,
    val ownedProductCount: Long = 0,

    val possibleRecipes: List<Recipe> = emptyList(),

    val selectedRecipe: Recipe? = null,
    val ownedMaterialCount: Long = 0,
    val canCraft: Boolean = false
)

class ProductScreenViewModel : ViewModel(), KoinComponent {
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val recipes: DataTable<Recipe> by inject(named<Recipe>())

    private val _uiState = MutableStateFlow(ProductScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun loadProduct(productItemId: String, toInventory: Inventory) {
        val productTemplate = itemTemplates.find(productItemId)
        val ownedProductCount = toInventory.items
            .firstOrNull { it.template.id == productItemId }?.count ?: 0

        val possibleRecipes = recipes.all()
            .filter { it.product.itemId == productItemId }

        _uiState.update {
            it.copy(
                productTemplate = productTemplate,
                ownedProductCount = ownedProductCount,
                possibleRecipes = possibleRecipes
            )
        }
    }

    fun selectRecipe(recipe: Recipe, fromInventory: Inventory) {
        val ownedMaterialCount = fromInventory
            .findItem(recipe.required.itemId)
            ?.count ?: 0

        val canCraft = ownedMaterialCount >= recipe.required.count

        _uiState.update {
            it.copy(
                selectedRecipe = recipe,
                ownedMaterialCount = ownedMaterialCount,
                canCraft = canCraft
            )
        }
    }

    fun closeRecipeDetail() {
        _uiState.update { it.copy(selectedRecipe = null) }
    }

    fun onCraft(craftResult: CraftResult, fromInventory: Inventory, toInventory: Inventory) {
        val consume = craftResult.consume
        val product = craftResult.product

        fromInventory.removeItem(
            Item(template = itemTemplates.find(consume.itemId), count = consume.count)
        )

        toInventory.addItem(
            Item(template = itemTemplates.find(product.itemId), count = product.count)
        )

        loadProduct(product.itemId, toInventory)
        _uiState.update { it.copy(selectedRecipe = null) }
    }
}


@Composable
fun ProductScreen(
    productItemId: String,
    fromInventory: Inventory,
    toInventory: Inventory,
    onClose: () -> Unit,
    viewModel: ProductScreenViewModel = koinViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productItemId) {
        viewModel.loadProduct(productItemId, toInventory)
    }

    if (!isWideScreen(windowSizeClass)) {
        if (uiState.selectedRecipe != null) {
            RecipeDetailPanel(
                uiState = uiState,
                viewModel = viewModel,
                fromInventory = fromInventory,
                toInventory = toInventory,
            )
            return
        }

        ProductOverviewPanel(
            uiState = uiState,
            onClose = onClose,
            viewModel = viewModel,
            fromInventory = fromInventory,
            toInventory = toInventory,
        )
        return
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ProductOverviewPanel(
            modifier = Modifier.weight(1f),
            uiState = uiState,
            onClose = onClose,
            viewModel = viewModel,
            fromInventory = fromInventory,
            toInventory = toInventory,
        )

        RecipeDetailPanel(
            modifier = Modifier.weight(1f),
            uiState = uiState,
            viewModel = viewModel,
            fromInventory = fromInventory,
            toInventory = toInventory,
        )
    }
}



@Composable
private fun ProductOverviewPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    fromInventory: Inventory,
    toInventory: Inventory,
    uiState: ProductScreenUiState,
    onClose: () -> Unit,
    viewModel: ProductScreenViewModel
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BaseCard {
            CardTitleWithCloseBtn(
                title = i18n.tr("Stock"),
                onClose = onClose
            )

            if (uiState.productTemplate != null) {
                ItemTemplateEntry(itemTemplate = uiState.productTemplate) {
                    Text("${uiState.ownedProductCount}")
                }
            }
        }

        BaseCard {
            CardTitle(title = i18n.tr("Materials"))
            for (recipe in uiState.possibleRecipes) {
                val material = viewModel.itemTemplates.find(recipe.required.itemId)
                val ownedMaterial = fromInventory.findItem(material.id)
                ItemTemplateEntry(
                    itemTemplate = material,
                    onClick = { viewModel.selectRecipe(recipe, fromInventory) }
                ) {
                    Text("${ownedMaterial?.count ?: i18n.tr("Empty")}")
                }
            }
        }
    }
}



@Composable
private fun RecipeDetailPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    fromInventory: Inventory,
    toInventory: Inventory,
    uiState: ProductScreenUiState,
    viewModel: ProductScreenViewModel,
) {
    Box(modifier = modifier) {
        val selectedRecipe = uiState.selectedRecipe
        if (selectedRecipe == null) {
            BaseCard {
                CardTitle(title = i18n.tr("Item"))
                Text(i18n.tr("Select a material"))
            }
            return
        }

        if (uiState.canCraft) {
            val ownedMaterial = fromInventory.findItem(selectedRecipe.required.itemId)!!
            RecipeItemSelectPanel(
                ownedItem = ownedMaterial,
                recipe = selectedRecipe,
                onCraft = { viewModel.onCraft(it, fromInventory, toInventory) },
                onClose = viewModel::closeRecipeDetail
            )
        } else {
            BaseCard {
                CardTitleWithCloseBtn(
                    title = i18n.tr("Item"),
                    onClose = viewModel::closeRecipeDetail
                )
                Text(i18n.tr("Not enough materials"))
            }
        }
    }
}


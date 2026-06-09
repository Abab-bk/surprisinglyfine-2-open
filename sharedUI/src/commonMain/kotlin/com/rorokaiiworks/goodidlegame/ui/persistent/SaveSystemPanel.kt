package com.rorokaiiworks.goodidlegame.ui.persistent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composables.icons.feather.*
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSlot
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSlotStatue
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSystem
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.persistent.SaveUiState.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

sealed class SaveUiState {
    object Idle : SaveUiState()
    object Creating : SaveUiState()
    data class ConfirmDelete(val slot: SaveSlot) : SaveUiState()
    data class Loading(val message: String) : SaveUiState()
}

class SaveSystemPanelViewModel : ViewModel(), KoinComponent {
    private val eventBus by inject<EventBus>()
    val saveSystem by inject<SaveSystem>()
    val i18n by inject<I18n>()

    private var _uiState = MutableStateFlow<SaveUiState>(Idle)
    val uiState = _uiState.asSharedFlow()

    fun onCancelCreateSlot() {
        _uiState.value = Idle
    }

    fun onConfirmCreateSlot(name: String) {
        viewModelScope.launch {
            _uiState.value = Loading(i18n.tr("Creating save"))
            saveSystem.createNewSlot(
                id = "slot_${System.currentTimeMillis()}",
                playerName = name
            )
            _uiState.value = Idle
        }
    }

    fun onConfirmDeleteSlot(slot: SaveSlot) {
        viewModelScope.launch {
            _uiState.value = Loading(i18n.tr("Deleting save"))
            saveSystem.deleteSlot(slot)
            _uiState.value = Idle
        }
    }

    fun onCancelDeleteSlot() {
        _uiState.value = Idle
    }

    fun onCreateSlotClick() {
        _uiState.value = Creating
    }

    fun onSlotDeleteClick(slot: SaveSlot) {
        _uiState.value = ConfirmDelete(slot)
    }

    fun onSlotClick(slot: SaveSlot) {
        viewModelScope.launch {
            _uiState.value = Loading(i18n.tr("Entering save"))
            startGame(slot)
        }
    }

    fun onRefreshSlots() {
        viewModelScope.launch {
            _uiState.value = Loading(i18n.tr("Refreshing"))
            saveSystem.syncMetadata()
            _uiState.value = Idle
        }
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.value = Loading(i18n.tr("Syncing cloud"))
            saveSystem.syncMetadata()
            _uiState.value = Idle
        }
    }

    fun startGame(slot: SaveSlot) {
        viewModelScope.launch {
            eventBus.emit(IEvent.StartGame(slot))
        }
    }
}


@Composable
fun SaveSystemPanel(
    modifier: Modifier = Modifier,
    viewModel: SaveSystemPanelViewModel = koinInject()
) {
    val i18n = viewModel.i18n
    val uiState by viewModel.uiState.collectAsState(
        initial = Loading(i18n.tr("Refreshing"))
    )

    LaunchedEffect(Unit) {
        viewModel.sync()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(modifier = Modifier.padding(
            vertical = 8.dp,
            horizontal = 12.dp))
        {
            when (val currentState = uiState) {
                is Idle -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SaveTopBar(
                            onRefresh = viewModel::onRefreshSlots
                        )

                        SaveList(
                            onSlotClick = { slot ->
                                viewModel.onSlotClick(slot)
                            },
                            onDeleteClick = viewModel::onSlotDeleteClick,
                            onCreateClick = viewModel::onCreateSlotClick,
                            slotsList = viewModel.saveSystem.cachedAppSave.slots
                        )
                    }
                }

                is Creating -> {
                    CreateSlotPanel(
                        onConfirm = viewModel::onConfirmCreateSlot,
                        onCancel = viewModel::onCancelCreateSlot
                    )
                }

                is ConfirmDelete -> {
                    DeleteConfirmDialog(
                        slot = currentState.slot,
                        onConfirm = { viewModel.onConfirmDeleteSlot(currentState.slot) },
                        onDismiss = viewModel::onCancelDeleteSlot
                    )
                }

                is Loading -> {
                    LoadingIndicator(currentState.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun SaveList(
    i18n: I18n = koinInject(),
    slotsList: List<SaveSlot>,
    onSlotClick: (SaveSlot) -> Unit,
    onDeleteClick: (SaveSlot) -> Unit,
    onCreateClick: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            slotsList.sortedByDescending { it.lastModified },
        ) { slot ->
            val visualStatus = SaveSlotStatue.Synced

            SaveSlotItem(
                slot = slot,
                status = visualStatus,
                onClick = { onSlotClick(slot) },
                onDelete = { onDeleteClick(slot) }
            )
        }

        item {
            OutlinedButton(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Feather.Plus,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = i18n.tr("New save"),
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun SaveSlotItem(
    i18n: I18n = koinInject(),
    slot: SaveSlot,
    status: SaveSlotStatue,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (_, _) = when (status) {
        SaveSlotStatue.Synced -> Feather.Cloud to Color(0xFF4CAF50)
        SaveSlotStatue.LocalOnly -> Feather.Save to Color(0xFFFFB300)
        SaveSlotStatue.CloudOnly -> Feather.DownloadCloud to Color(0xFF2196F3)
        SaveSlotStatue.Conflict -> Feather.AlertTriangle to Color(0xFFF44336)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = slot.playerName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Text(
                        text = slot.lastModified?.let {
                            Humanizer.instant(it)
                        } ?: i18n.tr("No modified time"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Feather.Trash,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CreateSlotPanel(
    i18n: I18n = koinInject(),
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val textState = rememberTextFieldState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // TODO: 加入取名限制
        TextField(
            modifier = Modifier.fillMaxWidth(0.8f),
            state = textState,
            label = {
                Text(i18n.tr("Character name"))
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = InputTransformation.maxLength(8),
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    i18n.tr("Cancel")
                )
            }

            Button(
                onClick = { onConfirm(textState.text.toString()) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
                enabled = textState.text.isNotBlank()
            ) { Text(i18n.tr("Create")) }
        }
    }
}

@Composable
private fun SaveTopBar(
    i18n: I18n = koinInject(),
    onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            i18n.tr("Saves"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onRefresh) {
            Icon(
                Feather.RefreshCw,
                contentDescription = i18n.tr("Refreshing")
            )
        }
    }
}


@Composable
private fun LoadingIndicator(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
private fun DeleteConfirmDialog(
    i18n: I18n = koinInject(),
    slot: SaveSlot,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GameDialog(
        title = i18n.tr("Delete save"),
        isDangerous = true,
        onDismissRequest = onDismiss,
        onConfirmation = onConfirm,
        content = {
            Text(i18n.tr("Delete {0} ?", slot.playerName))
        }
    )
}
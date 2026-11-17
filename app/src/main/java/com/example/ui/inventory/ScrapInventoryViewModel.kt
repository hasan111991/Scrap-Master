package com.example.ui.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScrapInventoryItem
import com.example.data.repository.IScrapInventoryRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface InventoryUiState {
    data object Loading : InventoryUiState
    data class Success(
        val items: List<ScrapInventoryItem>,
        val totalStockKg: Double,
        val totalEstimatedValuePkr: Double,
        val lowStockCount: Int
    ) : InventoryUiState
    data class Error(val message: String) : InventoryUiState
}

class ScrapInventoryViewModel(
    private val repository: IScrapInventoryRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    val uiState: StateFlow<InventoryUiState> = repository.observeAllItems(currentUserId)
        .map<List<ScrapInventoryItem>, InventoryUiState> { items ->
            InventoryUiState.Success(
                items = items,
                totalStockKg = items.sumOf { it.currentStockKg },
                totalEstimatedValuePkr = items.sumOf { it.estimatedTotalValuePkr },
                lowStockCount = items.count { it.isLowStock }
            )
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing scrap inventory", rootError)
            emit(InventoryUiState.Error(rootError.message ?: "Failed to load inventory items"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = InventoryUiState.Loading
        )

    fun addStockItem(item: ScrapInventoryItem) {
        viewModelScope.launch {
            val result = repository.addInventoryItem(item)
            result.fold(
                onSuccess = { id -> _operationStatus.value = "Saved inventory batch ($id)" },
                onFailure = { err -> _operationStatus.value = "Error adding item: ${err.localizedMessage ?: err.message}" }
            )
        }
    }

    fun adjustStock(itemId: String, deltaKg: Double) {
        viewModelScope.launch {
            val result = repository.adjustStock(itemId, deltaKg)
            result.fold(
                onSuccess = { _operationStatus.value = "Stock adjusted by ${if (deltaKg >= 0) "+" else ""}$deltaKg kg" },
                onFailure = { err -> _operationStatus.value = "Error updating stock: ${err.localizedMessage ?: err.message}" }
            )
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteInventoryItem(itemId)
            result.fold(
                onSuccess = { _operationStatus.value = "Inventory record deleted" },
                onFailure = { err -> _operationStatus.value = "Error removing item: ${err.localizedMessage ?: err.message}" }
            )
        }
    }

    fun seedStarterRawalpindiInventory() {
        viewModelScope.launch {
            val starters = listOf(
                ScrapInventoryItem(
                    materialId = "mat_copper_01",
                    name = "Pure Millberry Copper Wire",
                    urduName = "تانبا خالص وائر",
                    category = "COPPER_BRASS",
                    grade = "Grade A+ Bright",
                    currentStockKg = 145.0,
                    minThresholdKg = 50.0,
                    unitPricePkr = 2150.0,
                    warehouseLocation = "Rawalpindi Hub I-10/3"
                ),
                ScrapInventoryItem(
                    materialId = "mat_iron_01",
                    name = "Heavy Structural Iron (HMS-1)",
                    urduName = "بھاری لوہا اسکریپ",
                    category = "IRON_STEEL",
                    grade = "HMS-1",
                    currentStockKg = 1850.0,
                    minThresholdKg = 500.0,
                    unitPricePkr = 145.0,
                    warehouseLocation = "Islamabad Sector I-9 Hub"
                ),
                ScrapInventoryItem(
                    materialId = "mat_cardboard_01",
                    name = "Baled Corrugated Cardboard (Gatta)",
                    urduName = "گتہ اور کارٹن",
                    category = "CARDBOARD",
                    grade = "Dry Baled A",
                    currentStockKg = 42.0,
                    minThresholdKg = 100.0,
                    unitPricePkr = 38.0,
                    warehouseLocation = "Rawalpindi Pir Wadhai Hub"
                )
            )
            starters.forEach { repository.addInventoryItem(it) }
            _operationStatus.value = "Starter Rawalpindi/Islamabad warehouse inventory loaded."
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }

    private companion object {
        const val TAG = "ScrapInventoryVM"
    }
}

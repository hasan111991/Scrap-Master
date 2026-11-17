package com.example

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.ScrapInventoryItem
import com.example.data.repository.IScrapInventoryRepository
import com.example.ui.inventory.InventoryUiState
import com.example.ui.inventory.ScrapInventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScrapInventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        repo: IScrapInventoryRepository,
        store: ViewModelStore
    ): ScrapInventoryViewModel {
        val factory = viewModelFactory {
            initializer { ScrapInventoryViewModel(repo, currentUserId = "uid_123") }
        }
        return ViewModelProvider(store, factory)[ScrapInventoryViewModel::class.java]
    }

    private class FakeScrapInventoryRepository : IScrapInventoryRepository {
        val itemsFlow = MutableStateFlow<List<ScrapInventoryItem>>(emptyList())
        var shouldFailObserve = false

        override fun observeAllItems(userId: String): Flow<List<ScrapInventoryItem>> {
            if (shouldFailObserve) {
                return flow { throw IllegalStateException("Simulated Firestore error") }
            }
            return itemsFlow.map { list -> list.filter { it.userId == userId } }
        }

        override fun observeByCategory(userId: String, category: String): Flow<List<ScrapInventoryItem>> =
            itemsFlow.map { list -> list.filter { it.userId == userId && it.category == category } }

        override fun observeLowStockItems(userId: String): Flow<List<ScrapInventoryItem>> =
            itemsFlow.map { list -> list.filter { it.userId == userId && it.isLowStock } }

        override fun observeItemById(itemId: String): Flow<ScrapInventoryItem?> =
            itemsFlow.map { list -> list.find { it.id == itemId } }

        override suspend fun getItemById(itemId: String): Result<ScrapInventoryItem?> =
            Result.success(itemsFlow.value.find { it.id == itemId })

        override suspend fun addInventoryItem(item: ScrapInventoryItem): Result<String> {
            val id = item.id.ifBlank { "inv_${itemsFlow.value.size + 1}" }
            val saved = item.copy(
                id = id,
                status = if (item.currentStockKg <= item.minThresholdKg) "LOW_STOCK" else "IN_STOCK"
            )
            itemsFlow.value = itemsFlow.value + saved
            return Result.success(id)
        }

        override suspend fun updateInventoryItem(item: ScrapInventoryItem): Result<Unit> {
            itemsFlow.value = itemsFlow.value.map { if (it.id == item.id) item else it }
            return Result.success(Unit)
        }

        override suspend fun adjustStock(itemId: String, deltaKg: Double): Result<Unit> {
            itemsFlow.value = itemsFlow.value.map {
                if (it.id == itemId) {
                    val nextStock = (it.currentStockKg + deltaKg).coerceAtLeast(0.0)
                    it.copy(
                        currentStockKg = nextStock,
                        status = if (nextStock <= it.minThresholdKg) "LOW_STOCK" else "IN_STOCK"
                    )
                } else it
            }
            return Result.success(Unit)
        }

        override suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
            itemsFlow.value = itemsFlow.value.filterNot { it.id == itemId }
            return Result.success(Unit)
        }
    }

    @Test
    fun `uiState computes totals and updates on add, adjust, and delete`() = runTest(testDispatcher) {
        val repo = FakeScrapInventoryRepository()
        val store = ViewModelStore()
        try {
            val vm = createViewModel(repo, store)
            val collectJob = backgroundScope.launch { vm.uiState.collect {} }

            val initial = vm.uiState.first { it is InventoryUiState.Success } as InventoryUiState.Success
            assertEquals(0, initial.items.size)

            vm.addStockItem(
                ScrapInventoryItem(
                    userId = "uid_123",
                    materialId = "mat_copper",
                    name = "Pure Copper Wire",
                    category = "COPPER_BRASS",
                    currentStockKg = 40.0,
                    minThresholdKg = 50.0,
                    unitPricePkr = 2000.0
                )
            )
            advanceUntilIdle()

            val afterAdd = vm.uiState.value as InventoryUiState.Success
            assertEquals(1, afterAdd.items.size)
            assertEquals(40.0, afterAdd.totalStockKg, 0.01)
            assertEquals(80000.0, afterAdd.totalEstimatedValuePkr, 0.01)
            assertEquals(1, afterAdd.lowStockCount)

            val itemId = afterAdd.items.first().id
            vm.adjustStock(itemId, 25.0)
            advanceUntilIdle()

            val afterAdjust = vm.uiState.value as InventoryUiState.Success
            assertEquals(65.0, afterAdjust.totalStockKg, 0.01)
            assertEquals(0, afterAdjust.lowStockCount)

            vm.deleteItem(itemId)
            advanceUntilIdle()

            val afterDelete = vm.uiState.value as InventoryUiState.Success
            assertEquals(0, afterDelete.items.size)

            collectJob.cancel()
        } finally {
            store.clear()
            advanceUntilIdle()
        }
    }

    @Test
    fun `uiState transitions to Error when repository throws`() = runTest(testDispatcher) {
        val repo = FakeScrapInventoryRepository().apply { shouldFailObserve = true }
        val store = ViewModelStore()
        try {
            val vm = createViewModel(repo, store)
            val state = vm.uiState.first { it is InventoryUiState.Error }
            assertTrue(state is InventoryUiState.Error)
            assertTrue((state as InventoryUiState.Error).message.contains("Simulated Firestore error"))
        } finally {
            store.clear()
            advanceUntilIdle()
        }
    }
}

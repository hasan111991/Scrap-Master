package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ScrapInventoryItem
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository interface defining operations for scrap inventory management.
 */
interface IScrapInventoryRepository {
    fun observeAllItems(userId: String): Flow<List<ScrapInventoryItem>>
    fun observeByCategory(userId: String, category: String): Flow<List<ScrapInventoryItem>>
    fun observeLowStockItems(userId: String): Flow<List<ScrapInventoryItem>>
    fun observeItemById(itemId: String): Flow<ScrapInventoryItem?>
    suspend fun getItemById(itemId: String): Result<ScrapInventoryItem?>
    suspend fun addInventoryItem(item: ScrapInventoryItem): Result<String>
    suspend fun updateInventoryItem(item: ScrapInventoryItem): Result<Unit>
    suspend fun adjustStock(itemId: String, deltaKg: Double): Result<Unit>
    suspend fun deleteInventoryItem(itemId: String): Result<Unit>
}

/**
 * Production implementation of [IScrapInventoryRepository] backed by Firebase Firestore.
 * Uses named enterprise database ID (`R.string.firestore_database_id`), user-scoped queries
 * matching `firestore.rules`, server timestamps, and structured error reporting.
 */
class ScrapInventoryRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : IScrapInventoryRepository {

    /**
     * Secondary constructor resolving the provisioned enterprise Firestore database ID.
     */
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val inventoryCollection = firestore.collection(COLLECTION_INVENTORY)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeAllItems(userId: String): Flow<List<ScrapInventoryItem>> =
        inventoryCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScrapInventoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name.lowercase() }
            }

    override fun observeByCategory(userId: String, category: String): Flow<List<ScrapInventoryItem>> =
        inventoryCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScrapInventoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeLowStockItems(userId: String): Flow<List<ScrapInventoryItem>> =
        inventoryCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "LOW_STOCK")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ScrapInventoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeItemById(itemId: String): Flow<ScrapInventoryItem?> =
        inventoryCollection.document(itemId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    ScrapInventoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override suspend fun getItemById(itemId: String): Result<ScrapInventoryItem?> {
        return try {
            require(itemId.isNotBlank()) { "Item ID must not be blank" }
            val doc = inventoryCollection.document(itemId).get().await()
            Result.success(
                doc.toObject(
                    ScrapInventoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$COLLECTION_INVENTORY/$itemId", auth)
            Result.failure(e)
        }
    }

    override suspend fun addInventoryItem(item: ScrapInventoryItem): Result<String> {
        val docId = item.id.ifBlank { inventoryCollection.document().id }
        val docRef = inventoryCollection.document(docId)
        return try {
            val uid = requireUserId()
            require(item.name.isNotBlank() && item.name.length <= 120) { "Invalid item name" }
            require(item.currentStockKg >= 0.0 && item.minThresholdKg >= 0.0 && item.unitPricePkr >= 0.0) {
                "Stock and price values must be non-negative"
            }

            val payload = mapOf(
                "userId" to uid,
                "materialId" to item.materialId.ifBlank { "mat_custom" },
                "name" to item.name.trim(),
                "urduName" to item.urduName.trim(),
                "category" to item.category.ifBlank { "OTHER" },
                "grade" to item.grade.ifBlank { "Standard A" },
                "currentStockKg" to item.currentStockKg,
                "minThresholdKg" to item.minThresholdKg,
                "unitPricePkr" to item.unitPricePkr,
                "warehouseLocation" to item.warehouseLocation.ifBlank { "Rawalpindi Hub" },
                "status" to if (item.currentStockKg <= item.minThresholdKg) "LOW_STOCK" else "IN_STOCK",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { value ->
                when (value) {
                    is String -> value.isNotEmpty()
                    else -> true
                }
            }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun updateInventoryItem(item: ScrapInventoryItem): Result<Unit> {
        val docRef = inventoryCollection.document(item.id)
        return try {
            val uid = requireUserId()
            require(item.id.isNotBlank()) { "Item ID must not be blank for update" }
            if (item.userId.isNotBlank()) {
                check(item.userId == uid) { "Refusing cross-user inventory update" }
            }

            val payload = hashMapOf<String, Any>(
                "materialId" to item.materialId.ifBlank { "mat_custom" },
                "name" to item.name.trim(),
                "category" to item.category.ifBlank { "OTHER" },
                "grade" to item.grade.ifBlank { "Standard A" },
                "currentStockKg" to item.currentStockKg.coerceAtLeast(0.0),
                "minThresholdKg" to item.minThresholdKg.coerceAtLeast(0.0),
                "unitPricePkr" to item.unitPricePkr.coerceAtLeast(0.0),
                "warehouseLocation" to item.warehouseLocation.ifBlank { "Rawalpindi Hub" },
                "status" to if (item.currentStockKg <= item.minThresholdKg) "LOW_STOCK" else "IN_STOCK",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (item.urduName.isNotBlank()) {
                payload["urduName"] = item.urduName.trim()
            }

            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun adjustStock(itemId: String, deltaKg: Double): Result<Unit> {
        val docRef = inventoryCollection.document(itemId)
        return try {
            requireUserId()
            require(itemId.isNotBlank()) { "Item ID must not be blank" }
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStock = snapshot.getDouble("currentStockKg") ?: 0.0
                val minThreshold = snapshot.getDouble("minThresholdKg") ?: 50.0
                val newStock = (currentStock + deltaKg).coerceAtLeast(0.0)
                val newStatus = when {
                    newStock <= 0.0 -> "DEPLETED"
                    newStock <= minThreshold -> "LOW_STOCK"
                    else -> "IN_STOCK"
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "currentStockKg" to newStock,
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        val docRef = inventoryCollection.document(itemId)
        return try {
            requireUserId()
            require(itemId.isNotBlank()) { "Item ID must not be blank" }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_INVENTORY = "scrap_inventory"
    }
}

package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import androidx.annotation.Keep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Keep
@IgnoreExtraProperties
data class FsUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "Rawalpindi",
    val zone: String = "Chaklala Scheme III",
    val role: String = "CUSTOMER",
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsWalletLedgerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "CREDIT",
    val category: String = "PICKUP_PAYOUT",
    val referenceId: String = "",
    val idempotencyKey: String = "",
    val balanceAfter: Double = 0.0,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsPickupRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val city: String = "Rawalpindi",
    val zone: String = "Saddar",
    val address: String = "",
    val itemsSummary: String = "",
    val estimatedWeightKg: Double = 0.0,
    val estimatedTotalPkr: Double = 0.0,
    val verifiedWeightKg: Double = 0.0,
    val verifiedTotalPkr: Double = 0.0,
    val status: String = "ASSIGNED",
    val scheduledDate: String = "Today",
    val timeSlot: String = "02:00 PM - 04:00 PM",
    val paymentMethod: String = "CASH",
    val collectorId: String = "",
    val collectorName: String = "",
    val otpVerified: Boolean = false,
    val digitalReceiptNumber: String = "",
    val aiConfidenceNote: String = "",
    val customerRating: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsPricingRule(
    @DocumentId val id: String = "",
    val materialId: String = "",
    val materialName: String = "",
    val category: String = "",
    val grade: String = "Standard A",
    val city: String = "Rawalpindi",
    val zone: String = "All Zones",
    val marketRate: Double = 0.0,
    val buyingMargin: Double = 0.0,
    val sellingMargin: Double = 0.0,
    val collectionCost: Double = 0.0,
    val sortingCost: Double = 0.0,
    val minPrice: Double = 0.0,
    val maxPrice: Double = 0.0,
    val active: Boolean = true,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsSupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val category: String = "OTHER",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val resolutionNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsMarketplaceLot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lotCode: String = "",
    val title: String = "",
    val materialName: String = "",
    val category: String = "",
    val grade: String = "",
    val weightKg: Double = 0.0,
    val pricePerKg: Double = 0.0,
    val minimumBidPerKg: Double = 0.0,
    val highestBidPerKg: Double = 0.0,
    val highestBidderUid: String = "",
    val bidsCount: Long = 0L,
    val location: String = "",
    val warehouseName: String = "",
    val status: String = "AVAILABLE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Production Firestore repository managing user profiles, pickups, wallet ledger,
 * pricing rules, B2B marketplace lots, support tickets, and immutable audit logs.
 */
class FirestorePlatformRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<FsUserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    FsUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String?,
        city: String = "Rawalpindi",
        zone: String = "Chaklala Scheme III"
    ): Result<FsUserProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val profile = existing.toObject(
                    FsUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: FsUserProfile(userId = uid, displayName = displayName)
                Result.success(profile)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.ifBlank { "Digital Kabariya User" }.take(100),
                    "email" to (email ?: ""),
                    "city" to city,
                    "zone" to zone,
                    "role" to "CUSTOMER",
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { value -> !(value is String && value.isEmpty()) }
                docRef.set(payload).await()
                Result.success(
                    FsUserProfile(
                        id = uid,
                        userId = uid,
                        displayName = displayName,
                        email = email.orEmpty(),
                        city = city,
                        zone = zone,
                        role = "CUSTOMER",
                        status = "ACTIVE"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- PICKUP REQUESTS ---

    fun observeCustomerPickups(userId: String): Flow<List<FsPickupRequest>> =
        db.collection("pickups")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FsPickupRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    fun observeCollectorPickups(collectorId: String): Flow<List<FsPickupRequest>> =
        db.collection("pickups")
            .whereEqualTo("collectorId", collectorId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FsPickupRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }

    suspend fun createPickup(request: FsPickupRequest): Result<String> {
        val docRef = if (request.id.isNotBlank()) {
            db.collection("pickups").document(request.id)
        } else {
            db.collection("pickups").document()
        }
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "userId" to uid,
                "customerName" to request.customerName.ifBlank { "Customer" }.take(100),
                "customerPhone" to request.customerPhone.take(30),
                "city" to request.city.ifBlank { "Rawalpindi" },
                "zone" to request.zone.ifBlank { "Saddar" },
                "address" to request.address.ifBlank { "Rawalpindi" }.take(300),
                "itemsSummary" to request.itemsSummary.ifBlank { "Scrap" }.take(400),
                "estimatedWeightKg" to request.estimatedWeightKg.coerceAtLeast(0.0),
                "estimatedTotalPkr" to request.estimatedTotalPkr.coerceAtLeast(0.0),
                "verifiedWeightKg" to 0.0,
                "verifiedTotalPkr" to 0.0,
                "status" to "ASSIGNED",
                "scheduledDate" to request.scheduledDate.ifBlank { "Today" },
                "timeSlot" to request.timeSlot.ifBlank { "02:00 PM - 04:00 PM" },
                "paymentMethod" to request.paymentMethod.ifBlank { "CASH" },
                "collectorId" to request.collectorId.ifBlank { uid },
                "collectorName" to request.collectorName.ifBlank { "Rashid Khan" },
                "otpVerified" to false,
                "aiConfidenceNote" to request.aiConfidenceNote.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { value -> !(value is String && value.isEmpty()) }

            docRef.set(payload).await()
            writeAuditLog("PICKUP_CREATED", "PickupRequest", docRef.id, "Created pickup in ${request.city}")
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun confirmPickupPayment(
        pickupId: String,
        receiptNumber: String,
        rating: Int = 5
    ): Result<Unit> {
        val docRef = db.collection("pickups").document(pickupId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to "COMPLETED",
                    "digitalReceiptNumber" to receiptNumber,
                    "customerRating" to rating.coerceIn(1, 5),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("PAYMENT_CONFIRMED", "PickupRequest", pickupId, "Digital receipt $receiptNumber")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- WALLET LEDGER ---

    fun observeWalletLedger(userId: String): Flow<List<FsWalletLedgerEntry>> =
        db.collection("users").document(userId).collection("wallet_ledger")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FsWalletLedgerEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun recordLedgerTransaction(entry: FsWalletLedgerEntry): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("wallet_ledger").document()
        return try {
            require(entry.amount > 0.0) { "Transaction amount must be positive" }
            val payload = mapOf(
                "userId" to uid,
                "amount" to entry.amount,
                "type" to entry.type.ifBlank { "CREDIT" },
                "category" to entry.category.ifBlank { "PICKUP_PAYOUT" }.take(50),
                "referenceId" to entry.referenceId.ifBlank { docRef.id }.take(128),
                "idempotencyKey" to entry.idempotencyKey.ifBlank { "IDEMP-${UUID.randomUUID().toString().take(8)}" }.take(128),
                "balanceAfter" to entry.balanceAfter.coerceAtLeast(0.0),
                "description" to entry.description.ifBlank { "Digital Kabariya ledger entry" }.take(300),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- PRICING RULES ---

    fun observeActivePricingRules(): Flow<List<FsPricingRule>> =
        db.collection("pricing_rules")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FsPricingRule::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    // --- MARKETPLACE LOTS ---

    fun observeAvailableMarketplaceLots(): Flow<List<FsMarketplaceLot>> =
        db.collection("marketplace_lots")
            .whereIn("status", listOf("AVAILABLE", "UNDER_BID"))
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FsMarketplaceLot::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun placeBidOnLot(lotId: String, bidPerKg: Double, nextBidsCount: Long): Result<Unit> {
        val docRef = db.collection("marketplace_lots").document(lotId)
        return try {
            val uid = requireUserId()
            docRef.update(
                mapOf(
                    "highestBidPerKg" to bidPerKg,
                    "highestBidderUid" to uid,
                    "bidsCount" to nextBidsCount,
                    "status" to "UNDER_BID",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- SUPPORT TICKETS ---

    fun observeSupportTickets(userId: String): Flow<List<FsSupportTicket>> =
        db.collection("support_tickets")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FsSupportTicket::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun createSupportTicket(
        userName: String,
        category: String,
        subject: String,
        message: String
    ): Result<String> {
        val docRef = db.collection("support_tickets").document()
        return try {
            val uid = requireUserId()
            val cleanSubject = subject.trim()
            val cleanMessage = message.trim()
            require(cleanSubject.length >= 2) { "Subject must be at least 2 characters" }
            require(cleanMessage.length >= 5) { "Message must be at least 5 characters" }
            val payload = mapOf(
                "userId" to uid,
                "userName" to userName.ifBlank { "Customer" }.take(100),
                "category" to category.ifBlank { "OTHER" }.take(60),
                "subject" to cleanSubject.take(150),
                "message" to cleanMessage.take(1500),
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- AUDIT LOGS ---

    suspend fun writeAuditLog(
        action: String,
        entity: String,
        entityId: String,
        details: String,
        actorRole: String = "USER"
    ) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("audit_logs").document()
        try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "actorRole" to actorRole.take(60),
                    "action" to action.take(80),
                    "entity" to entity.take(80),
                    "entityId" to entityId.take(128),
                    "details" to details.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
        }
    }
}

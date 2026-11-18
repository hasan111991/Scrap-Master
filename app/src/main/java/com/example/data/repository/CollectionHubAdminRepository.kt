package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.CollectionHubCatalog
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Admin-only operations repository for the private /hub-admin portal.
 * All writes are strictly validated by firestore.rules isAdmin().
 */
class CollectionHubAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAdminUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Admin authentication required.")

    // --- ADMIN REALTIME STREAMS ---

    fun observeAllBookings(): Flow<List<HubBooking>> =
        db.collection("bookings").snapshots().map { snap ->
            snap.toObjects(HubBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllPoolItems(): Flow<List<HubPickupPoolItem>> =
        db.collection("pickup_pool").snapshots().map { snap ->
            snap.toObjects(HubPickupPoolItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllCustomers(): Flow<List<HubCustomerProfile>> =
        db.collection("customers").snapshots().map { snap ->
            snap.toObjects(HubCustomerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllCollectors(): Flow<List<HubCollectorProfile>> =
        db.collection("collectors").snapshots().map { snap ->
            snap.toObjects(HubCollectorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllPayments(): Flow<List<HubManualPayment>> =
        db.collection("payments").snapshots().map { snap ->
            snap.toObjects(HubManualPayment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllCommissions(): Flow<List<HubCommissionRecord>> =
        db.collection("commissions").snapshots().map { snap ->
            snap.toObjects(HubCommissionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllSettlements(): Flow<List<HubCommissionSettlement>> =
        db.collection("commission_settlements").snapshots().map { snap ->
            snap.toObjects(HubCommissionSettlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllComplaints(): Flow<List<HubComplaint>> =
        db.collection("complaints").snapshots().map { snap ->
            snap.toObjects(HubComplaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeAllAuditLogs(): Flow<List<HubAuditLog>> =
        db.collection("audit_logs").snapshots().map { snap ->
            snap.toObjects(HubAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    // --- BOOTSTRAP INITIAL LAUNCH CONFIGURATION (RAWALPINDI & ISLAMABAD) ---

    suspend fun bootstrapInitialPlatformDataIfEmpty(): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val batch = db.batch()
            var hasWrites = false

            if (db.collection("scrap_categories").limit(1).get().await().isEmpty) {
                CollectionHubCatalog.initialCategories.forEach { cat ->
                    batch.set(
                        db.collection("scrap_categories").document(cat.id),
                        mapOf(
                            "name" to cat.name,
                            "urduName" to cat.urduName,
                            "description" to cat.description,
                            "unitOptions" to cat.unitOptions,
                            "active" to true,
                            "sortOrder" to cat.sortOrder,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                hasWrites = true
            }

            if (db.collection("scrap_prices").limit(1).get().await().isEmpty) {
                CollectionHubCatalog.seedIndicativePriceSeeds.forEach { p ->
                    batch.set(
                        db.collection("scrap_prices").document(p.id),
                        mapOf(
                            "categoryId" to p.categoryId,
                            "categoryName" to p.categoryName,
                            "unit" to p.unit,
                            "minEstimatedPkr" to p.minEstimatedPkr,
                            "maxEstimatedPkr" to p.maxEstimatedPkr,
                            "indicative" to true,
                            "city" to p.city,
                            "active" to true,
                            "updatedBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                hasWrites = true
            }

            if (db.collection("payment_methods").limit(1).get().await().isEmpty) {
                val methods = listOf(
                    Triple("pm_cash", "Cash", "Pay cash in hand at doorstep after weight confirmation."),
                    Triple("pm_easypaisa", "Easypaisa", "Transfer manually via Easypaisa & record TID for admin verification."),
                    Triple("pm_bank", "Bank Transfer", "IBFT / Raast transfer manually & record bank reference.")
                )
                methods.forEach { (id, name, instr) ->
                    batch.set(
                        db.collection("payment_methods").document(id),
                        mapOf(
                            "name" to name,
                            "accountTitle" to "Collection Hub Pakistan",
                            "accountNumberOrNote" to if (name == "Cash") "Cash on Pickup" else "Configured by Admin",
                            "instructions" to instr,
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                hasWrites = true
            }

            if (db.collection("service_areas").limit(1).get().await().isEmpty) {
                CollectionHubCatalog.areasByCity.forEach { (city, areas) ->
                    areas.forEachIndexed { idx, areaName ->
                        val docId = "area_${city.lowercase()}_${idx + 1}"
                        batch.set(
                            db.collection("service_areas").document(docId),
                            mapOf(
                                "city" to city,
                                "areaName" to areaName,
                                "matchingRadiusKm" to 5.0,
                                "active" to true,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                hasWrites = true
            }

            if (!db.collection("app_settings").document("global").get().await().exists()) {
                val s = CollectionHubCatalog.initialDefaultSettings
                batch.set(
                    db.collection("app_settings").document("global"),
                    mapOf(
                        "defaultMatchingRadiusKm" to s.defaultMatchingRadiusKm,
                        "commissionRuleType" to s.commissionRuleType,
                        "defaultCommissionPercent" to s.defaultCommissionPercent,
                        "defaultFixedCommissionPkr" to s.defaultFixedCommissionPkr,
                        "supportPhone" to s.supportPhone,
                        "supportEmail" to s.supportEmail,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                hasWrites = true
            }

            if (hasWrites) batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admin_bootstrap", auth)
            Result.failure(e)
        }
    }

    // --- COLLECTOR APPROVALS ---

    suspend fun updateCollectorApproval(
        collectorId: String,
        newStatus: String,
        rejectionReason: String = ""
    ): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val updates = mutableMapOf<String, Any>(
                "approvalStatus" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus != "approved") {
                updates["availabilityOnline"] = false
            }
            if (rejectionReason.isNotBlank()) {
                updates["rejectionReason"] = rejectionReason.trim().take(300)
            }
            db.collection("collectors").document(collectorId).update(updates).await()
            writeAudit(uid, "COLLECTOR_${newStatus.uppercase()}", "collector", collectorId, rejectionReason.ifBlank { "Updated to $newStatus" })
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "collectors/$collectorId", auth)
            Result.failure(e)
        }
    }

    // --- MANUAL BOOKING ASSIGN / REASSIGN / CANCEL ---

    suspend fun adminAssignCollector(
        bookingId: String,
        collector: HubCollectorProfile
    ): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val batch = db.batch()
            batch.update(
                db.collection("bookings").document(bookingId),
                mapOf(
                    "status" to "assigned",
                    "assignedCollectorId" to collector.collectorId,
                    "assignedCollectorName" to "${collector.fullName} (${collector.shopOrBusinessName})".take(120),
                    "assignedCollectorPhone" to collector.phone.take(30),
                    "assignedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("pickup_pool").document(bookingId),
                mapOf(
                    "status" to "locked",
                    "assignedCollectorId" to collector.collectorId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            writeAudit(uid, "ADMIN_ASSIGN_COLLECTOR", "booking", bookingId, "Assigned to ${collector.fullName}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "bookings/$bookingId", auth)
            Result.failure(e)
        }
    }

    // --- SCRAP CATEGORIES & DYNAMIC INDICATIVE PRICES ---

    suspend fun saveScrapCategory(category: HubScrapCategory): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = category.id.ifBlank { "cat_${System.currentTimeMillis()}" }
        val ref = db.collection("scrap_categories").document(docId)
        return try {
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "urduName" to category.urduName.trim().ifBlank { category.name }.take(80),
                        "description" to category.description.trim().ifBlank { category.name }.take(240),
                        "unitOptions" to category.unitOptions.ifEmpty { listOf("KG") }.take(5),
                        "active" to category.active,
                        "sortOrder" to category.sortOrder,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "urduName" to category.urduName.trim().ifBlank { category.name }.take(80),
                        "description" to category.description.trim().ifBlank { category.name }.take(240),
                        "unitOptions" to category.unitOptions.ifEmpty { listOf("KG") }.take(5),
                        "active" to category.active,
                        "sortOrder" to category.sortOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAudit(uid, "SCRAP_CATEGORY_SAVED", "scrap_category", docId, category.name)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
            Result.failure(e)
        }
    }

    suspend fun saveScrapPrice(price: HubScrapPrice): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = price.id.ifBlank { "price_${System.currentTimeMillis()}" }
        val ref = db.collection("scrap_prices").document(docId)
        return try {
            val safeMin = price.minEstimatedPkr.coerceAtLeast(0.0)
            val safeMax = maxOf(safeMin, price.maxEstimatedPkr)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "categoryName" to price.categoryName.trim().take(80),
                        "unit" to price.unit,
                        "minEstimatedPkr" to safeMin,
                        "maxEstimatedPkr" to safeMax,
                        "indicative" to true,
                        "city" to price.city.trim().ifBlank { "Rawalpindi" }.take(60),
                        "active" to price.active,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "categoryId" to price.categoryId.ifBlank { docId },
                        "categoryName" to price.categoryName.trim().take(80),
                        "unit" to price.unit,
                        "minEstimatedPkr" to safeMin,
                        "maxEstimatedPkr" to safeMax,
                        "indicative" to true,
                        "city" to price.city.trim().ifBlank { "Rawalpindi" }.take(60),
                        "active" to price.active,
                        "updatedBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAudit(uid, "INDICATIVE_PRICE_UPDATED", "scrap_price", docId, "${price.categoryName}: Rs.${safeMin.toInt()}-${safeMax.toInt()} / ${price.unit}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
            Result.failure(e)
        }
    }

    // --- MANUAL PAYMENTS & COMMISSION SETTLEMENTS ---

    suspend fun verifyManualPayment(paymentId: String, status: String): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("payments").document(paymentId).update(
                mapOf(
                    "verificationStatus" to status,
                    "verifiedByAdminUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAudit(uid, "PAYMENT_${status.uppercase()}", "payment", paymentId, "Admin set status=$status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "payments/$paymentId", auth)
            Result.failure(e)
        }
    }

    suspend fun recordCommissionSettlement(
        collectorId: String,
        collectorName: String,
        periodLabel: String,
        unpaidCommissions: List<HubCommissionRecord>,
        paymentMethod: String,
        referenceNote: String
    ): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            require(unpaidCommissions.isNotEmpty()) { "No unpaid commissions selected to settle." }
            val total = unpaidCommissions.sumOf { it.commissionAmountPkr }
            require(total > 0.0) { "Total settlement amount must be greater than zero." }
            val settlementRef = db.collection("commission_settlements").document()
            val batch = db.batch()
            batch.set(
                settlementRef,
                mapOf(
                    "collectorId" to collectorId,
                    "collectorName" to collectorName.take(120),
                    "periodLabel" to periodLabel.trim().ifBlank { "Weekly Settlement" }.take(80),
                    "totalAmountPkr" to total,
                    "commissionIds" to unpaidCommissions.map { it.id }.take(100),
                    "paymentMethod" to paymentMethod,
                    "referenceNote" to referenceNote.trim().ifBlank { "Manual Settlement" }.take(200),
                    "recordedByAdminUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            unpaidCommissions.take(100).forEach { comm ->
                batch.update(
                    db.collection("commissions").document(comm.id),
                    mapOf(
                        "status" to "settled",
                        "settlementId" to settlementRef.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            writeAudit(uid, "COMMISSION_SETTLED", "commission_settlement", settlementRef.id, "$collectorName: Rs.${total.toInt()} (${unpaidCommissions.size} records)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "commission_settlements", auth)
            Result.failure(e)
        }
    }

    // --- SERVICE AREAS, COMPLAINTS & APP SETTINGS ---

    suspend fun saveServiceArea(city: String, areaName: String, radiusKm: Double): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val ref = db.collection("service_areas").document()
        return try {
            ref.set(
                mapOf(
                    "city" to if (city in CollectionHubCatalog.supportedCities) city else "Rawalpindi",
                    "areaName" to areaName.trim().take(80),
                    "matchingRadiusKm" to radiusKm.coerceIn(1.0, 50.0),
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAudit(uid, "SERVICE_AREA_ADDED", "service_area", ref.id, "$areaName, $city (${radiusKm}km)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path, auth)
            Result.failure(e)
        }
    }

    suspend fun saveGlobalSettings(settings: HubAppSettings): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("app_settings").document("global").set(
                mapOf(
                    "defaultMatchingRadiusKm" to settings.defaultMatchingRadiusKm.coerceIn(1.0, 50.0),
                    "commissionRuleType" to settings.commissionRuleType,
                    "defaultCommissionPercent" to settings.defaultCommissionPercent.coerceIn(0.0, 50.0),
                    "defaultFixedCommissionPkr" to settings.defaultFixedCommissionPkr.coerceAtLeast(0.0),
                    "supportPhone" to settings.supportPhone.trim().ifBlank { "+92 51 000 0000" }.take(30),
                    "supportEmail" to settings.supportEmail.trim().ifBlank { "support@collectionhub.pk" }.take(120),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAudit(uid, "SETTINGS_UPDATED", "app_settings", "global", "Radius=${settings.defaultMatchingRadiusKm}km, Rule=${settings.commissionRuleType}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_settings/global", auth)
            Result.failure(e)
        }
    }

    suspend fun updateComplaintStatus(complaintId: String, status: String, adminNotes: String): Result<Unit> {
        val uid = try {
            requireAdminUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (adminNotes.isNotBlank()) updates["adminNotes"] = adminNotes.trim().take(1000)
            db.collection("complaints").document(complaintId).update(updates).await()
            writeAudit(uid, "COMPLAINT_${status.uppercase()}", "complaint", complaintId, adminNotes.take(200))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "complaints/$complaintId", auth)
            Result.failure(e)
        }
    }

    private suspend fun writeAudit(uid: String, action: String, entity: String, entityId: String, details: String) {
        try {
            db.collection("audit_logs").document().set(
                mapOf(
                    "actorUid" to uid,
                    "actorRole" to "admin",
                    "action" to action.take(80),
                    "entity" to entity.take(80),
                    "entityId" to entityId.take(128),
                    "details" to details.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) { }
    }
}

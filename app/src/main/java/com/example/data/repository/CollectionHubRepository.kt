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
import java.util.UUID

class CollectionHubRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google first.")

    suspend fun checkIsVerifiedAdmin(uid: String, email: String?, emailVerified: Boolean): Boolean {
        if (email == "hasanaamir815@gmail.com" && emailVerified) return true
        return try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    // --- USER & CUSTOMER ACCOUNT ---

    fun observeUserAccount(uid: String): Flow<HubUserAccount?> =
        db.collection("users").document(uid).snapshots().map { snap ->
            snap.toObject(HubUserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeCustomerProfile(uid: String): Flow<HubCustomerProfile?> =
        db.collection("customers").document(uid).snapshots().map { snap ->
            snap.toObject(HubCustomerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    suspend fun ensureCustomerSession(
        displayName: String,
        email: String?,
        city: String = "Rawalpindi",
        area: String = "Chaklala Scheme III"
    ): Result<HubUserAccount> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userRef = db.collection("users").document(uid)
        val custRef = db.collection("customers").document(uid)
        return try {
            val snap = userRef.get().await()
            val safeName = displayName.ifBlank { "Collection Hub Member" }.take(100)
            val safeCity = if (city in CollectionHubCatalog.supportedCities) city else "Rawalpindi"
            if (!snap.exists()) {
                val userPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to (email ?: ""),
                    "city" to safeCity,
                    "area" to area,
                    "role" to "customer",
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { !(it is String && it.isEmpty()) }
                userRef.set(userPayload).await()
            }
            if (!custRef.get().await().exists()) {
                val custPayload = mapOf(
                    "customerId" to uid,
                    "name" to safeName,
                    "email" to (email ?: ""),
                    "city" to safeCity,
                    "area" to area,
                    "status" to "active",
                    "totalBookings" to 0L,
                    "totalCompletedKg" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { !(it is String && it.isEmpty()) }
                custRef.set(custPayload).await()
            }
            val latest = userRef.get().await().toObject(
                HubUserAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: HubUserAccount(userId = uid, displayName = safeName, email = email.orEmpty(), city = safeCity, area = area)
            Result.success(latest)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerProfile(
        name: String,
        phone: String,
        city: String,
        area: String,
        defaultAddress: String
    ): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val safeCity = if (city in CollectionHubCatalog.supportedCities) city else "Rawalpindi"
            val cleanName = name.trim().ifBlank { "Customer" }.take(100)
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "displayName" to cleanName,
                    "phone" to phone.trim().take(30),
                    "city" to safeCity,
                    "area" to area.trim().take(80),
                    "defaultAddress" to defaultAddress.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("customers").document(uid),
                mapOf(
                    "name" to cleanName,
                    "phone" to phone.trim().take(30),
                    "city" to safeCity,
                    "area" to area.trim().take(80),
                    "defaultAddress" to defaultAddress.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "customers/$uid", auth)
            Result.failure(e)
        }
    }

    // --- REFERENCE DATA STREAMS ---

    fun observeScrapCategories(): Flow<List<HubScrapCategory>> =
        db.collection("scrap_categories").snapshots().map { snap ->
            snap.toObjects(HubScrapCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.sortOrder }
        }

    fun observeScrapPrices(): Flow<List<HubScrapPrice>> =
        db.collection("scrap_prices").snapshots().map { snap ->
            snap.toObjects(HubScrapPrice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.categoryName }
        }

    fun observePaymentMethods(): Flow<List<HubPaymentMethodConfig>> =
        db.collection("payment_methods").snapshots().map { snap ->
            snap.toObjects(HubPaymentMethodConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeServiceAreas(): Flow<List<HubServiceArea>> =
        db.collection("service_areas").snapshots().map { snap ->
            snap.toObjects(HubServiceArea::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { "${it.city}_${it.areaName}" }
        }

    fun observeGlobalSettings(): Flow<HubAppSettings?> =
        db.collection("app_settings").document("global").snapshots().map { snap ->
            snap.toObject(HubAppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeNotifications(uid: String): Flow<List<HubNotificationItem>> =
        db.collection("notifications").whereEqualTo("recipientUid", uid).snapshots().map { snap ->
            snap.toObjects(HubNotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeCustomerComplaints(uid: String): Flow<List<HubComplaint>> =
        db.collection("complaints").whereEqualTo("userId", uid).snapshots().map { snap ->
            snap.toObjects(HubComplaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    // --- CUSTOMER BOOKINGS & WORKFLOW ---

    fun observeCustomerBookings(uid: String): Flow<List<HubBooking>> =
        db.collection("bookings").whereEqualTo("customerId", uid).snapshots().map { snap ->
            snap.toObjects(HubBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeCustomerPayments(uid: String): Flow<List<HubManualPayment>> =
        db.collection("payments").whereEqualTo("customerId", uid).snapshots().map { snap ->
            snap.toObjects(HubManualPayment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    suspend fun createBookingAtomic(
        customerName: String,
        customerPhone: String,
        city: String,
        area: String,
        fullAddress: String,
        categories: List<String>,
        itemsSummary: String,
        unitType: String,
        estimatedWeightKg: Double,
        estimatedMinPkr: Double,
        estimatedMaxPkr: Double,
        preferredDate: String,
        preferredTimeSlot: String,
        paymentMethod: String,
        photoUri: String?,
        notes: String?,
        matchingRadiusKm: Double
    ): Result<String> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val bookingId = "bk_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val bookingRef = db.collection("bookings").document(bookingId)
        val poolRef = db.collection("pickup_pool").document(bookingId)
        return try {
            require(categories.isNotEmpty()) { "Select at least one scrap category." }
            require(fullAddress.trim().length >= 5) { "Enter a complete street/house address." }
            require(customerPhone.trim().length >= 5) { "Enter a valid contact phone number." }

            val otpCode = (1000..9999).random().toString()
            val safeCity = if (city in CollectionHubCatalog.supportedCities) city else "Rawalpindi"
            val safeUnit = if (unitType in listOf("KG", "Piece", "Bag", "Bulk")) unitType else "KG"
            val safePayment = if (paymentMethod in listOf("Cash", "Easypaisa", "Bank Transfer")) paymentMethod else "Cash"
            val safeRadius = matchingRadiusKm.coerceIn(1.0, 50.0)
            val safeMin = estimatedMinPkr.coerceAtLeast(0.0)
            val safeMax = maxOf(safeMin, estimatedMaxPkr)

            val bookingPayload = mutableMapOf<String, Any>(
                "customerId" to uid,
                "customerName" to customerName.trim().ifBlank { "Customer" }.take(100),
                "customerPhone" to customerPhone.trim().take(30),
                "city" to safeCity,
                "area" to area.trim().ifBlank { "Saddar" }.take(80),
                "fullAddress" to fullAddress.trim().take(350),
                "categories" to categories.take(12),
                "itemsSummary" to itemsSummary.trim().ifBlank { categories.joinToString(", ") }.take(500),
                "unitType" to safeUnit,
                "estimatedWeightKg" to estimatedWeightKg.coerceAtLeast(0.0),
                "estimatedMinPkr" to safeMin,
                "estimatedMaxPkr" to safeMax,
                "preferredDate" to preferredDate.trim().ifBlank { "Today" }.take(60),
                "preferredTimeSlot" to preferredTimeSlot.trim().ifBlank { "Flexible" }.take(60),
                "paymentMethod" to safePayment,
                "status" to "searching",
                "otpCode" to otpCode,
                "otpVerified" to false,
                "verifiedWeightKg" to 0.0,
                "finalAmountPkr" to 0.0,
                "customerConfirmed" to false,
                "paymentConfirmedByCustomer" to false,
                "matchingRadiusKm" to safeRadius,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUri.isNullOrBlank()) bookingPayload["photoUri"] = photoUri.take(500)
            if (!notes.isNullOrBlank()) bookingPayload["notes"] = notes.trim().take(500)

            val qtyLabel = "${estimatedWeightKg.toInt().coerceAtLeast(1)} $safeUnit"
            val poolPayload = mapOf(
                "bookingId" to bookingId,
                "customerId" to uid,
                "city" to safeCity,
                "area" to area.trim().ifBlank { "Saddar" }.take(80),
                "categories" to categories.take(12),
                "itemsSummary" to categories.joinToString(", ").take(300),
                "unitType" to safeUnit,
                "estimatedQuantityLabel" to qtyLabel.take(60),
                "preferredDate" to preferredDate.trim().ifBlank { "Today" }.take(60),
                "preferredTimeSlot" to preferredTimeSlot.trim().ifBlank { "Flexible" }.take(60),
                "status" to "searching",
                "radiusKm" to safeRadius,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(bookingRef, bookingPayload)
            batch.set(poolRef, poolPayload)
            batch.commit().await()

            createNotification(
                recipientUid = uid,
                title = "Booking Submitted",
                body = "Searching nearby collectors in $area, $safeCity. OTP: $otpCode",
                type = "booking_created",
                referenceId = bookingId
            )
            writeAuditLog(uid, "customer", "BOOKING_CREATED", "booking", bookingId, "Created in $area, $safeCity")
            Result.success(bookingId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, bookingRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun cancelBookingAtomic(bookingId: String, reason: String): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val batch = db.batch()
            batch.update(
                db.collection("bookings").document(bookingId),
                mapOf(
                    "status" to "cancelled",
                    "cancellationReason" to reason.trim().ifBlank { "Cancelled by customer" }.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("pickup_pool").document(bookingId),
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            writeAuditLog(uid, "customer", "BOOKING_CANCELLED", "booking", bookingId, reason.take(200))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "bookings/$bookingId", auth)
            Result.failure(e)
        }
    }

    suspend fun customerConfirmCompletionAndPayment(
        booking: HubBooking,
        rating: Int,
        review: String,
        settings: HubAppSettings
    ): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val bookingRef = db.collection("bookings").document(booking.id)
        return try {
            val cleanReview = review.trim().take(400)
            val updates = mutableMapOf<String, Any>(
                "status" to "completed",
                "customerConfirmed" to true,
                "paymentConfirmedByCustomer" to true,
                "rating" to rating.coerceIn(1, 5),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanReview.isNotEmpty()) updates["review"] = cleanReview
            bookingRef.update(updates).await()

            // Record manual payment submission if collector didn't submit yet
            val payId = "pay_${booking.id}"
            val payRef = db.collection("payments").document(payId)
            if (!payRef.get().await().exists() && booking.assignedCollectorId.isNotBlank()) {
                payRef.set(
                    mapOf(
                        "bookingId" to booking.id,
                        "customerId" to uid,
                        "collectorId" to booking.assignedCollectorId,
                        "method" to booking.paymentMethod,
                        "amountPkr" to booking.finalAmountPkr.coerceAtLeast(0.0),
                        "submittedByUid" to uid,
                        "proofNote" to "Confirmed received by customer",
                        "customerConfirmed" to true,
                        "verificationStatus" to "pending_verification",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (payRef.get().await().exists()) {
                payRef.update(
                    mapOf(
                        "customerConfirmed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Duplicate-safe deterministic commission record: docId = comm_<bookingId>
            if (booking.assignedCollectorId.isNotBlank()) {
                val commRef = db.collection("commissions").document("comm_${booking.id}")
                if (!commRef.get().await().exists()) {
                    val commAmount = CollectionHubCatalog.computeCommissionAmount(
                        bookingAmountPkr = booking.finalAmountPkr,
                        categories = booking.categories,
                        settings = settings
                    )
                    val rateApplied = if (settings.commissionRuleType == "fixed") {
                        settings.defaultFixedCommissionPkr
                    } else {
                        settings.defaultCommissionPercent
                    }
                    commRef.set(
                        mapOf(
                            "bookingId" to booking.id,
                            "collectorId" to booking.assignedCollectorId,
                            "collectorName" to booking.assignedCollectorName.ifBlank { "Collector" },
                            "ruleType" to settings.commissionRuleType,
                            "rateApplied" to rateApplied,
                            "bookingAmountPkr" to booking.finalAmountPkr,
                            "commissionAmountPkr" to commAmount,
                            "status" to "unpaid",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                createNotification(
                    recipientUid = booking.assignedCollectorId,
                    title = "Pickup Completed",
                    body = "Customer confirmed Rs. ${booking.finalAmountPkr.toInt()} for #${booking.id.take(8)}.",
                    type = "booking_completed",
                    referenceId = booking.id
                )
            }
            writeAuditLog(uid, "customer", "BOOKING_COMPLETED", "booking", booking.id, "Confirmed Rs.${booking.finalAmountPkr.toInt()}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path, auth)
            Result.failure(e)
        }
    }

    // --- COLLECTOR PORTAL ---

    fun observeCollectorProfile(uid: String): Flow<HubCollectorProfile?> =
        db.collection("collectors").document(uid).snapshots().map { snap ->
            snap.toObject(HubCollectorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeOpenPickupPool(): Flow<List<HubPickupPoolItem>> =
        db.collection("pickup_pool").whereEqualTo("status", "searching").snapshots().map { snap ->
            snap.toObjects(HubPickupPoolItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeCollectorAssignedBookings(collectorUid: String): Flow<List<HubBooking>> =
        db.collection("bookings").whereEqualTo("assignedCollectorId", collectorUid).snapshots().map { snap ->
            snap.toObjects(HubBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeCollectorCommissions(collectorUid: String): Flow<List<HubCommissionRecord>> =
        db.collection("commissions").whereEqualTo("collectorId", collectorUid).snapshots().map { snap ->
            snap.toObjects(HubCommissionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    fun observeCollectorSettlements(collectorUid: String): Flow<List<HubCommissionSettlement>> =
        db.collection("commission_settlements").whereEqualTo("collectorId", collectorUid).snapshots().map { snap ->
            snap.toObjects(HubCommissionSettlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    suspend fun registerCollector(
        fullName: String,
        shopOrBusinessName: String,
        phone: String,
        city: String,
        serviceAreas: List<String>,
        acceptedCategories: List<String>,
        vehicleType: String,
        address: String,
        cnicLast4: String
    ): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val colRef = db.collection("collectors").document(uid)
        return try {
            require(fullName.trim().length >= 2) { "Full name is required." }
            require(shopOrBusinessName.trim().length >= 2) { "Shop or business name is required." }
            require(phone.trim().length >= 7) { "Valid phone number is required." }
            require(serviceAreas.isNotEmpty()) { "Select at least one service area." }
            require(acceptedCategories.isNotEmpty()) { "Select at least one scrap category." }

            val safeCity = if (city in CollectionHubCatalog.supportedCities) city else "Rawalpindi"
            val payload = mutableMapOf<String, Any>(
                "collectorId" to uid,
                "userId" to uid,
                "fullName" to fullName.trim().take(100),
                "shopOrBusinessName" to shopOrBusinessName.trim().take(120),
                "phone" to phone.trim().take(30),
                "city" to safeCity,
                "serviceAreas" to serviceAreas.take(20),
                "acceptedCategories" to acceptedCategories.take(12),
                "vehicleType" to vehicleType.trim().ifBlank { "Suzuki Pickup" }.take(80),
                "availabilityOnline" to false,
                "approvalStatus" to "pending_approval",
                "radiusKm" to 5.0,
                "completedPickupsCount" to 0L,
                "totalEarningsPkr" to 0.0,
                "commissionDuePkr" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (address.isNotBlank()) payload["address"] = address.trim().take(300)
            if (cnicLast4.isNotBlank()) payload["cnicLast4"] = cnicLast4.trim().take(16)

            colRef.set(payload).await()
            db.collection("users").document(uid).update(
                mapOf("role" to "collector", "updatedAt" to FieldValue.serverTimestamp())
            ).await()

            writeAuditLog(uid, "collector", "COLLECTOR_REGISTERED", "collector", uid, "Submitted for admin approval in $safeCity")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun toggleCollectorOnline(online: Boolean): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            db.collection("collectors").document(uid).update(
                mapOf(
                    "availabilityOnline" to online,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "collectors/$uid", auth)
            Result.failure(e)
        }
    }

    /**
     * Atomic first-accept transaction locking: prevents two collectors from accepting
     * the same booking simultaneously. Only locks pickup_pool + assigns bookings atomically.
     */
    suspend fun acceptPoolBookingAtomic(
        poolItem: HubPickupPoolItem,
        collector: HubCollectorProfile
    ): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val poolRef = db.collection("pickup_pool").document(poolItem.bookingId)
        val bookingRef = db.collection("bookings").document(poolItem.bookingId)
        return try {
            check(collector.approvalStatus == "approved") { "Admin approval is required before accepting pickups." }
            db.runTransaction { tx ->
                val snap = tx.get(poolRef)
                if (!snap.exists() || snap.getString("status") != "searching") {
                    throw IllegalStateException("This pickup was already claimed by another collector.")
                }
                tx.update(
                    poolRef,
                    mapOf(
                        "status" to "locked",
                        "assignedCollectorId" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    bookingRef,
                    mapOf(
                        "status" to "assigned",
                        "assignedCollectorId" to uid,
                        "assignedCollectorName" to "${collector.fullName} (${collector.shopOrBusinessName})".take(120),
                        "assignedCollectorPhone" to collector.phone.take(30),
                        "assignedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()

            createNotification(
                recipientUid = poolItem.customerId,
                title = "Collector Assigned!",
                body = "${collector.fullName} (${collector.shopOrBusinessName}) accepted your pickup in ${poolItem.area}.",
                type = "collector_assigned",
                referenceId = poolItem.bookingId
            )
            writeAuditLog(uid, "collector", "BOOKING_ACCEPTED", "booking", poolItem.bookingId, "Atomic first-accept lock won")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun advanceCollectorWorkflow(
        booking: HubBooking,
        nextStatus: String,
        otpVerified: Boolean = booking.otpVerified,
        verifiedWeightKg: Double = booking.verifiedWeightKg,
        finalAmountPkr: Double = booking.finalAmountPkr,
        collectorNotes: String = booking.collectorNotes
    ): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val bookingRef = db.collection("bookings").document(booking.id)
        return try {
            val payload = mutableMapOf<String, Any>(
                "status" to nextStatus,
                "otpVerified" to otpVerified,
                "verifiedWeightKg" to verifiedWeightKg.coerceAtLeast(0.0),
                "finalAmountPkr" to finalAmountPkr.coerceAtLeast(0.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (collectorNotes.isNotBlank()) payload["collectorNotes"] = collectorNotes.trim().take(500)
            bookingRef.update(payload).await()

            createNotification(
                recipientUid = booking.customerId,
                title = "Pickup Status: ${BookingStatus.fromCode(nextStatus).label}",
                body = when (nextStatus) {
                    "on_the_way" -> "Your collector is on the way to ${booking.area}."
                    "arrived" -> "Collector has arrived! Share OTP ${booking.otpCode} to start weighing."
                    "weight_price_confirmation" -> "Verified weight: ${verifiedWeightKg}kg • Final Amount: Rs.${finalAmountPkr.toInt()}. Please confirm."
                    else -> "Booking updated."
                },
                type = "status_update",
                referenceId = booking.id
            )
            writeAuditLog(uid, "collector", "BOOKING_STATUS_$nextStatus", "booking", booking.id, "Weight=$verifiedWeightKg, Final=Rs.${finalAmountPkr.toInt()}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, bookingRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun submitManualPaymentProof(
        booking: HubBooking,
        method: String,
        amountPkr: Double,
        referenceNumber: String,
        proofNote: String
    ): Result<Unit> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val payRef = db.collection("payments").document("pay_${booking.id}")
        return try {
            val payload = mutableMapOf<String, Any>(
                "bookingId" to booking.id,
                "customerId" to booking.customerId,
                "collectorId" to booking.assignedCollectorId.ifBlank { uid },
                "method" to method,
                "amountPkr" to amountPkr.coerceAtLeast(0.0),
                "submittedByUid" to uid,
                "customerConfirmed" to (uid == booking.customerId),
                "verificationStatus" to "pending_verification",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (referenceNumber.isNotBlank()) payload["referenceNumber"] = referenceNumber.trim().take(100)
            if (proofNote.isNotBlank()) payload["proofNote"] = proofNote.trim().take(300)

            payRef.set(payload).await()
            writeAuditLog(uid, "user", "PAYMENT_PROOF_SUBMITTED", "payment", payRef.id, "Method=$method, Amount=Rs.${amountPkr.toInt()} (Pending admin verification)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, payRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun submitComplaint(
        userName: String,
        userRole: String,
        bookingId: String?,
        category: String,
        subject: String,
        description: String
    ): Result<String> {
        val uid = try {
            requireUid()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("complaints").document()
        return try {
            require(subject.trim().length >= 3) { "Subject must be at least 3 characters." }
            require(description.trim().length >= 5) { "Description must be at least 5 characters." }
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "userName" to userName.ifBlank { "Member" }.take(100),
                "userRole" to if (userRole == "collector") "collector" else "customer",
                "category" to category.ifBlank { "General" }.take(60),
                "subject" to subject.trim().take(150),
                "description" to description.trim().take(1500),
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!bookingId.isNullOrBlank()) payload["bookingId"] = bookingId.trim().take(128)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId)
                .update("read", true)
                .await()
        } catch (_: Exception) { }
    }

    suspend fun createNotification(
        recipientUid: String,
        title: String,
        body: String,
        type: String,
        referenceId: String
    ) {
        try {
            db.collection("notifications").document().set(
                mapOf(
                    "recipientUid" to recipientUid,
                    "title" to title.take(120),
                    "body" to body.take(400),
                    "type" to type.take(60),
                    "referenceId" to referenceId.take(128),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) { }
    }

    suspend fun writeAuditLog(
        actorUid: String,
        actorRole: String,
        action: String,
        entity: String,
        entityId: String,
        details: String
    ) {
        try {
            db.collection("audit_logs").document().set(
                mapOf(
                    "actorUid" to actorUid,
                    "actorRole" to actorRole.take(60),
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

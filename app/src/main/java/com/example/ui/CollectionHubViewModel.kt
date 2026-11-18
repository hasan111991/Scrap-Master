package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CollectionHubCatalog
import com.example.data.repository.CollectionHubAdminRepository
import com.example.data.repository.CollectionHubRepository
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CollectionHubAdminState(
    val bookings: List<HubBooking> = emptyList(),
    val poolItems: List<HubPickupPoolItem> = emptyList(),
    val customers: List<HubCustomerProfile> = emptyList(),
    val collectors: List<HubCollectorProfile> = emptyList(),
    val payments: List<HubManualPayment> = emptyList(),
    val commissions: List<HubCommissionRecord> = emptyList(),
    val settlements: List<HubCommissionSettlement> = emptyList(),
    val complaints: List<HubComplaint> = emptyList(),
    val auditLogs: List<HubAuditLog> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionHubViewModel(
    private val repo: CollectionHubRepository,
    private val adminRepo: CollectionHubAdminRepository,
    val currentUserId: String,
    val displayName: String,
    val email: String?,
    val emailVerified: Boolean
) : ViewModel() {

    private val _isVerifiedAdmin = MutableStateFlow(false)
    val isVerifiedAdmin: StateFlow<Boolean> = _isVerifiedAdmin.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private fun <T> Flow<T>.guardFirestore(label: String): Flow<T> = catch { e ->
        if (e is CancellationException && e.cause !is FirebaseFirestoreException) throw e
        Log.e("CollectionHubVM", "Firestore stream [$label] error: ${e.message}", e)
    }

    init {
        viewModelScope.launch {
            repo.ensureCustomerSession(
                displayName = displayName,
                email = email
            )
            val verifiedAdmin = repo.checkIsVerifiedAdmin(
                uid = currentUserId,
                email = email,
                emailVerified = emailVerified
            )
            _isVerifiedAdmin.value = verifiedAdmin
            if (verifiedAdmin) {
                adminRepo.bootstrapInitialPlatformDataIfEmpty()
            }
        }
    }

    val userAccount: StateFlow<HubUserAccount?> =
        repo.observeUserAccount(currentUserId)
            .guardFirestore("userAccount")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val customerProfile: StateFlow<HubCustomerProfile?> =
        repo.observeCustomerProfile(currentUserId)
            .guardFirestore("customerProfile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val collectorProfile: StateFlow<HubCollectorProfile?> =
        repo.observeCollectorProfile(currentUserId)
            .guardFirestore("collectorProfile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val scrapCategories: StateFlow<List<HubScrapCategory>> =
        repo.observeScrapCategories()
            .guardFirestore("scrapCategories")
            .map { list -> list.ifEmpty { CollectionHubCatalog.initialCategories } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionHubCatalog.initialCategories)

    val scrapPrices: StateFlow<List<HubScrapPrice>> =
        repo.observeScrapPrices()
            .guardFirestore("scrapPrices")
            .map { list -> list.ifEmpty { CollectionHubCatalog.seedIndicativePriceSeeds } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionHubCatalog.seedIndicativePriceSeeds)

    val paymentMethods: StateFlow<List<HubPaymentMethodConfig>> =
        repo.observePaymentMethods()
            .guardFirestore("paymentMethods")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val serviceAreas: StateFlow<List<HubServiceArea>> =
        repo.observeServiceAreas()
            .guardFirestore("serviceAreas")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val globalSettings: StateFlow<HubAppSettings> =
        repo.observeGlobalSettings()
            .guardFirestore("globalSettings")
            .map { it ?: CollectionHubCatalog.initialDefaultSettings }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionHubCatalog.initialDefaultSettings)

    val notifications: StateFlow<List<HubNotificationItem>> =
        repo.observeNotifications(currentUserId)
            .guardFirestore("notifications")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customerBookings: StateFlow<List<HubBooking>> =
        repo.observeCustomerBookings(currentUserId)
            .guardFirestore("customerBookings")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customerPayments: StateFlow<List<HubManualPayment>> =
        repo.observeCustomerPayments(currentUserId)
            .guardFirestore("customerPayments")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customerComplaints: StateFlow<List<HubComplaint>> =
        repo.observeCustomerComplaints(currentUserId)
            .guardFirestore("customerComplaints")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val rawPickupPool: Flow<List<HubPickupPoolItem>> =
        collectorProfile.flatMapLatest { col ->
            if (col != null && col.approvalStatus == "approved") {
                repo.observeOpenPickupPool().guardFirestore("openPickupPool")
            } else {
                flowOf(emptyList())
            }
        }

    val openPoolForCollector: StateFlow<List<HubPickupPoolItem>> =
        combine(rawPickupPool, collectorProfile, globalSettings) { pool, col, settings ->
            if (col == null || col.approvalStatus != "approved" || !col.availabilityOnline) {
                emptyList()
            } else {
                pool.filter { item ->
                    item.customerId != currentUserId &&
                        item.status == "searching" &&
                        item.city.equals(col.city, ignoreCase = true) &&
                        (col.serviceAreas.isEmpty() || col.serviceAreas.any { it.equals(item.area, ignoreCase = true) }) &&
                        (col.acceptedCategories.isEmpty() || item.categories.any { cat ->
                            col.acceptedCategories.any { it.equals(cat, ignoreCase = true) }
                        }) &&
                        item.radiusKm <= settings.defaultMatchingRadiusKm
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collectorAssignedBookings: StateFlow<List<HubBooking>> =
        collectorProfile.flatMapLatest { col ->
            if (col != null && col.approvalStatus == "approved") {
                repo.observeCollectorAssignedBookings(currentUserId).guardFirestore("collectorAssignedBookings")
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collectorCommissions: StateFlow<List<HubCommissionRecord>> =
        collectorProfile.flatMapLatest { col ->
            if (col != null) {
                repo.observeCollectorCommissions(currentUserId).guardFirestore("collectorCommissions")
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collectorSettlements: StateFlow<List<HubCommissionSettlement>> =
        collectorProfile.flatMapLatest { col ->
            if (col != null) {
                repo.observeCollectorSettlements(currentUserId).guardFirestore("collectorSettlements")
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun <T> adminStream(block: () -> Flow<List<T>>, label: String): Flow<List<T>> =
        _isVerifiedAdmin.flatMapLatest { isAdmin ->
            if (!isAdmin) flowOf(emptyList()) else block().guardFirestore(label)
        }

    private val adminBookingsFlow = adminStream({ adminRepo.observeAllBookings() }, "adminBookings")
    private val adminPoolFlow = adminStream({ adminRepo.observeAllPoolItems() }, "adminPool")
    private val adminCustomersFlow = adminStream({ adminRepo.observeAllCustomers() }, "adminCustomers")
    private val adminCollectorsFlow = adminStream({ adminRepo.observeAllCollectors() }, "adminCollectors")
    private val adminPaymentsFlow = adminStream({ adminRepo.observeAllPayments() }, "adminPayments")
    private val adminCommissionsFlow = adminStream({ adminRepo.observeAllCommissions() }, "adminCommissions")
    private val adminSettlementsFlow = adminStream({ adminRepo.observeAllSettlements() }, "adminSettlements")
    private val adminComplaintsFlow = adminStream({ adminRepo.observeAllComplaints() }, "adminComplaints")
    private val adminAuditLogsFlow = adminStream({ adminRepo.observeAllAuditLogs() }, "adminAuditLogs")

    val adminState: StateFlow<CollectionHubAdminState> = combine(
        combine(adminBookingsFlow, adminPoolFlow, adminCustomersFlow, adminCollectorsFlow, adminPaymentsFlow) { b, p, cu, co, pay ->
            CollectionHubAdminState(
                bookings = b,
                poolItems = p,
                customers = cu,
                collectors = co,
                payments = pay
            )
        },
        combine(adminCommissionsFlow, adminSettlementsFlow, adminComplaintsFlow, adminAuditLogsFlow) { com, set, cmp, aud ->
            CollectionHubAdminState(
                commissions = com,
                settlements = set,
                complaints = cmp,
                auditLogs = aud
            )
        }
    ) { part1, part2 ->
        part1.copy(
            commissions = part2.commissions,
            settlements = part2.settlements,
            complaints = part2.complaints,
            auditLogs = part2.auditLogs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionHubAdminState())

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    private fun runAction(successMessage: String? = null, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = block()
                res.onSuccess {
                    if (!successMessage.isNullOrBlank()) _toastMessage.value = successMessage
                }.onFailure { err ->
                    _toastMessage.value = err.localizedMessage ?: "Action failed. Please retry."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    // --- CUSTOMER ACTIONS ---

    fun updateCustomerProfile(name: String, phone: String, city: String, area: String, defaultAddress: String) {
        runAction("Profile updated.") {
            repo.updateCustomerProfile(name, phone, city, area, defaultAddress)
        }
    }

    fun createBooking(
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
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val radius = globalSettings.value.defaultMatchingRadiusKm
                val res = repo.createBookingAtomic(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    city = city,
                    area = area,
                    fullAddress = fullAddress,
                    categories = categories,
                    itemsSummary = itemsSummary,
                    unitType = unitType,
                    estimatedWeightKg = estimatedWeightKg,
                    estimatedMinPkr = estimatedMinPkr,
                    estimatedMaxPkr = estimatedMaxPkr,
                    preferredDate = preferredDate,
                    preferredTimeSlot = preferredTimeSlot,
                    paymentMethod = paymentMethod,
                    photoUri = photoUri,
                    notes = notes,
                    matchingRadiusKm = radius
                )
                res.onSuccess { bookingId ->
                    _toastMessage.value = "Pickup request submitted! Searching nearby collectors."
                    onCreated(bookingId)
                }.onFailure { err ->
                    _toastMessage.value = err.localizedMessage ?: "Could not submit booking."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun cancelBooking(bookingId: String, reason: String) {
        runAction("Booking cancelled.") {
            repo.cancelBookingAtomic(bookingId, reason)
        }
    }

    fun confirmBookingCompletion(booking: HubBooking, rating: Int, review: String) {
        runAction("Pickup & payment confirmed! Thank you.") {
            repo.customerConfirmCompletionAndPayment(booking, rating, review, globalSettings.value)
        }
    }

    fun submitComplaint(userRole: String, bookingId: String?, category: String, subject: String, description: String) {
        runAction("Complaint submitted to Collection Hub support.") {
            repo.submitComplaint(
                userName = userAccount.value?.displayName ?: displayName,
                userRole = userRole,
                bookingId = bookingId,
                category = category,
                subject = subject,
                description = description
            )
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repo.markNotificationRead(id) }
    }

    // --- COLLECTOR ACTIONS ---

    fun registerCollector(
        fullName: String,
        shopOrBusinessName: String,
        phone: String,
        city: String,
        serviceAreas: List<String>,
        acceptedCategories: List<String>,
        vehicleType: String,
        address: String,
        cnicLast4: String
    ) {
        runAction("Collector application submitted for admin approval.") {
            repo.registerCollector(
                fullName = fullName,
                shopOrBusinessName = shopOrBusinessName,
                phone = phone,
                city = city,
                serviceAreas = serviceAreas,
                acceptedCategories = acceptedCategories,
                vehicleType = vehicleType,
                address = address,
                cnicLast4 = cnicLast4
            )
        }
    }

    fun toggleCollectorOnline(online: Boolean) {
        runAction(if (online) "You are now Online for pickups." else "You are now Offline.") {
            repo.toggleCollectorOnline(online)
        }
    }

    fun acceptPoolBooking(poolItem: HubPickupPoolItem) {
        val collector = collectorProfile.value
        if (collector == null) {
            _toastMessage.value = "Register as a collector first."
            return
        }
        runAction("Pickup accepted! Full address unlocked in Active Pickups.") {
            repo.acceptPoolBookingAtomic(poolItem, collector)
        }
    }

    fun advanceCollectorWorkflow(
        booking: HubBooking,
        nextStatus: String,
        otpVerified: Boolean = booking.otpVerified,
        verifiedWeightKg: Double = booking.verifiedWeightKg,
        finalAmountPkr: Double = booking.finalAmountPkr,
        collectorNotes: String = booking.collectorNotes
    ) {
        runAction("Pickup updated to ${BookingStatus.fromCode(nextStatus).label}.") {
            repo.advanceCollectorWorkflow(
                booking = booking,
                nextStatus = nextStatus,
                otpVerified = otpVerified,
                verifiedWeightKg = verifiedWeightKg,
                finalAmountPkr = finalAmountPkr,
                collectorNotes = collectorNotes
            )
        }
    }

    fun submitManualPaymentProof(
        booking: HubBooking,
        method: String,
        amountPkr: Double,
        referenceNumber: String,
        proofNote: String
    ) {
        runAction("Manual payment proof recorded (Pending Admin Verification).") {
            repo.submitManualPaymentProof(booking, method, amountPkr, referenceNumber, proofNote)
        }
    }

    // --- ADMIN PORTAL ACTIONS (/hub-admin only) ---

    fun adminBootstrapData() {
        if (!_isVerifiedAdmin.value) return
        runAction("Rawalpindi & Islamabad catalog & settings verified.") {
            adminRepo.bootstrapInitialPlatformDataIfEmpty()
        }
    }

    fun adminUpdateCollectorApproval(collectorId: String, newStatus: String, reason: String = "") {
        if (!_isVerifiedAdmin.value) return
        runAction("Collector status updated to $newStatus.") {
            adminRepo.updateCollectorApproval(collectorId, newStatus, reason)
        }
    }

    fun adminAssignCollector(bookingId: String, collector: HubCollectorProfile) {
        if (!_isVerifiedAdmin.value) return
        runAction("Assigned booking to ${collector.fullName}.") {
            adminRepo.adminAssignCollector(bookingId, collector)
        }
    }

    fun adminCancelBooking(bookingId: String, reason: String) {
        if (!_isVerifiedAdmin.value) return
        runAction("Booking cancelled by admin.") {
            repo.cancelBookingAtomic(bookingId, reason)
        }
    }

    fun adminSaveCategory(category: HubScrapCategory) {
        if (!_isVerifiedAdmin.value) return
        runAction("Scrap category saved.") {
            adminRepo.saveScrapCategory(category)
        }
    }

    fun adminSavePrice(price: HubScrapPrice) {
        if (!_isVerifiedAdmin.value) return
        runAction("Indicative rate updated.") {
            adminRepo.saveScrapPrice(price)
        }
    }

    fun adminVerifyPayment(paymentId: String, status: String) {
        if (!_isVerifiedAdmin.value) return
        runAction("Payment marked $status.") {
            adminRepo.verifyManualPayment(paymentId, status)
        }
    }

    fun adminSettleCollectorCommissions(
        collectorId: String,
        collectorName: String,
        periodLabel: String,
        unpaidCommissions: List<HubCommissionRecord>,
        paymentMethod: String,
        referenceNote: String
    ) {
        if (!_isVerifiedAdmin.value) return
        runAction("Commission settlement recorded.") {
            adminRepo.recordCommissionSettlement(
                collectorId = collectorId,
                collectorName = collectorName,
                periodLabel = periodLabel,
                unpaidCommissions = unpaidCommissions,
                paymentMethod = paymentMethod,
                referenceNote = referenceNote
            )
        }
    }

    fun adminSaveServiceArea(city: String, areaName: String, radiusKm: Double) {
        if (!_isVerifiedAdmin.value) return
        runAction("Service area added.") {
            adminRepo.saveServiceArea(city, areaName, radiusKm)
        }
    }

    fun adminSaveGlobalSettings(settings: HubAppSettings) {
        if (!_isVerifiedAdmin.value) return
        runAction("Platform settings saved.") {
            adminRepo.saveGlobalSettings(settings)
        }
    }

    fun adminUpdateComplaint(complaintId: String, status: String, adminNotes: String) {
        if (!_isVerifiedAdmin.value) return
        runAction("Complaint status updated.") {
            adminRepo.updateComplaintStatus(complaintId, status, adminNotes)
        }
    }
}

package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PortalRoute {
    PUBLIC_HOME,
    CUSTOMER_PORTAL,
    COLLECTOR_PORTAL,
    ADMIN_PORTAL
}

enum class BookingStatus(val code: String, val label: String, val urduLabel: String) {
    SEARCHING("searching", "Searching Collector", "کلیکٹر تلاش جاری"),
    ASSIGNED("assigned", "Collector Assigned", "کلیکٹر نامزد"),
    ON_THE_WAY("on_the_way", "On The Way", "راستے میں"),
    ARRIVED("arrived", "Arrived", "پہنچ گیا"),
    WEIGHT_PRICE_CONFIRMATION("weight_price_confirmation", "Confirm Weight & Price", "وزن اور قیمت کی تصدیق"),
    COMPLETED("completed", "Completed", "مکمل"),
    CANCELLED("cancelled", "Cancelled", "منسوخ");

    companion object {
        fun fromCode(raw: String?): BookingStatus =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: SEARCHING
    }
}

enum class CollectorApprovalStatus(val code: String, val label: String) {
    PENDING_APPROVAL("pending_approval", "Pending Approval"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected"),
    SUSPENDED("suspended", "Suspended");

    companion object {
        fun fromCode(raw: String?): CollectorApprovalStatus =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: PENDING_APPROVAL
    }
}

enum class HubPaymentMethod(val label: String, val urduLabel: String) {
    CASH("Cash", "نقد کیش"),
    EASYPAISA("Easypaisa", "ایزی پیسہ"),
    BANK_TRANSFER("Bank Transfer", "بینک ٹرانسفر");

    companion object {
        fun fromLabel(raw: String?): HubPaymentMethod =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: CASH
    }
}

@Keep
@IgnoreExtraProperties
data class HubUserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "Rawalpindi",
    val area: String = "Chaklala Scheme III",
    val defaultAddress: String = "",
    val role: String = "customer",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubCustomerProfile(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "Rawalpindi",
    val area: String = "Chaklala Scheme III",
    val defaultAddress: String = "",
    val status: String = "active",
    val totalBookings: Long = 0L,
    val totalCompletedKg: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubCollectorProfile(
    @DocumentId val id: String = "",
    val collectorId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val shopOrBusinessName: String = "",
    val phone: String = "",
    val city: String = "Rawalpindi",
    val serviceAreas: List<String> = emptyList(),
    val acceptedCategories: List<String> = emptyList(),
    val vehicleType: String = "",
    val address: String = "",
    val cnicLast4: String = "",
    val availabilityOnline: Boolean = false,
    val approvalStatus: String = "pending_approval",
    val rejectionReason: String = "",
    val radiusKm: Double = 5.0,
    val completedPickupsCount: Long = 0L,
    val totalEarningsPkr: Double = 0.0,
    val commissionDuePkr: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubScrapCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val urduName: String = "",
    val description: String = "",
    val unitOptions: List<String> = listOf("KG"),
    val active: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubScrapPrice(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val unit: String = "KG",
    val minEstimatedPkr: Double = 0.0,
    val maxEstimatedPkr: Double = 0.0,
    val indicative: Boolean = true,
    val city: String = "Rawalpindi",
    val active: Boolean = true,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubBooking(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val city: String = "Rawalpindi",
    val area: String = "",
    val fullAddress: String = "",
    val categories: List<String> = emptyList(),
    val itemsSummary: String = "",
    val unitType: String = "KG",
    val estimatedWeightKg: Double = 0.0,
    val estimatedMinPkr: Double = 0.0,
    val estimatedMaxPkr: Double = 0.0,
    val preferredDate: String = "",
    val preferredTimeSlot: String = "",
    val paymentMethod: String = "Cash",
    val photoUri: String = "",
    val notes: String = "",
    val status: String = "searching",
    val otpCode: String = "",
    val otpVerified: Boolean = false,
    val verifiedWeightKg: Double = 0.0,
    val finalAmountPkr: Double = 0.0,
    val customerConfirmed: Boolean = false,
    val paymentConfirmedByCustomer: Boolean = false,
    val assignedCollectorId: String = "",
    val assignedCollectorName: String = "",
    val assignedCollectorPhone: String = "",
    val collectorNotes: String = "",
    val rating: Long = 0L,
    val review: String = "",
    val cancellationReason: String = "",
    val matchingRadiusKm: Double = 5.0,
    @ServerTimestamp val assignedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubPickupPoolItem(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val city: String = "Rawalpindi",
    val area: String = "",
    val categories: List<String> = emptyList(),
    val itemsSummary: String = "",
    val unitType: String = "KG",
    val estimatedQuantityLabel: String = "",
    val preferredDate: String = "",
    val preferredTimeSlot: String = "",
    val status: String = "searching",
    val assignedCollectorId: String = "",
    val radiusKm: Double = 5.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubPaymentMethodConfig(
    @DocumentId val id: String = "",
    val name: String = "Cash",
    val accountTitle: String = "",
    val accountNumberOrNote: String = "",
    val instructions: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubManualPayment(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val collectorId: String = "",
    val method: String = "Cash",
    val amountPkr: Double = 0.0,
    val submittedByUid: String = "",
    val referenceNumber: String = "",
    val proofNote: String = "",
    val customerConfirmed: Boolean = false,
    val verificationStatus: String = "pending_verification",
    val verifiedByAdminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubCommissionRecord(
    @DocumentId val id: String = "",
    val bookingId: String = "",
    val collectorId: String = "",
    val collectorName: String = "",
    val ruleType: String = "percentage",
    val rateApplied: Double = 10.0,
    val bookingAmountPkr: Double = 0.0,
    val commissionAmountPkr: Double = 0.0,
    val status: String = "unpaid",
    val settlementId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubCommissionSettlement(
    @DocumentId val id: String = "",
    val collectorId: String = "",
    val collectorName: String = "",
    val periodLabel: String = "",
    val totalAmountPkr: Double = 0.0,
    val commissionIds: List<String> = emptyList(),
    val paymentMethod: String = "Cash",
    val referenceNote: String = "",
    val recordedByAdminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubNotificationItem(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val referenceId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubComplaint(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "customer",
    val bookingId: String = "",
    val category: String = "General",
    val subject: String = "",
    val description: String = "",
    val status: String = "open",
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubServiceArea(
    @DocumentId val id: String = "",
    val city: String = "Rawalpindi",
    val areaName: String = "",
    val matchingRadiusKm: Double = 5.0,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubAppSettings(
    @DocumentId val id: String = "",
    val defaultMatchingRadiusKm: Double = 5.0,
    val commissionRuleType: String = "percentage",
    val defaultCommissionPercent: Double = 10.0,
    val defaultFixedCommissionPkr: Double = 150.0,
    val supportPhone: String = "+92 300 0000000",
    val supportEmail: String = "support@collectionhub.pk",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HubAuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorRole: String = "",
    val action: String = "",
    val entity: String = "",
    val entityId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

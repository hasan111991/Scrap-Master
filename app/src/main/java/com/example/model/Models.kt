package com.example.model

enum class UserRole(val label: String, val urduLabel: String) {
    CUSTOMER("Customer", "گاہک / صارف"),
    COLLECTOR("Collector (Kabariya)", "کباڑیا / کلیکٹر"),
    HUB_OPERATOR("Collection Hub", "کلیکشن ہب"),
    RECYCLER("Recycler / B2B Buyer", "ری سائیکلر / خریدار"),
    CORPORATE_CLIENT("Corporate Client", "کارپوریٹ ادارہ"),
    ADMIN("Operations Admin", "انتظامیہ"),
    SUPER_ADMIN("Super Admin", "سپروائزر / چیف ایڈمن")
}

enum class ScrapCategory(val displayName: String, val urduName: String, val icon: String) {
    PAPER("Paper & Books", "کاغذ اور کتابیں", "📄"),
    CARDBOARD("Cardboard / Gatta", "گتہ / کارٹن", "📦"),
    IRON_STEEL("Iron & Steel (Loha)", "لوہا اور اسٹیل", "🔩"),
    ALUMINIUM("Aluminium", "سلور / ایلومینیم", "🥫"),
    COPPER_BRASS("Copper & Brass", "تانبا اور پیتل", "🔌"),
    PLASTIC("Plastics (PET/HDPE)", "پلاسٹک بوتلیں", "🧴"),
    GLASS("Glass Bottles", "شیشہ اور بوتلیں", "🍾"),
    ELECTRONICS("Electronics & E-Waste", "ای ویسٹ / الیکٹرانکس", "💻"),
    BATTERIES("Batteries & UPS", "یو پی ایس و کار بیٹریاں", "🔋"),
    APPLIANCES("Appliances (AC/Fridge)", "گھریلو سامان", "❄️"),
    CONSTRUCTION("Construction Scrap", "تعمیراتی اسکریپ", "🏗️"),
    EID_QURBANI("Eid Qurbani Waste", "عید قربانی آلائشیں", "🐑")
}

data class ScrapMaterial(
    val id: String,
    val name: String,
    val urduName: String,
    val category: ScrapCategory,
    val baseRatePerKg: Double,
    val unit: String = "kg",
    val description: String,
    val grade: String = "Standard A",
    val isEidSpecial: Boolean = false
)

data class PricingRule(
    val id: String,
    val materialId: String,
    val materialName: String,
    val category: ScrapCategory,
    val grade: String,
    val city: String,
    val zone: String,
    val marketRate: Double,
    val buyingMargin: Double, // margin % or PKR
    val sellingMargin: Double,
    val collectionCost: Double,
    val sortingCost: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val active: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Verified server-side formula:
    // Estimated Customer Rate = Market Rate - Collection Cost - Operational Margin + Quality Adjustment
    fun calculateCustomerRate(qualityScore: Double = 1.0): Double {
        val base = (marketRate - collectionCost - (marketRate * (buyingMargin / 100.0))) * qualityScore
        return base.coerceIn(minPrice, maxPrice)
    }
}

enum class PickupStatus(val title: String, val urduTitle: String) {
    REQUESTED("Pickup Requested", "درخواست موصول"),
    ASSIGNED("Collector Assigned", "کباڑیا نامزد"),
    EN_ROUTE("Collector On The Way", "راستے میں ہے"),
    ARRIVED("Collector Arrived", "پہنچ گیا"),
    WEIGHING("Weighing & Verification", "وزن اور جانچ"),
    CONFIRMED("Customer Confirmed", "صارف سے تصدیق"),
    PAID("Payment Processed", "ادائیگی مکمل"),
    RECEIVED_AT_HUB("Received at Hub", "ہب پر موصول"),
    SORTED("Sorted & Batched", "چھانٹی مکمل"),
    COMPLETED("Completed", "مکمل شدہ"),
    CANCELLED("Cancelled", "منسوخ")
}

enum class PaymentMethod(val label: String, val urduLabel: String) {
    CASH("Cash on Pickup", "نقد کیش"),
    EASYPAISA("Easypaisa Wallet", "ایزی پیسہ"),
    JAZZCASH("JazzCash Mobile", "جاز کیش"),
    RAAST("Raast Instant P2P", "راست فوری ٹرانسفر"),
    WALLET_BALANCE("Digital Kabariya Wallet", "ڈیجیٹل والٹ"),
    CORPORATE_INVOICE("Corporate Net-30 Invoice", "کارپوریٹ انوائس")
}

data class PickupItem(
    val materialId: String,
    val materialName: String,
    val category: ScrapCategory,
    val estimatedWeightKg: Double,
    val estimatedRatePerKg: Double,
    val verifiedWeightKg: Double = 0.0,
    val verifiedRatePerKg: Double = 0.0,
    val grade: String = "A",
    val photoUri: String? = null
) {
    val estimatedSubtotal: Double
        get() = estimatedWeightKg * estimatedRatePerKg

    val verifiedSubtotal: Double
        get() = verifiedWeightKg * verifiedRatePerKg
}

data class PickupRequest(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val city: String,
    val zone: String,
    val address: String,
    val latitude: Double = 24.8607, // Karachi default
    val longitude: Double = 67.0011,
    val items: List<PickupItem>,
    val status: PickupStatus,
    val scheduledDate: String,
    val timeSlot: String,
    val otpCode: String = "4892",
    val assignedCollectorId: String? = null,
    val assignedCollectorName: String? = null,
    val assignedCollectorPhone: String? = null,
    val collectorLatitude: Double = 24.8650,
    val collectorLongitude: Double = 67.0060,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentPaid: Boolean = false,
    val digitalReceiptNumber: String? = null,
    val customerNotes: String = "",
    val aiConfidenceNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val estimatedTotal: Double
        get() = items.sumOf { it.estimatedSubtotal }

    val verifiedTotal: Double
        get() = items.sumOf { it.verifiedSubtotal }

    val totalWeightKg: Double
        get() = if (status >= PickupStatus.CONFIRMED && items.any { it.verifiedWeightKg > 0 }) {
            items.sumOf { it.verifiedWeightKg }
        } else {
            items.sumOf { it.estimatedWeightKg }
        }
}

data class CollectorProfile(
    val id: String,
    val name: String,
    val phone: String,
    val cnic: String,
    val vehicleNumber: String,
    val vehicleType: String, // Qingqi Loader / Suzuki Ravi / Motorcycle Rickshaw
    val city: String,
    val zone: String,
    val rating: Double = 4.9,
    val completedPickupsCount: Int = 142,
    val totalWeightKg: Double = 8450.0,
    val todayEarnings: Double = 3650.0,
    val todayJobsCompleted: Int = 5,
    val dailyTargetJobs: Int = 8,
    val walletBalance: Double = 4200.0,
    val isOnline: Boolean = true
)

data class HubBatch(
    val batchId: String,
    val sourcePickupIds: List<String>,
    val warehouseName: String,
    val warehouseCity: String,
    val materialName: String,
    val category: ScrapCategory,
    val grade: String,
    val grossWeightKg: Double,
    val netWeightKg: Double,
    val varianceKg: Double, // Collector claimed vs Hub actual
    val purchaseCost: Double,
    val sortingCost: Double,
    val status: String, // INBOUND_PENDING, SORTED, READY_FOR_MARKETPLACE, DISPATCHED
    val createdAt: Long = System.currentTimeMillis()
)

data class MarketplaceLot(
    val lotId: String,
    val title: String,
    val materialName: String,
    val category: ScrapCategory,
    val grade: String,
    val weightKg: Double,
    val pricePerKg: Double,
    val minimumBidPerKg: Double,
    val location: String,
    val warehouseName: String,
    val status: String = "AVAILABLE", // AVAILABLE, UNDER_BID, SOLD, IN_TRANSIT
    val highestBidPerKg: Double = 0.0,
    val bidsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class CorporateBranch(
    val branchId: String,
    val branchName: String,
    val city: String,
    val address: String,
    val monthlyKg: Double,
    val lastPickupDate: String
)

data class CorporateClient(
    val id: String,
    val companyName: String,
    val ntn: String,
    val contactPerson: String,
    val contactEmail: String,
    val branches: List<CorporateBranch>,
    val totalWasteDivertedKg: Double,
    val co2SavedKg: Double,
    val treesSavedEquivalent: Int,
    val activeContractsCount: Int,
    val creditLimitPkr: Double
)

enum class LedgerTxType {
    CREDIT, DEBIT
}

data class WalletLedgerEntry(
    val id: String,
    val userId: String,
    val role: UserRole,
    val amount: Double,
    val type: LedgerTxType,
    val category: String, // PICKUP_PAYOUT, SCRAP_SALE, COMMISSION, WITHDRAWAL, REWARD
    val referenceId: String,
    val idempotencyKey: String,
    val balanceAfter: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String
)

data class SupportTicket(
    val id: String,
    val userId: String,
    val userName: String,
    val userRole: UserRole,
    val category: String,
    val subject: String,
    val message: String,
    val status: String = "OPEN", // OPEN, IN_PROGRESS, RESOLVED
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val resolutionNotes: String? = null
)

data class AuditLog(
    val id: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

data class FraudFlag(
    val id: String,
    val pickupId: String,
    val type: String,
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val description: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING_REVIEW"
)

data class EidCampaignConfig(
    val isCampaignActive: Boolean = true,
    val campaignName: String = "Eid-ul-Adha Clean Pakistan Mission 2026",
    val startDate: String = "2026-06-15",
    val endDate: String = "2026-06-25",
    val specialZones: List<String> = listOf("Karachi North", "Karachi Central", "Lahore Gulberg", "Rawalpindi Cantt"),
    val partnerNGOs: List<String> = listOf("Saylani Welfare", "Edhi Foundation", "Al-Khidmat"),
    val skinsPricePkr: Double = 650.0,
    val freeSanitationCollection: Boolean = true
)

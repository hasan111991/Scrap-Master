package com.example.service

import com.example.data.SeedData
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object PlatformRepository {

    // Current Active Role
    private val _currentRole = MutableStateFlow(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    // Language Toggle (English <-> Urdu)
    private val _isUrdu = MutableStateFlow(false)
    val isUrdu: StateFlow<Boolean> = _isUrdu.asStateFlow()

    // Materials Catalog
    private val _materials = MutableStateFlow(SeedData.materials)
    val materials: StateFlow<List<ScrapMaterial>> = _materials.asStateFlow()

    // Dynamic Pricing Rules
    private val _pricingRules = MutableStateFlow(SeedData.pricingRules)
    val pricingRules: StateFlow<List<PricingRule>> = _pricingRules.asStateFlow()

    // Pickup Requests
    private val _pickups = MutableStateFlow(SeedData.samplePickups)
    val pickups: StateFlow<List<PickupRequest>> = _pickups.asStateFlow()

    // Collector Profile
    private val _collectorProfile = MutableStateFlow(SeedData.sampleCollector)
    val collectorProfile: StateFlow<CollectorProfile> = _collectorProfile.asStateFlow()

    // Hub Batches & Inventory
    private val _hubBatches = MutableStateFlow(SeedData.sampleBatches)
    val hubBatches: StateFlow<List<HubBatch>> = _hubBatches.asStateFlow()

    // Recycler B2B Marketplace Lots
    private val _marketplaceLots = MutableStateFlow(SeedData.sampleMarketplaceLots)
    val marketplaceLots: StateFlow<List<MarketplaceLot>> = _marketplaceLots.asStateFlow()

    // Corporate Client
    private val _corporateClient = MutableStateFlow(SeedData.sampleCorporate)
    val corporateClient: StateFlow<CorporateClient> = _corporateClient.asStateFlow()

    // Customer Wallet Balance
    private val _customerWalletBalance = MutableStateFlow(3450.0)
    val customerWalletBalance: StateFlow<Double> = _customerWalletBalance.asStateFlow()

    // Immutable Financial Ledger
    private val _ledgerEntries = MutableStateFlow(
        listOf(
            WalletLedgerEntry(
                id = "TX-LEDGER-001",
                userId = "cust_001",
                role = UserRole.CUSTOMER,
                amount = 2850.0,
                type = LedgerTxType.CREDIT,
                category = "PICKUP_PAYOUT",
                referenceId = "PKP-7819",
                idempotencyKey = "IDEMP-99218274",
                balanceAfter = 3450.0,
                description = "Scrap payout for pickup #PKP-7819 (315kg Cardboard + 144kg PET)"
            ),
            WalletLedgerEntry(
                id = "TX-LEDGER-002",
                userId = "col_001",
                role = UserRole.COLLECTOR,
                amount = 1250.0,
                type = LedgerTxType.CREDIT,
                category = "COMMISSION",
                referenceId = "PKP-7819",
                idempotencyKey = "IDEMP-8837192",
                balanceAfter = 6200.0,
                description = "Trip and weight commission for pickup #PKP-7819"
            )
        )
    )
    val ledgerEntries: StateFlow<List<WalletLedgerEntry>> = _ledgerEntries.asStateFlow()

    // Audit Logs
    private val _auditLogs = MutableStateFlow(SeedData.sampleAuditLogs)
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    // Fraud Alerts
    private val _fraudAlerts = MutableStateFlow(SeedData.sampleFraudAlerts)
    val fraudAlerts: StateFlow<List<FraudFlag>> = _fraudAlerts.asStateFlow()

    // Customer Support Tickets
    private val _supportTickets = MutableStateFlow(
        listOf(
            SupportTicket(
                id = "TKT-301",
                userId = "cust_001",
                userName = "Muhammad Tariq",
                userRole = UserRole.CUSTOMER,
                category = "WEIGHT",
                subject = "Weight clarification for copper wiring",
                message = "Collector calibrated scale on uneven pavement, verified weight was 8.2kg vs my domestic digital scale 8.5kg.",
                status = "RESOLVED",
                resolutionNotes = "Operator checked scale photo. Difference of 300g within normal tolerance. Customer credited Rs. 200 goodwill bonus."
            )
        )
    )
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    // Eid-ul-Adha Waste Campaign
    private val _eidConfig = MutableStateFlow(EidCampaignConfig())
    val eidConfig: StateFlow<EidCampaignConfig> = _eidConfig.asStateFlow()

    // System Notification Toasts
    private val _systemToast = MutableStateFlow<String?>(null)
    val systemToast: StateFlow<String?> = _systemToast.asStateFlow()

    fun switchRole(newRole: UserRole) {
        _currentRole.value = newRole
        showToast("Switched to ${newRole.label} portal")
    }

    fun toggleLanguage() {
        _isUrdu.value = !_isUrdu.value
    }

    fun showToast(message: String) {
        _systemToast.value = message
    }

    fun clearToast() {
        _systemToast.value = null
    }

    // Customer Actions
    fun createPickupRequest(
        items: List<PickupItem>,
        city: String,
        zone: String,
        address: String,
        scheduledDate: String,
        timeSlot: String,
        paymentMethod: PaymentMethod,
        notes: String
    ): PickupRequest {
        val newId = "PKP-${(1000..9999).random()}"
        val otp = (1000..9999).random().toString()
        val pickup = PickupRequest(
            id = newId,
            customerId = "cust_current",
            customerName = "Ali Raza",
            customerPhone = "+92 300 4567890",
            city = city,
            zone = zone,
            address = address,
            items = items,
            status = PickupStatus.ASSIGNED,
            scheduledDate = scheduledDate,
            timeSlot = timeSlot,
            otpCode = otp,
            assignedCollectorId = "col_001",
            assignedCollectorName = "Rashid Khan (Qingqi Loader 412)",
            assignedCollectorPhone = "+92 333 9876543",
            paymentMethod = paymentMethod,
            customerNotes = notes
        )
        _pickups.value = listOf(pickup) + _pickups.value
        logAudit(
            action = "PICKUP_CREATED",
            entity = "PickupRequest",
            entityId = newId,
            details = "Created pickup in $city with ${items.size} scrap categories"
        )
        showToast("Pickup #$newId successfully requested! Collector assigned.")
        return pickup
    }

    // Collector Lifecycle Actions
    fun collectorAcceptJob(pickupId: String) {
        _pickups.value = _pickups.value.map {
            if (it.id == pickupId) it.copy(status = PickupStatus.EN_ROUTE, updatedAt = System.currentTimeMillis())
            else it
        }
        showToast("Job #$pickupId accepted! Navigating to customer location.")
    }

    fun collectorMarkArrived(pickupId: String) {
        _pickups.value = _pickups.value.map {
            if (it.id == pickupId) it.copy(status = PickupStatus.ARRIVED, updatedAt = System.currentTimeMillis())
            else it
        }
        showToast("Collector arrived at pickup location. Request customer OTP.")
    }

    fun collectorVerifyOtpAndWeigh(
        pickupId: String,
        otpEntered: String,
        verifiedWeights: Map<String, Double>
    ): Boolean {
        val currentPickup = _pickups.value.find { it.id == pickupId } ?: return false
        if (currentPickup.otpCode != otpEntered) {
            showToast("Invalid OTP! Please enter correct 4-digit code from customer.")
            return false
        }

        val updatedItems = currentPickup.items.map { item ->
            val weight = verifiedWeights[item.materialId] ?: item.estimatedWeightKg
            item.copy(
                verifiedWeightKg = weight,
                verifiedRatePerKg = item.estimatedRatePerKg
            )
        }

        val updatedPickup = currentPickup.copy(
            items = updatedItems,
            status = PickupStatus.CONFIRMED,
            updatedAt = System.currentTimeMillis()
        )

        // Evaluate AI Fraud Risk
        val fraudFlag = AiService.evaluatePickupFraudRisk(updatedPickup)
        if (fraudFlag != null) {
            _fraudAlerts.value = listOf(fraudFlag) + _fraudAlerts.value
            logAudit(
                action = "AI_FRAUD_ALERT",
                entity = "PickupRequest",
                entityId = pickupId,
                details = fraudFlag.description
            )
        }

        _pickups.value = _pickups.value.map { if (it.id == pickupId) updatedPickup else it }
        showToast("Physical weight recorded and verified! Customer confirmed.")
        return true
    }

    fun customerConfirmAndPay(pickupId: String) {
        val pickup = _pickups.value.find { it.id == pickupId } ?: return
        val totalPkr = pickup.verifiedTotal.takeIf { it > 0 } ?: pickup.estimatedTotal
        val totalKg = pickup.totalWeightKg

        val receiptNo = "RCPT-KHI-${(10000..99999).random()}"
        val updatedPickup = pickup.copy(
            status = PickupStatus.COMPLETED,
            paymentPaid = true,
            digitalReceiptNumber = receiptNo,
            updatedAt = System.currentTimeMillis()
        )

        _pickups.value = _pickups.value.map { if (it.id == pickupId) updatedPickup else it }

        // Financial Ledger entry for customer payout
        val idempotency = "IDEMP-${UUID.randomUUID().toString().take(8)}"
        val newCustomerBalance = _customerWalletBalance.value + totalPkr
        _customerWalletBalance.value = newCustomerBalance

        val customerEntry = WalletLedgerEntry(
            id = "TX-${UUID.randomUUID().toString().take(6)}",
            userId = pickup.customerId,
            role = UserRole.CUSTOMER,
            amount = totalPkr,
            type = LedgerTxType.CREDIT,
            category = "PICKUP_PAYOUT",
            referenceId = pickupId,
            idempotencyKey = idempotency,
            balanceAfter = newCustomerBalance,
            description = "Payout for Scrap Pickup #$pickupId via ${pickup.paymentMethod.label}"
        )

        // Calculate collector commission
        val commission = CommissionEngine.calculateCollectorEarnings(
            totalWeightKg = totalKg,
            hasHighValueMetals = pickup.items.any { it.category == ScrapCategory.COPPER_BRASS }
        )

        val newCollectorBalance = _collectorProfile.value.walletBalance + commission.totalCommission
        val collectorEntry = WalletLedgerEntry(
            id = "TX-${UUID.randomUUID().toString().take(6)}",
            userId = _collectorProfile.value.id,
            role = UserRole.COLLECTOR,
            amount = commission.totalCommission,
            type = LedgerTxType.CREDIT,
            category = "COMMISSION",
            referenceId = pickupId,
            idempotencyKey = "IDEMP-COL-${UUID.randomUUID().toString().take(8)}",
            balanceAfter = newCollectorBalance,
            description = "Commission for Pickup #$pickupId (Base: Rs.${commission.fixedTripCommission.toInt()} + Wt: Rs.${commission.weightCommission.toInt()})"
        )

        _ledgerEntries.value = listOf(customerEntry, collectorEntry) + _ledgerEntries.value

        _collectorProfile.value = _collectorProfile.value.copy(
            walletBalance = newCollectorBalance,
            todayEarnings = _collectorProfile.value.todayEarnings + commission.totalCommission,
            todayJobsCompleted = _collectorProfile.value.todayJobsCompleted + 1,
            completedPickupsCount = _collectorProfile.value.completedPickupsCount + 1,
            totalWeightKg = _collectorProfile.value.totalWeightKg + totalKg
        )

        logAudit(
            action = "PAYMENT_SETTLED",
            entity = "PickupRequest",
            entityId = pickupId,
            details = "Settled PKR $totalPkr to customer and PKR ${commission.totalCommission} commission to collector"
        )
        showToast("Payment settled! Digital Receipt #$receiptNo generated.")
    }

    // Collection Hub Actions
    fun hubCreateBatch(
        warehouseName: String,
        warehouseCity: String,
        materialName: String,
        category: ScrapCategory,
        grade: String,
        grossWeight: Double,
        netWeight: Double,
        purchaseCost: Double,
        sortingCost: Double,
        sourceIds: List<String>
    ) {
        val batchId = "BATCH-HUB-${(100..999).random()}"
        val variance = netWeight - grossWeight
        val batch = HubBatch(
            batchId = batchId,
            sourcePickupIds = sourceIds,
            warehouseName = warehouseName,
            warehouseCity = warehouseCity,
            materialName = materialName,
            category = category,
            grade = grade,
            grossWeightKg = grossWeight,
            netWeightKg = netWeight,
            varianceKg = variance,
            purchaseCost = purchaseCost,
            sortingCost = sortingCost,
            status = "READY_FOR_MARKETPLACE"
        )
        _hubBatches.value = listOf(batch) + _hubBatches.value

        // Also push lot into Recycler Marketplace
        val lot = MarketplaceLot(
            lotId = "LOT-${(100..999).random()}",
            title = "$grade $materialName",
            materialName = materialName,
            category = category,
            grade = grade,
            weightKg = netWeight,
            pricePerKg = (purchaseCost + sortingCost) / netWeight * 1.15, // 15% wholesale margin
            minimumBidPerKg = (purchaseCost + sortingCost) / netWeight * 1.08,
            location = "$warehouseName, $warehouseCity",
            warehouseName = warehouseName,
            status = "AVAILABLE"
        )
        _marketplaceLots.value = listOf(lot) + _marketplaceLots.value

        logAudit(
            action = "HUB_BATCH_CREATED",
            entity = "HubBatch",
            entityId = batchId,
            details = "Received and sorted $netWeight kg of $materialName in $warehouseCity"
        )
        showToast("Hub Batch #$batchId sorted and listed on B2B Marketplace!")
    }

    // Recycler B2B Actions
    fun placeMarketplaceBid(lotId: String, bidPerKg: Double) {
        _marketplaceLots.value = _marketplaceLots.value.map {
            if (it.lotId == lotId) {
                it.copy(
                    highestBidPerKg = maxOf(it.highestBidPerKg, bidPerKg),
                    bidsCount = it.bidsCount + 1,
                    status = "UNDER_BID"
                )
            } else it
        }
        logAudit(
            action = "MARKETPLACE_BID",
            entity = "MarketplaceLot",
            entityId = lotId,
            details = "Bid placed at PKR $bidPerKg / kg"
        )
        showToast("Bid of PKR $bidPerKg/kg placed on Lot #$lotId!")
    }

    fun buyMarketplaceLot(lotId: String) {
        _marketplaceLots.value = _marketplaceLots.value.map {
            if (it.lotId == lotId) it.copy(status = "SOLD") else it
        }
        logAudit(
            action = "MARKETPLACE_PURCHASE",
            entity = "MarketplaceLot",
            entityId = lotId,
            details = "Lot purchased at fixed price by Recycler Buyer"
        )
        showToast("Lot #$lotId successfully ordered! B2B Invoice generated.")
    }

    // Admin & Pricing Engine Actions
    fun updatePricingRule(
        ruleId: String,
        newMarketRate: Double,
        newBuyingMargin: Double,
        reason: String
    ) {
        val oldRule = _pricingRules.value.find { it.id == ruleId }
        _pricingRules.value = _pricingRules.value.map {
            if (it.id == ruleId) {
                it.copy(
                    marketRate = newMarketRate,
                    buyingMargin = newBuyingMargin,
                    updatedAt = System.currentTimeMillis()
                )
            } else it
        }

        logAudit(
            action = "PRICE_RULE_UPDATE",
            entity = "PricingRule",
            entityId = ruleId,
            oldValue = "Rate: PKR ${oldRule?.marketRate}, Margin: ${oldRule?.buyingMargin}%",
            newValue = "Rate: PKR $newMarketRate, Margin: $newBuyingMargin%",
            details = reason
        )
        showToast("Pricing rule updated and audited successfully.")
    }

    fun resolveFraudAlert(flagId: String, approved: Boolean) {
        _fraudAlerts.value = _fraudAlerts.value.map {
            if (it.id == flagId) {
                it.copy(status = if (approved) "APPROVED_EXCEPTION" else "CONFIRMED_FRAUD")
            } else it
        }
        logAudit(
            action = "FRAUD_ALERT_RESOLVED",
            entity = "FraudFlag",
            entityId = flagId,
            details = if (approved) "Cleared after manual scale review" else "Flag confirmed - collector warning issued"
        )
        showToast("Fraud flag updated: ${if (approved) "Cleared" else "Flagged for investigation"}")
    }

    fun createSupportTicket(category: String, subject: String, message: String) {
        val ticketId = "TKT-${(100..999).random()}"
        val ticket = SupportTicket(
            id = ticketId,
            userId = "cust_001",
            userName = "Customer User",
            userRole = _currentRole.value,
            category = category,
            subject = subject,
            message = message,
            status = "OPEN"
        )
        _supportTickets.value = listOf(ticket) + _supportTickets.value
        showToast("Support ticket #$ticketId opened. Our team will respond shortly.")
    }

    fun withdrawWalletFunds(amount: Double, accountDetails: String, method: PaymentMethod) {
        if (amount <= 0.0) {
            showToast("Enter a valid PKR amount greater than 0.")
            return
        }
        if (amount > _customerWalletBalance.value) {
            showToast("Insufficient wallet balance for withdrawal!")
            return
        }
        _customerWalletBalance.value -= amount
        val entry = WalletLedgerEntry(
            id = "TX-WD-${UUID.randomUUID().toString().take(6)}",
            userId = "cust_001",
            role = UserRole.CUSTOMER,
            amount = amount,
            type = LedgerTxType.DEBIT,
            category = "WITHDRAWAL",
            referenceId = accountDetails,
            idempotencyKey = "IDEMP-WD-${UUID.randomUUID().toString().take(8)}",
            balanceAfter = _customerWalletBalance.value,
            description = "Withdrawal of PKR $amount to $accountDetails via ${method.label}"
        )
        _ledgerEntries.value = listOf(entry) + _ledgerEntries.value
        logAudit(
            action = "WALLET_WITHDRAWAL",
            entity = "Wallet",
            entityId = "cust_001",
            details = "Withdrew PKR $amount to $accountDetails"
        )
        showToast("Withdrawal of PKR $amount initiated via ${method.label}!")
    }

    fun toggleEidCampaign(active: Boolean) {
        _eidConfig.value = _eidConfig.value.copy(isCampaignActive = active)
        logAudit(
            action = "EID_CAMPAIGN_TOGGLE",
            entity = "EidCampaign",
            entityId = "campaign_eid_2026",
            details = "Eid campaign state set to $active"
        )
        showToast("Eid-ul-Adha Waste Campaign status updated.")
    }

    private fun logAudit(
        action: String,
        entity: String,
        entityId: String,
        oldValue: String? = null,
        newValue: String? = null,
        details: String = ""
    ) {
        val audit = AuditLog(
            id = "AUD-${System.currentTimeMillis()}",
            actorId = "user_${_currentRole.value.name.lowercase()}",
            actorRole = _currentRole.value.name,
            action = action,
            entity = entity,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue,
            details = details
        )
        _auditLogs.value = listOf(audit) + _auditLogs.value
    }
}

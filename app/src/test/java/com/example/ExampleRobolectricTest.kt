package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.service.AiService
import com.example.service.CommissionEngine
import com.example.service.PlatformRepository
import com.example.service.PricingEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Digital Kabariya", appName)
    }

    @Test
    fun `test pricing engine formula adheres to boundaries`() {
        val rule = PricingRule(
            id = "test_rule_01",
            materialId = "mat_iron",
            materialName = "Iron",
            category = ScrapCategory.IRON_STEEL,
            grade = "Standard",
            city = "Karachi",
            zone = "Korangi",
            marketRate = 160.0,
            buyingMargin = 10.0,
            sellingMargin = 12.0,
            collectionCost = 5.0,
            sortingCost = 2.0,
            minPrice = 120.0,
            maxPrice = 170.0
        )

        val rate = PricingEngine.calculatePrice(rule)
        // Rate = 160 - (5+2) - (160 * 0.10) = 160 - 7 - 16 = 137.0
        assertEquals(137.0, rate, 0.01)
        assertTrue(rate >= rule.minPrice && rate <= rule.maxPrice)
    }

    @Test
    fun `test collector commission engine breakdown`() {
        val breakdown = CommissionEngine.calculateCollectorEarnings(
            totalWeightKg = 150.0,
            hasHighValueMetals = true,
            collectorRating = 4.9,
            isTargetBonusEligible = true
        )

        // Base: 150 + Weight (150 * 3.0 = 450) + HighValue (250) + Perf (300) = 1150.0
        assertEquals(150.0, breakdown.fixedTripCommission, 0.01)
        assertEquals(450.0, breakdown.weightCommission, 0.01)
        assertEquals(250.0, breakdown.highValueMaterialBonus, 0.01)
        assertEquals(300.0, breakdown.performanceBonus, 0.01)
        assertEquals(1150.0, breakdown.totalCommission, 0.01)
    }

    @Test
    fun `test ai scrap assistant vision estimate and grade`() {
        val result = AiService.analyzeScrapItem(ScrapCategory.COPPER_BRASS, 10.0)
        assertEquals(ScrapCategory.COPPER_BRASS, result.detectedCategory)
        assertTrue(result.confidenceScore >= 0.85)
        assertEquals(21500.0, result.estimatedTotalPkr, 0.01)
        assertTrue(result.advisoryNote.contains("AI estimate only"))
    }

    @Test
    fun `test fraud detection on abnormal weight variance`() {
        val abnormalPickup = PickupRequest(
            id = "PKP-TEST-FRAUD",
            customerId = "c1",
            customerName = "Test",
            customerPhone = "03001234567",
            city = "Karachi",
            zone = "DHA",
            address = "Test Street",
            items = listOf(
                PickupItem(
                    materialId = "m1",
                    materialName = "Iron",
                    category = ScrapCategory.IRON_STEEL,
                    estimatedWeightKg = 20.0,
                    estimatedRatePerKg = 145.0,
                    verifiedWeightKg = 90.0, // huge variance > 300%
                    verifiedRatePerKg = 145.0
                )
            ),
            status = PickupStatus.CONFIRMED,
            scheduledDate = "Today",
            timeSlot = "Slot"
        )

        val fraud = AiService.evaluatePickupFraudRisk(abnormalPickup)
        assertNotNull(fraud)
        assertEquals("ANOMALOUS_WEIGHT_VARIANCE", fraud?.type)
        assertEquals("HIGH", fraud?.severity)
    }

    @Test
    fun `test complete customer pickup and payment settlement lifecycle`() {
        val item = PickupItem(
            materialId = "mat_cardboard_01",
            materialName = "Cardboard",
            category = ScrapCategory.CARDBOARD,
            estimatedWeightKg = 50.0,
            estimatedRatePerKg = 38.0
        )

        val pickup = PlatformRepository.createPickupRequest(
            items = listOf(item),
            city = "Karachi",
            zone = "Gulshan",
            address = "Block 13-D Gulshan-e-Iqbal",
            scheduledDate = "Today",
            timeSlot = "02:00 PM - 04:00 PM",
            paymentMethod = PaymentMethod.EASYPAISA,
            notes = "Call before arriving"
        )

        assertEquals(PickupStatus.ASSIGNED, pickup.status)
        assertEquals(1900.0, pickup.estimatedTotal, 0.01)

        // Collector accepts
        PlatformRepository.collectorAcceptJob(pickup.id)
        val inRoute = PlatformRepository.pickups.value.find { it.id == pickup.id }
        assertEquals(PickupStatus.EN_ROUTE, inRoute?.status)

        // Collector marks arrived
        PlatformRepository.collectorMarkArrived(pickup.id)
        val arrived = PlatformRepository.pickups.value.find { it.id == pickup.id }
        assertEquals(PickupStatus.ARRIVED, arrived?.status)

        // Verify OTP & Weigh
        val verified = PlatformRepository.collectorVerifyOtpAndWeigh(
            pickup.id,
            pickup.otpCode,
            mapOf("mat_cardboard_01" to 52.0)
        )
        assertTrue(verified)

        // Customer confirms & pays
        val initialBalance = PlatformRepository.customerWalletBalance.value
        PlatformRepository.customerConfirmAndPay(pickup.id)

        val completed = PlatformRepository.pickups.value.find { it.id == pickup.id }
        assertEquals(PickupStatus.COMPLETED, completed?.status)
        assertTrue(completed?.paymentPaid == true)
        assertNotNull(completed?.digitalReceiptNumber)

        // Wallet balance increased by 52 * 38 = 1976.0
        assertEquals(initialBalance + 1976.0, PlatformRepository.customerWalletBalance.value, 0.01)
    }

    @Test
    fun `test scrap inventory item threshold and value calculation`() {
        val item = com.example.data.model.ScrapInventoryItem(
            id = "inv_copper_01",
            materialId = "mat_copper",
            name = "Pure Millberry Copper",
            category = "COPPER_BRASS",
            currentStockKg = 45.0,
            minThresholdKg = 50.0,
            unitPricePkr = 2250.0
        )

        assertTrue(item.isLowStock)
        assertEquals(101250.0, item.estimatedTotalValuePkr, 0.01)

        val replenished = item.copy(currentStockKg = 120.0)
        assertFalse(replenished.isLowStock)
        assertEquals(270000.0, replenished.estimatedTotalValuePkr, 0.01)
    }
}

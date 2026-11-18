package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CollectionHubCatalog
import com.example.model.BookingStatus
import com.example.model.CollectorApprovalStatus
import com.example.model.HubAppSettings
import com.example.model.HubPaymentMethod
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read Collection Hub app_name and tagline from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Collection Hub", context.getString(R.string.app_name))
        assertEquals(
            "Your Scrap. Your Collector. Your Collection Hub.",
            context.getString(R.string.tagline)
        )
    }

    @Test
    fun `catalog provides Rawalpindi Islamabad areas and 12 indicative categories`() {
        assertEquals(listOf("Rawalpindi", "Islamabad"), CollectionHubCatalog.supportedCities)
        assertEquals(12, CollectionHubCatalog.initialCategories.size)
        assertEquals(12, CollectionHubCatalog.seedIndicativePriceSeeds.size)
        assertTrue(CollectionHubCatalog.seedIndicativePriceSeeds.all { it.indicative })
        assertTrue(CollectionHubCatalog.areasByCity["Rawalpindi"]?.isNotEmpty() == true)
        assertTrue(CollectionHubCatalog.areasByCity["Islamabad"]?.isNotEmpty() == true)
        assertEquals(5.0, CollectionHubCatalog.initialDefaultSettings.defaultMatchingRadiusKm, 0.01)
    }

    @Test
    fun `commission engine supports percentage fixed and category rules`() {
        val pctSettings = HubAppSettings(
            commissionRuleType = "percentage",
            defaultCommissionPercent = 10.0,
            defaultFixedCommissionPkr = 150.0
        )
        assertEquals(
            500.0,
            CollectionHubCatalog.computeCommissionAmount(5000.0, listOf("Iron"), pctSettings),
            0.01
        )

        val fixedSettings = pctSettings.copy(commissionRuleType = "fixed")
        assertEquals(
            150.0,
            CollectionHubCatalog.computeCommissionAmount(5000.0, listOf("Iron"), fixedSettings),
            0.01
        )

        val categorySettings = pctSettings.copy(commissionRuleType = "category")
        // Copper gets +2% => 12% of 5000 = 600
        assertEquals(
            600.0,
            CollectionHubCatalog.computeCommissionAmount(5000.0, listOf("Copper"), categorySettings),
            0.01
        )
    }

    @Test
    fun `booking and collector enums parse codes safely`() {
        assertEquals(BookingStatus.SEARCHING, BookingStatus.fromCode("searching"))
        assertEquals(BookingStatus.WEIGHT_PRICE_CONFIRMATION, BookingStatus.fromCode("weight_price_confirmation"))
        assertEquals(BookingStatus.COMPLETED, BookingStatus.fromCode("completed"))
        assertEquals(CollectorApprovalStatus.PENDING_APPROVAL, CollectorApprovalStatus.fromCode("pending_approval"))
        assertEquals(CollectorApprovalStatus.APPROVED, CollectorApprovalStatus.fromCode("approved"))
        assertEquals(HubPaymentMethod.EASYPAISA, HubPaymentMethod.fromLabel("Easypaisa"))
    }
}

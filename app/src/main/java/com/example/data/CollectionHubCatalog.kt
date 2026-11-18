package com.example.data

import com.example.model.HubAppSettings
import com.example.model.HubScrapCategory
import com.example.model.HubScrapPrice

/**
 * Initial launch reference metadata for Rawalpindi & Islamabad, Pakistan.
 * Live scrap prices are fetched dynamically from Firestore `scrap_prices` and
 * always labeled as indicative/estimated pending physical weighing.
 */
object CollectionHubCatalog {

    val supportedCities = listOf("Rawalpindi", "Islamabad")

    val areasByCity: Map<String, List<String>> = mapOf(
        "Rawalpindi" to listOf(
            "Chaklala Scheme III",
            "Saddar",
            "Bahria Town Phase 1-8",
            "Satellite Town",
            "Gulraiz / Chaklala",
            "Adiala Road",
            "Peshawar Road"
        ),
        "Islamabad" to listOf(
            "F-6 / F-7 / F-8",
            "F-10 / F-11",
            "G-9 / G-10 / G-11",
            "G-13 / G-14",
            "I-8 / I-9 / I-10",
            "E-11",
            "DHA Islamabad"
        )
    )

    val initialCategories = listOf(
        HubScrapCategory(id = "cat_newspaper", name = "Newspaper", urduName = "اخبار", description = "Clean daily newspapers & magazines", unitOptions = listOf("KG", "Bag"), active = true, sortOrder = 1),
        HubScrapCategory(id = "cat_books_paper", name = "Books & Paper", urduName = "کتابیں اور کاغذ", description = "Notebooks, office paper, textbooks", unitOptions = listOf("KG", "Bag"), active = true, sortOrder = 2),
        HubScrapCategory(id = "cat_cardboard", name = "Cardboard", urduName = "گتہ / کارٹن", description = "Dry corrugated cartons & packing boxes", unitOptions = listOf("KG", "Bulk"), active = true, sortOrder = 3),
        HubScrapCategory(id = "cat_plastic", name = "Plastic", urduName = "پلاسٹک", description = "PET bottles, HDPE containers, household plastic", unitOptions = listOf("KG", "Bag"), active = true, sortOrder = 4),
        HubScrapCategory(id = "cat_iron", name = "Iron", urduName = "لوہا", description = "Heavy iron scrap, frames, rebar, sheets", unitOptions = listOf("KG", "Bulk"), active = true, sortOrder = 5),
        HubScrapCategory(id = "cat_steel", name = "Steel", urduName = "اسٹیل", description = "Stainless steel utensils, fixtures, trimmings", unitOptions = listOf("KG"), active = true, sortOrder = 6),
        HubScrapCategory(id = "cat_aluminium", name = "Aluminium", urduName = "ایلومینیم", description = "Window frames, cans, utensils, clean aluminium", unitOptions = listOf("KG"), active = true, sortOrder = 7),
        HubScrapCategory(id = "cat_copper", name = "Copper", urduName = "تانبا", description = "Stripped wire, motor windings, copper pipes", unitOptions = listOf("KG"), active = true, sortOrder = 8),
        HubScrapCategory(id = "cat_brass", name = "Brass", urduName = "پیتل", description = "Valves, taps, fittings, brass hardware", unitOptions = listOf("KG"), active = true, sortOrder = 9),
        HubScrapCategory(id = "cat_old_electronics", name = "Old Electronics", urduName = "پرانے الیکٹرانکس", description = "Old PCs, printers, routers, circuit boards", unitOptions = listOf("Piece", "KG"), active = true, sortOrder = 10),
        HubScrapCategory(id = "cat_home_appliances", name = "Home Appliances", urduName = "گھریلو آلات", description = "Broken ACs, washing machines, refrigerators", unitOptions = listOf("Piece", "Bulk"), active = true, sortOrder = 11),
        HubScrapCategory(id = "cat_other_scrap", name = "Other Scrap", urduName = "دیگر اسکریپ", description = "Mixed household recyclables pending inspection", unitOptions = listOf("KG", "Bag", "Bulk"), active = true, sortOrder = 12)
    )

    val defaultTimeSlots = listOf(
        "09:00 AM - 12:00 PM",
        "12:00 PM - 03:00 PM",
        "03:00 PM - 06:00 PM",
        "Flexible (Collector Coordinates)"
    )

    val defaultVehicleTypes = listOf(
        "Motorbike / Rickshaw",
        "Qingqi Loader",
        "Suzuki Pickup",
        "Local Kabari Shop Drop/Van"
    )

    val initialDefaultSettings = HubAppSettings(
        id = "global",
        defaultMatchingRadiusKm = 5.0,
        commissionRuleType = "percentage",
        defaultCommissionPercent = 10.0,
        defaultFixedCommissionPkr = 150.0,
        supportPhone = "+92 51 000 0000",
        supportEmail = "support@collectionhub.pk"
    )

    val seedIndicativePriceSeeds: List<HubScrapPrice> = listOf(
        HubScrapPrice(id = "price_newspaper", categoryId = "cat_newspaper", categoryName = "Newspaper", unit = "KG", minEstimatedPkr = 40.0, maxEstimatedPkr = 55.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_books", categoryId = "cat_books_paper", categoryName = "Books & Paper", unit = "KG", minEstimatedPkr = 45.0, maxEstimatedPkr = 65.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_cardboard", categoryId = "cat_cardboard", categoryName = "Cardboard", unit = "KG", minEstimatedPkr = 32.0, maxEstimatedPkr = 45.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_plastic", categoryId = "cat_plastic", categoryName = "Plastic", unit = "KG", minEstimatedPkr = 65.0, maxEstimatedPkr = 95.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_iron", categoryId = "cat_iron", categoryName = "Iron", unit = "KG", minEstimatedPkr = 130.0, maxEstimatedPkr = 155.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_steel", categoryId = "cat_steel", categoryName = "Steel", unit = "KG", minEstimatedPkr = 220.0, maxEstimatedPkr = 290.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_aluminium", categoryId = "cat_aluminium", categoryName = "Aluminium", unit = "KG", minEstimatedPkr = 390.0, maxEstimatedPkr = 470.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_copper", categoryId = "cat_copper", categoryName = "Copper", unit = "KG", minEstimatedPkr = 1950.0, maxEstimatedPkr = 2300.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_brass", categoryId = "cat_brass", categoryName = "Brass", unit = "KG", minEstimatedPkr = 1350.0, maxEstimatedPkr = 1600.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_electronics", categoryId = "cat_old_electronics", categoryName = "Old Electronics", unit = "Piece", minEstimatedPkr = 250.0, maxEstimatedPkr = 1800.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_appliances", categoryId = "cat_home_appliances", categoryName = "Home Appliances", unit = "Piece", minEstimatedPkr = 1500.0, maxEstimatedPkr = 6500.0, indicative = true, city = "Rawalpindi", active = true),
        HubScrapPrice(id = "price_other", categoryId = "cat_other_scrap", categoryName = "Other Scrap", unit = "KG", minEstimatedPkr = 20.0, maxEstimatedPkr = 80.0, indicative = true, city = "Rawalpindi", active = true)
    )

    val publicFaqs: List<Pair<String, String>> = listOf(
        "Which cities does Collection Hub serve?" to
            "Collection Hub is launching across Rawalpindi and Islamabad, connecting households and shops with nearby verified local kabari shops and independent collectors.",
        "Are prices shown in the app final?" to
            "No. All rates shown before pickup are indicative/estimated ranges configured dynamically by admins. Final payment depends on physical inspection and verified weight at your doorstep.",
        "How does collector matching and address privacy work?" to
            "Nearby approved collectors only see your approximate sector/area and scrap categories while your request is open. Your exact street address and phone number are shared only with the collector who accepts the booking.",
        "How do payments work?" to
            "Payments are settled manually at pickup or via agreed Cash, Easypaisa, or Bank Transfer, confirmed by both parties inside the app. Collection Hub does not process automated card gateways at this stage."
    )

    fun computeCommissionAmount(
        bookingAmountPkr: Double,
        categories: List<String>,
        settings: HubAppSettings
    ): Double {
        if (bookingAmountPkr <= 0.0) return 0.0
        return when (settings.commissionRuleType.lowercase()) {
            "fixed" -> settings.defaultFixedCommissionPkr.coerceAtLeast(0.0)
            "category" -> {
                val hasHighValueMetal = categories.any {
                    it.equals("Copper", ignoreCase = true) ||
                        it.equals("Brass", ignoreCase = true) ||
                        it.equals("Aluminium", ignoreCase = true)
                }
                val pct = if (hasHighValueMetal) {
                    (settings.defaultCommissionPercent + 2.0).coerceAtMost(25.0)
                } else {
                    settings.defaultCommissionPercent
                }
                kotlin.math.round(bookingAmountPkr * (pct / 100.0))
            }
            else -> kotlin.math.round(bookingAmountPkr * (settings.defaultCommissionPercent / 100.0))
        }
    }
}

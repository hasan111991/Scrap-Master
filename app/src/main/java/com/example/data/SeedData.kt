package com.example.data

import com.example.model.*

object SeedData {

    val materials = listOf(
        ScrapMaterial(
            id = "mat_iron_01",
            name = "Iron / Steel (Loha)",
            urduName = "لوہا اور اسٹیل",
            category = ScrapCategory.IRON_STEEL,
            baseRatePerKg = 145.0,
            description = "Structural iron, rebar cutouts, machinery frames, heavy scrap."
        ),
        ScrapMaterial(
            id = "mat_copper_01",
            name = "Pure Copper Wire (Taamba)",
            urduName = "تانبا (خالص وائر)",
            category = ScrapCategory.COPPER_BRASS,
            baseRatePerKg = 2150.0,
            description = "Stripped motor copper, bright electric wire, plumbing pipes."
        ),
        ScrapMaterial(
            id = "mat_brass_01",
            name = "Brass Scrap (Peetal)",
            urduName = "پیتل اسکریپ",
            category = ScrapCategory.COPPER_BRASS,
            baseRatePerKg = 1480.0,
            description = "Fittings, valves, plumbing fixtures, utensils."
        ),
        ScrapMaterial(
            id = "mat_cardboard_01",
            name = "Cardboard Boxes (Gatta)",
            urduName = "گتہ / کارٹن",
            category = ScrapCategory.CARDBOARD,
            baseRatePerKg = 38.0,
            description = "Dry packaging boxes, corrugated cartons, packing sheets."
        ),
        ScrapMaterial(
            id = "mat_paper_01",
            name = "Newspapers (Akhbarat)",
            urduName = "اخبارات / ردی",
            category = ScrapCategory.PAPER,
            baseRatePerKg = 48.0,
            description = "Clean bundled daily newspapers, magazines, office shredding."
        ),
        ScrapMaterial(
            id = "mat_paper_02",
            name = "Office Records & Books",
            urduName = "کتابیں اور کاپیاں",
            category = ScrapCategory.PAPER,
            baseRatePerKg = 55.0,
            description = "School books, notebooks, registers, white printer paper."
        ),
        ScrapMaterial(
            id = "mat_plastic_01",
            name = "PET Plastic Bottles",
            urduName = "پلاسٹک بوتلیں",
            category = ScrapCategory.PLASTIC,
            baseRatePerKg = 85.0,
            description = "Crushed water & beverage bottles, transparent clear PET."
        ),
        ScrapMaterial(
            id = "mat_aluminium_01",
            name = "Aluminium (Silver Utensils)",
            urduName = "سلور و ایلومینیم برتن",
            category = ScrapCategory.ALUMINIUM,
            baseRatePerKg = 440.0,
            description = "Window frames, cooking pots, soda cans, aluminium foil."
        ),
        ScrapMaterial(
            id = "mat_battery_01",
            name = "UPS & Car Lead Acid Battery",
            urduName = "یو پی ایس و گاڑی بیٹری",
            category = ScrapCategory.BATTERIES,
            baseRatePerKg = 380.0,
            description = "Heavy dry/acid batteries (unbroken case), UPS power batteries."
        ),
        ScrapMaterial(
            id = "mat_ewaste_01",
            name = "Motherboards & Computer E-Waste",
            urduName = "کمپیوٹر سرکٹ بورڈز",
            category = ScrapCategory.ELECTRONICS,
            baseRatePerKg = 680.0,
            description = "PC motherboards, telecom cards, laptop boards, server racks."
        ),
        ScrapMaterial(
            id = "mat_appliances_01",
            name = "AC Unit Scrap (Compressor & Coil)",
            urduName = "اے سی اسکریپ و کمپریسر",
            category = ScrapCategory.APPLIANCES,
            baseRatePerKg = 240.0,
            description = "Split AC indoor/outdoor units, fridge condenser motors."
        ),
        ScrapMaterial(
            id = "mat_eid_01",
            name = "Eid Qurbani Animal Hides & Leather",
            urduName = "قربانی کھالیں (بکرا و گائے)",
            category = ScrapCategory.EID_QURBANI,
            baseRatePerKg = 750.0,
            unit = "piece",
            description = "Salted animal hides, processed raw skins for tannery recycling.",
            isEidSpecial = true
        )
    )

    val pricingRules = listOf(
        PricingRule(
            id = "pr_01",
            materialId = "mat_iron_01",
            materialName = "Iron / Steel (Loha)",
            category = ScrapCategory.IRON_STEEL,
            grade = "A-Grade Heavy",
            city = "Karachi",
            zone = "Korangi Industrial",
            marketRate = 155.0,
            buyingMargin = 8.0,
            sellingMargin = 12.0,
            collectionCost = 6.0,
            sortingCost = 2.0,
            minPrice = 130.0,
            maxPrice = 165.0
        ),
        PricingRule(
            id = "pr_02",
            materialId = "mat_copper_01",
            materialName = "Pure Copper Wire (Taamba)",
            category = ScrapCategory.COPPER_BRASS,
            grade = "A-Grade Bright",
            city = "Karachi",
            zone = "SITE Area",
            marketRate = 2280.0,
            buyingMargin = 5.0,
            sellingMargin = 8.0,
            collectionCost = 15.0,
            sortingCost = 5.0,
            minPrice = 2000.0,
            maxPrice = 2400.0
        ),
        PricingRule(
            id = "pr_03",
            materialId = "mat_cardboard_01",
            materialName = "Cardboard Boxes (Gatta)",
            category = ScrapCategory.CARDBOARD,
            grade = "Dry Packaging",
            city = "Lahore",
            zone = "Gulberg",
            marketRate = 42.0,
            buyingMargin = 7.0,
            sellingMargin = 10.0,
            collectionCost = 3.0,
            sortingCost = 1.0,
            minPrice = 30.0,
            maxPrice = 50.0
        )
    )

    val samplePickups = listOf(
        PickupRequest(
            id = "PKP-7821",
            customerId = "cust_001",
            customerName = "Muhammad Tariq",
            customerPhone = "+92 300 1234567",
            city = "Karachi",
            zone = "DHA Phase 6",
            address = "House 42-C, Khayaban-e-Sehar, DHA Phase 6, Karachi",
            items = listOf(
                PickupItem(
                    materialId = "mat_iron_01",
                    materialName = "Iron / Steel (Loha)",
                    category = ScrapCategory.IRON_STEEL,
                    estimatedWeightKg = 45.0,
                    estimatedRatePerKg = 145.0,
                    verifiedWeightKg = 48.5,
                    verifiedRatePerKg = 145.0,
                    grade = "Standard Heavy"
                ),
                PickupItem(
                    materialId = "mat_copper_01",
                    materialName = "Pure Copper Wire (Taamba)",
                    category = ScrapCategory.COPPER_BRASS,
                    estimatedWeightKg = 8.0,
                    estimatedRatePerKg = 2150.0,
                    verifiedWeightKg = 8.2,
                    verifiedRatePerKg = 2150.0,
                    grade = "Stripped A"
                )
            ),
            status = PickupStatus.EN_ROUTE,
            scheduledDate = "Today",
            timeSlot = "02:00 PM - 04:00 PM",
            otpCode = "4892",
            assignedCollectorId = "col_001",
            assignedCollectorName = "Rashid Khan (Qingqi Loader 412)",
            assignedCollectorPhone = "+92 333 9876543",
            paymentMethod = PaymentMethod.RAAST,
            customerNotes = "Gate buzzer is broken, please call on arrival.",
            aiConfidenceNote = "AI Material Scan: 96% Iron & High-Gauge Copper identified."
        ),
        PickupRequest(
            id = "PKP-7819",
            customerId = "cust_002",
            customerName = "Fatima Textile Mills",
            customerPhone = "+92 321 5556677",
            city = "Karachi",
            zone = "Korangi Industrial Area",
            address = "Plot 89, Sector 15, Korangi Industrial, Karachi",
            items = listOf(
                PickupItem(
                    materialId = "mat_cardboard_01",
                    materialName = "Cardboard Boxes (Gatta)",
                    category = ScrapCategory.CARDBOARD,
                    estimatedWeightKg = 320.0,
                    estimatedRatePerKg = 38.0,
                    verifiedWeightKg = 315.0,
                    verifiedRatePerKg = 38.0
                ),
                PickupItem(
                    materialId = "mat_plastic_01",
                    materialName = "PET Plastic Bottles",
                    category = ScrapCategory.PLASTIC,
                    estimatedWeightKg = 140.0,
                    estimatedRatePerKg = 85.0,
                    verifiedWeightKg = 144.0,
                    verifiedRatePerKg = 85.0
                )
            ),
            status = PickupStatus.COMPLETED,
            scheduledDate = "Yesterday",
            timeSlot = "10:00 AM - 12:00 PM",
            otpCode = "8120",
            assignedCollectorId = "col_001",
            assignedCollectorName = "Rashid Khan",
            assignedCollectorPhone = "+92 333 9876543",
            paymentMethod = PaymentMethod.EASYPAISA,
            paymentPaid = true,
            digitalReceiptNumber = "RCPT-KHI-2026-9921",
            customerNotes = "Factory back gate dispatch."
        )
    )

    val sampleCollector = CollectorProfile(
        id = "col_001",
        name = "Rashid Khan",
        phone = "+92 333 9876543",
        cnic = "42101-7890123-1",
        vehicleNumber = "KHI-QNG-774",
        vehicleType = "Motorcycle Qingqi Loader (1000kg)",
        city = "Karachi",
        zone = "DHA & Clifton",
        rating = 4.92,
        completedPickupsCount = 284,
        totalWeightKg = 14820.0,
        todayEarnings = 4850.0,
        todayJobsCompleted = 4,
        dailyTargetJobs = 7,
        walletBalance = 6200.0,
        isOnline = true
    )

    val sampleBatches = listOf(
        HubBatch(
            batchId = "BATCH-HUB-KHI-801",
            sourcePickupIds = listOf("PKP-7819", "PKP-7798", "PKP-7762"),
            warehouseName = "Central Sorting Hub Korangi",
            warehouseCity = "Karachi",
            materialName = "Cardboard / Gatta",
            category = ScrapCategory.CARDBOARD,
            grade = "A-Grade Industrial Baled",
            grossWeightKg = 1420.0,
            netWeightKg = 1385.0,
            varianceKg = -35.0,
            purchaseCost = 52630.0,
            sortingCost = 2800.0,
            status = "READY_FOR_MARKETPLACE"
        ),
        HubBatch(
            batchId = "BATCH-HUB-KHI-802",
            sourcePickupIds = listOf("PKP-7815", "PKP-7801"),
            warehouseName = "SITE Hub West",
            warehouseCity = "Karachi",
            materialName = "Iron / Steel (Loha)",
            category = ScrapCategory.IRON_STEEL,
            grade = "Heavy Melting Scrap (HMS 1)",
            grossWeightKg = 3200.0,
            netWeightKg = 3180.0,
            varianceKg = -20.0,
            purchaseCost = 461100.0,
            sortingCost = 6400.0,
            status = "READY_FOR_MARKETPLACE"
        )
    )

    val sampleMarketplaceLots = listOf(
        MarketplaceLot(
            lotId = "LOT-MET-991",
            title = "Heavy Melting Iron Scrap (HMS 1/2)",
            materialName = "Iron / Steel (Loha)",
            category = ScrapCategory.IRON_STEEL,
            grade = "HMS-1 Sorted Rebar & Girders",
            weightKg = 5000.0,
            pricePerKg = 172.0,
            minimumBidPerKg = 168.0,
            location = "SITE Industrial Area, Karachi",
            warehouseName = "SITE Hub West",
            status = "AVAILABLE",
            highestBidPerKg = 170.0,
            bidsCount = 4
        ),
        MarketplaceLot(
            lotId = "LOT-COP-204",
            title = "Millberry Bright Copper Granules & Wire",
            materialName = "Copper & Brass",
            category = ScrapCategory.COPPER_BRASS,
            grade = "99.9% Pure Millberry",
            weightKg = 1200.0,
            pricePerKg = 2450.0,
            minimumBidPerKg = 2400.0,
            location = "Korangi Industrial Area, Karachi",
            warehouseName = "Central Sorting Hub Korangi",
            status = "AVAILABLE",
            highestBidPerKg = 2420.0,
            bidsCount = 7
        ),
        MarketplaceLot(
            lotId = "LOT-PLAS-112",
            title = "Baled Clear PET Bottle Flakes",
            materialName = "Plastics (PET)",
            category = ScrapCategory.PLASTIC,
            grade = "Washed & Hot-Baled Clear",
            weightKg = 3500.0,
            pricePerKg = 118.0,
            minimumBidPerKg = 110.0,
            location = "Sundar Industrial Estate, Lahore",
            warehouseName = "Sundar Hub Lahore",
            status = "AVAILABLE",
            highestBidPerKg = 114.0,
            bidsCount = 3
        )
    )

    val sampleCorporate = CorporateClient(
        id = "corp_001",
        companyName = "Packages Limited & Allied Offices",
        ntn = "1430982-7",
        contactPerson = "Engr. Salman Raza (CSR & ESG Head)",
        contactEmail = "salman.raza@packages.com.pk",
        branches = listOf(
            CorporateBranch("BR-01", "Head Office Shahrah-e-Roomi", "Lahore", "Ferozepur Road, Lahore", 1850.0, "2026-09-02"),
            CorporateBranch("BR-02", "Distribution Warehouse Korangi", "Karachi", "Plot 12-B Korangi Sector 23", 3400.0, "2026-09-05"),
            CorporateBranch("BR-03", "Islamabad Corporate Liaison Office", "Islamabad", "Blue Area, Jinnah Ave", 650.0, "2026-08-28")
        ),
        totalWasteDivertedKg = 48500.0,
        co2SavedKg = 112400.0,
        treesSavedEquivalent = 840,
        activeContractsCount = 3,
        creditLimitPkr = 2500000.0
    )

    val sampleAuditLogs = listOf(
        AuditLog(
            id = "aud_01",
            actorId = "admin_01",
            actorRole = "PRICING_MANAGER",
            action = "PRICE_UPDATE",
            entity = "PricingRule",
            entityId = "pr_01",
            oldValue = "PKR 140/kg",
            newValue = "PKR 145/kg",
            details = "Global scrap index adjusted based on Karachi Port imports"
        ),
        AuditLog(
            id = "aud_02",
            actorId = "hub_01",
            actorRole = "HUB_OPERATOR",
            action = "VARIANCE_CHECK",
            entity = "HubBatch",
            entityId = "BATCH-HUB-KHI-801",
            oldValue = "1420 kg",
            newValue = "1385 kg (Net after moisture 2.4%)",
            details = "Approved by Hub Scale Station Operator"
        )
    )

    val sampleFraudAlerts = listOf(
        FraudFlag(
            id = "fr_01",
            pickupId = "PKP-7744",
            type = "WEIGHT_SPIKE_VARIANCE",
            severity = "MEDIUM",
            description = "Estimated 15kg entered by customer vs 98kg claimed by collector. Requires photo review."
        ),
        FraudFlag(
            id = "fr_02",
            pickupId = "PKP-7712",
            type = "GPS_PROXIMITY_DEVIATION",
            severity = "LOW",
            description = "Collector marked arrived 450m away from customer pin code."
        )
    )
}

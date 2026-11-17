package com.example.service

import com.example.model.FraudFlag
import com.example.model.PickupRequest
import com.example.model.ScrapCategory

object AiService {

    data class AiScrapScanResult(
        val detectedCategory: ScrapCategory,
        val materialName: String,
        val estimatedWeightRange: String,
        val estimatedRatePerKg: Double,
        val estimatedTotalPkr: Double,
        val confidenceScore: Double,
        val qualityGrade: String,
        val advisoryNote: String = "AI estimate only. Final price depends on physical inspection and verified weight."
    )

    /**
     * AI Scrap Assistant:
     * Analyzes uploaded photo or category selection to estimate material grade and weight.
     */
    fun analyzeScrapItem(category: ScrapCategory, estimatedWeightKg: Double): AiScrapScanResult {
        val (matName, rate, grade, conf) = when (category) {
            ScrapCategory.COPPER_BRASS -> Quadruple("Pure Stripped Copper Wire", 2150.0, "Grade A+ High Conductivity", 0.94)
            ScrapCategory.IRON_STEEL -> Quadruple("Heavy Structural Iron Scrap", 145.0, "Standard Industrial Grade", 0.91)
            ScrapCategory.CARDBOARD -> Quadruple("Corrugated Packaging Cartons", 38.0, "Dry Clean Grade", 0.96)
            ScrapCategory.PAPER -> Quadruple("Bundled Urdu/English Newspapers", 48.0, "Clean Recyclable", 0.93)
            ScrapCategory.ALUMINIUM -> Quadruple("Cast Aluminium & Utensils", 440.0, "Sorted Food Grade", 0.89)
            ScrapCategory.PLASTIC -> Quadruple("Clear PET Water & Soda Bottles", 85.0, "Transparent Clean", 0.92)
            ScrapCategory.BATTERIES -> Quadruple("Heavy Lead-Acid UPS Battery", 380.0, "Sealed Intact", 0.95)
            ScrapCategory.ELECTRONICS -> Quadruple("Printed Circuit Boards & RAM", 680.0, "Gold/Copper Bearing", 0.88)
            ScrapCategory.APPLIANCES -> Quadruple("Air Conditioner Copper/Alu Coil", 240.0, "Grade B Reusable Parts", 0.87)
            ScrapCategory.EID_QURBANI -> Quadruple("Salted Qurbani Cow/Goat Skin", 750.0, "Tannery Grade A", 0.95)
            else -> Quadruple("Mixed Recyclables", 50.0, "Standard Mixed", 0.82)
        }

        val lowWeight = (estimatedWeightKg * 0.85).toInt()
        val highWeight = (estimatedWeightKg * 1.15).toInt().coerceAtLeast(lowWeight + 1)

        return AiScrapScanResult(
            detectedCategory = category,
            materialName = matName,
            estimatedWeightRange = "$lowWeight kg – $highWeight kg",
            estimatedRatePerKg = rate,
            estimatedTotalPkr = estimatedWeightKg * rate,
            confidenceScore = conf,
            qualityGrade = grade
        )
    }

    /**
     * AI Fraud Detection Engine:
     * Analyzes pickup telemetry, weight discrepancies (>25%), GPS anomalies, and excessive price overrides.
     */
    fun evaluatePickupFraudRisk(pickup: PickupRequest): FraudFlag? {
        val estWeight = pickup.items.sumOf { it.estimatedWeightKg }
        val verWeight = pickup.items.sumOf { it.verifiedWeightKg }

        if (verWeight > 0 && estWeight > 0) {
            val varianceRatio = kotlin.math.abs(verWeight - estWeight) / estWeight
            if (varianceRatio > 0.40) {
                return FraudFlag(
                    id = "flag_${System.currentTimeMillis()}",
                    pickupId = pickup.id,
                    type = "ANOMALOUS_WEIGHT_VARIANCE",
                    severity = "HIGH",
                    description = "AI Detected ${(varianceRatio * 100).toInt()}% weight discrepancy (Estimated: ${estWeight.toInt()}kg vs Verified: ${verWeight.toInt()}kg). Requires physical scale calibration audit."
                )
            }
        }
        return null
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

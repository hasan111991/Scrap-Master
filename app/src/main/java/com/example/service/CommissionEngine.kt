package com.example.service

import com.example.model.ScrapCategory

object CommissionEngine {

    data class CommissionBreakdown(
        val fixedTripCommission: Double,
        val weightCommission: Double,
        val highValueMaterialBonus: Double,
        val performanceBonus: Double,
        val totalCommission: Double
    )

    /**
     * Pakistan Collector Commission Engine:
     * Fixed Base (Rs. 150) + Weight Incentive (Rs. 3.0 / kg) + Special category bonus + Rating multiplier
     */
    fun calculateCollectorEarnings(
        totalWeightKg: Double,
        hasHighValueMetals: Boolean,
        collectorRating: Double = 4.9,
        isTargetBonusEligible: Boolean = false
    ): CommissionBreakdown {
        val fixed = 150.0 // Base trip payout
        val weightRate = when {
            totalWeightKg > 500 -> 3.5
            totalWeightKg > 100 -> 3.0
            else -> 2.5
        }
        val weightEarnings = totalWeightKg * weightRate
        val highValueBonus = if (hasHighValueMetals) 250.0 else 0.0
        val perfBonus = if (collectorRating >= 4.8 && isTargetBonusEligible) 300.0 else 0.0

        val total = fixed + weightEarnings + highValueBonus + perfBonus

        return CommissionBreakdown(
            fixedTripCommission = fixed,
            weightCommission = weightEarnings,
            highValueMaterialBonus = highValueBonus,
            performanceBonus = perfBonus,
            totalCommission = total
        )
    }
}

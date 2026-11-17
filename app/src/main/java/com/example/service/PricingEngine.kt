package com.example.service

import com.example.model.PricingRule
import com.example.model.ScrapCategory

object PricingEngine {

    /**
     * Pakistan Dynamic Scrap Pricing Formula:
     * Customer Rate = Base Market Rate - Collection Cost - (Market Rate * Buying Margin %) + Quality Bonus
     * Clamped between minPrice and maxPrice.
     * All price calculations are evaluated server-side to prevent client tampering.
     */
    fun calculatePrice(
        rule: PricingRule,
        qualityMultiplier: Double = 1.0,
        volumeDiscountMultiplier: Double = 1.0
    ): Double {
        val operationalCost = rule.collectionCost + rule.sortingCost
        val baseMargin = rule.marketRate * (rule.buyingMargin / 100.0)
        val netRate = (rule.marketRate - operationalCost - baseMargin) * qualityMultiplier * volumeDiscountMultiplier
        return netRate.coerceIn(rule.minPrice, rule.maxPrice)
    }

    /**
     * Fallback calculation if no city-specific pricing rule is configured.
     */
    fun estimateBaseRate(category: ScrapCategory, weightKg: Double): Double {
        val base = when (category) {
            ScrapCategory.PAPER -> 48.0
            ScrapCategory.CARDBOARD -> 38.0
            ScrapCategory.IRON_STEEL -> 145.0
            ScrapCategory.ALUMINIUM -> 440.0
            ScrapCategory.COPPER_BRASS -> 2150.0
            ScrapCategory.PLASTIC -> 85.0
            ScrapCategory.GLASS -> 18.0
            ScrapCategory.ELECTRONICS -> 680.0
            ScrapCategory.BATTERIES -> 380.0
            ScrapCategory.APPLIANCES -> 240.0
            ScrapCategory.CONSTRUCTION -> 135.0
            ScrapCategory.EID_QURBANI -> 750.0
        }
        // Bulk bonus for > 200kg
        return if (weightKg >= 200.0) base * 1.05 else base
    }
}

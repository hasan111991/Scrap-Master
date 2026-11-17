package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore data model representing a scrap material inventory record.
 * Follows zero-trust schema requirements:
 * - @Keep and @IgnoreExtraProperties for R8/minification resilience
 * - Default values for all properties (ensures no-arg constructor for .toObject())
 * - @DocumentId mapping (never stored inside document body)
 * - @ServerTimestamp for server-generated audit timestamps
 * - @get:Exclude on computed properties so CustomClassMapper never serializes them
 */
@Keep
@IgnoreExtraProperties
data class ScrapInventoryItem(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val materialId: String = "",
    val name: String = "",
    val urduName: String = "",
    val category: String = "",
    val grade: String = "Standard A",
    val currentStockKg: Double = 0.0,
    val minThresholdKg: Double = 50.0,
    val unitPricePkr: Double = 0.0,
    val warehouseLocation: String = "Central Hub Rawalpindi",
    val status: String = "IN_STOCK", // IN_STOCK, LOW_STOCK, DEPLETED, DISPATCHED
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isLowStock: Boolean
        get() = currentStockKg <= minThresholdKg

    @get:Exclude
    val estimatedTotalValuePkr: Double
        get() = currentStockKg * unitPricePkr
}

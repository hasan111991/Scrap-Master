package com.example.service

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ScrapCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiService"

/**
 * Production Gemini AI service powered by Firebase AI Logic (`Firebase.ai`) and App Check.
 * Uses `gemini-3.8-flash` for multimodal scrap vision classification and streaming assistant Q&A.
 */
object GeminiAiService {

    private val scrapClassificationSchema = Schema.obj(
        properties = mapOf(
            "detectedCategory" to Schema.enumeration(
                values = ScrapCategory.entries.map { it.name },
                description = "Closest Digital Kabariya scrap category enum name."
            ),
            "materialName" to Schema.string(description = "Specific scrap material name in Pakistan market terms."),
            "qualityGrade" to Schema.string(description = "Visible quality grade (e.g., Grade A Clean, Mixed Industrial)."),
            "confidenceScore" to Schema.double(description = "Confidence score from 0.0 to 1.0."),
            "suggestedWeightMinKg" to Schema.double(description = "Conservative lower bound weight estimate in kg."),
            "suggestedWeightMaxKg" to Schema.double(description = "Upper bound weight estimate in kg."),
            "inspectionNotes" to Schema.string(description = "Short inspection guidance and contamination checks.")
        )
    )

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scrapClassificationSchema
            }
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    data class GeminiScrapClassification(
        val detectedCategory: ScrapCategory,
        val materialName: String,
        val qualityGrade: String,
        val confidenceScore: Double,
        val estimatedWeightRange: String,
        val inspectionNotes: String,
        val disclaimer: String = "AI classification is an estimate. Final material, grade, and price are confirmed during physical inspection and verified weighing."
    )

    /**
     * Classifies scrap from a photo Bitmap and/or user text description via Firebase AI Logic.
     */
    suspend fun classifyScrapWithGemini(
        bitmap: Bitmap?,
        categoryHint: ScrapCategory,
        estimatedWeightKg: Double,
        notes: String
    ): Result<GeminiScrapClassification> {
        return try {
            val promptText = buildString {
                append("You are an expert Pakistani scrap & recycling grader for Digital Kabariya (Rawalpindi/Islamabad). ")
                append("Analyze the provided scrap item details and photo (if attached). ")
                append("Category selected by user: ${categoryHint.displayName} (${categoryHint.name}). ")
                append("User estimated weight: $estimatedWeightKg kg. ")
                if (notes.isNotBlank()) append("Notes: $notes. ")
                append("Classify the material, visible quality grade, realistic weight range around the user's estimate, and sorting/hazard notes (flag batteries/e-waste if hazardous).")
            }

            val response = withTimeout(60_000L) {
                visionModel.generateContent(
                    content {
                        if (bitmap != null) {
                            image(scaleDownIfNeeded(bitmap))
                        }
                        text(promptText)
                    }
                )
            }

            val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty classification response.")
            val json = JSONObject(jsonText)
            val categoryRaw = json.optString("detectedCategory", categoryHint.name)
            val category = ScrapCategory.entries.firstOrNull { it.name == categoryRaw } ?: categoryHint
            val minW = json.optDouble("suggestedWeightMinKg", estimatedWeightKg * 0.85).coerceAtLeast(0.5)
            val maxW = json.optDouble("suggestedWeightMaxKg", estimatedWeightKg * 1.15).coerceAtLeast(minW + 0.5)

            Result.success(
                GeminiScrapClassification(
                    detectedCategory = category,
                    materialName = json.optString("materialName", category.displayName),
                    qualityGrade = json.optString("qualityGrade", "Standard Grade"),
                    confidenceScore = json.optDouble("confidenceScore", 0.88).coerceIn(0.0, 1.0),
                    estimatedWeightRange = "${minW.toInt()} kg – ${maxW.toInt()} kg",
                    inspectionNotes = json.optString("inspectionNotes", "Verify moisture and sorting cleanliness at doorstep.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini classification timed out", e)
            Result.failure(Exception("AI classification timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini classification failed", e)
            Result.failure(e)
        }
    }

    /**
     * Streams responses from Gemini 3.8 Flash for the Digital Kabariya Bilingual Assistant.
     */
    fun streamAssistantResponse(
        userPrompt: String,
        isUrdu: Boolean,
        roleContext: String
    ): Flow<String> = flow {
        val systemInstruction = buildString {
            append("You are the Digital Kabariya AI Operations & Customer Assistant for Pakistan (launching in Rawalpindi & Islamabad). ")
            append("Active user role: $roleContext. ")
            if (isUrdu) {
                append("Respond concisely in clear Urdu (اردو) with PKR rates context where helpful. ")
            } else {
                append("Respond concisely in clear English suited for Pakistan (PKR currency). ")
            }
            append("Never guarantee AI price estimates—always note that final payment depends on physical inspection and verified weighing.\n\n")
            append("User question: $userPrompt")
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            assistantModel.generateContentStream(systemInstruction).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
    }.catch { e ->
        if (e is CancellationException && e !is TimeoutCancellationException) throw e
        Log.e(TAG, "Assistant stream failed", e)
        throw e
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / w, maxDimension.toFloat() / h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}

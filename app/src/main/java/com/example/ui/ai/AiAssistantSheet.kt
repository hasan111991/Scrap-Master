package com.example.ui.ai

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.model.ScrapCategory
import com.example.model.UserRole
import com.example.service.GeminiAiService
import com.example.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiAssistantViewModel : ViewModel() {
    private val _chatState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val chatState: StateFlow<AiUiState> = _chatState.asStateFlow()

    private val _scanState = MutableStateFlow<Result<GeminiAiService.GeminiScrapClassification>?>(null)
    val scanState: StateFlow<Result<GeminiAiService.GeminiScrapClassification>?> = _scanState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun askAssistant(prompt: String, isUrdu: Boolean, role: UserRole) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _chatState.value = AiUiState.Loading
            var lastText = ""
            try {
                GeminiAiService.streamAssistantResponse(
                    userPrompt = prompt.trim(),
                    isUrdu = isUrdu,
                    roleContext = role.label
                ).collect { accumulated ->
                    lastText = accumulated
                    _chatState.value = AiUiState.Streaming(accumulated)
                }
                _chatState.value = if (lastText.isNotBlank()) {
                    AiUiState.Success(lastText)
                } else {
                    AiUiState.Error("Gemini returned an empty response.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _chatState.value = AiUiState.Error(e.localizedMessage ?: "AI Assistant request failed.")
            }
        }
    }

    fun analyzePhoto(
        bitmap: Bitmap?,
        categoryHint: ScrapCategory,
        estimatedWeightKg: Double,
        notes: String
    ) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanState.value = GeminiAiService.classifyScrapWithGemini(
                bitmap = bitmap,
                categoryHint = categoryHint,
                estimatedWeightKg = estimatedWeightKg,
                notes = notes
            )
            _isScanning.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantModal(
    viewModel: AiAssistantViewModel,
    isUrdu: Boolean,
    currentRole: UserRole,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var queryInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ScrapCategory.COPPER_BRASS) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
            viewModel.analyzePhoto(
                bitmap = bitmap,
                categoryHint = selectedCategory,
                estimatedWeightKg = 15.0,
                notes = "Customer uploaded photo via Android Photo Picker"
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "ڈیجیٹل کباڑیا اے آئی اسسٹنٹ" else "Digital Kabariya AI Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close AI Assistant")
                }
            }

            // Photo Classification Card (Zero-permission Photo Picker)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isUrdu) "تصویر سے کباڑ کی شناخت (Gemini Vision)" else "AI Scrap Photo Classifier (Advisory Estimate)",
                        fontWeight = FontWeight.Bold,
                        color = OnEmeraldContainer,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "AI classification is an estimate. Final material and price are confirmed during physical inspection.",
                        fontSize = 11.sp,
                        color = OnEmeraldContainer.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_pick_scrap_photo_btn")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing Photo with Gemini...")
                        } else {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Scrap Photo to Analyze")
                        }
                    }

                    scanState?.fold(
                        onSuccess = { res ->
                            Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${res.detectedCategory.icon} ${res.materialName}", fontWeight = FontWeight.Bold, color = EmeraldGreenPrimary)
                                    Text("Grade: ${res.qualityGrade} • Confidence: ${(res.confidenceScore * 100).toInt()}%", fontSize = 12.sp, color = TextPrimaryLight)
                                    Text("Est. Weight Range: ${res.estimatedWeightRange}", fontSize = 12.sp, color = TextPrimaryLight)
                                    Text(res.inspectionNotes, fontSize = 11.sp, color = TextSecondaryLight)
                                    Text(res.disclaimer, fontSize = 10.sp, color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        },
                        onFailure = { err ->
                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = err.localizedMessage ?: "Photo analysis failed.",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Quick Assistant Prompts
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { viewModel.askAssistant("How is my final scrap payout calculated in PKR?", isUrdu, currentRole) },
                    label = { Text("Pricing Rules", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = { viewModel.askAssistant("How should I separate batteries and e-waste safely before pickup?", isUrdu, currentRole) },
                    label = { Text("E-Waste Safety", fontSize = 11.sp) }
                )
            }

            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                label = { Text(if (isUrdu) "اپنا سوال لکھیں..." else "Ask about pickups, rates, sorting, or analytics...") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            viewModel.askAssistant(queryInput, isUrdu, currentRole)
                            queryInput = ""
                        },
                        modifier = Modifier.testTag("ai_send_prompt_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Ask Gemini", tint = EmeraldGreenPrimary)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Streaming Response Area
            when (val s = chatState) {
                is AiUiState.Idle -> {}
                is AiUiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting to Gemini 3.8 Flash...", fontSize = 12.sp)
                    }
                }
                is AiUiState.Streaming -> {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                        Text(s.text, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }
                is AiUiState.Success -> {
                    Surface(color = EmeraldContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)) {
                        Text(s.text, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }
                is AiUiState.Error -> {
                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(12.dp)) {
                        Text(s.message, color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

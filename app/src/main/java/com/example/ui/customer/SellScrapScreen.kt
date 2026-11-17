package com.example.ui.customer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.service.AiService
import com.example.service.PlatformRepository
import com.example.service.PricingEngine
import com.example.ui.components.PkrText
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

@Composable
fun SellScrapScreen(
    isUrdu: Boolean,
    onBack: () -> Unit,
    initialCategory: ScrapCategory = ScrapCategory.IRON_STEEL,
    onSyncPickupToCloud: ((city: String, zone: String, address: String, itemsSummary: String, weightKg: Double, totalPkr: Double, date: String, slot: String, paymentMethod: String, aiNote: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Current Step in 18-step customer pickup flow
    var currentStep by remember { mutableStateOf(1) }

    BackHandler(enabled = currentStep in 2..8) {
        currentStep--
    }

    // Step 1 & 2: Materials & Quantities
    val materials by PlatformRepository.materials.collectAsState()
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var estimatedWeightInput by remember { mutableStateOf("25") }
    var uploadedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Step 3 & 4: Photos & AI Estimation
    var aiScanResult by remember(initialCategory) {
        mutableStateOf(AiService.analyzeScrapItem(initialCategory, 25.0))
    }

    // Step 5: Address
    var city by remember { mutableStateOf("Rawalpindi") }
    var zone by remember { mutableStateOf("Chaklala Scheme III") }
    var streetAddress by remember { mutableStateOf("House 14-B, Street 6, Chaklala Scheme III, Rawalpindi") }

    // Step 6 & 7: Date & Time Slot
    var selectedDate by remember { mutableStateOf("Today, 08 Sep") }
    var selectedSlot by remember { mutableStateOf("02:00 PM - 04:00 PM") }

    // Step 8: Payment method preference
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.EASYPAISA) }
    var customerNotes by remember { mutableStateOf("Scrap is kept in garage, loader can park in front.") }

    // Active pickup state created
    var activePickup by remember { mutableStateOf<PickupRequest?>(null) }

    // OTP entered simulation
    var enteredOtp by remember { mutableStateOf("4892") }

    // Step 18: Rating & Review
    var userRating by remember { mutableStateOf(5) }
    var reviewFeedback by remember { mutableStateOf("Great experience! Fair digital scale weighing and instant Easypaisa payment.") }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("sell_back_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isUrdu) "کباڑ فروخت کریں (مرحلہ $currentStep/18)" else "Sell Scrap (Step $currentStep/18)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = getStepTitle(currentStep, isUrdu),
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreenPrimary
                            )
                        }
                    }

                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${((currentStep / 18.0) * 100).toInt()}%",
                            color = OnEmeraldContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1 && currentStep < 9) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isUrdu) "پیچھے" else "Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            when (currentStep) {
                                1 -> currentStep = 2
                                2 -> {
                                    val w = estimatedWeightInput.toDoubleOrNull() ?: 10.0
                                    aiScanResult = AiService.analyzeScrapItem(selectedCategory, w)
                                    currentStep = 3
                                }
                                3 -> currentStep = 4
                                4 -> currentStep = 5
                                5 -> currentStep = 6
                                6 -> currentStep = 7
                                7 -> currentStep = 8
                                8 -> {
                                    // Confirm pickup and create in system!
                                    val w = estimatedWeightInput.toDoubleOrNull() ?: 10.0
                                    val item = PickupItem(
                                        materialId = "mat_${selectedCategory.name.lowercase()}",
                                        materialName = aiScanResult.materialName,
                                        category = selectedCategory,
                                        estimatedWeightKg = w,
                                        estimatedRatePerKg = aiScanResult.estimatedRatePerKg
                                    )
                                    val created = PlatformRepository.createPickupRequest(
                                        items = listOf(item),
                                        city = city,
                                        zone = zone,
                                        address = streetAddress,
                                        scheduledDate = selectedDate,
                                        timeSlot = selectedSlot,
                                        paymentMethod = selectedPaymentMethod,
                                        notes = customerNotes
                                    )
                                    onSyncPickupToCloud?.invoke(
                                        city,
                                        zone,
                                        streetAddress,
                                        aiScanResult.materialName,
                                        w,
                                        aiScanResult.estimatedTotalPkr,
                                        selectedDate,
                                        selectedSlot,
                                        selectedPaymentMethod.name,
                                        aiScanResult.advisoryNote
                                    )
                                    activePickup = created
                                    enteredOtp = created.otpCode
                                    currentStep = 9
                                }
                                9 -> {
                                    // Assign collector
                                    activePickup?.let { PlatformRepository.collectorAcceptJob(it.id) }
                                    currentStep = 10
                                }
                                10 -> {
                                    // Collector arrives
                                    activePickup?.let { PlatformRepository.collectorMarkArrived(it.id) }
                                    currentStep = 11
                                }
                                11 -> currentStep = 12 // OTP Verification
                                12 -> {
                                    // OTP verification and weighing
                                    val pickup = activePickup
                                    if (pickup != null) {
                                        val weightMap = mapOf(pickup.items.first().materialId to (pickup.items.first().estimatedWeightKg + 2.0))
                                        val ok = PlatformRepository.collectorVerifyOtpAndWeigh(pickup.id, enteredOtp, weightMap)
                                        if (ok) {
                                            activePickup = PlatformRepository.pickups.value.find { it.id == pickup.id }
                                            currentStep = 13
                                        }
                                    }
                                }
                                13 -> currentStep = 14 // Final calculation
                                14 -> currentStep = 15 // Customer confirmation
                                15 -> {
                                    // Customer confirmed, proceed to payout
                                    activePickup?.let { PlatformRepository.customerConfirmAndPay(it.id) }
                                    activePickup = PlatformRepository.pickups.value.find { it.id == activePickup?.id }
                                    currentStep = 16
                                }
                                16 -> currentStep = 17 // Digital receipt
                                17 -> currentStep = 18 // Rating & Review
                                18 -> {
                                    PlatformRepository.showToast("Thank you for your rating! Digital Kabariya appreciates your trust.")
                                    onBack()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("step_next_action_btn")
                    ) {
                        Text(
                            text = getButtonLabel(currentStep, isUrdu),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Linear Step Indicator Bar
            item {
                LinearProgressIndicator(
                    progress = { (currentStep / 18f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldGreenPrimary,
                    trackColor = BorderLight
                )
            }

            // Step Content Renderer
            when (currentStep) {
                1 -> item {
                    Step1SelectMaterial(
                        selectedCategory = selectedCategory,
                        isUrdu = isUrdu,
                        onSelect = { selectedCategory = it }
                    )
                }
                2 -> item {
                    Step2EnterQuantity(
                        category = selectedCategory,
                        weightInput = estimatedWeightInput,
                        isUrdu = isUrdu,
                        onWeightChange = { estimatedWeightInput = it }
                    )
                }
                3 -> item {
                    Step3UploadPhotos(
                        category = selectedCategory,
                        photoCount = uploadedPhotoUris.size,
                        onPhotoAdded = { uri ->
                            uploadedPhotoUris = uploadedPhotoUris + uri
                            PlatformRepository.showToast("Photo attached (${uploadedPhotoUris.size} ready for AI inspection).")
                        },
                        isUrdu = isUrdu
                    )
                }
                4 -> item {
                    Step4AiEstimatePrice(
                        aiResult = aiScanResult,
                        isUrdu = isUrdu
                    )
                }
                5 -> item {
                    Step5SelectAddress(
                        city = city,
                        zone = zone,
                        streetAddress = streetAddress,
                        isUrdu = isUrdu,
                        onCityChange = { city = it },
                        onZoneChange = { zone = it },
                        onStreetChange = { streetAddress = it }
                    )
                }
                6 -> item {
                    Step6SelectDate(
                        selectedDate = selectedDate,
                        isUrdu = isUrdu,
                        onDateSelect = { selectedDate = it }
                    )
                }
                7 -> item {
                    Step7SelectTimeSlot(
                        selectedSlot = selectedSlot,
                        isUrdu = isUrdu,
                        onSlotSelect = { selectedSlot = it }
                    )
                }
                8 -> item {
                    Step8ConfirmPickup(
                        category = selectedCategory,
                        weight = estimatedWeightInput,
                        aiResult = aiScanResult,
                        city = city,
                        address = streetAddress,
                        date = selectedDate,
                        slot = selectedSlot,
                        paymentMethod = selectedPaymentMethod,
                        isUrdu = isUrdu,
                        onPaymentSelect = { selectedPaymentMethod = it },
                        notes = customerNotes,
                        onNotesChange = { customerNotes = it }
                    )
                }
                9 -> item {
                    Step9AssignCollector(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                10 -> item {
                    Step10LiveTracking(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                11 -> item {
                    Step11CollectorArrived(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                12 -> item {
                    Step12OtpVerification(
                        pickup = activePickup,
                        enteredOtp = enteredOtp,
                        isUrdu = isUrdu,
                        onOtpChange = { enteredOtp = it }
                    )
                }
                13 -> item {
                    Step13PhysicalWeighing(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                14 -> item {
                    Step14FinalCalculation(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                15 -> item {
                    Step15CustomerConfirmation(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                16 -> item {
                    Step16PaymentPayout(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                17 -> item {
                    Step17DigitalReceipt(
                        pickup = activePickup,
                        isUrdu = isUrdu
                    )
                }
                18 -> item {
                    Step18RatingReview(
                        rating = userRating,
                        review = reviewFeedback,
                        isUrdu = isUrdu,
                        onRatingChange = { userRating = it },
                        onReviewChange = { reviewFeedback = it }
                    )
                }
            }
        }
    }
}

private fun getStepTitle(step: Int, isUrdu: Boolean): String {
    return if (isUrdu) {
        when (step) {
            1 -> "1. کباڑ کا سامان منتخب کریں"
            2 -> "2. متوقع وزن درج کریں (کلوگرام)"
            3 -> "3. کباڑ کی تصاویر شامل کریں"
            4 -> "4. اے آئی متوقع قیمت اور جانچ"
            5 -> "5. پک اپ کا پتہ منتخب کریں"
            6 -> "6. تاریخ منتخب کریں"
            7 -> "7. مناسب وقت کا انتخاب"
            8 -> "8. کباڑ پک اپ کی تصدیق"
            9 -> "9. کباڑیا نامزدگی"
            10 -> "10. لائیو جی پی ایس ٹریکنگ"
            11 -> "11. کباڑیا دروازے پر پہنچ گیا"
            12 -> "12. او ٹی پی کوڈ کی تصدیق"
            13 -> "13. ڈیجیٹل اسکیل سے وزن کی پیمائش"
            14 -> "14. حتمی رقم کا حساب"
            15 -> "15. کسٹمر کی حتمی منظوری"
            16 -> "16. فوری ادائیگی (ایزی پیسہ / کیش)"
            17 -> "17. ڈیجیٹل رسید و آڈٹ لاگ"
            18 -> "18. ریٹنگ اور سروس کا تاثرات"
            else -> "مرحلہ $step"
        }
    } else {
        when (step) {
            1 -> "1. Select Material"
            2 -> "2. Enter Estimated Quantity"
            3 -> "3. Upload Photos"
            4 -> "4. AI Estimated Price & Quality"
            5 -> "5. Select Address"
            6 -> "6. Select Date"
            7 -> "7. Select Time Slot"
            8 -> "8. Confirm Pickup Request"
            9 -> "9. Assign Collector"
            10 -> "10. Live GPS Tracking"
            11 -> "11. Collector Arrived"
            12 -> "12. Customer OTP Verification"
            13 -> "13. Physical Weighing"
            14 -> "14. Final Amount Calculation"
            15 -> "15. Customer Confirmation"
            16 -> "16. Payment & Payout"
            17 -> "17. Digital Receipt"
            18 -> "18. Rating & Review"
            else -> "Step $step"
        }
    }
}

private fun getButtonLabel(step: Int, isUrdu: Boolean): String {
    return if (isUrdu) {
        when (step) {
            1 -> "وزن درج کریں"
            2 -> "تصاویر لیں"
            3 -> "اے آئی جانچ دیکھیں"
            4 -> "پتہ منتخب کریں"
            5 -> "تاریخ منتخب کریں"
            6 -> "وقت منتخب کریں"
            7 -> "خلاصہ دیکھیں"
            8 -> "پک اپ کنفرم کریں"
            9 -> "کلیکٹر لائیو ٹریک کریں"
            10 -> "پہنچنے کی تصدیق"
            11 -> "او ٹی پی دکھائیں"
            12 -> "او ٹی پی ویریفائی کریں"
            13 -> "رقم کا حساب کریں"
            14 -> "صارف سے منظوری لیں"
            15 -> "ادائیگی حاصل کریں"
            16 -> "رسید حاصل کریں"
            17 -> "ریٹنگ دیں"
            18 -> "مکمل کریں"
            else -> "اگلا مرحلہ"
        }
    } else {
        when (step) {
            1 -> "Enter Quantity"
            2 -> "Add Photos"
            3 -> "View AI Estimate"
            4 -> "Select Address"
            5 -> "Choose Date"
            6 -> "Choose Time Slot"
            7 -> "Review Summary"
            8 -> "Confirm & Assign"
            9 -> "Track Collector"
            10 -> "Collector Arrived"
            11 -> "Show OTP"
            12 -> "Verify OTP & Weigh"
            13 -> "Calculate Total"
            14 -> "Confirm & Payout"
            15 -> "Process Payment"
            16 -> "View Receipt"
            17 -> "Leave Rating"
            18 -> "Finish & Done"
            else -> "Next Step"
        }
    }
}

// STEP 1: Select Material
@Composable
fun Step1SelectMaterial(
    selectedCategory: ScrapCategory,
    isUrdu: Boolean,
    onSelect: (ScrapCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isUrdu) "کون سا کباڑ فروخت کرنا چاہتے ہیں؟" else "What scrap do you want to sell?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = if (isUrdu) "ایک یا زیادہ کیٹیگری منتخب کریں" else "Select a primary category to start weighing and pricing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ScrapCategory.values().forEach { cat ->
            val isSelected = cat == selectedCategory
            Surface(
                color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)) else ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(cat) }
                    .testTag("cat_select_${cat.name}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = cat.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isUrdu) cat.urduName else cat.displayName,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) OnEmeraldContainer else MaterialTheme.colorScheme.onSurface
                            )
                            val estRate = PricingEngine.estimateBaseRate(cat, 10.0)
                            Text(
                                text = "Market Base: ~Rs. ${estRate.toInt()} / kg",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(cat) },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreenPrimary)
                    )
                }
            }
        }
    }
}

// STEP 2: Enter Estimated Quantity
@Composable
fun Step2EnterQuantity(
    category: ScrapCategory,
    weightInput: String,
    isUrdu: Boolean,
    onWeightChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = category.icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isUrdu) category.urduName else category.displayName,
                        fontWeight = FontWeight.Bold,
                        color = OnEmeraldContainer,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Approximate market rate: Rs. ${PricingEngine.estimateBaseRate(category, 10.0).toInt()} / kg",
                        fontSize = 12.sp,
                        color = OnEmeraldContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Text(
            text = if (isUrdu) "متوقع وزن درج کریں (کلوگرام)" else "Enter Estimated Weight (KG)",
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = weightInput,
            onValueChange = onWeightChange,
            label = { Text("Estimated Quantity (KG)") },
            trailingIcon = { Text("KG", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weight_input_field")
        )

        // Quick Selector Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("10", "25", "50", "100", "250")) { preset ->
                FilterChip(
                    selected = weightInput == preset,
                    onClick = { onWeightChange(preset) },
                    label = { Text("$preset kg") }
                )
            }
        }

        Surface(
            color = AmberGoldContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = OnAmberGoldContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUrdu) "نوٹ: حتمی رقم کباڑیا کے مصدقہ ڈیجیٹل کانٹے کے وزن پر طے ہوگی۔" else "Notice: Final payout depends on physical weighing by collector's certified digital scale.",
                    fontSize = 11.sp,
                    color = OnAmberGoldContainer
                )
            }
        }
    }
}

// STEP 3: Upload Photos
@Composable
fun Step3UploadPhotos(
    category: ScrapCategory,
    photoCount: Int,
    onPhotoAdded: (Uri) -> Unit,
    isUrdu: Boolean
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoAdded(uri)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (isUrdu) "کباڑ کی تصاویر لیں یا اپلوڈ کریں" else "Take or Upload Scrap Photos",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = if (isUrdu) "تصویر دیکھ کر ہمارا اے آئی سسٹم درست مٹیریل اور کوالٹی گریڈ کی جانچ کرتا ہے" else "Our AI model analyzes photos to classify material grade and detect impurities",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = if (photoCount > 0) "✅" else "📸", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (photoCount > 0) "$photoCount Photo(s) Attached" else "Optional Scrap Photo Preview",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${category.displayName} • Ready for inspection",
                    color = EmeraldGreenLight,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (photoCount > 0) "Add Another Photo" else "Select Scrap Photo")
            }
        }
    }
}

// STEP 4: AI Estimated Price
@Composable
fun Step4AiEstimatePrice(
    aiResult: AiService.AiScrapScanResult,
    isUrdu: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🤖", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "اے آئی کباڑ اسکین تجزیہ" else "AI Scrap Vision Scan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${(aiResult.confidenceScore * 100).toInt()}% Confidence",
                            color = OnEmeraldContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = aiResult.materialName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = EmeraldGreenPrimary
                )
                Text(
                    text = "Quality Grade: ${aiResult.qualityGrade}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Estimated Weight Range", fontSize = 11.sp, color = Color.Gray)
                        Text(aiResult.estimatedWeightRange, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rate Per KG", fontSize = 11.sp, color = Color.Gray)
                        PkrText(amount = aiResult.estimatedRatePerKg)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUrdu) "کل متوقع قیمت:" else "Estimated Payout:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    PkrText(amount = aiResult.estimatedTotalPkr, style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // CRITICAL MANDATORY DISCLAIMER
        Surface(
            color = Color(0xFFFEF2F2),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF87171)))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Warning",
                    tint = Color(0xFFDC2626)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isUrdu) "اہم: اے آئی کا تخمینہ حتمی نہیں ہے۔ حتمی قیمت کباڑیا کے روبرو تصدیق شدہ وزن اور جانچ پر طے ہوگی۔" else "AI estimate only. Final price depends on physical inspection and verified weight.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF991B1B)
                )
            }
        }
    }
}

// STEP 5: Select Address
@Composable
fun Step5SelectAddress(
    city: String,
    zone: String,
    streetAddress: String,
    isUrdu: Boolean,
    onCityChange: (String) -> Unit,
    onZoneChange: (String) -> Unit,
    onStreetChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isUrdu) "پک اپ کا پتہ درج کریں" else "Enter Pickup Address",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        // City Selector Chips
        Text(text = "City", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("Rawalpindi", "Islamabad", "Karachi", "Lahore", "Faisalabad")) { c ->
                FilterChip(
                    selected = city == c,
                    onClick = { onCityChange(c) },
                    label = { Text(c) }
                )
            }
        }

        OutlinedTextField(
            value = zone,
            onValueChange = onZoneChange,
            label = { Text("Zone / Area (e.g. DHA, Gulberg, F-10, Korangi)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = streetAddress,
            onValueChange = onStreetChange,
            label = { Text("Full Street & House / Shop Address") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Surface(
            color = EmeraldContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = EmeraldGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GPS Coordinates auto-pinned: 24.8607° N, 67.0011° E",
                    fontSize = 11.sp,
                    color = OnEmeraldContainer
                )
            }
        }
    }
}

// STEP 6: Select Date
@Composable
fun Step6SelectDate(
    selectedDate: String,
    isUrdu: Boolean,
    onDateSelect: (String) -> Unit
) {
    val dates = listOf("Today, 08 Sep", "Tomorrow, 09 Sep", "Wednesday, 10 Sep", "Thursday, 11 Sep")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isUrdu) "پک اپ کی تاریخ منتخب کریں" else "Select Pickup Date",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        dates.forEach { d ->
            val isSelected = d == selectedDate
            Surface(
                color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)) else ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateSelect(d) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = d,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) OnEmeraldContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onDateSelect(d) },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreenPrimary)
                    )
                }
            }
        }
    }
}

// STEP 7: Select Time Slot
@Composable
fun Step7SelectTimeSlot(
    selectedSlot: String,
    isUrdu: Boolean,
    onSlotSelect: (String) -> Unit
) {
    val slots = listOf(
        "09:00 AM - 11:00 AM" to "Morning Slot (Free pickup)",
        "02:00 PM - 04:00 PM" to "Afternoon Slot (Recommended)",
        "05:00 PM - 07:00 PM" to "Evening Slot (High demand)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isUrdu) "مناسب وقت منتخب کریں" else "Select Time Slot",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        slots.forEach { (slot, desc) ->
            val isSelected = slot == selectedSlot
            Surface(
                color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)) else ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSlotSelect(slot) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = slot,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) OnEmeraldContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSlotSelect(slot) },
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreenPrimary)
                    )
                }
            }
        }
    }
}

// STEP 8: Confirm Pickup Request
@Composable
fun Step8ConfirmPickup(
    category: ScrapCategory,
    weight: String,
    aiResult: AiService.AiScrapScanResult,
    city: String,
    address: String,
    date: String,
    slot: String,
    paymentMethod: PaymentMethod,
    isUrdu: Boolean,
    onPaymentSelect: (PaymentMethod) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isUrdu) "آرڈر کا جائزہ لیں اور بک کریں" else "Review Pickup Summary",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Material:", color = Color.Gray, fontSize = 13.sp)
                    Text(aiResult.materialName, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Weight:", color = Color.Gray, fontSize = 13.sp)
                    Text("$weight KG", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Date & Slot:", color = Color.Gray, fontSize = 13.sp)
                    Text("$date ($slot)", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pickup Address:", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$city • $address",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Payout (Advisory):", fontWeight = FontWeight.Bold)
                    PkrText(amount = aiResult.estimatedTotalPkr)
                }
            }
        }

        Text(text = "Preferred Payment Method on Completion:", fontWeight = FontWeight.Bold)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(PaymentMethod.CASH, PaymentMethod.EASYPAISA, PaymentMethod.JAZZCASH, PaymentMethod.RAAST)) { method ->
                FilterChip(
                    selected = paymentMethod == method,
                    onClick = { onPaymentSelect(method) },
                    label = { Text(method.label) }
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Special Gate / Parking Instructions") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// STEP 9: Assign Collector
@Composable
fun Step9AssignCollector(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(EmeraldContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🛵", fontSize = 36.sp)
        }

        Text(
            text = if (isUrdu) "کباڑیا نامزد ہو چکا ہے!" else "Collector Assigned!",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = EmeraldGreenPrimary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "RK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pickup?.assignedCollectorName ?: "Rashid Khan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Vehicle: Qingqi Loader KHI-412 (Verified)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐ 4.92 (284 pickups)", fontSize = 11.sp, color = AmberGoldSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { PlatformRepository.showToast("Calling collector +92 333 9876543...") },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Collector")
                    }

                    OutlinedButton(
                        onClick = { PlatformRepository.showToast("Message sent to collector.") },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat")
                    }
                }
            }
        }
    }
}

// STEP 10: Live Tracking
@Composable
fun Step10LiveTracking(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUrdu) "لائیو جی پی ایس ٹریکنگ" else "Live GPS Route Tracking",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "ETA: 12 Mins (1.8 km)",
                    color = Color(0xFF0369A1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Map Canvas Box Simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🗺️", fontSize = 42.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Qingqi Loader En-Route via Khayaban-e-Shamsheer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Speed: 28 km/h • Geofence Active",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Surface(
            color = AmberGoldContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔑", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Your Pickup Verification OTP: ${pickup?.otpCode ?: "4892"}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = OnAmberGoldContainer
                    )
                    Text(
                        text = "Do not share OTP until collector arrives at your gate with digital scale.",
                        fontSize = 11.sp,
                        color = OnAmberGoldContainer
                    )
                }
            }
        }
    }
}

// STEP 11: Collector Arrived
@Composable
fun Step11CollectorArrived(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3E8FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔔", fontSize = 36.sp)
        }

        Text(
            text = if (isUrdu) "کباڑیا پہنچ گیا ہے!" else "Collector Has Arrived!",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Color(0xFF7E22CE)
        )

        Text(
            text = "Rashid Khan is waiting at your gate with certified electronic hanging/platform scale.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Safety & Verification Checklist:", fontWeight = FontWeight.Bold)
                Text("✅ Digital Kabariya Badge & Uniform Checked", fontSize = 12.sp)
                Text("✅ Electronic scale zeroed before customer", fontSize = 12.sp)
                Text("✅ Pre-weighing scrap photo recorded", fontSize = 12.sp)
            }
        }
    }
}

// STEP 12: OTP Verification
@Composable
fun Step12OtpVerification(
    pickup: PickupRequest?,
    enteredOtp: String,
    isUrdu: Boolean,
    onOtpChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = if (isUrdu) "او ٹی پی تصدیق" else "Customer OTP Verification",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Collector asks for this 4-digit code to unlock weighing scale and record physical weights.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SECURITY PASSCODE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnEmeraldContainer)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pickup?.otpCode ?: "4892",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                    color = OnEmeraldContainer
                )
            }
        }

        OutlinedTextField(
            value = enteredOtp,
            onValueChange = onOtpChange,
            label = { Text("Enter OTP Code to confirm scale handshake") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// STEP 13: Physical Weighing
@Composable
fun Step13PhysicalWeighing(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isUrdu) "ڈیجیٹل ترازو سے پیمائش" else "Physical Scale Weighing",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DIGITAL WEIGHT SENSOR READOUT", color = EmeraldGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                val verWeight = pickup?.items?.firstOrNull()?.verifiedWeightKg ?: 27.0
                Text(
                    text = "${"%.1f".format(verWeight)} KG",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )
                Text("Tare Weight: 0.00 kg • Precision: ±50g calibrated", color = Color.Gray, fontSize = 11.sp)
            }
        }

        val firstItem = pickup?.items?.firstOrNull()
        val appliedRate = (firstItem?.verifiedRatePerKg?.takeIf { it > 0 } ?: firstItem?.estimatedRatePerKg) ?: 145.0

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Material Inspected:", fontWeight = FontWeight.Bold)
                Text(firstItem?.materialName ?: "Iron / Steel", color = EmeraldGreenPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Grade: ${firstItem?.grade ?: "Verified Grade A"} (Physical scale inspection completed)", fontSize = 12.sp)
                Text("Approved Unit Rate: ${formatPkr(appliedRate)} / KG", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// STEP 14: Final Calculation
@Composable
fun Step14FinalCalculation(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    val firstItem = pickup?.items?.firstOrNull()
    val verifiedWeight = pickup?.totalWeightKg?.takeIf { it > 0 } ?: 27.0
    val appliedRate = (firstItem?.verifiedRatePerKg?.takeIf { it > 0 } ?: firstItem?.estimatedRatePerKg) ?: 145.0
    val total = pickup?.verifiedTotal?.takeIf { it > 0 } ?: (verifiedWeight * appliedRate)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isUrdu) "حتمی رقم کا حساب" else "Final Amount Calculation",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("VERIFIED PAYOUT AMOUNT", color = EmeraldContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatPkr(total),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Verified Weight:", color = Color.White, fontSize = 13.sp)
                    Text("${"%.1f".format(verifiedWeight)} KG", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rate Applied:", color = Color.White, fontSize = 13.sp)
                    Text("${formatPkr(appliedRate)} / KG", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Doorstep Collection Fee:", color = Color.White, fontSize = 13.sp)
                    Text("FREE (Rs. 0)", color = EmeraldGreenLight, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// STEP 15: Customer Confirmation
@Composable
fun Step15CustomerConfirmation(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isUrdu) "کسٹمر کی حتمی منظوری" else "Customer Confirmation",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Please verify the scale reading and rate before approving payment release.",
            style = MaterialTheme.typography.bodySmall
        )

        Surface(
            color = Color(0xFFDCFCE7),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SuccessGreen))
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "I confirm that the physical weight and scrap quality were inspected in my presence.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF166534)
                )
            }
        }
    }
}

// STEP 16: Payment & Payout
@Composable
fun Step16PaymentPayout(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(EmeraldContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldGreenPrimary, modifier = Modifier.size(40.dp))
        }

        Text(
            text = if (isUrdu) "ادائیگی کامیابی سے منتقل ہو گئی!" else "Payment Processed Successfully!",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = EmeraldGreenPrimary
        )

        val total = pickup?.verifiedTotal?.takeIf { it > 0 } ?: 3915.0
        Text(
            text = formatPkr(total),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Paid via ${pickup?.paymentMethod?.label ?: "Easypaisa"}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Transaction ID: TX-PKR-${System.currentTimeMillis().toString().takeLast(8)} • Ledger Audited",
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// STEP 17: Digital Receipt
@Composable
fun Step17DigitalReceipt(
    pickup: PickupRequest?,
    isUrdu: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = if (isUrdu) "ڈیجیٹل کباڑیا باضابطہ رسید" else "Official Digital Receipt",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DIGITAL KABARIYA PK", fontWeight = FontWeight.Black, color = EmeraldGreenPrimary)
                    Text(pickup?.digitalReceiptNumber ?: "RCPT-KHI-2026-8812", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("Govt NTN: 8912304-1 • Recycling Tax Exempt", fontSize = 10.sp, color = Color.Gray)
                Divider(modifier = Modifier.padding(vertical = 4.dp))

                val receiptItem = pickup?.items?.firstOrNull()
                val receiptWeight = pickup?.totalWeightKg?.takeIf { it > 0 } ?: 27.0
                val receiptRate = (receiptItem?.verifiedRatePerKg?.takeIf { it > 0 } ?: receiptItem?.estimatedRatePerKg) ?: 145.0
                val receiptTotal = pickup?.verifiedTotal?.takeIf { it > 0 } ?: (receiptWeight * receiptRate)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Customer:", color = Color.Gray, fontSize = 12.sp)
                    Text("${pickup?.customerName ?: "Customer"} (${pickup?.city ?: "Rawalpindi"})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Collector:", color = Color.Gray, fontSize = 12.sp)
                    Text(pickup?.assignedCollectorName ?: "Rashid Khan", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Material:", color = Color.Gray, fontSize = 12.sp)
                    Text("${receiptItem?.materialName ?: "Iron / Steel"} (${receiptItem?.grade ?: "Grade A"})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Certified Weight:", color = Color.Gray, fontSize = 12.sp)
                    Text("${"%.1f".format(receiptWeight)} KG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rate Applied:", color = Color.Gray, fontSize = 12.sp)
                    Text("${formatPkr(receiptRate)} / KG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Payout Settled (${pickup?.paymentMethod?.label ?: "Easypaisa"}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    PkrText(amount = receiptTotal)
                }
            }
        }

        OutlinedButton(
            onClick = { PlatformRepository.showToast("Receipt downloaded as PDF to device.") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Download PDF Tax Receipt")
        }
    }
}

// STEP 18: Rating & Review
@Composable
fun Step18RatingReview(
    rating: Int,
    review: String,
    isUrdu: Boolean,
    onRatingChange: (Int) -> Unit,
    onReviewChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isUrdu) "کباڑیا کی خدمت کو ریٹنگ دیں" else "Rate Your Experience",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        // 5-Star Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { star ->
                IconButton(onClick = { onRatingChange(star) }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $star",
                        tint = if (star <= rating) AmberGoldSecondary else Color.LightGray,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = review,
            onValueChange = onReviewChange,
            label = { Text("Write your feedback") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}

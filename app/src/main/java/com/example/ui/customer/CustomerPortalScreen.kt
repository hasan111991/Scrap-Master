package com.example.ui.customer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollectionHubCatalog
import com.example.model.*
import com.example.ui.CollectionHubViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class CustomerTab(val label: String) {
    DASHBOARD("Dashboard"),
    BOOK("Book Pickup"),
    MY_PICKUPS("My Pickups"),
    ACCOUNT("Account")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerPortalScreen(
    viewModel: CollectionHubViewModel,
    categories: List<HubScrapCategory>,
    prices: List<HubScrapPrice>,
    bookings: List<HubBooking>,
    notifications: List<HubNotificationItem>,
    complaints: List<HubComplaint>,
    customerProfile: HubCustomerProfile?,
    userAccount: HubUserAccount?,
    isBusy: Boolean,
    openBookingWizardTrigger: Int,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(CustomerTab.DASHBOARD) }
    var wizardStep by remember { mutableIntStateOf(1) }

    LaunchedEffect(openBookingWizardTrigger) {
        if (openBookingWizardTrigger > 0) {
            activeTab = CustomerTab.BOOK
            wizardStep = 1
        }
    }

    BackHandler(enabled = activeTab != CustomerTab.DASHBOARD || wizardStep > 1) {
        if (activeTab == CustomerTab.BOOK && wizardStep > 1) {
            wizardStep--
        } else {
            activeTab = CustomerTab.DASHBOARD
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HubBackgroundLight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == CustomerTab.DASHBOARD,
                    onClick = { activeTab = CustomerTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = activeTab == CustomerTab.BOOK,
                    onClick = { activeTab = CustomerTab.BOOK },
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Book Pickup") },
                    label = { Text("Book") }
                )
                NavigationBarItem(
                    selected = activeTab == CustomerTab.MY_PICKUPS,
                    onClick = { activeTab = CustomerTab.MY_PICKUPS },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = "My Pickups") },
                    label = { Text("Pickups") }
                )
                NavigationBarItem(
                    selected = activeTab == CustomerTab.ACCOUNT,
                    onClick = { activeTab = CustomerTab.ACCOUNT },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    label = { Text("Account") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                CustomerTab.DASHBOARD -> CustomerDashboardTab(
                    bookings = bookings,
                    prices = prices,
                    notifications = notifications,
                    customerName = customerProfile?.name ?: userAccount?.displayName ?: viewModel.displayName,
                    onBookNow = {
                        wizardStep = 1
                        activeTab = CustomerTab.BOOK
                    },
                    onViewPickups = { activeTab = CustomerTab.MY_PICKUPS },
                    onMarkNotificationRead = { viewModel.markNotificationRead(it) }
                )

                CustomerTab.BOOK -> CustomerBookingWizardTab(
                    step = wizardStep,
                    onStepChange = { wizardStep = it },
                    categories = categories.filter { it.active },
                    prices = prices.filter { it.active },
                    defaultName = customerProfile?.name ?: userAccount?.displayName ?: viewModel.displayName,
                    defaultPhone = customerProfile?.phone ?: userAccount?.phone.orEmpty(),
                    defaultCity = customerProfile?.city ?: userAccount?.city ?: "Rawalpindi",
                    defaultArea = customerProfile?.area ?: userAccount?.area ?: "Chaklala Scheme III",
                    defaultAddress = customerProfile?.defaultAddress ?: userAccount?.defaultAddress.orEmpty(),
                    isBusy = isBusy,
                    onSubmitBooking = { name, phone, city, area, addr, cats, summary, unit, weight, minPkr, maxPkr, date, slot, pay, photo, notes ->
                        viewModel.createBooking(
                            customerName = name,
                            customerPhone = phone,
                            city = city,
                            area = area,
                            fullAddress = addr,
                            categories = cats,
                            itemsSummary = summary,
                            unitType = unit,
                            estimatedWeightKg = weight,
                            estimatedMinPkr = minPkr,
                            estimatedMaxPkr = maxPkr,
                            preferredDate = date,
                            preferredTimeSlot = slot,
                            paymentMethod = pay,
                            photoUri = photo,
                            notes = notes
                        ) {
                            wizardStep = 1
                            activeTab = CustomerTab.MY_PICKUPS
                        }
                    }
                )

                CustomerTab.MY_PICKUPS -> CustomerBookingsTab(
                    bookings = bookings,
                    onCancelBooking = { id, reason -> viewModel.cancelBooking(id, reason) },
                    onConfirmCompletion = { booking, rating, review ->
                        viewModel.confirmBookingCompletion(booking, rating, review)
                    }
                )

                CustomerTab.ACCOUNT -> CustomerAccountTab(
                    userAccount = userAccount,
                    customerProfile = customerProfile,
                    bookings = bookings,
                    complaints = complaints,
                    notifications = notifications,
                    onSaveProfile = { name, phone, city, area, addr ->
                        viewModel.updateCustomerProfile(name, phone, city, area, addr)
                    },
                    onSubmitComplaint = { bookingId, cat, subj, desc ->
                        viewModel.submitComplaint("customer", bookingId, cat, subj, desc)
                    },
                    onMarkNotificationRead = { viewModel.markNotificationRead(it) }
                )
            }
        }
    }
}

@Composable
private fun CustomerDashboardTab(
    bookings: List<HubBooking>,
    prices: List<HubScrapPrice>,
    notifications: List<HubNotificationItem>,
    customerName: String,
    onBookNow: () -> Unit,
    onViewPickups: () -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    val activeBookings = bookings.filter { it.status !in listOf("completed", "cancelled") }
    val completedCount = bookings.count { it.status == "completed" }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HubGreenPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Assalam-o-Alaikum, $customerName",
                        color = HubGreenContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ready to schedule a scrap pickup?",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Button(
                        onClick = onBookNow,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HubGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("customer_book_pickup_btn")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Pickup Request", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniCard(
                    title = "Active Pickups",
                    value = "${activeBookings.size}",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "Completed",
                    value = "$completedCount",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (activeBookings.isNotEmpty()) {
            item {
                val lead = activeBookings.first()
                HubSectionCard(
                    title = "Live Booking Status",
                    subtitle = "Booking #${lead.id.take(10)} • ${lead.area}, ${lead.city}"
                ) {
                    HubBookingStatusBadge(lead.status)
                    Text(
                        text = lead.itemsSummary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (lead.assignedCollectorName.isNotBlank()) {
                        Surface(color = HubGreenSoftBg, shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                Text("Assigned Collector: ${lead.assignedCollectorName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HubGreenPrimary)
                                if (lead.assignedCollectorPhone.isNotBlank()) {
                                    Text("Contact: ${lead.assignedCollectorPhone}", fontSize = 12.sp, color = HubTextSecondaryLight)
                                }
                            }
                        }
                    }
                    Surface(color = HubAmberContainer, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "Doorstep OTP Code: ${lead.otpCode}",
                            fontWeight = FontWeight.Bold,
                            color = OnHubAmberContainer,
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                        )
                    }
                    TextButton(onClick = onViewPickups) {
                        Text("Manage Pickup & Confirm Weight ->", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            HubSectionCard(
                title = "Indicative Scrap Rates (Rawalpindi & Islamabad)",
                subtitle = "Updated dynamically by admins; final amount verified at scale."
            ) {
                IndicativePriceDisclaimerBanner()
                prices.take(6).forEach { price ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(price.categoryName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = "Est. Rs. ${price.minEstimatedPkr.toInt()}-${price.maxEstimatedPkr.toInt()} / ${price.unit}",
                            color = HubGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    HorizontalDivider(color = HubBorderLight)
                }
            }
        }

        if (notifications.any { !it.read }) {
            item {
                HubSectionCard(title = "Recent Updates") {
                    notifications.filter { !it.read }.take(3).forEach { n ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkNotificationRead(n.id) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(n.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(n.body, fontSize = 12.sp, color = HubTextSecondaryLight)
                            }
                            Text("Mark read", fontSize = 11.sp, color = HubGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.border(1.dp, HubBorderLight, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = HubTextSecondaryLight)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = HubGreenPrimary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerBookingWizardTab(
    step: Int,
    onStepChange: (Int) -> Unit,
    categories: List<HubScrapCategory>,
    prices: List<HubScrapPrice>,
    defaultName: String,
    defaultPhone: String,
    defaultCity: String,
    defaultArea: String,
    defaultAddress: String,
    isBusy: Boolean,
    onSubmitBooking: (
        name: String,
        phone: String,
        city: String,
        area: String,
        addr: String,
        cats: List<String>,
        summary: String,
        unit: String,
        weight: Double,
        minPkr: Double,
        maxPkr: Double,
        date: String,
        slot: String,
        pay: String,
        photo: String?,
        notes: String?
    ) -> Unit
) {
    var selectedCategories by remember { mutableStateOf(listOf(categories.firstOrNull()?.name ?: "Iron")) }
    var unitType by remember { mutableStateOf("KG") }
    var weightInput by remember { mutableStateOf("20") }
    var itemsSummary by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    var city by remember(defaultCity) { mutableStateOf(defaultCity.ifBlank { "Rawalpindi" }) }
    var area by remember(defaultArea) { mutableStateOf(defaultArea.ifBlank { "Chaklala Scheme III" }) }
    var fullAddress by remember(defaultAddress) { mutableStateOf(defaultAddress) }
    var customerName by remember(defaultName) { mutableStateOf(defaultName) }
    var customerPhone by remember(defaultPhone) { mutableStateOf(defaultPhone) }

    var preferredDate by remember { mutableStateOf("Today") }
    var preferredSlot by remember { mutableStateOf(CollectionHubCatalog.defaultTimeSlots.first()) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        photoUri = uri
    }

    val numericQty = weightInput.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 10.0
    val matchingPrices = prices.filter { p -> selectedCategories.any { it.equals(p.categoryName, ignoreCase = true) } }
    val avgMinPerUnit = if (matchingPrices.isEmpty()) 45.0 else matchingPrices.map { it.minEstimatedPkr }.average()
    val avgMaxPerUnit = if (matchingPrices.isEmpty()) 90.0 else matchingPrices.map { it.maxEstimatedPkr }.average()
    val estimatedMinTotal = numericQty * avgMinPerUnit
    val estimatedMaxTotal = numericQty * avgMaxPerUnit

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HubSectionCard(
                title = "Book Scrap Pickup — Step $step of 5",
                subtitle = when (step) {
                    1 -> "Select one or more scrap categories"
                    2 -> "Estimated quantity & optional photo"
                    3 -> "Address in Rawalpindi or Islamabad"
                    4 -> "Schedule & preferred manual payout"
                    else -> "Review request & confirm booking"
                }
            ) {
                LinearProgressIndicator(
                    progress = { step / 5f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                    color = HubGreenPrimary,
                    trackColor = HubGreenContainer
                )
            }
        }

        item {
            when (step) {
                1 -> {
                    HubSectionCard(title = "Select Scrap Categories") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val selected = selectedCategories.contains(cat.name)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedCategories = if (selected && selectedCategories.size > 1) {
                                            selectedCategories - cat.name
                                        } else if (!selected) {
                                            selectedCategories + cat.name
                                        } else {
                                            selectedCategories
                                        }
                                    },
                                    label = { Text("${cat.name} (${cat.urduName})") }
                                )
                            }
                        }
                        IndicativePriceDisclaimerBanner()
                    }
                }

                2 -> {
                    HubSectionCard(title = "Quantity & Optional Photo") {
                        Text("Unit Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("KG", "Piece", "Bag", "Bulk").forEach { unit ->
                                FilterChip(
                                    selected = unitType == unit,
                                    onClick = { unitType = unit },
                                    label = { Text(unit) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Estimated Quantity ($unitType)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = itemsSummary,
                            onValueChange = { itemsSummary = it },
                            label = { Text("Item Details (optional)") },
                            placeholder = { Text("e.g., 15kg iron pipes + 5kg old newspapers") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (photoUri == null) "Attach Scrap Photo (Android Photo Picker)" else "Photo Selected (${photoUri?.lastPathSegment ?: "image"})")
                        }
                        Surface(color = HubGreenSoftBg, shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    text = "Indicative Estimated Range: ${formatPkr(estimatedMinTotal)} – ${formatPkr(estimatedMaxTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    color = HubGreenPrimary
                                )
                                Text(
                                    text = "Estimate only. Final amount is set after doorstep inspection & physical weighing.",
                                    fontSize = 11.sp,
                                    color = HubTextSecondaryLight
                                )
                            }
                        }
                    }
                }

                3 -> {
                    HubSectionCard(
                        title = "Pickup Address (Private Until Assigned)",
                        subtitle = "Collectors only see your area name until one accepts."
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Your Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Contact Phone (e.g., 0300-1234567)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("City", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CollectionHubCatalog.supportedCities.forEach { c ->
                                FilterChip(
                                    selected = city == c,
                                    onClick = {
                                        city = c
                                        area = CollectionHubCatalog.areasByCity[c]?.firstOrNull().orEmpty()
                                    },
                                    label = { Text(c) }
                                )
                            }
                        }
                        Text("Sector / Area", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (CollectionHubCatalog.areasByCity[city] ?: emptyList()).forEach { a ->
                                FilterChip(
                                    selected = area == a,
                                    onClick = { area = a },
                                    label = { Text(a, fontSize = 12.sp) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = fullAddress,
                            onValueChange = { fullAddress = it },
                            label = { Text("Complete House / Shop / Street Address") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                4 -> {
                    HubSectionCard(title = "Pickup Schedule & Manual Payment Preference") {
                        Text("Preferred Date", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Today", "Tomorrow", "In 2 Days").forEach { d ->
                                FilterChip(selected = preferredDate == d, onClick = { preferredDate = d }, label = { Text(d) })
                            }
                        }
                        Text("Time Slot", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CollectionHubCatalog.defaultTimeSlots.forEach { slot ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { preferredSlot = slot }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(selected = preferredSlot == slot, onClick = { preferredSlot = slot })
                                    Text(slot, fontSize = 13.sp)
                                }
                            }
                        }
                        Text("Preferred Manual Payment Method", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "Easypaisa", "Bank Transfer").forEach { pm ->
                                FilterChip(selected = paymentMethod == pm, onClick = { paymentMethod = pm }, label = { Text(pm) })
                            }
                        }
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Pickup Instructions (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    HubSectionCard(title = "Confirm Scrap Pickup Request") {
                        Text("Categories: ${selectedCategories.joinToString(", ")}", fontWeight = FontWeight.Bold)
                        Text("Estimated Qty: $numericQty $unitType")
                        Text("Indicative Range: ${formatPkr(estimatedMinTotal)} – ${formatPkr(estimatedMaxTotal)}", color = HubGreenPrimary, fontWeight = FontWeight.Bold)
                        Text("Location: $area, $city")
                        Text("Address: ${fullAddress.ifBlank { "(Required - go back to Step 3)" }}")
                        Text("Phone: ${customerPhone.ifBlank { "(Required - go back to Step 3)" }}")
                        Text("Schedule: $preferredDate ($preferredSlot)")
                        Text("Payout Method: $paymentMethod (settled manually at pickup)")
                        IndicativePriceDisclaimerBanner()
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { onStepChange(step - 1) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < 5) {
                            onStepChange(step + 1)
                        } else {
                            onSubmitBooking(
                                customerName,
                                customerPhone,
                                city,
                                area,
                                fullAddress,
                                selectedCategories,
                                itemsSummary.ifBlank { selectedCategories.joinToString(", ") },
                                unitType,
                                numericQty,
                                estimatedMinTotal,
                                estimatedMaxTotal,
                                preferredDate,
                                preferredSlot,
                                paymentMethod,
                                photoUri?.toString(),
                                notes
                            )
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("wizard_next_submit_btn")
                ) {
                    Text(if (step < 5) "Continue" else "Submit Pickup Request", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerBookingsTab(
    bookings: List<HubBooking>,
    onCancelBooking: (String, String) -> Unit,
    onConfirmCompletion: (HubBooking, Int, String) -> Unit
) {
    var cancelTargetId by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("Schedule changed") }
    var confirmTarget by remember { mutableStateOf<HubBooking?>(null) }
    var rating by remember { mutableIntStateOf(5) }
    var review by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bookings.isEmpty()) {
            item {
                HubEmptyState(
                    icon = Icons.Default.LocalShipping,
                    title = "No Pickup Bookings Yet",
                    subtitle = "Use the Book tab to request your first scrap pickup in Rawalpindi or Islamabad."
                )
            }
        } else {
            items(bookings, key = { it.id }) { booking ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HubBorderLight, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${booking.id.take(10)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            HubBookingStatusBadge(booking.status)
                        }
                        Text(booking.itemsSummary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${booking.area}, ${booking.city} • ${booking.preferredDate} (${booking.preferredTimeSlot})", fontSize = 12.sp, color = HubTextSecondaryLight)
                        Text(
                            text = "Est. Range: ${formatPkr(booking.estimatedMinPkr)} – ${formatPkr(booking.estimatedMaxPkr)} (${booking.estimatedWeightKg} ${booking.unitType})",
                            fontSize = 12.sp,
                            color = HubTextSecondaryLight
                        )

                        if (booking.assignedCollectorName.isNotBlank()) {
                            Surface(color = HubGreenSoftBg, shape = RoundedCornerShape(10.dp)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                    Text("Collector: ${booking.assignedCollectorName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HubGreenPrimary)
                                    Text("Phone: ${booking.assignedCollectorPhone}", fontSize = 12.sp, color = HubTextPrimaryLight)
                                }
                            }
                        }

                        if (booking.status !in listOf("completed", "cancelled")) {
                            Surface(color = HubAmberContainer, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "Pickup OTP: ${booking.otpCode} (share only when collector arrives)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = OnHubAmberContainer,
                                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                                )
                            }
                        }

                        if (booking.verifiedWeightKg > 0.0 || booking.finalAmountPkr > 0.0) {
                            Surface(color = HubGreenContainer, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "Verified Weight: ${booking.verifiedWeightKg} KG • Final Payout: ${formatPkr(booking.finalAmountPkr)} (${booking.paymentMethod})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = HubGreenDark,
                                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                                )
                            }
                        }

                        if (booking.status == "weight_price_confirmation") {
                            Button(
                                onClick = { confirmTarget = booking },
                                colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirm Weight, Price & Payment Received", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (booking.status in listOf("searching", "assigned")) {
                            OutlinedButton(
                                onClick = { cancelTargetId = booking.id },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Booking")
                            }
                        }
                    }
                }
            }
        }
    }

    if (cancelTargetId != null) {
        AlertDialog(
            onDismissRequest = { cancelTargetId = null },
            title = { Text("Cancel Pickup Booking") },
            text = {
                OutlinedTextField(
                    value = cancelReason,
                    onValueChange = { cancelReason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        cancelTargetId?.let { onCancelBooking(it, cancelReason) }
                        cancelTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Confirm Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { cancelTargetId = null }) { Text("Keep Booking") }
            }
        )
    }

    confirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmTarget = null },
            title = { Text("Confirm Completion & Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Verified Weight: ${target.verifiedWeightKg} KG")
                    Text("Final Amount Received (${target.paymentMethod}): ${formatPkr(target.finalAmountPkr)}", fontWeight = FontWeight.Bold, color = HubGreenPrimary)
                    Text("Rate Collector (1–5)", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            FilterChip(selected = rating == star, onClick = { rating = star }, label = { Text("$star★") })
                        }
                    }
                    OutlinedTextField(
                        value = review,
                        onValueChange = { review = it },
                        label = { Text("Feedback (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmCompletion(target, rating, review)
                        confirmTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                ) { Text("Confirm & Complete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CustomerAccountTab(
    userAccount: HubUserAccount?,
    customerProfile: HubCustomerProfile?,
    bookings: List<HubBooking>,
    complaints: List<HubComplaint>,
    notifications: List<HubNotificationItem>,
    onSaveProfile: (String, String, String, String, String) -> Unit,
    onSubmitComplaint: (String?, String, String, String) -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    var name by remember(customerProfile, userAccount) {
        mutableStateOf(customerProfile?.name ?: userAccount?.displayName.orEmpty())
    }
    var phone by remember(customerProfile, userAccount) {
        mutableStateOf(customerProfile?.phone ?: userAccount?.phone.orEmpty())
    }
    var city by remember(customerProfile, userAccount) {
        mutableStateOf(customerProfile?.city ?: userAccount?.city ?: "Rawalpindi")
    }
    var area by remember(customerProfile, userAccount) {
        mutableStateOf(customerProfile?.area ?: userAccount?.area ?: "Chaklala Scheme III")
    }
    var address by remember(customerProfile, userAccount) {
        mutableStateOf(customerProfile?.defaultAddress ?: userAccount?.defaultAddress.orEmpty())
    }

    var compSubject by remember { mutableStateOf("") }
    var compDesc by remember { mutableStateOf("") }
    var compCategory by remember { mutableStateOf("Weight / Pricing") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HubSectionCard(title = "Customer Profile", subtitle = userAccount?.email ?: "") {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectionHubCatalog.supportedCities.forEach { c ->
                        FilterChip(
                            selected = city == c,
                            onClick = {
                                city = c
                                area = CollectionHubCatalog.areasByCity[c]?.firstOrNull().orEmpty()
                            },
                            label = { Text(c) }
                        )
                    }
                }
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area / Sector") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Default Address") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { onSaveProfile(name, phone, city, area, address) },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Profile")
                }
            }
        }

        item {
            HubSectionCard(title = "Notifications (${notifications.size})") {
                if (notifications.isEmpty()) {
                    Text("No notifications yet.", fontSize = 13.sp, color = HubTextSecondaryLight)
                } else {
                    notifications.take(10).forEach { n ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkNotificationRead(n.id) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(n.title, fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, fontSize = 13.sp)
                                Text(n.body, fontSize = 12.sp, color = HubTextSecondaryLight)
                            }
                            if (!n.read) {
                                Text("NEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HubGreenPrimary)
                            }
                        }
                        HorizontalDivider(color = HubBorderLight)
                    }
                }
            }
        }

        item {
            HubSectionCard(title = "Raise Support Complaint") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Weight / Pricing", "Collector Conduct", "Payment").forEach { c ->
                        FilterChip(selected = compCategory == c, onClick = { compCategory = c }, label = { Text(c, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = compSubject, onValueChange = { compSubject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = compDesc, onValueChange = { compDesc = it }, label = { Text("Describe the issue") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onSubmitComplaint(bookings.firstOrNull()?.id, compCategory, compSubject, compDesc)
                        compSubject = ""
                        compDesc = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Complaint")
                }
                if (complaints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    complaints.forEach { cmp ->
                        Text("• [${cmp.status.uppercase()}] ${cmp.subject}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

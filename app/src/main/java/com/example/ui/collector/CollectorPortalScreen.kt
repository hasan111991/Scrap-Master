package com.example.ui.collector

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private enum class CollectorTab(val label: String) {
    POOL("Pickup Pool"),
    ACTIVE("Active"),
    EARNINGS("Earnings"),
    PROFILE("Profile")
}

@Composable
fun CollectorPortalScreen(
    viewModel: CollectionHubViewModel,
    collectorProfile: HubCollectorProfile?,
    openPool: List<HubPickupPoolItem>,
    assignedBookings: List<HubBooking>,
    commissions: List<HubCommissionRecord>,
    settlements: List<HubCommissionSettlement>,
    categories: List<HubScrapCategory>,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    if (collectorProfile == null) {
        CollectorRegistrationView(
            defaultName = viewModel.displayName,
            categories = categories.filter { it.active },
            isBusy = isBusy,
            onRegister = { fullName, shop, phone, city, areas, cats, vehicle, addr, cnic ->
                viewModel.registerCollector(fullName, shop, phone, city, areas, cats, vehicle, addr, cnic)
            },
            modifier = modifier
        )
        return
    }

    if (collectorProfile.approvalStatus != "approved") {
        CollectorApprovalGateView(
            profile = collectorProfile,
            modifier = modifier
        )
        return
    }

    var activeTab by remember { mutableStateOf(CollectorTab.POOL) }

    BackHandler(enabled = activeTab != CollectorTab.POOL) {
        activeTab = CollectorTab.POOL
    }

    val activeJobs = assignedBookings.filter { it.status !in listOf("completed", "cancelled") }
    val completedJobs = assignedBookings.filter { it.status == "completed" }

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
                    selected = activeTab == CollectorTab.POOL,
                    onClick = { activeTab = CollectorTab.POOL },
                    icon = { Icon(Icons.Default.Radar, contentDescription = "Pickup Pool") },
                    label = { Text("Pool (${openPool.size})") }
                )
                NavigationBarItem(
                    selected = activeTab == CollectorTab.ACTIVE,
                    onClick = { activeTab = CollectorTab.ACTIVE },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Active Pickups") },
                    label = { Text("Active (${activeJobs.size})") }
                )
                NavigationBarItem(
                    selected = activeTab == CollectorTab.EARNINGS,
                    onClick = { activeTab = CollectorTab.EARNINGS },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Earnings") },
                    label = { Text("Earnings") }
                )
                NavigationBarItem(
                    selected = activeTab == CollectorTab.PROFILE,
                    onClick = { activeTab = CollectorTab.PROFILE },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Online / Offline Status Bar
            Surface(
                color = if (collectorProfile.availabilityOnline) HubGreenContainer else Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = collectorProfile.shopOrBusinessName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HubGreenDark
                        )
                        Text(
                            text = if (collectorProfile.availabilityOnline) {
                                "ONLINE • Receiving ${collectorProfile.city} pickups within ${collectorProfile.radiusKm}km"
                            } else {
                                "OFFLINE • Toggle Online to receive nearby requests"
                            },
                            fontSize = 11.sp,
                            color = HubGreenPrimary
                        )
                    }
                    Switch(
                        checked = collectorProfile.availabilityOnline,
                        onCheckedChange = { viewModel.toggleCollectorOnline(it) },
                        modifier = Modifier.testTag("collector_online_switch")
                    )
                }
            }

            when (activeTab) {
                CollectorTab.POOL -> CollectorPoolTab(
                    isOnline = collectorProfile.availabilityOnline,
                    pool = openPool,
                    isBusy = isBusy,
                    onGoOnline = { viewModel.toggleCollectorOnline(true) },
                    onAccept = {
                        viewModel.acceptPoolBooking(it)
                        activeTab = CollectorTab.ACTIVE
                    }
                )

                CollectorTab.ACTIVE -> CollectorActiveJobsTab(
                    jobs = activeJobs,
                    onAdvance = { booking, status, otpVerified, weight, amount, note ->
                        viewModel.advanceCollectorWorkflow(booking, status, otpVerified, weight, amount, note)
                    },
                    onSubmitPaymentProof = { booking, method, amount, ref, note ->
                        viewModel.submitManualPaymentProof(booking, method, amount, ref, note)
                    }
                )

                CollectorTab.EARNINGS -> CollectorEarningsTab(
                    completedJobs = completedJobs,
                    commissions = commissions,
                    settlements = settlements
                )

                CollectorTab.PROFILE -> CollectorProfileTab(collectorProfile)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectorRegistrationView(
    defaultName: String,
    categories: List<HubScrapCategory>,
    isBusy: Boolean,
    onRegister: (
        fullName: String,
        shop: String,
        phone: String,
        city: String,
        areas: List<String>,
        cats: List<String>,
        vehicle: String,
        addr: String,
        cnic: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf(defaultName) }
    var shopName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Rawalpindi") }
    var selectedAreas by remember { mutableStateOf(setOf("Chaklala Scheme III", "Saddar")) }
    var selectedCats by remember { mutableStateOf(categories.take(6).map { it.name }.toSet()) }
    var vehicleType by remember { mutableStateOf(CollectionHubCatalog.defaultVehicleTypes.first()) }
    var address by remember { mutableStateOf("") }
    var cnicLast4 by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(HubBackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HubSectionCard(
                title = "Collector & Local Kabari Registration",
                subtitle = "Join Collection Hub across Rawalpindi & Islamabad. Admins review applications before going live."
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Kabari Shop / Business Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (e.g., 0300-1234567)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Operating City", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectionHubCatalog.supportedCities.forEach { c ->
                        FilterChip(
                            selected = city == c,
                            onClick = {
                                city = c
                                selectedAreas = (CollectionHubCatalog.areasByCity[c]?.take(2) ?: emptyList()).toSet()
                            },
                            label = { Text(c) }
                        )
                    }
                }
                Text("Sectors / Areas You Serve", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (CollectionHubCatalog.areasByCity[city] ?: emptyList()).forEach { area ->
                        val picked = selectedAreas.contains(area)
                        FilterChip(
                            selected = picked,
                            onClick = {
                                selectedAreas = if (picked && selectedAreas.size > 1) selectedAreas - area else selectedAreas + area
                            },
                            label = { Text(area, fontSize = 12.sp) }
                        )
                    }
                }
                Text("Scrap Categories Handled", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        val picked = selectedCats.contains(cat.name)
                        FilterChip(
                            selected = picked,
                            onClick = {
                                selectedCats = if (picked && selectedCats.size > 1) selectedCats - cat.name else selectedCats + cat.name
                            },
                            label = { Text(cat.name, fontSize = 12.sp) }
                        )
                    }
                }
                Text("Vehicle / Collection Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CollectionHubCatalog.defaultVehicleTypes.forEach { v ->
                        FilterChip(selected = vehicleType == v, onClick = { vehicleType = v }, label = { Text(v, fontSize = 12.sp) })
                    }
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop / Yard Address (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = cnicLast4,
                    onValueChange = { cnicLast4 = it.take(5) },
                    label = { Text("CNIC Last 4 Digits (for verification)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onRegister(
                            fullName,
                            shopName,
                            phone,
                            city,
                            selectedAreas.toList(),
                            selectedCats.toList(),
                            vehicleType,
                            address,
                            cnicLast4
                        )
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("collector_register_submit")
                ) {
                    Text("Submit Collector Application", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CollectorApprovalGateView(profile: HubCollectorProfile, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBackgroundLight)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val status = CollectorApprovalStatus.fromCode(profile.approvalStatus)
        HubSectionCard(
            title = "Application Status: ${status.label}",
            subtitle = "${profile.shopOrBusinessName} • ${profile.city}"
        ) {
            Text(
                text = when (status) {
                    CollectorApprovalStatus.PENDING_APPROVAL ->
                        "Your collector registration is awaiting manual verification by Collection Hub admins. Once approved, you can toggle Online and accept nearby pickups."
                    CollectorApprovalStatus.REJECTED ->
                        "Your application was not approved. Reason: ${profile.rejectionReason.ifBlank { "Please contact Collection Hub support." }}"
                    CollectorApprovalStatus.SUSPENDED ->
                        "Your collector account is temporarily suspended. Reason: ${profile.rejectionReason.ifBlank { "Contact support." }}"
                    CollectorApprovalStatus.APPROVED -> ""
                },
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun CollectorPoolTab(
    isOnline: Boolean,
    pool: List<HubPickupPoolItem>,
    isBusy: Boolean,
    onGoOnline: () -> Unit,
    onAccept: (HubPickupPoolItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isOnline) {
            item {
                HubEmptyState(
                    icon = Icons.Default.WifiOff,
                    title = "You Are Currently Offline",
                    subtitle = "Switch yourself Online above to view nearby pickup requests matching your areas and categories.",
                    actionLabel = "Go Online Now",
                    onAction = onGoOnline
                )
            }
        } else if (pool.isEmpty()) {
            item {
                HubEmptyState(
                    icon = Icons.Default.HourglassEmpty,
                    title = "No Open Pickups Right Now",
                    subtitle = "New requests in your covered Rawalpindi/Islamabad areas will appear here in real time."
                )
            }
        } else {
            items(pool, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().border(1.dp, HubBorderLight, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${item.area}, ${item.city}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = HubGreenPrimary)
                            Surface(color = HubAmberContainer, shape = RoundedCornerShape(999.dp)) {
                                Text("Approx. Area Only", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnHubAmberContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Text("Categories: ${item.categories.joinToString(", ")}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Est. Qty: ${item.estimatedQuantityLabel} • Slot: ${item.preferredDate} (${item.preferredTimeSlot})", fontSize = 12.sp, color = HubTextSecondaryLight)
                        Text("Full street address & phone unlock only after you accept.", fontSize = 11.sp, color = HubTextSecondaryLight)
                        Button(
                            onClick = { onAccept(item) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("accept_pool_${item.bookingId}")
                        ) {
                            Text("Accept Pickup (First-Accept Lock)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectorActiveJobsTab(
    jobs: List<HubBooking>,
    onAdvance: (HubBooking, String, Boolean, Double, Double, String) -> Unit,
    onSubmitPaymentProof: (HubBooking, String, Double, String, String) -> Unit
) {
    var otpTarget by remember { mutableStateOf<HubBooking?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var weightInput by remember { mutableStateOf("") }
    var finalAmountInput by remember { mutableStateOf("") }
    var collectorNotesInput by remember { mutableStateOf("") }

    var payTarget by remember { mutableStateOf<HubBooking?>(null) }
    var payMethod by remember { mutableStateOf("Cash") }
    var payRef by remember { mutableStateOf("") }
    var payNote by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (jobs.isEmpty()) {
            item {
                HubEmptyState(
                    icon = Icons.Default.LocalShipping,
                    title = "No Active Pickups",
                    subtitle = "Accepted pickups will appear here with full customer address, OTP verification, and weight entry."
                )
            }
        } else {
            items(jobs, key = { it.id }) { job ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().border(1.dp, HubBorderLight, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${job.id.take(10)}", fontWeight = FontWeight.ExtraBold)
                            HubBookingStatusBadge(job.status)
                        }
                        Text("Customer: ${job.customerName} • ${job.customerPhone}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Address: ${job.fullAddress}, ${job.area}, ${job.city}", fontSize = 13.sp)
                        Text("Items: ${job.itemsSummary} (Est. ${job.estimatedWeightKg} ${job.unitType})", fontSize = 12.sp, color = HubTextSecondaryLight)
                        Text("Preferred Payment: ${job.paymentMethod}", fontSize = 12.sp, color = HubGreenPrimary, fontWeight = FontWeight.SemiBold)

                        when (job.status) {
                            "assigned" -> {
                                Button(
                                    onClick = {
                                        onAdvance(job, "on_the_way", false, job.verifiedWeightKg, job.finalAmountPkr, job.collectorNotes)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Mark On The Way")
                                }
                            }
                            "on_the_way" -> {
                                Button(
                                    onClick = {
                                        onAdvance(job, "arrived", false, job.verifiedWeightKg, job.finalAmountPkr, job.collectorNotes)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Mark Arrived at Customer")
                                }
                            }
                            "arrived" -> {
                                Button(
                                    onClick = {
                                        otpTarget = job
                                        otpInput = ""
                                        otpError = null
                                        weightInput = job.estimatedWeightKg.toInt().toString()
                                        finalAmountInput = job.estimatedMinPkr.toInt().toString()
                                        collectorNotesInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verify OTP & Enter Verified Weight/Price")
                                }
                            }
                            "weight_price_confirmation" -> {
                                Surface(color = HubGreenSoftBg, shape = RoundedCornerShape(10.dp)) {
                                    Text(
                                        text = "Submitted ${job.verifiedWeightKg} KG • ${formatPkr(job.finalAmountPkr)}. Waiting for customer confirmation.",
                                        fontSize = 12.sp,
                                        color = HubGreenPrimary,
                                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        payTarget = job
                                        payMethod = job.paymentMethod
                                        payRef = ""
                                        payNote = "Paid ${formatPkr(job.finalAmountPkr)} manually"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Record Manual Payment Reference")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    otpTarget?.let { job ->
        AlertDialog(
            onDismissRequest = { otpTarget = null },
            title = { Text("OTP & Verified Weight") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("4-Digit Customer OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Verified Weight (KG)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = finalAmountInput,
                        onValueChange = { finalAmountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Agreed Final Amount (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = collectorNotesInput,
                        onValueChange = { collectorNotesInput = it },
                        label = { Text("Collector Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (otpError != null) {
                        Text(otpError.orEmpty(), color = ErrorRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (otpInput.trim() != job.otpCode) {
                            otpError = "OTP does not match customer's 4-digit code."
                            return@Button
                        }
                        val w = weightInput.toDoubleOrNull() ?: 0.0
                        val amt = finalAmountInput.toDoubleOrNull() ?: 0.0
                        if (w <= 0.0 || amt <= 0.0) {
                            otpError = "Enter verified weight and final PKR amount > 0."
                            return@Button
                        }
                        onAdvance(job, "weight_price_confirmation", true, w, amt, collectorNotesInput)
                        otpTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                ) { Text("Submit to Customer") }
            },
            dismissButton = {
                TextButton(onClick = { otpTarget = null }) { Text("Cancel") }
            }
        )
    }

    payTarget?.let { job ->
        AlertDialog(
            onDismissRequest = { payTarget = null },
            title = { Text("Submit Manual Payment Reference") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cash", "Easypaisa", "Bank Transfer").forEach { m ->
                            FilterChip(selected = payMethod == m, onClick = { payMethod = m }, label = { Text(m, fontSize = 11.sp) })
                        }
                    }
                    OutlinedTextField(
                        value = payRef,
                        onValueChange = { payRef = it },
                        label = { Text("Transaction ID / Receipt Ref (optional for Cash)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = payNote,
                        onValueChange = { payNote = it },
                        label = { Text("Payment Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Note: Manual payment proof remains pending admin verification.", fontSize = 11.sp, color = HubTextSecondaryLight)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitPaymentProof(job, payMethod, job.finalAmountPkr, payRef, payNote)
                        payTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                ) { Text("Record Proof") }
            },
            dismissButton = {
                TextButton(onClick = { payTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CollectorEarningsTab(
    completedJobs: List<HubBooking>,
    commissions: List<HubCommissionRecord>,
    settlements: List<HubCommissionSettlement>
) {
    val totalGross = completedJobs.sumOf { it.finalAmountPkr }
    val unpaidCommission = commissions.filter { it.status != "settled" }.sumOf { it.commissionAmountPkr }
    val settledCommission = commissions.filter { it.status == "settled" }.sumOf { it.commissionAmountPkr }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = HubGreenPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Completed Volume", color = HubGreenContainer, fontSize = 11.sp)
                        Text(formatPkr(totalGross), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("${completedJobs.size} Pickups", color = HubGreenContainer, fontSize = 11.sp)
                    }
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f).border(1.dp, HubBorderLight, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Commission Due", color = HubTextSecondaryLight, fontSize = 11.sp)
                        Text(formatPkr(unpaidCommission), color = WarningOrange, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Settled: ${formatPkr(settledCommission)}", color = HubTextSecondaryLight, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            HubSectionCard(title = "Commission Ledger (${commissions.size})") {
                if (commissions.isEmpty()) {
                    Text("No commission entries yet.", fontSize = 13.sp, color = HubTextSecondaryLight)
                } else {
                    commissions.forEach { comm ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Booking #${comm.bookingId.take(8)} (${comm.ruleType})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Pickup Amt: ${formatPkr(comm.bookingAmountPkr)}", fontSize = 11.sp, color = HubTextSecondaryLight)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatPkr(comm.commissionAmountPkr), fontWeight = FontWeight.Bold, color = HubGreenPrimary)
                                Text(comm.status.uppercase(), fontSize = 10.sp, color = if (comm.status == "settled") SuccessGreen else WarningOrange)
                            }
                        }
                        HorizontalDivider(color = HubBorderLight)
                    }
                }
            }
        }

        if (settlements.isNotEmpty()) {
            item {
                HubSectionCard(title = "Manual Settlement History") {
                    settlements.forEach { st ->
                        Text("${st.periodLabel} • ${formatPkr(st.totalAmountPkr)} (${st.paymentMethod})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Ref: ${st.referenceNote}", fontSize = 12.sp, color = HubTextSecondaryLight)
                        HorizontalDivider(color = HubBorderLight)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectorProfileTab(profile: HubCollectorProfile) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HubSectionCard(title = profile.shopOrBusinessName, subtitle = "${profile.fullName} • ${profile.city}") {
                Text("Approval Status: ${profile.approvalStatus.uppercase()}", fontWeight = FontWeight.Bold, color = HubGreenPrimary)
                Text("Phone: ${profile.phone}")
                Text("Vehicle: ${profile.vehicleType}")
                Text("Service Areas: ${profile.serviceAreas.joinToString(", ")}")
                Text("Accepted Categories: ${profile.acceptedCategories.joinToString(", ")}")
                Text("Matching Radius: ${profile.radiusKm} KM")
            }
        }
    }
}

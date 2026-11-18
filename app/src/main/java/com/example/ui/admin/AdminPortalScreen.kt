package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import com.example.ui.CollectionHubAdminState
import com.example.ui.CollectionHubViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class AdminModuleTab(val label: String) {
    OVERVIEW("1. Overview & Pool"),
    BOOKINGS("2. All Bookings"),
    COLLECTORS("3. Collectors & Approvals"),
    CUSTOMERS("4. Customers"),
    CATEGORIES_PRICES("5. Categories & Rates"),
    PAYMENTS_COMMISSIONS("6. Payments & Commissions"),
    COMPLAINTS("7. Complaints"),
    AREAS_SETTINGS("8. Areas & Settings"),
    REPORTS_LOGS("9. Reports & Audit")
}

/**
 * Private /hub-admin Portal.
 * Strictly gated by `isVerifiedAdmin`. Unlisted on public UI and non-admin portals.
 */
@Composable
fun AdminPortalScreen(
    isVerifiedAdmin: Boolean,
    viewModel: CollectionHubViewModel,
    adminState: CollectionHubAdminState,
    categories: List<HubScrapCategory>,
    prices: List<HubScrapPrice>,
    serviceAreas: List<HubServiceArea>,
    globalSettings: HubAppSettings,
    modifier: Modifier = Modifier
) {
    if (!isVerifiedAdmin) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(HubBackgroundLight)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            HubEmptyState(
                icon = Icons.Default.Lock,
                title = "Restricted Admin Portal",
                subtitle = "Access to /hub-admin is restricted strictly to verified Collection Hub administrators."
            )
        }
        return
    }

    var activeTab by remember { mutableStateOf(AdminModuleTab.OVERVIEW) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HubBackgroundLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminModuleTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeTab) {
            AdminModuleTab.OVERVIEW -> AdminOverviewAndPoolModule(
                adminState = adminState,
                onBootstrap = { viewModel.adminBootstrapData() }
            )
            AdminModuleTab.BOOKINGS -> AdminBookingsModule(
                bookings = adminState.bookings,
                approvedCollectors = adminState.collectors.filter { it.approvalStatus == "approved" },
                onAssignCollector = { bookingId, col -> viewModel.adminAssignCollector(bookingId, col) },
                onCancelBooking = { bookingId, reason -> viewModel.adminCancelBooking(bookingId, reason) }
            )
            AdminModuleTab.COLLECTORS -> AdminCollectorsModule(
                collectors = adminState.collectors,
                onUpdateApproval = { id, status, reason ->
                    viewModel.adminUpdateCollectorApproval(id, status, reason)
                }
            )
            AdminModuleTab.CUSTOMERS -> AdminCustomersModule(adminState.customers)
            AdminModuleTab.CATEGORIES_PRICES -> AdminCategoriesAndPricesModule(
                categories = categories,
                prices = prices,
                onSaveCategory = { viewModel.adminSaveCategory(it) },
                onSavePrice = { viewModel.adminSavePrice(it) }
            )
            AdminModuleTab.PAYMENTS_COMMISSIONS -> AdminPaymentsAndCommissionsModule(
                payments = adminState.payments,
                commissions = adminState.commissions,
                settlements = adminState.settlements,
                onVerifyPayment = { id, status -> viewModel.adminVerifyPayment(id, status) },
                onSettleCollector = { colId, colName, list, method, note ->
                    viewModel.adminSettleCollectorCommissions(colId, colName, "Manual Settlement", list, method, note)
                }
            )
            AdminModuleTab.COMPLAINTS -> AdminComplaintsModule(
                complaints = adminState.complaints,
                onUpdateComplaint = { id, status, notes -> viewModel.adminUpdateComplaint(id, status, notes) }
            )
            AdminModuleTab.AREAS_SETTINGS -> AdminAreasAndSettingsModule(
                serviceAreas = serviceAreas,
                settings = globalSettings,
                onAddArea = { city, area, radius -> viewModel.adminSaveServiceArea(city, area, radius) },
                onSaveSettings = { viewModel.adminSaveGlobalSettings(it) }
            )
            AdminModuleTab.REPORTS_LOGS -> AdminReportsAndLogsModule(adminState = adminState)
        }
    }
}

@Composable
private fun AdminOverviewAndPoolModule(
    adminState: CollectionHubAdminState,
    onBootstrap: () -> Unit
) {
    val pendingApprovals = adminState.collectors.count { it.approvalStatus == "pending_approval" }
    val searchingPool = adminState.poolItems.filter { it.status == "searching" }
    val completedAmount = adminState.bookings.filter { it.status == "completed" }.sumOf { it.finalAmountPkr }
    val unpaidCommissions = adminState.commissions.filter { it.status != "settled" }.sumOf { it.commissionAmountPkr }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HubSectionCard(
                title = "Admin Dashboard KPIs",
                subtitle = "Rawalpindi & Islamabad Launch Control Center"
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiBox("Total Bookings", "${adminState.bookings.size}", Modifier.weight(1f))
                    KpiBox("Pending Collectors", "$pendingApprovals", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiBox("Completed Vol.", formatPkr(completedAmount), Modifier.weight(1f))
                    KpiBox("Unpaid Comm.", formatPkr(unpaidCommissions), Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = onBootstrap,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seed / Verify Initial Launch Categories, Rates & Areas")
                }
            }
        }

        item {
            HubSectionCard(
                title = "Live Open Pickup Pool (${searchingPool.size})",
                subtitle = "Requests currently searching nearby collectors"
            ) {
                if (searchingPool.isEmpty()) {
                    Text("No searching requests in the pickup pool.", fontSize = 13.sp, color = HubTextSecondaryLight)
                } else {
                    searchingPool.forEach { item ->
                        Text("${item.area}, ${item.city} • ${item.categories.joinToString(", ")} (${item.estimatedQuantityLabel})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        HorizontalDivider(color = HubBorderLight)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = HubGreenSoftBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, HubGreenContainer, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = HubTextSecondaryLight)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = HubGreenPrimary)
        }
    }
}

@Composable
private fun AdminBookingsModule(
    bookings: List<HubBooking>,
    approvedCollectors: List<HubCollectorProfile>,
    onAssignCollector: (String, HubCollectorProfile) -> Unit,
    onCancelBooking: (String, String) -> Unit
) {
    var filterStatus by remember { mutableStateOf("all") }
    var assignTargetId by remember { mutableStateOf<String?>(null) }

    val filtered = if (filterStatus == "all") bookings else bookings.filter { it.status == filterStatus }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("all", "searching", "assigned", "on_the_way", "arrived", "weight_price_confirmation", "completed", "cancelled").forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = st },
                        label = { Text(st.replace("_", " "), fontSize = 11.sp) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { booking ->
            HubSectionCard(
                title = "#${booking.id.take(10)} • ${booking.customerName}",
                subtitle = "${booking.area}, ${booking.city} • ${booking.preferredDate}"
            ) {
                HubBookingStatusBadge(booking.status)
                Text("Address: ${booking.fullAddress} (${booking.customerPhone})", fontSize = 12.sp)
                Text("Items: ${booking.itemsSummary}", fontSize = 12.sp, color = HubTextSecondaryLight)
                if (booking.assignedCollectorName.isNotBlank()) {
                    Text("Collector: ${booking.assignedCollectorName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HubGreenPrimary)
                }
                if (booking.status !in listOf("completed", "cancelled")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { assignTargetId = booking.id },
                            colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (booking.assignedCollectorId.isBlank()) "Assign" else "Reassign", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onCancelBooking(booking.id, "Cancelled by Hub Admin") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    assignTargetId?.let { targetBookingId ->
        AlertDialog(
            onDismissRequest = { assignTargetId = null },
            title = { Text("Assign Approved Collector") },
            text = {
                if (approvedCollectors.isEmpty()) {
                    Text("No approved collectors available yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        approvedCollectors.forEach { col ->
                            OutlinedButton(
                                onClick = {
                                    onAssignCollector(targetBookingId, col)
                                    assignTargetId = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${col.fullName} (${col.shopOrBusinessName}) • ${col.city}")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { assignTargetId = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AdminCollectorsModule(
    collectors: List<HubCollectorProfile>,
    onUpdateApproval: (String, String, String) -> Unit
) {
    var rejectTargetId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (collectors.isEmpty()) {
            item {
                Text("No collector applications yet.", fontSize = 14.sp, color = HubTextSecondaryLight)
            }
        }
        items(collectors, key = { it.id }) { col ->
            HubSectionCard(
                title = "${col.shopOrBusinessName} (${col.fullName})",
                subtitle = "${col.city} • ${col.phone} • Status: ${col.approvalStatus.uppercase()}"
            ) {
                Text("Areas: ${col.serviceAreas.joinToString(", ")}", fontSize = 12.sp)
                Text("Categories: ${col.acceptedCategories.joinToString(", ")}", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (col.approvalStatus != "approved") {
                        Button(
                            onClick = { onUpdateApproval(col.id, "approved", "") },
                            colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                            modifier = Modifier.weight(1f)
                        ) { Text("Approve") }
                    }
                    if (col.approvalStatus != "rejected") {
                        OutlinedButton(
                            onClick = { rejectTargetId = col.id },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject / Suspend") }
                    }
                }
            }
        }
    }

    rejectTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { rejectTargetId = null },
            title = { Text("Reject or Suspend Collector") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateApproval(id, "rejected", rejectReason.ifBlank { "Incomplete verification" })
                        rejectTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdateApproval(id, "suspended", rejectReason.ifBlank { "Suspended by admin" })
                    rejectTargetId = null
                }) { Text("Suspend Instead") }
            }
        )
    }
}

@Composable
private fun AdminCustomersModule(customers: List<HubCustomerProfile>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(customers, key = { it.id }) { cust ->
            HubSectionCard(
                title = cust.name.ifBlank { "Customer" },
                subtitle = "${cust.city} • ${cust.area} • ${cust.email}"
            ) {
                Text("Phone: ${cust.phone.ifBlank { "Not set" }} • Address: ${cust.defaultAddress.ifBlank { "Not set" }}", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AdminCategoriesAndPricesModule(
    categories: List<HubScrapCategory>,
    prices: List<HubScrapPrice>,
    onSaveCategory: (HubScrapCategory) -> Unit,
    onSavePrice: (HubScrapPrice) -> Unit
) {
    var editingPrice by remember { mutableStateOf<HubScrapPrice?>(null) }
    var minInput by remember { mutableStateOf("") }
    var maxInput by remember { mutableStateOf("") }

    var newCatName by remember { mutableStateOf("") }
    var newCatUrdu by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            HubSectionCard(title = "Add Scrap Category") {
                OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("Category Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newCatUrdu, onValueChange = { newCatUrdu = it }, label = { Text("Urdu Name") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onSaveCategory(
                                HubScrapCategory(
                                    name = newCatName,
                                    urduName = newCatUrdu,
                                    description = newCatName,
                                    sortOrder = (categories.size + 1).toLong()
                                )
                            )
                            newCatName = ""
                            newCatUrdu = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                ) { Text("Save Category") }
            }
        }

        item {
            HubSectionCard(
                title = "Dynamic Indicative Scrap Prices",
                subtitle = "All prices remain marked indicative/estimated only."
            ) {
                prices.forEach { price ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${price.categoryName} (${price.city})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Est. Rs. ${price.minEstimatedPkr.toInt()}-${price.maxEstimatedPkr.toInt()} / ${price.unit}", fontSize = 12.sp, color = HubGreenPrimary)
                        }
                        TextButton(onClick = {
                            editingPrice = price
                            minInput = price.minEstimatedPkr.toInt().toString()
                            maxInput = price.maxEstimatedPkr.toInt().toString()
                        }) { Text("Edit") }
                    }
                    HorizontalDivider(color = HubBorderLight)
                }
            }
        }
    }

    editingPrice?.let { target ->
        AlertDialog(
            onDismissRequest = { editingPrice = null },
            title = { Text("Update Indicative Rate: ${target.categoryName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { minInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Min Estimated PKR / ${target.unit}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = maxInput,
                        onValueChange = { maxInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Estimated PKR / ${target.unit}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minV = minInput.toDoubleOrNull() ?: target.minEstimatedPkr
                        val maxV = maxInput.toDoubleOrNull() ?: target.maxEstimatedPkr
                        onSavePrice(target.copy(minEstimatedPkr = minV, maxEstimatedPkr = maxV))
                        editingPrice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                ) { Text("Save Rate") }
            },
            dismissButton = {
                TextButton(onClick = { editingPrice = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminPaymentsAndCommissionsModule(
    payments: List<HubManualPayment>,
    commissions: List<HubCommissionRecord>,
    settlements: List<HubCommissionSettlement>,
    onVerifyPayment: (String, String) -> Unit,
    onSettleCollector: (String, String, List<HubCommissionRecord>, String, String) -> Unit
) {
    val unpaidByCollector = commissions.filter { it.status != "settled" }.groupBy { it.collectorId }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            HubSectionCard(
                title = "Manual Payments (${payments.size})",
                subtitle = "Screenshots/notes never auto-confirm without admin verification."
            ) {
                if (payments.isEmpty()) {
                    Text("No manual payments recorded yet.", fontSize = 13.sp, color = HubTextSecondaryLight)
                } else {
                    payments.forEach { pay ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Booking #${pay.bookingId.take(8)} • ${pay.method} • ${formatPkr(pay.amountPkr)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Status: ${pay.verificationStatus} • Ref: ${pay.referenceNumber.ifBlank { pay.proofNote }}", fontSize = 12.sp, color = HubTextSecondaryLight)
                            if (pay.verificationStatus == "pending_verification") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onVerifyPayment(pay.id, "verified") }, colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)) {
                                        Text("Verify", fontSize = 12.sp)
                                    }
                                    OutlinedButton(onClick = { onVerifyPayment(pay.id, "rejected") }) {
                                        Text("Reject", fontSize = 12.sp)
                                    }
                                }
                            }
                            HorizontalDivider(color = HubBorderLight)
                        }
                    }
                }
            }
        }

        item {
            HubSectionCard(
                title = "Collector Commission Settlement Queue",
                subtitle = "Settle unpaid commission balances manually"
            ) {
                if (unpaidByCollector.isEmpty()) {
                    Text("All commissions settled.", fontSize = 13.sp, color = HubTextSecondaryLight)
                } else {
                    unpaidByCollector.forEach { (colId, records) ->
                        val colName = records.firstOrNull()?.collectorName ?: colId
                        val due = records.sumOf { it.commissionAmountPkr }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(colName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${records.size} unpaid bookings • ${formatPkr(due)}", fontSize = 12.sp, color = WarningOrange)
                            }
                            Button(
                                onClick = { onSettleCollector(colId, colName, records, "Cash", "Admin manual settlement") },
                                colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                            ) {
                                Text("Settle", fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = HubBorderLight)
                    }
                }
            }
        }

        if (settlements.isNotEmpty()) {
            item {
                HubSectionCard(title = "Completed Settlements (${settlements.size})") {
                    settlements.forEach { st ->
                        Text("${st.collectorName} • ${formatPkr(st.totalAmountPkr)} (${st.paymentMethod})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminComplaintsModule(
    complaints: List<HubComplaint>,
    onUpdateComplaint: (String, String, String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (complaints.isEmpty()) {
            item { Text("No open or past complaints.", fontSize = 14.sp, color = HubTextSecondaryLight) }
        }
        items(complaints, key = { it.id }) { cmp ->
            HubSectionCard(title = cmp.subject, subtitle = "${cmp.userName} (${cmp.userRole}) • ${cmp.status.uppercase()}") {
                Text(cmp.description, fontSize = 13.sp)
                if (cmp.status != "resolved") {
                    Button(
                        onClick = { onUpdateComplaint(cmp.id, "resolved", "Resolved by Collection Hub Admin") },
                        colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                    ) { Text("Mark Resolved") }
                }
            }
        }
    }
}

@Composable
private fun AdminAreasAndSettingsModule(
    serviceAreas: List<HubServiceArea>,
    settings: HubAppSettings,
    onAddArea: (String, String, Double) -> Unit,
    onSaveSettings: (HubAppSettings) -> Unit
) {
    var city by remember { mutableStateOf("Rawalpindi") }
    var areaName by remember { mutableStateOf("") }
    var radiusText by remember(settings) { mutableStateOf(settings.defaultMatchingRadiusKm.toString()) }
    var ruleType by remember(settings) { mutableStateOf(settings.commissionRuleType) }
    var pctText by remember(settings) { mutableStateOf(settings.defaultCommissionPercent.toString()) }
    var fixedText by remember(settings) { mutableStateOf(settings.defaultFixedCommissionPkr.toString()) }
    var supportPhone by remember(settings) { mutableStateOf(settings.supportPhone) }
    var supportEmail by remember(settings) { mutableStateOf(settings.supportEmail) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            HubSectionCard(title = "Global Matching Radius & Commission Settings") {
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Default Matching Radius (KM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Commission Rule Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("percentage", "fixed", "category").forEach { r ->
                        FilterChip(selected = ruleType == r, onClick = { ruleType = r }, label = { Text(r) })
                    }
                }
                OutlinedTextField(
                    value = pctText,
                    onValueChange = { pctText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Default Commission (%)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fixedText,
                    onValueChange = { fixedText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Fixed Fee Commission (PKR)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = supportPhone, onValueChange = { supportPhone = it }, label = { Text("Support Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onSaveSettings(
                            settings.copy(
                                defaultMatchingRadiusKm = radiusText.toDoubleOrNull() ?: 5.0,
                                commissionRuleType = ruleType,
                                defaultCommissionPercent = pctText.toDoubleOrNull() ?: 10.0,
                                defaultFixedCommissionPkr = fixedText.toDoubleOrNull() ?: 150.0,
                                supportPhone = supportPhone,
                                supportEmail = supportEmail
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Platform Settings") }
            }
        }

        item {
            HubSectionCard(title = "Service Areas (${serviceAreas.size})") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectionHubCatalog.supportedCities.forEach { c ->
                        FilterChip(selected = city == c, onClick = { city = c }, label = { Text(c) })
                    }
                }
                OutlinedTextField(value = areaName, onValueChange = { areaName = it }, label = { Text("Add Sector / Area Name") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (areaName.isNotBlank()) {
                            onAddArea(city, areaName, radiusText.toDoubleOrNull() ?: 5.0)
                            areaName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary)
                ) { Text("Add Service Area") }

                serviceAreas.forEach { area ->
                    Text("• ${area.city}: ${area.areaName} (${area.matchingRadiusKm} km)", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminReportsAndLogsModule(adminState: CollectionHubAdminState) {
    val completed = adminState.bookings.filter { it.status == "completed" }
    val totalKg = completed.sumOf { it.verifiedWeightKg }
    val totalPkr = completed.sumOf { it.finalAmountPkr }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            HubSectionCard(title = "Marketplace Summary Report") {
                Text("Completed Pickups: ${completed.size}")
                Text("Total Verified Weight Collected: $totalKg KG")
                Text("Total Customer Payout Volume: ${formatPkr(totalPkr)}")
                Text("Registered Customers: ${adminState.customers.size}")
                Text("Approved Collectors: ${adminState.collectors.count { it.approvalStatus == "approved" }}")
            }
        }

        item {
            HubSectionCard(title = "Immutable Audit Logs (${adminState.auditLogs.size})") {
                if (adminState.auditLogs.isEmpty()) {
                    Text("No audit records logged yet.", fontSize = 13.sp, color = HubTextSecondaryLight)
                } else {
                    adminState.auditLogs.take(30).forEach { log ->
                        Text("${log.action} • ${log.entity} (${log.entityId.take(10)})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${log.actorRole} • ${log.details}", fontSize = 11.sp, color = HubTextSecondaryLight)
                        HorizontalDivider(color = HubBorderLight)
                    }
                }
            }
        }
    }
}

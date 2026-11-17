package com.example.ui.portals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.PlatformRepository
import com.example.ui.PlatformSyncUiState
import com.example.ui.PlatformSyncViewModel
import com.example.ui.components.PkrText
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

enum class ExtendedPortalTab(val title: String, val urduTitle: String) {
    OPERATIONS("Operations", "آپریشنز"),
    FINANCE("Finance", "مالیات"),
    PRICING("Pricing", "نرخ انجن"),
    SUPPORT("Support", "کسٹمر سپورٹ"),
    FRANCHISE("Cities", "شہر و فرنچائز"),
    SUPER_ADMIN("Governance", "گورننس / آر بی اے سی")
}

@Composable
fun ExtendedPortalsScreen(
    initialTab: ExtendedPortalTab,
    isUrdu: Boolean,
    syncViewModel: PlatformSyncViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val syncState by syncViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = ExtendedPortalTab.entries.indexOf(selectedTab),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldGreenPrimary
        ) {
            ExtendedPortalTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Tab(
                    selected = selected,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = if (isUrdu) tab.urduTitle else tab.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("extended_tab_${tab.name}")
                )
            }
        }

        when (selectedTab) {
            ExtendedPortalTab.OPERATIONS -> OperationsPortalView(isUrdu = isUrdu)
            ExtendedPortalTab.FINANCE -> FinancePortalView(isUrdu = isUrdu, syncState = syncState)
            ExtendedPortalTab.PRICING -> PricingManagementPortalView(isUrdu = isUrdu, syncState = syncState)
            ExtendedPortalTab.SUPPORT -> CustomerSupportPortalView(
                isUrdu = isUrdu,
                syncState = syncState,
                onSubmitTicket = { cat, subj, msg ->
                    syncViewModel.submitSupportTicket(cat, subj, msg)
                    PlatformRepository.createSupportTicket(cat, subj, msg)
                }
            )
            ExtendedPortalTab.FRANCHISE -> FranchiseCitiesPortalView(isUrdu = isUrdu)
            ExtendedPortalTab.SUPER_ADMIN -> SuperAdminGovernanceView(
                isUrdu = isUrdu,
                onBackToHome = onBackToHome
            )
        }
    }
}

@Composable
private fun OperationsPortalView(isUrdu: Boolean) {
    val pickups by PlatformRepository.pickups.collectAsState()
    val zones = listOf(
        Triple("Rawalpindi • Chaklala Scheme III", "6 Active Collectors", "98.4% SLA"),
        Triple("Rawalpindi • Bahria Town Phase 1-8", "9 Active Collectors", "97.1% SLA"),
        Triple("Islamabad • Sectors F-6, F-7, F-10", "5 Active Collectors", "99.0% SLA"),
        Triple("Islamabad • I-9 / I-10 Industrial Hub", "4 Hub Trucks", "96.5% SLA"),
        Triple("Karachi • DHA / Korangi Corridor", "14 Active Collectors", "95.8% SLA"),
        Triple("Lahore • Gulberg / Sundar Estate", "11 Active Collectors", "96.9% SLA")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isUrdu) "لائیو آپریشنز و زون ریدار" else "Operations & Dispatch Command",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Rawalpindi & Islamabad Launch Grid + National Hub Corridors",
                        color = EmeraldContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(zones) { (zoneName, fleet, sla) ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(zoneName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(fleet, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = EmeraldContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = sla,
                            color = OnEmeraldContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isUrdu) "پک اپ ڈسپیچ قطار" else "Active Dispatch Queue (${pickups.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        items(pickups) { pickup ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("#${pickup.id} • ${pickup.city} (${pickup.zone})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${pickup.customerName} -> ${pickup.assignedCollectorName ?: "Pending"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(status = pickup.status, isUrdu = isUrdu)
                }
            }
        }
    }
}

@Composable
private fun FinancePortalView(isUrdu: Boolean, syncState: PlatformSyncUiState) {
    val localLedger by PlatformRepository.ledgerEntries.collectAsState()
    val revenue = 2480000.0
    val purchaseCost = 1790000.0
    val collectionCost = 145000.0
    val sortingCost = 68000.0
    val commissionPaid = 112000.0
    val netProfit = revenue - purchaseCost - collectionCost - sortingCost - commissionPaid

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isUrdu) "فنانس اور یونٹ اکنامکس (PKR)" else "Finance & Unit Economics Engine (PKR)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Gross B2B Revenue", color = EmeraldContainer, fontSize = 11.sp)
                            Text(formatPkr(revenue), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Operating Profit", color = EmeraldContainer, fontSize = 11.sp)
                            Text(formatPkr(netProfit), color = AmberGoldSecondary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Text(
                        text = "Scrap Purchase: ${formatPkr(purchaseCost)} • Collection: ${formatPkr(collectionCost)} • Hub Sorting: ${formatPkr(sortingCost)} • Collector Comm: ${formatPkr(commissionPaid)}",
                        color = EmeraldContainer,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "Cloud Wallet Ledger (${syncState.walletLedger.size} Firestore • ${localLedger.size} Local)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        items(syncState.walletLedger) { entry ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${entry.category} • ${entry.idempotencyKey}", fontSize = 10.sp, color = Color.Gray)
                    }
                    PkrText(amount = entry.amount)
                }
            }
        }

        items(localLedger) { entry ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${entry.role.label} • ${entry.idempotencyKey}", fontSize = 10.sp, color = Color.Gray)
                    }
                    PkrText(amount = entry.amount)
                }
            }
        }
    }
}

@Composable
private fun PricingManagementPortalView(isUrdu: Boolean, syncState: PlatformSyncUiState) {
    val rules by PlatformRepository.pricingRules.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AmberGoldContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isUrdu) "پرائسنگ انجن کا فارمولا" else "Verified Pricing Guardrails Formula",
                        fontWeight = FontWeight.Bold,
                        color = OnAmberGoldContainer
                    )
                    Text(
                        text = "Customer Rate = clamp(Market Rate - Collection Cost - Margin%, minPrice, maxPrice). Final payout strictly requires verified weight & grade.",
                        fontSize = 11.sp,
                        color = OnAmberGoldContainer
                    )
                }
            }
        }

        if (syncState.pricingRules.isNotEmpty()) {
            item {
                Text("Live Firestore Pricing Rules (${syncState.pricingRules.size})", fontWeight = FontWeight.Bold)
            }
            items(syncState.pricingRules) { rule ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${rule.materialName} (${rule.grade})", fontWeight = FontWeight.Bold)
                            Text("${rule.city} • ${rule.zone}", fontSize = 11.sp, color = Color.Gray)
                        }
                        PkrText(amount = rule.marketRate)
                    }
                }
            }
        }

        item {
            Text(
                text = if (isUrdu) "علاقائی نرخ قواعد" else "Active Regional Rate Rules",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        items(rules) { rule ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${rule.materialName} • ${rule.city}", fontWeight = FontWeight.Bold)
                        Text("Margin: ${rule.buyingMargin}%", fontSize = 12.sp, color = EmeraldGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text("Zone: ${rule.zone} • Bounds: ${formatPkr(rule.minPrice)} – ${formatPkr(rule.maxPrice)}", fontSize = 11.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Calculated Customer Rate:", fontSize = 12.sp)
                        PkrText(amount = rule.calculateCustomerRate())
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerSupportPortalView(
    isUrdu: Boolean,
    syncState: PlatformSyncUiState,
    onSubmitTicket: (String, String, String) -> Unit
) {
    val localTickets by PlatformRepository.supportTickets.collectAsState()
    var category by remember { mutableStateOf("WEIGHT") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isUrdu) "نیا سپورٹ ٹکٹ بنائیں (فائر اسٹور)" else "Open Customer Support Ticket",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("WEIGHT", "PAYMENT", "COLLECTOR", "OTHER").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_subject_input")
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Describe your issue (weight dispute, payout delay, address)") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_message_input")
                    )
                    Button(
                        onClick = {
                            onSubmitTicket(category, subject, message)
                            subject = ""
                            message = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_support_ticket_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUrdu) "ٹکٹ جمع کرائیں" else "Submit Ticket to Support")
                    }
                }
            }
        }

        if (syncState.supportTickets.isNotEmpty()) {
            item {
                Text("Your Cloud Support Tickets (${syncState.supportTickets.size})", fontWeight = FontWeight.Bold)
            }
            items(syncState.supportTickets) { t ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.subject, fontWeight = FontWeight.Bold)
                            Surface(color = EmeraldContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(t.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnEmeraldContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(t.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(localTickets) { t ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${t.id} • ${t.subject}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(t.status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreenPrimary)
                    }
                    Text(t.message, fontSize = 12.sp)
                    t.resolutionNotes?.let {
                        Text("Resolution: $it", fontSize = 11.sp, color = EmeraldGreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FranchiseCitiesPortalView(isUrdu: Boolean) {
    val cities = listOf(
        Triple("Rawalpindi (Launch City)", "Chaklala, Saddar, Bahria Town, Satellite Town", "ACTIVE • 18 Collectors"),
        Triple("Islamabad (Launch City)", "Sectors F, G, H, I, DHA & Bahria Enclave", "ACTIVE • 14 Collectors"),
        Triple("Lahore Hub", "Gulberg, Johar Town, Sundar Industrial Estate", "ACTIVE • 22 Collectors"),
        Triple("Karachi Metro", "DHA, Clifton, Korangi Industrial, SITE", "ACTIVE • 31 Collectors"),
        Triple("Faisalabad", "Peoples Colony & Textile Industrial Belt", "ONBOARDING • Q4"),
        Triple("Multan & Peshawar", "Regional Hub Partner Evaluation", "PLANNED • Expansion")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isUrdu) "پاکستان سٹی اور فرنچائز نیٹ ورک" else "Pakistan City & Franchise Operations",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        items(cities) { (city, areas, status) ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(city, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreenPrimary)
                    }
                    Text(areas, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SuperAdminGovernanceView(isUrdu: Boolean, onBackToHome: () -> Unit) {
    val auditLogs by PlatformRepository.auditLogs.collectAsState()
    val matrix = listOf(
        "Customer" to "Own Profile, Own Pickups, Own Wallet Ledger, Support Tickets",
        "Collector (Kabariya)" to "Assigned Jobs Only (No PII leak), OTP & Scale Weighing",
        "Collection Hub" to "Inbound Batches, Scale Variance Check, Warehouse Inventory",
        "Recycler / B2B Buyer" to "Marketplace Lots, Sealed Bidding, Purchase Orders",
        "Corporate Client" to "Multi-branch Scheduling, ESG Waste Diversion Certificates",
        "Finance & Pricing" to "PKR Margin Formulas, Double-Entry Ledger, Payout Approvals",
        "Super Admin" to "Zero-Trust RBAC Governance, Immutable Audit Trail, Fraud Radar"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Zero-Trust Firestore RBAC Matrix", fontWeight = FontWeight.Bold, color = OnEmeraldContainer)
                    Text(
                        text = "Enforced server-side via hardened firestore.rules + Firebase App Check + Credential Manager Google Sign-In.",
                        fontSize = 11.sp,
                        color = OnEmeraldContainer
                    )
                }
            }
        }

        items(matrix) { (role, perms) ->
            Card(shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(role, fontWeight = FontWeight.Bold, color = EmeraldGreenPrimary, fontSize = 13.sp)
                    Text(perms, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Text(
                text = if (isUrdu) "حالیہ آڈٹ لاگز" else "Recent Immutable Audit Logs (${auditLogs.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        items(auditLogs.take(5)) { log ->
            Card(shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${log.action} • ${log.entityId}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("extended_portal_home_btn")
            ) {
                Text("Return to Primary Portal Home")
            }
        }
    }
}

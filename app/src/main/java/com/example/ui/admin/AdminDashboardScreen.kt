package com.example.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PricingRule
import com.example.service.PlatformRepository
import com.example.ui.components.PkrText
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "Overview" to "جائزہ",
        "Pricing Engine" to "نرخ انجن",
        "AI Fraud & Quality" to "اے آئی سیکیورٹی",
        "Eid Campaign" to "عید مہم",
        "Financial Ledger" to "مالیاتی لیجر",
        "Audit Logs" to "آڈٹ لاگز"
    )

    val pickups by PlatformRepository.pickups.collectAsState()
    val pricingRules by PlatformRepository.pricingRules.collectAsState()
    val fraudAlerts by PlatformRepository.fraudAlerts.collectAsState()
    val eidConfig by PlatformRepository.eidConfig.collectAsState()
    val ledger by PlatformRepository.ledgerEntries.collectAsState()
    val auditLogs by PlatformRepository.auditLogs.collectAsState()

    var editingRule by remember { mutableStateOf<PricingRule?>(null) }
    var editMarketRate by remember { mutableStateOf("") }
    var editMargin by remember { mutableStateOf("") }
    var editReason by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Navigation Bar
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldGreenPrimary
        ) {
            tabs.forEachIndexed { index, (en, ur) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = if (isUrdu) ur else en,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: KPI Overview & Pickups
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiStatCard("Total Recycled", "148,200 KG", "♻️", Modifier.weight(1f))
                            KpiStatCard("Total Pickups", "${pickups.size + 140}", "🚚", Modifier.weight(1f))
                            KpiStatCard("Net Payouts", "Rs. 1.2M", "💰", Modifier.weight(1f))
                        }
                    }

                    item {
                        Text(
                            text = if (isUrdu) "لائیو آپریشنز اور پک اپ مانیٹر" else "Live Field Operations & Dispatches",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    items(pickups) { pickup ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${pickup.id} • ${pickup.city} (${pickup.zone})", fontWeight = FontWeight.Bold)
                                    StatusBadge(status = pickup.status, isUrdu = isUrdu)
                                }
                                Text("Customer: ${pickup.customerName} (${pickup.customerPhone})", fontSize = 12.sp)
                                Text("Collector: ${pickup.assignedCollectorName ?: "Unassigned"}", fontSize = 12.sp, color = EmeraldGreenPrimary, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Est / Ver Weight: ${pickup.totalWeightKg.toInt()} kg", fontSize = 11.sp, color = Color.Gray)
                                    PkrText(amount = if (pickup.verifiedTotal > 0) pickup.verifiedTotal else pickup.estimatedTotal)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Dynamic Pricing Engine
                    item {
                        Text(
                            text = if (isUrdu) "ڈائنامک پرائسنگ انجن رولز" else "Dynamic Pricing Rules Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    items(pricingRules) { rule ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rule.materialName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Surface(color = EmeraldContainer, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "${rule.city} (${rule.zone})",
                                            color = OnEmeraldContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Wholesale Base:", fontSize = 11.sp, color = Color.Gray)
                                        PkrText(amount = rule.marketRate)
                                    }
                                    Column {
                                        Text("Buying Margin:", fontSize = 11.sp, color = Color.Gray)
                                        Text("${rule.buyingMargin}%", fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Customer Payout:", fontSize = 11.sp, color = Color.Gray)
                                        PkrText(amount = rule.calculateCustomerRate())
                                    }
                                }

                                Button(
                                    onClick = {
                                        editingRule = rule
                                        editMarketRate = rule.marketRate.toString()
                                        editMargin = rule.buyingMargin.toString()
                                        editReason = "Global commodity index update"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Edit Rate & Buying Margin")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: AI Fraud & Quality
                    item {
                        Text(
                            text = if (isUrdu) "اے آئی اینوملی اور فراڈ ڈیٹیکشن" else "AI Quality & Anomaly Radar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    items(fraudAlerts) { alert ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(alert.type, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                    Surface(color = Color(0xFFDC2626), shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = alert.severity,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(text = alert.description, fontSize = 12.sp, color = Color(0xFF7F1D1D))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { PlatformRepository.resolveFraudAlert(alert.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Clear Exception")
                                    }
                                    OutlinedButton(
                                        onClick = { PlatformRepository.resolveFraudAlert(alert.id, false) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Audit Scale")
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: Eid Campaign
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AmberGoldContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🐑 ${eidConfig.campaignName}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = OnAmberGoldContainer)
                                Text("Dates: ${eidConfig.startDate} to ${eidConfig.endDate}", fontSize = 12.sp, color = OnAmberGoldContainer)
                                Text("Special Collection Zones: ${eidConfig.specialZones.joinToString()}", fontSize = 12.sp, color = OnAmberGoldContainer)
                                Text("Partner Welfare NGOs: ${eidConfig.partnerNGOs.joinToString()}", fontSize = 12.sp, color = OnAmberGoldContainer)

                                Button(
                                    onClick = { PlatformRepository.toggleEidCampaign(!eidConfig.isCampaignActive) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (eidConfig.isCampaignActive) Color(0xFFDC2626) else EmeraldGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (eidConfig.isCampaignActive) "Deactivate Eid Campaign" else "Activate Eid Campaign")
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: Financial Ledger
                    item {
                        Text(
                            text = if (isUrdu) "ڈبل انٹری مالیاتی لیجر" else "Double-Entry Financial Ledger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    items(ledger) { entry ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entry.id, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(formatPkr(entry.amount), fontWeight = FontWeight.Black, color = EmeraldGreenPrimary)
                                }
                                Text(entry.description, fontSize = 11.sp)
                                Text("Idempotency Key: ${entry.idempotencyKey}", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                5 -> {
                    // TAB 5: Audit Logs
                    item {
                        Text(
                            text = if (isUrdu) "مکمل سیکیورٹی آڈٹ لاگز" else "System Audit & Compliance Logs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    items(auditLogs) { log ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldGreenPrimary)
                                    Text(log.actorRole, fontSize = 10.sp, color = Color.Gray)
                                }
                                Text("${log.entity}: ${log.entityId}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                if (log.details.isNotEmpty()) {
                                    Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Pricing Rule Dialog
    if (editingRule != null) {
        AlertDialog(
            onDismissRequest = { editingRule = null },
            title = { Text("Update Pricing Rule: ${editingRule?.materialName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editMarketRate,
                        onValueChange = { editMarketRate = it },
                        label = { Text("Base Market Rate (PKR/KG)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editMargin,
                        onValueChange = { editMargin = it },
                        label = { Text("Buying Margin (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editReason,
                        onValueChange = { editReason = it },
                        label = { Text("Audit Reason for Price Adjustment") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = editMarketRate.toDoubleOrNull() ?: 0.0
                        val margin = editMargin.toDoubleOrNull() ?: 0.0
                        editingRule?.let {
                            PlatformRepository.updatePricingRule(it.id, rate, margin, editReason)
                        }
                        editingRule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text("Apply & Audit Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRule = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun KpiStatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = EmeraldGreenPrimary)
            Text(text = title, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

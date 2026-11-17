package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.repository.FsWalletLedgerEntry
import com.example.model.PaymentMethod
import com.example.model.PickupRequest
import com.example.model.PickupStatus
import com.example.service.PlatformRepository
import com.example.ui.components.PkrText
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatPkr
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun CurrentRatesScreen(
    isUrdu: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val materials by PlatformRepository.materials.collectAsState()
    var selectedCity by remember { mutableStateOf("Rawalpindi") }
    val cityFactor = when (selectedCity) {
        "Karachi" -> 1.03
        "Lahore" -> 1.01
        "Islamabad" -> 1.00
        "Rawalpindi" -> 1.00
        else -> 0.98
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("rates_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isUrdu) "آج کے تصدیق شدہ کباڑ ریٹس" else "Live Scrap Market Rates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Regional Index: $selectedCity • Wholesale Indexed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // City Selector
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("Rawalpindi", "Islamabad", "Karachi", "Lahore", "Faisalabad")) { city ->
                        FilterChip(
                            selected = selectedCity == city,
                            onClick = { selectedCity = city },
                            label = { Text(city) }
                        )
                    }
                }
            }

            // Rates Banner Notice
            item {
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚖️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isUrdu) "تمام قیمتیں کلوگرام کے حساب سے ہیں۔ 200 کلوگرام سے زائد بلک پر اضافی 5% بونس دیا جاتا ہے۔" else "All rates in PKR per Kilogram. Bulk scrap (>200kg) receives an automatic 5% high-volume bonus.",
                            fontSize = 11.sp,
                            color = OnEmeraldContainer
                        )
                    }
                }
            }

            // Material List Items
            items(materials) { mat ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(text = mat.category.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isUrdu) mat.urduName else mat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = mat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            PkrText(amount = (mat.baseRatePerKg * cityFactor).roundToInt().toDouble(), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "per ${mat.unit}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerWalletScreen(
    isUrdu: Boolean,
    onBack: () -> Unit,
    cloudLedger: List<FsWalletLedgerEntry> = emptyList(),
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val walletBalance by PlatformRepository.customerWalletBalance.collectAsState()
    val ledger by PlatformRepository.ledgerEntries.collectAsState()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("1500") }
    var accountDetails by remember { mutableStateOf("03001234567") }
    var selectedPayoutMethod by remember { mutableStateOf(PaymentMethod.EASYPAISA) }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wallet_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "ڈیجیٹل کباڑیا والٹ" else "Wallet & Cashouts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
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
            // Balance Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isUrdu) "موجودہ قابلِ اخراج بیلنس" else "Total Available Scrap Balance",
                            color = EmeraldContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatPkr(walletBalance),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showWithdrawDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGoldSecondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("withdraw_funds_btn")
                            ) {
                                Icon(imageVector = Icons.Default.NorthEast, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isUrdu) "رقم نکالیں" else "Withdraw Funds",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            if (cloudLedger.isNotEmpty()) {
                item {
                    Text(
                        text = if (isUrdu) "کلاؤڈ لیجر ریکارڈ (${cloudLedger.size})" else "Synced Cloud Ledger (${cloudLedger.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                items(cloudLedger) { entry ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${entry.category} • ${entry.idempotencyKey}", fontSize = 10.sp, color = Color.Gray)
                            }
                            PkrText(amount = entry.amount)
                        }
                    }
                }
            }

            // Ledger Transaction History
            item {
                Text(
                    text = if (isUrdu) "لین دین کی تاریخ (آڈٹ شدہ لیجر)" else "Transaction History & Audit Trail",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (entry.type == com.example.model.LedgerTxType.CREDIT) EmeraldContainer else Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (entry.type == com.example.model.LedgerTxType.CREDIT) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (entry.type == com.example.model.LedgerTxType.CREDIT) EmeraldGreenPrimary else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = entry.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${entry.category} • Idempotency: ${entry.idempotencyKey}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (entry.type == com.example.model.LedgerTxType.CREDIT) "+" else "-"}${formatPkr(entry.amount)}",
                                fontWeight = FontWeight.Black,
                                color = if (entry.type == com.example.model.LedgerTxType.CREDIT) EmeraldGreenPrimary else Color(0xFFDC2626),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Bal: ${formatPkr(entry.balanceAfter)}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Cashout Dialog
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text(if (isUrdu) "والٹ سے رقم نکالیں" else "Withdraw Scrap Funds") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Payout Channel:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(listOf(PaymentMethod.EASYPAISA, PaymentMethod.JAZZCASH, PaymentMethod.RAAST)) { method ->
                            FilterChip(
                                selected = selectedPayoutMethod == method,
                                onClick = { selectedPayoutMethod = method },
                                label = { Text(method.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Amount (PKR)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountDetails,
                        onValueChange = { accountDetails = it },
                        label = { Text("Mobile / Raast IBAN Number") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                        PlatformRepository.withdrawWalletFunds(amt, accountDetails, selectedPayoutMethod)
                        showWithdrawDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    modifier = Modifier.testTag("confirm_withdraw_btn")
                ) {
                    Text(if (isUrdu) "منتقل کریں" else "Confirm Transfer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun ActivePickupScreen(
    pickup: PickupRequest,
    isUrdu: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pickup #${pickup.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    StatusBadge(status = pickup.status, isUrdu = isUrdu)
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
            // Live map & ETA
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🗺️", fontSize = 40.sp)
                        Text(
                            text = "Collector Geofenced: 1.2 km away",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Arriving at ${pickup.address.take(28)}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            // OTP Code Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberGoldContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SECURITY OTP CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnAmberGoldContainer)
                            Text(pickup.otpCode, fontSize = 32.sp, fontWeight = FontWeight.Black, color = OnAmberGoldContainer)
                        }
                        Button(
                            onClick = { PlatformRepository.showToast("OTP ${pickup.otpCode} copied.") },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGoldSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Copy OTP", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Collector Contact Details
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Assigned Collector", fontSize = 11.sp, color = Color.Gray)
                            Text(pickup.assignedCollectorName ?: "Rashid Khan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(pickup.assignedCollectorPhone ?: "+92 333 9876543", fontSize = 12.sp, color = EmeraldGreenPrimary)
                        }
                        IconButton(
                            onClick = { PlatformRepository.showToast("Calling ${pickup.assignedCollectorPhone}...") },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldContainer)
                        ) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = "Call", tint = EmeraldGreenPrimary)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.PlatformSyncViewModel
import com.example.ui.theme.*

@Composable
fun CustomerServicesScreen(
    isUrdu: Boolean,
    userDisplayName: String,
    userEmail: String?,
    syncViewModel: PlatformSyncViewModel,
    onToggleUrdu: () -> Unit,
    onOpenSupportPortal: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val syncState by syncViewModel.uiState.collectAsStateWithLifecycle()
    var savedAddresses by remember {
        mutableStateOf(
            listOf(
                "Home • House 14, Street 6, Chaklala Scheme III, Rawalpindi",
                "Office • Suite 204, Blue Area, Jinnah Ave, Islamabad"
            )
        )
    }
    var newAddressInput by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var whatsappUpdatesEnabled by remember { mutableStateOf(true) }
    var referralCopied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("customer_services_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "پروفائل، انعامات اور ترتیبات" else "Profile, Addresses & Rewards",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = userDisplayName.ifBlank { "Digital Kabariya Member" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Text(userEmail, color = EmeraldContainer, fontSize = 12.sp)
                        }
                        Text(
                            text = "City: ${syncState.userProfile?.city ?: "Rawalpindi"} • Role: ${syncState.userProfile?.role ?: "CUSTOMER"}",
                            color = AmberGoldSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Rewards, Referrals & Offers
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberGoldContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎁 Rewards & Referral Code", fontWeight = FontWeight.Bold, color = OnAmberGoldContainer)
                        Text(
                            text = "Green Points: 420 pts • Invite friends in Rawalpindi/Islamabad and earn Rs. 150 bonus after their first completed pickup!",
                            fontSize = 12.sp,
                            color = OnAmberGoldContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Code: KABARIYA-PK150", fontWeight = FontWeight.Black, color = OnAmberGoldContainer)
                            Button(
                                onClick = {
                                    referralCopied = true
                                    PlatformRepository.showToast("Referral code KABARIYA-PK150 copied!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(if (referralCopied) "Copied!" else "Share Code", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Active Offers
            item {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🔥 Active Offers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("• +5% Bulk Bonus on >200 kg Iron, Paper, or Cardboard", fontSize = 12.sp)
                        Text("• Zero Pickup Fee in Rawalpindi & Islamabad Launch Zones", fontSize = 12.sp)
                    }
                }
            }

            // Saved Addresses
            item {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📍 Saved Pickup Addresses", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        savedAddresses.forEach { addr ->
                            Text("• $addr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = newAddressInput,
                            onValueChange = { newAddressInput = it },
                            label = { Text("Add new Rawalpindi / Islamabad address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                if (newAddressInput.isNotBlank()) {
                                    savedAddresses = savedAddresses + newAddressInput.trim()
                                    newAddressInput = ""
                                    PlatformRepository.showToast("Address saved!")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Save Address")
                        }
                    }
                }
            }

            // Notifications & Settings
            item {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔔 Notifications & Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pickup & Collector Alerts", fontSize = 13.sp)
                            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SMS / Receipt Alerts", fontSize = 13.sp)
                            Switch(checked = whatsappUpdatesEnabled, onCheckedChange = { whatsappUpdatesEnabled = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Urdu Language (اردو)", fontSize = 13.sp)
                            Switch(checked = isUrdu, onCheckedChange = { onToggleUrdu() })
                        }
                    }
                }
            }

            // Support Portal Shortcut
            item {
                Button(
                    onClick = onOpenSupportPortal,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_support_from_profile_btn")
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isUrdu) "کسٹمر سپورٹ ٹکٹ کھولیں" else "Open Customer Support Center")
                }
            }
        }
    }
}

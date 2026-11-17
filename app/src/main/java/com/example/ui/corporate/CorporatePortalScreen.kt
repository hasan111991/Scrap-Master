package com.example.ui.corporate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.service.PlatformRepository
import com.example.ui.components.PkrText
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

@Composable
fun CorporatePortalScreen(
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    val corp by PlatformRepository.corporateClient.collectAsState()

    var showCertificateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Corporate Company Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = corp.companyName,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = EmeraldGreenPrimary
                            )
                            Text(
                                text = "NTN: ${corp.ntn} • ${corp.contactPerson}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ESG TIER 1",
                                color = OnEmeraldContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showCertificateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("view_esg_cert_btn")
                    ) {
                        Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUrdu) "سرکاری ای ایس جی ری سائیکلنگ سرٹیفکیٹ" else "View Official ESG Green Certificate")
                    }
                }
            }
        }

        // ESG Impact Metrics Grid (3 Cards)
        item {
            Text(
                text = if (isUrdu) "ماحولیاتی بچت اور اثرات" else "Sustainability Impact Metrics (ESG)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EsgMetricCard(
                    title = "Waste Diverted",
                    value = "${corp.totalWasteDivertedKg.toInt()} KG",
                    icon = "♻️",
                    modifier = Modifier.weight(1f)
                )
                EsgMetricCard(
                    title = "CO2 Prevented",
                    value = "${(corp.co2SavedKg / 1000).toInt()} Tons",
                    icon = "🌍",
                    modifier = Modifier.weight(1f)
                )
                EsgMetricCard(
                    title = "Trees Saved",
                    value = "${corp.treesSavedEquivalent}",
                    icon = "🌳",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Branches List
        item {
            Text(
                text = if (isUrdu) "شاخیں اور فیکٹری سائٹس" else "Registered Corporate Branches & Sites",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        items(corp.branches) { branch ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(branch.branchName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${branch.city} • ${branch.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Monthly Scrap: ${branch.monthlyKg.toInt()} kg • Last: ${branch.lastPickupDate}", fontSize = 11.sp, color = EmeraldGreenPrimary)
                    }

                    OutlinedButton(
                        onClick = { PlatformRepository.showToast("Scheduled dedicated pickup for ${branch.branchName}") },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Schedule")
                    }
                }
            }
        }
    }

    if (showCertificateDialog) {
        AlertDialog(
            onDismissRequest = { showCertificateDialog = false },
            title = { Text("ESG Green Recycling Certificate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Issued by Digital Kabariya Pakistan Environmental Standards Unit", fontSize = 11.sp, color = Color.Gray)
                    Divider()
                    Text("Certificate ID: CERT-DK-PK-2026-092", fontWeight = FontWeight.Bold)
                    Text("Holder: ${corp.companyName} (NTN: ${corp.ntn})")
                    Text("Total Waste Diverted from Landfills: ${corp.totalWasteDivertedKg.toInt()} KG")
                    Text("Carbon Footprint Abatement: ${corp.co2SavedKg.toInt()} KG CO2e")
                    Text("Equivalence: ${corp.treesSavedEquivalent} mature trees planted")
                    Divider()
                    Text("Certified Clean Chain of Custody Verified by EPA Sindh & Punjab", fontSize = 11.sp, color = EmeraldGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PlatformRepository.showToast("Certificate downloaded.")
                        showCertificateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text("Download PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCertificateDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun EsgMetricCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 14.sp, color = OnEmeraldContainer)
            Text(text = title, fontSize = 10.sp, color = OnEmeraldContainer.copy(alpha = 0.8f))
        }
    }
}

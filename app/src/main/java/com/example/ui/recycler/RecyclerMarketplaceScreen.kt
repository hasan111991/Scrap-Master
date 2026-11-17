package com.example.ui.recycler

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
import com.example.model.MarketplaceLot
import com.example.service.PlatformRepository
import com.example.ui.components.PkrText
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

@Composable
fun RecyclerMarketplaceScreen(
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    val lots by PlatformRepository.marketplaceLots.collectAsState()

    var showBidDialog by remember { mutableStateOf(false) }
    var selectedLotForBid by remember { mutableStateOf<MarketplaceLot?>(null) }
    var bidInput by remember { mutableStateOf("172.0") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recycler B2B Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AmberGoldSecondary)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AmberGoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🛒", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isUrdu) "بی ٹو بی اسکریپ ہول سیل مارکیٹ" else "B2B Recycler & Industrial Lots",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Wholesale sorted tonnage directly from verified collection hubs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Available Lots
        item {
            Text(
                text = if (isUrdu) "دستیاب اسکریپ لاٹس اور بولیاں" else "Available Scrap Lots & Auctions",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        items(lots) { lot ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lot.lotId, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Surface(
                            color = if (lot.status == "SOLD") Color(0xFFFEE2E2) else EmeraldContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = lot.status,
                                color = if (lot.status == "SOLD") Color(0xFFDC2626) else OnEmeraldContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = lot.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = EmeraldGreenPrimary
                    )

                    Text(
                        text = "Warehouse: ${lot.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Tonnage:", fontSize = 11.sp, color = Color.Gray)
                            Text("${lot.weightKg.toInt()} KG", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Min Bid / KG:", fontSize = 11.sp, color = Color.Gray)
                            PkrText(amount = lot.minimumBidPerKg, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Buy Now / KG:", fontSize = 11.sp, color = Color.Gray)
                            PkrText(amount = lot.pricePerKg, style = MaterialTheme.typography.bodyMedium, color = AmberGoldSecondary)
                        }
                    }

                    if (lot.highestBidPerKg > 0) {
                        Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "Highest Bid: Rs. ${lot.highestBidPerKg.toInt()}/kg (${lot.bidsCount} bids)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (lot.status != "SOLD") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedLotForBid = lot
                                    bidInput = (lot.highestBidPerKg.takeIf { it > 0 } ?: lot.minimumBidPerKg).toString()
                                    showBidDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Place Bid")
                            }

                            Button(
                                onClick = { PlatformRepository.buyMarketplaceLot(lot.lotId) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("buy_now_${lot.lotId}")
                            ) {
                                Text("Buy Now")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBidDialog && selectedLotForBid != null) {
        AlertDialog(
            onDismissRequest = { showBidDialog = false },
            title = { Text("Place Bid on ${selectedLotForBid?.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your bid per Kilogram (PKR):", fontSize = 12.sp)
                    OutlinedTextField(
                        value = bidInput,
                        onValueChange = { bidInput = it },
                        label = { Text("Bid Rate (PKR/KG)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val b = bidInput.toDoubleOrNull() ?: 0.0
                        selectedLotForBid?.let { PlatformRepository.placeMarketplaceBid(it.lotId, b) }
                        showBidDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text("Submit Bid")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBidDialog = false }) { Text("Cancel") }
            }
        )
    }
}

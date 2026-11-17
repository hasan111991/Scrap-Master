package com.example.ui.hub

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
import com.example.model.ScrapCategory
import com.example.service.PlatformRepository
import com.example.ui.components.PkrText
import com.example.ui.components.formatPkr
import com.example.ui.inventory.ScrapInventoryScreen
import com.example.ui.inventory.ScrapInventoryViewModel
import com.example.ui.theme.*

@Composable
fun CollectionHubScreen(
    isUrdu: Boolean,
    inventoryViewModel: ScrapInventoryViewModel? = null,
    modifier: Modifier = Modifier
) {
    val batches by PlatformRepository.hubBatches.collectAsState()

    var selectedHubTab by remember { mutableStateOf(0) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var selectedMaterial by remember { mutableStateOf("Cardboard / Gatta") }
    var grossWeightInput by remember { mutableStateOf("1250") }
    var netWeightInput by remember { mutableStateOf("1210") }
    var sortingCostInput by remember { mutableStateOf("2500") }

    Column(modifier = modifier.fillMaxSize()) {
        if (inventoryViewModel != null) {
            TabRow(
                selectedTabIndex = selectedHubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldGreenPrimary
            ) {
                Tab(
                    selected = selectedHubTab == 0,
                    onClick = { selectedHubTab = 0 },
                    text = { Text(if (isUrdu) "ہب بیجز و ترازو" else "Hub Weighing & Batches") }
                )
                Tab(
                    selected = selectedHubTab == 1,
                    onClick = { selectedHubTab = 1 },
                    text = { Text(if (isUrdu) "گودام انوینٹری (فائر اسٹور)" else "Warehouse Inventory") }
                )
            }
        }

        if (selectedHubTab == 1 && inventoryViewModel != null) {
            ScrapInventoryScreen(
                viewModel = inventoryViewModel,
                isUrdu = isUrdu,
                modifier = Modifier.fillMaxSize()
            )
        } else {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hub Station Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🏭", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Central Sorting Hub Korangi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Karachi Zone 4 • Scale Station #02",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = EmeraldGreenPrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OPERATIONAL",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showBatchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inbound_weigh_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Scale, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUrdu) "ان باؤنڈ کباڑ وصولی و ترازو جانچ" else "Inbound Scrap Receiving & Weighbridge Audit")
                    }
                }
            }
        }

        // Active Sorted Batches in Warehouse
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUrdu) "گودام میں موجود لاٹس اور بیجز" else "Warehouse Batches & Inventory",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${batches.size} Batches",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        items(batches) { batch ->
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
                        Text(batch.batchId, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Surface(color = EmeraldContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = batch.status,
                                color = OnEmeraldContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${batch.materialName} (${batch.grade})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = EmeraldGreenPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gross Weight:", fontSize = 11.sp, color = Color.Gray)
                            Text("${batch.grossWeightKg.toInt()} kg", fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Net Weight:", fontSize = 11.sp, color = Color.Gray)
                            Text("${batch.netWeightKg.toInt()} kg", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Variance / Moisture:", fontSize = 11.sp, color = Color.Gray)
                            Text("${batch.varianceKg.toInt()} kg", color = if (batch.varianceKg < 0) Color(0xFFDC2626) else EmeraldGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Hub Purchase Cost:", fontSize = 12.sp, color = Color.Gray)
                        PkrText(amount = batch.purchaseCost, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    }
    }

    if (showBatchDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDialog = false },
            title = { Text("Log Inbound Batch & Weighbridge Audit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = selectedMaterial,
                        onValueChange = { selectedMaterial = it },
                        label = { Text("Material Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = grossWeightInput,
                        onValueChange = { grossWeightInput = it },
                        label = { Text("Gross Scale Weight (KG)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = netWeightInput,
                        onValueChange = { netWeightInput = it },
                        label = { Text("Net Weight after Moisture/Dirt (KG)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sortingCostInput,
                        onValueChange = { sortingCostInput = it },
                        label = { Text("Baling & Labor Cost (PKR)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gross = grossWeightInput.toDoubleOrNull() ?: 1000.0
                        val net = netWeightInput.toDoubleOrNull() ?: 980.0
                        val cost = sortingCostInput.toDoubleOrNull() ?: 2000.0
                        PlatformRepository.hubCreateBatch(
                            warehouseName = "Central Sorting Hub Korangi",
                            warehouseCity = "Karachi",
                            materialName = selectedMaterial,
                            category = ScrapCategory.CARDBOARD,
                            grade = "Industrial Baled A-Grade",
                            grossWeight = gross,
                            netWeight = net,
                            purchaseCost = net * 38.0,
                            sortingCost = cost,
                            sourceIds = listOf("PKP-7819", "PKP-7821")
                        )
                        showBatchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    modifier = Modifier.testTag("submit_hub_batch_btn")
                ) {
                    Text("Save & Dispatch to B2B Marketplace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDialog = false }) { Text("Cancel") }
            }
        )
    }
}

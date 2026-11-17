package com.example.ui.inventory

import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScrapInventoryItem
import com.example.model.ScrapCategory
import com.example.ui.components.PkrText
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

@Composable
fun ScrapInventoryScreen(
    viewModel: ScrapInventoryViewModel,
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val opStatus by viewModel.operationStatus.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf(ScrapCategory.IRON_STEEL) }
    var newGrade by remember { mutableStateOf("Standard A") }
    var newStockKg by remember { mutableStateOf("250") }
    var newThresholdKg by remember { mutableStateOf("50") }
    var newPricePkr by remember { mutableStateOf("145") }
    var newWarehouse by remember { mutableStateOf("Rawalpindi Hub I-10") }

    Column(modifier = modifier.fillMaxSize()) {
        if (opStatus != null) {
            Surface(
                color = EmeraldContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = opStatus!!,
                        color = OnEmeraldContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearOperationStatus() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = OnEmeraldContainer)
                    }
                }
            }
        }

        when (val state = uiState) {
            is InventoryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isUrdu) "فائر اسٹور انوینٹری لوڈ ہو رہی ہے..." else "Syncing Cloud Firestore Inventory...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is InventoryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
                            Text("Inventory Sync Error", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Text(state.message, fontSize = 12.sp, color = Color(0xFF7F1D1D))
                        }
                    }
                }
            }

            is InventoryUiState.Success -> {
                val filteredItems = remember(state.items, selectedCategoryFilter) {
                    when (selectedCategoryFilter) {
                        "ALL" -> state.items
                        "LOW_STOCK" -> state.items.filter { it.isLowStock }
                        else -> state.items.filter { it.category == selectedCategoryFilter }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Inventory Overview Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isUrdu) "گودام اسکریپ انوینٹری (لائیو)" else "Warehouse Scrap Inventory (Firestore)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "${state.items.size} SKUs tracked • Hazardous waste isolated",
                                            color = EmeraldContainer,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Button(
                                        onClick = { showAddDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberGoldSecondary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("add_inventory_item_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Stock", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Weight", color = EmeraldContainer, fontSize = 11.sp)
                                        Text("${state.totalStockKg.toInt()} KG", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    }
                                    Column {
                                        Text("Est. Valuation", color = EmeraldContainer, fontSize = 11.sp)
                                        Text(formatPkr(state.totalEstimatedValuePkr), color = AmberGoldSecondary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Low Stock", color = EmeraldContainer, fontSize = 11.sp)
                                        Text("${state.lowStockCount} Alerts", color = if (state.lowStockCount > 0) Color(0xFFFCA5A5) else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Filter Chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "ALL",
                                    onClick = { selectedCategoryFilter = "ALL" },
                                    label = { Text("All (${state.items.size})") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "LOW_STOCK",
                                    onClick = { selectedCategoryFilter = "LOW_STOCK" },
                                    label = { Text("Low Stock (${state.lowStockCount})") }
                                )
                            }
                            items(listOf(ScrapCategory.COPPER_BRASS, ScrapCategory.IRON_STEEL, ScrapCategory.CARDBOARD, ScrapCategory.PLASTIC, ScrapCategory.BATTERIES, ScrapCategory.ELECTRONICS)) { cat ->
                                FilterChip(
                                    selected = selectedCategoryFilter == cat.name,
                                    onClick = { selectedCategoryFilter = cat.name },
                                    label = { Text("${cat.icon} ${cat.displayName}") }
                                )
                            }
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("📦", fontSize = 40.sp)
                                    Text(
                                        text = if (isUrdu) "انوینٹری خالی ہے" else "No Scrap Inventory Records Yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Add your first warehouse scrap batch or load starter Rawalpindi & Islamabad hub stock.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.seedStarterRawalpindiInventory() },
                                            modifier = Modifier.testTag("seed_inventory_btn")
                                        ) {
                                            Text("Load Starter Stock")
                                        }
                                        Button(
                                            onClick = { showAddDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                                        ) {
                                            Text("Add Item")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            InventoryItemCard(
                                item = item,
                                isUrdu = isUrdu,
                                onAdjustStock = { delta -> viewModel.adjustStock(item.id, delta) },
                                onDelete = { viewModel.deleteItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Warehouse Scrap Stock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Material Name (e.g. Copper Pipe)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ScrapCategory.entries.take(8)) { cat ->
                            FilterChip(
                                selected = newCategory == cat,
                                onClick = { newCategory = cat },
                                label = { Text("${cat.icon} ${cat.displayName}") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newGrade,
                        onValueChange = { newGrade = it },
                        label = { Text("Grade") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newStockKg,
                            onValueChange = { newStockKg = it },
                            label = { Text("Stock (KG)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newPricePkr,
                            onValueChange = { newPricePkr = it },
                            label = { Text("Rate (PKR/KG)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = newWarehouse,
                        onValueChange = { newWarehouse = it },
                        label = { Text("Warehouse Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val stock = (newStockKg.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        val threshold = (newThresholdKg.toDoubleOrNull() ?: 50.0).coerceAtLeast(0.0)
                        val price = (newPricePkr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        if (newName.isNotBlank()) {
                            viewModel.addStockItem(
                                ScrapInventoryItem(
                                    materialId = "mat_${newCategory.name.lowercase()}",
                                    name = newName.trim(),
                                    urduName = newCategory.urduName,
                                    category = newCategory.name,
                                    grade = newGrade.ifBlank { "Standard A" },
                                    currentStockKg = stock,
                                    minThresholdKg = threshold,
                                    unitPricePkr = price,
                                    warehouseLocation = newWarehouse.ifBlank { "Rawalpindi Hub" }
                                )
                            )
                            newName = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    modifier = Modifier.testTag("confirm_add_inventory_btn")
                ) {
                    Text("Save to Firestore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InventoryItemCard(
    item: ScrapInventoryItem,
    isUrdu: Boolean,
    onAdjustStock: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val isHazardous = item.category == ScrapCategory.BATTERIES.name || item.category == ScrapCategory.ELECTRONICS.name

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            .copy(brush = androidx.compose.ui.graphics.SolidColor(if (item.isLowStock) Color(0xFFEF4444) else BorderLight))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUrdu && item.urduName.isNotBlank()) item.urduName else item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${item.grade} • ${item.warehouseLocation}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isHazardous) {
                        Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "HAZARD ISOLATED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        color = if (item.isLowStock) Color(0xFFFEE2E2) else EmeraldContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.status,
                            color = if (item.isLowStock) Color(0xFFB91C1C) else OnEmeraldContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Stock Available", fontSize = 10.sp, color = Color.Gray)
                    Text("${item.currentStockKg.toInt()} KG", fontWeight = FontWeight.Black, fontSize = 16.sp, color = EmeraldGreenPrimary)
                }
                Column {
                    Text("Unit Rate", fontSize = 10.sp, color = Color.Gray)
                    PkrText(amount = item.unitPricePkr, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Batch Value", fontSize = 10.sp, color = Color.Gray)
                    PkrText(amount = item.estimatedTotalValuePkr, style = MaterialTheme.typography.bodyMedium, color = AmberGoldSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onAdjustStock(-10.0) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("-10 kg", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { onAdjustStock(25.0) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+25 kg", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onAdjustStock(100.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+100 kg", fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete inventory item", tint = Color(0xFFDC2626))
                }
            }
        }
    }
}

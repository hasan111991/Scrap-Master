package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.repository.FsPickupRequest
import com.example.model.PickupRequest
import com.example.model.PickupStatus
import com.example.model.ScrapCategory
import com.example.service.PlatformRepository
import com.example.ui.components.PkrText
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

@Composable
fun CustomerHomeScreen(
    isUrdu: Boolean,
    onNavigateSell: (ScrapCategory) -> Unit,
    onNavigateRates: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateActivePickup: (PickupRequest) -> Unit,
    onNavigateEidCampaign: () -> Unit,
    onNavigateServices: (() -> Unit)? = null,
    cloudPickups: List<FsPickupRequest> = emptyList(),
    modifier: Modifier = Modifier
) {
    val walletBalance by PlatformRepository.customerWalletBalance.collectAsState()
    val pickups by PlatformRepository.pickups.collectAsState()
    val eidConfig by PlatformRepository.eidConfig.collectAsState()

    val activePickup = pickups.firstOrNull { it.status != PickupStatus.COMPLETED && it.status != PickupStatus.CANCELLED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Pickup Banner if en-route
        if (activePickup != null) {
            item {
                ActivePickupBanner(
                    pickup = activePickup,
                    isUrdu = isUrdu,
                    onClick = { onNavigateActivePickup(activePickup) }
                )
            }
        }

        // Wallet & Quick Balance Card
        item {
            CustomerBalanceCard(
                balance = walletBalance,
                isUrdu = isUrdu,
                onSellClick = { onNavigateSell(ScrapCategory.IRON_STEEL) },
                onWalletClick = onNavigateWallet
            )
        }

        // Eid Campaign Special Banner (if active)
        if (eidConfig.isCampaignActive) {
            item {
                EidCampaignBanner(
                    config = eidConfig,
                    isUrdu = isUrdu,
                    onClick = onNavigateEidCampaign
                )
            }
        }

        // Quick Service Grid (4 Core Tiles)
        item {
            Text(
                text = if (isUrdu) "خدمات و سہولیات" else "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = if (isUrdu) "کباڑ فروخت کریں" else "Sell Scrap",
                    subtitle = if (isUrdu) "گھر بیٹھے پک اپ" else "Doorstep pickup",
                    icon = "🚚",
                    bgColor = EmeraldContainer,
                    onClick = { onNavigateSell(ScrapCategory.IRON_STEEL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_sell_scrap")
                )
                ActionCard(
                    title = if (isUrdu) "آج کے ریٹس" else "Live Rates",
                    subtitle = if (isUrdu) "کراچی و لاہور" else "Daily market price",
                    icon = "📈",
                    bgColor = AmberGoldContainer,
                    onClick = onNavigateRates,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_view_rates")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = if (isUrdu) "ڈیجیٹل والٹ" else "Wallet & Payouts",
                    subtitle = if (isUrdu) "ایزی پیسہ / راست" else "Easypaisa & Raast",
                    icon = "💳",
                    bgColor = SkyBlueContainer,
                    onClick = onNavigateWallet,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_view_wallet")
                )
                ActionCard(
                    title = if (isUrdu) "ماحولیاتی اثر" else "Eco Savings",
                    subtitle = if (isUrdu) "84 درخت محفوظ" else "124 kg recycled",
                    icon = "🌱",
                    bgColor = Color(0xFFDCFCE7),
                    onClick = {
                        PlatformRepository.showToast("Your scrap has saved 84 trees and 240 kWh of energy!")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_view_eco")
                )
            }
        }

        if (onNavigateServices != null) {
            item {
                OutlinedButton(
                    onClick = onNavigateServices,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("action_customer_services")
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "پروفائل، پتے، انعامات اور سپورٹ" else "Addresses, Rewards, Referrals, Support & Profile",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Popular Categories Carousel
        item {
            Text(
                text = if (isUrdu) "ہم کیا خریدتے ہیں؟" else "Accepted Scrap Categories",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            ScrapCategoryGrid(isUrdu = isUrdu, onCategoryClick = onNavigateSell)
        }

        // Synced Cloud Firestore Pickups (if any)
        if (cloudPickups.isNotEmpty()) {
            item {
                Text(
                    text = if (isUrdu) "کلاؤڈ سنک شدہ پک اپس (${cloudPickups.size})" else "Cloud Synced Pickups (${cloudPickups.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(cloudPickups) { fsPickup ->
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
                            Text(
                                text = "${fsPickup.itemsSummary} • ${fsPickup.city}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${fsPickup.scheduledDate} (${fsPickup.timeSlot}) • ${fsPickup.status}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PkrText(amount = fsPickup.estimatedTotalPkr)
                            Text("CLOUD SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldGreenPrimary)
                        }
                    }
                }
            }
        }

        // Recent Pickup History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUrdu) "حالیہ پک اپ ریکارڈ" else "Pickup History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isUrdu) "تمام دیکھیں" else "View All",
                    style = MaterialTheme.typography.labelMedium.copy(color = EmeraldGreenPrimary),
                    modifier = Modifier.clickable { onNavigateSell(ScrapCategory.IRON_STEEL) }
                )
            }
        }

        items(pickups) { pickup ->
            PickupHistoryItem(
                pickup = pickup,
                isUrdu = isUrdu,
                onClick = { onNavigateActivePickup(pickup) }
            )
        }
    }
}

@Composable
fun CustomerBalanceCard(
    balance: Double,
    isUrdu: Boolean,
    onSellClick: () -> Unit,
    onWalletClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isUrdu) "ڈیجیٹل کباڑیا والٹ بیلنس" else "Available Scrap Wallet",
                        color = EmeraldContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatPkr(balance),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Surface(
                    color = AmberGoldSecondary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onWalletClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUrdu) "کیش آؤٹ" else "Cashout",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick sell action button
            Button(
                onClick = onSellClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("book_pickup_now_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUrdu) "ابھی کباڑ پک اپ بک کریں" else "Book Scrap Pickup Now",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun ActivePickupBanner(
    pickup: PickupRequest,
    isUrdu: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3B82F6))),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("active_pickup_banner")
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🛵", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pickup #${pickup.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E3A8A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(status = pickup.status, isUrdu = isUrdu)
                    }
                    Text(
                        text = pickup.assignedCollectorName ?: "Assigning nearest collector...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        text = "OTP: ${pickup.otpCode} • Tap to view live map",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF3B82F6)
            )
        }
    }
}

@Composable
fun EidCampaignBanner(
    config: com.example.model.EidCampaignConfig,
    isUrdu: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AmberGoldSecondary)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("eid_campaign_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🐑", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isUrdu) "عید قربانی آلائشیں و کھالیں کلیکشن" else "Eid Qurbani Waste & Hides",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF78350F)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = Color(0xFFDC2626), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = if (isUrdu) "صاف ستھرا پاکستان مشن • کھالیں بک کرائیں Rs. 750" else "Clean City Mission • Hides pickup Rs. 750/pc",
                    fontSize = 11.sp,
                    color = Color(0xFF92400E)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF78350F)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: String,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
        }
    }
}

private data class CategoryTile(
    val category: ScrapCategory,
    val en: String,
    val ur: String,
    val icon: String
)

@Composable
fun ScrapCategoryGrid(
    isUrdu: Boolean,
    onCategoryClick: (ScrapCategory) -> Unit
) {
    val items = listOf(
        CategoryTile(ScrapCategory.IRON_STEEL, "Loha / Steel", "لوہا اور اسٹیل", "🔩"),
        CategoryTile(ScrapCategory.COPPER_BRASS, "Copper (Taamba)", "تانبا وائر", "🔌"),
        CategoryTile(ScrapCategory.CARDBOARD, "Cardboard (Gatta)", "گتہ کارٹن", "📦"),
        CategoryTile(ScrapCategory.PAPER, "Akhbar / Books", "ردی اخبار", "📄"),
        CategoryTile(ScrapCategory.PLASTIC, "PET Bottles", "پلاسٹک بوتل", "🧴"),
        CategoryTile(ScrapCategory.BATTERIES, "UPS Battery", "بیٹریاں", "🔋")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in items.indices step 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0 until 3) {
                    val index = i + j
                    if (index < items.size) {
                        val tile = items[index]
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCategoryClick(tile.category) }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = tile.icon, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isUrdu) tile.ur else tile.en,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickupHistoryItem(
    pickup: PickupRequest,
    isUrdu: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${pickup.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${pickup.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = pickup.status, isUrdu = isUrdu)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${pickup.items.size} categories • ${pickup.city} (${pickup.zone})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Scheduled: ${pickup.scheduledDate} (${pickup.timeSlot})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                PkrText(amount = if (pickup.status >= PickupStatus.CONFIRMED && pickup.verifiedTotal > 0) pickup.verifiedTotal else pickup.estimatedTotal)
                Text(
                    text = "${pickup.totalWeightKg.toInt()} kg",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

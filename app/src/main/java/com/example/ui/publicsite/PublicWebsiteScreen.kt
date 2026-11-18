package com.example.ui.publicsite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollectionHubCatalog
import com.example.firebase.GoogleSignInButton
import com.example.model.HubScrapCategory
import com.example.model.HubScrapPrice
import com.example.ui.components.CollectionHubLogo
import com.example.ui.components.IndicativePriceDisclaimerBanner
import com.example.ui.theme.*

/**
 * Collection Hub Public Website (/).
 * Never displays any Admin Portal links, fake stats, fake reviews, or out-of-scope features.
 */
@Composable
fun PublicWebsiteScreen(
    isSignedIn: Boolean,
    categories: List<HubScrapCategory>,
    indicativePrices: List<HubScrapPrice>,
    supportEmail: String,
    supportPhone: String,
    onBookPickupClick: () -> Unit,
    onCollectorJoinClick: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var authError by remember { mutableStateOf<String?>(null) }
    var activePolicyDialog by remember { mutableStateOf<String?>(null) }
    var expandedFaqIndex by remember { mutableStateOf<Int?>(0) }

    val displayCategories = categories.filter { it.active }.ifEmpty { CollectionHubCatalog.initialCategories }
    val displayPrices = indicativePrices.filter { it.active }.ifEmpty { CollectionHubCatalog.seedIndicativePriceSeeds }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HubSurfaceLight),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // 1 & 2. Header & Hero Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(HubGreenDark, HubGreenPrimary)
                        )
                    )
                    .then(if (!isSignedIn) Modifier.statusBarsPadding() else Modifier)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!isSignedIn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CollectionHubLogo(size = 44.dp, bgColor = Color.White, tint = HubGreenPrimary)
                            Column {
                                Text(
                                    text = "Collection Hub",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 19.sp
                                )
                                Text(
                                    text = "Rawalpindi & Islamabad, Pakistan",
                                    color = HubGreenContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Digital Scrap Collection Marketplace",
                            color = HubGreenContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Text(
                        text = "Your Scrap. Your Collector.\nYour Collection Hub.",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        lineHeight = 32.sp
                    )

                    Text(
                        text = "Sell household and shop recyclables directly to nearby verified local kabari shops, scrap dealers, and independent collectors across Rawalpindi and Islamabad.",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // 3. Book Pickup & Collector Join CTAs or Sign-In Card
                    if (!isSignedIn) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Sign In to Book Pickup or Register as Collector",
                                    fontWeight = FontWeight.Bold,
                                    color = HubTextPrimaryLight,
                                    fontSize = 14.sp
                                )
                                GoogleSignInButton(
                                    isUrdu = false,
                                    onAuthSuccess = {
                                        authError = null
                                        onAuthSuccess()
                                    },
                                    onAuthError = { err -> authError = err }
                                )
                                if (authError != null) {
                                    Surface(color = ErrorContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = authError.orEmpty(),
                                            color = ErrorRed,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Secured by Google Sign-In & Firebase App Check. Your exact address stays private until a collector accepts.",
                                    fontSize = 11.sp,
                                    color = HubTextSecondaryLight
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onBookPickupClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = HubGreenPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("public_book_pickup_cta")
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Book Pickup", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onCollectorJoinClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                    .copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.7f))),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("public_collector_join_cta")
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Join Collector", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. How It Works
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "How Collection Hub Works",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = HubTextPrimaryLight
                )
                PublicStepTile(
                    step = "1",
                    title = "Book Scrap Pickup",
                    body = "Select one or more scrap categories, add estimated weight, and choose a preferred Rawalpindi or Islamabad time slot."
                )
                PublicStepTile(
                    step = "2",
                    title = "Nearby Collector Accepts",
                    body = "Only approved local collectors see your approximate area until one accepts. Share your 4-digit OTP at the doorstep."
                )
                PublicStepTile(
                    step = "3",
                    title = "Verify Weight & Settle Manually",
                    body = "Agree on verified physical weight and final PKR amount, then get paid via Cash, Easypaisa, or Bank Transfer."
                )
            }
        }

        // 5. 12 Scrap Categories (Indicative Rates Only)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "12 Accepted Scrap Categories",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = HubTextPrimaryLight
                )
                IndicativePriceDisclaimerBanner()

                displayCategories.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { cat ->
                            val matchedPrice = displayPrices.firstOrNull {
                                it.categoryId == cat.id || it.categoryName.equals(cat.name, ignoreCase = true)
                            }
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, HubBorderLight, RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HubTextPrimaryLight)
                                    Text(cat.urduName, fontSize = 12.sp, color = HubGreenMid)
                                    if (matchedPrice != null) {
                                        Text(
                                            text = "Est. Rs. ${matchedPrice.minEstimatedPkr.toInt()}-${matchedPrice.maxEstimatedPkr.toInt()} / ${matchedPrice.unit}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HubGreenPrimary
                                        )
                                    }
                                    Text(
                                        text = cat.description,
                                        fontSize = 11.sp,
                                        color = HubTextSecondaryLight,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 6. Why Collection Hub
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Why Collection Hub",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = HubTextPrimaryLight
                )
                WhyFeatureCard(
                    icon = Icons.Default.Lock,
                    title = "Privacy-First Area Matching",
                    body = "Your street address and phone number are hidden from the public pool and shared only with your assigned collector."
                )
                WhyFeatureCard(
                    icon = Icons.Default.VerifiedUser,
                    title = "Admin-Verified Local Kabari Collectors",
                    body = "Every collector on the platform goes through manual review before receiving pickup requests."
                )
                WhyFeatureCard(
                    icon = Icons.Default.Balance,
                    title = "Transparent Physical Weighing & OTP",
                    body = "No automatic price locks — confirm the verified weight and PKR payout on your screen before finalizing."
                )
            }
        }

        // 7. How Collectors Join
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HubGreenContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Are You a Local Kabari Shop or Independent Collector?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = HubGreenDark
                    )
                    Text(
                        text = "Register your business, choose Rawalpindi or Islamabad sectors you cover, toggle online when ready, and accept nearby doorstep pickups on your own schedule.",
                        fontSize = 13.sp,
                        color = HubGreenPrimary,
                        lineHeight = 19.sp
                    )
                    if (isSignedIn) {
                        Button(
                            onClick = onCollectorJoinClick,
                            colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("collector_apply_from_public_btn")
                        ) {
                            Text("Open Collector Portal")
                        }
                    }
                }
            }
        }

        // 8. FAQ
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Frequently Asked Questions",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = HubTextPrimaryLight
                )
                CollectionHubCatalog.publicFaqs.forEachIndexed { idx, (q, a) ->
                    val expanded = expandedFaqIndex == idx
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HubBorderLight, RoundedCornerShape(12.dp))
                            .clickable { expandedFaqIndex = if (expanded) null else idx }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(q, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HubGreenPrimary, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle answer",
                                    tint = HubGreenPrimary
                                )
                            }
                            if (expanded) {
                                Text(a, fontSize = 12.sp, color = HubTextSecondaryLight, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // 9. Contact Us
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, HubBorderLight, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Contact Support — Rawalpindi & Islamabad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Email: $supportEmail", fontSize = 13.sp, color = HubTextSecondaryLight)
                    Text("Phone: $supportPhone", fontSize = 13.sp, color = HubTextSecondaryLight)
                    Text("Launch Hours: Mon–Sat, 09:00 AM – 06:00 PM PKT", fontSize = 12.sp, color = HubTextSecondaryLight)
                }
            }
        }

        // 10. Footer (NO admin link anywhere!)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Privacy Policy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubGreenPrimary,
                        modifier = Modifier.clickable { activePolicyDialog = "Privacy Policy" }
                    )
                    Text(
                        text = "Terms of Service",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubGreenPrimary,
                        modifier = Modifier.clickable { activePolicyDialog = "Terms of Service" }
                    )
                    Text(
                        text = "Manual Payment Policy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubGreenPrimary,
                        modifier = Modifier.clickable { activePolicyDialog = "Manual Payment Policy" }
                    )
                }
                Text(
                    text = "Collection Hub • Your Scrap. Your Collector. Your Collection Hub.",
                    fontSize = 11.sp,
                    color = HubTextSecondaryLight
                )
            }
        }
    }

    if (activePolicyDialog != null) {
        AlertDialog(
            onDismissRequest = { activePolicyDialog = null },
            title = { Text(activePolicyDialog.orEmpty(), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (activePolicyDialog) {
                        "Privacy Policy" -> "Collection Hub displays only your city, sector/area, and scrap categories to nearby online collectors. Exact street address and contact phone are revealed strictly to the single collector who accepts your booking."
                        "Manual Payment Policy" -> "Payments are settled manually in PKR via Cash, Easypaisa, or Bank Transfer. Uploaded reference notes or customer confirmations remain pending until verified when needed; automated payment gateways are not used."
                        else -> "All scrap rates displayed before weighing are indicative ranges. Final amount depends on doorstep inspection, sorting quality, and verified weight confirmed by OTP and customer approval."
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { activePolicyDialog = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PublicStepTile(step: String, title: String, body: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HubBorderLight, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(HubGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HubTextPrimaryLight)
                Text(body, fontSize = 12.sp, color = HubTextSecondaryLight, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun WhyFeatureCard(icon: ImageVector, title: String, body: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HubGreenSoftBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HubGreenContainer, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = HubGreenPrimary, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HubGreenDark)
                Text(body, fontSize = 12.sp, color = HubGreenPrimary, lineHeight = 17.sp)
            }
        }
    }
}

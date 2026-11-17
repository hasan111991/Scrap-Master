package com.example.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.GoogleSignInButton
import com.example.model.ScrapCategory
import com.example.ui.theme.*

/**
 * Public Website & Authentication Gate for Digital Kabariya.
 * Rendered when Firebase.auth.currentUser == null so no Firestore queries run unauthenticated.
 */
@Composable
fun AuthScreen(
    isUrdu: Boolean,
    onToggleUrdu: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var authError by remember { mutableStateOf<String?>(null) }
    var activePolicyDialog by remember { mutableStateOf<String?>(null) }
    var selectedPublicTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Top Public Header & Language Toggle
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(EmeraldGreenDark, EmeraldGreenPrimary)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("♻️", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DIGITAL KABARIYA",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (isUrdu) "راولپنڈی و اسلام آباد • پاکستان" else "Rawalpindi & Islamabad • Pakistan",
                                        color = EmeraldContainer,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onToggleUrdu,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                    .copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("public_lang_toggle")
                            ) {
                                Text(if (isUrdu) "EN" else "اردو", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Hero Headline
                        Text(
                            text = if (isUrdu) "اپنا کباڑ آن لائن بیچیں۔ فوری ادائیگی پائیں!" else "Sell Your Scrap Online. Get Paid Instantly!",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            lineHeight = 30.sp
                        )
                        Text(
                            text = if (isUrdu)
                                "گھر بیٹھے پک اپ بُک کریں، شفاف ریٹس دیکھیں، اور تصدیق شدہ وزن کے بعد فوری نقد یا ایزی پیسہ/راست ادائیگی حاصل کریں۔"
                            else
                                "Book a doorstep pickup, receive transparent scrap rates, and get paid after verified weighing.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        // Google Sign-In Card
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
                                    text = if (isUrdu) "شروع کرنے کے لیے سائن ان کریں" else "Sign In to Book Pickup or Open Portal",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight,
                                    fontSize = 14.sp
                                )
                                GoogleSignInButton(
                                    isUrdu = isUrdu,
                                    onAuthSuccess = {
                                        authError = null
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg -> authError = msg }
                                )
                                if (authError != null) {
                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = authError!!,
                                            color = Color(0xFFB91C1C),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isUrdu)
                                        "محفوظ گوگل تصدیق • کوئی پوشیدہ چارجز نہیں"
                                    else
                                        "Secured by Google Sign-In & Firebase App Check • Free doorstep pickup",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    }
                }
            }

            // Public Website Section Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        if (isUrdu) "طریقہ کار" else "How It Works",
                        if (isUrdu) "اقسام" else "Categories",
                        if (isUrdu) "کاروبار" else "Business & FAQ"
                    )
                    tabs.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedPublicTab == idx,
                            onClick = { selectedPublicTab = idx },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            when (selectedPublicTab) {
                0 -> {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isUrdu) "تین آسان مراحل" else "Three-Step Doorstep Recycling",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            PublicStepCard(
                                step = "1",
                                title = if (isUrdu) "کباڑ منتخب کریں اور وقت طے کریں" else "Select Scrap & Book Pickup",
                                body = if (isUrdu) "کاغذ، گتہ، لوہا، تانبا، پلاسٹک یا ای ویسٹ منتخب کریں اور راولپنڈی/اسلام آباد میں ٹائم سلاٹ بک کریں۔"
                                else "Choose categories, upload a photo for an advisory AI estimate, and pick a convenient time slot."
                            )
                            PublicStepCard(
                                step = "2",
                                title = if (isUrdu) "او ٹی پی تصدیق اور شفاف وزن" else "OTP Verification & Verified Weighing",
                                body = if (isUrdu) "تصدیق شدہ کباڑیا آپ کے دروازے پر آ کر 4 ہندسوں کے او ٹی پی اور ڈیجیٹل کانٹے سے وزن کرتا ہے۔"
                                else "Verified collector arrives, validates your 4-digit OTP, inspects grade, and weighs materials transparently."
                            )
                            PublicStepCard(
                                step = "3",
                                title = if (isUrdu) "فوری ادائیگی اور ڈیجیٹل رسید" else "Instant Payment & Digital Receipt",
                                body = if (isUrdu) "حتمی رقم کی منظوری کے بعد نقد، ایزی پیسہ، جاز کیش یا راست کے ذریعے فوری ادائیگی اور رسید حاصل کریں۔"
                                else "Approve the final verified amount and get paid via Cash, Easypaisa, JazzCash, or Raast with an audit receipt."
                            )
                        }
                    }
                }

                1 -> {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isUrdu) "قابلِ قبول اسکریپ اقسام" else "Accepted Scrap & Recycling Categories",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Surface(color = AmberGoldContainer, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "Estimated prices are advisory only. Live city/zone prices load after sign-in and final payment depends on physical inspection and verified weight.",
                                    fontSize = 11.sp,
                                    color = OnAmberGoldContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            ScrapCategory.entries.chunked(2).forEach { rowCats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowCats.forEach { cat ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(cat.icon, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isUrdu) cat.urduName else cat.displayName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    if (rowCats.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (isUrdu) "گھریلو، کمرشل اور انڈسٹریل سروسز" else "Households, Businesses & Recyclers",
                                        fontWeight = FontWeight.Bold,
                                        color = OnEmeraldContainer
                                    )
                                    Text(
                                        text = "Serving Households, Shops, Offices, Schools, Restaurants, Hospitals, Factories, Warehouses & Housing Societies across Rawalpindi & Islamabad (DHA, Bahria Town, Chaklala, Saddar, Sectors F/G/H/I).",
                                        fontSize = 12.sp,
                                        color = OnEmeraldContainer
                                    )
                                }
                            }

                            PublicFaqItem(
                                q = "Is the AI or app estimate guaranteed?",
                                a = "No. All preliminary estimates are advisory. Final payout is strictly calculated from verified material grade and physical scale weight at pickup."
                            )
                            PublicFaqItem(
                                q = "How are batteries and e-waste handled?",
                                a = "Hazardous items such as UPS/car lead-acid batteries and circuit boards are routed strictly through authorized environmental handlers and never mixed with general scrap."
                            )
                        }
                    }
                }
            }

            // Footer Policies
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Privacy Policy",
                        fontSize = 12.sp,
                        color = EmeraldGreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { activePolicyDialog = "Privacy Policy" }
                    )
                    Text(
                        text = "Terms & Conditions",
                        fontSize = 12.sp,
                        color = EmeraldGreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { activePolicyDialog = "Terms & Conditions" }
                    )
                    Text(
                        text = "Payment Policy",
                        fontSize = 12.sp,
                        color = EmeraldGreenPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { activePolicyDialog = "Payment Policy" }
                    )
                }
            }
        }
    }

    if (activePolicyDialog != null) {
        AlertDialog(
            onDismissRequest = { activePolicyDialog = null },
            title = { Text(activePolicyDialog ?: "") },
            text = {
                Text(
                    when (activePolicyDialog) {
                        "Payment Policy" -> "Payments are processed in PKR via Cash, Bank Transfer, Easypaisa, JazzCash, or Raast only after physical weighing and customer approval. Every transaction creates an immutable ledger entry with a digital receipt."
                        "Privacy Policy" -> "Digital Kabariya collects only essential contact, pickup address, and transaction records protected under strict zero-trust Firestore isolation. Collector visibility is limited to active assigned pickups."
                        else -> "Pickups require 4-digit OTP verification and physical inspection. Hazardous waste (batteries/e-waste) is handled under EPA-compliant channels."
                    },
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { activePolicyDialog = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PublicStepCard(step: String, title: String, body: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PublicFaqItem(q: String, a: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(q, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldGreenPrimary)
            Text(a, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

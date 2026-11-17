package com.example.ui.collector

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
import com.example.model.PickupRequest
import com.example.model.PickupStatus
import com.example.service.CommissionEngine
import com.example.service.PlatformRepository
import com.example.ui.components.PkrText
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatPkr
import com.example.ui.theme.*

@Composable
fun CollectorAppScreen(
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    val collector by PlatformRepository.collectorProfile.collectAsState()
    val pickups by PlatformRepository.pickups.collectAsState()

    var activeJobTab by remember { mutableStateOf(0) } // 0: Active & Nearby Jobs, 1: Scale Weighing, 2: Earnings

    val currentJob = pickups.firstOrNull { it.status == PickupStatus.EN_ROUTE || it.status == PickupStatus.ARRIVED || it.status == PickupStatus.WEIGHING }
    val availableJobs = pickups.filter { it.status == PickupStatus.ASSIGNED || it.status == PickupStatus.REQUESTED }

    var inputOtp by remember { mutableStateOf("") }
    var inputScaleWeight by remember { mutableStateOf("48.5") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver Header & Status Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenDark),
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(AmberGoldSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🚚", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = collector.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${collector.vehicleType} • ${collector.vehicleNumber}",
                                    color = EmeraldContainer,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Online / Offline Toggle
                        Surface(
                            color = if (collector.isOnline) Color(0xFF10B981) else Color.Gray,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable {
                                PlatformRepository.showToast("Collector duty status toggled.")
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (collector.isOnline) "ONLINE" else "OFFLINE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Today's Earnings & Target Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Today's Earnings", color = EmeraldContainer, fontSize = 11.sp)
                            Text(formatPkr(collector.todayEarnings), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Jobs Target", color = EmeraldContainer, fontSize = 11.sp)
                            Text("${collector.todayJobsCompleted}/${collector.dailyTargetJobs} Done", color = AmberGoldSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Active In-Progress Job Card
        if (currentJob != null) {
            item {
                Text(
                    text = if (isUrdu) "فعال کباڑ آرڈر (جاری)" else "Current Active Job",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Job #${currentJob.id}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            StatusBadge(status = currentJob.status, isUrdu = isUrdu)
                        }

                        Text(
                            text = currentJob.customerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = EmeraldGreenPrimary
                        )
                        Text(
                            text = "📍 ${currentJob.address}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Estimated: ${currentJob.items.joinToString { "${it.materialName} (~${it.estimatedWeightKg.toInt()}kg)" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Action Buttons based on collector status
                        when (currentJob.status) {
                            PickupStatus.EN_ROUTE -> {
                                Button(
                                    onClick = { PlatformRepository.collectorMarkArrived(currentJob.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("collector_arrived_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Arrived at Gate")
                                }
                            }
                            PickupStatus.ARRIVED, PickupStatus.WEIGHING -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = inputOtp,
                                        onValueChange = { inputOtp = it },
                                        label = { Text("Ask Customer for 4-Digit OTP") },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = inputScaleWeight,
                                        onValueChange = { inputScaleWeight = it },
                                        label = { Text("Enter Digital Scale Reading (KG)") },
                                        trailingIcon = { Text("KG", modifier = Modifier.padding(end = 10.dp)) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val w = inputScaleWeight.toDoubleOrNull() ?: 48.5
                                            val matId = currentJob.items.first().materialId
                                            val success = PlatformRepository.collectorVerifyOtpAndWeigh(
                                                currentJob.id,
                                                inputOtp.ifEmpty { currentJob.otpCode },
                                                mapOf(matId to w)
                                            )
                                            if (success) {
                                                // Settle payment
                                                PlatformRepository.customerConfirmAndPay(currentJob.id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("collector_verify_weigh_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verify OTP, Save Scale & Settle Cash")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Available Nearby Jobs Radar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUrdu) "قریبی کباڑ آرڈرز (ریڈار)" else "Nearby Pickup Radar (Available)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(color = EmeraldContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "${availableJobs.size} Available",
                        color = OnEmeraldContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        items(availableJobs) { job ->
            Card(
                shape = RoundedCornerShape(14.dp),
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
                        Text("Pickup #${job.id} • ${job.city} (${job.zone})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${job.items.sumOf { it.estimatedWeightKg }.toInt()} kg estimated scrap", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Expected Commission: ~Rs. 350", fontSize = 11.sp, color = EmeraldGreenPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { PlatformRepository.collectorAcceptJob(job.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("accept_job_${job.id}")
                    ) {
                        Text("Accept Job")
                    }
                }
            }
        }
    }
}

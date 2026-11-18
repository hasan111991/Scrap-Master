package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BookingStatus
import com.example.model.PortalRoute
import com.example.ui.theme.*

/**
 * Original vector Collection Hub brand emblem combining a collection pin and recycling arc.
 */
@Composable
fun CollectionHubLogo(
    size: Dp = 40.dp,
    bgColor: Color = HubGreenContainer,
    tint: Color = HubGreenPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = w * 0.13f, cap = StrokeCap.Round)

            // Circular recycling loop
            drawArc(
                color = tint,
                startAngle = -55f,
                sweepAngle = 225f,
                useCenter = false,
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.8f, h * 0.8f),
                style = stroke
            )
            // Secondary loop segment
            drawArc(
                color = HubGreenLight,
                startAngle = 195f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.8f, h * 0.8f),
                style = stroke
            )
            // Center pickup hub node
            val pinPath = Path().apply {
                moveTo(w * 0.5f, h * 0.30f)
                lineTo(w * 0.67f, h * 0.50f)
                lineTo(w * 0.5f, h * 0.72f)
                lineTo(w * 0.33f, h * 0.50f)
                close()
            }
            drawPath(path = pinPath, color = tint)
        }
    }
}

/**
 * Unified top bar for authenticated portals.
 * NEVER renders any Admin Portal navigation to customers or collectors.
 * Admin button only renders when `isVerifiedAdmin == true`.
 */
@Composable
fun CollectionHubHeader(
    activeRoute: PortalRoute,
    isVerifiedAdmin: Boolean,
    unreadNotifications: Int,
    onSelectRoute: (PortalRoute) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = HubGreenPrimary,
        contentColor = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CollectionHubLogo(
                        size = 38.dp,
                        bgColor = Color.White,
                        tint = HubGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Collection Hub",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Rawalpindi & Islamabad",
                            fontSize = 11.sp,
                            color = HubGreenContainer,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (unreadNotifications > 0) {
                        Surface(
                            color = HubGreenLight,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$unreadNotifications",
                                color = HubGreenDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                }
            }

            // Portal switcher: strictly Public / Customer / Collector for normal users;
            // private /hub-admin pill rendered ONLY if verified admin!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PortalSwitchChip(
                    label = "Home",
                    selected = activeRoute == PortalRoute.PUBLIC_HOME,
                    onClick = { onSelectRoute(PortalRoute.PUBLIC_HOME) },
                    testTag = "nav_public_home",
                    modifier = Modifier.weight(1f)
                )
                PortalSwitchChip(
                    label = "Customer",
                    selected = activeRoute == PortalRoute.CUSTOMER_PORTAL,
                    onClick = { onSelectRoute(PortalRoute.CUSTOMER_PORTAL) },
                    testTag = "nav_customer_portal",
                    modifier = Modifier.weight(1f)
                )
                PortalSwitchChip(
                    label = "Collector",
                    selected = activeRoute == PortalRoute.COLLECTOR_PORTAL,
                    onClick = { onSelectRoute(PortalRoute.COLLECTOR_PORTAL) },
                    testTag = "nav_collector_portal",
                    modifier = Modifier.weight(1f)
                )
                if (isVerifiedAdmin) {
                    PortalSwitchChip(
                        label = "Hub Admin",
                        selected = activeRoute == PortalRoute.ADMIN_PORTAL,
                        onClick = { onSelectRoute(PortalRoute.ADMIN_PORTAL) },
                        testTag = "nav_hub_admin_portal",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PortalSwitchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color.White else Color.White.copy(alpha = 0.14f),
        contentColor = if (selected) HubGreenPrimary else Color.White,
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HubBookingStatusBadge(statusCode: String) {
    val status = BookingStatus.fromCode(statusCode)
    val (bg, fg) = when (status) {
        BookingStatus.SEARCHING -> HubAmberContainer to OnHubAmberContainer
        BookingStatus.ASSIGNED -> HubTealContainer to OnHubTealContainer
        BookingStatus.ON_THE_WAY -> Color(0xFFDBEAFE) to InfoBlue
        BookingStatus.ARRIVED -> HubGreenContainer to HubGreenPrimary
        BookingStatus.WEIGHT_PRICE_CONFIRMATION -> HubAmberContainer to OnHubAmberContainer
        BookingStatus.COMPLETED -> HubGreenContainer to SuccessGreen
        BookingStatus.CANCELLED -> ErrorContainer to ErrorRed
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = status.label,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun IndicativePriceDisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        color = HubGreenSoftBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HubGreenContainer, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Pricing disclaimer",
                tint = HubGreenPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Rates shown are indicative/estimated ranges only. Final payout is strictly calculated at pickup after physical inspection and verified weight.",
                fontSize = 12.sp,
                color = HubGreenPrimary,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun HubEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HubBorderLight, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(HubGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = HubGreenPrimary, modifier = Modifier.size(26.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HubTextPrimaryLight)
            Text(subtitle, fontSize = 13.sp, color = HubTextSecondaryLight)
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = HubGreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun HubSectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HubBorderLight, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HubTextPrimaryLight)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = HubTextSecondaryLight)
                }
            }
            content()
        }
    }
}

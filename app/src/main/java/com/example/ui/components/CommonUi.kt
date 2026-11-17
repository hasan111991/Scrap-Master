package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PickupStatus
import com.example.model.UserRole
import com.example.service.PlatformRepository
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DigitalKabariyaHeader(
    currentRole: UserRole,
    isUrdu: Boolean,
    onRoleClick: () -> Unit,
    onUrduToggle: () -> Unit,
    onSignOutClick: (() -> Unit)? = null,
    activePortalLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = EmeraldGreenDark,
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand and Tagline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♻️",
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DIGITAL KABARIYA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AmberGoldSecondary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isUrdu) "کباڑ بیچیں • رقم کمائیں • ماحول بچائیں" else "Sell Scrap. Get Paid. Recycle Better.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EmeraldContainer.copy(alpha = 0.9f),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls: Urdu Toggle & Role Selector Chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language Switcher
                    OutlinedButton(
                        onClick = onUrduToggle,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenLight)),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("lang_toggle_btn")
                    ) {
                        Text(
                            text = if (isUrdu) "EN" else "اردو",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Portal Role Selector Chip
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onRoleClick() }
                            .testTag("portal_selector_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = when (currentRole) {
                                    UserRole.CUSTOMER -> Icons.Default.Person
                                    UserRole.COLLECTOR -> Icons.Default.LocalShipping
                                    UserRole.HUB_OPERATOR -> Icons.Default.Warehouse
                                    UserRole.RECYCLER -> Icons.Default.Storefront
                                    UserRole.CORPORATE_CLIENT -> Icons.Default.Business
                                    else -> Icons.Default.AdminPanelSettings
                                },
                                contentDescription = "Role",
                                tint = AmberGoldSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activePortalLabel ?: if (isUrdu) currentRole.urduLabel else currentRole.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (onSignOutClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onSignOutClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sign_out_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = if (isUrdu) "سائن آؤٹ" else "Sign out",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSwitcherDialog(
    selectedRole: UserRole,
    isUrdu: Boolean,
    onRoleSelected: (UserRole) -> Unit,
    onSelectExtendedPortal: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isUrdu) "پورٹل اور کردار تبدیل کریں" else "Switch Portal & Role",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUrdu) "ڈیجیٹل کباڑیا کے تمام 13 پورٹلز تک رسائی حاصل کریں" else "Access any of Digital Kabariya's 13 role-based portals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserRole.entries.forEach { role ->
                    val isCurrent = role == selectedRole
                    Surface(
                        color = if (isCurrent) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isCurrent) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreenPrimary)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onRoleSelected(role)
                                onDismiss()
                            }
                            .testTag("role_item_${role.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) EmeraldGreenPrimary else Color.Gray.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (role) {
                                            UserRole.CUSTOMER -> Icons.Default.Person
                                            UserRole.COLLECTOR -> Icons.Default.LocalShipping
                                            UserRole.HUB_OPERATOR -> Icons.Default.Warehouse
                                            UserRole.RECYCLER -> Icons.Default.Storefront
                                            UserRole.CORPORATE_CLIENT -> Icons.Default.Business
                                            else -> Icons.Default.Security
                                        },
                                        contentDescription = null,
                                        tint = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isUrdu) role.urduLabel else role.label,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) OnEmeraldContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = getRoleSubtext(role, isUrdu),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = EmeraldGreenPrimary
                                )
                            }
                        }
                    }
                }

                if (onSelectExtendedPortal != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (isUrdu) "خصوصی انٹرپرائز پورٹلز" else "Dedicated Enterprise Portals",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldGreenPrimary
                    )
                    val extraPortals = listOf(
                        "INVENTORY" to "Inventory Portal (Firestore SKUs)",
                        "OPERATIONS" to "Operations & Zone Dispatch",
                        "FINANCE" to "Finance & Settlement Portal",
                        "PRICING" to "Pricing Management Portal",
                        "SUPPORT" to "Customer Support Portal",
                        "FRANCHISE" to "Cities & Franchise Network"
                    )
                    extraPortals.forEach { (key, title) ->
                        OutlinedButton(
                            onClick = {
                                onSelectExtendedPortal(key)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("portal_shortcut_$key")
                        ) {
                            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_role_dialog_btn")
            ) {
                Text(if (isUrdu) "بند کریں" else "Close")
            }
        }
    )
}

private fun getRoleSubtext(role: UserRole, isUrdu: Boolean): String {
    return if (isUrdu) {
        when (role) {
            UserRole.CUSTOMER -> "کباڑ فروخت کریں اور کیش یا والٹ میں رقم لیں"
            UserRole.COLLECTOR -> "قریبی آرڈرز قبول کریں اور کمیشن کمائیں"
            UserRole.HUB_OPERATOR -> "اسکریپ ان باؤنڈ وصولی، وزن اور چھانٹی"
            UserRole.RECYCLER -> "تھوک اسکریپ لاٹس پر بولی لگائیں اور خریدیں"
            UserRole.CORPORATE_CLIENT -> "ملٹیپل برانچز کا کچرا اور ری سائیکلنگ سرٹیفکیٹ"
            UserRole.ADMIN -> "آپریشنز، قیمتیں، لائیو پک اپس اور والٹ"
            UserRole.SUPER_ADMIN -> "مکمل سیکیورٹی، سسٹم سیٹنگز اور آڈٹ لاگز"
        }
    } else {
        when (role) {
            UserRole.CUSTOMER -> "Sell scrap, schedule pickup & get paid via Easypaisa/Cash"
            UserRole.COLLECTOR -> "Accept nearby jobs, physical weighing & earn commissions"
            UserRole.HUB_OPERATOR -> "Inbound scrap receiving, scale variance audit & sorting"
            UserRole.RECYCLER -> "B2B wholesale scrap lots, bidding & purchase orders"
            UserRole.CORPORATE_CLIENT -> "Multi-branch waste tracking & ESG recycling certificates"
            UserRole.ADMIN -> "Operations, dynamic pricing, real-time dispatch & finance"
            UserRole.SUPER_ADMIN -> "Enterprise governance, RBAC permissions & audit logs"
        }
    }
}

@Composable
fun StatusBadge(
    status: PickupStatus,
    isUrdu: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        PickupStatus.REQUESTED -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
        PickupStatus.ASSIGNED -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        PickupStatus.EN_ROUTE -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        PickupStatus.ARRIVED -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
        PickupStatus.WEIGHING -> Color(0xFFFFFBEB) to Color(0xFFD97706)
        PickupStatus.CONFIRMED -> Color(0xFFECFDF5) to Color(0xFF059669)
        PickupStatus.PAID -> Color(0xFFD1FAE5) to Color(0xFF047857)
        PickupStatus.RECEIVED_AT_HUB -> Color(0xFFE0E7FF) to Color(0xFF4338CA)
        PickupStatus.SORTED -> Color(0xFFF1F5F9) to Color(0xFF334155)
        PickupStatus.COMPLETED -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        PickupStatus.CANCELLED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Text(
            text = if (isUrdu) status.urduTitle else status.title,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun formatPkr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "Rs. ${formatter.format(amount.toInt())}"
}

@Composable
fun PkrText(
    amount: Double,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = EmeraldGreenPrimary,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatPkr(amount),
        style = style.copy(fontWeight = FontWeight.Bold, color = color),
        modifier = modifier
    )
}

@Composable
fun ToastNotification(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        message?.let { text ->
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

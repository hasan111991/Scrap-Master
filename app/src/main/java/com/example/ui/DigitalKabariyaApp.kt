package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.FirestorePlatformRepository
import com.example.data.repository.ScrapInventoryRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.signOut
import com.example.model.PickupRequest
import com.example.model.ScrapCategory
import com.example.model.UserRole
import com.example.service.PlatformRepository
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.ai.AiAssistantModal
import com.example.ui.ai.AiAssistantViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.collector.CollectorAppScreen
import com.example.ui.components.DigitalKabariyaHeader
import com.example.ui.components.RoleSwitcherDialog
import com.example.ui.components.ToastNotification
import com.example.ui.corporate.CorporatePortalScreen
import com.example.ui.customer.*
import com.example.ui.hub.CollectionHubScreen
import com.example.ui.inventory.ScrapInventoryScreen
import com.example.ui.inventory.ScrapInventoryViewModel
import com.example.ui.portals.ExtendedPortalTab
import com.example.ui.portals.ExtendedPortalsScreen
import com.example.ui.recycler.RecyclerMarketplaceScreen
import com.example.ui.theme.EmeraldGreenPrimary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

sealed class CustomerScreenRoute {
    data object Home : CustomerScreenRoute()
    data class SellScrap(val initialCategory: ScrapCategory = ScrapCategory.IRON_STEEL) : CustomerScreenRoute()
    data object Rates : CustomerScreenRoute()
    data object Wallet : CustomerScreenRoute()
    data class ActivePickup(val pickup: PickupRequest) : CustomerScreenRoute()
    data object EidCampaign : CustomerScreenRoute()
    data object Services : CustomerScreenRoute()
}

private sealed class ActiveDestination {
    data class RolePortal(val role: UserRole) : ActiveDestination()
    data object InventoryPortal : ActiveDestination()
    data class ExtendedPortal(val tab: ExtendedPortalTab) : ActiveDestination()
}

@Composable
fun DigitalKabariyaApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    val isUrdu by PlatformRepository.isUrdu.collectAsState()
    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            isUrdu = isUrdu,
            onToggleUrdu = { PlatformRepository.toggleLanguage() },
            onAuthSuccess = {
                PlatformRepository.showToast(
                    if (isUrdu) "خوش آمدید! ڈیجیٹل کباڑیا میں سائن ان مکمل ہوا"
                    else "Signed in to Digital Kabariya!"
                )
            },
            modifier = modifier
        )
    } else {
        key(user.uid) {
            AuthenticatedDigitalKabariyaShell(
                user = user,
                credentialManager = credentialManager,
                isUrdu = isUrdu,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AuthenticatedDigitalKabariyaShell(
    user: FirebaseUser,
    credentialManager: CredentialManager,
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val inventoryViewModel: ScrapInventoryViewModel = viewModel(
        key = "inv_${user.uid}",
        factory = viewModelFactory {
            initializer {
                ScrapInventoryViewModel(
                    repository = ScrapInventoryRepository(context.applicationContext),
                    currentUserId = user.uid
                )
            }
        }
    )

    val syncViewModel: PlatformSyncViewModel = viewModel(
        key = "sync_${user.uid}",
        factory = viewModelFactory {
            initializer {
                PlatformSyncViewModel(
                    repository = FirestorePlatformRepository(context.applicationContext),
                    currentUserId = user.uid,
                    displayName = user.displayName ?: "Digital Kabariya User",
                    email = user.email
                )
            }
        }
    )

    val aiViewModel: AiAssistantViewModel = viewModel(key = "ai_${user.uid}")

    val currentRole by PlatformRepository.currentRole.collectAsState()
    val localToast by PlatformRepository.systemToast.collectAsState()
    val syncState by syncViewModel.uiState.collectAsStateWithLifecycle()
    val cloudActionToast by syncViewModel.actionMessage.collectAsStateWithLifecycle()
    val activeToast = cloudActionToast ?: localToast

    var showRoleDialog by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var customerRoute by remember { mutableStateOf<CustomerScreenRoute>(CustomerScreenRoute.Home) }
    var activeDestination by remember { mutableStateOf<ActiveDestination>(ActiveDestination.RolePortal(currentRole)) }

    // Keep RolePortal aligned if role switches from elsewhere
    LaunchedEffect(currentRole) {
        if (activeDestination is ActiveDestination.RolePortal) {
            activeDestination = ActiveDestination.RolePortal(currentRole)
        }
    }

    // Back handling for secondary destinations and customer sub-screens
    val hasBackTarget = when (val dest = activeDestination) {
        is ActiveDestination.RolePortal -> dest.role == UserRole.CUSTOMER && customerRoute != CustomerScreenRoute.Home
        is ActiveDestination.InventoryPortal,
        is ActiveDestination.ExtendedPortal -> true
    }

    BackHandler(enabled = hasBackTarget) {
        when (activeDestination) {
            is ActiveDestination.RolePortal -> {
                if (customerRoute != CustomerScreenRoute.Home) {
                    customerRoute = CustomerScreenRoute.Home
                }
            }
            is ActiveDestination.InventoryPortal,
            is ActiveDestination.ExtendedPortal -> {
                activeDestination = ActiveDestination.RolePortal(currentRole)
            }
        }
    }

    val activePortalLabel = when (val dest = activeDestination) {
        is ActiveDestination.RolePortal -> if (isUrdu) dest.role.urduLabel else dest.role.label
        is ActiveDestination.InventoryPortal -> if (isUrdu) "گودام انوینٹری" else "Inventory"
        is ActiveDestination.ExtendedPortal -> if (isUrdu) dest.tab.urduTitle else dest.tab.title
    }

    Scaffold(
        topBar = {
            DigitalKabariyaHeader(
                currentRole = currentRole,
                isUrdu = isUrdu,
                onRoleClick = { showRoleDialog = true },
                onUrduToggle = { PlatformRepository.toggleLanguage() },
                activePortalLabel = activePortalLabel,
                onSignOutClick = {
                    signOut(
                        credentialManager = credentialManager,
                        onSignOutComplete = {
                            PlatformRepository.showToast("Signed out safely.")
                        },
                        scope = coroutineScope
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiAssistant = true },
                containerColor = EmeraldGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("open_ai_assistant_fab")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Scrap Assistant")
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isUrdu) "اے آئی مدد" else "AI Scan & Help")
            }
        },
        snackbarHost = {
            ToastNotification(
                message = activeToast,
                onDismiss = {
                    syncViewModel.clearActionMessage()
                    PlatformRepository.clearToast()
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val dest = activeDestination) {
                is ActiveDestination.InventoryPortal -> {
                    ScrapInventoryScreen(
                        viewModel = inventoryViewModel,
                        isUrdu = isUrdu
                    )
                }

                is ActiveDestination.ExtendedPortal -> {
                    ExtendedPortalsScreen(
                        initialTab = dest.tab,
                        isUrdu = isUrdu,
                        syncViewModel = syncViewModel,
                        onBackToHome = {
                            activeDestination = ActiveDestination.RolePortal(UserRole.CUSTOMER)
                            customerRoute = CustomerScreenRoute.Home
                        }
                    )
                }

                is ActiveDestination.RolePortal -> {
                    when (dest.role) {
                        UserRole.CUSTOMER -> {
                            when (val route = customerRoute) {
                                is CustomerScreenRoute.Home -> {
                                    CustomerHomeScreen(
                                        isUrdu = isUrdu,
                                        onNavigateSell = { cat -> customerRoute = CustomerScreenRoute.SellScrap(cat) },
                                        onNavigateRates = { customerRoute = CustomerScreenRoute.Rates },
                                        onNavigateWallet = { customerRoute = CustomerScreenRoute.Wallet },
                                        onNavigateActivePickup = { p -> customerRoute = CustomerScreenRoute.ActivePickup(p) },
                                        onNavigateEidCampaign = { customerRoute = CustomerScreenRoute.EidCampaign },
                                        onNavigateServices = { customerRoute = CustomerScreenRoute.Services },
                                        cloudPickups = syncState.pickups
                                    )
                                }
                                is CustomerScreenRoute.SellScrap -> {
                                    SellScrapScreen(
                                        isUrdu = isUrdu,
                                        onBack = { customerRoute = CustomerScreenRoute.Home },
                                        initialCategory = route.initialCategory,
                                        onSyncPickupToCloud = { city, zone, address, itemsSummary, w, total, date, slot, pay, aiNote ->
                                            syncViewModel.syncPickupToFirestore(
                                                city = city,
                                                zone = zone,
                                                address = address,
                                                itemsSummary = itemsSummary,
                                                estimatedWeightKg = w,
                                                estimatedTotalPkr = total,
                                                scheduledDate = date,
                                                timeSlot = slot,
                                                paymentMethod = pay,
                                                aiConfidenceNote = aiNote
                                            )
                                        }
                                    )
                                }
                                is CustomerScreenRoute.Rates -> {
                                    CurrentRatesScreen(
                                        isUrdu = isUrdu,
                                        onBack = { customerRoute = CustomerScreenRoute.Home }
                                    )
                                }
                                is CustomerScreenRoute.Wallet -> {
                                    CustomerWalletScreen(
                                        isUrdu = isUrdu,
                                        onBack = { customerRoute = CustomerScreenRoute.Home },
                                        cloudLedger = syncState.walletLedger
                                    )
                                }
                                is CustomerScreenRoute.ActivePickup -> {
                                    ActivePickupScreen(
                                        pickup = route.pickup,
                                        isUrdu = isUrdu,
                                        onBack = { customerRoute = CustomerScreenRoute.Home }
                                    )
                                }
                                is CustomerScreenRoute.EidCampaign -> {
                                    SellScrapScreen(
                                        isUrdu = isUrdu,
                                        onBack = { customerRoute = CustomerScreenRoute.Home },
                                        initialCategory = ScrapCategory.EID_QURBANI,
                                        onSyncPickupToCloud = { city, zone, address, itemsSummary, w, total, date, slot, pay, aiNote ->
                                            syncViewModel.syncPickupToFirestore(
                                                city = city,
                                                zone = zone,
                                                address = address,
                                                itemsSummary = itemsSummary,
                                                estimatedWeightKg = w,
                                                estimatedTotalPkr = total,
                                                scheduledDate = date,
                                                timeSlot = slot,
                                                paymentMethod = pay,
                                                aiConfidenceNote = aiNote
                                            )
                                        }
                                    )
                                }
                                is CustomerScreenRoute.Services -> {
                                    CustomerServicesScreen(
                                        isUrdu = isUrdu,
                                        userDisplayName = user.displayName ?: "Digital Kabariya User",
                                        userEmail = user.email,
                                        syncViewModel = syncViewModel,
                                        onToggleUrdu = { PlatformRepository.toggleLanguage() },
                                        onOpenSupportPortal = {
                                            activeDestination = ActiveDestination.ExtendedPortal(ExtendedPortalTab.SUPPORT)
                                        },
                                        onBack = { customerRoute = CustomerScreenRoute.Home }
                                    )
                                }
                            }
                        }
                        UserRole.COLLECTOR -> {
                            CollectorAppScreen(isUrdu = isUrdu)
                        }
                        UserRole.HUB_OPERATOR -> {
                            CollectionHubScreen(
                                isUrdu = isUrdu,
                                inventoryViewModel = inventoryViewModel
                            )
                        }
                        UserRole.RECYCLER -> {
                            RecyclerMarketplaceScreen(isUrdu = isUrdu)
                        }
                        UserRole.CORPORATE_CLIENT -> {
                            CorporatePortalScreen(isUrdu = isUrdu)
                        }
                        UserRole.ADMIN -> {
                            AdminDashboardScreen(isUrdu = isUrdu)
                        }
                        UserRole.SUPER_ADMIN -> {
                            ExtendedPortalsScreen(
                                initialTab = ExtendedPortalTab.SUPER_ADMIN,
                                isUrdu = isUrdu,
                                syncViewModel = syncViewModel,
                                onBackToHome = {
                                    PlatformRepository.switchRole(UserRole.CUSTOMER)
                                    activeDestination = ActiveDestination.RolePortal(UserRole.CUSTOMER)
                                    customerRoute = CustomerScreenRoute.Home
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showRoleDialog) {
            RoleSwitcherDialog(
                selectedRole = currentRole,
                isUrdu = isUrdu,
                onRoleSelected = { newRole ->
                    PlatformRepository.switchRole(newRole)
                    activeDestination = ActiveDestination.RolePortal(newRole)
                    customerRoute = CustomerScreenRoute.Home
                },
                onSelectExtendedPortal = { portalKey ->
                    activeDestination = when (portalKey) {
                        "INVENTORY" -> ActiveDestination.InventoryPortal
                        "OPERATIONS" -> ActiveDestination.ExtendedPortal(ExtendedPortalTab.OPERATIONS)
                        "FINANCE" -> ActiveDestination.ExtendedPortal(ExtendedPortalTab.FINANCE)
                        "PRICING" -> ActiveDestination.ExtendedPortal(ExtendedPortalTab.PRICING)
                        "SUPPORT" -> ActiveDestination.ExtendedPortal(ExtendedPortalTab.SUPPORT)
                        "FRANCHISE" -> ActiveDestination.ExtendedPortal(ExtendedPortalTab.FRANCHISE)
                        else -> ActiveDestination.ExtendedPortal(ExtendedPortalTab.SUPER_ADMIN)
                    }
                },
                onDismiss = { showRoleDialog = false }
            )
        }

        if (showAiAssistant) {
            AiAssistantModal(
                viewModel = aiViewModel,
                isUrdu = isUrdu,
                currentRole = currentRole,
                onDismiss = { showAiAssistant = false }
            )
        }
    }
}

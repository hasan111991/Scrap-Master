package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.CollectionHubCatalog
import com.example.data.repository.CollectionHubAdminRepository
import com.example.data.repository.CollectionHubRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.signOut
import com.example.model.PortalRoute
import com.example.ui.admin.AdminPortalScreen
import com.example.ui.collector.CollectorPortalScreen
import com.example.ui.components.CollectionHubHeader
import com.example.ui.customer.CustomerPortalScreen
import com.example.ui.publicsite.PublicWebsiteScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

@Composable
fun CollectionHubApp(
    initialRoute: PortalRoute = PortalRoute.CUSTOMER_PORTAL,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

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
        PublicWebsiteScreen(
            isSignedIn = false,
            categories = CollectionHubCatalog.initialCategories,
            indicativePrices = CollectionHubCatalog.seedIndicativePriceSeeds,
            supportEmail = CollectionHubCatalog.initialDefaultSettings.supportEmail,
            supportPhone = CollectionHubCatalog.initialDefaultSettings.supportPhone,
            onBookPickupClick = {},
            onCollectorJoinClick = {},
            onAuthSuccess = {},
            modifier = modifier
        )
    } else {
        key(user.uid) {
            AuthenticatedCollectionHubShell(
                user = user,
                credentialManager = credentialManager,
                initialRoute = initialRoute,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AuthenticatedCollectionHubShell(
    user: FirebaseUser,
    credentialManager: CredentialManager,
    initialRoute: PortalRoute,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: CollectionHubViewModel = viewModel(
        key = "ch_vm_${user.uid}",
        factory = viewModelFactory {
            initializer {
                CollectionHubViewModel(
                    repo = CollectionHubRepository(context.applicationContext),
                    adminRepo = CollectionHubAdminRepository(context.applicationContext),
                    currentUserId = user.uid,
                    displayName = user.displayName ?: "Collection Hub Member",
                    email = user.email,
                    emailVerified = user.isEmailVerified
                )
            }
        }
    )

    val isVerifiedAdmin by viewModel.isVerifiedAdmin.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val customerProfile by viewModel.customerProfile.collectAsStateWithLifecycle()
    val collectorProfile by viewModel.collectorProfile.collectAsStateWithLifecycle()
    val categories by viewModel.scrapCategories.collectAsStateWithLifecycle()
    val prices by viewModel.scrapPrices.collectAsStateWithLifecycle()
    val serviceAreas by viewModel.serviceAreas.collectAsStateWithLifecycle()
    val globalSettings by viewModel.globalSettings.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val customerBookings by viewModel.customerBookings.collectAsStateWithLifecycle()
    val customerComplaints by viewModel.customerComplaints.collectAsStateWithLifecycle()
    val openPool by viewModel.openPoolForCollector.collectAsStateWithLifecycle()
    val assignedBookings by viewModel.collectorAssignedBookings.collectAsStateWithLifecycle()
    val commissions by viewModel.collectorCommissions.collectAsStateWithLifecycle()
    val settlements by viewModel.collectorSettlements.collectAsStateWithLifecycle()
    val adminState by viewModel.adminState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var activeRoute by remember(initialRoute) {
        mutableStateOf(
            if (initialRoute == PortalRoute.ADMIN_PORTAL) PortalRoute.CUSTOMER_PORTAL else initialRoute
        )
    }
    var openBookingWizardTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialRoute, isVerifiedAdmin) {
        if (initialRoute == PortalRoute.ADMIN_PORTAL && isVerifiedAdmin) {
            activeRoute = PortalRoute.ADMIN_PORTAL
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    BackHandler(enabled = activeRoute != PortalRoute.CUSTOMER_PORTAL) {
        activeRoute = PortalRoute.CUSTOMER_PORTAL
    }

    val unreadCount = notifications.count { !it.read }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CollectionHubHeader(
                activeRoute = activeRoute,
                isVerifiedAdmin = isVerifiedAdmin,
                unreadNotifications = unreadCount,
                onSelectRoute = { nextRoute ->
                    if (nextRoute == PortalRoute.ADMIN_PORTAL && !isVerifiedAdmin) {
                        viewModel.showToast("Admin access is restricted.")
                    } else {
                        activeRoute = nextRoute
                    }
                },
                onSignOut = {
                    signOut(
                        credentialManager = credentialManager,
                        onSignOutComplete = {},
                        scope = coroutineScope
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeRoute) {
                PortalRoute.PUBLIC_HOME -> PublicWebsiteScreen(
                    isSignedIn = true,
                    categories = categories,
                    indicativePrices = prices,
                    supportEmail = globalSettings.supportEmail,
                    supportPhone = globalSettings.supportPhone,
                    onBookPickupClick = {
                        openBookingWizardTrigger++
                        activeRoute = PortalRoute.CUSTOMER_PORTAL
                    },
                    onCollectorJoinClick = {
                        activeRoute = PortalRoute.COLLECTOR_PORTAL
                    },
                    onAuthSuccess = {}
                )

                PortalRoute.CUSTOMER_PORTAL -> CustomerPortalScreen(
                    viewModel = viewModel,
                    categories = categories,
                    prices = prices,
                    bookings = customerBookings,
                    notifications = notifications,
                    complaints = customerComplaints,
                    customerProfile = customerProfile,
                    userAccount = userAccount,
                    isBusy = isBusy,
                    openBookingWizardTrigger = openBookingWizardTrigger
                )

                PortalRoute.COLLECTOR_PORTAL -> CollectorPortalScreen(
                    viewModel = viewModel,
                    collectorProfile = collectorProfile,
                    openPool = openPool,
                    assignedBookings = assignedBookings,
                    commissions = commissions,
                    settlements = settlements,
                    categories = categories,
                    isBusy = isBusy
                )

                PortalRoute.ADMIN_PORTAL -> AdminPortalScreen(
                    isVerifiedAdmin = isVerifiedAdmin,
                    viewModel = viewModel,
                    adminState = adminState,
                    categories = categories,
                    prices = prices,
                    serviceAreas = serviceAreas,
                    globalSettings = globalSettings
                )
            }
        }
    }
}

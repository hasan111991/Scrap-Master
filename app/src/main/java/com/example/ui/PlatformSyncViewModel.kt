package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FirestorePlatformRepository
import com.example.data.repository.FsMarketplaceLot
import com.example.data.repository.FsPickupRequest
import com.example.data.repository.FsPricingRule
import com.example.data.repository.FsSupportTicket
import com.example.data.repository.FsUserProfile
import com.example.data.repository.FsWalletLedgerEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlatformSyncUiState(
    val isLoading: Boolean = true,
    val userProfile: FsUserProfile? = null,
    val pickups: List<FsPickupRequest> = emptyList(),
    val walletLedger: List<FsWalletLedgerEntry> = emptyList(),
    val pricingRules: List<FsPricingRule> = emptyList(),
    val marketplaceLots: List<FsMarketplaceLot> = emptyList(),
    val supportTickets: List<FsSupportTicket> = emptyList(),
    val syncError: String? = null
)

/**
 * Authenticated-only ViewModel syncing Digital Kabariya user profile, pickups,
 * wallet ledger, dynamic pricing rules, marketplace lots, and support tickets with Firestore.
 */
class PlatformSyncViewModel(
    private val repository: FirestorePlatformRepository,
    private val currentUserId: String,
    private val displayName: String,
    private val email: String?
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val result = repository.ensureUserProfile(
                displayName = displayName,
                email = email
            )
            result.onFailure { err ->
                Log.w(TAG, "Profile bootstrap failed: ${err.message}", err)
            }
        }
    }

    private fun <T> unwrapFirestoreError(error: Throwable, fallback: T): T {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Firestore stream warning", firestoreError ?: error)
        return fallback
    }

    private val profileFlow = repository.observeUserProfile(currentUserId)
        .catch { emit(unwrapFirestoreError(it, null)) }

    private val pickupsFlow = repository.observeCustomerPickups(currentUserId)
        .catch { emit(unwrapFirestoreError(it, emptyList())) }

    private val ledgerFlow = repository.observeWalletLedger(currentUserId)
        .catch { emit(unwrapFirestoreError(it, emptyList())) }

    private val pricingFlow = repository.observeActivePricingRules()
        .catch { emit(unwrapFirestoreError(it, emptyList())) }

    private val lotsFlow = repository.observeAvailableMarketplaceLots()
        .catch { emit(unwrapFirestoreError(it, emptyList())) }

    private val ticketsFlow = repository.observeSupportTickets(currentUserId)
        .catch { emit(unwrapFirestoreError(it, emptyList())) }

    val uiState: StateFlow<PlatformSyncUiState> = combine(
        profileFlow,
        pickupsFlow,
        ledgerFlow,
        pricingFlow,
        combine(lotsFlow, ticketsFlow) { lots, tickets -> lots to tickets }
    ) { profile, pickups, ledger, pricing, lotsAndTickets ->
        PlatformSyncUiState(
            isLoading = false,
            userProfile = profile,
            pickups = pickups,
            walletLedger = ledger,
            pricingRules = pricing,
            marketplaceLots = lotsAndTickets.first,
            supportTickets = lotsAndTickets.second,
            syncError = null
        )
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(
            PlatformSyncUiState(
                isLoading = false,
                syncError = (firestoreError ?: error).localizedMessage ?: "Cloud sync error"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PlatformSyncUiState(isLoading = true)
    )

    fun syncPickupToFirestore(
        city: String,
        zone: String,
        address: String,
        itemsSummary: String,
        estimatedWeightKg: Double,
        estimatedTotalPkr: Double,
        scheduledDate: String,
        timeSlot: String,
        paymentMethod: String,
        aiConfidenceNote: String
    ) {
        viewModelScope.launch {
            val result = repository.createPickup(
                FsPickupRequest(
                    userId = currentUserId,
                    customerName = displayName.ifBlank { "Customer" },
                    city = city,
                    zone = zone,
                    address = address,
                    itemsSummary = itemsSummary,
                    estimatedWeightKg = estimatedWeightKg,
                    estimatedTotalPkr = estimatedTotalPkr,
                    scheduledDate = scheduledDate,
                    timeSlot = timeSlot,
                    paymentMethod = paymentMethod,
                    collectorId = currentUserId,
                    aiConfidenceNote = aiConfidenceNote
                )
            )
            result.fold(
                onSuccess = { id -> _actionMessage.value = "Pickup synced to Cloud Firestore ($id)" },
                onFailure = { err -> _actionMessage.value = "Cloud pickup sync warning: ${err.localizedMessage}" }
            )
        }
    }

    fun recordLedgerTransaction(
        amount: Double,
        type: String,
        category: String,
        referenceId: String,
        balanceAfter: Double,
        description: String
    ) {
        viewModelScope.launch {
            val result = repository.recordLedgerTransaction(
                FsWalletLedgerEntry(
                    userId = currentUserId,
                    amount = amount,
                    type = type,
                    category = category,
                    referenceId = referenceId,
                    balanceAfter = balanceAfter,
                    description = description
                )
            )
            result.onFailure { err ->
                _actionMessage.value = "Ledger sync note: ${err.localizedMessage}"
            }
        }
    }

    fun submitSupportTicket(
        category: String,
        subject: String,
        message: String
    ) {
        if (subject.trim().length < 2 || message.trim().length < 5) {
            _actionMessage.value = "Please enter a valid subject and detailed message (min 5 chars)."
            return
        }
        viewModelScope.launch {
            val result = repository.createSupportTicket(
                userName = displayName.ifBlank { "Customer" },
                category = category,
                subject = subject.trim(),
                message = message.trim()
            )
            result.fold(
                onSuccess = { id -> _actionMessage.value = "Support Ticket #$id saved to Firestore!" },
                onFailure = { err -> _actionMessage.value = "Ticket submission failed: ${err.localizedMessage}" }
            )
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "PlatformSyncVM"
    }
}

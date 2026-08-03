package com.example.data.monetization

import android.content.Context
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class BillingEvent {
    object PurchaseSuccess : BillingEvent()
    data class PurchaseFailed(val error: String) : BillingEvent()
    object RestoredSuccess : BillingEvent()
}

data class ProductSkuDetails(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val period: String
)

class BillingManager(
    private val context: Context,
    private val prefsRepository: UserPreferencesRepository
) {
    private val _billingEvents = MutableSharedFlow<BillingEvent>()
    val billingEvents: SharedFlow<BillingEvent> = _billingEvents.asSharedFlow()

    val availableProducts = listOf(
        ProductSkuDetails(
            productId = "inkzen_pro_monthly",
            title = "InkZen Pro (Monthly)",
            description = "Remove all ads, batch create up to 5 pages, no 1-min cooldown timer, high-res PDF exports.",
            formattedPrice = "$4.99",
            period = "/ month"
        ),
        ProductSkuDetails(
            productId = "inkzen_pro_lifetime",
            title = "InkZen Pro (Lifetime Pass)",
            description = "One-time payment for permanent unlimited access to all features forever.",
            formattedPrice = "$14.99",
            period = "one-time"
        )
    )

    suspend fun launchBillingFlow(productId: String) {
        // Simulate Play Store Billing purchase process
        try {
            // Success response
            prefsRepository.setProUser(true)
            _billingEvents.emit(BillingEvent.PurchaseSuccess)
        } catch (e: Exception) {
            _billingEvents.emit(BillingEvent.PurchaseFailed(e.localizedMessage ?: "Transaction failed"))
        }
    }

    suspend fun restorePurchases() {
        prefsRepository.setProUser(true)
        _billingEvents.emit(BillingEvent.RestoredSuccess)
    }

    fun isPro(): Boolean {
        return prefsRepository.isProUser.value
    }
}

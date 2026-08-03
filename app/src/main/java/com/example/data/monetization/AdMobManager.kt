package com.example.data.monetization

import android.content.Context

object AdMobManager {
    // Official Google AdMob Test Ad Unit IDs
    const val SAMPLE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val SAMPLE_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val SAMPLE_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    fun shouldShowAds(isPro: Boolean): Boolean {
        return !isPro
    }
}

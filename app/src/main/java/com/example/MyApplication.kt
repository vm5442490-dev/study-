package com.example

import android.app.Application
import com.example.util.InterstitialAdManager
import com.google.android.gms.ads.MobileAds

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the Google Mobile Ads SDK
        MobileAds.initialize(this) {}
        
        // Pre-load Interstitial Ad
        InterstitialAdManager.loadAd(this)
    }
}
package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {
    private var mInterstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var lastAdShownTime = 0L
    private const val AD_UNIT_ID = "ca-app-pub-3665825190622425/3369282791" // Real ID

    fun loadAd(context: Context) {
        if (mInterstitialAd != null || isLoadingAd) {
            return
        }

        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    isLoadingAd = false
                    Log.d("InterstitialAdManager", "Failed to load ad: ${adError.message}")
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isLoadingAd = false
                    Log.d("InterstitialAdManager", "Ad loaded.")
                }
            }
        )
    }

    fun showAdIfReady(activity: Activity, onAdDismissed: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (mInterstitialAd != null && (currentTime - lastAdShownTime > 60_000L)) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    lastAdShownTime = System.currentTimeMillis()
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    mInterstitialAd = null
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            if (mInterstitialAd == null && !isLoadingAd) {
                loadAd(activity)
            }
            onAdDismissed()
        }
    }
}

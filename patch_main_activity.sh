#!/bin/bash

# Add imports
sed -i '/import androidx.compose.material3.\*/a import android.app.Activity\nimport androidx.activity.compose.BackHandler\nimport androidx.compose.ui.platform.LocalContext' app/src/main/java/com/example/MainActivity.kt

# Inject navigateBackWithAd inside SuperStudyApp
sed -i '/var activePdf/a \
\
    val context = LocalContext.current\n    val activity = context as? Activity\n    val navigateBackWithAd = {\n        if (activity != null) {\n            com.example.util.InterstitialAdManager.showAdIfReady(activity) {\n                navController.popBackStack()\n            }\n        } else {\n            navController.popBackStack()\n        }\n    }' app/src/main/java/com/example/MainActivity.kt

# Inject BackHandler inside Scaffold
sed -i '/NavHost(/i \
                val isTopLevel = currentRoute in listOf(\n                    NavDestination.Home.route,\n                    NavDestination.Study.route,\n                    NavDestination.Tests.route,\n                    NavDestination.Leaderboard.route,\n                    NavDestination.More.route\n                )\n                if (!isTopLevel && currentRoute != "test_result") {\n                    BackHandler {\n                        navigateBackWithAd()\n                    }\n                }\n' app/src/main/java/com/example/MainActivity.kt

# Replace onNavigateBack = { navController.popBackStack() }
sed -i 's/onNavigateBack = { navController.popBackStack() }/onNavigateBack = navigateBackWithAd/g' app/src/main/java/com/example/MainActivity.kt


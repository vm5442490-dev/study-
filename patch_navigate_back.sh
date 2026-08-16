#!/bin/bash
sed -i 's/} else {/    Unit\n        } else {/g' app/src/main/java/com/example/MainActivity.kt
sed -i '/navController.popBackStack()/ { /} else {/! s/navController.popBackStack()/navController.popBackStack()\n            Unit/g }' app/src/main/java/com/example/MainActivity.kt

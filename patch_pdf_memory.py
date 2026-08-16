import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

# Add imports if they don't exist
if "import android.content.ComponentCallbacks2" not in content:
    content = content.replace("import android.content.Context", "import android.content.ComponentCallbacks2\nimport android.content.res.Configuration\nimport android.content.Context")

# Add interface implementation
content = content.replace("class PdfDownloadManager(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {",
"""class PdfDownloadManager(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application), ComponentCallbacks2 {

    init {
        application.registerComponentCallbacks(this)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<android.app.Application>().unregisterComponentCallbacks(this)
        pageBitmapCache.evictAll()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            pageBitmapCache.trimToSize(cacheSize / 2)
        }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            pageBitmapCache.evictAll()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
    override fun onLowMemory() {
        pageBitmapCache.evictAll()
    }""")

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
